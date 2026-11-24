package com.example.ui.screens.sources

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.source.MediaFileHelper
import com.example.model.ContentOrigin
import com.example.model.LibraryViewMode
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
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
import com.example.ui.theme.ZuzuCyanSoft
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ContentSourceUiState
import com.example.viewmodel.ContentSourceViewModel

@Composable
fun MediaLibrarySection(
  uiState: ContentSourceUiState,
  viewModel: ContentSourceViewModel,
  glowEnabled: Boolean,
) {
  val filter = uiState.filterState
  var previewEntry by remember { mutableStateOf<MediaLibraryEntry?>(null) }
  var editingEntry by remember { mutableStateOf<MediaLibraryEntry?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader(
      title = "Media Library",
      subtitle = "Search, filter, preview, and select approved sources for AI Studio workflows",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      OutlinedTextField(
        value = filter.searchQuery,
        onValueChange = viewModel::updateSearchQuery,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        label = { Text("Search media by name or source") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("library_search_input"),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ZuzuSecondaryActionButton(
          text = filter.sortOrder.displayLabel,
          onClick = viewModel::toggleSortOrder,
          accentColor = ZuzuElectricBlue,
          icon = Icons.AutoMirrored.Rounded.Sort,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = { viewModel.setViewMode(LibraryViewMode.Grid) },
            modifier = Modifier.testTag("library_grid_mode"),
          ) {
            Icon(
              imageVector = Icons.Rounded.GridView,
              contentDescription = "Grid view",
              tint = if (filter.viewMode == LibraryViewMode.Grid) ZuzuPurple else ZuzuInkSecondary,
            )
          }
          IconButton(
            onClick = { viewModel.setViewMode(LibraryViewMode.List) },
            modifier = Modifier.testTag("library_list_mode"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ViewList,
              contentDescription = "List view",
              tint = if (filter.viewMode == LibraryViewMode.List) ZuzuPurple else ZuzuInkSecondary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Media Type",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkSecondary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ZuzuPillChip(
          label = "All Types",
          selected = filter.mediaTypeFilter == null,
          onClick = { viewModel.updateMediaTypeFilter(null) },
          gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
        )
        MediaType.entries.forEach { type ->
          ZuzuPillChip(
            label = type.displayLabel,
            selected = filter.mediaTypeFilter == type,
            onClick = { viewModel.updateMediaTypeFilter(type) },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Source Origin",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkSecondary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ZuzuPillChip(
          label = "All Sources",
          selected = filter.originFilter == null,
          onClick = { viewModel.updateOriginFilter(null) },
          gradientColors = listOf(ZuzuViolet, ZuzuMagenta),
        )
        ContentOrigin.entries.forEach { origin ->
          ZuzuPillChip(
            label = origin.displayLabel,
            selected = filter.originFilter == origin,
            onClick = { viewModel.updateOriginFilter(origin) },
            gradientColors = listOf(ZuzuViolet, ZuzuMagenta),
          )
        }
      }
    }

    if (uiState.selectedProductionIds.isNotEmpty()) {
      StatusPill(
        label = "${uiState.selectedProductionIds.size} approved source(s) selected for future AI Studio",
        tint = ZuzuPurple,
      )
    }

    val entries = uiState.filteredMediaEntries
    if (entries.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = stringResource(R.string.sources_empty_library_title),
        description = stringResource(R.string.sources_empty_library_desc),
        icon = Icons.Rounded.PermMedia,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("media_library_empty"),
      )
    } else if (filter.viewMode == LibraryViewMode.Grid) {
      entries.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          pair.forEach { entry ->
            MediaLibraryGridCard(
              entry = entry,
              selectedForProduction = entry.id in uiState.selectedProductionIds,
              glowEnabled = glowEnabled,
              onPreview = { previewEntry = entry },
              onEdit = { editingEntry = entry },
              onRemove = { viewModel.removeMediaEntry(entry.id) },
              onToggleProduction = { viewModel.toggleProductionSelection(entry.id) },
              modifier = Modifier.weight(1f),
            )
          }
          if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    } else {
      entries.forEach { entry ->
        MediaLibraryListCard(
          entry = entry,
          selectedForProduction = entry.id in uiState.selectedProductionIds,
          glowEnabled = glowEnabled,
          onPreview = { previewEntry = entry },
          onEdit = { editingEntry = entry },
          onRemove = { viewModel.removeMediaEntry(entry.id) },
          onToggleProduction = { viewModel.toggleProductionSelection(entry.id) },
        )
      }
    }
  }

  previewEntry?.let { entry ->
    MediaPreviewDialog(
      entry = entry,
      onDismiss = { previewEntry = null },
    )
  }

  editingEntry?.let { entry ->
    EditMediaMetadataDialog(
      entry = entry,
      onDismiss = { editingEntry = null },
      onSave = { title, description, notes ->
        viewModel.updateMediaMetadata(entry.id, title, description, notes)
        editingEntry = null
      },
    )
  }
}

@Composable
private fun MediaLibraryGridCard(
  entry: MediaLibraryEntry,
  selectedForProduction: Boolean,
  glowEnabled: Boolean,
  onPreview: () -> Unit,
  onEdit: () -> Unit,
  onRemove: () -> Unit,
  onToggleProduction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = if (selectedForProduction) listOf(ZuzuViolet, ZuzuCyan) else listOf(ZuzuCyan, ZuzuTurquoise),
    onClick = onPreview,
    modifier = modifier,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .height(74.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Brush.linearGradient(listOf(ZuzuPurpleSoft, ZuzuCyanSoft))),
    ) {
      ZuzuGradientIconBadge(
        icon = mediaTypeIcon(entry.mediaType),
        contentDescription = entry.mediaType.displayLabel,
        gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
        size = 42.dp,
        iconSize = 22.dp,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = entry.title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = ZuzuInkPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "${entry.mediaType.displayLabel} • ${entry.origin.displayLabel}",
      style = MaterialTheme.typography.labelSmall,
      color = ZuzuInkSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(6.dp))
    StatusPill(
      label = if (entry.isEligibleForProduction) "Ready" else entry.permission.status.displayLabel,
      tint = if (entry.isEligibleForProduction) ZuzuGreen else ZuzuAmber,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(onClick = onEdit) {
        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = ZuzuElectricBlue, modifier = Modifier.size(18.dp))
      }
      IconButton(onClick = onToggleProduction) {
        Icon(
          Icons.Rounded.CheckCircle,
          contentDescription = "Select for production",
          tint = if (selectedForProduction) ZuzuGreen else ZuzuInkSecondary.copy(alpha = 0.45f),
          modifier = Modifier.size(18.dp),
        )
      }
      IconButton(onClick = onRemove) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = ZuzuError, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
private fun MediaLibraryListCard(
  entry: MediaLibraryEntry,
  selectedForProduction: Boolean,
  glowEnabled: Boolean,
  onPreview: () -> Unit,
  onEdit: () -> Unit,
  onRemove: () -> Unit,
  onToggleProduction: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuCyan, ZuzuPurple),
    onClick = onPreview,
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
          icon = mediaTypeIcon(entry.mediaType),
          contentDescription = entry.mediaType.displayLabel,
          gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
          size = 40.dp,
          iconSize = 20.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Text(
            text = "${entry.mediaType.displayLabel} • ${entry.origin.displayLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }
      Row {
        IconButton(onClick = onPreview) {
          Icon(Icons.Rounded.Visibility, contentDescription = "Preview", tint = ZuzuElectricBlue)
        }
        IconButton(onClick = onEdit) {
          Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = ZuzuPurple)
        }
        IconButton(onClick = onToggleProduction) {
          Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = "Select for production",
            tint = if (selectedForProduction) ZuzuGreen else ZuzuInkSecondary.copy(alpha = 0.45f),
          )
        }
        IconButton(onClick = onRemove) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = ZuzuError)
        }
      }
    }
  }
}

