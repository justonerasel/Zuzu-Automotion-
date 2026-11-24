package com.example.data.publishing

import com.example.model.ApprovalContentItem
import com.example.model.ApprovalStatus
import com.example.model.ConnectedSocialAccount
import com.example.model.PlatformPostCustomization
import com.example.model.PublishDispatchMode
import com.example.model.PublishingAuthErrorCode
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.model.TikTokPublishMode
import com.example.model.YouTubePrivacyStatus

sealed interface PublishingValidationResult {
  data object Valid : PublishingValidationResult
  data class Blocked(
    val code: PublishingAuthErrorCode,
    val reason: String,
  ) : PublishingValidationResult
}

/**
 * Mandatory security gate enforced before any job enters Publishing Now or Schedule dispatch.
 * Never permits unapproved, pending, rejected, permission-unverified, or unauthenticated posts.
 */
object PublishingSecurityValidator {

  fun validatePreconditions(
    item: ApprovalContentItem?,
    platformState: SocialPlatformAccountState?,
    account: ConnectedSocialAccount?,
    customization: PlatformPostCustomization,
    mode: PublishDispatchMode,
    scheduledAtMillis: Long?,
    nowMillis: Long,
  ): PublishingValidationResult {
    if (item == null) {
      return PublishingValidationResult.Blocked(
        code = PublishingAuthErrorCode.ApprovalBlocked,
        reason = "Select an explicitly approved video or motion post before publishing.",
      )
    }

    if (item.status != ApprovalStatus.Approved) {
      return PublishingValidationResult.Blocked(
        code = PublishingAuthErrorCode.ApprovalBlocked,
        reason = "Mandatory Security Gate: '${item.title}' is '${item.status.displayLabel}'. Only explicitly Approved content can be published.",
      )
    }

    val permissionError = item.permissionBlockingReason()
    if (permissionError != null) {
      return PublishingValidationResult.Blocked(
        code = PublishingAuthErrorCode.ApprovalBlocked,
        reason = "Permission Validation Failed: $permissionError",
      )
    }

    if (mode == PublishDispatchMode.Draft) {
      return PublishingValidationResult.Valid
    }

    if (platformState == null || !platformState.isConnected || account == null) {
      return PublishingValidationResult.Blocked(
        code = PublishingAuthErrorCode.AccountDisconnected,
        reason = "${customization.platform.displayName} has no authorized OAuth account connected. Connect an official account in Social Accounts first.",
      )
    }

    val scopeCheck = validateScopesAndCustomization(
      platform = customization.platform,
      account = account,
      customization = customization,
      mode = mode,
      scheduledAtMillis = scheduledAtMillis,
      nowMillis = nowMillis,
    )
    if (scopeCheck != null) return scopeCheck

    return PublishingValidationResult.Valid
  }

  private fun validateScopesAndCustomization(
    platform: SocialPlatform,
    account: ConnectedSocialAccount,
    customization: PlatformPostCustomization,
    mode: PublishDispatchMode,
    scheduledAtMillis: Long?,
    nowMillis: Long,
  ): PublishingValidationResult? {
    if (mode == PublishDispatchMode.Schedule) {
      if (scheduledAtMillis == null || scheduledAtMillis <= nowMillis) {
        return PublishingValidationResult.Blocked(
          code = PublishingAuthErrorCode.ApiRejected,
          reason = "Choose a valid future date and time for server-side scheduled publishing.",
        )
      }
    }

    val scopes = account.grantedScopes.map { it.lowercase() }.toSet()

    return when (platform) {
      SocialPlatform.YouTube -> {
        val hasUpload = scopes.any { it.contains("youtube.upload") || it.contains("youtube.force-ssl") }
        if (!hasUpload) {
          PublishingValidationResult.Blocked(
            code = PublishingAuthErrorCode.InsufficientScopes,
            reason = "YouTube account '${account.handle}' lacks 'youtube.upload' OAuth permission.",
          )
        } else if (customization.title.trim().isEmpty()) {
          PublishingValidationResult.Blocked(
            code = PublishingAuthErrorCode.ApiRejected,
            reason = "YouTube requires a non-empty video title (max 100 characters).",
          )
        } else if (mode == PublishDispatchMode.Schedule &&
          customization.youtubePrivacy != YouTubePrivacyStatus.Private
        ) {
          PublishingValidationResult.Blocked(
            code = PublishingAuthErrorCode.ApiRejected,
            reason = "YouTube Data API requires initial privacy status to be 'Private' when setting a future scheduled publishAt time.",
          )
        } else {
          null
        }
      }

      SocialPlatform.Facebook -> {
        val hasPagesScope = scopes.any { it.contains("pages_manage_posts") || it.contains("publish_video") }
        if (!hasPagesScope) {
          PublishingValidationResult.Blocked(
            code = PublishingAuthErrorCode.InsufficientScopes,
            reason = "Facebook Page '${account.displayName}' lacks 'pages_manage_posts' Graph API permission.",
          )
        } else if (customization.facebookPageId.isBlank() && account.accountId.isBlank()) {
          PublishingValidationResult.Blocked(
            code = PublishingAuthErrorCode.ApiRejected,
            reason = "Facebook Meta Graph API publishes only to authorized Facebook Pages (personal profiles are not supported).",
          )
        } else {
          null
        }
      }

      SocialPlatform.TikTok -> {
        val requiredScope = when (customization.tiktokMode) {
          TikTokPublishMode.DirectPost -> "video.publish"
          TikTokPublishMode.InboxDraft -> "video.upload"
        }
        val hasScope = scopes.any { it.contains(requiredScope) || it.contains("video.publish") }
        if (!hasScope) {
          PublishingValidationResult.Blocked(
            code = PublishingAuthErrorCode.InsufficientScopes,
            reason = "TikTok account '${account.handle}' lacks '$requiredScope' scope for ${customization.tiktokMode.displayLabel}.",
          )
        } else {
          null
        }
      }
    }
  }
}
