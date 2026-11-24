package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.editor.InMemoryVideoEditorRepository
import com.example.data.editor.VideoEditorRepository
import com.example.data.factory.InMemoryVideoFactoryRepository
import com.example.data.factory.VideoFactoryRepository
import com.example.data.oauth.SocialAccountsRepository
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.phase78.Phase78Repository
import com.example.data.source.ContentSourceRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.AccentTone
import com.example.model.ActivityItem
import com.example.model.AppDestination
import com.example.model.AppSettingsState
import com.example.model.ConnectionStatus
import com.example.model.DashboardMetric
import com.example.model.OAuthLaunchRequest
import com.example.model.PermissionPolicy
import com.example.model.PlatformConnectionPhase
import com.example.model.PrimaryTab
import com.example.model.ProjectItem
import com.example.model.SocialAccountSummary
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.model.SourceDashboardStats
import com.example.model.StudioModuleSpec
import com.example.model.StudioModuleType
import com.example.model.VideoFactorySection
import com.example.security.LocalStorageSummary
import com.example.security.SecuritySanitizer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ZuzuUiState(
  val destination: AppDestination = AppDestination.Tab(PrimaryTab.Dashboard),
  val socialAccounts: List<SocialAccountSummary> = defaultSocialAccounts(),
  val platformAccountStates: List<SocialPlatformAccountState> = emptyList(),
  val sourceStats: SourceDashboardStats = SourceDashboardStats(),
  val metrics: List<DashboardMetric> = defaultMetrics(),
  val recentActivity: List<ActivityItem> = defaultActivity(),
  val studioModules: List<StudioModuleSpec> = defaultStudioModules(),
  val projects: List<ProjectItem> = emptyList(),
  val settings: AppSettingsState = AppSettingsState(),
  val storageSummary: LocalStorageSummary = LocalStorageSummary(),
  val storageActionMessage: String? = null,
) {
  val activeTab: PrimaryTab
    get() = when (destination) {
      is AppDestination.Tab -> destination.tab
      is AppDestination.ModuleIntro -> destination.returnTab
      is AppDestination.SocialAccounts -> destination.returnTab
      is AppDestination.AiVideoFactory -> destination.returnTab
      is AppDestination.MotionStudio -> destination.returnTab
      is AppDestination.ApprovalCenter -> destination.returnTab
      is AppDestination.SchedulerAutomation -> destination.returnTab
      is AppDestination.PublishingCenter -> destination.returnTab
      is AppDestination.AnalyticsDashboard -> destination.returnTab
    }

  val connectedPlatformCount: Int
    get() = platformAccountStates.count { it.isConnected }
}

