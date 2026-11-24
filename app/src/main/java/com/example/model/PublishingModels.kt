package com.example.model

import com.squareup.moshi.JsonClass

enum class PublishingHistoryStatus(val displayLabel: String) {
  Published("Published"),
  Scheduled("Scheduled"),
  Processing("Processing"),
  Failed("Failed"),
  Draft("Draft"),
  Cancelled("Cancelled"),
}

enum class PublishDispatchMode(val displayLabel: String) {
  PublishNow("Publish Now"),
  Schedule("Schedule"),
  Draft("Save Draft"),
}

enum class YouTubePrivacyStatus(val apiValue: String, val displayLabel: String) {
  Public("public", "Public"),
  Unlisted("unlisted", "Unlisted"),
  Private("private", "Private"),
}

enum class TikTokPublishMode(val displayLabel: String, val description: String) {
  DirectPost(
    displayLabel = "Direct Post",
    description = "Publishes via TikTok Direct Post API (requires audited app + video.publish scope).",
  ),
  InboxDraft(
    displayLabel = "TikTok Inbox Draft",
    description = "Uploads video to creator's TikTok Inbox; final editing & publishing must be completed inside the TikTok app.",
  ),
}

enum class TikTokPrivacyLevel(val apiValue: String, val displayLabel: String) {
  Public("PUBLIC_TO_EVERYONE", "Everyone"),
  Friends("MUTUAL_FOLLOW_FRIENDS", "Friends"),
  OnlyMe("SELF_ONLY", "Only Me"),
}

enum class PublishingAuthErrorCode(val displayLabel: String) {
  None("None"),
  SetupRequired("Setup Required"),
  TokenExpired("Token Expired"),
  InsufficientScopes("Missing OAuth Scopes"),
  RateLimited("API Rate Limit Reached"),
  AccountDisconnected("Account Disconnected"),
  ApprovalBlocked("Approval/Permission Blocked"),
  ApiRejected("Platform API Rejected"),
}

@JsonClass(generateAdapter = true)
data class PlatformPostCustomization(
  val platform: SocialPlatform,
  val title: String = "",
  val caption: String = "",
  val description: String = "",
  val hashtagsCsv: String = "",
  // YouTube Data API v3 fields
  val youtubePrivacy: YouTubePrivacyStatus = YouTubePrivacyStatus.Private,
  val youtubeThumbnailUriString: String? = null,
  val youtubeCategoryId: String = "22", // People & Blogs
  // Facebook Pages Meta Graph API fields (Pages only, no personal profile)
  val facebookPageId: String = "",
  val facebookPageName: String = "",
  // TikTok Content Posting API v2 fields
  val tiktokMode: TikTokPublishMode = TikTokPublishMode.InboxDraft,
  val tiktokPrivacyLevel: TikTokPrivacyLevel = TikTokPrivacyLevel.OnlyMe,
  val tiktokAllowComments: Boolean = true,
  val tiktokAllowDuet: Boolean = true,
  val tiktokAllowStitch: Boolean = true,
) {
  fun effectiveSummaryTitle(): String = when (platform) {
    SocialPlatform.YouTube -> title.ifBlank { caption }
    SocialPlatform.Facebook -> caption.ifBlank { title }
    SocialPlatform.TikTok -> caption.ifBlank { title }
  }
}

@JsonClass(generateAdapter = true)
data class PublishingJobRecord(
  val id: String,
  val approvalItemId: String,
  val sourceKind: ApprovalSourceKind,
  val postTitle: String,
  val mediaUriString: String? = null,
  val mediaFilePath: String? = null,
  val aspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
  val platform: SocialPlatform,
  val accountId: String,
  val accountDisplayName: String,
  val accountHandle: String,
  val customization: PlatformPostCustomization,
  val dispatchMode: PublishDispatchMode = PublishDispatchMode.PublishNow,
  val scheduledAtMillis: Long? = null,
  val status: PublishingHistoryStatus = PublishingHistoryStatus.Draft,
  val idempotencyKey: String,
  val backendJobId: String? = null,
  val remotePlatformPostId: String? = null,
  val remotePostUrl: String? = null,
  val statusDetail: String = "",
  val errorMessage: String? = null,
  val authErrorCode: PublishingAuthErrorCode = PublishingAuthErrorCode.None,
  val attemptCount: Int = 0,
  val maxRetries: Int = 3,
  val nextRetryAtMillis: Long? = null,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  val isEditableOrCancellableSchedule: Boolean
    get() = status == PublishingHistoryStatus.Scheduled

  val canRetrySafely: Boolean
    get() = status == PublishingHistoryStatus.Failed && attemptCount < maxRetries
}

data class PlatformPublishingCapability(
  val platform: SocialPlatform,
  val apiName: String,
  val officialRestrictionsNote: String,
  val requiredScopes: List<String>,
  val credentialsReady: Boolean,
  val apiReviewApproved: Boolean,
  val missingSetupChecklist: List<String>,
) {
  val statusLabel: String
    get() = if (credentialsReady && apiReviewApproved) "API Ready" else "Setup Required"
}

data class PublishingBackendStatusReport(
  val backendEndpointConfigured: Boolean = false,
  val platformCapabilities: List<PlatformPublishingCapability> = emptyList(),
  val securityComplianceNotes: List<String> = listOf(
    "OAuth client secrets, access tokens, and refresh tokens reside exclusively on the HTTPS backend vault.",
    "Tokens are never persisted in Android code, Room entities, SharedPreferences, logs, or UI.",
    "Facebook publishing targets authorized Facebook Pages only (personal profile posting is disabled).",
  ),
  val lastCheckedAtMillis: Long = System.currentTimeMillis(),
) {
  val anyPlatformApiReady: Boolean
    get() = backendEndpointConfigured && platformCapabilities.any { it.credentialsReady && it.apiReviewApproved }
}
