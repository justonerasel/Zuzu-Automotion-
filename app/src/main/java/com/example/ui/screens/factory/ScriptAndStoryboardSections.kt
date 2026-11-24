package com.example.ui.screens.factory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ai.ScriptAiAction
import com.example.model.StoryboardScene
import com.example.model.VideoFactorySection
import com.example.model.VideoScript
import com.example.model.VideoTone
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
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel
import java.util.UUID

@Composable
fun ScriptWriterSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  val project = uiState.activeProject
  if (project == null) {
    MissingProjectGuardCard(
      title = "Select or Create a Video Project",
      description = "Create a video project first to write, edit, and save structured Bangla or English scripts.",
      onCreateProject = viewModel::startNewProjectConfig,
      glowEnabled = glowEnabled,
    )
    return
  }

  val context = LocalContext.current
  var openingHook by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.openingHook) }
  var introduction by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.introduction) }
  var mainContent by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.mainContent) }
  var sceneNarration by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.sceneNarration) }
  var transitions by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.transitions) }
  var ending by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.ending) }
  var callToAction by remember(project.id, project.script.lastSavedMillis) { mutableStateOf(project.script.callToAction) }

  var showRewriteDialog by remember { mutableStateOf(false) }
  var showToneDialog by remember { mutableStateOf(false) }
  var copiedFeedback by remember { mutableStateOf(false) }

  val currentDraft = VideoScript(
    openingHook = openingHook,
    introduction = introduction,
    mainContent = mainContent,
    sceneNarration = sceneNarration,
    transitions = transitions,
    ending = ending,
    callToAction = callToAction,
    isAiDraft = project.script.isAiDraft,
  )

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Script Writer",
      subtitle = "${project.topic} • ${project.language.displayLabel} (${project.language.nativeLabel}) • ${project.tone.displayLabel}",
      gradientColors = listOf(ZuzuViolet, ZuzuPurple),
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuViolet, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusPill(
          label = "${currentDraft.wordCount} words • ${project.duration.rangeLabel}",
          tint = ZuzuPurple,
        )
        if (copiedFeedback) {
          StatusPill(label = "Copied!", tint = ZuzuGreen)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "AI Script Tools (Requires Gemini Setup)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Regenerate Script",
          onClick = { viewModel.requestAiScriptAction(project, ScriptAiAction.Regenerate) },
          accentColor = ZuzuViolet,
          icon = Icons.Rounded.AutoAwesome,
        )
        ZuzuSecondaryActionButton(
          text = "Rewrite Selected Text",
          onClick = { showRewriteDialog = true },
          accentColor = ZuzuElectricBlue,
        )
        ZuzuSecondaryActionButton(
          text = "Change Tone",
          onClick = { showToneDialog = true },
          accentColor = ZuzuMagenta,
        )
        ZuzuSecondaryActionButton(
          text = "Shorten Script",
          onClick = { viewModel.requestAiScriptAction(project, ScriptAiAction.Shorten) },
          accentColor = ZuzuTurquoise,
        )
        ZuzuSecondaryActionButton(
          text = "Expand Script",
          onClick = { viewModel.requestAiScriptAction(project, ScriptAiAction.Expand) },
          accentColor = ZuzuPurple,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      val placeholderHint = if (project.language == com.example.model.VideoLanguage.Bangla) {
        "বাংলায় লিখুন বা সম্পাদনা করুন..."
      } else {
        "Write or edit in English..."
      }

      ScriptSectionField("Opening Hook", openingHook, { openingHook = it }, placeholderHint)
      ScriptSectionField("Introduction", introduction, { introduction = it }, placeholderHint)
      ScriptSectionField("Main Content", mainContent, { mainContent = it }, placeholderHint, minLines = 4)
      ScriptSectionField("Scene Narration", sceneNarration, { sceneNarration = it }, placeholderHint, minLines = 3)
      ScriptSectionField("Transitions", transitions, { transitions = it }, "e.g. Smooth Cut -> Zoom Transition")
      ScriptSectionField("Ending", ending, { ending = it }, placeholderHint)
      ScriptSectionField("Call To Action", callToAction, { callToAction = it }, placeholderHint)

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Copy Script",
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Zuzu Video Script", currentDraft.toCombinedText()))
            copiedFeedback = true
          },
          enabled = currentDraft.hasContent,
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.ContentCopy,
          modifier = Modifier
            .weight(1f)
            .testTag("vf_copy_script_button"),
        )
        ZuzuGradientButton(
          text = "Save Draft",
          onClick = { viewModel.saveScriptDraft(project.id, currentDraft) },
          gradientColors = ZuzuHeroGradient,
          icon = Icons.Rounded.Save,
          modifier = Modifier
            .weight(1f)
            .testTag("vf_save_script_button"),
        )
      }
    }
  }

  if (showRewriteDialog) {
    var snippet by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showRewriteDialog = false },
      containerColor = ZuzuWhite,
      title = { Text("Rewrite Selected Text", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Paste or enter the script excerpt you want Gemini AI to rewrite.",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          OutlinedTextField(
            value = snippet,
            onValueChange = { snippet = it },
            label = { Text("Selected Excerpt") },
            minLines = 3,
          )
        }
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Rewrite",
          onClick = {
            showRewriteDialog = false
            viewModel.requestAiScriptAction(project, ScriptAiAction.RewriteSelection, selectedText = snippet)
          },
          enabled = snippet.isNotBlank(),
          gradientColors = ZuzuVioletPinkGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { showRewriteDialog = false }) { Text("Cancel", color = ZuzuInkSecondary) }
      },
    )
  }

  if (showToneDialog) {
    var pickedTone by remember { mutableStateOf(project.tone) }
    AlertDialog(
      onDismissRequest = { showToneDialog = false },
      containerColor = ZuzuWhite,
      title = { Text("Change Script Tone", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
      text = {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          VideoTone.entries.forEach { tone ->
            ZuzuPillChip(
              label = tone.displayLabel,
              selected = pickedTone == tone,
              onClick = { pickedTone = tone },
              gradientColors = ZuzuVioletPinkGradient,
            )
          }
        }
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Apply Tone",
          onClick = {
            showToneDialog = false
            viewModel.requestAiScriptAction(project, ScriptAiAction.ChangeTone, targetTone = pickedTone)
          },
          gradientColors = ZuzuVioletPinkGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { showToneDialog = false }) { Text("Cancel", color = ZuzuInkSecondary) }
      },
    )
  }
}