class ZuzuAppViewModel(
  private val socialAccountsRepository: SocialAccountsRepository = SocialAccountsRepository(),
  private val contentSourceRepository: ContentSourceRepository = InMemoryContentSourceRepository(),
  private val videoFactoryRepository: VideoFactoryRepository = InMemoryVideoFactoryRepository(),
  private val videoEditorRepository: VideoEditorRepository = InMemoryVideoEditorRepository(),
  private val phase78Repository: Phase78Repository = InMemoryPhase78Repository(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    ZuzuUiState(
      platformAccountStates = socialAccountsRepository.platformStates.value,
      socialAccounts = mapToSummaries(socialAccountsRepository.platformStates.value),
    ),
  )
  val uiState: StateFlow<ZuzuUiState> = _uiState.asStateFlow()

  val pendingOAuthLaunch: StateFlow<OAuthLaunchRequest?> =
    socialAccountsRepository.launchRequest

  init {
    viewModelScope.launch {
      socialAccountsRepository.platformStates.collect { platformStates ->
        _uiState.update { state ->
          state.copy(
            platformAccountStates = platformStates,
            socialAccounts = mapToSummaries(platformStates),
          )
        }
      }
    }
    viewModelScope.launch {
      combine(
        contentSourceRepository.mediaEntries,
        contentSourceRepository.creatorSources,
      ) { media, creators ->
        val verifiedMedia = media.count { PermissionPolicy.isVerifiedStatus(it.permission.status) }
        val pendingMedia = media.size - verifiedMedia
        val verifiedCreators = creators.count { PermissionPolicy.isVerifiedStatus(it.permission.status) }
        val pendingCreators = creators.size - verifiedCreators
        SourceDashboardStats(
          totalMediaFiles = media.size,
          uploadedVideos = media.count { it.mediaType == com.example.model.MediaType.Video },
          uploadedImages = media.count { it.mediaType == com.example.model.MediaType.Image },
          audioFiles = media.count { it.mediaType == com.example.model.MediaType.Audio },
          savedUrls = media.count { it.mediaType == com.example.model.MediaType.ExternalUrl },
          creatorSources = creators.size,
          permissionVerified = verifiedMedia + verifiedCreators,
          permissionPending = pendingMedia + pendingCreators,
        )
      }.collect { stats ->
        _uiState.update { state ->
          state.copy(sourceStats = stats)
        }
      }
    }
    viewModelScope.launch {
      videoFactoryRepository.projects.collect { videoProjects ->
        val mapped = videoProjects.map { vp ->
          ProjectItem(
            id = vp.id,
            title = vp.topic,
            typeLabel = "${vp.videoType.displayLabel} • ${vp.language.displayLabel} (${vp.aspectRatio.label})",
            statusLabel = vp.stage.label,
          )
        }
        _uiState.update { state ->
          state.copy(
            projects = mapped,
            metrics = state.metrics.map { metric ->
              if (metric.id == "total_projects") metric.copy(count = mapped.size) else metric
            },
          )
        }
      }
    }
    viewModelScope.launch {
      videoEditorRepository.exportedVideos.collect { exports ->
        _uiState.update { state ->
          state.copy(
            metrics = state.metrics.map { metric ->
              if (metric.id == "videos_created") metric.copy(count = exports.size) else metric
            },
          )
        }
      }
    }
    viewModelScope.launch {
      phase78Repository.motionProjects.collect { motions ->
        _uiState.update { state ->
          state.copy(
            metrics = state.metrics.map { metric ->
              if (metric.id == "motion_posts_created") metric.copy(count = motions.size) else metric
            },
          )
        }
      }
    }
    viewModelScope.launch {
      phase78Repository.scheduledJobs.collect { jobs ->
        val activeCount = jobs.count {
          it.status != com.example.model.ScheduledJobStatus.Cancelled
        }
        _uiState.update { state ->
          state.copy(
            metrics = state.metrics.map { metric ->
              if (metric.id == "scheduled_posts") metric.copy(count = activeCount) else metric
            },
          )
        }
      }
    }
  }

  fun selectTab(tab: PrimaryTab) {
    _uiState.update { it.copy(destination = AppDestination.Tab(tab)) }
  }

  fun openModuleIntro(
    moduleType: StudioModuleType,
    fromTab: PrimaryTab = _uiState.value.activeTab,
  ) {
    _uiState.update {
      it.copy(
        destination = AppDestination.ModuleIntro(
          moduleType = moduleType,
          returnTab = fromTab,
        ),
      )
    }
  }

  fun openVideoFactory(
    fromTab: PrimaryTab = _uiState.value.activeTab,
    initialSection: VideoFactorySection = VideoFactorySection.Dashboard,
    projectId: String? = null,
  ) {
    _uiState.update {
      it.copy(
        destination = AppDestination.AiVideoFactory(
          returnTab = fromTab,
          initialSection = initialSection,
          projectId = projectId,
        ),
      )
    }
  }

  fun openMotionStudio(
    fromTab: PrimaryTab = _uiState.value.activeTab,
    projectId: String? = null,
  ) {
    _uiState.update {
      it.copy(
        destination = AppDestination.MotionStudio(
          returnTab = fromTab,
          projectId = projectId,
        ),
      )
    }
  }

  fun openApprovalCenter(
    fromTab: PrimaryTab = _uiState.value.activeTab,
  ) {
    _uiState.update {
      it.copy(destination = AppDestination.ApprovalCenter(returnTab = fromTab))
    }
  }

  fun openSchedulerAutomation(
    fromTab: PrimaryTab = _uiState.value.activeTab,
    showCloudStatusFirst: Boolean = false,
  ) {
    _uiState.update {
      it.copy(
        destination = AppDestination.SchedulerAutomation(
          returnTab = fromTab,
          showCloudStatusFirst = showCloudStatusFirst,
        ),
      )
    }
  }

  fun openPublishingCenter(
    fromTab: PrimaryTab = _uiState.value.activeTab,
    initialApprovalId: String? = null,
    initialSectionName: String? = null,
  ) {
    _uiState.update {
      it.copy(
        destination = AppDestination.PublishingCenter(
          returnTab = fromTab,
          initialApprovalId = initialApprovalId,
          initialSectionName = initialSectionName,
        ),
      )
    }
  }

  fun openSocialAccounts(
    fromTab: PrimaryTab = _uiState.value.activeTab,
  ) {
    _uiState.update {
      it.copy(destination = AppDestination.SocialAccounts(returnTab = fromTab))
    }
  }

  fun openAnalyticsDashboard(
    fromTab: PrimaryTab = _uiState.value.activeTab,
    initialContentJobId: String? = null,
  ) {
    _uiState.update {
      it.copy(
        destination = AppDestination.AnalyticsDashboard(
          returnTab = fromTab,
          initialContentJobId = initialContentJobId,
        ),
      )
    }
  }

  fun navigateBack() {
    when (val current = _uiState.value.destination) {
      is AppDestination.ModuleIntro -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.SocialAccounts -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.AiVideoFactory -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.MotionStudio -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.ApprovalCenter -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.SchedulerAutomation -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.PublishingCenter -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.AnalyticsDashboard -> {
        _uiState.update { it.copy(destination = AppDestination.Tab(current.returnTab)) }
      }
      is AppDestination.Tab -> Unit
    }
  }

  fun connectPlatform(platform: SocialPlatform) {
    viewModelScope.launch {
      socialAccountsRepository.connectPlatform(platform)
    }
  }

  fun disconnectAccount(platform: SocialPlatform, accountId: String) {
    viewModelScope.launch {
      socialAccountsRepository.disconnectAccount(platform, accountId)
    }
  }

  fun disconnectPlatform(platform: SocialPlatform) {
    viewModelScope.launch {
      socialAccountsRepository.disconnectAll(platform)
    }
  }

  fun resetPlatformStatus(platform: SocialPlatform) {
    socialAccountsRepository.resetStatus(platform)
  }

  fun consumeOAuthLaunch() {
    socialAccountsRepository.consumeLaunchRequest()
  }

  fun toggleAmbientGlow(enabled: Boolean) {
    _uiState.update { state ->
      state.copy(settings = state.settings.copy(ambientGlowEnabled = enabled))
    }
  }

  fun selectLanguage(language: String) {
    _uiState.update { state ->
      state.copy(settings = state.settings.copy(selectedLanguage = language))
    }
  }

  fun toggleWorkflowAlerts(enabled: Boolean) {
    _uiState.update { state ->
      state.copy(settings = state.settings.copy(workflowAlertsEnabled = enabled))
    }
  }

  fun toggleScheduleReminders(enabled: Boolean) {
    _uiState.update { state ->
      state.copy(settings = state.settings.copy(scheduleRemindersEnabled = enabled))
    }
  }

  fun moduleSpecFor(type: StudioModuleType): StudioModuleSpec {
    return _uiState.value.studioModules.first { it.type == type }
  }

  fun refreshStorageSummary(exportsDir: File, cacheDir: File, databaseDir: File?) {
    viewModelScope.launch {
      val summary = withContext(Dispatchers.IO) {
        LocalStorageSummary(
          exportedFilesCount = SecuritySanitizer.countFilesInDirectory(exportsDir),
          exportsSizeBytes = SecuritySanitizer.directorySizeBytes(exportsDir),
          cacheSizeBytes = SecuritySanitizer.directorySizeBytes(cacheDir),
          databaseSizeBytes = SecuritySanitizer.directorySizeBytes(databaseDir),
        )
      }
      _uiState.update { it.copy(storageSummary = summary) }
    }
  }

  fun clearTemporaryCache(exportsDir: File, cacheDir: File, databaseDir: File?) {
    viewModelScope.launch {
      val (removed, summary) = withContext(Dispatchers.IO) {
        val count = SecuritySanitizer.clearDirectoryContentsSafely(cacheDir)
        val refreshed = LocalStorageSummary(
          exportedFilesCount = SecuritySanitizer.countFilesInDirectory(exportsDir),
          exportsSizeBytes = SecuritySanitizer.directorySizeBytes(exportsDir),
          cacheSizeBytes = SecuritySanitizer.directorySizeBytes(cacheDir),
          databaseSizeBytes = SecuritySanitizer.directorySizeBytes(databaseDir),
        )
        count to refreshed
      }
      _uiState.update {
        it.copy(
          storageSummary = summary,
          storageActionMessage = if (removed > 0) {
            "Cleared $removed temporary cache item(s). Saved projects and exports preserved."
          } else {
            "Temporary cache is already clean. Saved projects and exports preserved."
          },
        )
      }
    }
  }

  fun clearStorageActionMessage() {
    _uiState.update { it.copy(storageActionMessage = null) }
  }
}

