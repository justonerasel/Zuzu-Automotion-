package com.example.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SubtitleBackgroundStyle
import com.example.model.SubtitleCue
import com.example.model.SubtitlePosition
import com.example.model.VideoEditorDraft
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoEditorViewModel
import java.util.UUID

@Composable
fun SubtitleEditorTab(
  draft: VideoEditorDraft,
  activeProject: VideoProject?,
  editorViewModel: VideoEditorViewModel,
  glowEnabled: Boolean,
) {
  var editingCue by remember { mutableStateOf<SubtitleCue?>(null) }
  var showDialog by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = "Subtitle Editor (${draft.subtitles.size})",
      subtitle = "Bangla & English timed subtitles with live style preview",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuCyan, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Add Subtitle",
          onClick = {
            editingCue = null
            showDialog = true
          },
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.Add,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_add_subtitle_btn"),
        )
        if (activeProject != null) {
          ZuzuSecondaryActionButton(
            text = "From Storyboard",
            onClick = { editorViewModel.importSubtitlesFromProject(activeProject) },
            accentColor = ZuzuTurquoise,
            icon = Icons.Rounded.AutoFixHigh,
            modifier = Modifier
              .weight(1f)
              .testTag("editor_import_subtitles_btn"),
          )
        }
      }
    }

    if (draft.subtitles.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Subtitles Added Yet",
        description = "Tap 'Add Subtitle' for Bangla or English captions, or import directly from your Storyboard narration.",
        icon = Icons.Rounded.Subtitles,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      draft.subtitles.forEach { cue ->
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              StatusPill(label = cue.language.displayLabel, tint = ZuzuPurple)
              StatusPill(
                label = "${cue.startMs / 1000L}s–${cue.endMs / 1000L}s • ${cue.position.displayLabel}",
                tint = ZuzuElectricBlue,
              )
            }
            Row {
              IconButton(onClick = {
                editingCue = cue
                showDialog = true
              }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit subtitle", tint = ZuzuPurple)
              }
              IconButton(onClick = { editorViewModel.removeSubtitle(cue.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove subtitle", tint = ZuzuError)
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          SubtitleStylePreviewBox(cue = cue)
        }
      }
    }
  }

  if (showDialog) {
    SubtitleEditDialog(
      initial = editingCue,
      defaultStartMs = 0L,
      defaultEndMs = draft.totalDurationMs.coerceIn(2_000L, 5_000L),
      onDismiss = { showDialog = false },
      onSave = { cue ->
        editorViewModel.upsertSubtitle(cue)
        showDialog = false
      },
    )
  }
}

