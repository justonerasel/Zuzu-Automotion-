package com.example.data.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.AudioFormat
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.EditorAudioTrack
import com.example.model.EditorClipType
import com.example.model.EditorTimelineClip
import com.example.model.VideoEditorDraft
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Native Android MP4 renderer built on MediaCodec (H.264 AVC + AAC) and MediaMuxer.
 * Zero external FFmpeg binaries required; performs genuine encoding and verifies output MP4 files.
 */
class AndroidMediaCodecMp4Renderer(
  private val context: Context,
) : VideoMp4Renderer {

  override fun checkCapability(): RendererCapabilityStatus {
    return runCatching {
      val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
      val avcEncoder = list.codecInfos.firstOrNull { info ->
        info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
      }
      val aacEncoder = list.codecInfos.firstOrNull { info ->
        info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) }
      }
      if (avcEncoder != null) {
        RendererCapabilityStatus.Available(
          engineName = "Android MediaCodec H.264 / AAC",
          description = buildString {
            append("Video encoder: ${avcEncoder.name}")
            if (aacEncoder != null) append(" • Audio encoder: ${aacEncoder.name}")
          },
        )
      } else {
        RendererCapabilityStatus.Unavailable(
          engineName = "Android MediaCodec",
          reason = "No H.264 (video/avc) hardware or software encoder available on this device.",
        )
      }
    }.getOrElse { err ->
      RendererCapabilityStatus.Unavailable(
        engineName = "Android MediaCodec",
        reason = err.message ?: "MediaCodec query unavailable in this environment.",
      )
    }
  }

  override suspend fun renderToMp4(
    draft: VideoEditorDraft,
    outputFile: File,
    onProgress: (RenderProgress) -> Unit,
  ): RenderOutcome = withContext(Dispatchers.IO) {
    if (draft.clips.isEmpty() || draft.totalDurationMs <= 0L) {
      return@withContext RenderOutcome.Error("Add at least one scene clip to the timeline before rendering.")
    }

    val cap = checkCapability()
    if (cap is RendererCapabilityStatus.Unavailable) {
      return@withContext RenderOutcome.Error(cap.reason)
    }

    outputFile.parentFile?.mkdirs()
    val tempVideoFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_vtmp.mp4")
    val tempAudioFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_atmp.m4a")

    val imageBitmaps = mutableMapOf<String, Bitmap>()
    val videoRetrievers = mutableMapOf<String, MediaMetadataRetriever>()

    try {
      onProgress(RenderProgress(0.03f, "Preparing timeline media..."))
      currentCoroutineContext().ensureActive()

      val width = draft.exportSettings.outputWidth
      val height = draft.exportSettings.outputHeight
      val fps = draft.exportSettings.quality.fps
      val videoBitrate = draft.exportSettings.videoBitrateBps
      val totalDurationMs = draft.totalDurationMs

      // Preload image bitmaps and initialize video retrievers
      draft.clips.forEach { clip ->
        currentCoroutineContext().ensureActive()
        val uriStr = clip.mediaUriString ?: return@forEach
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return@forEach
        when (clip.clipType) {
          EditorClipType.Image -> {
            decodeScaledBitmap(uri, maxOf(width, height))?.let { bmp ->
              imageBitmaps[clip.id] = bmp
            }
          }
          EditorClipType.Video -> {
            val retriever = MediaMetadataRetriever()
            runCatching {
              retriever.setDataSource(context, uri)
              videoRetrievers[clip.id] = retriever
            }.onFailure {
              runCatching { retriever.release() }
            }
          }
          EditorClipType.TitleCard -> Unit
        }
      }

      var lastVideoBucketKey: String? = null
      var lastVideoBitmap: Bitmap? = null

      onProgress(RenderProgress(0.08f, "Encoding H.264 video frames ($width x $height)..."))
      encodeVideoTrack(
        draft = draft,
        outputVideoFile = tempVideoFile,
        width = width,
        height = height,
        fps = fps,
        bitrate = videoBitrate,
        totalDurationMs = totalDurationMs,
        bitmapProvider = { clip, localMs ->
          when (clip.clipType) {
            EditorClipType.Image -> imageBitmaps[clip.id]
            EditorClipType.Video -> {
              val ret = videoRetrievers[clip.id]
              val targetUs = (clip.trimStartMs + localMs) * 1000L
              val bucketKey = "${clip.id}_${targetUs / 250_000L}"
              if (bucketKey != lastVideoBucketKey) {
                lastVideoBitmap?.let { old -> if (!old.isRecycled) old.recycle() }
                lastVideoBitmap = ret?.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                lastVideoBucketKey = bucketKey
              }
              lastVideoBitmap
            }
            EditorClipType.TitleCard -> null
          }
        },
        onProgress = { videoFrac ->
          onProgress(RenderProgress(0.08f + videoFrac * 0.65f, "Encoding video ${(videoFrac * 100).roundToInt()}%"))
        },
      )
      lastVideoBitmap?.let { old -> if (!old.isRecycled) old.recycle() }

      val activeAudioTracks = draft.audioTracks.filter { !it.isMuted && it.volume > 0.01f }
      var hasAudioTrack = false
      if (activeAudioTracks.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        onProgress(RenderProgress(0.76f, "Mixing & encoding AAC audio tracks..."))
        hasAudioTrack = encodeMixedAudioTrack(
          tracks = activeAudioTracks,
          totalDurationMs = totalDurationMs,
          audioBitrate = draft.exportSettings.quality.audioBitrateBps,
          outputAudioFile = tempAudioFile,
        )
      }

      currentCoroutineContext().ensureActive()
      onProgress(RenderProgress(0.92f, "Muxing final MP4 container..."))
      muxTracksToFinalMp4(
        videoFile = tempVideoFile,
        audioFile = if (hasAudioTrack && tempAudioFile.exists()) tempAudioFile else null,
        outputFile = outputFile,
      )

      if (!outputFile.exists() || outputFile.length() <= 0L) {
        return@withContext RenderOutcome.Error("Rendered MP4 file was empty or missing.")
      }

      val verifiedDuration = verifyOutputDuration(outputFile) ?: totalDurationMs
      onProgress(RenderProgress(1.0f, "Export completed!"))
      RenderOutcome.Success(
        outputFile = outputFile,
        durationMs = verifiedDuration,
        width = width,
        height = height,
        fileSizeBytes = outputFile.length(),
      )
    } catch (_: kotlinx.coroutines.CancellationException) {
      runCatching { outputFile.delete() }
      RenderOutcome.Cancelled
    } catch (t: Throwable) {
      runCatching { outputFile.delete() }
      RenderOutcome.Error(t.message ?: "MP4 rendering failed.")
    } finally {
      imageBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
      videoRetrievers.values.forEach { runCatching { it.release() } }
      runCatching { tempVideoFile.delete() }
      runCatching { tempAudioFile.delete() }
    }
  }

  private suspend fun encodeVideoTrack(
    draft: VideoEditorDraft,
    outputVideoFile: File,
    width: Int,
    height: Int,
    fps: Int,
    bitrate: Int,
    totalDurationMs: Long,
    bitmapProvider: (EditorTimelineClip, Long) -> Bitmap?,
    onProgress: (Float) -> Unit,
  ) {
    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
      setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
      setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
      setInteger(MediaFormat.KEY_FRAME_RATE, fps)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val muxer = MediaMuxer(outputVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val frameCanvas = Canvas(frameBitmap)
    val argbPixels = IntArray(width * height)
    val bufferInfo = MediaCodec.BufferInfo()

    var trackIndex = -1
    var muxerStarted = false

    try {
      codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      codec.start()

      val totalFrames = ((totalDurationMs * fps) / 1000L).toInt().coerceAtLeast(1)
      val frameDurationUs = 1_000_000L / fps
      var frameIdx = 0
      var inputDone = false
      var outputDone = false

      while (!outputDone) {
        currentCoroutineContext().ensureActive()

        if (!inputDone) {
          val inputIndex = codec.dequeueInputBuffer(10_000L)
          if (inputIndex >= 0) {
            if (frameIdx >= totalFrames) {
              codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inputDone = true
            } else {
              val ptsUs = frameIdx * frameDurationUs
              val timeMs = (frameIdx * 1000L) / fps
              RenderCanvasDrawer.drawFrame(
                canvas = frameCanvas,
                width = width,
                height = height,
                timeMs = timeMs,
                draft = draft,
                bitmapProvider = bitmapProvider,
              )
              frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)

              val img = runCatching { codec.getInputImage(inputIndex) }.getOrNull()
              val writtenBytes = if (img != null) {
                writeArgbToYuvImage(argbPixels, width, height, img)
                img.close()
                width * height * 3 / 2
              } else {
                val inputBuf = codec.getInputBuffer(inputIndex)
                if (inputBuf != null) {
                  writeArgbToI420Buffer(argbPixels, width, height, inputBuf)
                } else {
                  0
                }
              }

              codec.queueInputBuffer(inputIndex, 0, writtenBytes, ptsUs, 0)
              frameIdx++
              if (frameIdx % 6 == 0 || frameIdx == totalFrames) {
                onProgress(frameIdx.toFloat() / totalFrames.toFloat())
              }
            }
          }
        }

        val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
        when {
          outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            check(!muxerStarted) { "Video output format changed twice" }
            trackIndex = muxer.addTrack(codec.outputFormat)
            muxer.start()
            muxerStarted = true
          }
          outputIndex >= 0 -> {
            val encodedData = codec.getOutputBuffer(outputIndex)
            if (encodedData != null) {
              if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                bufferInfo.size = 0
              }
              if (bufferInfo.size > 0 && muxerStarted) {
                encodedData.position(bufferInfo.offset)
                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
              }
            }
            codec.releaseOutputBuffer(outputIndex, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              outputDone = true
            }
          }
        }
      }
    } finally {
      frameBitmap.recycle()
      runCatching { codec.stop() }
      runCatching { codec.release() }
      if (muxerStarted) {
        runCatching { muxer.stop() }
      }
      runCatching { muxer.release() }
    }
  }

  private suspend fun encodeMixedAudioTrack(
    tracks: List<EditorAudioTrack>,
    totalDurationMs: Long,
    audioBitrate: Int,
    outputAudioFile: File,
  ): Boolean {
    val sampleRate = 44_100
    val channelCount = 2
    val totalSamplesPerChannel = ((totalDurationMs * sampleRate) / 1000L).toInt().coerceAtLeast(sampleRate / 2)
    val mixAccumulator = FloatArray(totalSamplesPerChannel * channelCount)
    var anyDecoded = false

    for (track in tracks) {
      currentCoroutineContext().ensureActive()
      val uri = runCatching { Uri.parse(track.uriString) }.getOrNull() ?: continue
      val pcm = decodeAudioToStereo44k(uri, track.trimStartMs, track.trimEndMs) ?: continue
      if (pcm.isEmpty()) continue
      anyDecoded = true
      val startSampleIdx = ((track.startOffsetMs * sampleRate) / 1000L).toInt() * channelCount
      val vol = track.volume.coerceIn(0f, 1.5f)
      for (i in pcm.indices) {
        val dstIdx = startSampleIdx + i
        if (dstIdx in mixAccumulator.indices) {
          mixAccumulator[dstIdx] += (pcm[i].toFloat() / 32768f) * vol
        }
      }
    }

    if (!anyDecoded) return false

    val mixedPcmBytes = ByteArray(mixAccumulator.size * 2)
    val bb = ByteBuffer.wrap(mixedPcmBytes).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in mixAccumulator) {
      val clamped = (sample.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
      bb.putShort(clamped)
    }

    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
      setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
      setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
      setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
    }

    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    val muxer = MediaMuxer(outputAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val bufferInfo = MediaCodec.BufferInfo()
    var trackIndex = -1
    var muxerStarted = false

    try {
      codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      codec.start()

      var byteOffset = 0
      var totalFramesSent = 0L
      var inputDone = false
      var outputDone = false
      val bytesPerFrame = channelCount * 2

      while (!outputDone) {
        currentCoroutineContext().ensureActive()
        if (!inputDone) {
          val inputIdx = codec.dequeueInputBuffer(10_000L)
          if (inputIdx >= 0) {
            val buf = codec.getInputBuffer(inputIdx)!!
            buf.clear()
            if (byteOffset >= mixedPcmBytes.size) {
              codec.queueInputBuffer(inputIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inputDone = true
            } else {
              val chunkSize = minOf(buf.capacity(), mixedPcmBytes.size - byteOffset)
              buf.put(mixedPcmBytes, byteOffset, chunkSize)
              val ptsUs = (totalFramesSent * 1_000_000L) / sampleRate
              codec.queueInputBuffer(inputIdx, 0, chunkSize, ptsUs, 0)
              byteOffset += chunkSize
              totalFramesSent += (chunkSize / bytesPerFrame)
            }
          }
        }

        val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
        when {
          outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            trackIndex = muxer.addTrack(codec.outputFormat)
            muxer.start()
            muxerStarted = true
          }
          outIdx >= 0 -> {
            val outBuf = codec.getOutputBuffer(outIdx)
            if (outBuf != null) {
              if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                bufferInfo.size = 0
              }
              if (bufferInfo.size > 0 && muxerStarted) {
                outBuf.position(bufferInfo.offset)
                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
              }
            }
            codec.releaseOutputBuffer(outIdx, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              outputDone = true
            }
          }
        }
      }
      return muxerStarted
    } finally {
      runCatching { codec.stop() }
      runCatching { codec.release() }
      if (muxerStarted) runCatching { muxer.stop() }
      runCatching { muxer.release() }
    }
  }

  private fun decodeAudioToStereo44k(
    uri: Uri,
    trimStartMs: Long,
    trimEndMs: Long,
  ): ShortArray? {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    return try {
      extractor.setDataSource(context, uri, null)
      var audioTrackIdx = -1
      var trackFormat: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith("audio/")) {
          audioTrackIdx = i
          trackFormat = fmt
          break
        }
      }
      if (audioTrackIdx < 0 || trackFormat == null) return null

      extractor.selectTrack(audioTrackIdx)
      if (trimStartMs > 0L) {
        extractor.seekTo(trimStartMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
      }

      val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
      decoder = MediaCodec.createDecoderByType(mime)
      decoder.configure(trackFormat, null, null, 0)
      decoder.start()

      val rawChunks = mutableListOf<ShortArray>()
      var srcSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var srcChannels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      val info = MediaCodec.BufferInfo()
      var inputDone = false
      var outputDone = false
      val maxEndUs = if (trimEndMs > trimStartMs && trimEndMs > 0L) trimEndMs * 1000L else Long.MAX_VALUE

      while (!outputDone) {
        if (!inputDone) {
          val inIdx = decoder.dequeueInputBuffer(5_000L)
          if (inIdx >= 0) {
            val inBuf = decoder.getInputBuffer(inIdx)!!
            val sampleSize = extractor.readSampleData(inBuf, 0)
            val sampleTimeUs = extractor.sampleTime
            if (sampleSize < 0 || sampleTimeUs > maxEndUs) {
              decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inputDone = true
            } else {
              decoder.queueInputBuffer(inIdx, 0, sampleSize, sampleTimeUs, 0)
              extractor.advance()
            }
          }
        }

        val outIdx = decoder.dequeueOutputBuffer(info, 5_000L)
        when {
          outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            val outFmt = decoder.outputFormat
            if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
              srcSampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
              srcChannels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
          }
          outIdx >= 0 -> {
            val outBuf = decoder.getOutputBuffer(outIdx)
            if (outBuf != null && info.size > 0) {
              val slice = outBuf.duplicate().apply {
                position(info.offset)
                limit(info.offset + info.size)
                order(ByteOrder.LITTLE_ENDIAN)
              }
              val shortBuf = slice.asShortBuffer()
              val shorts = ShortArray(shortBuf.remaining())
              shortBuf.get(shorts)
              if (info.presentationTimeUs >= trimStartMs * 1000L) {
                rawChunks += shorts
              }
            }
            decoder.releaseOutputBuffer(outIdx, false)
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              outputDone = true
            }
          }
        }
      }

      val totalShorts = rawChunks.sumOf { it.size }
      if (totalShorts == 0) return null
      val combined = ShortArray(totalShorts)
      var pos = 0
      rawChunks.forEach { chunk ->
        chunk.copyInto(combined, pos)
        pos += chunk.size
      }

      convertToStereo44k(combined, srcSampleRate, srcChannels)
    } catch (_: Exception) {
      null
    } finally {
      runCatching { decoder?.stop() }
      runCatching { decoder?.release() }
      runCatching { extractor.release() }
    }
  }

  private fun convertToStereo44k(
    input: ShortArray,
    srcRate: Int,
    srcChannels: Int,
  ): ShortArray {
    val srcFrames = input.size / srcChannels.coerceAtLeast(1)
    if (srcFrames <= 0) return ShortArray(0)
    if (srcRate == 44_100 && srcChannels == 2) return input

    val dstFrames = ((srcFrames.toLong() * 44_100L) / srcRate.coerceAtLeast(8000)).toInt().coerceAtLeast(1)
    val out = ShortArray(dstFrames * 2)
    val ratio = srcFrames.toDouble() / dstFrames.toDouble()
    for (f in 0 until dstFrames) {
      val srcF = (f * ratio).toInt().coerceIn(0, srcFrames - 1)
      val base = srcF * srcChannels
      val left = input[base]
      val right = if (srcChannels > 1) input[base + 1] else left
      out[f * 2] = left
      out[f * 2 + 1] = right
    }
    return out
  }

  private fun muxTracksToFinalMp4(
    videoFile: File,
    audioFile: File?,
    outputFile: File,
  ) {
    val videoExtractor = MediaExtractor()
    val audioExtractor = MediaExtractor()
    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    try {
      videoExtractor.setDataSource(videoFile.absolutePath)
      var vSrcTrack = -1
      var vOutTrack = -1
      for (i in 0 until videoExtractor.trackCount) {
        val fmt = videoExtractor.getTrackFormat(i)
        if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
          vSrcTrack = i
          vOutTrack = muxer.addTrack(fmt)
          break
        }
      }
      require(vSrcTrack >= 0) { "Temp video track missing." }

      var aSrcTrack = -1
      var aOutTrack = -1
      if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
        audioExtractor.setDataSource(audioFile.absolutePath)
        for (i in 0 until audioExtractor.trackCount) {
          val fmt = audioExtractor.getTrackFormat(i)
          if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
            aSrcTrack = i
            aOutTrack = muxer.addTrack(fmt)
            break
          }
        }
      }

      muxer.start()
      val buf = ByteBuffer.allocateDirect(512 * 1024)
      val info = MediaCodec.BufferInfo()

      videoExtractor.selectTrack(vSrcTrack)
      while (true) {
        val sampleSize = videoExtractor.readSampleData(buf, 0)
        if (sampleSize < 0) break
        info.offset = 0
        info.size = sampleSize
        info.presentationTimeUs = videoExtractor.sampleTime
        info.flags = mapExtractorFlagsToBufferFlags(videoExtractor.sampleFlags)
        muxer.writeSampleData(vOutTrack, buf, info)
        videoExtractor.advance()
      }

      if (aSrcTrack >= 0) {
        audioExtractor.selectTrack(aSrcTrack)
        while (true) {
          val sampleSize = audioExtractor.readSampleData(buf, 0)
          if (sampleSize < 0) break
          info.offset = 0
          info.size = sampleSize
          info.presentationTimeUs = audioExtractor.sampleTime
          info.flags = mapExtractorFlagsToBufferFlags(audioExtractor.sampleFlags)
          muxer.writeSampleData(aOutTrack, buf, info)
          audioExtractor.advance()
        }
      }
    } finally {
      runCatching { muxer.stop() }
      runCatching { muxer.release() }
      runCatching { videoExtractor.release() }
      runCatching { audioExtractor.release() }
    }
  }

  private fun mapExtractorFlagsToBufferFlags(sampleFlags: Int): Int {
    var bufferFlags = 0
    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
      bufferFlags = bufferFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
    }
    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
      bufferFlags = bufferFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
    }
    return bufferFlags
  }

  private fun decodeScaledBitmap(uri: Uri, maxDim: Int): Bitmap? {
    return try {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
      }
      var sample = 1
      val largest = maxOf(bounds.outWidth, bounds.outHeight)
      while (largest / sample > maxDim) {
        sample *= 2
      }
      val opts = BitmapFactory.Options().apply { inSampleSize = sample }
      context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun verifyOutputDuration(file: File): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(file.absolutePath)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun writeArgbToYuvImage(argb: IntArray, width: Int, height: Int, image: Image) {
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuf = yPlane.buffer
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer

    val yRowStride = yPlane.rowStride
    val yPixStride = yPlane.pixelStride
    val uvRowStride = uPlane.rowStride
    val uvPixStride = uPlane.pixelStride

    val yRow = ByteArray(width)
    for (j in 0 until height) {
      val rowOffset = j * width
      if (yPixStride == 1) {
        for (i in 0 until width) {
          val c = argb[rowOffset + i]
          val r = (c ushr 16) and 0xFF
          val g = (c ushr 8) and 0xFF
          val b = c and 0xFF
          val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
          yRow[i] = y.coerceIn(0, 255).toByte()
        }
        yBuf.position(j * yRowStride)
        yBuf.put(yRow)
      } else {
        var dst = j * yRowStride
        for (i in 0 until width) {
          val c = argb[rowOffset + i]
          val r = (c ushr 16) and 0xFF
          val g = (c ushr 8) and 0xFF
          val b = c and 0xFF
          val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
          yBuf.put(dst, y.coerceIn(0, 255).toByte())
          dst += yPixStride
        }
      }
    }

    val halfW = width / 2
    val halfH = height / 2
    for (j in 0 until halfH) {
      val rowOffset = (j * 2) * width
      var uvDst = j * uvRowStride
      for (i in 0 until halfW) {
        val c = argb[rowOffset + (i * 2)]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
        val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
        uBuf.put(uvDst, u.coerceIn(0, 255).toByte())
        vBuf.put(uvDst, v.coerceIn(0, 255).toByte())
        uvDst += uvPixStride
      }
    }
  }

  private fun writeArgbToI420Buffer(argb: IntArray, width: Int, height: Int, buf: ByteBuffer): Int {
    buf.clear()
    val frameSize = width * height
    var uIndex = frameSize
    var vIndex = frameSize + frameSize / 4
    for (j in 0 until height) {
      val rowOffset = j * width
      for (i in 0 until width) {
        val c = argb[rowOffset + i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        val y = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
        buf.put(rowOffset + i, y.toByte())
        if (j % 2 == 0 && i % 2 == 0) {
          val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
          val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
          if (uIndex < buf.capacity()) buf.put(uIndex++, u.toByte())
          if (vIndex < buf.capacity()) buf.put(vIndex++, v.toByte())
        }
      }
    }
    val total = frameSize * 3 / 2
    buf.position(0)
    buf.limit(minOf(total, buf.capacity()))
    return buf.limit()
  }
}