private fun mapToSummaries(
  platformStates: List<SocialPlatformAccountState>,
): List<SocialAccountSummary> {
  if (platformStates.isEmpty()) return defaultSocialAccounts()
  return platformStates.map { state ->
    val summaryStatus = when (state.phase) {
      PlatformConnectionPhase.Connected -> ConnectionStatus.Connected
      PlatformConnectionPhase.Error -> ConnectionStatus.Error
      PlatformConnectionPhase.NotConnected,
      PlatformConnectionPhase.Connecting,
      PlatformConnectionPhase.SetupRequired,
      -> ConnectionStatus.NotConnected
    }
    val subtitle = when {
      state.connectedAccounts.size > 1 ->
        "${state.connectedAccounts.size} accounts connected"
      state.connectedAccounts.size == 1 ->
        state.connectedAccounts.first().handle
      state.phase == PlatformConnectionPhase.SetupRequired ->
        "OAuth backend setup required"
      state.phase == PlatformConnectionPhase.Connecting ->
        "Checking authorization..."
      else -> when (state.platform) {
        SocialPlatform.YouTube -> "Google OAuth ready for setup"
        SocialPlatform.Facebook -> "Meta OAuth ready for setup"
        SocialPlatform.TikTok -> "TikTok Login Kit ready for setup"
      }
    }
    SocialAccountSummary(
      id = state.platform.id,
      platformName = state.platform.displayName,
      handlePlaceholder = subtitle,
      status = summaryStatus,
    )
  }
}

