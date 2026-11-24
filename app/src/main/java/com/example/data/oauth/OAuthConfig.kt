package com.example.data.oauth

import java.net.URI

/**
 * Holds ONLY public, non-secret client configuration for official OAuth flows.
 * Client secrets, app secrets, and OAuth refresh/access tokens must NEVER be stored
 * in Android source code or packaged into the client APK.
 */
data class OAuthEnvironmentConfig(
  val backendBaseUrl: String = "",
  val youtubeClientId: String = "",
  val facebookAppId: String = "",
  val tiktokClientKey: String = "",
  val appRedirectScheme: String = "zuzusocial://oauth/callback",
)

object OAuthUrlValidator {
  fun isValidSecureBackendUrl(rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return false
    return try {
      val uri = URI(trimmed)
      val scheme = uri.scheme?.lowercase() ?: return false
      val host = uri.host?.lowercase() ?: return false
      if (scheme != "https") return false
      if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0") return false
      if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")) {
        return false
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  fun isValidAuthorizationUrl(rawUrl: String): Boolean {
    return try {
      val uri = URI(rawUrl)
      uri.scheme?.lowercase() == "https" && !uri.host.isNullOrBlank()
    } catch (_: Exception) {
      false
    }
  }
}
