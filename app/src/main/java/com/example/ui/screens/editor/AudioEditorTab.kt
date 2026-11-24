package com.example.ui.screens.editor

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.source.MediaFileHelper
import com.example.model.EditorAudioTrack
import com.example.model.EditorAudioTrackType
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.StudioAsset
import com.example.model.VideoEditorDraft
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuCyan
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
import kotlin.math.roundToInt

@Composable
fun AudioEditorTab(
  draft: VideoEditorDraft,
  studioAssets: List<StudioAsset>,
  mediaLibraryEntries: List<MediaLibraryEntry>,
  editorViewModel: VideoEditorViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  var pendingPickerType by remember { mutableStateOf(EditorAudioTrackType.Voiceover) }
  var showLibraryDialogFor by remember { mutableStateOf<EditorAudioTrackType?>(null) }
  var trimTargetTrack by remember { mutableStateOf<EditorAudioTrack?>(null) }

  var playingTrackId by remember { mutableStateOf<String?>(null) }
  val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      runCatching { mediaPlayer.value?.release() }
      mediaPlayer.value = null
    }
  }

  val audioFilePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { upload ->
          if (upload.mediaType != MediaType.Audio) {
            editorViewModel.setError("Please select an MP3 or WAV audio file.")
          } else {
            editorViewModel.addAudioTrack(
              trackType = pendingPickerType,
              title = upload.fileName.substringBeforeLast('.'),
              uriString = upload.uriString,
              durationMs = upload.durationMs,
            )
          }
        }
        .onFailure { err ->
          editorViewModel.setError(err.message ?: "Unable to read audio file.")
        }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = "Audio Editor (${draft.audioTracks.size} tracks)",
      subtitle = "Manage voiceovers, background music, volume, trimming & muting",
      gradientColors = ZuzuVioletPinkGradient,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuPink, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Add Voiceover",
          onClick = {
            pendingPickerType = EditorAudioTrackType.Voiceover
            audioFilePicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/x-wav"))
          },
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.RecordVoiceOver,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_add_voiceover_btn"),
        )
        ZuzuSecondaryActionButton(
          text = "Add Music",
          onClick = {
            pendingPickerType = EditorAudioTrackType.BackgroundMusic
            audioFilePicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/x-wav"))
          },
          accentColor = ZuzuPink,
          icon = Icons.Rounded.MusicNote,
          modifier = Modifier
            .weight(1f)
            .testTag("editor_add_music_btn"),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      ZuzuSecondaryActionButton(
        text = "Pick from Voice Studio / Media Library",
        onClick = { showLibraryDialogFor = EditorAudioTrackType.Voiceover },
        accentColor = ZuzuElectricBlue,
        icon = Icons.Rounded.Collections,
        modifier = Modifier.fillMaxWidth(),
      )
    }

    if (draft.audioTracks.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Audio Tracks Added Yet",
        description = "Import voiceover or background music (MP3/WAV) to mix into your MP4 export.",
        icon = Icons.Rounded.AudioFile,
        gradientColors = listOf(ZuzuPink, ZuzuMagenta, ZuzuPurple),
        glowEnabled = glowEnabled,
      )
    } else {
      draft.audioTracks.forEach { track ->
        val isPlaying = playingTrackId == track.id
        val trackGradient = if (track.trackType == EditorAudioTrackType.Voiceover) {
          listOf(ZuzuViolet, ZuzuElectricBlue)
        } else {
          listOf(ZuzuPink, ZuzuMagenta)
        }
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = trackGradient,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              ZuzuGradientIconBadge(
                icon = if (track.trackType == EditorAudioTrackType.Voiceover) {
                  Icons.Rounded.RecordVoiceOver
                } else {
                  Icons.Rounded.MusicNote
                },
                contentDescription = track.trackType.displayLabel,
                gradientColors = trackGradient,
                size = 40.dp,
                iconSize = 20.dp,
                cornerRadius = 12.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  StatusPill(
                    label = track.trackType.displayLabel,
                    tint = if (track.trackType == EditorAudioTrackType.Voiceover) ZuzuPurple else ZuzuPink,
                  )
                  if (track.isMuted) {
                    StatusPill(label = "Muted", tint = ZuzuError)
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = track.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = ZuzuInkPrimary,
                )
                val trimInfo = buildString {
                  append("Trim: ${track.trimStartMs / 1000L}s – ")
                  append(if (track.trimEndMs > 0L) "${track.trimEndMs / 1000L}s" else "End")
                }
                Text(trimInfo, style = MaterialTheme.typography.labelSmall, color = ZuzuInkSecondary)
              }
            }
            Row {
              IconButton(
                onClick = {
                  if (isPlaying) {
                    runCatching { mediaPlayer.value?.stop() }
                    runCatching { mediaPlayer.value?.release() }
                    mediaPlayer.value = null
                    playingTrackId = null
                  } else {
                    runCatching {
                      mediaPlayer.value?.release()
                      val mp = MediaPlayer.create(context, Uri.parse(track.uriString))
                      if (mp != null) {
                        val effectiveVol = if (track.isMuted) 0f else track.volume
                        mp.setVolume(effectiveVol, effectiveVol)
                        if (track.trimStartMs > 0L) {
                          mp.seekTo(track.trimStartMs.toInt())
                        }
                        mp.setOnCompletionListener { playingTrackId = null }
                        mp.start()
                        mediaPlayer.value = mp
                        playingTrackId = track.id
                      } else {
                        editorViewModel.setError("Unable to play '${track.title}'.")
                      }
                    }.onFailure {
                      editorViewModel.setError("Audio preview failed.")
                    }
                  }
                },
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                  tint = ZuzuElectricBlue,
                )
              }
              IconButton(onClick = {
                editorViewModel.toggleAudioMute(track.id)
                if (isPlaying) {
                  val nextVol = if (!track.isMuted) 0f else track.volume
                  runCatching { mediaPlayer.value?.setVolume(nextVol, nextVol) }
                }
              }) {
                Icon(
                  imageVector = if (track.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                  contentDescription = "Toggle mute",
                  tint = if (track.isMuted) ZuzuError else ZuzuPurple,
                )
              }
              IconButton(onClick = { trimTargetTrack = track }) {
                Icon(Icons.Rounded.ContentCut, contentDescription = "Trim audio", tint = ZuzuCyan)
              }
              IconButton(onClick = {
                if (isPlaying) {
                  runCatching { mediaPlayer.value?.release() }
                  mediaPlayer.value = null
                  playingTrackId = null
                }
                editorViewModel.removeAudioTrack(track.id)
              }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove track", tint = ZuzuError)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Volume: ${(track.volume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Slider(
            value = track.volume,
            onValueChange = { v ->
              editorViewModel.updateAudioVolume(track.id, v)
              if (isPlaying && !track.isMuted) {
                runCatching { mediaPlayer.value?.setVolume(v, v) }
              }
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
              thumbColor = ZuzuMagenta,
              activeTrackColor = ZuzuPurple,
              inactiveTrackColor = ZuzuPurpleSoft,
            ),
          )
        }
      }
    }
  }

  trimTargetTrack?.let { track ->
    var startSec by remember { mutableStateOf((track.trimStartMs / 1000L).toString()) }
    var endSec by remember { mutableStateOf((track.trimEndMs / 1000L).toString()) }
    AlertDialog(
      onDismissRequest = { trimTargetTrack = null },
      containerColor = ZuzuWhite,
      title = {
        Text("Trim Audio: ${track.title}", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Set 0 for End Time to keep playing through the end of the track.",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          OutlinedTextField(
            value = startSec,
            onValueChange = { startSec = it.filter { c -> c.isDigit() } },
            label = { Text("Trim Start (seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = endSec,
            onValueChange = { endSec = it.filter { c -> c.isDigit() } },
            label = { Text("Trim End (seconds, 0 = natural end)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Apply Trim",
          onClick = {
            val s = (startSec.toLongOrNull() ?: 0L) * 1000L
            val e = (endSec.toLongOrNull() ?: 0L) * 1000L
            editorViewModel.updateAudioTrim(track.id, s, e)
            trimTargetTrack = null
          },
          gradientColors = ZuzuVioletPinkGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { trimTargetTrack = null }) { Text("Cancel") }
      },
    )
  }

  showLibraryDialogFor?.let { initialType ->
    var pickAsType by remember { mutableStateOf(initialType) }
    val audioStudio = studioAssets.filter { it.mediaType == MediaType.Audio && !it.localUriString.isNullOrBlank() }
    val audioLibrary = mediaLibraryEntries.filter { it.mediaType == MediaType.Audio && !it.localUriString.isNullOrBlank() }

    AlertDialog(
      onDismissRequest = { showLibraryDialogFor = null },
      containerColor = ZuzuWhite,
      title = {
        Text("Add Audio from Library", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorAudioTrackType.entries.forEach { t ->
              ZuzuPillChip(
                label = t.displayLabel,
                selected = pickAsType == t,
                onClick = { pickAsType = t },
                gradientColors = ZuzuVioletPinkGradient,
                modifier = Modifier.weight(1f),
              )
            }
          }
          if (audioStudio.isEmpty() && audioLibrary.isEmpty()) {
            Text(
              text = "No local audio files in Asset Library or Media Library yet.",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          }
          audioStudio.forEach { asset ->
            ZuzuSecondaryActionButton(
              text = asset.title,
              onClick = {
                editorViewModel.addAudioTrack(
                  trackType = pickAsType,
                  title = asset.title,
                  uriString = asset.localUriString!!,
                  durationMs = asset.durationMs,
                )
                showLibraryDialogFor = null
              },
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.AudioFile,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          audioLibrary.forEach { entry ->
            ZuzuSecondaryActionButton(
              text = "${entry.title} (Library)",
              onClick = {
                editorViewModel.addAudioTrack(
                  trackType = pickAsType,
                  title = entry.title,
                  uriString = entry.localUriString!!,
                  durationMs = entry.durationMs,
                )
                showLibraryDialogFor = null
              },
              accentColor = ZuzuTurquoise,
              icon = Icons.Rounded.AudioFile,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLibraryDialogFor = null }) {
          Text("Close", color = ZuzuPurple, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}