@Composable
private fun SubtitleStylePreviewBox(cue: SubtitleCue) {
  val bgColor = when (cue.backgroundStyle) {
    SubtitleBackgroundStyle.None -> Color.Transparent
    SubtitleBackgroundStyle.DarkTranslucent -> Color(0xBA10101A)
    SubtitleBackgroundStyle.PurpleGlass -> Color(0xCC6D28D9)
    SubtitleBackgroundStyle.SolidBlack -> Color.Black
    SubtitleBackgroundStyle.SolidWhite -> Color.White
  }
  val alignment = when (cue.position) {
    SubtitlePosition.Top -> Alignment.TopCenter
    SubtitlePosition.Middle -> Alignment.Center
    SubtitlePosition.Bottom -> Alignment.BottomCenter
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(84.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Brush.horizontalGradient(listOf(ZuzuDeepPurple, ZuzuElectricBlue)))
      .padding(8.dp),
    contentAlignment = alignment,
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = bgColor,
    ) {
      Text(
        text = cue.text.ifBlank { "Subtitle preview" },
        color = Color(cue.textColorArgb),
        fontSize = cue.fontSizeSp.coerceIn(12, 24).sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun SubtitleEditDialog(
  initial: SubtitleCue?,
  defaultStartMs: Long,
  defaultEndMs: Long,
  onDismiss: () -> Unit,
  onSave: (SubtitleCue) -> Unit,
) {
  var text by remember { mutableStateOf(initial?.text ?: "") }
  var language by remember { mutableStateOf(initial?.language ?: VideoLanguage.English) }
  var startSec by remember { mutableStateOf(((initial?.startMs ?: defaultStartMs) / 1000L).toString()) }
  var endSec by remember { mutableStateOf(((initial?.endMs ?: defaultEndMs) / 1000L).toString()) }
  var fontSize by remember { mutableFloatStateOf((initial?.fontSizeSp ?: 18).toFloat()) }
  var position by remember { mutableStateOf(initial?.position ?: SubtitlePosition.Bottom) }
  var textColorArgb by remember { mutableStateOf(initial?.textColorArgb ?: 0xFFFFFFFFL) }
  var backgroundStyle by remember { mutableStateOf(initial?.backgroundStyle ?: SubtitleBackgroundStyle.DarkTranslucent) }

  val textColors = listOf(0xFFFFFFFFL, 0xFFFFEB3BL, 0xFF06B6D4L, 0xFFEC4899L, 0xFF111827L)

  val previewCue = SubtitleCue(
    id = "preview",
    text = text.ifBlank {
      if (language == VideoLanguage.Bangla) "বাংলা সাবটাইটেল প্রিভিউ" else "English Subtitle Preview"
    },
    language = language,
    fontSizeSp = fontSize.toInt(),
    position = position,
    textColorArgb = textColorArgb,
    backgroundStyle = backgroundStyle,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = if (initial == null) "Add Subtitle" else "Edit Subtitle",
        fontWeight = FontWeight.ExtraBold,
        color = ZuzuInkPrimary,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SubtitleStylePreviewBox(cue = previewCue)

        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VideoLanguage.entries.forEach { lang ->
            ZuzuPillChip(
              label = "${lang.displayLabel} (${lang.nativeLabel})",
              selected = language == lang,
              onClick = { language = lang },
              gradientColors = ZuzuVioletPinkGradient,
            )
          }
        }

        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Subtitle Text") },
          placeholder = {
            Text(
              if (language == VideoLanguage.Bangla) "বাংলায় সাবটাইটেল লিখুন..." else "Enter subtitle text...",
            )
          },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startSec,
            onValueChange = { startSec = it.filter { c -> c.isDigit() } },
            label = { Text("Start (sec)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = endSec,
            onValueChange = { endSec = it.filter { c -> c.isDigit() } },
            label = { Text("End (sec)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }

        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Slider(
          value = fontSize,
          onValueChange = { fontSize = it },
          valueRange = 12f..36f,
          colors = SliderDefaults.colors(
            thumbColor = ZuzuMagenta,
            activeTrackColor = ZuzuPurple,
            inactiveTrackColor = ZuzuPurpleSoft,
          ),
        )

        Text("Position", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SubtitlePosition.entries.forEach { pos ->
            ZuzuPillChip(
              label = pos.displayLabel,
              selected = position == pos,
              onClick = { position = pos },
              gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
            )
          }
        }

        Text("Text Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          textColors.forEach { argb ->
            Surface(
              shape = CircleShape,
              color = Color(argb),
              border = BorderStroke(
                width = if (textColorArgb == argb) 3.dp else 1.dp,
                color = if (textColorArgb == argb) ZuzuPurple else ZuzuInkSecondary,
              ),
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { textColorArgb = argb },
            ) {}
          }
        }

        Text("Background Style", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SubtitleBackgroundStyle.entries.forEach { style ->
            ZuzuPillChip(
              label = style.displayLabel,
              selected = backgroundStyle == style,
              onClick = { backgroundStyle = style },
              gradientColors = listOf(ZuzuViolet, ZuzuPink),
            )
          }
        }
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Save Subtitle",
        onClick = {
          val startMs = (startSec.toLongOrNull() ?: 0L) * 1000L
          val endMs = maxOf((endSec.toLongOrNull() ?: 4L) * 1000L, startMs + 500L)
          onSave(
            SubtitleCue(
              id = initial?.id ?: UUID.randomUUID().toString(),
              text = text.trim(),
              language = language,
              startMs = startMs,
              endMs = endMs,
              fontSizeSp = fontSize.toInt(),
              position = position,
              textColorArgb = textColorArgb,
              backgroundStyle = backgroundStyle,
            ),
          )
        },
        enabled = text.isNotBlank(),
        gradientColors = ZuzuVioletPinkGradient,
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
