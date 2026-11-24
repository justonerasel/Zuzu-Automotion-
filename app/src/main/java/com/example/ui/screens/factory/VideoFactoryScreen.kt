package com.example.ui.screens.factory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.AiServiceStatus
import com.example.model.ExportedVideoRecord
import com.example.model.MediaLibraryEntry
import com.example.model.VideoFactorySection
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.screens.editor.ExportedVideoLibrarySection
import com.example.ui.screens.editor.VideoEditorSection
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.AiSetupNotice
import com.example.viewmodel.ProviderSetupNotice
import com.example.viewmodel.VideoEditorUiState
import com.example.viewmodel.VideoEditorViewModel
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel
import com.example.viewmodel.VoiceVisualUiState
import com.example.viewmodel.VoiceVisualViewModel

@Composable
fun VideoFactoryScreen(
  uiState: VideoFactoryUiState,
  voiceUiState: VoiceVisualUiState,
  editorUiState: VideoEditorUiState,
  mediaLibraryEntries: List<MediaLibraryEntry>,
  viewModel: VideoFactoryViewModel,
  voiceViewModel: VoiceVisualViewModel,
  editorViewModel: VideoEditorViewModel,
  glowEnabled: Boolean,
  onSendExportToApproval: ((ExportedVideoRecord) -> Unit)? = null,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZuzuSecondaryTopBar(
          title = stringResource(R.string.vf_title),
          onBack = onBack,
          backButtonTag = "vf_back_button",
          badgeText = stringResource(R.string.vf_badge_active),
          badgeTint = ZuzuViolet,
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "Script • Voice • Visuals • Editor • MP4",
          title = stringResource(R.string.vf_title),
          subtitle = stringResource(R.string.vf_subtitle),
          icon = Icons.Rounded.MovieCreation,
          gradientColors = ZuzuHeroGradient,
        )
      }

      if (uiState.aiStatus is AiServiceStatus.SetupRequired) {
        item {
          ZuzuAlertBanner(
            message = stringResource(R.string.vf_ai_setup_banner),
            tint = ZuzuAmber,
            icon = Icons.Rounded.Info,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("vf_ai_setup_banner"),
          )
        }
      }

      item {
        FactorySectionStrip(
          activeSection = uiState.activeSection,
          onSelectSection = { section ->
            viewModel.selectSection(section)
            voiceViewModel.clearMessages()
            editorViewModel.clearMessages()
          },
        )
      }

      if (uiState.projects.isNotEmpty() && uiState.activeSection != VideoFactorySection.Configure) {
        item {
          ActiveProjectSelectorStrip(
            uiState = uiState,
            onSelectProject = { projectId ->
              viewModel.openProject(projectId, uiState.activeSection)
            },
          )
        }
      }

      item {
        val combinedFeedback = editorUiState.feedbackMessage
          ?: voiceUiState.feedbackMessage
          ?: uiState.feedbackMessage
        val combinedError = editorUiState.errorMessage
          ?: voiceUiState.errorMessage
          ?: uiState.errorMessage
        AnimatedVisibility(visible = !combinedFeedback.isNullOrBlank()) {
          ZuzuAlertBanner(
            message = combinedFeedback.orEmpty(),
            tint = ZuzuGreen,
            icon = Icons.Rounded.CheckCircle,
            onDismiss = {
              viewModel.clearMessages()
              voiceViewModel.clearMessages()
              editorViewModel.clearMessages()
            },
          )
        }
        AnimatedVisibility(visible = !combinedError.isNullOrBlank()) {
          ZuzuAlertBanner(
            message = combinedError.orEmpty(),
            tint = ZuzuError,
            icon = Icons.Rounded.ErrorOutline,
            onDismiss = {
              viewModel.clearMessages()
              voiceViewModel.clearMessages()
              editorViewModel.clearMessages()
            },
          )
        }
      }

      item {
        AnimatedContent(
          targetState = uiState.activeSection,
          transitionSpec = {
            fadeIn(tween(180)) togetherWith fadeOut(tween(120))
          },
          label = "VideoFactorySectionContent",
        ) { section ->
          when (section) {
            VideoFactorySection.Dashboard -> VideoFactoryDashboardSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.Configure -> VideoConfigSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.IdeaGenerator -> IdeaGeneratorSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.ResearchStudio -> ResearchStudioSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.ScriptWriter -> ScriptWriterSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.StoryboardStudio -> StoryboardStudioSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.VoiceStudio -> VoiceStudioSection(
              factoryUiState = uiState,
              voiceUiState = voiceUiState,
              factoryViewModel = viewModel,
              voiceViewModel = voiceViewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.VisualStudio -> VisualStudioSection(
              factoryUiState = uiState,
              voiceUiState = voiceUiState,
              factoryViewModel = viewModel,
              voiceViewModel = voiceViewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.AssetLibrary -> AssetLibrarySection(
              factoryUiState = uiState,
              voiceUiState = voiceUiState,
              voiceViewModel = voiceViewModel,
              onOpenVideoEditor = { viewModel.selectSection(VideoFactorySection.VideoEditor) },
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.VideoEditor -> VideoEditorSection(
              factoryUiState = uiState,
              studioAssets = voiceUiState.allAssets,
              mediaLibraryEntries = mediaLibraryEntries,
              editorUiState = editorUiState,
              factoryViewModel = viewModel,
              editorViewModel = editorViewModel,
              glowEnabled = glowEnabled,
            )
            VideoFactorySection.ExportedLibrary -> ExportedVideoLibrarySection(
              editorUiState = editorUiState,
              editorViewModel = editorViewModel,
              glowEnabled = glowEnabled,
              onSendToApproval = onSendExportToApproval,
            )
            VideoFactorySection.Projects -> VideoProjectsSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
          }
        }
      }
    }
  }

  uiState.setupNotice?.let { notice ->
    AiSetupRequiredDialog(
      notice = notice,
      onDismiss = viewModel::dismissSetupNotice,
    )
  }

  voiceUiState.setupNotice?.let { notice ->
    ProviderSetupRequiredDialog(
      notice = notice,
      onDismiss = voiceViewModel::dismissSetupNotice,
    )
  }
}

