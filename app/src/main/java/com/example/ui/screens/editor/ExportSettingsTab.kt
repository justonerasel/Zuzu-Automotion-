package com.example.ui.screens.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.editor.RendererCapabilityStatus
import com.example.data.source.MediaFileHelper
import com.example.model.ExportQuality
import com.example.model.ExportResolution
import com.example.model.ExportedVideoRecord
import com.example.model.VideoAspectRatio
import com.example.model.VideoEditorDraft
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.viewmodel.VideoEditorUiState
import com.example.viewmodel.VideoEditorViewModel
import kotlin.math.roundToInt

@Composable
fun ExportSettingsTab(
  draft: VideoEditorDraft,
  editorUiState: VideoEditorUiState,
  editorViewModel: VideoEditorViewModel,
  onOpenExportLibrary: () -> Unit,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  val settings = draft.exportSettings
  val renderState = editorUiState.renderState
  val estimatedBytes = editorUiState.estimatedFileSizeBytes
  var previewRecord by remember { mutableStateOf<ExportedVideoRecord?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = "Export Settings & MP4 Renderer",
      subtitle = "MP4 (H.264 + AAC) • ${settings.outputWidth}x${settings.outputHeight}",
      gradientColors = ZuzuVioletPinkGradient,
    )

    // Engine Status Card
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          ZuzuGradientIconBadge(
            icon = Icons.Rounded.Tune,
            contentDescription = "Render Engine",
            gradientColors = ZuzuBlueTurquoiseGradient,
            size = 38.dp,
            iconSize = 20.dp,
            cornerRadius = 12.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Render Engine",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
        }
        when (val cap = editorUiState.rendererCapability) {
          is RendererCapabilityStatus.Available -> StatusPill(label = cap.engineName, tint = ZuzuTurquoise)
          is RendererCapabilityStatus.Unavailable -> StatusPill(label = "Unavailable", tint = ZuzuAmber)
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = when (val cap = editorUiState.rendererCapability) {
          is RendererCapabilityStatus.Available -> cap.description
          is RendererCapabilityStatus.Unavailable -> cap.reason
        },
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
      )
    }

    // Export Options Card
    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuPurple, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Output Format",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        StatusPill(label = "MP4 (H.264 / AAC)", tint = ZuzuPurple)
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Aspect Ratio",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoAspectRatio.entries.forEach { ratio ->
          ZuzuPillChip(
            label = "${ratio.label} • ${ratio.description}",
            selected = settings.aspectRatio == ratio,
            onClick = { editorViewModel.setExportAspectRatio(ratio) },
            gradientColors = ZuzuBlueTurquoiseGradient,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Resolution",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ExportResolution.entries.forEach { res ->
          ZuzuPillChip(
            label = res.displayLabel,
            selected = settings.resolution == res,
            onClick = { editorViewModel.setExportResolution(res) },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuPurple),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Export Quality",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ExportQuality.entries.forEach { q ->
          ZuzuPillChip(
            label = "${q.displayLabel} (${q.fps}fps)",
            selected = settings.quality == q,
            onClick = { editorViewModel.setExportQuality(q) },
            gradientColors = ZuzuVioletPinkGradient,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = ZuzuSoftLavender,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Resolution: ${settings.outputWidth} × ${settings.outputHeight} MP4",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (estimatedBytes != null) {
              "Estimated File Size: ~${MediaFileHelper.formatFileSize(estimatedBytes)} (target bitrate estimate)"
            } else {
              "Estimated File Size: Add at least one timeline clip to calculate."
            },
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }
    }

    // Active Render Progress or Action Buttons
    if (renderState.isRendering) {
      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = listOf(ZuzuPink, ZuzuOrange),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Rendering MP4 (${(renderState.progressFraction * 100).roundToInt()}%)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuPurple,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = renderState.stageLabel,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
          progress = { renderState.progressFraction },
          color = ZuzuMagenta,
          trackColor = ZuzuPurpleSoft,
          strokeCap = StrokeCap.Round,
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ZuzuSecondaryActionButton(
          text = "Cancel Render",
          onClick = editorViewModel::cancelRender,
          accentColor = ZuzuError,
          icon = Icons.Rounded.Cancel,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_cancel_render_btn"),
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ZuzuSecondaryActionButton(
          text = "Save Draft",
          onClick = editorViewModel::saveDraft,
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.Save,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_export_save_draft_btn"),
        )
        ZuzuGradientButton(
          text = "Render MP4",
          onClick = editorViewModel::startRender,
          gradientColors = ZuzuVioletPinkGradient,
          icon = Icons.Rounded.Movie,
          enabled = draft.clips.isNotEmpty(),
          modifier = Modifier
            .weight(1.4f)
            .testTag("editor_render_mp4_btn"),
        )
      }
    }

    // Completed Export Quick Result Card
    renderState.lastExportedVideo?.let { exported ->
      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = listOf(ZuzuTurquoise, ZuzuGreen),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ZuzuGradientIconBadge(
            icon = Icons.Rounded.CheckCircle,
            contentDescription = null,
            gradientColors = listOf(ZuzuTurquoise, ZuzuGreen),
            size = 40.dp,
            iconSize = 22.dp,
            cornerRadius = 12.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = exported.fileName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ZuzuInkPrimary,
            )
            Text(
              text = "${exported.width}x${exported.height} • ${MediaFileHelper.formatDuration(exported.durationMs) ?: "0:00"} • ${MediaFileHelper.formatFileSize(exported.fileSizeBytes)}",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ZuzuSecondaryActionButton(
            text = "Preview",
            onClick = { previewRecord = exported },
            accentColor = ZuzuElectricBlue,
            icon = Icons.Rounded.PlayCircle,
            modifier = Modifier.weight(1f),
          )
          ZuzuSecondaryActionButton(
            text = "Share",
            onClick = { shareExportedMp4(context, exported) },
            accentColor = ZuzuPurple,
            icon = Icons.Rounded.Share,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ZuzuGradientButton(
          text = "Open Exported Video Library",
          onClick = onOpenExportLibrary,
          gradientColors = ZuzuBlueTurquoiseGradient,
          icon = Icons.Rounded.VideoLibrary,
          modifier = Modifier.fillMaxWidth(),
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
}

fun shareExportedMp4(context: Context, record: ExportedVideoRecord) {
  val uri = runCatching { Uri.parse(record.fileUriString) }.getOrNull() ?: return
  val sendIntent = Intent(Intent.ACTION_SEND).apply {
    type = "video/mp4"
    putExtra(Intent.EXTRA_STREAM, uri)
    putExtra(Intent.EXTRA_SUBJECT, record.fileName)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  val chooser = Intent.createChooser(sendIntent, "Share ${record.fileName}")
  runCatching { context.startActivity(chooser) }
}
