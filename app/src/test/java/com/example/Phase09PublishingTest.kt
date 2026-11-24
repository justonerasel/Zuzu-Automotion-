package com.example

import com.example.data.cloud.CloudAuthSession
import com.example.data.cloud.CloudBackendConfig
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.oauth.OAuthEnvironmentConfig
import com.example.data.oauth.SocialAccountsRepository
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.publishing.DefaultPublishingBackendGateway
import com.example.data.publishing.InMemoryPublishingRepository
import com.example.data.publishing.PublishBackendJobRequest
import com.example.data.publishing.PublishBackendJobResponse
import com.example.data.publishing.PublishingBackendConfig
import com.example.data.publishing.PublishingRetrofitApi
import com.example.data.publishing.PublishingSecurityValidator
import com.example.data.publishing.PublishingValidationResult
import com.example.data.publishing.TokenRefreshResponse
import com.example.model.AppDestination
import com.example.model.ApprovalContentItem
import com.example.model.ApprovalSourceKind
import com.example.model.ApprovalStatus
import com.example.model.ConnectedSocialAccount
import com.example.model.ContentOrigin
import com.example.model.PermissionStatus
import com.example.model.PrimaryTab
import com.example.model.PublishDispatchMode
import com.example.model.PublishingAuthErrorCode
import com.example.model.PublishingHistoryStatus
import com.example.model.SocialPlatform
import com.example.model.TikTokPublishMode
import com.example.model.YouTubePrivacyStatus
import com.example.viewmodel.PublishingCenterViewModel
import com.example.viewmodel.ZuzuAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class Phase09PublishingTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `mandatory approval security blocks pending rejected draft and unverified permission items`() = runTest {
    val nowMs = 1_750_000_000_000L
    val socialRepo = configuredSocialRepo()
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.YouTube,
      accountId = "yt_main",
      displayName = "Zuzu Creator",
      handle = "@zuzu",
      scopes = listOf("youtube.readonly", "youtube.upload"),
    )

    val phase78Repo = InMemoryPhase78Repository()
    val pendingItem = makeApprovalItem(id = "apr_pending", status = ApprovalStatus.PendingReview)
    val rejectedItem = makeApprovalItem(id = "apr_rejected", status = ApprovalStatus.Rejected)
    val draftItem = makeApprovalItem(id = "apr_draft", status = ApprovalStatus.Draft)
    val unverifiedPermItem = makeApprovalItem(
      id = "apr_unverified",
      status = ApprovalStatus.Approved,
      origin = ContentOrigin.YouTube,
      permissionStatus = PermissionStatus.PermissionPending,
      approvedForReuse = false,
    )
    phase78Repo.saveApprovalItem(pendingItem)
    phase78Repo.saveApprovalItem(rejectedItem)
    phase78Repo.saveApprovalItem(draftItem)
    phase78Repo.saveApprovalItem(unverifiedPermItem)

    val ytState = socialRepo.platformStates.value.first { it.platform == SocialPlatform.YouTube }
    val ytAccount = ytState.connectedAccounts.first()
    val ytCustom = com.example.model.PlatformPostCustomization(
      platform = SocialPlatform.YouTube,
      title = "Launch Video",
      youtubePrivacy = YouTubePrivacyStatus.Public,
    )

    listOf(pendingItem, rejectedItem, draftItem, unverifiedPermItem).forEach { blockedItem ->
      val res = PublishingSecurityValidator.validatePreconditions(
        item = blockedItem,
        platformState = ytState,
        account = ytAccount,
        customization = ytCustom,
        mode = PublishDispatchMode.PublishNow,
        scheduledAtMillis = null,
        nowMillis = nowMs,
      )
      assertTrue(res is PublishingValidationResult.Blocked)
      assertEquals(PublishingAuthErrorCode.ApprovalBlocked, (res as PublishingValidationResult.Blocked).code)
    }

    val pubRepo = InMemoryPublishingRepository()
    val vm = PublishingCenterViewModel(
      phase78Repository = phase78Repo,
      socialAccountsRepository = socialRepo,
      publishingRepository = pubRepo,
      publishingBackend = DefaultPublishingBackendGateway(),
      clockMs = { nowMs },
    )
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    // None of the 4 blocked items appear in approvedItems
    assertTrue(vm.uiState.value.approvedItems.isEmpty())
    assertEquals(4, vm.uiState.value.nonApprovedBlockedCount)

    // Trying executePublishOrSchedule with a PendingReview id override is strictly blocked
    vm.executePublishOrSchedule(approvalIdOverride = "apr_pending")
    assertTrue(pubRepo.publishingJobs.first().isEmpty())
    assertNotNull(vm.uiState.value.errorMessage)

    collectJob.cancel()
  }

  @Test
  fun `account scope and platform rules validate YouTube scheduled privacy Facebook Pages and TikTok modes`() = runTest {
    val nowMs = 1_750_000_000_000L
    val futureTs = nowMs + 3_600_000L
    val socialRepo = configuredSocialRepo()

    // Connect YouTube with youtube.upload, Facebook with pages_manage_posts, TikTok with video.upload (InboxDraft only)
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.YouTube,
      accountId = "yt_1",
      displayName = "YT Channel",
      handle = "@yt",
      scopes = listOf("youtube.upload"),
    )
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.Facebook,
      accountId = "fb_page_1",
      displayName = "Zuzu Brand Page",
      handle = "Zuzu Page",
      scopes = listOf("pages_show_list", "pages_manage_posts"),
    )
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.TikTok,
      accountId = "tt_1",
      displayName = "TT Creator",
      handle = "@tt",
      scopes = listOf("user.info.basic", "video.upload"),
    )

    val approvedItem = makeApprovalItem(id = "apr_ok", status = ApprovalStatus.Approved)
    val states = socialRepo.platformStates.value

    // 1. Disconnected check
    val disconnected = PublishingSecurityValidator.validatePreconditions(
      item = approvedItem,
      platformState = states.first { it.platform == SocialPlatform.YouTube }.copy(connectedAccounts = emptyList()),
      account = null,
      customization = com.example.model.PlatformPostCustomization(SocialPlatform.YouTube, title = "Hi"),
      mode = PublishDispatchMode.PublishNow,
      scheduledAtMillis = null,
      nowMillis = nowMs,
    )
    assertEquals(PublishingAuthErrorCode.AccountDisconnected, (disconnected as PublishingValidationResult.Blocked).code)

    // 2. YouTube scheduled publishAt requires initial privacy = Private
    val ytState = states.first { it.platform == SocialPlatform.YouTube }
    val ytSchedulePublic = PublishingSecurityValidator.validatePreconditions(
      item = approvedItem,
      platformState = ytState,
      account = ytState.connectedAccounts.first(),
      customization = com.example.model.PlatformPostCustomization(
        platform = SocialPlatform.YouTube,
        title = "Scheduled Launch",
        youtubePrivacy = YouTubePrivacyStatus.Public,
      ),
      mode = PublishDispatchMode.Schedule,
      scheduledAtMillis = futureTs,
      nowMillis = nowMs,
    )
    assertTrue(ytSchedulePublic is PublishingValidationResult.Blocked)
    assertTrue((ytSchedulePublic as PublishingValidationResult.Blocked).reason.contains("Private"))

    val ytSchedulePrivate = PublishingSecurityValidator.validatePreconditions(
      item = approvedItem,
      platformState = ytState,
      account = ytState.connectedAccounts.first(),
      customization = com.example.model.PlatformPostCustomization(
        platform = SocialPlatform.YouTube,
        title = "Scheduled Launch",
        youtubePrivacy = YouTubePrivacyStatus.Private,
      ),
      mode = PublishDispatchMode.Schedule,
      scheduledAtMillis = futureTs,
      nowMillis = nowMs,
    )
    assertTrue(ytSchedulePrivate is PublishingValidationResult.Valid)

    // 3. Facebook Page missing pages_manage_posts scope is blocked
    val fbState = states.first { it.platform == SocialPlatform.Facebook }
    val fbMissingScope = PublishingSecurityValidator.validatePreconditions(
      item = approvedItem,
      platformState = fbState,
      account = fbState.connectedAccounts.first().copy(grantedScopes = listOf("public_profile")),
      customization = com.example.model.PlatformPostCustomization(SocialPlatform.Facebook, caption = "Post"),
      mode = PublishDispatchMode.PublishNow,
      scheduledAtMillis = null,
      nowMillis = nowMs,
    )
    assertEquals(PublishingAuthErrorCode.InsufficientScopes, (fbMissingScope as PublishingValidationResult.Blocked).code)

    // 4. TikTok DirectPost requires video.publish, whereas InboxDraft succeeds with video.upload
    val ttState = states.first { it.platform == SocialPlatform.TikTok }
    val ttDirectBlocked = PublishingSecurityValidator.validatePreconditions(
      item = approvedItem,
      platformState = ttState,
      account = ttState.connectedAccounts.first(),
      customization = com.example.model.PlatformPostCustomization(
        platform = SocialPlatform.TikTok,
        caption = "Hello TikTok",
        tiktokMode = TikTokPublishMode.DirectPost,
      ),
      mode = PublishDispatchMode.PublishNow,
      scheduledAtMillis = null,
      nowMillis = nowMs,
    )
    assertEquals(PublishingAuthErrorCode.InsufficientScopes, (ttDirectBlocked as PublishingValidationResult.Blocked).code)

    val ttInboxAllowed = PublishingSecurityValidator.validatePreconditions(
      item = approvedItem,
      platformState = ttState,
      account = ttState.connectedAccounts.first(),
      customization = com.example.model.PlatformPostCustomization(
        platform = SocialPlatform.TikTok,
        caption = "Hello TikTok",
        tiktokMode = TikTokPublishMode.InboxDraft,
      ),
      mode = PublishDispatchMode.PublishNow,
      scheduledAtMillis = null,
      nowMillis = nowMs,
    )
    assertTrue(ttInboxAllowed is PublishingValidationResult.Valid)
  }

  @Test
  fun `unconfigured backend reports SetupRequired honestly and never fakes published or scheduled status`() = runTest {
    val nowMs = 1_750_000_000_000L
    val socialRepo = configuredSocialRepo()
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.YouTube,
      accountId = "yt_1",
      displayName = "YT Creator",
      handle = "@creator",
      scopes = listOf("youtube.upload"),
    )

    val phase78Repo = InMemoryPhase78Repository()
    phase78Repo.saveApprovalItem(makeApprovalItem(id = "apr_1", status = ApprovalStatus.Approved))
    val pubRepo = InMemoryPublishingRepository()
    val vm = PublishingCenterViewModel(
      phase78Repository = phase78Repo,
      socialAccountsRepository = socialRepo,
      publishingRepository = pubRepo,
      publishingBackend = DefaultPublishingBackendGateway(), // Unconfigured
      clockMs = { nowMs },
    )
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    vm.selectApprovedItem("apr_1")
    vm.executePublishOrSchedule()

    val jobs = pubRepo.publishingJobs.first()
    assertEquals(1, jobs.size)
    assertEquals(PublishingHistoryStatus.Failed, jobs.first().status)
    assertEquals(PublishingAuthErrorCode.SetupRequired, jobs.first().authErrorCode)
    assertNull(jobs.first().remotePlatformPostId)
    assertTrue(vm.uiState.value.errorMessage?.contains("Setup Required") == true)

    collectJob.cancel()
  }

  @Test
  fun `configured backend handles publish now token refresh retry scheduling edit cancel drafts and safe retry`() = runTest {
    var nowMs = 1_750_000_000_000L
    val socialRepo = configuredSocialRepo()
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.YouTube,
      accountId = "yt_1",
      displayName = "Zuzu YT",
      handle = "@zuzuyt",
      scopes = listOf("youtube.readonly", "youtube.upload"),
    )
    connectVerifiedAccount(
      repo = socialRepo,
      platform = SocialPlatform.Facebook,
      accountId = "fb_page_99",
      displayName = "Zuzu Official Page",
      handle = "Zuzu Page",
      scopes = listOf("pages_show_list", "pages_read_engagement", "pages_manage_posts"),
    )

    val phase78Repo = InMemoryPhase78Repository()
    phase78Repo.saveApprovalItem(makeApprovalItem(id = "apr_vid_1", status = ApprovalStatus.Approved))
    val pubRepo = InMemoryPublishingRepository()
    val fakeApi = FakePublishingRetrofitApi()
    val cloudGateway = DefaultCloudBackendGateway(
      config = CloudBackendConfig(
        firebaseProjectId = "zuzu-prod",
        cloudStorageBucket = "zuzu-prod.appspot.com",
        cloudAutomationBaseUrl = "https://api.zuzu.example.com/",
      ),
    )
    cloudGateway.signInPromptResult(
      CloudAuthSession(uid = "uid_1", email = "creator@example.com", displayName = "Creator", idToken = "jwt-123"),
    )

    val backendGateway = DefaultPublishingBackendGateway(
      config = PublishingBackendConfig(
        backendBaseUrl = "https://publish.zuzu.example.com/",
        youtubeApiCredentialsConfigured = true,
        youtubeAppApproved = true,
        facebookMetaAppConfigured = true,
        facebookPagesReviewApproved = true,
        tiktokClientConfigured = true,
        tiktokDirectPostApproved = false,
      ),
      apiProvider = { fakeApi },
    )

    val vm = PublishingCenterViewModel(
      phase78Repository = phase78Repo,
      socialAccountsRepository = socialRepo,
      publishingRepository = pubRepo,
      publishingBackend = backendGateway,
      cloudGateway = cloudGateway,
      clockMs = { nowMs },
    )
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    vm.selectApprovedItem("apr_vid_1")

    // 1. Save Draft locally
    vm.savePublishingDraft()
    val draftJobs = pubRepo.publishingJobs.first()
    assertEquals(1, draftJobs.size)
    assertEquals(PublishingHistoryStatus.Draft, draftJobs.first().status)

    // 2. Publish Now with simulated initial TOKEN_EXPIRED -> backend token refresh -> retry succeeds
    fakeApi.failFirstDispatchWithTokenExpired = true
    vm.setDispatchMode(PublishDispatchMode.PublishNow)
    vm.executePublishOrSchedule()
    assertEquals(1, fakeApi.tokenRefreshCalls)
    val publishedJob = pubRepo.publishingJobs.first().first { it.status == PublishingHistoryStatus.Published }
    assertEquals("yt_remote_101", publishedJob.remotePlatformPostId)
    assertEquals(PublishingAuthErrorCode.None, publishedJob.authErrorCode)

    // 3. Duplicate immediate publish within same bucket blocked
    vm.executePublishOrSchedule()
    assertTrue(vm.uiState.value.errorMessage?.contains("Duplicate post blocked") == true)

    // 4. Schedule Facebook Page post, edit timestamp/caption, then cancel
    nowMs += 300_000L
    vm.togglePlatform(SocialPlatform.YouTube) // Deselect YT
    vm.togglePlatform(SocialPlatform.Facebook) // Select FB Pages
    vm.setDispatchMode(PublishDispatchMode.Schedule)
    val schedTs = nowMs + 3_600_000L
    vm.setScheduledTimestamp(schedTs)
    vm.executePublishOrSchedule()

    val scheduledJob = pubRepo.publishingJobs.first().first { it.status == PublishingHistoryStatus.Scheduled }
    assertEquals(SocialPlatform.Facebook, scheduledJob.platform)
    assertEquals("fb_page_99", scheduledJob.customization.facebookPageId)

    val editedTs = schedTs + 1_800_000L
    vm.editScheduledPublishJob(scheduledJob.id, "Updated Title", "Updated Page Caption", editedTs)
    val editedJob = pubRepo.getPublishingJob(scheduledJob.id)!!
    assertEquals("Updated Title", editedJob.customization.title)
    assertEquals(editedTs, editedJob.scheduledAtMillis)

    vm.cancelScheduledPublishJob(scheduledJob.id)
    assertEquals(PublishingHistoryStatus.Cancelled, pubRepo.getPublishingJob(scheduledJob.id)!!.status)

    // 5. Rate-limited failure -> Safe Retry succeeds and preserves idempotency, respects maxRetries
    nowMs += 300_000L
    fakeApi.returnRateLimitOnDispatch = true
    vm.setDispatchMode(PublishDispatchMode.PublishNow)
    vm.executePublishOrSchedule()

    val failedJob = pubRepo.publishingJobs.first().first { it.status == PublishingHistoryStatus.Failed }
    assertEquals(PublishingAuthErrorCode.RateLimited, failedJob.authErrorCode)
    assertEquals(1, failedJob.attemptCount)

    fakeApi.returnRateLimitOnDispatch = false
    vm.retryFailedPublishJob(failedJob.id)
    val retriedSuccess = pubRepo.getPublishingJob(failedJob.id)!!
    assertEquals(PublishingHistoryStatus.Published, retriedSuccess.status)
    assertEquals(2, retriedSuccess.attemptCount)

    // Max retries guard when attemptCount == maxRetries
    val exhaustedJob = retriedSuccess.copy(status = PublishingHistoryStatus.Failed, attemptCount = 3, maxRetries = 3)
    pubRepo.savePublishingJob(exhaustedJob)
    vm.retryFailedPublishJob(exhaustedJob.id)
    assertTrue(vm.uiState.value.errorMessage?.contains("Maximum safe retry attempts") == true)

    collectJob.cancel()
  }

  @Test
  fun `main app navigation routes cleanly to PublishingCenter and preserves returnTab`() = runTest {
    val appVm = ZuzuAppViewModel()

    appVm.openPublishingCenter(
      fromTab = PrimaryTab.Projects,
      initialApprovalId = "apr_99",
      initialSectionName = "History",
    )

    val dest = appVm.uiState.value.destination
    assertTrue(dest is AppDestination.PublishingCenter)
    val pubDest = dest as AppDestination.PublishingCenter
    assertEquals(PrimaryTab.Projects, pubDest.returnTab)
    assertEquals("apr_99", pubDest.initialApprovalId)
    assertEquals("History", pubDest.initialSectionName)
    assertEquals(PrimaryTab.Projects, appVm.uiState.value.activeTab)

    appVm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Projects), appVm.uiState.value.destination)
  }

  private fun configuredSocialRepo(): SocialAccountsRepository = SocialAccountsRepository(
    config = OAuthEnvironmentConfig(
      backendBaseUrl = "https://oauth.zuzu.example.com",
      youtubeClientId = "yt-client",
      facebookAppId = "fb-app",
      tiktokClientKey = "tt-key",
    ),
  )

  private suspend fun connectVerifiedAccount(
    repo: SocialAccountsRepository,
    platform: SocialPlatform,
    accountId: String,
    displayName: String,
    handle: String,
    scopes: List<String>,
  ) {
    repo.connectPlatform(platform)
    val launch = repo.launchRequest.value!!
    repo.consumeLaunchRequest()
    val ok = repo.onAuthorizationVerified(
      platform = platform,
      stateToken = launch.stateToken,
      verifiedAccount = ConnectedSocialAccount(
        accountId = accountId,
        platform = platform,
        displayName = displayName,
        handle = handle,
        grantedScopes = scopes,
        connectedAtLabel = "Just now",
      ),
    )
    assertTrue(ok)
  }

  private fun makeApprovalItem(
    id: String,
    status: ApprovalStatus,
    origin: ContentOrigin = ContentOrigin.MyUploads,
    permissionStatus: PermissionStatus = PermissionStatus.OriginalContent,
    approvedForReuse: Boolean = true,
  ) = ApprovalContentItem(
    id = id,
    sourceKind = ApprovalSourceKind.VideoExport,
    sourceId = "exp_$id",
    title = "Creator Video $id",
    description = "Official launch preview",
    hashtagsCsv = "#ZuzuAI #Launch",
    targetPlatforms = listOf(SocialPlatform.YouTube),
    permissionOrigin = origin,
    permissionStatus = permissionStatus,
    permissionOwner = "Creator",
    permissionEvidence = "Original project export",
    permissionApprovedForReuse = approvedForReuse,
    status = status,
  )

  private class FakePublishingRetrofitApi : PublishingRetrofitApi {
    var failFirstDispatchWithTokenExpired = false
    var returnRateLimitOnDispatch = false
    var tokenRefreshCalls = 0
    private var dispatchAttempts = 0

    override suspend fun dispatchPublishNow(
      bearerJwt: String,
      request: PublishBackendJobRequest,
    ): Response<PublishBackendJobResponse> {
      dispatchAttempts += 1
      if (failFirstDispatchWithTokenExpired) {
        failFirstDispatchWithTokenExpired = false
        return Response.success(
          PublishBackendJobResponse(
            jobId = request.jobId,
            backendJobId = "srv_tmp",
            status = "FAILED",
            errorCode = "TOKEN_EXPIRED",
            errorMessage = "OAuth access token expired",
          ),
        )
      }
      if (returnRateLimitOnDispatch) {
        return Response.error(
          429,
          """{"error":"RATE_LIMITED"}""".toResponseBody("application/json".toMediaType()),
        )
      }
      return Response.success(
        PublishBackendJobResponse(
          jobId = request.jobId,
          backendJobId = "srv_pub_$dispatchAttempts",
          status = "PUBLISHED",
          remotePlatformPostId = if (request.platform == "youtube") "yt_remote_101" else "fb_remote_202",
          remotePostUrl = "https://example.com/post/$dispatchAttempts",
          detailMessage = "Confirmed by official API",
        ),
      )
    }

    override suspend fun schedulePublishJob(
      bearerJwt: String,
      request: PublishBackendJobRequest,
    ): Response<PublishBackendJobResponse> = Response.success(
      PublishBackendJobResponse(
        jobId = request.jobId,
        backendJobId = "srv_sched_1",
        status = "SCHEDULED",
        detailMessage = "Scheduled on HTTPS backend queue",
      ),
    )

    override suspend fun updateScheduledPublishJob(
      bearerJwt: String,
      jobId: String,
      request: PublishBackendJobRequest,
    ): Response<PublishBackendJobResponse> = Response.success(
      PublishBackendJobResponse(
        jobId = jobId,
        backendJobId = "srv_sched_1_updated",
        status = "SCHEDULED",
        detailMessage = "Schedule updated cleanly",
      ),
    )

    override suspend fun cancelScheduledPublishJob(
      bearerJwt: String,
      jobId: String,
    ): Response<Unit> = Response.success(Unit)

    override suspend fun retryPublishJob(
      bearerJwt: String,
      jobId: String,
    ): Response<PublishBackendJobResponse> = Response.success(
      PublishBackendJobResponse(
        jobId = jobId,
        backendJobId = "srv_retry_ok",
        status = "PUBLISHED",
        remotePlatformPostId = "fb_remote_retry_1",
        detailMessage = "Retry published cleanly",
      ),
    )

    override suspend fun refreshPlatformToken(
      bearerJwt: String,
      platform: String,
      accountPayload: Map<String, String>,
    ): Response<TokenRefreshResponse> {
      tokenRefreshCalls += 1
      return Response.success(
        TokenRefreshResponse(
          platform = platform,
          accountId = accountPayload["accountId"].orEmpty(),
          refreshed = true,
          expiresAtMillis = System.currentTimeMillis() + 3_600_000L,
          statusMessage = "Refreshed cleanly on server",
        ),
      )
    }

    override suspend fun getPublishJobStatus(
      bearerJwt: String,
      jobId: String,
    ): Response<PublishBackendJobResponse> = Response.success(
      PublishBackendJobResponse(
        jobId = jobId,
        backendJobId = "srv_status",
        status = "PUBLISHED",
      ),
    )
  }
}
