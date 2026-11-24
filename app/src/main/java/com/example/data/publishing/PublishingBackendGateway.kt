package com.example.data.publishing

import com.example.data.cloud.CloudAuthSession
import com.example.data.oauth.OAuthUrlValidator
import com.example.model.CloudOperationResult
import com.example.model.PlatformPostCustomization
import com.example.model.PlatformPublishingCapability
import com.example.model.PublishingAuthErrorCode
import com.example.model.PublishingBackendStatusReport
import com.example.model.PublishingHistoryStatus
import com.example.model.PublishingJobRecord
import com.example.model.SocialPlatform
import com.example.model.TikTokPublishMode
import com.example.security.SecuritySanitizer
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Non-secret backend deployment configuration for Official Social Media Publishing.
 * OAuth client secrets, Meta app secrets, TikTok client secrets, access tokens, and refresh
 * tokens stay strictly on the HTTPS backend server.
 */
data class PublishingBackendConfig(
  val backendBaseUrl: String = "",
  val youtubeApiCredentialsConfigured: Boolean = false,
  val youtubeAppApproved: Boolean = false,
  val facebookMetaAppConfigured: Boolean = false,
  val facebookPagesReviewApproved: Boolean = false,
  val tiktokClientConfigured: Boolean = false,
  val tiktokDirectPostApproved: Boolean = false,
) {
  val hasSecureEndpoint: Boolean
    get() = OAuthUrlValidator.isValidSecureBackendUrl(backendBaseUrl)

  fun buildStatusReport(): PublishingBackendStatusReport {
    val ytChecklist = buildList {
      if (!hasSecureEndpoint) add("Deploy HTTPS backend publishing worker & token vault.")
      if (!youtubeApiCredentialsConfigured) add("Configure Google Cloud YouTube Data API v3 credentials on backend.")
      if (!youtubeAppApproved) add("Verify YouTube API quota & OAuth consent verification (youtube.upload scope).")
    }
    val fbChecklist = buildList {
      if (!hasSecureEndpoint) add("Deploy HTTPS backend publishing worker & token vault.")
      if (!facebookMetaAppConfigured) add("Configure Meta App ID & server-side App Secret on backend.")
      if (!facebookPagesReviewApproved) add("Complete Meta App Review for pages_manage_posts & pages_read_engagement (Pages only).")
    }
    val ttChecklist = buildList {
      if (!hasSecureEndpoint) add("Deploy HTTPS backend publishing worker & token vault.")
      if (!tiktokClientConfigured) add("Configure TikTok Client Key & server-side Client Secret on backend.")
      if (!tiktokDirectPostApproved) add("Complete TikTok Content Posting API audit for Direct Post (Inbox Draft requires user finalization in TikTok).")
    }

    return PublishingBackendStatusReport(
      backendEndpointConfigured = hasSecureEndpoint,
      platformCapabilities = listOf(
        PlatformPublishingCapability(
          platform = SocialPlatform.YouTube,
          apiName = "YouTube Data API v3 (videos.insert / thumbnails.set)",
          officialRestrictionsNote = "Supports title, description, tags, privacy, custom thumbnail & scheduled publishAt (privacy must start as Private).",
          requiredScopes = listOf("youtube.readonly", "youtube.upload"),
          credentialsReady = hasSecureEndpoint && youtubeApiCredentialsConfigured,
          apiReviewApproved = youtubeAppApproved,
          missingSetupChecklist = ytChecklist,
        ),
        PlatformPublishingCapability(
          platform = SocialPlatform.Facebook,
          apiName = "Meta Graph API v20.0 (Page Videos & Feed)",
          officialRestrictionsNote = "Authorized Facebook Pages ONLY. Personal profile publishing is blocked by Meta policy.",
          requiredScopes = listOf("pages_show_list", "pages_read_engagement", "pages_manage_posts"),
          credentialsReady = hasSecureEndpoint && facebookMetaAppConfigured,
          apiReviewApproved = facebookPagesReviewApproved,
          missingSetupChecklist = fbChecklist,
        ),
        PlatformPublishingCapability(
          platform = SocialPlatform.TikTok,
          apiName = "TikTok Content Posting API v2",
          officialRestrictionsNote = "Direct Post requires audited app + video.publish; Inbox Draft requires user confirmation inside TikTok app.",
          requiredScopes = listOf("user.info.basic", "video.list", "video.publish"),
          credentialsReady = hasSecureEndpoint && tiktokClientConfigured,
          apiReviewApproved = tiktokDirectPostApproved,
          missingSetupChecklist = ttChecklist,
        ),
      ),
      lastCheckedAtMillis = System.currentTimeMillis(),
    )
  }

  fun missingForPlatform(platform: SocialPlatform): List<String> =
    buildStatusReport().platformCapabilities
      .firstOrNull { it.platform == platform }
      ?.missingSetupChecklist
      ?: listOf("Configure HTTPS publishing backend.")
}