private fun defaultSocialAccounts(): List<SocialAccountSummary> = listOf(
  SocialAccountSummary(
    id = "youtube",
    platformName = "YouTube",
    handlePlaceholder = "Google OAuth ready for setup",
    status = ConnectionStatus.NotConnected,
  ),
  SocialAccountSummary(
    id = "facebook",
    platformName = "Facebook",
    handlePlaceholder = "Meta OAuth ready for setup",
    status = ConnectionStatus.NotConnected,
  ),
  SocialAccountSummary(
    id = "tiktok",
    platformName = "TikTok",
    handlePlaceholder = "TikTok Login Kit ready for setup",
    status = ConnectionStatus.NotConnected,
  ),
)

private fun defaultMetrics(): List<DashboardMetric> = listOf(
  DashboardMetric(
    id = "total_projects",
    label = "Total Projects",
    count = 0,
    accentTone = AccentTone.Purple,
  ),
  DashboardMetric(
    id = "videos_created",
    label = "Videos Created",
    count = 0,
    accentTone = AccentTone.ElectricBlue,
  ),
  DashboardMetric(
    id = "motion_posts_created",
    label = "Motion Posts Created",
    count = 0,
    accentTone = AccentTone.ElectricBlue,
  ),
  DashboardMetric(
    id = "scheduled_posts",
    label = "Scheduled Posts",
    count = 0,
    accentTone = AccentTone.Purple,
  ),
)

private fun defaultActivity(): List<ActivityItem> = listOf(
  ActivityItem(
    id = "init_foundation",
    title = "Zuzu Social AI Initialized",
    description = "Phase 01 app foundation and Phase 02 Social Accounts OAuth module ready.",
    timeLabel = "Today",
  ),
)

private fun defaultStudioModules(): List<StudioModuleSpec> = listOf(
  StudioModuleSpec(
    type = StudioModuleType.AiVideoFactory,
    title = "AI Video Factory",
    shortDescription = "Automated script-to-short video pipeline for multi-channel publishing.",
    overviewDescription =
      "AI Video Factory will transform ideas, scripts, and source clips into vertical and widescreen social videos tailored for YouTube Shorts, Reels, and TikTok.",
    plannedSteps = listOf(
      "Choose topic, script brief, or source media",
      "Generate AI voiceover, captions, and pacing",
      "Preview, approve, and queue to Projects",
    ),
    accentTone = AccentTone.ElectricBlue,
  ),
  StudioModuleSpec(
    type = StudioModuleType.AutoMotionPostStudio,
    title = "Auto Motion Post Studio",
    shortDescription = "Dynamic branded motion graphics and social carousels generated cleanly.",
    overviewDescription =
      "Auto Motion Post Studio will design animated promo cards, kinetic typography posts, and visual carousels aligned with your brand palette.",
    plannedSteps = listOf(
      "Select campaign template and aspect ratio",
      "Customize AI headline, motion style, and accents",
      "Export motion asset directly to your Project library",
    ),
    accentTone = AccentTone.Purple,
  ),
)
