package com.example.ui.screens.editor

import android.graphics.Bitmap
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.source.MediaFileHelper
import com.example.model.ExportedVideoRecord
import com.example.model.MediaType
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoEditorUiState
import com.example.viewmodel.VideoEditorViewModel

@Composable
fun ExportedVideoLibrarySection(
  editorUiState: VideoEditorUiState,
  editorViewModel: VideoEditorViewModel,
  glowEnabled: Boolean,
  onSendToApproval: ((ExportedVideoRecord) -> Unit)? = null,
) {
  val context = LocalContext.current
  var previewRecord by remember { mutableStateOf<ExportedVideoRecord?>(null) }
  var renameRecord by remember { mutableStateOf<ExportedVideoRecord?>(null) }
  var deleteRecord by remember { mutableStateOf<ExportedVideoRecord?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = stringResource(R.string.exported_library_title),
      subtitle = stringResource(R.string.exported_library_subtitle),
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    val exports = editorUiState.exportedVideos
    if (exports.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Exported MP4 Videos Yet",
        description = "Open the Video Editor, configure your timeline and subtitles, and render an MP4 video.",
        icon = Icons.Rounded.VideoLibrary,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("exported_library_empty"),
      )
    } else {
      exports.forEach { video ->
        ExportedVideoCard(
          record = video,
          glowEnabled = glowEnabled,
          onPreview = { previewRecord = video },
          onRename = { renameRecord = video },
          onShare = { shareExportedMp4(context, video) },
          onDelete = { deleteRecord = video },
          onSendToApproval = onSendToApproval?.let { cb -> { cb(video) } },
        )
      }
    }
  }

  previewRecord?.let { record ->
    Mp4VideoPlayerDialog(
      record = record,
      onDismiss = { previewRecord = null },
      onShare = { shareExportedMp4(context, record) },
    )
  }

  renameRecord?.let { record ->
    var nameInput by remember { mutableStateOf(record.fileName.removeSuffix(".mp4")) }
    AlertDialog(
      onDismissRequest = { renameRecord = null },
      containerColor = ZuzuWhite,
      title = {
        Text("Rename Exported Video", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("MP4 File Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Rename",
          onClick = {
            editorViewModel.renameExportedVideo(record.id, nameInput)
            renameRecord = null
          },
          enabled = nameInput.isNotBlank(),
          gradientColors = ZuzuBlueTurquoiseGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { renameRecord = null }) { Text("Cancel") }
      },
    )
  }

  deleteRecord?.let { record ->
    AlertDialog(
      onDismissRequest = { deleteRecord = null },
      containerColor = ZuzuWhite,
      title = {
        Text("Delete Exported Video?", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Text(
          text = "Remove '${record.fileName}' from the Exported Video Library and delete the local MP4 file?",
          color = ZuzuInkSecondary,
        )
      },
      confirmButton = {
        ZuzuSecondaryActionButton(
          text = "Delete",
          onClick = {
            editorViewModel.deleteExportedVideo(record.id)
            deleteRecord = null
          },
          accentColor = ZuzuError,
          icon = Icons.Rounded.DeleteOutline,
        )
      },
      dismissButton = {
        TextButton(onClick = { deleteRecord = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun ExportedVideoCard(
  record: ExportedVideoRecord,
  glowEnabled: Boolean,
  onPreview: () -> Unit,
  onRename: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit,
  onSendToApproval: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  var thumbnail by remember(record.id, record.fileUriString) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(record.id, record.fileUriString) {
    thumbnail = MediaFileHelper.loadPreviewBitmap(context, record.fileUriString, MediaType.Video)
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuElectricBlue, ZuzuPink),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(width = 76.dp, height = 96.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Brush.linearGradient(listOf(ZuzuSoftLavender, ZuzuPurple.copy(alpha = 0.14f)))),
      ) {
        if (thumbnail != null) {
          Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = record.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
          )
        } else {
          Icon(Icons.Rounded.Movie, contentDescription = null, tint = ZuzuPurple, modifier = Modifier.size(32.dp))
        }
      }

      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = record.fileName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Resolution: ${record.width}×${record.height} (${record.aspectRatio.label})",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
        Text(
          text = "Duration: ${MediaFileHelper.formatDuration(record.durationMs) ?: "0:00"} • Size: ${MediaFileHelper.formatFileSize(record.fileSizeBytes)}",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
        if (record.savedMediaEntryId != null) {
          Spacer(modifier = Modifier.height(6.dp))
          StatusPill(label = "Saved in Media Library", tint = ZuzuTurquoise)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ZuzuSecondaryActionButton(
        text = "Preview",
        onClick = onPreview,
        accentColor = ZuzuElectricBlue,
        icon = Icons.Rounded.PlayCircle,
        modifier = Modifier.weight(1f),
      )
      ZuzuSecondaryActionButton(
        text = "Share",
        onClick = onShare,
        accentColor = ZuzuPurple,
        icon = Icons.Rounded.Share,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onRename) {
        Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Rename", tint = ZuzuCyan)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = ZuzuError)
      }
    }
    if (onSendToApproval != null) {
      Spacer(modifier = Modifier.height(10.dp))
      ZuzuGradientButton(
        text = "Send to Approval Center",
        onClick = onSendToApproval,
        gradientColors = ZuzuVioletPinkGradient,
        icon = Icons.Rounded.Approval,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("export_send_approval_${record.id}"),
      )
    }
  }
}

@Composable
fun Mp4VideoPlayerDialog(
  record: ExportedVideoRecord,
  onDismiss: () -> Unit,
  onShare: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(record.fileName, fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
          contentAlignment = Alignment.Center,
        ) {
          AndroidView(
            factory = { ctx ->
              VideoView(ctx).apply {
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(record.fileUriString))
                setOnPreparedListener { mp ->
                  mp.isLooping = false
                  start()
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp),
          )
        }
        Text(
          text = "${record.width}×${record.height} • ${MediaFileHelper.formatFileSize(record.fileSizeBytes)}",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Share MP4",
        onClick = onShare,
        gradientColors = ZuzuBlueTurquoiseGradient,
        icon = Icons.Rounded.Share,
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}
