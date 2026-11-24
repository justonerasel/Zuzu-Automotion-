package com.example.ui.screens.factory

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.source.MediaFileHelper
import com.example.model.ImageProviderType
import com.example.model.StoryboardScene
import com.example.model.StudioAsset
import com.example.model.VideoAspectRatio
import com.example.model.VideoClipProviderType
import com.example.model.VideoFactorySection
import com.example.model.VisualAssetStatus
import com.example.model.VisualStudioStyle
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
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
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VideoFactoryViewModel
import com.example.viewmodel.VoiceVisualUiState
import com.example.viewmodel.VoiceVisualViewModel

@Composable
fun VisualStudioSection(
  factoryUiState: VideoFactoryUiState,
  voiceUiState: VoiceVisualUiState,
  factoryViewModel: VideoFactoryViewModel,
  voiceViewModel: VoiceVisualViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  val project = factoryUiState.activeProject

  var pendingImportScene by remember { mutableStateOf<StoryboardScene?>(null) }

  val visualPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { upload ->
          voiceViewModel.importVisualAsset(
            upload = upload,
            project = project,
            scene = pendingImportScene,
          )
        }
        .onFailure { err ->
          voiceViewModel.setError(err.message ?: "Unable to read selected visual file.")
        }
    }
    pendingImportScene = null
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = stringResource(R.string.visual_studio_title),
      subtitle = stringResource(R.string.visual_studio_subtitle),
      gradientColors = ZuzuVioletPinkGradient,
    )

    DisclaimerNoticeCard(text = stringResource(R.string.visual_setup_note))

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuPink, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Image Provider", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
        StatusPill(label = "Setup Required", tint = ZuzuAmber)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ImageProviderType.entries.forEach { provider ->
          ZuzuPillChip(
            label = provider.displayName,
            selected = voiceUiState.selectedImageProvider == provider,
            onClick = { voiceViewModel.selectImageProvider(provider) },
            gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Video Clip Provider", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
        StatusPill(label = "Setup Required", tint = ZuzuAmber)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoClipProviderType.entries.forEach { provider ->
          ZuzuPillChip(
            label = provider.displayName,
            selected = voiceUiState.selectedVideoProvider == provider,
            onClick = { voiceViewModel.selectVideoProvider(provider) },
            gradientColors = ZuzuVioletPinkGradient,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text("Visual Style", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VisualStudioStyle.entries.forEach { style ->
          ZuzuPillChip(
            label = style.displayLabel,
            selected = voiceUiState.selectedVisualStyle == style,
            onClick = { voiceViewModel.selectVisualStyle(style) },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VideoAspectRatio.entries.forEach { ratio ->
          ZuzuPillChip(
            label = "${ratio.label} • ${ratio.description}",
            selected = voiceUiState.selectedAspectRatio == ratio,
            onClick = { voiceViewModel.selectAspectRatio(ratio) },
            gradientColors = listOf(ZuzuPurple, ZuzuMagenta),
          )
        }
      }
    }

    if (project == null || project.scenes.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = if (project == null) "No Active Video Project" else "No Storyboard Scenes Yet",
        description = if (project == null) {
          "Create a video project first so Visual Studio can generate or attach scene visuals."
        } else {
          "Add scenes in Storyboard Studio for '${project.topic}' so Visual Studio can generate or attach scene assets."
        },
        icon = Icons.Rounded.Palette,
        gradientColors = ZuzuVioletPinkGradient,
        glowEnabled = glowEnabled,
        ctaLabel = if (project == null) "Configure New Video" else "Open Storyboard Studio",
        onCtaClick = {
          factoryViewModel.selectSection(
            if (project == null) VideoFactorySection.Configure else VideoFactorySection.StoryboardStudio,
          )
        },
      )
    } else {
      SectionHeader(
        title = "Scene-Based Visual Production (${project.scenes.size} scenes)",
        gradientColors = ZuzuBlueTurquoiseGradient,
      )

      project.scenes.forEach { scene ->
        val assignedAsset: StudioAsset? = voiceUiState.allAssets.firstOrNull {
          it.id == scene.assignedVisualAssetId
        }
        val status: VisualAssetStatus = when {
          assignedAsset != null && assignedAsset.isAiGenerated -> VisualAssetStatus.Generated
          assignedAsset != null -> VisualAssetStatus.Imported
          else -> voiceUiState.sceneStatusOverrides[scene.id] ?: VisualAssetStatus.NotGenerated
        }

        SceneVisualCard(
          scene = scene,
          assignedAsset = assignedAsset,
          status = status,
          selectedStyle = voiceUiState.selectedVisualStyle,
          aspectRatio = project.aspectRatio,
          glowEnabled = glowEnabled,
          onSavePrompts = { imgPrompt, vidPrompt ->
            voiceViewModel.updateScenePrompts(project, scene.id, imgPrompt, vidPrompt)
          },
          onGenerateImage = { imgPrompt ->
            voiceViewModel.generateSceneImage(project, scene, imgPrompt)
          },
          onGenerateVideo = { vidPrompt ->
            voiceViewModel.generateSceneVideoClip(project, scene, vidPrompt)
          },
          onImportVisual = {
            pendingImportScene = scene
            visualPicker.launch(arrayOf("image/jpeg", "image/png", "video/mp4", "video/quicktime"))
          },
          onSaveToMediaLibrary = {
            if (assignedAsset != null) {
              voiceViewModel.saveAssetToMediaLibrary(assignedAsset)
            }
          },
        )
      }
    }

    ZuzuSecondaryActionButton(
      text = "Next: Open Asset Library",
      onClick = { factoryViewModel.selectSection(VideoFactorySection.AssetLibrary) },
      accentColor = ZuzuElectricBlue,
      icon = Icons.Rounded.ChevronRight,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vf_next_asset_library_button"),
    )
  }
}

@Composable
private fun SceneVisualCard(
  scene: StoryboardScene,
  assignedAsset: StudioAsset?,
  status: VisualAssetStatus,
  selectedStyle: VisualStudioStyle,
  aspectRatio: VideoAspectRatio,
  glowEnabled: Boolean,
  onSavePrompts: (String, String) -> Unit,
  onGenerateImage: (String) -> Unit,
  onGenerateVideo: (String) -> Unit,
  onImportVisual: () -> Unit,
  onSaveToMediaLibrary: () -> Unit,
) {
  val context = LocalContext.current
  var imagePrompt by remember(scene.id, scene.imageGenerationPrompt) {
    mutableStateOf(scene.imageGenerationPrompt)
  }
  var videoPrompt by remember(scene.id, scene.videoGenerationPrompt) {
    mutableStateOf(scene.videoGenerationPrompt)
  }
  var previewBitmap by remember(assignedAsset?.id, assignedAsset?.localUriString) {
    mutableStateOf<Bitmap?>(null)
  }

  LaunchedEffect(assignedAsset?.id, assignedAsset?.localUriString) {
    val uriStr = assignedAsset?.localUriString
    previewBitmap = if (uriStr != null) {
      MediaFileHelper.loadPreviewBitmap(context, uriStr, assignedAsset.mediaType)
    } else {
      null
    }
  }

  val statusTint = when (status) {
    VisualAssetStatus.Generated, VisualAssetStatus.Imported -> ZuzuGreen
    VisualAssetStatus.SetupRequired -> ZuzuAmber
    VisualAssetStatus.Error -> ZuzuError
    VisualAssetStatus.NotGenerated -> ZuzuInkSecondary
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuPink, ZuzuCyan),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Scene ${scene.sceneNumber}: ${scene.sceneTitle}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Text(
          text = "${selectedStyle.displayLabel} • ${aspectRatio.label}",
          style = MaterialTheme.typography.labelSmall,
          color = ZuzuPurple,
        )
      }
      StatusPill(label = status.displayLabel, tint = statusTint)
    }

    if (scene.visualDescription.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = scene.visualDescription,
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    if (previewBitmap != null) {
      Image(
        bitmap = previewBitmap!!.asImageBitmap(),
        contentDescription = "Scene ${scene.sceneNumber} preview",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(145.dp)
          .clip(RoundedCornerShape(14.dp)),
      )
      Spacer(modifier = Modifier.height(8.dp))
    } else if (assignedAsset != null) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = ZuzuSoftLavender,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Image, contentDescription = null, tint = ZuzuPurple)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${assignedAsset.title} (${assignedAsset.providerLabel})",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkPrimary,
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedTextField(
      value = imagePrompt,
      onValueChange = { imagePrompt = it },
      label = { Text("Image Prompt") },
      minLines = 2,
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = videoPrompt,
      onValueChange = { videoPrompt = it },
      label = { Text("Video Prompt") },
      minLines = 2,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ZuzuSecondaryActionButton(
        text = "Save Prompts",
        onClick = { onSavePrompts(imagePrompt, videoPrompt) },
        accentColor = ZuzuElectricBlue,
        icon = Icons.Rounded.Save,
        modifier = Modifier.weight(1f),
      )
      ZuzuSecondaryActionButton(
        text = "Import Visual",
        onClick = onImportVisual,
        accentColor = ZuzuTurquoise,
        icon = Icons.Rounded.UploadFile,
        modifier = Modifier
          .weight(1f)
          .testTag("visual_import_scene_${scene.sceneNumber}"),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ZuzuGradientButton(
        text = "Gen Image",
        onClick = { onGenerateImage(imagePrompt) },
        gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
        icon = Icons.Rounded.AutoAwesome,
        modifier = Modifier.weight(1f),
      )
      ZuzuGradientButton(
        text = "Gen Video",
        onClick = { onGenerateVideo(videoPrompt) },
        gradientColors = ZuzuVioletPinkGradient,
        icon = Icons.Rounded.VideoLibrary,
        modifier = Modifier.weight(1f),
      )
    }

    if (assignedAsset != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "Regenerate",
          onClick = { onGenerateImage(imagePrompt) },
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.Refresh,
          modifier = Modifier.weight(1f),
        )
        ZuzuSecondaryActionButton(
          text = if (assignedAsset.savedToMediaLibrary) "In Media Library" else "Save to Media Library",
          onClick = onSaveToMediaLibrary,
          enabled = !assignedAsset.savedToMediaLibrary,
          accentColor = ZuzuGreen,
          icon = Icons.Rounded.LibraryAddCheck,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
