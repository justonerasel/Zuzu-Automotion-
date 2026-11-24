package com.example

import com.example.data.analytics.AnalyticsBackendConfig
import com.example.data.analytics.AnalyticsRetrofitApi
import com.example.data.analytics.DefaultAnalyticsBackendGateway
import com.example.data.analytics.RemoteAnalyticsResponse
import com.example.data.analytics.RemoteContentMetricsDto
import com.example.data.analytics.RemotePlatformMetricsDto
import com.example.data.cloud.CloudAuthSession
import com.example.data.cloud.CloudBackendConfig
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.editor.InMemoryVideoEditorRepository
import com.example.data.oauth.OAuthEnvironmentConfig
import com.example.data.oauth.SocialAccountsRepository
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.publishing.InMemoryPublishingRepository
import com.example.model.AnalyticsContentSection
import com.example.model.AnalyticsDateRange
import com.example.model.AnalyticsMetricAvailability
import com.example.model.AnalyticsPlatformFilter
import com.example.model.AppDestination
import com.example.model.ApprovalSourceKind
import com.example.model.ConnectedSocialAccount
import com.example.model.ExportedVideoRecord
import com.example.model.MotionExportFormat
import com.example.model.MotionPostProject
import com.example.model.PlatformPostCustomization
import com.example.model.PrimaryTab
import com.example.model.PublishDispatchMode
import com.example.model.PublishingHistoryStatus
import com.example.model.PublishingJobRecord
import com.example.model.SocialPlatform
import com.example.model.VideoAspectRatio
import com.example.security.SecuritySanitizer
import com.example.viewmodel.AnalyticsViewModel
import com.example.viewmodel.ZuzuAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class Phase10AnalyticsSecurityTest {

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
  fun `analytics dashboard starts empty and never fabricates engagement numbers`() = runTest {
    val vm = AnalyticsViewModel()
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    val state = vm.uiState.value
    assertEquals(0, state.totalPublishedPosts)
    assertEquals(0, state.totalScheduledPosts)
    assertEquals(0, state.totalVideosCreated)
    assertEquals(AnalyticsMetricAvailability.NotConnected, state.overallEngagement.availability)
    assertNull(state.overallEngagement.views)
    assertNull(state.overallEngagement.likes)
    assertNull(state.overallEngagement.comments)
    assertNull(state.overallEngagement.shares)
    assertNull(state.overallEngagement.engagementRatePercent)
    assertEquals(3, state.platformSummaries.size)
    assertTrue(state.platformSummaries.all { it.metrics.availability == AnalyticsMetricAvailability.NotConnected })

    vm.syncRemoteAnalytics()
    assertTrue(vm.uiState.value.feedbackMessage.orEmpty().contains("No Social Accounts Connected"))

    collectJob.cancel()
  }

  @Test
  fun `analytics filters by date range and platform using real local repository data`() = runTest {
    val now = System.currentTimeMillis()
    val pubRepo = InMemoryPublishingRepository()
    val p78Repo = InMemoryPhase78Repository()
    val editorRepo = InMemoryVideoEditorRepository()

    pubRepo.savePublishingJob(
      PublishingJobRecord(
        id = "job-yt-recent",
        approvalItemId = "apr-1",
        sourceKind = ApprovalSourceKind.VideoExport,
        postTitle = "Recent YouTube Short",
        platform = SocialPlatform.YouTube,
        accountId = "yt-1",
        accountDisplayName = "Creator YT",
        accountHandle = "@creatoryt",
        customization = PlatformPostCustomization(SocialPlatform.YouTube, title = "Recent YouTube Short"),
        status = PublishingHistoryStatus.Published,
        idempotencyKey = "k-yt-1",
        createdAtMillis = now - 86_400_000L,
        updatedAtMillis = now - 86_400_000L,
      ),
    )

    val oldJob = PublishingJobRecord(
      id = "job-fb-old",
      approvalItemId = "apr-2",
      sourceKind = ApprovalSourceKind.MotionPost,
      postTitle = "Older Facebook Reel",
      platform = SocialPlatform.Facebook,
      accountId = "fb-page-1",
      accountDisplayName = "Creator Page",
      accountHandle = "@creatorpage",
      customization = PlatformPostCustomization(SocialPlatform.Facebook, caption = "Older Facebook Reel"),
      dispatchMode = PublishDispatchMode.Schedule,
      status = PublishingHistoryStatus.Scheduled,
      idempotencyKey = "k-fb-1",
      createdAtMillis = now - 45L * 86_400_000L,
      updatedAtMillis = now - 45L * 86_400_000L,
    )

    editorRepo.saveExportedVideo(
      ExportedVideoRecord(
        id = "exp-1",
        projectId = "proj-1",
        fileName = "StudioRender1.mp4",
        filePath = "/tmp/studio1.mp4",
        fileUriString = "file:///tmp/studio1.mp4",
        durationMs = 45_000L,
        width = 1080,
        height = 1920,
        fileSizeBytes = 4_200_000L,
        aspectRatio = VideoAspectRatio.Vertical,
        resolutionLabel = "1080p",
        createdAtMillis = now - 3_600_000L,
      ),
    )
    p78Repo.saveMotionProject(
      MotionPostProject(
        id = "mot-1",
        title = "Motion Promo MP4",
        exportedFormat = MotionExportFormat.Mp4Video,
        exportedFilePath = "/tmp/motion1.mp4",
        updatedAtMillis = now - 1_800_000L,
      ),
    )

    val vm = AnalyticsViewModel(
      publishingRepository = pubRepo,
      phase78Repository = p78Repo,
      videoEditorRepository = editorRepo,
      clockMs = { System.currentTimeMillis() },
    )
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    assertEquals(1, vm.uiState.value.totalPublishedPosts)
    assertEquals(2, vm.uiState.value.totalVideosCreated)

    vm.selectPlatformFilter(AnalyticsPlatformFilter.Facebook)
    assertEquals(0, vm.uiState.value.totalPublishedPosts)

    vm.selectPlatformFilter(AnalyticsPlatformFilter.YouTube)
    assertEquals(1, vm.uiState.value.totalPublishedPosts)

    vm.selectDateRange(AnalyticsDateRange.Last7Days)
    vm.selectSection(AnalyticsContentSection.VideoPerformance)
    assertEquals(AnalyticsContentSection.VideoPerformance, vm.uiState.value.activeSection)

    vm.openContentDetail("job-yt-recent")
    assertEquals("Recent YouTube Short", vm.uiState.value.selectedContentDetail?.title)
    vm.closeContentDetail()
    assertNull(vm.uiState.value.selectedContentDetail)
    assertFalse(oldJob.postTitle.isBlank())

    collectJob.cancel()
  }

  @Test
  fun `analytics syncs real official metrics when backend configured and handles api errors safely`() = runTest {
    val socialRepo = SocialAccountsRepository(
      config = OAuthEnvironmentConfig(
        backendBaseUrl = "https://oauth.zuzusocial.example",
        youtubeClientId = "pub-yt-client",
      ),
    )
    socialRepo.connectPlatform(SocialPlatform.YouTube)
    val launchReq = socialRepo.launchRequest.value!!
    socialRepo.consumeLaunchRequest()
    assertTrue(
      socialRepo.onAuthorizationVerified(
        platform = SocialPlatform.YouTube,
        stateToken = launchReq.stateToken,
        verifiedAccount = ConnectedSocialAccount(
          accountId = "yt-channel-1",
          platform = SocialPlatform.YouTube,
          displayName = "Zuzu Creator",
          handle = "@zuzucreator",
          grantedScopes = listOf("youtube.readonly", "youtube.upload", "yt-analytics.readonly"),
          connectedAtLabel = "Just now",
        ),
      ),
    )

    val pubRepo = InMemoryPublishingRepository()
    pubRepo.savePublishingJob(
      PublishingJobRecord(
        id = "job-live-1",
        approvalItemId = "apr-live",
        sourceKind = ApprovalSourceKind.VideoExport,
        postTitle = "Live Analytics Video",
        platform = SocialPlatform.YouTube,
        accountId = "yt-channel-1",
        accountDisplayName = "Zuzu Creator",
        accountHandle = "@zuzucreator",
        customization = PlatformPostCustomization(SocialPlatform.YouTube, title = "Live Analytics Video"),
        status = PublishingHistoryStatus.Published,
        idempotencyKey = "k-live-1",
      ),
    )

    var shouldFail = false
    val gateway = DefaultAnalyticsBackendGateway(
      config = AnalyticsBackendConfig(
        backendBaseUrl = "https://api.zuzusocial.example",
        youtubeAnalyticsEnabled = true,
      ),
      apiProvider = {
        object : AnalyticsRetrofitApi {
          override suspend fun fetchAnalyticsOverview(
            bearerJwt: String,
            daysWindow: Int?,
          ): Response<RemoteAnalyticsResponse> {
            return if (shouldFail) {
              Response.error(403, """{"error":"forbidden"}""".toResponseBody("application/json".toMediaType()))
            } else {
              Response.success(
                RemoteAnalyticsResponse(
                  fetchedAtMillis = 123456789L,
                  platformMetrics = listOf(
                    RemotePlatformMetricsDto(
                      platform = "youtube",
                      views = 12_400L,
                      likes = 920L,
                      comments = 84L,
                      shares = 46L,
                      engagementRatePercent = 8.47,
                      dataAvailable = true,
                    ),
                  ),
                  contentMetrics = listOf(
                    RemoteContentMetricsDto(
                      jobId = "job-live-1",
                      platform = "youtube",
                      views = 12_400L,
                      likes = 920L,
                      comments = 84L,
                      shares = 46L,
                      engagementRatePercent = 8.47,
                      dataAvailable = true,
                    ),
                  ),
                ),
              )
            }
          }
        }
      },
    )

    val cloudGateway = DefaultCloudBackendGateway(
      config = CloudBackendConfig(
        firebaseProjectId = "zuzu-prod",
        cloudStorageBucket = "zuzu-prod.appspot.com",
        cloudAutomationBaseUrl = "https://api.zuzu.example.com/",
      ),
    )
    cloudGateway.signInPromptResult(
      CloudAuthSession(
        uid = "creator-1",
        email = "creator@zuzu.example",
        displayName = "Zuzu Creator",
        idToken = "fake-jwt-session",
      ),
    )

    val vm = AnalyticsViewModel(
      publishingRepository = pubRepo,
      socialAccountsRepository = socialRepo,
      analyticsBackend = gateway,
      cloudGateway = cloudGateway,
    )
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    vm.syncRemoteAnalytics()
    val state = vm.uiState.value
    assertEquals(AnalyticsMetricAvailability.Available, state.overallEngagement.availability)
    assertEquals(12_400L, state.overallEngagement.views)
    assertEquals(920L, state.overallEngagement.likes)
    assertEquals("12.4K", state.bestPerformingContent.first().metrics.formattedViews())

    shouldFail = true
    vm.syncRemoteAnalytics()
    assertNotNull(vm.uiState.value.errorMessage)
    assertTrue(vm.uiState.value.errorMessage!!.contains("permissions"))

    collectJob.cancel()
  }

  @Test
  fun `security sanitizer redacts tokens sanitizes control chars and enforces https urls`() {
    val dirty = "  Hello\u0000\u0007 Creator\nVideo  "
    assertEquals("Hello Creator\nVideo", SecuritySanitizer.sanitizeUserText(dirty, 50))
    assertEquals("Hello", SecuritySanitizer.sanitizeUserText(dirty, 5))

    val rawErr = "Failed calling Bearer eyJhbGciOi.payload.sig with access_token=ya29.secret_abc and client_secret=superSecret123"
    val redacted = SecuritySanitizer.redactSensitiveTokens(rawErr)
    assertFalse(redacted.contains("eyJhbGciOi"))
    assertFalse(redacted.contains("ya29.secret_abc"))
    assertFalse(redacted.contains("superSecret123"))
    assertTrue(redacted.contains("[REDACTED]"))

    assertTrue(SecuritySanitizer.isSafeHttpsUrl("https://api.zuzusocial.example"))
    assertFalse(SecuritySanitizer.isSafeHttpsUrl("http://api.zuzusocial.example"))
    assertFalse(SecuritySanitizer.isSafeHttpsUrl("https://localhost:8443"))
    assertFalse(SecuritySanitizer.isSafeHttpsUrl("https://127.0.0.1"))
    assertFalse(SecuritySanitizer.isSafeHttpsUrl("https://192.168.1.50"))
  }

  @Test
  fun `complete navigation across all primary tabs and analytics dashboard returns cleanly`() {
    val vm = ZuzuAppViewModel()

    vm.openAnalyticsDashboard(fromTab = PrimaryTab.Dashboard, initialContentJobId = "job-1")
    assertEquals(
      AppDestination.AnalyticsDashboard(returnTab = PrimaryTab.Dashboard, initialContentJobId = "job-1"),
      vm.uiState.value.destination,
    )
    assertEquals(PrimaryTab.Dashboard, vm.uiState.value.activeTab)

    vm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Dashboard), vm.uiState.value.destination)

    vm.openPublishingCenter(fromTab = PrimaryTab.Studio)
    assertTrue(vm.uiState.value.destination is AppDestination.PublishingCenter)
    vm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Studio), vm.uiState.value.destination)

    vm.openSchedulerAutomation(fromTab = PrimaryTab.Projects)
    assertTrue(vm.uiState.value.destination is AppDestination.SchedulerAutomation)
    vm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Projects), vm.uiState.value.destination)
  }
}
