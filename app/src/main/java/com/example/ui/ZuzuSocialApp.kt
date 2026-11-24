package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.oauth.OAuthUrlValidator
import com.example.model.AppDestination
import com.example.model.PrimaryTab
import com.example.ui.components.ZuzuBackground
import com.example.ui.components.ZuzuFloatingBottomBar
import com.example.ui.components.ZuzuNavRail
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SocialAccountsScreen
import com.example.ui.screens.StudioIntroScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.automation.ApprovalCenterScreen
import com.example.ui.screens.automation.SchedulerAutomationScreen
import com.example.ui.screens.factory.VideoFactoryScreen
import com.example.ui.screens.motion.MotionStudioScreen
import com.example.ui.screens.publishing.PublishingCenterScreen
import com.example.ui.screens.sources.ContentSourcesScreen
import com.example.viewmodel.AnalyticsUiState
import com.example.viewmodel.AnalyticsViewModel
import com.example.viewmodel.ApprovalSchedulerUiState
import com.example.viewmodel.ApprovalSchedulerViewModel
import com.example.viewmodel.ContentSourceUiState
import com.example.viewmodel.ContentSourceViewModel
import com.example.viewmodel.MotionStudioUiState
import com.example.viewmodel.MotionStudioViewModel
import com.example.viewmodel.PublishingCenterUiState
import com.example.viewmodel.PublishingCenterViewModel
import com.example.viewmodel.VideoEditorUiState
import com.example.viewmodel.VideoEditorViewModel
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel
import com.example.viewmodel.VoiceVisualUiState
import com.example.viewmodel.VoiceVisualViewModel
import com.example.viewmodel.ZuzuAppViewModel
import com.example.viewmodel.ZuzuUiState
import com.example.viewmodel.ZuzuViewModelFactory

