package com.example.ui.screens.motion

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.motion.MotionCanvasDrawer
import com.example.model.MediaType
import com.example.model.MotionExportFormat
import com.example.model.MotionPostProject
import com.example.model.VideoAspectRatio
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPinkSoft
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.viewmodel.MotionStudioUiState
import com.example.viewmodel.MotionStudioViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun MotionPreviewPlayerCard(
  project: MotionPostProject,
  uiState: MotionStudioUiState,
  viewModel: MotionStudioViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  var mediaBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

  val primarySlot = project.mediaSlots.firstOrNull()
  LaunchedEffect(primarySlot?.uriString, primarySlot?.mediaType) {
    mediaBitmap = if (primarySlot == null) {
      null
    } else if (primarySlot.mediaType == MediaType.Video) {
      val ret = MediaMetadataRetriever()
      runCatching {
        ret.setDataSource(context, Uri.parse(primarySlot.uriString))
        ret.getFrameAtTime(400_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      }.also { runCatching { ret.release() } }.getOrNull()
    } else {
      runCatching {
        context.contentResolver.openInputStream(Uri.parse(primarySlot.uriString))?.use { stream ->
          BitmapFactory.decodeStream(stream)
        }
      }.getOrNull()
    }
  }

  LaunchedEffect(project.logoUriString) {
    logoBitmap = project.logoUriString?.let { uriStr ->
      runCatching {
        context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
          BitmapFactory.decodeStream(stream)
        }
      }.getOrNull()
    }
  }

  // Background music playback during preview
  val audioPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
  LaunchedEffect(uiState.isPreviewPlaying, project.backgroundMusicUriString, project.backgroundMusicVolume) {
    val uriStr = project.backgroundMusicUriString
    if (uiState.isPreviewPlaying && !uriStr.isNullOrBlank()) {
      runCatching {
        audioPlayer.value?.release()
        MediaPlayer.create(context, Uri.parse(uriStr))?.apply {
          setVolume(project.backgroundMusicVolume, project.backgroundMusicVolume)
          isLooping = true
          start()
          audioPlayer.value = this
        }
      }
    } else {
      runCatching { audioPlayer.value?.pause() }
      runCatching { audioPlayer.value?.release() }
      audioPlayer.value = null
    }
  }
  DisposableEffect(Unit) {
    onDispose {
      runCatching { audioPlayer.value?.release() }
      audioPlayer.value = null
    }
  }

  // Animation loop while preview playing
  LaunchedEffect(uiState.isPreviewPlaying, project.durationSeconds) {
    var cursor = uiState.previewProgress
    val stepPerTick = 0.08f / project.durationSeconds.coerceIn(3, 30).toFloat()
    while (uiState.isPreviewPlaying) {
      delay(80L)
      cursor += stepPerTick
      if (cursor >= 1f) cursor = 0f
      viewModel.setPreviewProgress(cursor)
    }
  }

  // Render preview frame on canvas bitmap
  LaunchedEffect(
    uiState.previewProgress,
    project.title,
    project.headlineText,
    project.subheadlineText,
    project.templateCategory,
    project.animationStyle,
    project.transition,
    project.aspectRatio,
    project.textColorArgb,
    project.textFontSizeSp,
    project.logoLabel,
    project.stickers,
    mediaBitmap,
    logoBitmap,
  ) {
    val (w, h) = when (project.aspectRatio) {
      VideoAspectRatio.Vertical -> 270 to 480
      VideoAspectRatio.Landscape -> 480 to 270
      VideoAspectRatio.Square -> 360 to 360
    }
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    MotionCanvasDrawer.drawMotionFrame(
      canvas = canvas,
      width = w,
      height = h,
      progress = uiState.previewProgress,
      project = project,
      mediaBitmap = mediaBitmap,
      logoBitmap = logoBitmap,
    )
    previewBitmap = bmp
  }

  val ratioFloat = when (project.aspectRatio) {
    VideoAspectRatio.Vertical -> 9f / 16f
    VideoAspectRatio.Landscape -> 16f / 9f
    VideoAspectRatio.Square -> 1f
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuPink, ZuzuOrange),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("motion_preview_stage"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusPill(
        label = "${project.aspectRatio.label} • ${project.durationSeconds}s",
        tint = ZuzuPink,
      )
      StatusPill(
        label = if (uiState.hasUnsavedChanges) "Unsaved Draft" else "Saved",
        tint = if (uiState.hasUnsavedChanges) ZuzuAmber else ZuzuTurquoise,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF0F172A),
      border = BorderStroke(
        width = 1.dp,
        brush = Brush.horizontalGradient(listOf(ZuzuPink.copy(alpha = 0.55f), ZuzuCyan.copy(alpha = 0.55f))),
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(225.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
      ) {
        val rendered = previewBitmap
        if (rendered != null) {
          Image(
            bitmap = rendered.asImageBitmap(),
            contentDescription = project.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .height(225.dp)
              .aspectRatio(ratioFloat)
              .clip(RoundedCornerShape(14.dp)),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Slider(
      value = uiState.previewProgress,
      onValueChange = viewModel::setPreviewProgress,
      colors = SliderDefaults.colors(
        thumbColor = ZuzuPink,
        activeTrackColor = ZuzuMagenta,
        inactiveTrackColor = ZuzuPurpleSoft,
      ),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = ZuzuPinkSoft,
        ) {
          IconButton(
            onClick = { viewModel.setPreviewPlaying(!uiState.isPreviewPlaying) },
            modifier = Modifier.testTag("motion_play_pause_btn"),
          ) {
            Icon(
              imageVector = if (uiState.isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (uiState.isPreviewPlaying) "Pause preview" else "Play preview",
              tint = ZuzuPink,
            )
          }
        }
        Surface(
          shape = CircleShape,
          color = ZuzuOrange.copy(alpha = 0.12f),
        ) {
          IconButton(
            onClick = {
              viewModel.setPreviewPlaying(false)
              viewModel.setPreviewProgress(0f)
            },
          ) {
            Icon(Icons.Rounded.Stop, contentDescription = "Reset preview", tint = ZuzuOrange)
          }
        }
        Text(
          text = "${(uiState.previewProgress * project.durationSeconds).roundToInt()}s / ${project.durationSeconds}s",
          style = MaterialTheme.typography.labelMedium,
          color = ZuzuInkPrimary,
          fontWeight = FontWeight.Bold,
        )
      }

      ZuzuSecondaryActionButton(
        text = "Save",
        onClick = viewModel::saveProject,
        accentColor = ZuzuPurple,
        icon = Icons.Rounded.Save,
        modifier = Modifier.testTag("motion_save_btn"),
      )
    }
  }
}

@Composable
fun MotionExportAndApprovalCard(
  uiState: MotionStudioUiState,
  viewModel: MotionStudioViewModel,
  onSendToApproval: () -> Unit,
  glowEnabled: Boolean,
) {
  val project = uiState.activeProject

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuViolet, ZuzuPink),
    modifier = Modifier.fillMaxWidth(),
  ) {
    SectionHeader(
      title = "Export & Workflow Handoff",
      subtitle = if (uiState.canEncodeMp4) {
        "Export native H.264 MP4 animation or high-res PNG/JPEG graphic, then submit for explicit user approval."
      } else {
        "MP4 hardware encoder unavailable here; PNG & JPEG poster export remains ready."
      },
      gradientColors = ZuzuVioletPinkGradient,
    )

    Spacer(modifier = Modifier.height(14.dp))

    if (uiState.exportState.isExporting) {
      LinearProgressIndicator(
        progress = { uiState.exportState.progress },
        color = ZuzuPink,
        trackColor = ZuzuPurpleSoft,
        strokeCap = StrokeCap.Round,
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(CircleShape),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = uiState.exportState.stageLabel,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        ZuzuSecondaryActionButton(
          text = "Cancel",
          onClick = viewModel::cancelExport,
          accentColor = ZuzuError,
          icon = Icons.Rounded.Cancel,
        )
      }
    } else {
      ZuzuGradientButton(
        text = "Export MP4",
        onClick = { viewModel.exportMotionPost(MotionExportFormat.Mp4Video) },
        gradientColors = listOf(ZuzuPink, ZuzuMagenta, ZuzuOrange),
        icon = Icons.Rounded.Movie,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("motion_export_mp4_btn"),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Export PNG",
          onClick = { viewModel.exportMotionPost(MotionExportFormat.PngImage) },
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.Image,
          modifier = Modifier
            .weight(1f)
            .testTag("motion_export_png_btn"),
        )
        ZuzuSecondaryActionButton(
          text = "Export JPG",
          onClick = { viewModel.exportMotionPost(MotionExportFormat.JpegImage) },
          accentColor = ZuzuTurquoise,
          icon = Icons.Rounded.Image,
          modifier = Modifier
            .weight(1f)
            .testTag("motion_export_jpg_btn"),
        )
      }
    }

    if (!project.exportedFilePath.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(12.dp))
      ZuzuAlertBanner(
        message = "Last export (${project.exportedFormat?.displayLabel ?: "File"}) saved to Media Library.",
        tint = ZuzuTurquoise,
        icon = Icons.Rounded.CheckCircle,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))
    ZuzuGradientButton(
      text = "Send to Approval Center",
      onClick = {
        viewModel.sendToApprovalCenter(onQueued = onSendToApproval)
      },
      gradientColors = listOf(ZuzuElectricBlue, ZuzuPurple, ZuzuMagenta),
      icon = Icons.Rounded.Approval,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("motion_send_to_approval_btn"),
    )
  }
}