@Composable
private fun MediaPreviewDialog(
  entry: MediaLibraryEntry,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var previewBitmap by remember(entry.id) { mutableStateOf<Bitmap?>(null) }
  var isAudioPlaying by remember { mutableStateOf(false) }
  val mediaPlayer = remember(entry.id) { mutableStateOf<MediaPlayer?>(null) }

  LaunchedEffect(entry.id) {
    val uriStr = entry.localUriString
    if (uriStr != null && (entry.mediaType == MediaType.Image || entry.mediaType == MediaType.Video)) {
      previewBitmap = MediaFileHelper.loadPreviewBitmap(context, uriStr, entry.mediaType)
    }
  }

  DisposableEffect(entry.id) {
    onDispose {
      runCatching { mediaPlayer.value?.release() }
      mediaPlayer.value = null
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text(entry.title, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (previewBitmap != null) {
          Image(
            bitmap = previewBitmap!!.asImageBitmap(),
            contentDescription = entry.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
              .clip(RoundedCornerShape(14.dp)),
          )
        }
        if (entry.mediaType == MediaType.Audio && entry.localUriString != null) {
          ZuzuSecondaryActionButton(
            text = if (isAudioPlaying) "Pause Audio Preview" else "Play Audio Preview",
            onClick = {
              val player = mediaPlayer.value
              if (player == null) {
                runCatching {
                  MediaPlayer.create(context, Uri.parse(entry.localUriString))?.apply {
                    setOnCompletionListener { isAudioPlaying = false }
                    start()
                    mediaPlayer.value = this
                    isAudioPlaying = true
                  }
                }
              } else if (player.isPlaying) {
                player.pause()
                isAudioPlaying = false
              } else {
                player.start()
                isAudioPlaying = true
              }
            },
            accentColor = ZuzuElectricBlue,
            icon = if (isAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Text("Type: ${entry.mediaType.displayLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        Text("Source: ${entry.origin.displayLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        if (!entry.fileName.isNullOrBlank()) {
          Text("File: ${entry.fileName} (${MediaFileHelper.formatFileSize(entry.fileSizeBytes)})", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        }
        if (!entry.externalUrl.isNullOrBlank()) {
          Text("URL: ${entry.externalUrl}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        }
        Text("Permission: ${entry.permission.status.displayLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        Text(
          text = if (entry.isEligibleForProduction) {
            "Production Status: Approved for AI Studio workflows"
          } else {
            "Production Status: Permission review required before reuse"
          },
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = if (entry.isEligibleForProduction) ZuzuGreen else ZuzuAmber,
        )
        if (entry.description.isNotBlank()) {
          Text("Description: ${entry.description}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
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

@Composable
private fun EditMediaMetadataDialog(
  entry: MediaLibraryEntry,
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit,
) {
  var title by remember { mutableStateOf(entry.title) }
  var description by remember { mutableStateOf(entry.description) }
  var notes by remember { mutableStateOf(entry.notes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text("Edit Media Metadata", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title") },
          singleLine = true,
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes") },
        )
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Save",
        onClick = { onSave(title, description, notes) },
        enabled = title.isNotBlank(),
        gradientColors = ZuzuBlueTurquoiseGradient,
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", color = ZuzuInkSecondary) }
    },
  )
}