@JsonClass(generateAdapter = true)
data class PublishBackendJobRequest(
  val jobId: String,
  val approvalItemId: String,
  val idempotencyKey: String,
  val platform: String,
  val accountId: String,
  val mediaUriOrStorageKey: String,
  val title: String,
  val caption: String,
  val description: String,
  val hashtagsCsv: String,
  val youtubePrivacy: String,
  val youtubeThumbnailUri: String?,
  val facebookPageId: String,
  val tiktokMode: String,
  val tiktokPrivacyLevel: String,
  val tiktokAllowComments: Boolean,
  val tiktokAllowDuet: Boolean,
  val tiktokAllowStitch: Boolean,
  val scheduledAtMillis: Long?,
  val permissionVerified: Boolean,
)

@JsonClass(generateAdapter = true)
data class PublishBackendJobResponse(
  val jobId: String,
  val backendJobId: String,
  val status: String, // PUBLISHED, SCHEDULED, PROCESSING, FAILED, INBOX_DRAFT_READY
  val remotePlatformPostId: String? = null,
  val remotePostUrl: String? = null,
  val detailMessage: String = "",
  val errorCode: String? = null, // TOKEN_EXPIRED, INSUFFICIENT_SCOPES, RATE_LIMITED, API_REJECTED
  val errorMessage: String? = null,
)

@JsonClass(generateAdapter = true)
data class TokenRefreshResponse(
  val platform: String,
  val accountId: String,
  val refreshed: Boolean,
  val expiresAtMillis: Long?,
  val statusMessage: String,
)

/**
 * Retrofit contract for server-side social publishing endpoints.
 * Notice zero OAuth client secrets or tokens ever cross this client boundary.
 */
interface PublishingRetrofitApi {
  @POST("v1/publishing/jobs/dispatch")
  suspend fun dispatchPublishNow(
    @Header("Authorization") bearerJwt: String,
    @Body request: PublishBackendJobRequest,
  ): Response<PublishBackendJobResponse>

  @POST("v1/publishing/jobs/schedule")
  suspend fun schedulePublishJob(
    @Header("Authorization") bearerJwt: String,
    @Body request: PublishBackendJobRequest,
  ): Response<PublishBackendJobResponse>

  @PATCH("v1/publishing/jobs/{jobId}")
  suspend fun updateScheduledPublishJob(
    @Header("Authorization") bearerJwt: String,
    @Path("jobId") jobId: String,
    @Body request: PublishBackendJobRequest,
  ): Response<PublishBackendJobResponse>

  @DELETE("v1/publishing/jobs/{jobId}")
  suspend fun cancelScheduledPublishJob(
    @Header("Authorization") bearerJwt: String,
    @Path("jobId") jobId: String,
  ): Response<Unit>

  @POST("v1/publishing/jobs/{jobId}/retry")
  suspend fun retryPublishJob(
    @Header("Authorization") bearerJwt: String,
    @Path("jobId") jobId: String,
  ): Response<PublishBackendJobResponse>

  @POST("v1/publishing/oauth/{platform}/refresh")
  suspend fun refreshPlatformToken(
    @Header("Authorization") bearerJwt: String,
    @Path("platform") platform: String,
    @Body accountPayload: Map<String, String>,
  ): Response<TokenRefreshResponse>

  @GET("v1/publishing/jobs/{jobId}")
  suspend fun getPublishJobStatus(
    @Header("Authorization") bearerJwt: String,
    @Path("jobId") jobId: String,
  ): Response<PublishBackendJobResponse>
}

interface PublishingBackendGateway {
  val statusReport: StateFlow<PublishingBackendStatusReport>

  fun refreshBackendReport(): PublishingBackendStatusReport

