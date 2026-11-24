package com.example.data.source

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object MediaFileHelper {

  val supportedMimeTypes: Array<String> = arrayOf(
    "video/mp4",
    "video/quicktime",
    "image/jpeg",
    "image/png",
    "audio/mpeg",
    "audio/wav",
    "audio/x-wav",
  )

  fun extractPendingUpload(
    context: Context,
    uri: Uri,
  ): Result<PendingLocalUpload> {
    val contentResolver = context.contentResolver
    runCatching {
      contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    var fileName = "selected_media"
    var sizeBytes = 0L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (cursor.moveToFirst()) {
        if (nameIndex >= 0) {
          cursor.getString(nameIndex)?.let { fileName = it }
        }
        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
          sizeBytes = cursor.getLong(sizeIndex)
        }
      }
    }

    val rawMime = contentResolver.getType(uri)?.lowercase().orEmpty()
    val extension = fileName.substringAfterLast('.', "").lowercase()

    val mediaType = resolveMediaType(rawMime, extension)
      ?: return Result.failure(
        IllegalArgumentException(
          "Unsupported file format. Please choose MP4, MOV, JPG, PNG, MP3, or WAV.",
        ),
      )

    val resolvedMime = rawMime.ifBlank {
      when (extension) {
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        else -> "application/octet-stream"
      }
    }

    val durationMs = if (mediaType == MediaType.Video || mediaType == MediaType.Audio) {
      readDurationMs(context, uri)
    } else {
      null
    }

    return Result.success(
      PendingLocalUpload(
        uriString = uri.toString(),
        fileName = fileName,
        mimeType = resolvedMime,
        mediaType = mediaType,
        fileSizeBytes = sizeBytes,
        durationMs = durationMs,
      ),
    )
  }

  private fun resolveMediaType(mime: String, ext: String): MediaType? = when {
    mime == "video/mp4" || mime == "video/quicktime" || ext == "mp4" || ext == "mov" -> MediaType.Video
    mime == "image/jpeg" || mime == "image/png" || ext == "jpg" || ext == "jpeg" || ext == "png" -> MediaType.Image
    mime == "audio/mpeg" || mime == "audio/wav" || mime == "audio/x-wav" || ext == "mp3" || ext == "wav" -> MediaType.Audio
    else -> null
  }

  private fun readDurationMs(context: Context, uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  suspend fun loadPreviewBitmap(
    context: Context,
    uriString: String,
    mediaType: MediaType,
  ): Bitmap? = withContext(Dispatchers.IO) {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext null
    when (mediaType) {
      MediaType.Image -> decodeSampledBitmap(context, uri, maxDimension = 480)
      MediaType.Video -> extractVideoThumbnail(context, uri)
      else -> null
    }
  }

  private fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int,
  ): Bitmap? {
    return try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }
      var sampleSize = 1
      val maxActual = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
      while (maxActual / sampleSize > maxDimension) {
        sampleSize *= 2
      }
      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun extractVideoThumbnail(context: Context, uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
      mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
      kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
      else -> "$bytes B"
    }
  }

  fun formatDuration(durationMs: Long?): String? {
    if (durationMs == null || durationMs <= 0L) return null
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
  }
}
