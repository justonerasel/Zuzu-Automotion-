package com.example

import com.example.data.oauth.OAuthEnvironmentConfig
import com.example.data.oauth.OAuthUrlValidator
import com.example.data.oauth.SocialAccountsRepository
import com.example.model.ConnectedSocialAccount
import com.example.model.PlatformConnectionPhase
import com.example.model.SocialPlatform
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialAccountsRepositoryTest {

  @Test
  fun `unconfigured environment enters SetupRequired and never fakes connected state`() = runTest {
    val repo = SocialAccountsRepository(config = OAuthEnvironmentConfig())

    SocialPlatform.entries.forEach { platform ->
      repo.connectPlatform(platform)
      val state = repo.platformStates.value.first { it.platform == platform }
      assertEquals(PlatformConnectionPhase.SetupRequired, state.phase)
      assertTrue(state.connectedAccounts.isEmpty())
      assertTrue(state.missingSetupItems.isNotEmpty())
      assertNull(repo.launchRequest.value)
    }
  }

  @Test
  fun `url validator rejects insecure http and loopback addresses`() {
    assertFalse(OAuthUrlValidator.isValidSecureBackendUrl(""))
    assertFalse(OAuthUrlValidator.isValidSecureBackendUrl("http://api.example.com"))
    assertFalse(OAuthUrlValidator.isValidSecureBackendUrl("https://localhost:8443"))
    assertFalse(OAuthUrlValidator.isValidSecureBackendUrl("https://127.0.0.1"))
    assertFalse(OAuthUrlValidator.isValidSecureBackendUrl("https://192.168.1.10"))
    assertTrue(OAuthUrlValidator.isValidSecureBackendUrl("https://oauth.zuzusocial.example.com"))
  }

  @Test
  fun `configured pkce flow validates state and supports multi-account disconnect`() = runTest {
    val repo = SocialAccountsRepository(
      config = OAuthEnvironmentConfig(
        backendBaseUrl = "https://oauth.zuzusocial.example.com",
        youtubeClientId = "test-youtube-client-id",
        facebookAppId = "test-facebook-app-id",
        tiktokClientKey = "test-tiktok-client-key",
      ),
    )

    repo.connectPlatform(SocialPlatform.YouTube)
    val launch = repo.launchRequest.value
    assertNotNull(launch)
    assertEquals(SocialPlatform.YouTube, launch!!.platform)
    assertTrue(launch.authorizationUrl.startsWith("https://accounts.google.com/o/oauth2/v2/auth"))
    assertTrue(launch.authorizationUrl.contains("code_challenge_method=S256"))

    // Reject mismatched state token
    val rejected = repo.onAuthorizationVerified(
      platform = SocialPlatform.YouTube,
      stateToken = "wrong-state",
      verifiedAccount = ConnectedSocialAccount(
        accountId = "yt-1",
        platform = SocialPlatform.YouTube,
        displayName = "Creator Channel",
        handle = "@creator",
        grantedScopes = listOf("youtube.readonly"),
        connectedAtLabel = "Just now",
      ),
    )
    assertFalse(rejected)
    assertEquals(
      PlatformConnectionPhase.Error,
      repo.platformStates.value.first { it.platform == SocialPlatform.YouTube }.phase,
    )

    // Re-initiate and verify with matching state token
    repo.consumeLaunchRequest()
    repo.connectPlatform(SocialPlatform.YouTube)
    val secondLaunch = repo.launchRequest.value!!
    val accepted = repo.onAuthorizationVerified(
      platform = SocialPlatform.YouTube,
      stateToken = secondLaunch.stateToken,
      verifiedAccount = ConnectedSocialAccount(
        accountId = "yt-1",
        platform = SocialPlatform.YouTube,
        displayName = "Creator Channel",
        handle = "@creator",
        grantedScopes = listOf("youtube.readonly"),
        connectedAtLabel = "Just now",
      ),
    )
    assertTrue(accepted)

    val connectedState = repo.platformStates.value.first { it.platform == SocialPlatform.YouTube }
    assertEquals(PlatformConnectionPhase.Connected, connectedState.phase)
    assertEquals(1, connectedState.connectedAccounts.size)

    // Disconnect account
    repo.disconnectAccount(SocialPlatform.YouTube, "yt-1")
    val disconnectedState = repo.platformStates.value.first { it.platform == SocialPlatform.YouTube }
    assertEquals(PlatformConnectionPhase.NotConnected, disconnectedState.phase)
    assertTrue(disconnectedState.connectedAccounts.isEmpty())
  }
}
