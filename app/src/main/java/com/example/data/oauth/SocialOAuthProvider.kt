package com.example.data.oauth

import com.example.model.OAuthDisconnectResult
import com.example.model.OAuthProviderMetadata
import com.example.model.OAuthStartResult
import com.example.model.SocialPlatform
import java.net.URLEncoder

interface SocialOAuthProvider {
  val platform: SocialPlatform
  val metadata: OAuthProviderMetadata

  fun missingConfigurationItems(config: OAuthEnvironmentConfig): List<String>

  suspend fun initiateAuthorization(config: OAuthEnvironmentConfig): OAuthStartResult

  suspend fun revokeAccount(
    config: OAuthEnvironmentConfig,
    accountId: String,
  ): OAuthDisconnectResult
}

internal fun encodeQueryParam(value: String): String =
  URLEncoder.encode(value, Charsets.UTF_8.name())
