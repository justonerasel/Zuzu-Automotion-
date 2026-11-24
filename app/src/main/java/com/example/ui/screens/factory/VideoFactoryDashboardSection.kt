package com.example.ui.screens.factory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiServiceStatus
import com.example.model.VideoFactorySection
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuCompactStatCard
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel

@Composable
fun VideoFactoryDashboardSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  val aiReadyLabel = when (uiState.aiStatus) {
    is AiServiceStatus.Ready -> "Ready"
    is AiServiceStatus.SetupRequired -> "Setup Req."
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Factory Overview",
      subtitle = "Track ideas, local video projects, and script-to-storyboard readiness",
      gradientColors = ZuzuHeroGradient,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ZuzuCompactStatCard(
        label = "My Video Projects",
        value = uiState.projects.size.toString(),
        icon = Icons.Rounded.FolderCopy,
        gradientColors = listOf(ZuzuViolet, ZuzuPurple),
        statusLabel = "Projects",
        statusTint = ZuzuViolet,
        glowEnabled = glowEnabled,
        modifier = Modifier.weight(1f),
      )
      ZuzuCompactStatCard(
        label = "Saved Ideas",
        value = uiState.ideas.size.toString(),
        icon = Icons.Rounded.Lightbulb,
        gradientColors = listOf(ZuzuPink, ZuzuMagenta),
        statusLabel = "Ideas",
        statusTint = ZuzuPink,
        glowEnabled = glowEnabled,
        modifier = Modifier.weight(1f),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ZuzuCompactStatCard(
        label = "Storyboard Scenes",
        value = uiState.totalScenesCount.toString(),
        icon = Icons.Rounded.ViewCarousel,
        gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
        statusLabel = "Scenes",
        statusTint = ZuzuElectricBlue,
        glowEnabled = glowEnabled,
        modifier = Modifier.weight(1f),
      )
      ZuzuCompactStatCard(
        label = "Gemini AI Engine",
        value = aiReadyLabel,
        icon = Icons.Rounded.SettingsSuggest,
        gradientColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
        statusLabel = if (uiState.aiStatus is AiServiceStatus.Ready) "Live" else "Setup",
        statusTint = if (uiState.aiStatus is AiServiceStatus.Ready) ZuzuGreen else ZuzuAmber,
        glowEnabled = glowEnabled,
        modifier = Modifier.weight(1f),
      )
    }

    ZuzuGradientButton(
      text = "Create New Video",
      onClick = viewModel::startNewProjectConfig,
      gradientColors = ZuzuHeroGradient,
      icon = Icons.Rounded.AddCircle,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vf_create_new_video_button"),
    )

    SectionHeader(
      title = "Production Progress",
      gradientColors = listOf(ZuzuPink, ZuzuCyan),
    )

    val activeProject = uiState.activeProject
    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuViolet, ZuzuCyan),
      modifier = Modifier.fillMaxWidth(),
    ) {
      if (activeProject == null) {
        Text(
          text = "No Active Video Project Yet",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Tap 'Create New Video' or pick an idea from the AI Idea Generator to track research, script, and storyboard progress.",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeProject.topic,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ZuzuInkPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${activeProject.videoType.displayLabel} • ${activeProject.language.displayLabel} • ${activeProject.duration.rangeLabel}",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          }
          StatusPill(
            label = "${activeProject.completedStepsCount}/4 Stages",
            tint = ZuzuPurple,
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
          progress = { activeProject.progressFraction },
          color = ZuzuPurple,
          trackColor = ZuzuSoftLavender,
          strokeCap = StrokeCap.Round,
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          StageIndicatorLabel("1. Config", true)
          StageIndicatorLabel("2. Research", activeProject.research.hasAnyContent)
          StageIndicatorLabel("3. Script", activeProject.script.hasContent)
          StageIndicatorLabel("4. Storyboard", activeProject.scenes.isNotEmpty())
        }
      }
    }

    SectionHeader(
      title = "Factory Modules",
      gradientColors = ZuzuVioletPinkGradient,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      FactoryModuleRow(
        title = "AI Idea Generator",
        subtitle = "Brainstorm topics, hooks, and audience concepts; turn ideas into projects",
        icon = Icons.Rounded.Lightbulb,
        gradient = listOf(ZuzuPink, ZuzuMagenta),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_ideas",
        onClick = { viewModel.selectSection(VideoFactorySection.IdeaGenerator) },
      )
      FactoryModuleRow(
        title = "Research Studio",
        subtitle = "Organize facts, points, suggested sources, and outlines with verification badges",
        icon = Icons.Rounded.Science,
        gradient = listOf(ZuzuElectricBlue, ZuzuCyan),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_research",
        onClick = { viewModel.selectSection(VideoFactorySection.ResearchStudio) },
      )
      FactoryModuleRow(
        title = "Script Writer",
        subtitle = "Write and edit structured Bangla or English scripts with clipboard copy & drafts",
        icon = Icons.Rounded.Description,
        gradient = listOf(ZuzuViolet, ZuzuPurple),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_script",
        onClick = { viewModel.selectSection(VideoFactorySection.ScriptWriter) },
      )
      FactoryModuleRow(
        title = "Storyboard Studio",
        subtitle = "Convert scripts into editable scene cards with visual & generation prompts",
        icon = Icons.Rounded.ViewCarousel,
        gradient = listOf(ZuzuTurquoise, ZuzuCyan),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_storyboard",
        onClick = { viewModel.selectSection(VideoFactorySection.StoryboardStudio) },
      )
      FactoryModuleRow(
        title = "Voice Studio",
        subtitle = "Configure Bangla/English voices, speed, tone, audio import & scene voiceovers",
        icon = Icons.Rounded.RecordVoiceOver,
        gradient = listOf(ZuzuViolet, ZuzuElectricBlue),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_voice",
        onClick = { viewModel.selectSection(VideoFactorySection.VoiceStudio) },
      )
      FactoryModuleRow(
        title = "AI Visual Studio",
        subtitle = "Scene-based image & video clip generation providers, styles & manual visual import",
        icon = Icons.Rounded.Palette,
        gradient = listOf(ZuzuPink, ZuzuMagenta),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_visuals",
        onClick = { viewModel.selectSection(VideoFactorySection.VisualStudio) },
      )
      FactoryModuleRow(
        title = "Asset Library",
        subtitle = "Browse, search, preview, delete, and assign generated or imported studio assets",
        icon = Icons.Rounded.PermMedia,
        gradient = listOf(ZuzuCyan, ZuzuElectricBlue),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_assets",
        onClick = { viewModel.selectSection(VideoFactorySection.AssetLibrary) },
      )
      FactoryModuleRow(
        title = "Video Editor & MP4 Renderer",
        subtitle = "Edit timeline clips, Bangla/English subtitles, audio tracks, effects & render MP4",
        icon = Icons.Rounded.Movie,
        gradient = listOf(ZuzuDeepPurple, ZuzuPink),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_editor",
        onClick = { viewModel.selectSection(VideoFactorySection.VideoEditor) },
      )
      FactoryModuleRow(
        title = "Exported Video Library",
        subtitle = "Preview, rename, share, or manage your rendered MP4 video exports",
        icon = Icons.Rounded.VideoLibrary,
        gradient = listOf(ZuzuElectricBlue, ZuzuTurquoise),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_exports",
        onClick = { viewModel.selectSection(VideoFactorySection.ExportedLibrary) },
      )
      FactoryModuleRow(
        title = "My Video Projects",
        subtitle = "Reopen, edit configuration, or manage saved local video projects",
        icon = Icons.Rounded.FolderCopy,
        gradient = listOf(ZuzuPurple, ZuzuPink),
        glowEnabled = glowEnabled,
        testTag = "vf_quick_projects",
        onClick = { viewModel.selectSection(VideoFactorySection.Projects) },
      )
    }

    SectionHeader(
      title = "Recent Projects",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    if (uiState.projects.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Recent Projects",
        description = "Configure your first video project to begin tracking ideas, scripts, voiceovers, and renders.",
        icon = Icons.Rounded.FolderCopy,
        gradientColors = ZuzuHeroGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      uiState.projects.take(3).forEach { project ->
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuViolet, ZuzuElectricBlue),
          onClick = { viewModel.openProject(project.id, VideoFactorySection.ScriptWriter) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = project.topic,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ZuzuInkPrimary,
              )
              Text(
                text = "${project.videoType.displayLabel} • ${project.aspectRatio.label} • ${project.scenes.size} scenes",
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuInkSecondary,
              )
            }
            StatusPill(label = project.stage.label, tint = ZuzuViolet)
          }
        }
      }
    }
  }
}

@Composable
private fun StageIndicatorLabel(
  label: String,
  completed: Boolean,
) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelSmall,
    fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
    color = if (completed) ZuzuGreen else ZuzuInkSecondary,
  )
}

@Composable
private fun FactoryModuleRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  gradient: List<Color>,
  glowEnabled: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradient,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ZuzuGradientIconBadge(
        icon = icon,
        contentDescription = title,
        gradientColors = gradient,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
      Surface(
        shape = CircleShape,
        color = gradient.first().copy(alpha = 0.12f),
        modifier = Modifier.size(30.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = gradient.first(),
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
