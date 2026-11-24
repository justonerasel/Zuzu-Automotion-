package com.example.ui.screens.factory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.VideoAspectRatio
import com.example.model.VideoDurationPreset
import com.example.model.VideoFactorySection
import com.example.model.VideoLanguage
import com.example.model.VideoTone
import com.example.model.VideoType
import com.example.model.VisualStyle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel

@Composable
fun VideoConfigSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  val draft = uiState.configDraft
  val isEditing = draft.editingProjectId != null

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = if (isEditing) "Edit Video Configuration" else "Create New Video Project",
      subtitle = "Customize topic, language, duration, style, and aspect ratio before saving",
      gradientColors = ZuzuVioletPinkGradient,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuViolet, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      OutlinedTextField(
        value = draft.topic,
        onValueChange = viewModel::updateDraftTopic,
        label = { Text("Video Topic *") },
        placeholder = { Text("e.g. How AI Assists Small Creators") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vf_config_topic_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = draft.category,
        onValueChange = viewModel::updateDraftCategory,
        label = { Text("Video Category") },
        placeholder = { Text("e.g. Tech Explained / Storytelling") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = draft.audience,
        onValueChange = viewModel::updateDraftAudience,
        label = { Text("Target Audience") },
        placeholder = { Text("e.g. Students, Entrepreneurs, Bangla Creators") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(12.dp))
      ConfigLabel("Language")
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoLanguage.entries.forEach { lang ->
          ZuzuPillChip(
            label = "${lang.displayLabel} (${lang.nativeLabel})",
            selected = draft.language == lang,
            onClick = { viewModel.updateDraftLanguage(lang) },
            gradientColors = listOf(ZuzuViolet, ZuzuPurple),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      ConfigLabel("Duration (Default Medium: 1–3 minutes)")
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoDurationPreset.entries.forEach { preset ->
          ZuzuPillChip(
            label = "${preset.displayLabel} (${preset.rangeLabel})",
            selected = draft.duration == preset,
            onClick = { viewModel.updateDraftDuration(preset) },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      ConfigLabel("Video Type")
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoType.entries.forEach { type ->
          ZuzuPillChip(
            label = type.displayLabel,
            selected = draft.videoType == type,
            onClick = { viewModel.updateDraftVideoType(type) },
            gradientColors = listOf(ZuzuPink, ZuzuMagenta),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      ConfigLabel("Video Tone")
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoTone.entries.forEach { tone ->
          ZuzuPillChip(
            label = tone.displayLabel,
            selected = draft.tone == tone,
            onClick = { viewModel.updateDraftTone(tone) },
            gradientColors = listOf(ZuzuViolet, ZuzuPink),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      ConfigLabel("Visual Style")
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VisualStyle.entries.forEach { style ->
          ZuzuPillChip(
            label = style.displayLabel,
            selected = draft.visualStyle == style,
            onClick = { viewModel.updateDraftVisualStyle(style) },
            gradientColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      ConfigLabel("Aspect Ratio")
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoAspectRatio.entries.forEach { ratio ->
          ZuzuPillChip(
            label = "${ratio.label} • ${ratio.description}",
            selected = draft.aspectRatio == ratio,
            onClick = { viewModel.updateDraftAspectRatio(ratio) },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuPurple),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      ZuzuGradientButton(
        text = if (isEditing) "Update Video Project" else "Create Project & Open Research",
        onClick = { viewModel.saveProjectConfig() },
        gradientColors = ZuzuBlueTurquoiseGradient,
        icon = Icons.Rounded.MovieCreation,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vf_save_config_button"),
      )
    }
  }
}

@Composable
fun VideoProjectsSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = "My Video Projects (${uiState.projects.size})",
      subtitle = "Reopen projects to edit details, research, script drafts, and storyboards",
      gradientColors = ZuzuVioletPinkGradient,
    )

    ZuzuGradientButton(
      text = "New Video Project",
      onClick = viewModel::startNewProjectConfig,
      gradientColors = ZuzuHeroGradient,
      icon = Icons.Rounded.Add,
      modifier = Modifier.fillMaxWidth(),
    )

    if (uiState.projects.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Video Projects Saved",
        description = "Create a new video project or convert a saved idea to start building scripts and storyboards.",
        icon = Icons.Rounded.FolderOff,
        gradientColors = ZuzuVioletPinkGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      uiState.projects.forEach { project ->
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuViolet, ZuzuPink),
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
                text = "${project.videoType.displayLabel} • ${project.language.displayLabel} • ${project.duration.rangeLabel} • ${project.aspectRatio.label}",
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuInkSecondary,
              )
            }
            Row {
              IconButton(onClick = { viewModel.editProjectConfig(project) }) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Edit project details",
                  tint = ZuzuElectricBlue,
                )
              }
              IconButton(onClick = { viewModel.deleteProject(project.id) }) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "Delete project",
                  tint = ZuzuError,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          StatusPill(
            label = "${project.stage.label} • ${project.script.wordCount} words • ${project.scenes.size} scenes",
            tint = ZuzuPurple,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ZuzuSecondaryActionButton(
              text = "Research",
              onClick = { viewModel.openProject(project.id, VideoFactorySection.ResearchStudio) },
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.Science,
              modifier = Modifier.weight(1f),
            )
            ZuzuSecondaryActionButton(
              text = "Script",
              onClick = { viewModel.openProject(project.id, VideoFactorySection.ScriptWriter) },
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.Description,
              modifier = Modifier.weight(1f),
            )
            ZuzuSecondaryActionButton(
              text = "Scenes",
              onClick = { viewModel.openProject(project.id, VideoFactorySection.StoryboardStudio) },
              accentColor = ZuzuPink,
              icon = Icons.Rounded.ViewCarousel,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ConfigLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.Bold,
    color = ZuzuInkPrimary,
  )
  Spacer(modifier = Modifier.height(6.dp))
}