@Composable
fun StoryboardStudioSection(
  uiState: VideoFactoryUiState,
  viewModel: VideoFactoryViewModel,
  glowEnabled: Boolean,
) {
  val project = uiState.activeProject
  if (project == null) {
    MissingProjectGuardCard(
      title = "Select or Create a Video Project",
      description = "Create a video project first to convert script sections into visual storyboard scenes.",
      onCreateProject = viewModel::startNewProjectConfig,
      glowEnabled = glowEnabled,
    )
    return
  }

  var showSceneDialog by remember { mutableStateOf(false) }
  var editingScene by remember { mutableStateOf<StoryboardScene?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Storyboard Studio",
      subtitle = "${project.topic} • ${project.scenes.size} scenes • ${project.totalStoryboardSeconds}s total",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuCyan, ZuzuTurquoise),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Split Script",
          onClick = { viewModel.convertScriptToStoryboardScenes(project) },
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.AutoFixHigh,
          modifier = Modifier
            .weight(1f)
            .testTag("vf_convert_script_scenes_button"),
        )
        ZuzuSecondaryActionButton(
          text = "AI Scenes",
          onClick = { viewModel.requestAiStoryboard(project) },
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.AutoAwesome,
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      ZuzuGradientButton(
        text = "Add Storyboard Scene",
        onClick = {
          editingScene = null
          showSceneDialog = true
        },
        gradientColors = ZuzuBlueTurquoiseGradient,
        icon = Icons.Rounded.Add,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vf_add_scene_button"),
      )
    }

    if (project.scenes.isNotEmpty()) {
      SectionHeader(
        title = "Visual Timeline",
        gradientColors = listOf(ZuzuViolet, ZuzuCyan),
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        project.scenes.forEach { scene ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = ZuzuSoftLavender,
            border = BorderStroke(1.dp, ZuzuPurple.copy(alpha = 0.35f)),
            modifier = Modifier.width(148.dp),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ZuzuPurple),
                ) {
                  Text(
                    text = "${scene.sceneNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZuzuWhite,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Text(
                  text = "${scene.suggestedDurationSeconds}s",
                  style = MaterialTheme.typography.labelSmall,
                  color = ZuzuElectricBlue,
                  fontWeight = FontWeight.Bold,
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = scene.sceneTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ZuzuInkPrimary,
                maxLines = 1,
              )
              Text(
                text = scene.transitionSuggestion,
                style = MaterialTheme.typography.labelSmall,
                color = ZuzuInkSecondary,
                maxLines = 1,
              )
            }
          }
        }
      }
    }

    if (project.scenes.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Storyboard Scenes Yet",
        description = "Tap 'Split Script' after writing script sections, or tap 'Add Storyboard Scene' to build your visual shot list manually.",
        icon = Icons.Rounded.ViewCarousel,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      project.scenes.forEachIndexed { index, scene ->
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuCyan, ZuzuPurple),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Scene ${scene.sceneNumber}: ${scene.sceneTitle}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ZuzuInkPrimary,
              )
              Text(
                text = "${scene.suggestedDurationSeconds}s • Transition: ${scene.transitionSuggestion}",
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuElectricBlue,
              )
            }
            Row {
              IconButton(
                onClick = { viewModel.moveScene(project, index, -1) },
                enabled = index > 0,
              ) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
              }
              IconButton(
                onClick = { viewModel.moveScene(project, index, 1) },
                enabled = index < project.scenes.lastIndex,
              ) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
              }
              IconButton(
                onClick = {
                  editingScene = scene
                  showSceneDialog = true
                },
              ) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit scene", tint = ZuzuPurple)
              }
              IconButton(onClick = { viewModel.deleteScene(project, scene.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete scene", tint = ZuzuError)
              }
            }
          }
          if (scene.narration.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Narration: ${scene.narration}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
          }
          if (scene.visualDescription.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Visual: ${scene.visualDescription}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
          }
          if (scene.imageGenerationPrompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Image Prompt: ${scene.imageGenerationPrompt}", style = MaterialTheme.typography.labelSmall, color = ZuzuPurple)
          }
          if (scene.videoGenerationPrompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text("Video Prompt: ${scene.videoGenerationPrompt}", style = MaterialTheme.typography.labelSmall, color = ZuzuMagenta)
          }
          if (scene.assignedVoiceAssetId != null || scene.assignedVisualAssetId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (scene.assignedVoiceAssetId != null) {
                StatusPill(label = "Voiceover Assigned", tint = ZuzuElectricBlue)
              }
              if (scene.assignedVisualAssetId != null) {
                StatusPill(label = "Visual Assigned", tint = ZuzuGreen)
              }
            }
          }
        }
      }
    }

    ZuzuSecondaryActionButton(
      text = "Next: Open Voice Studio",
      onClick = { viewModel.selectSection(VideoFactorySection.VoiceStudio) },
      accentColor = ZuzuPurple,
      icon = Icons.Rounded.ChevronRight,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vf_next_voice_studio_button"),
    )
  }

  if (showSceneDialog) {
    SceneEditorDialog(
      projectId = project.id,
      nextNumber = project.scenes.size + 1,
      initial = editingScene,
      onDismiss = { showSceneDialog = false },
      onSave = { scene ->
        viewModel.upsertScene(project, scene)
        showSceneDialog = false
      },
    )
  }
}