  suspend fun dispatchPublishNow(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse>

  suspend fun schedulePublishJob(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse>

  suspend fun updateScheduledPublishJob(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse>

  suspend fun cancelScheduledPublishJob(
    session: CloudAuthSession?,
    jobId: String,
    platform: SocialPlatform,
  ): CloudOperationResult<Unit>

  suspend fun retryPublishJob(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse>
}

class DefaultPublishingBackendGateway(
  private val config: PublishingBackendConfig = PublishingBackendConfig(),
  private val apiProvider: ((String) -> PublishingRetrofitApi)? = null,
) : PublishingBackendGateway {

  private val _statusReport = MutableStateFlow(config.buildStatusReport())
  override val statusReport: StateFlow<PublishingBackendStatusReport> = _statusReport.asStateFlow()

  override fun refreshBackendReport(): PublishingBackendStatusReport {
    val updated = config.buildStatusReport()
    _statusReport.value = updated
    return updated
  }

  private fun verifyBackendReady(
    session: CloudAuthSession?,
    platform: SocialPlatform,
    tiktokMode: TikTokPublishMode? = null,
  ): Result<Pair<CloudAuthSession, PublishingRetrofitApi>> {
    val report = refreshBackendReport()
    val cap = report.platformCapabilities.firstOrNull { it.platform == platform }
    val requiresDirectAudit = platform != SocialPlatform.TikTok || tiktokMode == TikTokPublishMode.DirectPost

    if (!config.hasSecureEndpoint || cap == null || !cap.credentialsReady || (requiresDirectAudit && !cap.apiReviewApproved)) {
      return Result.failure(BackendSetupNotReadyException())
    }
    if (session == null || !session.isAuthenticated) {
      return Result.failure(BackendSetupNotReadyException())
    }
    val api = apiProvider?.invoke(config.backendBaseUrl)
      ?: return Result.failure(BackendSetupNotReadyException())
    return Result.success(session to api)
  }

  private fun buildSetupRequiredResult(
    platform: SocialPlatform,
    session: CloudAuthSession?,
  ): CloudOperationResult.SetupRequired {
    val missing = buildList {
      addAll(config.missingForPlatform(platform))
      if (session == null || !session.isAuthenticated) {
        add("Authenticated creator backend session required before invoking publishing jobs.")
      }
    }.distinct()
    return CloudOperationResult.SetupRequired(
      title = "${platform.displayName} Official Publishing Setup Required",
      reason = "Publishing did not run because backend OAuth credentials or platform API approvals are missing. No upload or post was simulated.",
      missingItems = missing,
    )
  }

  override suspend fun dispatchPublishNow(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse> {
    val (auth, api) = verifyBackendReady(session, job.platform, job.customization.tiktokMode).getOrElse {
      return buildSetupRequiredResult(job.platform, session)
    }
    return executeWithTokenRefresh(
      auth = auth,
      api = api,
      platform = job.platform,
      accountId = job.accountId,
    ) {
      api.dispatchPublishNow("Bearer ${auth.idToken}", job.toRequest())
    }
  }

  override suspend fun schedulePublishJob(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse> {
    val (auth, api) = verifyBackendReady(session, job.platform, job.customization.tiktokMode).getOrElse {
      return buildSetupRequiredResult(job.platform, session)
    }
    return executeWithTokenRefresh(
      auth = auth,
      api = api,
      platform = job.platform,
      accountId = job.accountId,
    ) {
      api.schedulePublishJob("Bearer ${auth.idToken}", job.toRequest())
    }
  }

  override suspend fun updateScheduledPublishJob(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse> {
    val (auth, api) = verifyBackendReady(session, job.platform, job.customization.tiktokMode).getOrElse {
      return buildSetupRequiredResult(job.platform, session)
    }
    return executeWithTokenRefresh(
      auth = auth,
      api = api,
      platform = job.platform,
      accountId = job.accountId,
    ) {
      api.updateScheduledPublishJob("Bearer ${auth.idToken}", job.id, job.toRequest())
    }
  }

  override suspend fun cancelScheduledPublishJob(
    session: CloudAuthSession?,
    jobId: String,
    platform: SocialPlatform,
  ): CloudOperationResult<Unit> {
    val (auth, api) = verifyBackendReady(session, platform).getOrElse {
      return buildSetupRequiredResult(platform, session)
    }
    return runCatching {
      val resp = api.cancelScheduledPublishJob("Bearer ${auth.idToken}", jobId)
      if (resp.isSuccessful) {
        CloudOperationResult.Success(Unit)
      } else {
        CloudOperationResult.Failure("Backend refused scheduled job cancellation (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to cancel scheduled publishing job.")
    }
  }

  override suspend fun retryPublishJob(
    session: CloudAuthSession?,
    job: PublishingJobRecord,
  ): CloudOperationResult<PublishBackendJobResponse> {
    val (auth, api) = verifyBackendReady(session, job.platform, job.customization.tiktokMode).getOrElse {
      return buildSetupRequiredResult(job.platform, session)
    }
    return executeWithTokenRefresh(
      auth = auth,
      api = api,
      platform = job.platform,
      accountId = job.accountId,
    ) {
      api.retryPublishJob("Bearer ${auth.idToken}", job.id)
    }
  }

  /**
   * Executes a server-side publishing API call and handles 401/TOKEN_EXPIRED via a backend
   * server-side token refresh call (never accessing refresh tokens on device).
   */
  private suspend fun executeWithTokenRefresh(
    auth: CloudAuthSession,
    api: PublishingRetrofitApi,
    platform: SocialPlatform,
    accountId: String,
    call: suspend () -> Response<PublishBackendJobResponse>,
  ): CloudOperationResult<PublishBackendJobResponse> {
    return runCatching {
      var resp = call()
      var body = resp.body()
      val tokenExpired = resp.code() == 401 || body?.errorCode == "TOKEN_EXPIRED"

      if (tokenExpired) {
        val refreshResp = api.refreshPlatformToken(
          bearerJwt = "Bearer ${auth.idToken}",
          platform = platform.id,
          accountPayload = mapOf("accountId" to accountId),
        )
        if (refreshResp.isSuccessful && refreshResp.body()?.refreshed == true) {
          resp = call()
          body = resp.body()
        } else {
          return@runCatching CloudOperationResult.Failure(
            "${platform.displayName} OAuth authorization expired and could not be refreshed on backend. Re-authorize your account in Social Accounts.",
          )
        }
      }

      if (resp.isSuccessful && body != null && body.status != "FAILED") {
        CloudOperationResult.Success(body)
      } else {
        val reason = when {
          resp.code() == 429 || body?.errorCode == "RATE_LIMITED" ->
            "${platform.displayName} API rate limit reached. Safe retry queued without duplicating posts."
          resp.code() == 403 || body?.errorCode == "INSUFFICIENT_SCOPES" ->
            SecuritySanitizer.redactSensitiveTokens(body?.errorMessage)
              .ifBlank { "${platform.displayName} rejected request due to missing OAuth permissions." }
          else ->
            SecuritySanitizer.redactSensitiveTokens(body?.errorMessage)
              .ifBlank { "${platform.displayName} publishing failed (HTTP ${resp.code()})." }
        }
        CloudOperationResult.Failure(reason)
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(
        SecuritySanitizer.redactSensitiveTokens(err.message)
          .ifBlank { "Network error contacting publishing backend." },
      )
    }
  }

  private fun PublishingJobRecord.toRequest() = PublishBackendJobRequest(
    jobId = id,
    approvalItemId = approvalItemId,
    idempotencyKey = idempotencyKey,
    platform = platform.id,
    accountId = accountId,
    mediaUriOrStorageKey = mediaFilePath ?: mediaUriString.orEmpty(),
    title = SecuritySanitizer.sanitizeUserText(customization.title, 100),
    caption = SecuritySanitizer.sanitizeUserText(customization.caption, 2200),
    description = SecuritySanitizer.sanitizeUserText(customization.description, 5000),
    hashtagsCsv = SecuritySanitizer.sanitizeUserText(customization.hashtagsCsv, 500),
    youtubePrivacy = customization.youtubePrivacy.apiValue,
    youtubeThumbnailUri = customization.youtubeThumbnailUriString,
    facebookPageId = customization.facebookPageId.ifBlank { accountId },
    tiktokMode = customization.tiktokMode.name,
    tiktokPrivacyLevel = customization.tiktokPrivacyLevel.apiValue,
    tiktokAllowComments = customization.tiktokAllowComments,
    tiktokAllowDuet = customization.tiktokAllowDuet,
    tiktokAllowStitch = customization.tiktokAllowStitch,
    scheduledAtMillis = scheduledAtMillis,
    permissionVerified = true,
  )

  private class BackendSetupNotReadyException : Exception()
}

fun String.toPublishingHistoryStatus(): PublishingHistoryStatus = when (uppercase()) {
  "PUBLISHED", "INBOX_DRAFT_READY" -> PublishingHistoryStatus.Published
  "SCHEDULED" -> PublishingHistoryStatus.Scheduled
  "PROCESSING" -> PublishingHistoryStatus.Processing
  "CANCELLED" -> PublishingHistoryStatus.Cancelled
  "DRAFT" -> PublishingHistoryStatus.Draft
  else -> PublishingHistoryStatus.Failed
}

fun String?.toAuthErrorCode(): PublishingAuthErrorCode = when (this?.uppercase()) {
  "TOKEN_EXPIRED" -> PublishingAuthErrorCode.TokenExpired
  "INSUFFICIENT_SCOPES" -> PublishingAuthErrorCode.InsufficientScopes
  "RATE_LIMITED" -> PublishingAuthErrorCode.RateLimited
  "SETUP_REQUIRED" -> PublishingAuthErrorCode.SetupRequired
  "ACCOUNT_DISCONNECTED" -> PublishingAuthErrorCode.AccountDisconnected
  "APPROVAL_BLOCKED" -> PublishingAuthErrorCode.ApprovalBlocked
  else -> PublishingAuthErrorCode.ApiRejected
}
