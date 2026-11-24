package com.example.model

enum class PrimaryTab(val testTag: String) {
  Dashboard("nav_dashboard"),
  Sources("nav_sources"),
  Studio("nav_studio"),
  Projects("nav_projects"),
  Settings("nav_settings"),
}

enum class StudioModuleType {
  AiVideoFactory,
  AutoMotionPostStudio,
}

sealed interface AppDestination {
  data class Tab(val tab: PrimaryTab) : AppDestination
  data class ModuleIntro(
    val moduleType: StudioModuleType,
    val returnTab: PrimaryTab,
  ) : AppDestination
  data class SocialAccounts(
    val returnTab: PrimaryTab,
  ) : AppDestination
  data class AiVideoFactory(
    val returnTab: PrimaryTab,
    val initialSection: VideoFactorySection = VideoFactorySection.Dashboard,
    val projectId: String? = null,
  ) : AppDestination
  data class MotionStudio(
    val returnTab: PrimaryTab,
    val projectId: String? = null,
  ) : AppDestination
  data class ApprovalCenter(
    val returnTab: PrimaryTab,
  ) : AppDestination
  data class SchedulerAutomation(
    val returnTab: PrimaryTab,
    val showCloudStatusFirst: Boolean = false,
  ) : AppDestination
  data class PublishingCenter(
    val returnTab: PrimaryTab,
    val initialApprovalId: String? = null,
    val initialSectionName: String? = null,
  ) : AppDestination
  data class AnalyticsDashboard(
    val returnTab: PrimaryTab,
    val initialContentJobId: String? = null,
  ) : AppDestination
}

enum class ConnectionStatus {
  NotConnected,
  Connected,
  Error,
}

data class SocialAccountSummary(
  val id: String,
  val platformName: String,
  val handlePlaceholder: String,
  val status: ConnectionStatus,
)

enum class AccentTone {
  Purple,
  ElectricBlue,
}

data class DashboardMetric(
  val id: String,
  val label: String,
  val count: Int,
  val accentTone: AccentTone,
)

data class ActivityItem(
  val id: String,
  val title: String,
  val description: String,
  val timeLabel: String,
)

data class StudioModuleSpec(
  val type: StudioModuleType,
  val title: String,
  val shortDescription: String,
  val overviewDescription: String,
  val plannedSteps: List<String>,
  val accentTone: AccentTone,
)

data class ProjectItem(
  val id: String,
  val title: String,
  val typeLabel: String,
  val statusLabel: String,
)

data class AppSettingsState(
  val ambientGlowEnabled: Boolean = true,
  val selectedLanguage: String = "English (US)",
  val workflowAlertsEnabled: Boolean = true,
  val scheduleRemindersEnabled: Boolean = false,
)
