package com.example.security

import com.example.data.oauth.OAuthUrlValidator
import java.io.File
import java.net.URI
import java.util.Locale

/**
 * Centralized input validation, log/error redaction, URL safety validation,
 * and privacy-preserving local storage inspection helpers.
 */
object SecuritySanitizer {

  private val bearerRegex = Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+")
  private val keyValueSecretRegex = Regex(
    "(?i)(access_token|refresh_token|id_token|client_secret|api_key|apikey|app_secret|password|authorization)\\s*([=:])\\s*[^\\s,;\"'&]+",
  )
  private val tokenPrefixRegex = Regex("\\b(ya29\\.[A-Za-z0-9_-]+|AIza[0-9A-Za-z_-]{10,}|sk-[A-Za-z0-9]{10,}|EAAG[A-Za-z0-9]+|ghp_[A-Za-z0-9]+)\\b")

  fun sanitizeUserText(input: String, maxLength: Int = 500): String {
    if (maxLength <= 0) return ""
    val filtered = buildString(input.length) {
      for (ch in input) {
        if (!ch.isISOControl() || ch == '\n' || ch == '\t') {
          append(ch)
        }
      }
    }
    return filtered.trim().take(maxLength)
  }

  fun redactSensitiveTokens(message: String?): String {
    if (message.isNullOrBlank()) return ""
    var clean = bearerRegex.replace(message, "$1[REDACTED]")
    clean = keyValueSecretRegex.replace(clean, "$1$2[REDACTED]")
    clean = tokenPrefixRegex.replace(clean, "[REDACTED_CREDENTIAL]")
    return clean.trim()
  }

  fun isSafeHttpsUrl(rawUrl: String): Boolean {
    return OAuthUrlValidator.isValidSecureBackendUrl(rawUrl)
  }

  fun isSafeExternalHttpUrl(rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return false
    return try {
      val uri = URI(trimmed)
      val scheme = uri.scheme?.lowercase() ?: return false
      val host = uri.host?.lowercase() ?: return false
      scheme == "https" &&
        host.isNotBlank() &&
        host != "localhost" &&
        host != "127.0.0.1" &&
        host != "0.0.0.0"
    } catch (_: Exception) {
      false
    }
  }

  fun formatBytesReadable(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    return when {
      safe < 1024L -> "$safe B"
      safe < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", safe / 1024.0)
      safe < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f MB", safe / (1024.0 * 1024.0))
      else -> String.format(Locale.US, "%.2f GB", safe / (1024.0 * 1024.0 * 1024.0))
    }
  }

  fun directorySizeBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    var total = 0L
    dir.listFiles()?.forEach { child ->
      total += if (child.isDirectory) directorySizeBytes(child) else child.length()
    }
    return total
  }

  fun countFilesInDirectory(dir: File?): Int {
    if (dir == null || !dir.exists()) return 0
    if (dir.isFile) return 1
    var count = 0
    dir.listFiles()?.forEach { child ->
      count += if (child.isDirectory) countFilesInDirectory(child) else 1
    }
    return count
  }

  fun clearDirectoryContentsSafely(dir: File?): Int {
    if (dir == null || !dir.exists() || !dir.isDirectory) return 0
    var removed = 0
    dir.listFiles()?.forEach { child ->
      if (child.deleteRecursively()) {
        removed++
      }
    }
    return removed
  }
}

data class LocalStorageSummary(
  val exportedFilesCount: Int = 0,
  val exportsSizeBytes: Long = 0L,
  val cacheSizeBytes: Long = 0L,
  val databaseSizeBytes: Long = 0L,
) {
  val totalManagedBytes: Long
    get() = exportsSizeBytes + cacheSizeBytes + databaseSizeBytes
}
