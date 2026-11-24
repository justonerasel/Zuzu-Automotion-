package com.example.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.ViewTimeline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.source.MediaFileHelper
import com.example.model.EditorClipType
import com.example.model.EditorTimelineClip
import com.example.model.EditorTransition
import com.example.model.EditorVisualEffect
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.OverlayPosition
import com.example.model.StudioAsset
import com.example.model.TextOverlay
import com.example.model.VideoEditorDraft
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoEditorViewModel
import java.util.UUID

@Composable
fun TimelineEditorTab(
  draft: VideoEditorDraft,
  studioAssets: List<StudioAsset>,
  mediaLibraryEntries: List<MediaLibraryEntry>,
  editorViewModel: VideoEditorViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  var selectedClipId by remember(draft.id) { mutableStateOf(draft.clips.firstOrNull()?.id) }
  var showAddSceneDialog by remember { mutableStateOf(false) }
  var showLibraryPicker by remember { mutableStateOf(false) }
  var editingClip by remember { mutableStateOf<EditorTimelineClip?>(null) }
  var showOverlayDialog by remember { mutableStateOf(false) }
  var editingOverlay by remember { mutableStateOf<TextOverlay?>(null) }

  val mediaPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { editorViewModel.addMediaClipFromUpload(it) }
        .onFailure { editorViewModel.setError(it.message ?: "Unable to read media clip.") }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = "Scene Timeline (${draft.clips.size})",
      subtitle = "Total: ${MediaFileHelper.formatDuration(draft.totalDurationMs) ?: "0:00"}",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    // Add Clip Actions
    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuViolet, ZuzuCyan),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Scene Card",
          onClick = { showAddSceneDialog = true },
          accentColor = ZuzuViolet,
          icon = Icons.Rounded.Add,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_add_scene_btn"),
        )
        ZuzuSecondaryActionButton(
          text = "Image/Clip",
          onClick = {
            mediaPicker.launch(arrayOf("image/jpeg", "image/png", "video/mp4", "video/quicktime"))
          },
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.UploadFile,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_pick_device_media_btn"),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      ZuzuSecondaryActionButton(
        text = "Pick from Asset / Media Library",
        onClick = { showLibraryPicker = true },
        accentColor = ZuzuTurquoise,
        icon = Icons.Rounded.Collections,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("editor_pick_library_btn"),
      )
    }

    if (draft.clips.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "Timeline Ready for Clips",
        description = "Add a Scene Card or pick images/video clips from your device or Asset Library.",
        icon = Icons.Rounded.ViewTimeline,
        gradientColors = ZuzuVioletPinkGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      // Visual Timeline Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        draft.clips.forEachIndexed { index, clip ->
          val isSelected = selectedClipId == clip.id || (selectedClipId == null && index == 0)
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isSelected) ZuzuSoftLavender else ZuzuWhite,
            border = BorderStroke(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) ZuzuPurple else ZuzuBorderLight,
            ),
            shadowElevation = if (isSelected) 5.dp else 1.dp,
            modifier = Modifier
              .width(164.dp)
              .clip(RoundedCornerShape(18.dp))
              .clickable { selectedClipId = clip.id },
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                StatusPill(
                  label = "#${index + 1} ${clip.clipType.displayLabel}",
                  tint = if (clip.clipType == EditorClipType.Video) ZuzuPink else ZuzuElectricBlue,
                )
                Text(
                  text = "${clip.durationMs / 1000L}s",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = ZuzuPurple,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = clip.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ZuzuInkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${clip.transition.displayLabel} • ${clip.visualEffect.displayLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = ZuzuInkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                IconButton(
                  onClick = { editorViewModel.moveClip(index, -1) },
                  enabled = index > 0,
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Move left", modifier = Modifier.size(16.dp))
                }
                IconButton(
                  onClick = { editingClip = clip },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.Rounded.Edit, contentDescription = "Edit clip", tint = ZuzuPurple, modifier = Modifier.size(16.dp))
                }
                IconButton(
                  onClick = { editorViewModel.removeClip(clip.id) },
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete clip", tint = ZuzuError, modifier = Modifier.size(16.dp))
                }
                IconButton(
                  onClick = { editorViewModel.moveClip(index, 1) },
                  enabled = index < draft.clips.lastIndex,
                  modifier = Modifier.size(30.dp),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Move right", modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }

      // Selected Clip Inspector
      val activeClip = draft.clips.firstOrNull { it.id == selectedClipId } ?: draft.clips.first()
      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = listOf(ZuzuPink, ZuzuPurple),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Clip Settings: ${activeClip.title}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ZuzuInkPrimary,
            )
            Text(
              text = "Duration: ${activeClip.durationMs / 1000L}s • Trim Start: ${activeClip.trimStartMs / 1000L}s",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          ZuzuSecondaryActionButton(
            text = "Trim / Duration",
            onClick = { editingClip = activeClip },
            accentColor = ZuzuPurple,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Transition",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          EditorTransition.entries.forEach { tr ->
            ZuzuPillChip(
              label = tr.displayLabel,
              selected = activeClip.transition == tr,
              onClick = { editorViewModel.updateClip(activeClip.id, transition = tr) },
              gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Visual Effect",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          EditorVisualEffect.entries.forEach { fx ->
            ZuzuPillChip(
              label = fx.displayLabel,
              selected = activeClip.visualEffect == fx,
              onClick = { editorViewModel.updateClip(activeClip.id, visualEffect = fx) },
              gradientColors = ZuzuVioletPinkGradient,
            )
          }
        }
      }
    }

    // Text Overlays Section
    SectionHeader(
      title = "Text Overlays (${draft.textOverlays.size})",
      subtitle = "Add titles, callouts, or watermarks timed on the video",
      gradientColors = listOf(ZuzuPink, ZuzuOrange),
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuOrange, ZuzuPink),
      modifier = Modifier.fillMaxWidth(),
    ) {
      ZuzuSecondaryActionButton(
        text = "Add Text Overlay",
        onClick = {
          editingOverlay = null
          showOverlayDialog = true
        },
        accentColor = ZuzuMagenta,
        icon = Icons.Rounded.TextFields,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("editor_add_overlay_btn"),
      )

      if (draft.textOverlays.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        draft.textOverlays.forEach { overlay ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = ZuzuWhite,
            border = BorderStroke(1.dp, ZuzuBorderLight),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 3.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(overlay.colorArgb)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = overlay.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ZuzuInkPrimary,
                  )
                  Text(
                    text = "${overlay.startMs / 1000L}s–${overlay.endMs / 1000L}s • ${overlay.position.displayLabel} • ${overlay.fontSizeSp}sp",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZuzuInkSecondary,
                  )
                }
              }
              Row {
                IconButton(onClick = {
                  editingOverlay = overlay
                  showOverlayDialog = true
                }) {
                  Icon(Icons.Rounded.Edit, contentDescription = "Edit overlay", tint = ZuzuPurple)
                }
                IconButton(onClick = { editorViewModel.removeTextOverlay(overlay.id) }) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete overlay", tint = ZuzuError)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddSceneDialog) {
    var title by remember { mutableStateOf("Scene ${draft.clips.size + 1}") }
    var durText by remember { mutableStateOf("5") }
    AlertDialog(
      onDismissRequest = { showAddSceneDialog = false },
      containerColor = ZuzuWhite,
      title = {
        Text("Add Scene Card", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Scene Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = durText,
            onValueChange = { durText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Duration (seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Add",
          onClick = {
            editorViewModel.addTitleCardClip(title, durText.toIntOrNull() ?: 5)
            showAddSceneDialog = false
          },
          gradientColors = ZuzuVioletPinkGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { showAddSceneDialog = false }) { Text("Cancel") }
      },
    )
  }

  editingClip?.let { clip ->
    var title by remember { mutableStateOf(clip.title) }
    var trimStartSec by remember { mutableStateOf((clip.trimStartMs / 1000L).toString()) }
    var durationSec by remember { mutableStateOf((clip.durationMs / 1000L).toString()) }
    AlertDialog(
      onDismissRequest = { editingClip = null },
      containerColor = ZuzuWhite,
      title = {
        Text("Trim & Duration", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Clip Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = durationSec,
            onValueChange = { durationSec = it.filter { c -> c.isDigit() } },
            label = { Text("Clip Duration (seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          if (clip.clipType == EditorClipType.Video) {
            OutlinedTextField(
              value = trimStartSec,
              onValueChange = { trimStartSec = it.filter { c -> c.isDigit() } },
              label = { Text("Trim Start Offset (seconds)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Apply",
          onClick = {
            val durMs = (durationSec.toLongOrNull() ?: 5L).coerceIn(1L, 120L) * 1000L
            val trimMs = (trimStartSec.toLongOrNull() ?: 0L).coerceAtLeast(0L) * 1000L
            editorViewModel.updateClip(clip.id, title = title, trimStartMs = trimMs, durationMs = durMs)
            editingClip = null
          },
          gradientColors = ZuzuVioletPinkGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { editingClip = null }) { Text("Cancel") }
      },
    )
  }

  if (showOverlayDialog) {
    TextOverlayDialog(
      initial = editingOverlay,
      defaultEndMs = draft.totalDurationMs.coerceAtLeast(5_000L),
      onDismiss = { showOverlayDialog = false },
      onSave = {
        editorViewModel.addTextOverlay(it)
        showOverlayDialog = false
      },
    )
  }

  if (showLibraryPicker) {
    val visualStudioAssets = studioAssets.filter {
      (it.mediaType == MediaType.Image || it.mediaType == MediaType.Video) && !it.localUriString.isNullOrBlank()
    }
    val visualSourceEntries = mediaLibraryEntries.filter {
      (it.mediaType == MediaType.Image || it.mediaType == MediaType.Video) && !it.localUriString.isNullOrBlank()
    }
    AlertDialog(
      onDismissRequest = { showLibraryPicker = false },
      containerColor = ZuzuWhite,
      title = {
        Text("Pick Image or Clip from Library", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (visualStudioAssets.isEmpty() && visualSourceEntries.isEmpty()) {
            Text(
              text = "No local images or video clips found in Asset Library or Media Library yet.",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          }
          visualStudioAssets.forEach { asset ->
            ZuzuSecondaryActionButton(
              text = asset.title,
              onClick = {
                editorViewModel.addMediaClip(
                  title = asset.title,
                  clipType = if (asset.mediaType == MediaType.Video) EditorClipType.Video else EditorClipType.Image,
                  uriString = asset.localUriString,
                  assetId = asset.id,
                  durationMs = (asset.durationMs ?: 5_000L).coerceIn(2_000L, 60_000L),
                )
                showLibraryPicker = false
              },
              accentColor = if (asset.mediaType == MediaType.Video) ZuzuPink else ZuzuElectricBlue,
              icon = if (asset.mediaType == MediaType.Video) Icons.Rounded.Movie else Icons.Rounded.Image,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          visualSourceEntries.forEach { entry ->
            ZuzuSecondaryActionButton(
              text = "${entry.title} (Media Library)",
              onClick = {
                editorViewModel.addMediaClip(
                  title = entry.title,
                  clipType = if (entry.mediaType == MediaType.Video) EditorClipType.Video else EditorClipType.Image,
                  uriString = entry.localUriString,
                  durationMs = (entry.durationMs ?: 5_000L).coerceIn(2_000L, 60_000L),
                )
                showLibraryPicker = false
              },
              accentColor = ZuzuTurquoise,
              icon = if (entry.mediaType == MediaType.Video) Icons.Rounded.Movie else Icons.Rounded.Image,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLibraryPicker = false }) {
          Text("Close", color = ZuzuPurple, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}

@Composable
private fun TextOverlayDialog(
  initial: TextOverlay?,
  defaultEndMs: Long,
  onDismiss: () -> Unit,
  onSave: (TextOverlay) -> Unit,
) {
  var text by remember { mutableStateOf(initial?.text ?: "") }
  var startSec by remember { mutableStateOf(((initial?.startMs ?: 0L) / 1000L).toString()) }
  var endSec by remember { mutableStateOf(((initial?.endMs ?: defaultEndMs) / 1000L).toString()) }
  var fontSize by remember { mutableStateOf((initial?.fontSizeSp ?: 22).toString()) }
  var position by remember { mutableStateOf(initial?.position ?: OverlayPosition.Top) }
  var colorArgb by remember { mutableStateOf(initial?.colorArgb ?: 0xFFFFFFFF) }

  val colorSwatches = listOf(0xFFFFFFFFL, 0xFFFFEB3BL, 0xFF06B6D4L, 0xFFEC4899L, 0xFF1E1B4BL)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = if (initial == null) "Add Text Overlay" else "Edit Text Overlay",
        fontWeight = FontWeight.ExtraBold,
        color = ZuzuInkPrimary,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Overlay Text (Bangla / English)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startSec,
            onValueChange = { startSec = it.filter { c -> c.isDigit() } },
            label = { Text("Start (s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = endSec,
            onValueChange = { endSec = it.filter { c -> c.isDigit() } },
            label = { Text("End (s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = fontSize,
            onValueChange = { fontSize = it.filter { c -> c.isDigit() } },
            label = { Text("Size") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        Text("Position", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OverlayPosition.entries.forEach { pos ->
            ZuzuPillChip(
              label = pos.displayLabel,
              selected = position == pos,
              onClick = { position = pos },
              gradientColors = listOf(ZuzuPurple, ZuzuPink),
            )
          }
        }
        Text("Text Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          colorSwatches.forEach { swatch ->
            Surface(
              shape = CircleShape,
              color = Color(swatch),
              border = BorderStroke(
                width = if (colorArgb == swatch) 3.dp else 1.dp,
                color = if (colorArgb == swatch) ZuzuPurple else ZuzuInkSecondary,
              ),
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { colorArgb = swatch },
            ) {}
          }
        }
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Save",
        onClick = {
          val start = (startSec.toLongOrNull() ?: 0L) * 1000L
          val end = maxOf((endSec.toLongOrNull() ?: 5L) * 1000L, start + 500L)
          onSave(
            TextOverlay(
              id = initial?.id ?: UUID.randomUUID().toString(),
              text = text.trim(),
              startMs = start,
              endMs = end,
              fontSizeSp = (fontSize.toIntOrNull() ?: 22).coerceIn(12, 42),
              position = position,
              colorArgb = colorArgb,
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
