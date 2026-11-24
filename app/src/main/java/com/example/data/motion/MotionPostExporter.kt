package com.example.data.motion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.MediaType
import com.example.model.MotionExportFormat
import com.example.model.MotionPostProject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

sealed interface MotionExportOutcome {
  data class Success(
    val outputFile: File,
    val format: MotionExportFormat,
    val width: Int,
    val height: Int,
    val durationMs: Long?,
    val fileSizeBytes: Long,
  ) : MotionExportOutcome

  data object Cancelled : MotionExportOutcome
  data class Error(val message: String) : MotionExportOutcome
}

interface MotionPostExporter {
  fun canEncodeMp4(): Boolean
  suspend fun exportMotionPost(
    project: MotionPostProject,
    format: MotionExportFormat,
    outputFile: File,
    onProgress: (Float, String) -> Unit,
  ): MotionExportOutcome
}

/**
 * Exports Auto Motion Post Studio projects cleanly to MP4 (native MediaCodec H.264/AAC + MediaMuxer)
 * or high-resolution PNG / JPEG graphics. Never fakes export output on error or missing codecs.
 */
class AndroidMotionPostExporter(
  private val context: Context,
) : MotionPostExporter {

  override fun canEncodeMp4(): Boolean = runCatching {
    val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    list.codecInfos.any { info ->
      info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
    }
  }.getOrDefault(false)

  override suspend fun exportMotionPost(
    project: MotionPostProject,
    format: MotionExportFormat,
    outputFile: File,
    onProgress: (Float, String) -> Unit,
  ): MotionExportOutcome = withContext(Dispatchers.IO) {
    outputFile.parentFile?.mkdirs()
    var mediaBitmap: Bitmap? = null
    var logoBitmap: Bitmap? = null

    try {
      onProgress(0.05f, "Loading motion media & logo assets...")
      currentCoroutineContext().ensureActive()

      val (width, height) = project.outputDimensions
      val primarySlot = project.mediaSlots.firstOrNull()
      if (primarySlot != null) {
        mediaBitmap = if (primarySlot.mediaType == MediaType.Video) {
          extractVideoThumbnail(primarySlot.uriString)
        } else {
          decodeScaledBitmap(Uri.parse(primarySlot.uriString), maxOf(width, height))
        }
      }
      if (!project.logoUriString.isNullOrBlank()) {
        logoBitmap = decodeScaledBitmap(Uri.parse(project.logoUriString), 256)
      }

      when (format) {
        MotionExportFormat.PngImage, MotionExportFormat.JpegImage -> {
          onProgress(0.50f, "Rendering ${format.displayLabel} frame...")
          val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          try {
            val canvas = Canvas(bmp)
            MotionCanvasDrawer.drawMotionFrame(
              canvas = canvas,
              width = width,
              height = height,
              progress = 0.55f,
              project = project,
              mediaBitmap = mediaBitmap,
              logoBitmap = logoBitmap,
            )
            onProgress(0.85f, "Saving ${format.extension.uppercase()} file...")
            FileOutputStream(outputFile).use { fos ->
              val compressFmt = if (format == MotionExportFormat.PngImage) {
                Bitmap.CompressFormat.PNG
              } else {
                Bitmap.CompressFormat.JPEG
              }
              check(bmp.compress(compressFmt, 94, fos)) { "Failed to compress ${format.extension} output." }
            }
          } finally {
            bmp.recycle()
          }
          onProgress(1f, "Graphic exported!")
          MotionExportOutcome.Success(
            outputFile = outputFile,
            format = format,
            width = width,
            height = height,
            durationMs = null,
            fileSizeBytes = outputFile.length(),
          )
        }

        MotionExportFormat.Mp4Video -> {
          if (!canEncodeMp4()) {
            return@withContext MotionExportOutcome.Error("No H.264 (video/avc) encoder available on this device.")
          }
          encodeMotionMp4(
            project = project,
            width = width,
            height = height,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
            outputFile = outputFile,
            onProgress = onProgress,
          )
        }
      }
    } catch (_: kotlinx.coroutines.CancellationException) {
      runCatching { outputFile.delete() }
      MotionExportOutcome.Cancelled
    } catch (t: Throwable) {
      runCatching { outputFile.delete() }
      MotionExportOutcome.Error(t.message ?: "Motion post export failed.")
    } finally {
      mediaBitmap?.let { if (!it.isRecycled) it.recycle() }
      logoBitmap?.let { if (!it.isRecycled) it.recycle() }
    }
  }

  private suspend fun encodeMotionMp4(
    project: MotionPostProject,
    width: Int,
    height: Int,
    mediaBitmap: Bitmap?,
    logoBitmap: Bitmap?,
    outputFile: File,
    onProgress: (Float, String) -> Unit,
  ): MotionExportOutcome {
    val tempVideo = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_mv.mp4")
    val tempAudio = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_ma.m4a")
    val fps = 24
    val bitrate = if (width * height > 900_000) 4_200_000 else 2_800_000
    val totalFrames = (project.durationSeconds.coerceIn(3, 30) * fps)

    try {
      val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
      }
      val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
      val muxer = MediaMuxer(tempVideo.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val frameCanvas = Canvas(frameBitmap)
      val argbPixels = IntArray(width * height)
      val bufferInfo = MediaCodec.BufferInfo()
      var trackIndex = -1
      var muxerStarted = false

      try {
        codec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val frameDurationUs = 1_000_000L / fps
        var frameIdx = 0
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
          currentCoroutineContext().ensureActive()
          if (!inputDone) {
            val inIdx = codec.dequeueInputBuffer(10_000L)
            if (inIdx >= 0) {
              if (frameIdx >= totalFrames) {
                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputDone = true
              } else {
                val p = frameIdx.toFloat() / totalFrames.toFloat()
                MotionCanvasDrawer.drawMotionFrame(
                  canvas = frameCanvas,
                  width = width,
                  height = height,
                  progress = p,
                  project = project,
                  mediaBitmap = mediaBitmap,
                  logoBitmap = logoBitmap,
                )
                frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                val img = runCatching { codec.getInputImage(inIdx) }.getOrNull()
                val written = if (img != null) {
                  writeArgbToYuvImage(argbPixels, width, height, img)
                  img.close()
                  width * height * 3 / 2
                } else {
                  val buf = codec.getInputBuffer(inIdx)
                  if (buf != null) writeArgbToI420Buffer(argbPixels, width, height, buf) else 0
                }
                codec.queueInputBuffer(inIdx, 0, written, frameIdx * frameDurationUs, 0)
                frameIdx++
                if (frameIdx % 6 == 0 || frameIdx == totalFrames) {
                  onProgress(0.10f + (frameIdx.toFloat() / totalFrames) * 0.72f, "Encoding motion frames...")
                }
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
              val encoded = codec.getOutputBuffer(outIdx)
              if (encoded != null) {
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bufferInfo.size = 0
                if (bufferInfo.size > 0 && muxerStarted) {
                  encoded.position(bufferInfo.offset)
                  encoded.limit(bufferInfo.offset + bufferInfo.size)
                  muxer.writeSampleData(trackIndex, encoded, bufferInfo)
                }
              }
              codec.releaseOutputBuffer(outIdx, false)
              if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
            }
          }
        }
      } finally {
        frameBitmap.recycle()
        runCatching { codec.stop() }
        runCatching { codec.release() }
        if (muxerStarted) runCatching { muxer.stop() }
        runCatching { muxer.release() }
      }

      var hasAudio = false
      val bgUriStr = project.backgroundMusicUriString
      if (!bgUriStr.isNullOrBlank()) {
        onProgress(0.86f, "Encoding background music...")
        hasAudio = encodeBackgroundMusic(
          musicUri = Uri.parse(bgUriStr),
          durationMs = project.durationMs,
          volume = project.backgroundMusicVolume,
          outputAudioFile = tempAudio,
        )
      }

      onProgress(0.94f, "Finalizing MP4 container...")
      muxTracks(
        videoFile = tempVideo,
        audioFile = if (hasAudio && tempAudio.exists()) tempAudio else null,
        outputFile = outputFile,
      )
      check(outputFile.exists() && outputFile.length() > 0L) { "Motion MP4 output was empty." }

      onProgress(1f, "Motion MP4 exported!")
      return MotionExportOutcome.Success(
        outputFile = outputFile,
        format = MotionExportFormat.Mp4Video,
        width = width,
        height = height,
        durationMs = project.durationMs,
        fileSizeBytes = outputFile.length(),
      )
    } finally {
      runCatching { tempVideo.delete() }
      runCatching { tempAudio.delete() }
    }
  }

  private fun encodeBackgroundMusic(
    musicUri: Uri,
    durationMs: Long,
    volume: Float,
    outputAudioFile: File,
  ): Boolean {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    return try {
      extractor.setDataSource(context, musicUri, null)
      var trackIdx = -1
      var fmt: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
          trackIdx = i
          fmt = f
          break
        }
      }
      if (trackIdx < 0 || fmt == null) return false
      extractor.selectTrack(trackIdx)
      val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return false
      decoder = MediaCodec.createDecoderByType(mime)
      decoder.configure(fmt, null, null, 0)
      decoder.start()

      var sampleRate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      val pcmChunks = mutableListOf<ShortArray>()
      val info = MediaCodec.BufferInfo()
      var inDone = false
      var outDone = false
      val maxUs = durationMs * 1000L

      while (!outDone) {
        if (!inDone) {
          val inIdx = decoder.dequeueInputBuffer(5_000L)
          if (inIdx >= 0) {
            val buf = decoder.getInputBuffer(inIdx)!!
            val sz = extractor.readSampleData(buf, 0)
            val ts = extractor.sampleTime
            if (sz < 0 || ts > maxUs) {
              decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inDone = true
            } else {
              decoder.queueInputBuffer(inIdx, 0, sz, ts, 0)
              extractor.advance()
            }
          }
        }
        val outIdx = decoder.dequeueOutputBuffer(info, 5_000L)
        when {
          outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            val of = decoder.outputFormat
            if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          }
          outIdx >= 0 -> {
            val outBuf = decoder.getOutputBuffer(outIdx)
            if (outBuf != null && info.size > 0) {
              val slice = outBuf.duplicate().apply {
                position(info.offset)
                limit(info.offset + info.size)
                order(ByteOrder.LITTLE_ENDIAN)
              }
              val sb = slice.asShortBuffer()
              val arr = ShortArray(sb.remaining())
              sb.get(arr)
              for (i in arr.indices) {
                arr[i] = (arr[i] * volume).roundToInt().coerceIn(-32768, 32767).toShort()
              }
              pcmChunks += arr
            }
            decoder.releaseOutputBuffer(outIdx, false)
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outDone = true
          }
        }
      }
      if (pcmChunks.isEmpty()) return false
      val totalShorts = pcmChunks.sumOf { it.size }
      val pcmBytes = ByteArray(totalShorts * 2)
      val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
      pcmChunks.forEach { chunk -> chunk.forEach { s -> bb.putShort(s) } }

      encodePcmToAac(pcmBytes, sampleRate, channels.coerceIn(1, 2), outputAudioFile)
    } catch (_: Exception) {
      false
    } finally {
      runCatching { decoder?.stop() }
      runCatching { decoder?.release() }
      runCatching { extractor.release() }
    }
  }

  private fun encodePcmToAac(
    pcmBytes: ByteArray,
    sampleRate: Int,
    channels: Int,
    outputAudioFile: File,
  ): Boolean {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
      setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
      setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
      setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
    }
    val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    val muxer = MediaMuxer(outputAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val info = MediaCodec.BufferInfo()
    var trackIdx = -1
    var started = false
    try {
      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()
      var offset = 0
      var frames = 0L
      var inDone = false
      var outDone = false
      val bpf = channels * 2
      while (!outDone) {
        if (!inDone) {
          val inIdx = encoder.dequeueInputBuffer(10_000L)
          if (inIdx >= 0) {
            val buf = encoder.getInputBuffer(inIdx)!!
            buf.clear()
            if (offset >= pcmBytes.size) {
              encoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inDone = true
            } else {
              val chunk = minOf(buf.capacity(), pcmBytes.size - offset)
              buf.put(pcmBytes, offset, chunk)
              encoder.queueInputBuffer(inIdx, 0, chunk, (frames * 1_000_000L) / sampleRate, 0)
              offset += chunk
              frames += chunk / bpf
            }
          }
        }
        val outIdx = encoder.dequeueOutputBuffer(info, 10_000L)
        when {
          outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            trackIdx = muxer.addTrack(encoder.outputFormat)
            muxer.start()
            started = true
          }
          outIdx >= 0 -> {
            val outBuf = encoder.getOutputBuffer(outIdx)
            if (outBuf != null) {
              if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
              if (info.size > 0 && started) {
                outBuf.position(info.offset)
                outBuf.limit(info.offset + info.size)
                muxer.writeSampleData(trackIdx, outBuf, info)
              }
            }
            encoder.releaseOutputBuffer(outIdx, false)
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outDone = true
          }
        }
      }
      return started
    } finally {
      runCatching { encoder.stop() }
      runCatching { encoder.release() }
      if (started) runCatching { muxer.stop() }
      runCatching { muxer.release() }
    }
  }

  private fun muxTracks(videoFile: File, audioFile: File?, outputFile: File) {
    val vExt = MediaExtractor()
    val aExt = MediaExtractor()
    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    try {
      vExt.setDataSource(videoFile.absolutePath)
      var vSrc = -1
      var vOut = -1
      for (i in 0 until vExt.trackCount) {
        val f = vExt.getTrackFormat(i)
        if (f.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
          vSrc = i
          vOut = muxer.addTrack(f)
          break
        }
      }
      var aSrc = -1
      var aOut = -1
      if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
        aExt.setDataSource(audioFile.absolutePath)
        for (i in 0 until aExt.trackCount) {
          val f = aExt.getTrackFormat(i)
          if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
            aSrc = i
            aOut = muxer.addTrack(f)
            break
          }
        }
      }
      muxer.start()
      val buf = ByteBuffer.allocateDirect(512 * 1024)
      val info = MediaCodec.BufferInfo()
      vExt.selectTrack(vSrc)
      while (true) {
        val sz = vExt.readSampleData(buf, 0)
        if (sz < 0) break
        info.set(0, sz, vExt.sampleTime, mapExtractorFlagsToBufferFlags(vExt.sampleFlags))
        muxer.writeSampleData(vOut, buf, info)
        vExt.advance()
      }
      if (aSrc >= 0) {
        aExt.selectTrack(aSrc)
        while (true) {
          val sz = aExt.readSampleData(buf, 0)
          if (sz < 0) break
          info.set(0, sz, aExt.sampleTime, mapExtractorFlagsToBufferFlags(aExt.sampleFlags))
          muxer.writeSampleData(aOut, buf, info)
          aExt.advance()
        }
      }
    } finally {
      runCatching { muxer.stop() }
      runCatching { muxer.release() }
      runCatching { vExt.release() }
      runCatching { aExt.release() }
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

  private fun decodeScaledBitmap(uri: Uri, maxDim: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / sample > maxDim) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
  }.getOrNull()

  private fun extractVideoThumbnail(uriStr: String): Bitmap? {
    val ret = MediaMetadataRetriever()
    return try {
      ret.setDataSource(context, Uri.parse(uriStr))
      ret.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: Exception) {
      null
    } finally {
      runCatching { ret.release() }
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
          yRow[i] = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255).toByte()
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
          yBuf.put(dst, (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255).toByte())
          dst += yPixStride
        }
      }
    }
    for (j in 0 until height / 2) {
      val rowOffset = (j * 2) * width
      var uvDst = j * uvRowStride
      for (i in 0 until width / 2) {
        val c = argb[rowOffset + (i * 2)]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        uBuf.put(uvDst, (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte())
        vBuf.put(uvDst, (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte())
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
        buf.put(rowOffset + i, (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255).toByte())
        if (j % 2 == 0 && i % 2 == 0) {
          if (uIndex < buf.capacity()) {
            buf.put(uIndex++, (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte())
          }
          if (vIndex < buf.capacity()) {
            buf.put(vIndex++, (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255).toByte())
          }
        }
      }
    }
    buf.position(0)
    buf.limit(minOf(frameSize * 3 / 2, buf.capacity()))
    return buf.limit()
  }
}
