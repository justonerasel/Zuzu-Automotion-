package com.example.ui.screens.editor

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.source.MediaFileHelper
import com.example.model.EditorClipType
import com.example.model.EditorTimelineClip
import com.example.model.EditorVisualEffect
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.OverlayPosition
import com.example.model.StudioAsset
import com.example.model.SubtitleBackgroundStyle
import com.example.model.SubtitlePosition
import com.example.model.VideoAspectRatio
import com.example.model.VideoEditorDraft
import com.example.model.VideoEditorTab
import com.example.model.VideoFactorySection
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSegmentedTabStrip
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
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
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoEditorUiState
import com.example.viewmodel.VideoEditorViewModel
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel
import kotlinx.coroutines.delay

@Composable
fun VideoEditorSection(
  factoryUiState: VideoFactoryUiState,
  studioAssets: List<StudioAsset>,
  mediaLibraryEntries: List<MediaLibraryEntry>,
  editorUiState: VideoEditorUiState,
  factoryViewModel: VideoFactoryViewModel,
  editorViewModel: VideoEditorViewModel,
  glowEnabled: Boolean,
) {
  val project = factoryUiState.activeProject
  var showDraftsDialog by remember { mutableStateOf(false) }

  LaunchedEffect(project?.id) {
    editorViewModel.openProjectInEditor(
      project = project,
      studioAssets = studioAssets,
      forceReloadFromProject = false,
    )
  }

  val draft = editorUiState.currentDraft ?: return

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = stringResource(R.string.video_editor_title),
      subtitle = draft.title,
      gradientColors = ZuzuVioletPinkGradient,
    )

    // Draft Save & Resume Controls
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
          label = if (editorUiState.hasUnsavedChanges) "Unsaved Edits" else "Draft Saved",
          tint = if (editorUiState.hasUnsavedChanges) ZuzuAmber else ZuzuTurquoise,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ZuzuSecondaryActionButton(
            text = "Save Draft",
            onClick = editorViewModel::saveDraft,
            accentColor = ZuzuPurple,
            icon = Icons.Rounded.Save,
            modifier = Modifier.testTag("editor_save_draft_btn"),
          )
          if (editorUiState.savedDrafts.isNotEmpty()) {
            ZuzuSecondaryActionButton(
              text = "Resume (${editorUiState.savedDrafts.size})",
              onClick = { showDraftsDialog = true },
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.FolderOpen,
            )
          }
        }
      }
      if (project != null) {
        Spacer(modifier = Modifier.height(10.dp))
        ZuzuSecondaryActionButton(
          text = "Re-Sync from Project Storyboard & Assets",
          onClick = {
            editorViewModel.openProjectInEditor(
              project = project,
              studioAssets = studioAssets,
              forceReloadFromProject = true,
            )
          },
          accentColor = ZuzuCyan,
          icon = Icons.Rounded.Sync,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    // Interactive Video Preview Player
    InteractiveTimelinePreviewPlayer(
      draft = draft,
      playheadMs = editorUiState.playheadMs,
      isPlaying = editorUiState.isPreviewPlaying,
      onPlayPause = { editorViewModel.setPreviewPlaying(!editorUiState.isPreviewPlaying) },
      onStop = {
        editorViewModel.setPreviewPlaying(false)
        editorViewModel.setPlayheadMs(0L)
      },
      onSeek = editorViewModel::setPlayheadMs,
      glowEnabled = glowEnabled,
    )

    // Sub-Tab Selector
    ZuzuSegmentedTabStrip(
      items = VideoEditorTab.entries,
      selectedItem = editorUiState.activeTab,
      labelFor = { it.displayLabel },
      onSelect = editorViewModel::selectTab,
      gradientColors = ZuzuVioletPinkGradient,
      testTagFor = { it.testTag },
      scrollable = true,
    )

    // Active Editor Sub-Tab Content
    when (editorUiState.activeTab) {
      VideoEditorTab.Timeline -> TimelineEditorTab(
        draft = draft,
        studioAssets = studioAssets,
        mediaLibraryEntries = mediaLibraryEntries,
        editorViewModel = editorViewModel,
        glowEnabled = glowEnabled,
      )
      VideoEditorTab.Subtitles -> SubtitleEditorTab(
        draft = draft,
        activeProject = project,
        editorViewModel = editorViewModel,
        glowEnabled = glowEnabled,
      )
      VideoEditorTab.Audio -> AudioEditorTab(
        draft = draft,
        studioAssets = studioAssets,
        mediaLibraryEntries = mediaLibraryEntries,
        editorViewModel = editorViewModel,
        glowEnabled = glowEnabled,
      )
      VideoEditorTab.Export -> ExportSettingsTab(
        draft = draft,
        editorUiState = editorUiState,
        editorViewModel = editorViewModel,
        onOpenExportLibrary = { factoryViewModel.selectSection(VideoFactorySection.ExportedLibrary) },
        glowEnabled = glowEnabled,
      )
    }
  }

  if (showDraftsDialog) {
    AlertDialog(
      onDismissRequest = { showDraftsDialog = false },
      containerColor = ZuzuWhite,
      title = {
        Text(
          text = "Resume Saved Draft",
          fontWeight = FontWeight.ExtraBold,
          color = ZuzuInkPrimary,
        )
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          editorUiState.savedDrafts.forEach { saved ->
            ZuzuSecondaryActionButton(
              text = "${saved.title} (${saved.clips.size} clips)",
              onClick = {
                editorViewModel.resumeSavedDraft(saved)
                showDraftsDialog = false
              },
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.Movie,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDraftsDialog = false }) {
          Text("Close", color = ZuzuPurple, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}

@Composable
private fun InteractiveTimelinePreviewPlayer(
  draft: VideoEditorDraft,
  playheadMs: Long,
  isPlaying: Boolean,
  onPlayPause: () -> Unit,
  onStop: () -> Unit,
  onSeek: (Long) -> Unit,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  val bitmapCache = remember { mutableStateMapOf<String, Bitmap?>() }

  // Load preview bitmaps cleanly for timeline clips
  LaunchedEffect(draft.clips) {
    draft.clips.forEach { clip ->
      val uri = clip.mediaUriString
      if (!uri.isNullOrBlank() && !bitmapCache.containsKey(clip.id)) {
        val type = if (clip.clipType == EditorClipType.Video) MediaType.Video else MediaType.Image
        bitmapCache[clip.id] = MediaFileHelper.loadPreviewBitmap(context, uri, type)
      }
    }
  }

  // Audio playback during timeline preview
  val previewAudioPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
  LaunchedEffect(isPlaying, draft.audioTracks) {
    if (isPlaying) {
      val primaryTrack = draft.audioTracks.firstOrNull { !it.isMuted && it.volume > 0.01f }
      if (primaryTrack != null) {
        runCatching {
          previewAudioPlayer.value?.release()
          MediaPlayer.create(context, Uri.parse(primaryTrack.uriString))?.apply {
            setVolume(primaryTrack.volume, primaryTrack.volume)
            val targetPos = (primaryTrack.trimStartMs + playheadMs).toInt()
            if (targetPos in 0 until duration) seekTo(targetPos)
            start()
            previewAudioPlayer.value = this
          }
        }
      }
    } else {
      runCatching { previewAudioPlayer.value?.pause() }
      runCatching { previewAudioPlayer.value?.release() }
      previewAudioPlayer.value = null
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      runCatching { previewAudioPlayer.value?.release() }
      previewAudioPlayer.value = null
    }
  }

  // Advance playhead while playing
  LaunchedEffect(isPlaying, draft.totalDurationMs) {
    var cursor = playheadMs
    val maxDur = draft.totalDurationMs
    while (isPlaying && maxDur > 0L) {
      delay(100L)
      cursor += 100L
      if (cursor >= maxDur) {
        onSeek(0L)
        onPlayPause()
        break
      } else {
        onSeek(cursor)
      }
    }
  }

  val activeClip = resolveActiveClip(draft.clips, playheadMs)
  val activeOverlays = draft.textOverlays.filter { playheadMs in it.startMs..it.endMs }
  val activeSubtitles = draft.subtitles.filter { playheadMs in it.startMs..it.endMs }
  val ratioFloat = when (draft.exportSettings.aspectRatio) {
    VideoAspectRatio.Vertical -> 9f / 16f
    VideoAspectRatio.Landscape -> 16f / 9f
    VideoAspectRatio.Square -> 1f
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuViolet, ZuzuPink),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("editor_preview_player"),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xFF0F1020),
      border = BorderStroke(
        width = 1.dp,
        brush = Brush.horizontalGradient(listOf(ZuzuPurple.copy(alpha = 0.55f), ZuzuCyan.copy(alpha = 0.55f))),
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(216.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
      ) {
        Box(
          modifier = Modifier
            .height(216.dp)
            .aspectRatio(ratioFloat)
            .clip(RoundedCornerShape(12.dp))
            .background(
              Brush.linearGradient(listOf(ZuzuDeepPurple, ZuzuElectricBlue, ZuzuPink)),
            ),
          contentAlignment = Alignment.Center,
        ) {
          if (activeClip != null) {
            val bmp = bitmapCache[activeClip.id]
            if (bmp != null) {
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = activeClip.title,
                contentScale = ContentScale.Crop,
                colorFilter = composeEffectFilter(activeClip.visualEffect),
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              Text(
                text = activeClip.title,
                color = ZuzuWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp),
              )
            }
          } else {
            Text(
              text = "No Clips",
              color = ZuzuWhite.copy(alpha = 0.8f),
              style = MaterialTheme.typography.bodySmall,
            )
          }

          activeOverlays.forEach { overlay ->
            val align = when (overlay.position) {
              OverlayPosition.Top -> Alignment.TopCenter
              OverlayPosition.Center -> Alignment.Center
              OverlayPosition.Bottom -> Alignment.BottomCenter
            }
            Box(
              contentAlignment = align,
              modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            ) {
              Text(
                text = overlay.text,
                color = Color(overlay.colorArgb),
                fontSize = (overlay.fontSizeSp * 0.7f).coerceIn(11f, 20f).sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
              )
            }
          }

          activeSubtitles.forEach { cue ->
            val align = when (cue.position) {
              SubtitlePosition.Top -> Alignment.TopCenter
              SubtitlePosition.Middle -> Alignment.Center
              SubtitlePosition.Bottom -> Alignment.BottomCenter
            }
            val bg = when (cue.backgroundStyle) {
              SubtitleBackgroundStyle.None -> Color.Transparent
              SubtitleBackgroundStyle.DarkTranslucent -> Color(0xBA10101A)
              SubtitleBackgroundStyle.PurpleGlass -> Color(0xCC6D28D9)
              SubtitleBackgroundStyle.SolidBlack -> Color.Black
              SubtitleBackgroundStyle.SolidWhite -> Color.White
            }
            Box(
              contentAlignment = align,
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            ) {
              Surface(shape = RoundedCornerShape(6.dp), color = bg) {
                Text(
                  text = cue.text,
                  color = Color(cue.textColorArgb),
                  fontSize = (cue.fontSizeSp * 0.65f).coerceIn(10f, 18f).sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    val totalMs = draft.totalDurationMs.coerceAtLeast(1L)
    Slider(
      value = (playheadMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f),
      onValueChange = { frac -> onSeek((frac * totalMs).toLong()) },
      enabled = draft.totalDurationMs > 0L,
      colors = SliderDefaults.colors(
        thumbColor = ZuzuMagenta,
        activeTrackColor = ZuzuPurple,
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
          color = ZuzuSoftLavender,
        ) {
          IconButton(
            onClick = onPlayPause,
            enabled = draft.clips.isNotEmpty(),
            modifier = Modifier.testTag("editor_play_pause_btn"),
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = if (isPlaying) "Pause preview" else "Play preview",
              tint = ZuzuPurple,
            )
          }
        }
        Surface(
          shape = CircleShape,
          color = ZuzuOrange.copy(alpha = 0.12f),
        ) {
          IconButton(
            onClick = onStop,
            enabled = playheadMs > 0L || isPlaying,
          ) {
            Icon(Icons.Rounded.Stop, contentDescription = "Stop preview", tint = ZuzuOrange)
          }
        }
      }

      StatusPill(
        label = "${MediaFileHelper.formatDuration(playheadMs) ?: "0:00"} / ${MediaFileHelper.formatDuration(draft.totalDurationMs) ?: "0:00"}",
        tint = ZuzuElectricBlue,
      )
    }
  }
}

private fun resolveActiveClip(clips: List<EditorTimelineClip>, timeMs: Long): EditorTimelineClip? {
  var cursor = 0L
  for (clip in clips) {
    val dur = clip.durationMs.coerceAtLeast(500L)
    if (timeMs < cursor + dur) return clip
    cursor += dur
  }
  return clips.lastOrNull()
}

private fun composeEffectFilter(effect: EditorVisualEffect): ColorFilter? {
  val matrix = ColorMatrix()
  return when (effect) {
    EditorVisualEffect.None, EditorVisualEffect.CinematicVignette -> null
    EditorVisualEffect.WarmGlow -> {
      matrix.setToScale(1.08f, 1.02f, 0.90f, 1f)
      ColorFilter.colorMatrix(matrix)
    }
    EditorVisualEffect.HighContrast -> {
      val c = 1.28f
      val t = (-0.5f * c + 0.5f) * 255f
      matrix.set(
        ColorMatrix(
          floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f,
          ),
        ),
      )
      ColorFilter.colorMatrix(matrix)
    }
    EditorVisualEffect.SoftMono -> {
      matrix.setToSaturation(0.18f)
      ColorFilter.colorMatrix(matrix)
    }
  }
}
