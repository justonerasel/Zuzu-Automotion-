package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.analytics.AnalyticsBackendGateway
import com.example.data.analytics.DefaultAnalyticsBackendGateway
import com.example.data.analytics.RemoteAnalyticsSnapshot
import com.example.data.cloud.CloudBackendGateway
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.editor.InMemoryVideoEditorRepository
import com.example.data.editor.VideoEditorRepository
import com.example.data.oauth.SocialAccountsRepository
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.phase78.Phase78Repository
import com.example.data.publishing.InMemoryPublishingRepository
import com.example.data.publishing.PublishingRepository
import com.example.model.AnalyticsContentSection
import com.example.model.AnalyticsDateRange
import com.example.model.AnalyticsMetricAvailability
import com.example.model.AnalyticsPlatformFilter
import com.example.model.CloudOperationResult
import com.example.model.ContentPerformanceItem
import com.example.model.EngagementMetricsSnapshot
import com.example.model.EngagementTrendPoint
import com.example.model.ExportedVideoRecord
import com.example.model.MotionExportFormat
import com.example.model.MotionPostProject
import com.example.model.PlatformAnalyticsSummary
import com.example.model.PublishingHistoryStatus
import com.example.model.PublishingJobRecord
import com.example.model.ScheduledJobStatus
import com.example.model.ScheduledPostJob
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.security.SecuritySanitizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnalyticsUiState(
  val dateRange: AnalyticsDateRange = AnalyticsDateRange.Last30Days,
  val platformFilter: AnalyticsPlatformFilter = AnalyticsPlatformFilter.All,
  val activeSection: AnalyticsContentSection = AnalyticsContentSection.Overview,
  val totalPublishedPosts: Int = 0,
  val totalScheduledPosts: Int = 0,
  val totalVideosCreated: Int = 0,
  val totalMotionPostsCreated: Int = 0,
  val overallEngagement: EngagementMetricsSnapshot = EngagementMetricsSnapshot(),
  val platformSummaries: List<PlatformAnalyticsSummary> = emptyList(),
  val allContentItems: List<ContentPerformanceItem> = emptyList(),
  val bestPerformingContent: List<ContentPerformanceItem> = emptyList(),
  val recentPosts: List<ContentPerformanceItem> = emptyList(),
  val videoPerformanceItems: List<ContentPerformanceItem> = emptyList(),
  val publishingHistoryItems: List<ContentPerformanceItem> = emptyList(),
  val engagementTrends: List<EngagementTrendPoint> = emptyList(),
  val selectedContentDetail: ContentPerformanceItem? = null,
  val lastSyncedAtMillis: Long? = null,
  val isSyncing: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class AnalyticsViewModel(
  private val publishingRepository: PublishingRepository = InMemoryPublishingRepository(),
  private val phase78Repository: Phase78Repository = InMemoryPhase78Repository(),
  private val videoEditorRepository: VideoEditorRepository = InMemoryVideoEditorRepository(),
  private val socialAccountsRepository: SocialAccountsRepository = SocialAccountsRepository(),
  private val analyticsBackend: AnalyticsBackendGateway = DefaultAnalyticsBackendGateway(),
  private val cloudGateway: CloudBackendGateway = DefaultCloudBackendGateway(),
  private val clockMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

  private val dateRange = MutableStateFlow(AnalyticsDateRange.Last30Days)
  private val platformFilter = MutableStateFlow(AnalyticsPlatformFilter.All)
  private val activeSection = MutableStateFlow(AnalyticsContentSection.Overview)
  private val selectedContentId = MutableStateFlow<String?>(null)
  private val isSyncing = MutableStateFlow(false)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private val filterStateFlow = combine(
    dateRange,
    platformFilter,
    activeSection,
    selectedContentId,
  ) { range, platform, section, detailId ->
    FilterSelection(range, platform, section, detailId)
  }

  private val syncStateFlow = combine(
    isSyncing,
    feedbackMessage,
    errorMessage,
  ) { syncing, feedback, err ->
    SyncBannerState(syncing, feedback, err)
  }

  private val dataFlow = combine(
    publishingRepository.publishingJobs,
    phase78Repository.scheduledJobs,
    phase78Repository.motionProjects,
    videoEditorRepository.exportedVideos,
    socialAccountsRepository.platformStates,
  ) { jobs, schedJobs, motions, exports, platforms ->
    LocalAnalyticsData(jobs, schedJobs, motions, exports, platforms)
  }

  val uiState: StateFlow<AnalyticsUiState> = combine(
    dataFlow,
    analyticsBackend.cachedSnapshot,
    filterStateFlow,
    syncStateFlow,
  ) { local, remote, filter, sync ->
    buildUiState(local, remote, filter, sync)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = AnalyticsUiState(),
  )

  fun selectDateRange(range: AnalyticsDateRange) {
    dateRange.value = range
  }

  fun selectPlatformFilter(filter: AnalyticsPlatformFilter) {
    platformFilter.value = filter
  }

  fun selectSection(section: AnalyticsContentSection) {
    activeSection.value = section
  }

  fun openContentDetail(contentId: String) {
    selectedContentId.value = contentId
  }

  fun closeContentDetail() {
    selectedContentId.value = null
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  fun syncRemoteAnalytics() {
    viewModelScope.launch {
      clearMessages()
      isSyncing.value = true
      val connected = socialAccountsRepository.platformStates.value
        .filter { it.isConnected }
        .map { it.platform }
        .toSet()

      val result = analyticsBackend.syncPlatformAnalytics(
        session = cloudGateway.authSession.value,
        connectedPlatforms = connected,
        dateRange = dateRange.value,
      )
      isSyncing.value = false

      when (result) {
        is CloudOperationResult.Success -> {
          feedbackMessage.value = "Official platform analytics refreshed."
        }
        is CloudOperationResult.SetupRequired -> {
          feedbackMessage.value = "${result.title}: ${result.reason}"
        }
        is CloudOperationResult.Failure -> {
          errorMessage.value = SecuritySanitizer.redactSensitiveTokens(result.message)
        }
      }
    }
  }

  private fun buildUiState(
    local: LocalAnalyticsData,
    remote: RemoteAnalyticsSnapshot,
    filter: FilterSelection,
    sync: SyncBannerState,
  ): AnalyticsUiState {
    val now = clockMs()
    val cutoffMillis = filter.range.daysWindow?.let { days ->
      now - days.toLong() * 86_400_000L
    }

    val jobsInDate = local.publishingJobs.filter { cutoffMillis == null || it.updatedAtMillis >= cutoffMillis }
    val filteredJobs = jobsInDate.filter { filter.platform.matches(it.platform) }

    val schedJobsInDate = local.scheduledJobs.filter { cutoffMillis == null || it.updatedAtMillis >= cutoffMillis }
    val filteredSchedJobs = schedJobsInDate.filter { job ->
      filter.platform == AnalyticsPlatformFilter.All ||
        job.platforms.any { filter.platform.matches(it) }
    }

    val exportsInDate = local.exportedVideos.filter { cutoffMillis == null || it.createdAtMillis >= cutoffMillis }
    val motionsInDate = local.motionProjects.filter { cutoffMillis == null || it.updatedAtMillis >= cutoffMillis }

    val publishedJobs = filteredJobs.filter { it.status == PublishingHistoryStatus.Published }
    val scheduledPublishingIds = filteredJobs
      .filter { it.status == PublishingHistoryStatus.Scheduled }
      .map { it.id }
      .toSet()
    val extraCloudScheduled = filteredSchedJobs.count {
      it.status == ScheduledJobStatus.ScheduledCloudPending ||
        it.status == ScheduledJobStatus.QueuedLocalSetupRequired ||
        it.status == ScheduledJobStatus.Retrying
    }
    val totalPublishedCount = publishedJobs.size +
      filteredSchedJobs.count { it.status == ScheduledJobStatus.Completed }
    val totalScheduledCount = scheduledPublishingIds.size + extraCloudScheduled

    val motionVideosCount = motionsInDate.count {
      it.exportedFormat == MotionExportFormat.Mp4Video && !it.exportedFilePath.isNullOrBlank()
    }
    val totalVideosCreated = exportsInDate.size + motionVideosCount

    val platformSummaries = buildPlatformSummaries(
      platformStates = local.platformStates,
      filteredJobs = jobsInDate,
      remote = remote,
    )

    val visibleSummaries = platformSummaries.filter {
      filter.platform.matches(it.platform)
    }

    val overallEngagement = aggregateOverallEngagement(
      summaries = visibleSummaries,
      hasPublishedPosts = totalPublishedCount > 0,
    )

    val contentItems = buildContentItems(
      publishingJobs = filteredJobs,
      exportedVideos = if (filter.platform == AnalyticsPlatformFilter.All) exportsInDate else emptyList(),
      motionProjects = if (filter.platform == AnalyticsPlatformFilter.All) motionsInDate else emptyList(),
      platformStates = local.platformStates,
      remote = remote,
    )

    val bestPerforming = contentItems
      .filter { it.status == PublishingHistoryStatus.Published }
      .sortedWith(
        compareByDescending<ContentPerformanceItem> { it.metrics.views ?: -1L }
          .thenByDescending { it.metrics.likes ?: -1L }
          .thenByDescending { it.updatedAtMillis },
      )

    val recentPosts = contentItems.sortedByDescending { it.updatedAtMillis }
    val videoPerformance = contentItems.filter {
      it.sourceKindLabel.contains("Video", ignoreCase = true) ||
        it.sourceKindLabel.contains("MP4", ignoreCase = true)
    }
    val historyItems = filteredJobs
      .sortedByDescending { it.updatedAtMillis }
      .map { job -> job.toContentPerformanceItem(local.platformStates, remote) }

    val trends = buildEngagementTrends(
      dateRange = filter.range,
      nowMillis = now,
      jobs = filteredJobs,
      remote = remote,
    )

    val detailItem = filter.detailId?.let { id ->
      contentItems.firstOrNull { it.id == id }
    }

    return AnalyticsUiState(
      dateRange = filter.range,
      platformFilter = filter.platform,
      activeSection = filter.section,
      totalPublishedPosts = totalPublishedCount,
      totalScheduledPosts = totalScheduledCount,
      totalVideosCreated = totalVideosCreated,
      totalMotionPostsCreated = motionsInDate.size,
      overallEngagement = overallEngagement,
      platformSummaries = platformSummaries,
      allContentItems = contentItems,
      bestPerformingContent = bestPerforming,
      recentPosts = recentPosts,
      videoPerformanceItems = videoPerformance,
      publishingHistoryItems = historyItems,
      engagementTrends = trends,
      selectedContentDetail = detailItem,
      lastSyncedAtMillis = remote.fetchedAtMillis.takeIf { it > 0L },
      isSyncing = sync.isSyncing,
      feedbackMessage = sync.feedbackMessage,
      errorMessage = sync.errorMessage,
    )
  }

  private fun buildPlatformSummaries(
    platformStates: List<SocialPlatformAccountState>,
    filteredJobs: List<PublishingJobRecord>,
    remote: RemoteAnalyticsSnapshot,
  ): List<PlatformAnalyticsSummary> {
    return SocialPlatform.entries.map { platform ->
      val accountState = platformStates.firstOrNull { it.platform == platform }
      val isConnected = accountState?.isConnected == true
      val handles = accountState?.connectedAccounts?.map { it.handle }.orEmpty()
      val platformJobs = filteredJobs.filter { it.platform == platform }
      val publishedCount = platformJobs.count { it.status == PublishingHistoryStatus.Published }
      val scheduledCount = platformJobs.count { it.status == PublishingHistoryStatus.Scheduled }
      val failedCount = platformJobs.count { it.status == PublishingHistoryStatus.Failed }

      val remoteSnapshot = remote.platformMetrics[platform]
      val availabilitySnapshot = when {
        remoteSnapshot != null && remoteSnapshot.availability == AnalyticsMetricAvailability.Available -> remoteSnapshot
        !isConnected -> EngagementMetricsSnapshot(
          availability = AnalyticsMetricAvailability.NotConnected,
          availabilityNote = "${platform.displayName} account is not connected. Connect via OAuth in Social Accounts.",
        )
        !analyticsBackend.config.isEnabledFor(platform) -> EngagementMetricsSnapshot(
          availability = AnalyticsMetricAvailability.SetupRequired,
          availabilityNote = "${platform.displayName} official analytics API credentials are not configured on backend.",
        )
        else -> remoteSnapshot ?: EngagementMetricsSnapshot(
          availability = AnalyticsMetricAvailability.DataUnavailable,
          availabilityNote = "Connected. Tap Refresh Analytics to fetch live ${platform.displayName} API metrics.",
        )
      }

      val apiNote = when (platform) {
        SocialPlatform.YouTube -> "YouTube Analytics API (yt-analytics.readonly)"
        SocialPlatform.Facebook -> "Meta Graph Page Insights (pages_read_engagement)"
        SocialPlatform.TikTok -> "TikTok Video Query API v2 (video.list)"
      }

      PlatformAnalyticsSummary(
        platform = platform,
        isAccountConnected = isConnected,
        connectedHandles = handles,
        publishedCount = publishedCount,
        scheduledCount = scheduledCount,
        failedCount = failedCount,
        apiCapabilityNote = apiNote,
        metrics = availabilitySnapshot,
      )
    }
  }

  private fun aggregateOverallEngagement(
    summaries: List<PlatformAnalyticsSummary>,
    hasPublishedPosts: Boolean,
  ): EngagementMetricsSnapshot {
    val liveSummaries = summaries.filter { it.metrics.hasLiveNumbers }
    if (liveSummaries.isEmpty()) {
      val anyConnected = summaries.any { it.isAccountConnected }
      val anySetupMissing = summaries.any {
        it.metrics.availability == AnalyticsMetricAvailability.SetupRequired
      }
      val availability = when {
        !anyConnected -> AnalyticsMetricAvailability.NotConnected
        anySetupMissing -> AnalyticsMetricAvailability.SetupRequired
        else -> AnalyticsMetricAvailability.DataUnavailable
      }
      val note = when (availability) {
        AnalyticsMetricAvailability.NotConnected ->
          "No social accounts are connected. Views, likes, comments, and shares are never fabricated."
        AnalyticsMetricAvailability.SetupRequired ->
          "Official platform Analytics/Insights API credentials must be deployed on the HTTPS backend to display live views, likes, comments, and shares."
        AnalyticsMetricAvailability.DataUnavailable ->
          if (hasPublishedPosts) {
            "No remote engagement telemetry returned yet by platform APIs."
          } else {
            "Publish approved content to connected accounts and enable Analytics API sync to view engagement metrics."
          }
        AnalyticsMetricAvailability.Available -> ""
      }
      return EngagementMetricsSnapshot(
        availability = availability,
        availabilityNote = note,
      )
    }

    val totalViews = liveSummaries.sumOf { it.metrics.views ?: 0L }
    val totalLikes = liveSummaries.sumOf { it.metrics.likes ?: 0L }
    val totalComments = liveSummaries.sumOf { it.metrics.comments ?: 0L }
    val totalShares = liveSummaries.sumOf { it.metrics.shares ?: 0L }
    val rate = if (totalViews > 0L) {
      ((totalLikes + totalComments + totalShares).toDouble() / totalViews.toDouble()) * 100.0
    } else {
      0.0
    }

    return EngagementMetricsSnapshot(
      views = totalViews,
      likes = totalLikes,
      comments = totalComments,
      shares = totalShares,
      engagementRatePercent = rate,
      availability = AnalyticsMetricAvailability.Available,
      availabilityNote = "Aggregated from connected platform Analytics APIs.",
    )
  }

  private fun buildContentItems(
    publishingJobs: List<PublishingJobRecord>,
    exportedVideos: List<ExportedVideoRecord>,
    motionProjects: List<MotionPostProject>,
    platformStates: List<SocialPlatformAccountState>,
    remote: RemoteAnalyticsSnapshot,
  ): List<ContentPerformanceItem> {
    val jobItems = publishingJobs.map { it.toContentPerformanceItem(platformStates, remote) }
    val trackedPaths = jobItems.mapNotNull { it.mediaFilePath }.toSet()

    val exportItems = exportedVideos
      .filter { it.filePath !in trackedPaths }
      .map { exp ->
        val seconds = (exp.durationMs / 1000L).coerceAtLeast(0L)
        val durationText = String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L)
        ContentPerformanceItem(
          id = "exp_${exp.id}",
          title = exp.fileName.substringBeforeLast("."),
          platform = null,
          status = PublishingHistoryStatus.Draft,
          sourceKindLabel = "Rendered MP4 (${exp.resolutionLabel})",
          aspectRatioLabel = exp.aspectRatio.label,
          mediaFilePath = exp.filePath,
          mediaUriString = exp.fileUriString,
          statusDetail = "$durationText • Ready for Approval & Publishing",
          createdAtMillis = exp.createdAtMillis,
          updatedAtMillis = exp.createdAtMillis,
          metrics = EngagementMetricsSnapshot(
            availability = AnalyticsMetricAvailability.DataUnavailable,
            availabilityNote = "Local rendered video — not yet published to a social channel.",
          ),
        )
      }

    val motionItems = motionProjects
      .filter { proj ->
        val path = proj.exportedFilePath
        path == null || path !in trackedPaths
      }
      .map { motion ->
        val formatLabel = motion.exportedFormat?.displayLabel ?: "Motion Draft"
        ContentPerformanceItem(
          id = "mot_${motion.id}",
          title = motion.title.ifBlank { motion.headlineText },
          platform = null,
          status = PublishingHistoryStatus.Draft,
          sourceKindLabel = "Motion Studio ($formatLabel)",
          aspectRatioLabel = motion.aspectRatio.label,
          mediaFilePath = motion.exportedFilePath,
          mediaUriString = motion.exportedFileUriString,
          statusDetail = if (motion.exportedFilePath != null) {
            "Exported $formatLabel • Ready for Approval"
          } else {
            "Motion draft (${motion.templateCategory.displayLabel})"
          },
          createdAtMillis = motion.updatedAtMillis,
          updatedAtMillis = motion.updatedAtMillis,
          metrics = EngagementMetricsSnapshot(
            availability = AnalyticsMetricAvailability.DataUnavailable,
            availabilityNote = "Motion project — telemetry becomes available after official publishing & API sync.",
          ),
        )
      }

    return (jobItems + exportItems + motionItems).sortedByDescending { it.updatedAtMillis }
  }

  private fun PublishingJobRecord.toContentPerformanceItem(
    platformStates: List<SocialPlatformAccountState>,
    remote: RemoteAnalyticsSnapshot,
  ): ContentPerformanceItem {
    val isConnected = platformStates.firstOrNull { it.platform == platform }?.isConnected == true
    val remoteMetrics = remote.contentMetricsByJobId[id]
    val fallbackAvailability = when {
      status != PublishingHistoryStatus.Published -> AnalyticsMetricAvailability.DataUnavailable
      !isConnected -> AnalyticsMetricAvailability.NotConnected
      !analyticsBackend.config.isEnabledFor(platform) -> AnalyticsMetricAvailability.SetupRequired
      else -> AnalyticsMetricAvailability.DataUnavailable
    }
    val fallbackNote = when {
      status != PublishingHistoryStatus.Published ->
        "Status is ${status.displayLabel}. Official engagement telemetry applies after publishing succeeds."
      !isConnected ->
        "${platform.displayName} account is currently disconnected."
      !analyticsBackend.config.isEnabledFor(platform) ->
        "${platform.displayName} Analytics API is not configured on backend."
      else ->
        "No live metrics returned yet for this post."
    }

    return ContentPerformanceItem(
      id = id,
      title = postTitle,
      platform = platform,
      accountHandle = accountHandle,
      status = status,
      sourceKindLabel = sourceKind.displayLabel,
      aspectRatioLabel = aspectRatio.label,
      mediaFilePath = mediaFilePath,
      mediaUriString = mediaUriString,
      remotePlatformPostId = remotePlatformPostId,
      remotePostUrl = remotePostUrl,
      statusDetail = statusDetail,
      createdAtMillis = createdAtMillis,
      updatedAtMillis = updatedAtMillis,
      metrics = remoteMetrics ?: EngagementMetricsSnapshot(
        availability = fallbackAvailability,
        availabilityNote = fallbackNote,
      ),
    )
  }

  private fun buildEngagementTrends(
    dateRange: AnalyticsDateRange,
    nowMillis: Long,
    jobs: List<PublishingJobRecord>,
    remote: RemoteAnalyticsSnapshot,
  ): List<EngagementTrendPoint> {
    val bucketCount = 6
    val totalWindowDays = dateRange.daysWindow ?: 30
    val stepMillis = (totalWindowDays.toLong() * 86_400_000L) / bucketCount
    val startMillis = nowMillis - totalWindowDays.toLong() * 86_400_000L
    val fmt = SimpleDateFormat("MMM d", Locale.US)

    return (0 until bucketCount).map { idx ->
      val bucketStart = startMillis + idx * stepMillis
      val bucketEnd = if (idx == bucketCount - 1) nowMillis + 1L else bucketStart + stepMillis
      val bucketJobs = jobs.filter { it.updatedAtMillis in bucketStart until bucketEnd }
      val viewsList = bucketJobs.mapNotNull { remote.contentMetricsByJobId[it.id]?.views }
      val likesList = bucketJobs.mapNotNull { remote.contentMetricsByJobId[it.id]?.likes }

      EngagementTrendPoint(
        label = fmt.format(Date(bucketStart)),
        publishedCount = bucketJobs.count { it.status == PublishingHistoryStatus.Published },
        scheduledCount = bucketJobs.count { it.status == PublishingHistoryStatus.Scheduled },
        views = if (viewsList.isNotEmpty()) viewsList.sum() else null,
        likes = if (likesList.isNotEmpty()) likesList.sum() else null,
      )
    }
  }

  private data class FilterSelection(
    val range: AnalyticsDateRange,
    val platform: AnalyticsPlatformFilter,
    val section: AnalyticsContentSection,
    val detailId: String?,
  )

  private data class SyncBannerState(
    val isSyncing: Boolean,
    val feedbackMessage: String?,
    val errorMessage: String?,
  )

  private data class LocalAnalyticsData(
    val publishingJobs: List<PublishingJobRecord>,
    val scheduledJobs: List<ScheduledPostJob>,
    val motionProjects: List<MotionPostProject>,
    val exportedVideos: List<ExportedVideoRecord>,
    val platformStates: List<SocialPlatformAccountState>,
  )
}
