package com.example.model

enum class SocialPlatform(
  val id: String,
  val displayName: String,
  val oauthProtocolLabel: String,
) {
  YouTube(
    id = "youtube",
    displayName = "YouTube",
    oauthProtocolLabel = "YouTube / Google OAuth 2.0",
  ),
  Facebook(
    id = "facebook",
    displayName = "Facebook",
    oauthProtocolLabel = "Facebook / Meta OAuth",
  ),
  TikTok(
    id = "tiktok",
    displayName = "TikTok",
    oauthProtocolLabel = "TikTok Login Kit",
  ),
}

enum class PlatformConnectionPhase {
  NotConnected,
  Connecting,
  SetupRequired,
  Connected,
  Error,
}

data class ConnectedSocialAccount(
  val accountId: String,
  val platform: SocialPlatform,
  val displayName: String,
  val handle: String,
  val grantedScopes: List<String>,
  val connectedAtLabel: String,
)

data class OAuthProviderMetadata(
  val platform: SocialPlatform,
  val authorizationEndpoint: String,
  val requiredScopes: List<String>,
  val redirectUriScheme: String,
)

data class SocialPlatformAccountState(
  val platform: SocialPlatform,
  val phase: PlatformConnectionPhase = PlatformConnectionPhase.NotConnected,
  val connectedAccounts: List<ConnectedSocialAccount> = emptyList(),
  val statusMessage: String? = null,
  val errorMessage: String? = null,
  val missingSetupItems: List<String> = emptyList(),
  val metadata: OAuthProviderMetadata,
  val supportsMultiAccount: Boolean = true,
) {
  val isConnected: Boolean
    get() = phase == PlatformConnectionPhase.Connected && connectedAccounts.isNotEmpty()
}

data class OAuthLaunchRequest(
  val platform: SocialPlatform,
  val authorizationUrl: String,
  val stateToken: String,
)

sealed interface OAuthStartResult {
  data class LaunchAuthorization(
    val authorizationUrl: String,
    val stateToken: String,
    val codeVerifier: String,
  ) : OAuthStartResult

  data class SetupRequired(
    val missingItems: List<String>,
    val explanation: String,
  ) : OAuthStartResult

  data class Failure(
    val reason: String,
  ) : OAuthStartResult
}

sealed interface OAuthDisconnectResult {
  data object Success : OAuthDisconnectResult
  data class Failure(val reason: String) : OAuthDisconnectResult
}
