package com.example.ui.screens.factory

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.source.MediaFileHelper
import com.example.model.MediaType
import com.example.model.StudioAsset
import com.example.model.StudioAssetCategory
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.VideoFactoryUiState
import com.example.viewmodel.VoiceVisualUiState
import com.example.viewmodel.VoiceVisualViewModel

@Composable
fun AssetLibrarySection(
  factoryUiState: VideoFactoryUiState,
  voiceUiState: VoiceVisualUiState,
  voiceViewModel: VoiceVisualViewModel,
  onOpenVideoEditor: () -> Unit = {},
  glowEnabled: Boolean,
) {
  val project = factoryUiState.activeProject
  var previewAsset by remember { mutableStateOf<StudioAsset?>(null) }
  var assignAsset by remember { mutableStateOf<StudioAsset?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = stringResource(R.string.asset_library_title),
      subtitle = stringResource(R.string.asset_library_subtitle),
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuCyan, ZuzuPurple),
      modifier = Modifier.fillMaxWidth(),
    ) {
      OutlinedTextField(
        value = voiceUiState.assetSearchQuery,
        onValueChange = voiceViewModel::updateAssetSearchQuery,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        label = { Text("Search assets by title or provider") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("asset_library_search"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuPillChip(
          label = "All (${voiceUiState.allAssets.size})",
          selected = voiceUiState.assetCategoryFilter == null,
          onClick = { voiceViewModel.updateAssetCategoryFilter(null) },
          gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
        )
        StudioAssetCategory.entries.forEach { category ->
          val count = voiceUiState.allAssets.count { it.category == category }
          ZuzuPillChip(
            label = "${category.displayLabel} ($count)",
            selected = voiceUiState.assetCategoryFilter == category,
            onClick = { voiceViewModel.updateAssetCategoryFilter(category) },
            gradientColors = listOf(ZuzuViolet, ZuzuPink),
          )
        }
      }
    }

    val assets = voiceUiState.filteredAssets
    if (assets.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "Asset Library Is Empty",
        description = "Import audio or visual assets in Voice Studio and AI Visual Studio, or configure providers to generate assets.",
        icon = Icons.Rounded.PermMedia,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("asset_library_empty"),
      )
    } else {
      assets.forEach { asset ->
        val assignedSceneNum = project?.scenes?.firstOrNull { it.id == asset.assignedSceneId }?.sceneNumber
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuElectricBlue, ZuzuPink),
          onClick = { previewAsset = asset },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              ZuzuGradientIconBadge(
                icon = when (asset.mediaType) {
                  MediaType.Audio -> Icons.Rounded.AudioFile
                  MediaType.Video -> Icons.Rounded.VideoLibrary
                  else -> Icons.Rounded.Image
                },
                contentDescription = asset.mediaType.displayLabel,
                gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
                size = 40.dp,
                iconSize = 20.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(asset.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
                Text(
                  text = "${asset.category.displayLabel} • ${asset.providerLabel}",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
            }
            Row {
              IconButton(onClick = { previewAsset = asset }) {
                Icon(Icons.Rounded.Visibility, contentDescription = "Preview asset", tint = ZuzuElectricBlue)
              }
              IconButton(onClick = { voiceViewModel.deleteAsset(asset.id, project) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete asset", tint = ZuzuError)
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (assignedSceneNum != null) {
              StatusPill(label = "Scene $assignedSceneNum", tint = ZuzuGreen)
            }
            if (asset.savedToMediaLibrary) {
              StatusPill(label = "In Media Library", tint = ZuzuCyan)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ZuzuSecondaryActionButton(
              text = "Assign Scene",
              onClick = { assignAsset = asset },
              enabled = project != null && project.scenes.isNotEmpty(),
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.AssignmentTurnedIn,
              modifier = Modifier.weight(1f),
            )
            ZuzuSecondaryActionButton(
              text = if (asset.savedToMediaLibrary) "Saved" else "To Media Library",
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
      text = "Next: Open Video Editor",
      onClick = onOpenVideoEditor,
      accentColor = ZuzuPurple,
      icon = Icons.Rounded.ChevronRight,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("vf_next_video_editor_button"),
    )
  }

  previewAsset?.let { asset ->
    AssetPreviewDialog(asset = asset, onDismiss = { previewAsset = null })
  }

  assignAsset?.let { asset ->
    if (project != null) {
      AlertDialog(
        onDismissRequest = { assignAsset = null },
        containerColor = ZuzuWhite,
        title = { Text("Assign '${asset.title}' to Scene", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
        text = {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            project.scenes.forEach { scene ->
              ZuzuSecondaryActionButton(
                text = "Scene ${scene.sceneNumber}: ${scene.sceneTitle}",
                onClick = {
                  if (asset.mediaType == MediaType.Audio) {
                    voiceViewModel.assignVoiceAssetToScene(project, scene.id, asset.id)
                  } else {
                    voiceViewModel.assignVisualAssetToScene(project, scene.id, asset.id)
                  }
                  assignAsset = null
                },
                accentColor = ZuzuPurple,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { assignAsset = null }) {
            Text(stringResource(R.string.dialog_close), color = ZuzuPurple, fontWeight = FontWeight.Bold)
          }
        },
      )
    }
  }
}

@Composable
private fun AssetPreviewDialog(
  asset: StudioAsset,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var bitmap by remember(asset.id) { mutableStateOf<Bitmap?>(null) }
  var playing by remember { mutableStateOf(false) }
  val playerHolder = remember(asset.id) { mutableStateOf<MediaPlayer?>(null) }

  LaunchedEffect(asset.id) {
    val uriStr = asset.localUriString
    if (uriStr != null && (asset.mediaType == MediaType.Image || asset.mediaType == MediaType.Video)) {
      bitmap = MediaFileHelper.loadPreviewBitmap(context, uriStr, asset.mediaType)
    }
  }

  DisposableEffect(asset.id) {
    onDispose {
      runCatching { playerHolder.value?.release() }
      playerHolder.value = null
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text(asset.title, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (bitmap != null) {
          Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = asset.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
              .clip(RoundedCornerShape(14.dp)),
          )
        }
        if (asset.mediaType == MediaType.Audio && asset.localUriString != null) {
          ZuzuSecondaryActionButton(
            text = if (playing) "Pause Audio" else "Play Audio",
            onClick = {
              val current = playerHolder.value
              if (current == null) {
                runCatching {
                  MediaPlayer.create(context, Uri.parse(asset.localUriString))?.apply {
                    setOnCompletionListener { playing = false }
                    start()
                    playerHolder.value = this
                    playing = true
                  }
                }
              } else if (current.isPlaying) {
                current.pause()
                playing = false
              } else {
                current.start()
                playing = true
              }
            },
            accentColor = ZuzuElectricBlue,
            icon = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Text("Category: ${asset.category.displayLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        Text("Provider: ${asset.providerLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        asset.fileName?.let {
          Text("File: $it (${MediaFileHelper.formatFileSize(asset.fileSizeBytes)})", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        }
        if (asset.promptUsed.isNotBlank()) {
          Text("Prompt: ${asset.promptUsed}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.dialog_close), color = ZuzuPurple, fontWeight = FontWeight.Bold)
      }
    },
  )
}