@Composable
private fun ScriptSectionField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  minLines: Int = 2,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = { Text(placeholder) },
    minLines = minLines,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 10.dp),
  )
}

@Composable
private fun SceneEditorDialog(
  projectId: String,
  nextNumber: Int,
  initial: StoryboardScene?,
  onDismiss: () -> Unit,
  onSave: (StoryboardScene) -> Unit,
) {
  var sceneTitle by remember { mutableStateOf(initial?.sceneTitle ?: "Scene $nextNumber") }
  var narration by remember { mutableStateOf(initial?.narration ?: "") }
  var visualDesc by remember { mutableStateOf(initial?.visualDescription ?: "") }
  var imagePrompt by remember { mutableStateOf(initial?.imageGenerationPrompt ?: "") }
  var videoPrompt by remember { mutableStateOf(initial?.videoGenerationPrompt ?: "") }
  var durationText by remember { mutableStateOf((initial?.suggestedDurationSeconds ?: 15).toString()) }
  var transition by remember { mutableStateOf(initial?.transitionSuggestion ?: "Smooth Cut") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = if (initial == null) "Add Storyboard Scene #$nextNumber" else "Edit Scene #${initial.sceneNumber}",
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
          value = sceneTitle,
          onValueChange = { sceneTitle = it },
          label = { Text("Scene Title *") },
          singleLine = true,
        )
        OutlinedTextField(
          value = narration,
          onValueChange = { narration = it },
          label = { Text("Narration") },
          minLines = 2,
        )
        OutlinedTextField(
          value = visualDesc,
          onValueChange = { visualDesc = it },
          label = { Text("Visual Description") },
          minLines = 2,
        )
        OutlinedTextField(
          value = imagePrompt,
          onValueChange = { imagePrompt = it },
          label = { Text("Image Generation Prompt") },
          minLines = 2,
        )
        OutlinedTextField(
          value = videoPrompt,
          onValueChange = { videoPrompt = it },
          label = { Text("Video Generation Prompt") },
          minLines = 2,
        )
        OutlinedTextField(
          value = durationText,
          onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Suggested Duration (seconds)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
        OutlinedTextField(
          value = transition,
          onValueChange = { transition = it },
          label = { Text("Transition Suggestion") },
          singleLine = true,
        )
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Save Scene",
        onClick = {
          onSave(
            StoryboardScene(
              id = initial?.id ?: UUID.randomUUID().toString(),
              projectId = projectId,
              sceneNumber = initial?.sceneNumber ?: nextNumber,
              sceneTitle = sceneTitle.trim(),
              narration = narration.trim(),
              visualDescription = visualDesc.trim(),
              imageGenerationPrompt = imagePrompt.trim(),
              videoGenerationPrompt = videoPrompt.trim(),
              suggestedDurationSeconds = durationText.toIntOrNull()?.coerceIn(1, 600) ?: 15,
              transitionSuggestion = transition.trim().ifEmpty { "Smooth Cut" },
              assignedVoiceAssetId = initial?.assignedVoiceAssetId,
              assignedVisualAssetId = initial?.assignedVisualAssetId,
            ),
          )
        },
        enabled = sceneTitle.isNotBlank(),
        gradientColors = ZuzuBlueTurquoiseGradient,
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", color = ZuzuInkSecondary) }
    },
  )
}
