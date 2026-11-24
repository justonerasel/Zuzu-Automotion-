package com.example

import com.example.data.cloud.CloudAuthSession
import com.example.data.cloud.CloudAutomationRetrofitApi
import com.example.data.cloud.CloudBackendConfig
import com.example.data.cloud.CloudScheduleTaskRequest
import com.example.data.cloud.CloudScheduleTaskResponse
import com.example.data.cloud.CloudStorageSignedUrlResponse
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.cloud.FirestoreSchemaContract
import com.example.data.motion.MotionExportOutcome
import com.example.data.motion.MotionPostExporter
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.AppDestination
import com.example.model.ApprovalContentItem
import com.example.model.ApprovalSourceKind
import com.example.model.ApprovalStatus
import com.example.model.CloudOperationResult
import com.example.model.CloudServiceHealth
import com.example.model.CloudSyncState
import com.example.model.ContentOrigin
import com.example.model.ExportedVideoRecord
import com.example.model.MediaType
import com.example.model.MotionAnimationStyle
import com.example.model.MotionExportFormat
import com.example.model.MotionPlatformPreset
import com.example.model.MotionPostProject
import com.example.model.MotionTemplateCategory
import com.example.model.MotionTransition
import com.example.model.PermissionStatus
import com.example.model.PrimaryTab
import com.example.model.ScheduledJobStatus
import com.example.model.SocialPlatform
import com.example.model.VideoAspectRatio
import com.example.viewmodel.ApprovalSchedulerViewModel
import com.example.viewmodel.MotionStudioViewModel
import com.example.viewmodel.ZuzuAppViewModel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class Phase78WorkflowTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var tempDir: File

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    tempDir = createTempDirectory("zuzu_phase78_test").toFile()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    tempDir.deleteRecursively()
  }

  @Test
  fun `motion template selection, styles, formats, stickers, music edits, and local save work cleanly`() = runTest {
    val phase78Repo = InMemoryPhase78Repository()
    val sourceRepo = InMemoryContentSourceRepository()
    val vm = MotionStudioViewModel(
      repository = phase78Repo,
      contentSourceRepository = sourceRepo,
      exporter = FakeMotionPostExporter(),
      exportsDirectoryProvider = { tempDir },
    )
    val job = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    assertEquals(9, MotionTemplateCategory.entries.size)
    assertEquals(8, MotionAnimationStyle.entries.size)

    MotionTemplateCategory.entries.forEach { template ->
      vm.applyTemplate(template)
      assertEquals(template, vm.uiState.value.activeProject.templateCategory)
      assertEquals(template.defaultStyle, vm.uiState.value.activeProject.animationStyle)
    }

    MotionAnimationStyle.entries.forEach { style ->
      vm.setAnimationStyle(style)
      assertEquals(style, vm.uiState.value.activeProject.animationStyle)
    }

    vm.selectPlatformPreset(MotionPlatformPreset.TikTokVertical)
    assertEquals(VideoAspectRatio.Vertical, vm.uiState.value.activeProject.aspectRatio)

    vm.setAspectRatio(VideoAspectRatio.Square)
    assertEquals(VideoAspectRatio.Square, vm.uiState.value.activeProject.aspectRatio)

    vm.setTransition(MotionTransition.SlideSweep)
    vm.setDurationSeconds(12)
    vm.updateTitle("Autumn Brand Drop")
    vm.updateCopy("50% Launch Promo", "Tap to explore")
    vm.addSticker("FLASH SALE", "⚡")
    vm.setLogoBadge("ZUZU")
    vm.setBackgroundMusic("LoFi Pulse", "content://media/audio/1")
    vm.setMusicVolume(0.65f)
    vm.saveProject()

    val saved = phase78Repo.motionProjects.first()
    assertEquals(1, saved.size)
    assertEquals("Autumn Brand Drop", saved.first().title)
    assertEquals(12, saved.first().durationSeconds)
    assertEquals(1, saved.first().stickers.size)
    assertEquals("ZUZU", saved.first().logoLabel)

    job.cancel()
  }

  @Test
  fun `motion export creates MP4, PNG, and JPEG files and registers entries in Media Library`() = runTest {
    val phase78Repo = InMemoryPhase78Repository()
    val sourceRepo = InMemoryContentSourceRepository()
    val vm = MotionStudioViewModel(
      repository = phase78Repo,
      contentSourceRepository = sourceRepo,
      exporter = FakeMotionPostExporter(),
      exportsDirectoryProvider = { tempDir },
    )
    val job = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    vm.updateTitle("Product Spotlight")
    vm.exportMotionPost(MotionExportFormat.Mp4Video)
    assertNotNull(vm.uiState.value.activeProject.exportedFilePath)
    assertEquals(MotionExportFormat.Mp4Video, vm.uiState.value.activeProject.exportedFormat)

    vm.exportMotionPost(MotionExportFormat.PngImage)
    assertEquals(MotionExportFormat.PngImage, vm.uiState.value.activeProject.exportedFormat)

    vm.exportMotionPost(MotionExportFormat.JpegImage)
    assertEquals(MotionExportFormat.JpegImage, vm.uiState.value.activeProject.exportedFormat)

    val libraryEntries = sourceRepo.mediaEntries.first()
    assertEquals(3, libraryEntries.size)
    assertTrue(libraryEntries.any { it.mediaType == MediaType.Video })
    assertTrue(libraryEntries.count { it.mediaType == MediaType.Image } == 2)

    job.cancel()
  }

  @Test
  fun `approval center enforces content permission gate before approving external media`() = runTest {
    val phase78Repo = InMemoryPhase78Repository()
    val sourceRepo = InMemoryContentSourceRepository()
    val gateway = DefaultCloudBackendGateway()
    val vm = ApprovalSchedulerViewModel(
      repository = phase78Repo,
      contentSourceRepository = sourceRepo,
      cloudGateway = gateway,
    )
    val job = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    val externalItem = ApprovalContentItem(
      id = "apr_ext_1",
      sourceKind = ApprovalSourceKind.VideoExport,
      sourceId = "exp_1",
      title = "Partner Clip Showcase",
      description = "Partner highlight",
      hashtagsCsv = "#Partner",
      targetPlatforms = listOf(SocialPlatform.YouTube),
      permissionOrigin = ContentOrigin.YouTube,
      permissionStatus = PermissionStatus.PermissionPending,
      permissionOwner = "",
      permissionEvidence = "",
      permissionApprovedForReuse = false,
      status = ApprovalStatus.PendingReview,
    )
    vm.submitExternalItemForApproval(externalItem)

    // Attempt approval without verified permission -> MUST be blocked
    vm.approveItem("apr_ext_1")
    val blocked = phase78Repo.getApprovalItem("apr_ext_1")!!
    assertEquals(ApprovalStatus.PendingReview, blocked.status)
    assertTrue(vm.uiState.value.errorMessage?.contains("Permission Check Blocked Approval") == true)

    // Verify permission with owner & license evidence
    vm.updateItemPermissionEvidence(
      itemId = "apr_ext_1",
      status = PermissionStatus.LicensedContent,
      owner = "Partner Studio",
      evidence = "Signed Commercial License #8891",
      approvedForReuse = true,
    )

    // Now explicit approval succeeds locally and marks SetupRequired honestly
    vm.approveItem("apr_ext_1")
    val approved = phase78Repo.getApprovalItem("apr_ext_1")!!
    assertEquals(ApprovalStatus.Approved, approved.status)
    assertEquals(CloudSyncState.SyncPendingSetupRequired, approved.cloudSyncState)

    // Draft & Reject flows work cleanly
    vm.saveApprovalDraft("apr_ext_1")
    assertEquals(ApprovalStatus.Draft, phase78Repo.getApprovalItem("apr_ext_1")!!.status)

    vm.rejectItem("apr_ext_1", "Update thumbnail contrast")
    val rejected = phase78Repo.getApprovalItem("apr_ext_1")!!
    assertEquals(ApprovalStatus.Rejected, rejected.status)
    assertEquals("Update thumbnail contrast", rejected.rejectionReason)

    job.cancel()
  }

  @Test
  fun `scheduler blocks unapproved items, past dates, duplicates and enforces max retries safely`() = runTest {
    var nowMs = 1_700_000_000_000L
    val phase78Repo = InMemoryPhase78Repository()
    val sourceRepo = InMemoryContentSourceRepository()
    val gateway = DefaultCloudBackendGateway()
    val vm = ApprovalSchedulerViewModel(
      repository = phase78Repo,
      contentSourceRepository = sourceRepo,
      cloudGateway = gateway,
      clockMs = { nowMs },
    )
    val job = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    vm.submitExportedVideoForApproval(
      ExportedVideoRecord(
        id = "vid_100",
        projectId = "proj_1",
        fileName = "launch_reel.mp4",
        filePath = "/tmp/launch_reel.mp4",
        fileUriString = "file:///tmp/launch_reel.mp4",
        aspectRatio = VideoAspectRatio.Vertical,
        resolutionLabel = "720p HD",
        durationMs = 8_000L,
        width = 720,
        height = 1280,
        fileSizeBytes = 128_000L,
      ),
    )

    val aprId = "apr_video_vid_100"
    val futureTs = nowMs + 3_600_000L

    // Unapproved item cannot be scheduled
    vm.scheduleApprovedPost(aprId, listOf(SocialPlatform.YouTube), futureTs)
    assertTrue(phase78Repo.scheduledJobs.first().isEmpty())
    assertTrue(vm.uiState.value.errorMessage?.contains("Explicit user approval is required") == true)

    // Approve first
    vm.approveItem(aprId)

    // Past timestamp rejected
    vm.scheduleApprovedPost(aprId, listOf(SocialPlatform.YouTube), nowMs - 1_000L)
    assertTrue(phase78Repo.scheduledJobs.first().isEmpty())

    // Future schedule saved locally in SetupRequired state (honest when no cloud creds)
    vm.scheduleApprovedPost(aprId, listOf(SocialPlatform.YouTube, SocialPlatform.TikTok), futureTs)
    val jobs = phase78Repo.scheduledJobs.first()
    assertEquals(1, jobs.size)
    assertEquals(ScheduledJobStatus.QueuedLocalSetupRequired, jobs.first().status)
    assertNull(jobs.first().cloudTaskName)

    // Duplicate publish with identical key must be blocked
    vm.scheduleApprovedPost(aprId, listOf(SocialPlatform.TikTok, SocialPlatform.YouTube), futureTs)
    assertEquals(1, phase78Repo.scheduledJobs.first().size)
    assertTrue(vm.uiState.value.errorMessage?.contains("Duplicate publish blocked") == true)

    // Edit job updates platforms and timestamp
    val jobId = jobs.first().id
    val updatedTs = futureTs + 7_200_000L
    vm.editScheduledJob(jobId, listOf(SocialPlatform.Facebook), updatedTs)
    val edited = phase78Repo.getScheduledJob(jobId)!!
    assertEquals(listOf(SocialPlatform.Facebook), edited.platforms)
    assertEquals(updatedTs, edited.scheduledAtMillis)

    // Cancel job marks Cancelled
    vm.cancelScheduledJob(jobId)
    assertEquals(ScheduledJobStatus.Cancelled, phase78Repo.getScheduledJob(jobId)!!.status)

    // Max retries safety check
    val futureTs2 = nowMs + 9_000_000L
    vm.scheduleApprovedPost(aprId, listOf(SocialPlatform.YouTube), futureTs2)
    val secondJob = phase78Repo.scheduledJobs.first().first { it.status != ScheduledJobStatus.Cancelled }
    vm.markJobFailedForRetryTest(secondJob.id, "Transient network error")
    phase78Repo.saveScheduledJob(
      phase78Repo.getScheduledJob(secondJob.id)!!.copy(attemptCount = 3, maxRetries = 3),
    )
    vm.retryScheduledJob(secondJob.id)
    assertTrue(vm.uiState.value.errorMessage?.contains("Maximum safe retry limit") == true)

    job.cancel()
  }

  @Test
  fun `cloud backend gateway distinguishes SetupRequired from configured live Cloud Tasks sync`() = runTest {
    // 1. Unconfigured -> SetupRequired without fake success
    val unconfiguredGateway = DefaultCloudBackendGateway()
    val unconfiguredReport = unconfiguredGateway.refreshStatus()
    assertEquals(CloudServiceHealth.SetupRequired, unconfiguredReport.overallHealth)
    assertTrue(unconfiguredReport.setupChecklist.isNotEmpty())
    assertEquals(
      "users/uid123/scheduled_posts",
      FirestoreSchemaContract.scheduledPostsPath("uid123"),
    )

    // 2. Configured + signed-in session + working API -> Ready & Synced
    val fakeApi = FakeCloudAutomationApi()
    val configuredGateway = DefaultCloudBackendGateway(
      config = CloudBackendConfig(
        firebaseProjectId = "zuzu-prod-app",
        firestoreDatabaseId = "(default)",
        cloudStorageBucket = "zuzu-prod-app.appspot.com",
        cloudAutomationBaseUrl = "https://automation.zuzu.example.com/",
      ),
      apiProvider = { fakeApi },
    )
    val signInResult = configuredGateway.signInPromptResult(
      CloudAuthSession(
        uid = "creator_42",
        email = "creator@example.com",
        displayName = "Creator",
        idToken = "verified-token",
      ),
    )
    assertTrue(signInResult is CloudOperationResult.Success)
    val readyReport = configuredGateway.refreshStatus()
    assertEquals(CloudServiceHealth.Ready, readyReport.overallHealth)

    val phase78Repo = InMemoryPhase78Repository()
    val vm = ApprovalSchedulerViewModel(
      repository = phase78Repo,
      contentSourceRepository = InMemoryContentSourceRepository(),
      cloudGateway = configuredGateway,
      clockMs = { 1_700_000_000_000L },
    )
    val collectJob = backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }

    vm.submitExternalItemForApproval(
      ApprovalContentItem(
        id = "apr_cloud_1",
        sourceKind = ApprovalSourceKind.MotionPost,
        sourceId = "mot_1",
        title = "Cloud Scheduled Motion",
        targetPlatforms = listOf(SocialPlatform.YouTube),
        permissionOrigin = ContentOrigin.MyUploads,
        permissionStatus = PermissionStatus.OriginalContent,
        permissionApprovedForReuse = true,
      ),
    )
    vm.approveItem("apr_cloud_1")
    val approved = phase78Repo.getApprovalItem("apr_cloud_1")!!
    assertEquals(ApprovalStatus.Approved, approved.status)
    assertEquals(CloudSyncState.Synced, approved.cloudSyncState)

    vm.scheduleApprovedPost("apr_cloud_1", listOf(SocialPlatform.YouTube), 1_700_005_000_000L)
    val scheduled = phase78Repo.scheduledJobs.first().first()
    assertEquals(ScheduledJobStatus.ScheduledCloudPending, scheduled.status)
    assertEquals("projects/zuzu/locations/asia-east1/queues/publish/tasks/t-1", scheduled.cloudTaskName)

    // Safe retry increments attemptCount on configured backend
    vm.markJobFailedForRetryTest(scheduled.id, "503 temporary")
    vm.retryScheduledJob(scheduled.id)
    assertEquals(2, phase78Repo.getScheduledJob(scheduled.id)!!.attemptCount)
    assertEquals(ScheduledJobStatus.ScheduledCloudPending, phase78Repo.getScheduledJob(scheduled.id)!!.status)

    collectJob.cancel()
  }

  @Test
  fun `main app navigation and dashboard metrics track motion posts and scheduled posts cleanly`() = runTest {
    val phase78Repo = InMemoryPhase78Repository()
    val appVm = ZuzuAppViewModel(phase78Repository = phase78Repo)

    appVm.openMotionStudio(fromTab = PrimaryTab.Studio)
    assertTrue(appVm.uiState.value.destination is AppDestination.MotionStudio)
    appVm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Studio), appVm.uiState.value.destination)

    appVm.openApprovalCenter(fromTab = PrimaryTab.Dashboard)
    assertTrue(appVm.uiState.value.destination is AppDestination.ApprovalCenter)
    appVm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Dashboard), appVm.uiState.value.destination)

    appVm.openSchedulerAutomation(fromTab = PrimaryTab.Projects, showCloudStatusFirst = true)
    val dest = appVm.uiState.value.destination as AppDestination.SchedulerAutomation
    assertTrue(dest.showCloudStatusFirst)
    appVm.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Projects), appVm.uiState.value.destination)

    phase78Repo.saveMotionProject(MotionPostProject(id = "m1", title = "Promo #1"))
    assertEquals(
      1,
      appVm.uiState.value.metrics.first { it.id == "motion_posts_created" }.count,
    )
  }

  private class FakeMotionPostExporter : MotionPostExporter {
    override fun canEncodeMp4(): Boolean = true

    override suspend fun exportMotionPost(
      project: MotionPostProject,
      format: MotionExportFormat,
      outputFile: File,
      onProgress: (Float, String) -> Unit,
    ): MotionExportOutcome {
      outputFile.parentFile?.mkdirs()
      outputFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x04))
      onProgress(1f, "Done")
      return MotionExportOutcome.Success(
        outputFile = outputFile,
        format = format,
        width = 720,
        height = 1280,
        durationMs = if (format == MotionExportFormat.Mp4Video) project.durationSeconds * 1000L else null,
        fileSizeBytes = outputFile.length(),
      )
    }
  }

  private class FakeCloudAutomationApi : CloudAutomationRetrofitApi {
    override suspend fun syncProject(bearerToken: String, payload: com.example.data.cloud.CloudProjectSyncPayload) =
      Response.success(Unit)

    override suspend fun syncMediaMetadata(bearerToken: String, payload: com.example.data.cloud.CloudMediaMetadataPayload) =
      Response.success(Unit)

    override suspend fun syncApproval(bearerToken: String, payload: com.example.data.cloud.CloudApprovalSyncPayload) =
      Response.success(Unit)

    override suspend fun requestSignedUploadUrl(bearerToken: String, payload: com.example.data.cloud.CloudMediaMetadataPayload) =
      Response.success(CloudStorageSignedUrlResponse("https://upload.example/signed", "gs://bucket/obj"))

    override suspend fun uploadMediaBinary(signedUrl: String, binaryBody: RequestBody) =
      Response.success(Unit)

    override suspend fun createCloudTaskSchedule(bearerToken: String, request: CloudScheduleTaskRequest) =
      Response.success(
        CloudScheduleTaskResponse(
          jobId = request.jobId,
          cloudTaskName = "projects/zuzu/locations/asia-east1/queues/publish/tasks/t-1",
          cloudSchedulerJobId = "sched-1",
          status = "SCHEDULED",
        ),
      )

    override suspend fun updateCloudTaskSchedule(bearerToken: String, jobId: String, request: CloudScheduleTaskRequest) =
      Response.success(
        CloudScheduleTaskResponse(
          jobId = jobId,
          cloudTaskName = "projects/zuzu/locations/asia-east1/queues/publish/tasks/t-1-updated",
          cloudSchedulerJobId = "sched-1",
          status = "SCHEDULED",
        ),
      )

    override suspend fun cancelCloudTaskSchedule(bearerToken: String, jobId: String) =
      Response.success(Unit)

    override suspend fun retryCloudTaskSchedule(bearerToken: String, jobId: String) =
      Response.success(
        CloudScheduleTaskResponse(
          jobId = jobId,
          cloudTaskName = "projects/zuzu/locations/asia-east1/queues/publish/tasks/t-1-retry",
          cloudSchedulerJobId = "sched-1",
          status = "SCHEDULED",
        ),
      )

    override suspend fun getJobStatus(bearerToken: String, jobId: String) =
      Response.success(
        CloudScheduleTaskResponse(
          jobId = jobId,
          cloudTaskName = "projects/zuzu/locations/asia-east1/queues/publish/tasks/t-1",
          cloudSchedulerJobId = "sched-1",
          status = "SCHEDULED",
        ),
      )
  }
}
