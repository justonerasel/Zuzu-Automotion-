package com.example.data.oauth

import com.example.model.OAuthDisconnectResult
import com.example.model.OAuthProviderMetadata
import com.example.model.OAuthStartResult
import com.example.model.SocialPlatform

class FacebookMetaOAuthProvider : SocialOAuthProvider {

  override val platform: SocialPlatform = SocialPlatform.Facebook

  override val metadata: OAuthProviderMetadata = OAuthProviderMetadata(
    platform = SocialPlatform.Facebook,
    authorizationEndpoint = "https://www.facebook.com/v20.0/dialog/oauth",
    requiredScopes = listOf(
      "pages_show_list",
      "pages_read_engagement",
      "pages_manage_posts",
    ),
    redirectUriScheme = "zuzusocial://oauth/callback/facebook",
  )

  override fun missingConfigurationItems(config: OAuthEnvironmentConfig): List<String> {
    val missing = mutableListOf<String>()
    if (config.facebookAppId.isBlank()) {
      missing.add("Meta Developer App ID with Facebook Login enabled")
    }
    if (!OAuthUrlValidator.isValidSecureBackendUrl(config.backendBaseUrl)) {
      missing.add("Secure HTTPS OAuth backend endpoint for Meta token exchange")
    }
    return missing
  }

  override suspend fun initiateAuthorization(config: OAuthEnvironmentConfig): OAuthStartResult {
    val missing = missingConfigurationItems(config)
    if (missing.isNotEmpty()) {
      return OAuthStartResult.SetupRequired(
        missingItems = missing,
        explanation =
          "Facebook / Meta OAuth requires a registered Meta App ID and a backend HTTPS token-exchange server so the Meta App Secret never resides on the device.",
      )
    }

    val pkce = PkceGenerator.create()
    val scopeParam = encodeQueryParam(metadata.requiredScopes.joinToString(","))
    val url = buildString {
      append(metadata.authorizationEndpoint)
      append("?client_id=").append(encodeQueryParam(config.facebookAppId.trim()))
      append("&redirect_uri=").append(encodeQueryParam(metadata.redirectUriScheme))
      append("&response_type=code")
      append("&scope=").append(scopeParam)
      append("&code_challenge=").append(pkce.codeChallenge)
      append("&code_challenge_method=S256")
      append("&state=").append(pkce.stateToken)
    }

    if (!OAuthUrlValidator.isValidAuthorizationUrl(url)) {
      return OAuthStartResult.Failure("Generated Meta OAuth authorization URL failed validation.")
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
        "Backend Meta token revocation endpoint is not configured.",
      )
    }
    return OAuthDisconnectResult.Success
  }
}