@Composable
private fun FactorySectionStrip(
  activeSection: VideoFactorySection,
  onSelectSection: (VideoFactorySection) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    VideoFactorySection.entries.forEach { section ->
      ZuzuPillChip(
        label = section.label,
        selected = activeSection == section,
        onClick = { onSelectSection(section) },
        gradientColors = listOf(ZuzuViolet, ZuzuPurple, ZuzuMagenta),
        leadingIcon = factorySectionIcon(section),
        modifier = Modifier.testTag(section.testTag),
      )
    }
  }
}

@Composable
private fun ActiveProjectSelectorStrip(
  uiState: VideoFactoryUiState,
  onSelectProject: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Active Video Project",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = ZuzuInkSecondary,
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      uiState.projects.forEach { project ->
        ZuzuPillChip(
          label = project.topic,
          selected = uiState.activeProject?.id == project.id,
          onClick = { onSelectProject(project.id) },
          gradientColors = listOf(ZuzuElectricBlue, ZuzuPurple),
        )
      }
    }
  }
}

@Composable
private fun AiSetupRequiredDialog(
  notice: AiSetupNotice,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ZuzuGradientIconBadge(
          icon = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          gradientColors = ZuzuVioletPinkGradient,
          size = 34.dp,
          iconSize = 18.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(notice.title, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = notice.message,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkPrimary,
        )
        Text(
          text = "Gemini Integration Checklist",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuPurple,
        )
        notice.setupSteps.forEachIndexed { idx, step ->
          Text(
            text = "${idx + 1}. $step",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.dialog_close), color = ZuzuPurple, fontWeight = FontWeight.Bold)
      }
    },
  )
}

@Composable
private fun ProviderSetupRequiredDialog(
  notice: ProviderSetupNotice,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ZuzuGradientIconBadge(
          icon = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          gradientColors = listOf(ZuzuPink, ZuzuMagenta),
          size = 34.dp,
          iconSize = 18.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("${notice.providerName} Setup Required", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = notice.message,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkPrimary,
        )
        if (notice.setupSteps.isNotEmpty()) {
          Text(
            text = "Provider Integration Checklist",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ZuzuPurple,
          )
          notice.setupSteps.forEachIndexed { idx, step ->
            Text(
              text = "${idx + 1}. $step",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.dialog_close), color = ZuzuPurple, fontWeight = FontWeight.Bold)
      }
    },
  )
}

private fun factorySectionIcon(section: VideoFactorySection): ImageVector = when (section) {
  VideoFactorySection.Dashboard -> Icons.Rounded.Dashboard
  VideoFactorySection.Configure -> Icons.Rounded.Tune
  VideoFactorySection.IdeaGenerator -> Icons.Rounded.Lightbulb
  VideoFactorySection.ResearchStudio -> Icons.Rounded.Science
  VideoFactorySection.ScriptWriter -> Icons.Rounded.Description
  VideoFactorySection.StoryboardStudio -> Icons.Rounded.ViewCarousel
  VideoFactorySection.VoiceStudio -> Icons.Rounded.RecordVoiceOver
  VideoFactorySection.VisualStudio -> Icons.Rounded.Palette
  VideoFactorySection.AssetLibrary -> Icons.Rounded.PermMedia
  VideoFactorySection.VideoEditor -> Icons.Rounded.Movie
  VideoFactorySection.ExportedLibrary -> Icons.Rounded.VideoLibrary
  VideoFactorySection.Projects -> Icons.Rounded.FolderCopy
}
