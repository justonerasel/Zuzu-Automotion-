package com.example.ui.screens.factory

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.MediaProviderStatus
import com.example.data.source.MediaFileHelper
import com.example.model.StoryboardScene
import com.example.model.StudioAsset
import com.example.model.VideoFactorySection
import com.example.model.VideoLanguage
import com.example.model.VoiceGender
import com.example.model.VoiceProviderType
import com.example.model.VoiceStudioTone
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
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
import com.example.viewmodel.VoiceVisualUiState
import com.example.viewmodel.VoiceVisualViewModel

@Composable
fun VoiceStudioSection(
  factoryUiState: VideoFactoryUiState,
  voiceUiState: VoiceVisualUiState,
  factoryViewModel: VideoFactoryViewModel,
  voiceViewModel: VoiceVisualViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  val project = factoryUiState.activeProject
  val settings = voiceUiState.voiceSettings

  var targetScene by remember(project?.id) { mutableStateOf<StoryboardScene?>(null) }
  var narrationInput by remember(project?.id, targetScene?.id) {
    mutableStateOf(
      targetScene?.narration
        ?: project?.script?.toCombinedText()
        ?: "",
    )
  }
  var assignDialogAsset by remember { mutableStateOf<StudioAsset?>(null) }

  var playingAssetId by remember { mutableStateOf<String?>(null) }
  val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      runCatching { mediaPlayer.value?.release() }
      mediaPlayer.value = null
    }
  }

  val audioPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { upload ->
          voiceViewModel.importAudioFile(
            upload = upload,
            project = project,
            targetSceneId = targetScene?.id,
          )
        }
        .onFailure { err ->
          voiceViewModel.setError(err.message ?: "Unable to read selected audio file.")
        }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = stringResource(R.string.voice_studio_title),
      subtitle = stringResource(R.string.voice_studio_subtitle),
      gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
    )

    DisclaimerNoticeCard(text = stringResource(R.string.voice_setup_note))

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuViolet, ZuzuCyan),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "TTS Provider",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        StatusPill(
          label = when (voiceUiState.voiceProviderStatus) {
            is MediaProviderStatus.Configured -> "Configured"
            is MediaProviderStatus.SetupRequired -> "Setup Required"
          },
          tint = when (voiceUiState.voiceProviderStatus) {
            is MediaProviderStatus.Configured -> ZuzuGreen
            is MediaProviderStatus.SetupRequired -> ZuzuAmber
          },
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VoiceProviderType.entries.forEach { provider ->
          ZuzuPillChip(
            label = provider.displayName,
            selected = voiceUiState.selectedVoiceProvider == provider,
            onClick = { voiceViewModel.selectVoiceProvider(provider) },
            gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text("Voice Language", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoLanguage.entries.forEach { lang ->
          ZuzuPillChip(
            label = "${lang.displayLabel} (${lang.nativeLabel})",
            selected = settings.language == lang,
            onClick = { voiceViewModel.setVoiceLanguage(lang) },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text("Voice Gender Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VoiceGender.entries.forEach { gender ->
          ZuzuPillChip(
            label = gender.displayLabel,
            selected = settings.gender == gender,
            onClick = { voiceViewModel.setVoiceGender(gender) },
            gradientColors = ZuzuVioletPinkGradient,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text("Voice Options", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      if (voiceUiState.availableVoices.isEmpty()) {
        Text(
          text = "Multiple voice options load automatically once ${voiceUiState.selectedVoiceProvider.displayName} API setup is configured.",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      } else {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          voiceUiState.availableVoices.forEach { opt ->
            ZuzuPillChip(
              label = opt.displayName,
              selected = settings.selectedVoiceId == opt.id,
              onClick = { voiceViewModel.selectVoiceOption(opt.id) },
              gradientColors = listOf(ZuzuPurple, ZuzuMagenta),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text("Voice Tone", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VoiceStudioTone.entries.forEach { tone ->
          ZuzuPillChip(
            label = tone.displayLabel,
            selected = settings.tone == tone,
            onClick = { voiceViewModel.setVoiceTone(tone) },
            gradientColors = listOf(ZuzuPink, ZuzuMagenta),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Voice Speed: ${String.format(Locale.US, "%.2fx", settings.speed)}",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Slider(
        value = settings.speed,
        onValueChange = voiceViewModel::setVoiceSpeed,
        valueRange = 0.5f..2.0f,
      )

      val pitchSupported = voiceUiState.selectedVoiceProvider.supportsPitch
      Text(
        text = if (pitchSupported) {
          "Voice Pitch: ${String.format(Locale.US, "%.2fx", settings.pitch)}"
        } else {
          "Voice Pitch: Not supported by ${voiceUiState.selectedVoiceProvider.displayName}"
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (pitchSupported) ZuzuInkPrimary else ZuzuInkSecondary,
      )
      Slider(
        value = settings.pitch,
        onValueChange = voiceViewModel::setVoicePitch,
        valueRange = 0.5f..2.0f,
        enabled = pitchSupported,
      )
    }

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuPink, ZuzuPurple),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "Script-to-Voice & Scene Target",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      if (project != null && project.scenes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ZuzuPillChip(
            label = "Full Script",
            selected = targetScene == null,
            onClick = {
              targetScene = null
              narrationInput = project.script.toCombinedText()
            },
            gradientColors = listOf(ZuzuViolet, ZuzuPink),
          )
          project.scenes.forEach { scene ->
            ZuzuPillChip(
              label = "Scene ${scene.sceneNumber}",
              selected = targetScene?.id == scene.id,
              onClick = {
                targetScene = scene
                narrationInput = scene.narration
              },
              gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = narrationInput,
        onValueChange = { narrationInput = it },
        label = { Text("Narration Text (${settings.language.displayLabel})") },
        minLines = 3,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("voice_narration_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Preview Voice",
          onClick = { voiceViewModel.previewVoice(narrationInput.ifBlank { "Sample preview text" }) },
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.PlayArrow,
          modifier = Modifier
            .weight(1f)
            .testTag("voice_preview_button"),
        )
        ZuzuSecondaryActionButton(
          text = "Regenerate",
          onClick = {
            if (project != null) {
              voiceViewModel.generateVoiceFromScript(
                project = project,
                scriptText = narrationInput,
                targetSceneId = targetScene?.id,
                isRegenerate = true,
              )
            } else {
              voiceViewModel.setError("Create or select a video project first.")
            }
          },
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.Refresh,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Import Audio",
          onClick = {
            audioPicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/x-wav"))
          },
          accentColor = ZuzuTurquoise,
          icon = Icons.Rounded.UploadFile,
          modifier = Modifier
            .weight(1f)
            .testTag("voice_import_audio_button"),
        )
        ZuzuGradientButton(
          text = "Generate Voice",
          onClick = {
            if (project != null) {
              voiceViewModel.generateVoiceFromScript(
                project = project,
                scriptText = narrationInput,
                targetSceneId = targetScene?.id,
                isRegenerate = false,
              )
            } else {
              voiceViewModel.setError("Create or select a video project first.")
            }
          },
          gradientColors = ZuzuHeroGradient,
          icon = Icons.Rounded.AutoAwesome,
          modifier = Modifier
            .weight(1f)
            .testTag("voice_generate_button"),
        )
      }
    }

    SectionHeader(
      title = "Voiceovers & Audio (${voiceUiState.audioAssets.size})",
      gradientColors = listOf(ZuzuPink, ZuzuCyan),
    )

    if (voiceUiState.audioAssets.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Voiceovers Saved Yet",
        description = "Import an MP3/WAV audio file or configure a TTS provider to synthesize narration.",
        icon = Icons.Rounded.RecordVoiceOver,
        gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
        glowEnabled = glowEnabled,
      )
    } else {
      voiceUiState.audioAssets.forEach { asset ->
        val isPlaying = playingAssetId == asset.id
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuElectricBlue, ZuzuCyan),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              ZuzuGradientIconBadge(
                icon = Icons.Rounded.AudioFile,
                contentDescription = asset.title,
                gradientColors = ZuzuBlueTurquoiseGradient,
                size = 40.dp,
                iconSize = 20.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(asset.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
                Text(
                  text = listOfNotNull(
                    asset.providerLabel,
                    MediaFileHelper.formatDuration(asset.durationMs),
                    MediaFileHelper.formatFileSize(asset.fileSizeBytes),
                  ).joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
            }
            Row {
              IconButton(
                onClick = {
                  val uriStr = asset.localUriString
                  if (uriStr == null) {
                    voiceViewModel.setError("No local audio file URI available for playback.")
                  } else if (isPlaying) {
                    runCatching { mediaPlayer.value?.pause() }
                    playingAssetId = null
                  } else {
                    runCatching {
                      mediaPlayer.value?.release()
                      val player = MediaPlayer.create(context, Uri.parse(uriStr))
                      if (player != null) {
                        runCatching {
                          player.playbackParams = PlaybackParams().apply {
                            speed = settings.speed
                            if (voiceUiState.selectedVoiceProvider.supportsPitch) {
                              pitch = settings.pitch
                            }
                          }
                        }
                        player.setOnCompletionListener { playingAssetId = null }
                        player.start()
                        mediaPlayer.value = player
                        playingAssetId = asset.id
                      } else {
                        voiceViewModel.setError("Unable to play selected audio URI.")
                      }
                    }.onFailure {
                      voiceViewModel.setError("Playback failed for ${asset.title}.")
                    }
                  }
                },
                modifier = Modifier.testTag("voice_play_${asset.id}"),
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                  contentDescription = if (isPlaying) "Pause" else "Play",
                  tint = ZuzuElectricBlue,
                )
              }
              if (isPlaying) {
                IconButton(
                  onClick = {
                    runCatching { mediaPlayer.value?.stop() }
                    runCatching { mediaPlayer.value?.release() }
                    mediaPlayer.value = null
                    playingAssetId = null
                  },
                ) {
                  Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = ZuzuPink)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ZuzuSecondaryActionButton(
              text = "Assign to Scene",
              onClick = { assignDialogAsset = asset },
              enabled = project != null && project.scenes.isNotEmpty(),
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.AssignmentTurnedIn,
              modifier = Modifier.weight(1f),
            )
            ZuzuSecondaryActionButton(
              text = if (asset.savedToMediaLibrary) "In Library" else "Save to Library",
              onClick = { voiceViewModel.saveAssetToMediaLibrary(asset) },
              enabled = !asset.savedToMediaLibrary,
              accentColor = ZuzuTurquoise,
              icon = Icons.Rounded.LibraryAddCheck,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    ZuzuSecondaryActionButton(
      text = "Next: Open AI Visual Studio",
      onClick = { factoryViewModel.selectSection(VideoFactorySection.VisualStudio) },
      accentColor = ZuzuMagenta,
      icon = Icons.Rounded.ChevronRight,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vf_next_visual_studio_button"),
    )
  }

  assignDialogAsset?.let { asset ->
    if (project != null) {
      AlertDialog(
        onDismissRequest = { assignDialogAsset = null },
        containerColor = ZuzuWhite,
        title = { Text("Assign Voiceover to Scene", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            project.scenes.forEach { scene ->
              ZuzuSecondaryActionButton(
                text = "Scene ${scene.sceneNumber}: ${scene.sceneTitle}",
                onClick = {
                  voiceViewModel.assignVoiceAssetToScene(project, scene.id, asset.id)
                  assignDialogAsset = null
                },
                accentColor = ZuzuPurple,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { assignDialogAsset = null }) {
            Text("Close", color = ZuzuPurple, fontWeight = FontWeight.Bold)
          }
        },
      )
    }
  }
}
