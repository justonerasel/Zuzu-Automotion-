package com.example.data.oauth

import com.example.model.OAuthDisconnectResult
import com.example.model.OAuthProviderMetadata
import com.example.model.OAuthStartResult
import com.example.model.SocialPlatform

class TikTokLoginKitProvider : SocialOAuthProvider {

  override val platform: SocialPlatform = SocialPlatform.TikTok

  override val metadata: OAuthProviderMetadata = OAuthProviderMetadata(
    platform = SocialPlatform.TikTok,
    authorizationEndpoint = "https://www.tiktok.com/v2/auth/authorize/",
    requiredScopes = listOf(
      "user.info.basic",
      "video.list",
      "video.publish",
    ),
    redirectUriScheme = "zuzusocial://oauth/callback/tiktok",
  )

  override fun missingConfigurationItems(config: OAuthEnvironmentConfig): List<String> {
    val missing = mutableListOf<String>()
    if (config.tiktokClientKey.isBlank()) {
      missing.add("TikTok Developer Client Key with Login Kit enabled")
    }
    if (!OAuthUrlValidator.isValidSecureBackendUrl(config.backendBaseUrl)) {
      missing.add("Secure HTTPS OAuth backend endpoint for TikTok token exchange")
    }
    return missing
  }

  override suspend fun initiateAuthorization(config: OAuthEnvironmentConfig): OAuthStartResult {
    val missing = missingConfigurationItems(config)
    if (missing.isNotEmpty()) {
      return OAuthStartResult.SetupRequired(
        missingItems = missing,
        explanation =
          "TikTok Login Kit requires a registered TikTok Client Key and a backend HTTPS server to exchange the authorization code using your server-side Client Secret.",
      )
    }

    val pkce = PkceGenerator.create()
    val scopeParam = encodeQueryParam(metadata.requiredScopes.joinToString(","))
    val url = buildString {
      append(metadata.authorizationEndpoint)
      append("?client_key=").append(encodeQueryParam(config.tiktokClientKey.trim()))
      append("&redirect_uri=").append(encodeQueryParam(metadata.redirectUriScheme))
      append("&response_type=code")
      append("&scope=").append(scopeParam)
      append("&code_challenge=").append(pkce.codeChallenge)
      append("&code_challenge_method=S256")
      append("&state=").append(pkce.stateToken)
    }

    if (!OAuthUrlValidator.isValidAuthorizationUrl(url)) {
      return OAuthStartResult.Failure("Generated TikTok Login Kit authorization URL failed validation.")
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
        "Backend TikTok token revocation endpoint is not configured.",
      )
    }
    return OAuthDisconnectResult.Success
  }
}
