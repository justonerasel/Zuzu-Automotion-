package com.example.data.oauth

import com.example.model.OAuthDisconnectResult
import com.example.model.OAuthProviderMetadata
import com.example.model.OAuthStartResult
import com.example.model.SocialPlatform

class YouTubeGoogleOAuthProvider : SocialOAuthProvider {

  override val platform: SocialPlatform = SocialPlatform.YouTube

  override val metadata: OAuthProviderMetadata = OAuthProviderMetadata(
    platform = SocialPlatform.YouTube,
    authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
    requiredScopes = listOf(
      "https://www.googleapis.com/auth/youtube.readonly",
      "https://www.googleapis.com/auth/youtube.upload",
    ),
    redirectUriScheme = "zuzusocial://oauth/callback/youtube",
  )

  override fun missingConfigurationItems(config: OAuthEnvironmentConfig): List<String> {
    val missing = mutableListOf<String>()
    if (config.youtubeClientId.isBlank()) {
      missing.add("Google Cloud OAuth 2.0 Client ID (Android / Web)")
    }
    if (!OAuthUrlValidator.isValidSecureBackendUrl(config.backendBaseUrl)) {
      missing.add("Secure HTTPS OAuth backend endpoint for token exchange")
    }
    return missing
  }

  override suspend fun initiateAuthorization(config: OAuthEnvironmentConfig): OAuthStartResult {
    val missing = missingConfigurationItems(config)
    if (missing.isNotEmpty()) {
      return OAuthStartResult.SetupRequired(
        missingItems = missing,
        explanation =
          "YouTube / Google OAuth requires a registered Google Cloud OAuth Client ID and a backend HTTPS endpoint to exchange authorization codes without exposing client secrets in the app.",
      )
    }

    val pkce = PkceGenerator.create()
    val scopeParam = encodeQueryParam(metadata.requiredScopes.joinToString(" "))
    val url = buildString {
      append(metadata.authorizationEndpoint)
      append("?client_id=").append(encodeQueryParam(config.youtubeClientId.trim()))
      append("&redirect_uri=").append(encodeQueryParam(metadata.redirectUriScheme))
      append("&response_type=code")
      append("&scope=").append(scopeParam)
      append("&access_type=offline")
      append("&prompt=consent")
      append("&code_challenge=").append(pkce.codeChallenge)
      append("&code_challenge_method=S256")
      append("&state=").append(pkce.stateToken)
    }

    if (!OAuthUrlValidator.isValidAuthorizationUrl(url)) {
      return OAuthStartResult.Failure("Generated Google OAuth authorization URL failed validation.")
    }

    return OAuthStartResult.LaunchAuthorization(
      authorizationUrl = url,
      stateToken = pkce.stateToken,
      codeVerifier = pkce.codeVerifier,
    )
  }

  override suspend fun revokeAccount(
    config: OAuthEnvironmentConfig,
    accountId: String,
  ): OAuthDisconnectResult {
    if (!OAuthUrlValidator.isValidSecureBackendUrl(config.backendBaseUrl)) {
      return OAuthDisconnectResult.Failure(
        "Backend token revocation endpoint is not configured.",
      )
    }
    return OAuthDisconnectResult.Success
  }
}
