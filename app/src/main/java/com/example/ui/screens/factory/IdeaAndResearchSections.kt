package com.example.ui.screens.factory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PlayForWork
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.IdeaCategory
import com.example.model.ResearchWorkspace
import com.example.model.VideoIdea
import com.example.model.VideoLanguage
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel

@Composable
fun IdeaGeneratorSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  var topicPrompt by remember { mutableStateOf("") }
  var audiencePrompt by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(IdeaCategory.OriginalTopic) }
  var selectedLanguage by remember { mutableStateOf(VideoLanguage.English) }
  var showIdeaDialog by remember { mutableStateOf(false) }
  var editingIdea by remember { mutableStateOf<VideoIdea?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "AI Idea Generator",
      subtitle = "Brainstorm topics, hooks, and audience concepts in Bangla or English",
      gradientColors = ZuzuVioletPinkGradient,
    )

    DisclaimerNoticeCard(text = stringResource(R.string.vf_idea_disclaimer))

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuPink, ZuzuMagenta),
      modifier = Modifier.fillMaxWidth(),
    ) {
      OutlinedTextField(
        value = topicPrompt,
        onValueChange = { topicPrompt = it },
        label = { Text("Niche or Topic Focus") },
        placeholder = { Text("e.g. Productivity habits, Space facts, Tech tutorials") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = audiencePrompt,
        onValueChange = { audiencePrompt = it },
        label = { Text("Target Audience (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Idea Category",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IdeaCategory.entries.forEach { category ->
          ZuzuPillChip(
            label = category.displayLabel,
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            gradientColors = ZuzuVioletPinkGradient,
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoLanguage.entries.forEach { lang ->
          ZuzuPillChip(
            label = "${lang.displayLabel} (${lang.nativeLabel})",
            selected = selectedLanguage == lang,
            onClick = { selectedLanguage = lang },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Add Idea",
          onClick = {
            editingIdea = null
            showIdeaDialog = true
          },
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.Add,
          modifier = Modifier
            .weight(1f)
            .testTag("vf_add_idea_manual_button"),
        )
        ZuzuGradientButton(
          text = "AI Generate",
          onClick = {
            viewModel.requestAiIdeas(
              nicheOrTopic = topicPrompt,
              audience = audiencePrompt,
              category = selectedCategory,
              language = selectedLanguage,
            )
          },
          gradientColors = ZuzuVioletPinkGradient,
          icon = Icons.Rounded.AutoAwesome,
          modifier = Modifier
            .weight(1f)
            .testTag("vf_generate_ideas_ai_button"),
        )
      }
    }

    SectionHeader(
      title = "Saved Ideas (${uiState.ideas.size})",
      gradientColors = listOf(ZuzuViolet, ZuzuPink),
    )

    if (uiState.ideas.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Ideas Saved Yet",
        description = "Add your own video ideas now or connect Gemini AI later to brainstorm automatically.",
        icon = Icons.Rounded.Lightbulb,
        gradientColors = ZuzuVioletPinkGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      uiState.ideas.forEach { idea ->
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuPink, ZuzuMagenta),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = idea.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ZuzuInkPrimary,
              )
              Text(
                text = "${idea.category.displayLabel} • ${idea.language.displayLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuInkSecondary,
              )
            }
            Row {
              IconButton(
                onClick = {
                  editingIdea = idea
                  showIdeaDialog = true
                },
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit idea", tint = ZuzuPurple)
              }
              IconButton(onClick = { viewModel.deleteIdea(idea.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete idea", tint = ZuzuError)
              }
            }
          }
          if (idea.hookConcept.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = idea.hookConcept,
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkPrimary,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            StatusPill(
              label = if (idea.isAiSuggested) "AI Suggestion (Unverified)" else "Saved Idea",
              tint = if (idea.isAiSuggested) ZuzuAmber else ZuzuGreen,
            )
            ZuzuSecondaryActionButton(
              text = "Turn into Project",
              onClick = { viewModel.turnIdeaIntoProject(idea) },
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.PlayForWork,
            )
          }
        }
      }
    }
  }

  if (showIdeaDialog) {
    IdeaEditorDialog(
      initial = editingIdea,
      defaultTopic = topicPrompt,
      defaultAudience = audiencePrompt,
      defaultCategory = selectedCategory,
      defaultLanguage = selectedLanguage,
      onDismiss = { showIdeaDialog = false },
      onSave = { title, topic, hook, category, audience, language ->
        viewModel.saveIdea(
          editingId = editingIdea?.id,
          title = title,
          topic = topic,
          hookConcept = hook,
          category = category,
          targetAudience = audience,
          language = language,
        )
        showIdeaDialog = false
      },
    )
  }
}

@Composable
fun ResearchStudioSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  val project = uiState.activeProject
  if (project == null) {
    MissingProjectGuardCard(
      title = "Select or Create a Video Project",
      description = "Create a video project in Configure first so research notes and outlines save directly to your project.",
      onCreateProject = viewModel::startNewProjectConfig,
      glowEnabled = glowEnabled,
    )
    return
  }

  var overview by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.topicOverview)
  }
  var keyFacts by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.keyFacts)
  }
  var importantPoints by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.importantPoints)
  }
  var suggestedSources by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.suggestedSources)
  }
  var researchNotes by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.researchNotes)
  }
  var contentOutline by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.contentOutline)
  }
  var isAiUnverified by remember(project.id, project.research.lastUpdatedMillis) {
    mutableStateOf(project.research.isAiSuggestedUnverified)
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Research Studio",
      subtitle = "Organize facts, points, sources, and outline for '${project.topic}'",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    DisclaimerNoticeCard(text = stringResource(R.string.vf_research_disclaimer))

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuElectricBlue, ZuzuCyan),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusPill(
          label = if (isAiUnverified) "Unverified AI Suggestions" else "Manual Research Workspace",
          tint = if (isAiUnverified) ZuzuAmber else ZuzuGreen,
        )
        ZuzuSecondaryActionButton(
          text = "AI Research",
          onClick = { viewModel.requestAiResearch(project) },
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.AutoAwesome,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      ResearchTextField(
        label = "Topic Overview",
        value = overview,
        onValueChange = { overview = it; isAiUnverified = false },
      )
      ResearchTextField(
        label = "Key Facts (User-Entered / Verified)",
        value = keyFacts,
        onValueChange = { keyFacts = it; isAiUnverified = false },
      )
      ResearchTextField(
        label = "Important Points",
        value = importantPoints,
        onValueChange = { importantPoints = it },
      )
      ResearchTextField(
        label = "Suggested Sources (Provide Real Citations/URLs)",
        value = suggestedSources,
        onValueChange = { suggestedSources = it },
      )
      ResearchTextField(
        label = "Research Notes",
        value = researchNotes,
        onValueChange = { researchNotes = it },
      )
      ResearchTextField(
        label = "Content Outline",
        value = contentOutline,
        onValueChange = { contentOutline = it },
      )

      Spacer(modifier = Modifier.height(8.dp))
      ZuzuGradientButton(
        text = "Save Research",
        onClick = {
          viewModel.saveResearch(
            project.id,
            ResearchWorkspace(
              topicOverview = overview.trim(),
              keyFacts = keyFacts.trim(),
              importantPoints = importantPoints.trim(),
              suggestedSources = suggestedSources.trim(),
              researchNotes = researchNotes.trim(),
              contentOutline = contentOutline.trim(),
              isAiSuggestedUnverified = isAiUnverified,
            ),
          )
        },
        gradientColors = ZuzuBlueTurquoiseGradient,
        icon = Icons.Rounded.Save,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vf_save_research_button"),
      )
    }
  }
}