@Composable
fun ZuzuSocialApp() {
  val context = LocalContext.current
  val factory = remember(context.applicationContext) {
    ZuzuViewModelFactory(context.applicationContext)
  }
  val viewModel: ZuzuAppViewModel = viewModel(factory = factory)
  val contentSourceViewModel: ContentSourceViewModel = viewModel(factory = factory)
  val videoFactoryViewModel: VideoFactoryViewModel = viewModel(factory = factory)
  val voiceVisualViewModel: VoiceVisualViewModel = viewModel(factory = factory)
  val videoEditorViewModel: VideoEditorViewModel = viewModel(factory = factory)
  val motionStudioViewModel: MotionStudioViewModel = viewModel(factory = factory)
  val approvalSchedulerViewModel: ApprovalSchedulerViewModel = viewModel(factory = factory)
  val publishingCenterViewModel: PublishingCenterViewModel = viewModel(factory = factory)
  val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val contentSourceUiState by contentSourceViewModel.uiState.collectAsStateWithLifecycle()
  val videoFactoryUiState by videoFactoryViewModel.uiState.collectAsStateWithLifecycle()
  val voiceVisualUiState by voiceVisualViewModel.uiState.collectAsStateWithLifecycle()
  val videoEditorUiState by videoEditorViewModel.uiState.collectAsStateWithLifecycle()
  val motionStudioUiState by motionStudioViewModel.uiState.collectAsStateWithLifecycle()
  val approvalSchedulerUiState by approvalSchedulerViewModel.uiState.collectAsStateWithLifecycle()
  val publishingCenterUiState by publishingCenterViewModel.uiState.collectAsStateWithLifecycle()
  val analyticsUiState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
  val pendingLaunch by viewModel.pendingOAuthLaunch.collectAsStateWithLifecycle()

  LaunchedEffect(pendingLaunch) {
    val request = pendingLaunch ?: return@LaunchedEffect
    if (OAuthUrlValidator.isValidAuthorizationUrl(request.authorizationUrl)) {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.authorizationUrl))
      runCatching { context.startActivity(intent) }
    }
    viewModel.consumeOAuthLaunch()
  }

  val isSecondaryScreen =
    uiState.destination is AppDestination.ModuleIntro ||
      uiState.destination is AppDestination.SocialAccounts ||
      uiState.destination is AppDestination.AiVideoFactory ||
      uiState.destination is AppDestination.MotionStudio ||
      uiState.destination is AppDestination.ApprovalCenter ||
      uiState.destination is AppDestination.SchedulerAutomation ||
      uiState.destination is AppDestination.PublishingCenter ||
      uiState.destination is AppDestination.AnalyticsDashboard

  ZuzuBackground(glowEnabled = uiState.settings.ambientGlowEnabled) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      if (useNavRail) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        ) {
          if (!isSecondaryScreen) {
            ZuzuNavRail(
              activeTab = uiState.activeTab,
              onSelectTab = viewModel::selectTab,
            )
          }
          Box(modifier = Modifier.weight(1f)) {
            DestinationContent(
              destination = uiState.destination,
              viewModel = viewModel,
              uiState = uiState,
              contentSourceViewModel = contentSourceViewModel,
              contentSourceUiState = contentSourceUiState,
              videoFactoryViewModel = videoFactoryViewModel,
              videoFactoryUiState = videoFactoryUiState,
              voiceVisualViewModel = voiceVisualViewModel,
              voiceVisualUiState = voiceVisualUiState,
              videoEditorViewModel = videoEditorViewModel,
              videoEditorUiState = videoEditorUiState,
              motionStudioViewModel = motionStudioViewModel,
              motionStudioUiState = motionStudioUiState,
              approvalSchedulerViewModel = approvalSchedulerViewModel,
              approvalSchedulerUiState = approvalSchedulerUiState,
              publishingCenterViewModel = publishingCenterViewModel,
              publishingCenterUiState = publishingCenterUiState,
              analyticsViewModel = analyticsViewModel,
              analyticsUiState = analyticsUiState,
              hasFloatingBottomBar = false,
            )
          }
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        ) {
          DestinationContent(
            destination = uiState.destination,
            viewModel = viewModel,
            uiState = uiState,
            contentSourceViewModel = contentSourceViewModel,
            contentSourceUiState = contentSourceUiState,
            videoFactoryViewModel = videoFactoryViewModel,
            videoFactoryUiState = videoFactoryUiState,
            voiceVisualViewModel = voiceVisualViewModel,
            voiceVisualUiState = voiceVisualUiState,
            videoEditorViewModel = videoEditorViewModel,
            videoEditorUiState = videoEditorUiState,
            motionStudioViewModel = motionStudioViewModel,
            motionStudioUiState = motionStudioUiState,
            approvalSchedulerViewModel = approvalSchedulerViewModel,
            approvalSchedulerUiState = approvalSchedulerUiState,
            publishingCenterViewModel = publishingCenterViewModel,
            publishingCenterUiState = publishingCenterUiState,
            analyticsViewModel = analyticsViewModel,
            analyticsUiState = analyticsUiState,
            hasFloatingBottomBar = !isSecondaryScreen,
          )
          if (!isSecondaryScreen) {
            ZuzuFloatingBottomBar(
              activeTab = uiState.activeTab,
              onSelectTab = viewModel::selectTab,
              modifier = Modifier.align(Alignment.BottomCenter),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DestinationContent(
  destination: AppDestination,
  viewModel: ZuzuAppViewModel,
  uiState: ZuzuUiState,
  contentSourceViewModel: ContentSourceViewModel,
  contentSourceUiState: ContentSourceUiState,
  videoFactoryViewModel: VideoFactoryViewModel,
  videoFactoryUiState: VideoFactoryUiState,
  voiceVisualViewModel: VoiceVisualViewModel,
  voiceVisualUiState: VoiceVisualUiState,
  videoEditorViewModel: VideoEditorViewModel,
  videoEditorUiState: VideoEditorUiState,
  motionStudioViewModel: MotionStudioViewModel,
  motionStudioUiState: MotionStudioUiState,
  approvalSchedulerViewModel: ApprovalSchedulerViewModel,
  approvalSchedulerUiState: ApprovalSchedulerUiState,
  publishingCenterViewModel: PublishingCenterViewModel,
  publishingCenterUiState: PublishingCenterUiState,
  analyticsViewModel: AnalyticsViewModel,
  analyticsUiState: AnalyticsUiState,
  hasFloatingBottomBar: Boolean,
) {
  val bottomInsetPadding = if (hasFloatingBottomBar) 108.dp else 20.dp

  AnimatedContent(
    targetState = destination,
    transitionSpec = {
      fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
        fadeOut(animationSpec = tween(durationMillis = 150))
    },
    label = "ZuzuDestinationTransition",
  ) { target ->
    when (target) {
      is AppDestination.Tab -> when (target.tab) {
        PrimaryTab.Dashboard -> DashboardScreen(
          uiState = uiState,
          analyticsUiState = analyticsUiState,
          bottomContentPadding = bottomInsetPadding,
          onOpenModuleIntro = { moduleType ->
            when (moduleType) {
              com.example.model.StudioModuleType.AiVideoFactory ->
                viewModel.openVideoFactory(fromTab = PrimaryTab.Dashboard)
              com.example.model.StudioModuleType.AutoMotionPostStudio ->
                viewModel.openMotionStudio(fromTab = PrimaryTab.Dashboard)
            }
          },
          onOpenSocialAccounts = {
            viewModel.openSocialAccounts(fromTab = PrimaryTab.Dashboard)
          },
          onOpenContentSources = {
            viewModel.selectTab(PrimaryTab.Sources)
          },
          onOpenApprovalCenter = {
            viewModel.openApprovalCenter(fromTab = PrimaryTab.Dashboard)
          },
          onOpenScheduler = {
            viewModel.openSchedulerAutomation(fromTab = PrimaryTab.Dashboard)
          },
          onOpenPublishingCenter = {
            viewModel.openPublishingCenter(fromTab = PrimaryTab.Dashboard)
          },
          onOpenAnalytics = {
            viewModel.openAnalyticsDashboard(fromTab = PrimaryTab.Dashboard)
          },
        )
        PrimaryTab.Sources -> ContentSourcesScreen(
          uiState = contentSourceUiState,
          viewModel = contentSourceViewModel,
          glowEnabled = uiState.settings.ambientGlowEnabled,
          bottomContentPadding = bottomInsetPadding,
        )
        PrimaryTab.Studio -> StudioScreen(
          uiState = uiState,
          bottomContentPadding = bottomInsetPadding,
          onOpenModuleIntro = { moduleType ->
            when (moduleType) {
              com.example.model.StudioModuleType.AiVideoFactory ->
                viewModel.openVideoFactory(fromTab = PrimaryTab.Studio)
              com.example.model.StudioModuleType.AutoMotionPostStudio ->
                viewModel.openMotionStudio(fromTab = PrimaryTab.Studio)
            }
          },
          onOpenApprovalCenter = {
            viewModel.openApprovalCenter(fromTab = PrimaryTab.Studio)
          },
          onOpenScheduler = {
            viewModel.openSchedulerAutomation(
              fromTab = PrimaryTab.Studio,
              showCloudStatusFirst = false,
            )
          },
          onOpenCloudStatus = {
            viewModel.openSchedulerAutomation(
              fromTab = PrimaryTab.Studio,
              showCloudStatusFirst = true,
            )
          },
          onOpenPublishingCenter = {
            viewModel.openPublishingCenter(fromTab = PrimaryTab.Studio)
          },
          onOpenAnalytics = {
            viewModel.openAnalyticsDashboard(fromTab = PrimaryTab.Studio)
          },
        )
        PrimaryTab.Projects -> ProjectsScreen(
          uiState = uiState,
          bottomContentPadding = bottomInsetPadding,
          onExploreStudio = { viewModel.selectTab(PrimaryTab.Studio) },
          onOpenProject = { project ->
            videoFactoryViewModel.openProject(
              projectId = project.id,
              targetSection = com.example.model.VideoFactorySection.Dashboard,
            )
            viewModel.openVideoFactory(
              fromTab = PrimaryTab.Projects,
              projectId = project.id,
            )
          },
          onOpenMotionStudio = {
            viewModel.openMotionStudio(fromTab = PrimaryTab.Projects)
          },
          onOpenApprovalCenter = {
            viewModel.openApprovalCenter(fromTab = PrimaryTab.Projects)
          },
          onOpenScheduler = {
            viewModel.openSchedulerAutomation(fromTab = PrimaryTab.Projects)
          },
          onOpenPublishingCenter = {
            viewModel.openPublishingCenter(fromTab = PrimaryTab.Projects)
          },
          onOpenAnalytics = {
            viewModel.openAnalyticsDashboard(fromTab = PrimaryTab.Projects)
          },
        )
        PrimaryTab.Settings -> SettingsScreen(
          uiState = uiState,
          bottomContentPadding = bottomInsetPadding,
          onToggleAmbientGlow = viewModel::toggleAmbientGlow,
          onSelectLanguage = viewModel::selectLanguage,
          onToggleWorkflowAlerts = viewModel::toggleWorkflowAlerts,
          onToggleScheduleReminders = viewModel::toggleScheduleReminders,
          onRefreshStorage = viewModel::refreshStorageSummary,
          onClearTemporaryCache = viewModel::clearTemporaryCache,
          onClearStorageMessage = viewModel::clearStorageActionMessage,
          onOpenSocialAccounts = {
            viewModel.openSocialAccounts(fromTab = PrimaryTab.Settings)
          },
          onOpenCloudStatus = {
            viewModel.openSchedulerAutomation(
              fromTab = PrimaryTab.Settings,
              showCloudStatusFirst = true,
            )
          },
          onOpenPublishingApiStatus = {
            viewModel.openPublishingCenter(
              fromTab = PrimaryTab.Settings,
              initialSectionName = com.example.viewmodel.PublishingCenterTab.BackendApi.name,
            )
          },
        )
      }

      is AppDestination.ModuleIntro -> StudioIntroScreen(
        module = viewModel.moduleSpecFor(target.moduleType),
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onBack = viewModel::navigateBack,
      )

      is AppDestination.SocialAccounts -> SocialAccountsScreen(
        uiState = uiState,
        onBack = viewModel::navigateBack,
        onConnectPlatform = viewModel::connectPlatform,
        onDisconnectAccount = viewModel::disconnectAccount,
        onDisconnectPlatform = viewModel::disconnectPlatform,
        onDismissStatus = viewModel::resetPlatformStatus,
      )

      is AppDestination.AiVideoFactory -> VideoFactoryScreen(
        uiState = videoFactoryUiState,
        voiceUiState = voiceVisualUiState,
        editorUiState = videoEditorUiState,
        mediaLibraryEntries = contentSourceUiState.allMediaEntries,
        viewModel = videoFactoryViewModel,
        voiceViewModel = voiceVisualViewModel,
        editorViewModel = videoEditorViewModel,
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onSendExportToApproval = { record ->
          approvalSchedulerViewModel.submitExportedVideoForApproval(record)
          viewModel.openApprovalCenter(fromTab = target.returnTab)
        },
        onBack = viewModel::navigateBack,
      )

      is AppDestination.MotionStudio -> MotionStudioScreen(
        uiState = motionStudioUiState,
        mediaLibraryEntries = contentSourceUiState.allMediaEntries,
        viewModel = motionStudioViewModel,
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onOpenApprovalCenter = {
          viewModel.openApprovalCenter(fromTab = target.returnTab)
        },
        onBack = viewModel::navigateBack,
      )

      is AppDestination.ApprovalCenter -> ApprovalCenterScreen(
        uiState = approvalSchedulerUiState,
        viewModel = approvalSchedulerViewModel,
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onOpenScheduler = {
          viewModel.openSchedulerAutomation(
            fromTab = target.returnTab,
            showCloudStatusFirst = false,
          )
        },
        onOpenPublishingCenter = { approvalId ->
          viewModel.openPublishingCenter(
            fromTab = target.returnTab,
            initialApprovalId = approvalId,
          )
        },
        onBack = viewModel::navigateBack,
      )

      is AppDestination.SchedulerAutomation -> SchedulerAutomationScreen(
        uiState = approvalSchedulerUiState,
        platformAccounts = uiState.platformAccountStates,
        showCloudStatusFirst = target.showCloudStatusFirst,
        viewModel = approvalSchedulerViewModel,
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onOpenApprovalCenter = {
          viewModel.openApprovalCenter(fromTab = target.returnTab)
        },
        onOpenSocialAccounts = {
          viewModel.openSocialAccounts(fromTab = target.returnTab)
        },
        onOpenPublishingCenter = {
          viewModel.openPublishingCenter(fromTab = target.returnTab)
        },
        onBack = viewModel::navigateBack,
      )

      is AppDestination.PublishingCenter -> PublishingCenterScreen(
        uiState = publishingCenterUiState,
        initialApprovalId = target.initialApprovalId,
        initialSectionName = target.initialSectionName,
        viewModel = publishingCenterViewModel,
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onOpenApprovalCenter = {
          viewModel.openApprovalCenter(fromTab = target.returnTab)
        },
        onOpenSocialAccounts = {
          viewModel.openSocialAccounts(fromTab = target.returnTab)
        },
        onOpenAnalytics = {
          viewModel.openAnalyticsDashboard(fromTab = target.returnTab)
        },
        onBack = viewModel::navigateBack,
      )

      is AppDestination.AnalyticsDashboard -> AnalyticsScreen(
        uiState = analyticsUiState,
        initialContentJobId = target.initialContentJobId,
        viewModel = analyticsViewModel,
        glowEnabled = uiState.settings.ambientGlowEnabled,
        onOpenSocialAccounts = {
          viewModel.openSocialAccounts(fromTab = target.returnTab)
        },
        onOpenPublishingCenter = {
          viewModel.openPublishingCenter(fromTab = target.returnTab)
        },
        onBack = viewModel::navigateBack,
      )
    }
  }
}
