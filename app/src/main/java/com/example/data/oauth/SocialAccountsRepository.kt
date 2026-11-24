package com.example.data.oauth

import com.example.model.ConnectedSocialAccount
import com.example.model.OAuthDisconnectResult
import com.example.model.OAuthLaunchRequest
import com.example.model.OAuthStartResult
import com.example.model.PlatformConnectionPhase
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SocialAccountsRepository(
  private val config: OAuthEnvironmentConfig = OAuthEnvironmentConfig(),
  providers: List<SocialOAuthProvider> = listOf(
    YouTubeGoogleOAuthProvider(),
    FacebookMetaOAuthProvider(),
    TikTokLoginKitProvider(),
  ),
) {
  private val providerMap: Map<SocialPlatform, SocialOAuthProvider> =
    providers.associateBy { it.platform }

  private val pendingStates = mutableMapOf<SocialPlatform, PendingOAuthSession>()

  private val _platformStates = MutableStateFlow(
    SocialPlatform.entries.map { platform ->
      val provider = providerMap.getValue(platform)
      SocialPlatformAccountState(
        platform = platform,
        phase = PlatformConnectionPhase.NotConnected,
        metadata = provider.metadata,
        missingSetupItems = provider.missingConfigurationItems(config),
      )
    },
  )
  val platformStates: StateFlow<List<SocialPlatformAccountState>> = _platformStates.asStateFlow()

  private val _launchRequest = MutableStateFlow<OAuthLaunchRequest?>(null)
  val launchRequest: StateFlow<OAuthLaunchRequest?> = _launchRequest.asStateFlow()

  suspend fun connectPlatform(platform: SocialPlatform) {
    val provider = providerMap[platform] ?: run {
      updatePlatform(platform) { state ->
        state.copy(
          phase = PlatformConnectionPhase.Error,
          errorMessage = "OAuth provider adapter for ${platform.displayName} is unavailable.",
        )
      }
      return
    }

    updatePlatform(platform) { state ->
      state.copy(
        phase = PlatformConnectionPhase.Connecting,
        statusMessage = "Checking ${platform.oauthProtocolLabel} configuration...",
        errorMessage = null,
      )
    }

    when (val result = provider.initiateAuthorization(config)) {
      is OAuthStartResult.LaunchAuthorization -> {
        pendingStates[platform] = PendingOAuthSession(
          stateToken = result.stateToken,
          codeVerifier = result.codeVerifier,
        )
        updatePlatform(platform) { state ->
          state.copy(
            phase = PlatformConnectionPhase.Connecting,
            statusMessage = "Waiting for ${platform.displayName} authorization in browser...",
            errorMessage = null,
          )
        }
        _launchRequest.value = OAuthLaunchRequest(
          platform = platform,
          authorizationUrl = result.authorizationUrl,
          stateToken = result.stateToken,
        )
      }

      is OAuthStartResult.SetupRequired -> {
        updatePlatform(platform) { state ->
          state.copy(
            phase = if (state.connectedAccounts.isNotEmpty()) {
              PlatformConnectionPhase.Connected
            } else {
              PlatformConnectionPhase.SetupRequired
            },
            statusMessage = result.explanation,
            missingSetupItems = result.missingItems,
            errorMessage = null,
          )
        }
      }

      is OAuthStartResult.Failure -> {
        updatePlatform(platform) { state ->
          state.copy(
            phase = PlatformConnectionPhase.Error,
            errorMessage = result.reason,
            statusMessage = null,
          )
        }
      }
    }
  }

  fun consumeLaunchRequest() {
    _launchRequest.value = null
  }

  /**
   * Called only after a real backend OAuth token exchange verifies the authorization code
   * and CSRF state token. Never invoked with mock/fake credentials in production.
   */
  fun onAuthorizationVerified(
    platform: SocialPlatform,
    stateToken: String,
    verifiedAccount: ConnectedSocialAccount,
  ): Boolean {
    val pending = pendingStates[platform] ?: return false
    if (pending.stateToken != stateToken) {
      updatePlatform(platform) { state ->
        state.copy(
          phase = PlatformConnectionPhase.Error,
          errorMessage = "OAuth state mismatch. Authorization cancelled for security.",
          statusMessage = null,
        )
      }
      return false
    }

    pendingStates.remove(platform)
    updatePlatform(platform) { state ->
      val updatedList = (state.connectedAccounts.filterNot { it.accountId == verifiedAccount.accountId } +
        verifiedAccount)
      state.copy(
        phase = PlatformConnectionPhase.Connected,
        connectedAccounts = updatedList,
        statusMessage = null,
        errorMessage = null,
      )
    }
    return true
  }

  fun onAuthorizationFailed(platform: SocialPlatform, reason: String) {
    pendingStates.remove(platform)
    updatePlatform(platform) { state ->
      state.copy(
        phase = PlatformConnectionPhase.Error,
        errorMessage = reason,
        statusMessage = null,
      )
    }
  }

  suspend fun disconnectAccount(platform: SocialPlatform, accountId: String) {
    val provider = providerMap[platform] ?: return
    updatePlatform(platform) { state ->
      state.copy(
        phase = PlatformConnectionPhase.Connecting,
        statusMessage = "Disconnecting account...",
        errorMessage = null,
      )
    }

    when (val result = provider.revokeAccount(config, accountId)) {
      is OAuthDisconnectResult.Success -> {
        updatePlatform(platform) { state ->
          val remaining = state.connectedAccounts.filterNot { it.accountId == accountId }
          state.copy(
            phase = if (remaining.isEmpty()) {
              PlatformConnectionPhase.NotConnected
            } else {
              PlatformConnectionPhase.Connected
            },
            connectedAccounts = remaining,
            statusMessage = null,
            errorMessage = null,
          )
        }
      }

      is OAuthDisconnectResult.Failure -> {
        updatePlatform(platform) { state ->
          state.copy(
            phase = PlatformConnectionPhase.Error,
            errorMessage = result.reason,
            statusMessage = null,
          )
        }
      }
    }
  }

  suspend fun disconnectAll(platform: SocialPlatform) {
    val current = _platformStates.value.firstOrNull { it.platform == platform } ?: return
    val accounts = current.connectedAccounts
    if (accounts.isEmpty()) {
      resetStatus(platform)
      return
    }
    accounts.forEach { account ->
      disconnectAccount(platform, account.accountId)
    }
  }

  fun resetStatus(platform: SocialPlatform) {
    pendingStates.remove(platform)
    updatePlatform(platform) { state ->
      state.copy(
        phase = if (state.connectedAccounts.isNotEmpty()) {
          PlatformConnectionPhase.Connected
        } else {
          PlatformConnectionPhase.NotConnected
        },
        statusMessage = null,
        errorMessage = null,
      )
    }
  }

  private fun updatePlatform(
    platform: SocialPlatform,
    transform: (SocialPlatformAccountState) -> SocialPlatformAccountState,
  ) {
    _platformStates.update { list ->
      list.map { item -> if (item.platform == platform) transform(item) else item }
    }
  }

  private data class PendingOAuthSession(
    val stateToken: String,
    val codeVerifier: String,
  )
}