@Composable
private fun ResearchTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    minLines = 2,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 10.dp),
  )
}

@Composable
private fun IdeaEditorDialog(
  initial: VideoIdea?,
  defaultTopic: String,
  defaultAudience: String,
  defaultCategory: IdeaCategory,
  defaultLanguage: VideoLanguage,
  onDismiss: () -> Unit,
  onSave: (String, String, String, IdeaCategory, String, VideoLanguage) -> Unit,
) {
  var title by remember { mutableStateOf(initial?.title ?: "") }
  var topic by remember { mutableStateOf(initial?.topic ?: defaultTopic) }
  var hook by remember { mutableStateOf(initial?.hookConcept ?: "") }
  var category by remember { mutableStateOf(initial?.category ?: defaultCategory) }
  var audience by remember { mutableStateOf(initial?.targetAudience ?: defaultAudience) }
  var language by remember { mutableStateOf(initial?.language ?: defaultLanguage) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = if (initial == null) "Save Video Idea" else "Edit Video Idea",
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Idea / Video Title *") },
          singleLine = true,
        )
        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Core Topic") },
          singleLine = true,
        )
        OutlinedTextField(
          value = hook,
          onValueChange = { hook = it },
          label = { Text("Concept / Hook Notes") },
          minLines = 2,
        )
        OutlinedTextField(
          value = audience,
          onValueChange = { audience = it },
          label = { Text("Target Audience") },
          singleLine = true,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IdeaCategory.entries.forEach { cat ->
            ZuzuPillChip(
              label = cat.displayLabel,
              selected = category == cat,
              onClick = { category = cat },
              gradientColors = ZuzuVioletPinkGradient,
            )
          }
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          VideoLanguage.entries.forEach { lang ->
            ZuzuPillChip(
              label = lang.displayLabel,
              selected = language == lang,
              onClick = { language = lang },
              gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
            )
          }
        }
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Save",
        onClick = { onSave(title, topic, hook, category, audience, language) },
        enabled = title.isNotBlank(),
        gradientColors = ZuzuVioletPinkGradient,
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", color = ZuzuInkSecondary) }
    },
  )
}

@Composable
internal fun DisclaimerNoticeCard(text: String) {
  ZuzuAlertBanner(
    message = text,
    tint = ZuzuViolet,
    icon = Icons.Rounded.Policy,
  )
}

@Composable
internal fun MissingProjectGuardCard(
  title: String,
  description: String,
  onCreateProject: () -> Unit,
  glowEnabled: Boolean,
) {
  ZuzuEmptyStatePanel(
    title = title,
    description = description,
    icon = Icons.Rounded.MovieCreation,
    gradientColors = ZuzuHeroGradient,
    glowEnabled = glowEnabled,
    ctaLabel = "Configure New Video Project",
    ctaIcon = Icons.Rounded.Add,
    onCtaClick = onCreateProject,
  )
}
