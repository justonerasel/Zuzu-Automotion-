package com.example.ui.screens.sources

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.source.ExternalUrlValidator
import com.example.data.source.MediaFileHelper
import com.example.model.ContentOrigin
import com.example.model.ExternalPlatformCategory
import com.example.model.MediaType
import com.example.model.PermissionStatus
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
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
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.viewmodel.ContentSourceUiState
import com.example.viewmodel.ContentSourceViewModel

@Composable
fun MyUploadsSection(
  uiState: ContentSourceUiState,
  viewModel: ContentSourceViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  val pending = uiState.pendingUpload

  var titleInput by remember(pending?.uriString) {
    mutableStateOf(pending?.fileName?.substringBeforeLast('.') ?: "")
  }
  var descriptionInput by remember(pending?.uriString) { mutableStateOf("") }
  var notesInput by remember(pending?.uriString) { mutableStateOf("") }
  var selectedPermission by remember(pending?.uriString) {
    mutableStateOf(PermissionStatus.OriginalContent)
  }

  val documentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { viewModel.setPendingUpload(it) }
        .onFailure { viewModel.setPickerError(it.message ?: "Unsupported file selected.") }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "My Uploads",
      subtitle = "Select device videos, images, or audio using Android's secure document picker",
      gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuViolet, ZuzuElectricBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = stringResource(R.string.sources_supported_formats),
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.sources_local_storage_note),
        style = MaterialTheme.typography.labelSmall,
        color = ZuzuElectricBlue,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(modifier = Modifier.height(14.dp))
      ZuzuGradientButton(
        text = stringResource(R.string.sources_pick_file_cta),
        onClick = { documentPickerLauncher.launch(MediaFileHelper.supportedMimeTypes) },
        gradientColors = ZuzuBlueTurquoiseGradient,
        icon = Icons.Rounded.UploadFile,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pick_media_file_button"),
      )
    }

    if (pending != null) {
      var previewBitmap by remember(pending.uriString) { mutableStateOf<Bitmap?>(null) }
      LaunchedEffect(pending.uriString) {
        previewBitmap = MediaFileHelper.loadPreviewBitmap(
          context = context,
          uriString = pending.uriString,
          mediaType = pending.mediaType,
        )
      }

      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = listOf(ZuzuCyan, ZuzuTurquoise),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pending_upload_card"),
      ) {
        Text(
          text = "Selected File Preview",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (previewBitmap != null) {
          Image(
            bitmap = previewBitmap!!.asImageBitmap(),
            contentDescription = pending.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(170.dp)
              .clip(RoundedCornerShape(16.dp)),
          )
          Spacer(modifier = Modifier.height(10.dp))
        } else {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = ZuzuSoftLavender,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ZuzuGradientIconBadge(
                icon = mediaTypeIcon(pending.mediaType),
                contentDescription = pending.mediaType.displayLabel,
                gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = pending.fileName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = ZuzuInkPrimary,
                )
                val duration = MediaFileHelper.formatDuration(pending.durationMs)
                Text(
                  text = listOfNotNull(
                    pending.mediaType.displayLabel,
                    pending.mimeType,
                    MediaFileHelper.formatFileSize(pending.fileSizeBytes),
                    duration,
                  ).joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        if (previewBitmap != null) {
          Text(
            text = "${pending.fileName} • ${pending.mediaType.displayLabel} • ${MediaFileHelper.formatFileSize(pending.fileSizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
          value = titleInput,
          onValueChange = { titleInput = it },
          label = { Text("Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = descriptionInput,
          onValueChange = { descriptionInput = it },
          label = { Text("Description (Optional)") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Rights / Permission Status",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PermissionStatus.entries.forEach { status ->
            ZuzuPillChip(
              label = status.displayLabel,
              selected = selectedPermission == status,
              onClick = { selectedPermission = status },
              gradientColors = listOf(ZuzuViolet, ZuzuMagenta),
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = notesInput,
          onValueChange = { notesInput = it },
          label = { Text("Notes (Optional)") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ZuzuSecondaryActionButton(
            text = "Cancel",
            onClick = viewModel::cancelPendingUpload,
            accentColor = ZuzuInkSecondary,
            modifier = Modifier
              .weight(1f)
              .testTag("cancel_upload_button"),
          )
          ZuzuGradientButton(
            text = "Save Locally",
            onClick = {
              viewModel.savePendingUpload(
                title = titleInput,
                description = descriptionInput,
                permissionStatus = selectedPermission,
                notes = notesInput,
              )
            },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
            modifier = Modifier
              .weight(1f)
              .testTag("save_upload_button"),
          )
        }
      }
    }

    val localUploads = uiState.allMediaEntries.filter { it.origin == ContentOrigin.MyUploads }
    SectionHeader(
      title = "Saved Local Uploads (${localUploads.size})",
      gradientColors = listOf(ZuzuPurple, ZuzuPink),
    )

    if (localUploads.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Local Uploads Yet",
        description = "Tap '${stringResource(R.string.sources_pick_file_cta)}' to add media metadata from your device.",
        icon = Icons.Rounded.UploadFile,
        gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
        glowEnabled = glowEnabled,
      )
    } else {
      localUploads.forEach { entry ->
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuViolet, ZuzuCyan),
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
                gradientColors = listOf(ZuzuViolet, ZuzuElectricBlue),
                size = 40.dp,
                iconSize = 20.dp,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = entry.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = ZuzuInkPrimary,
                )
                Text(
                  text = "${entry.mediaType.displayLabel} • ${MediaFileHelper.formatFileSize(entry.fileSizeBytes)} • Saved on device",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
            }
            IconButton(
              onClick = { viewModel.removeMediaEntry(entry.id) },
              modifier = Modifier.testTag("remove_upload_${entry.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Remove media",
                tint = ZuzuError,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun VideoUrlImportSection(
  uiState: ContentSourceUiState,
  viewModel: ContentSourceViewModel,
  glowEnabled: Boolean,
) {
  val urlError by viewModel.urlDraftError.collectAsStateWithLifecycle()

  var urlInput by remember { mutableStateOf("") }
  var selectedPlatform by remember { mutableStateOf(ExternalPlatformCategory.YouTube) }
  var titleInput by remember { mutableStateOf("") }
  var descriptionInput by remember { mutableStateOf("") }
  var permissionStatus by remember { mutableStateOf(PermissionStatus.PermissionPending) }
  var notesInput by remember { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Video URL Import",
      subtitle = "Save authorized external video or page references without unauthorized downloading",
      gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
    )

    ZuzuAlertBanner(
      message = stringResource(R.string.sources_url_compliance_note),
      tint = ZuzuViolet,
      icon = Icons.Rounded.Policy,
    )

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuElectricBlue, ZuzuCyan),
      modifier = Modifier.fillMaxWidth(),
    ) {
      OutlinedTextField(
        value = urlInput,
        onValueChange = { next ->
          urlInput = next
          viewModel.validateUrlDraft(next)?.let { detected ->
            selectedPlatform = detected
          }
        },
        label = { Text("Video or Content URL") },
        placeholder = { Text("https://youtube.com/watch?v=...") },
        isError = urlError != null,
        supportingText = if (urlError != null) {
          { Text(urlError.orEmpty()) }
        } else {
          null
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("url_import_input"),
      )

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Source Platform",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ExternalPlatformCategory.entries.forEach { category ->
          ZuzuPillChip(
            label = category.displayLabel,
            selected = selectedPlatform == category,
            onClick = { selectedPlatform = category },
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = titleInput,
        onValueChange = { titleInput = it },
        label = { Text("Content Title") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("url_title_input"),
      )

      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = descriptionInput,
        onValueChange = { descriptionInput = it },
        label = { Text("Description (Optional)") },
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Permission Status",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PermissionStatus.entries.forEach { status ->
          ZuzuPillChip(
            label = status.displayLabel,
            selected = permissionStatus == status,
            onClick = { permissionStatus = status },
            gradientColors = listOf(ZuzuViolet, ZuzuPink),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = notesInput,
        onValueChange = { notesInput = it },
        label = { Text("Notes (Optional)") },
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(14.dp))
      ZuzuGradientButton(
        text = "Save URL Metadata",
        onClick = {
          val validationError = ExternalUrlValidator.validate(urlInput)
          if (validationError != null) {
            viewModel.validateUrlDraft(urlInput)
          }
          viewModel.importExternalUrl(
            url = urlInput,
            platform = selectedPlatform,
            title = titleInput,
            description = descriptionInput,
            permissionStatus = permissionStatus,
            notes = notesInput,
          )
          if (validationError == null && titleInput.isNotBlank()) {
            urlInput = ""
            titleInput = ""
            descriptionInput = ""
            notesInput = ""
          }
        },
        gradientColors = ZuzuHeroGradient,
        icon = Icons.Rounded.Link,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("url_save_button"),
      )
    }

    val savedUrls = uiState.allMediaEntries.filter { it.mediaType == MediaType.ExternalUrl }
    SectionHeader(
      title = "Saved URLs (${savedUrls.size})",
      gradientColors = listOf(ZuzuCyan, ZuzuPurple),
    )

    if (savedUrls.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No External URLs Saved",
        description = "Import authorized YouTube, TikTok, Facebook, or web metadata above to catalog external sources.",
        icon = Icons.Rounded.Link,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      savedUrls.forEach { entry ->
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              ZuzuGradientIconBadge(
                icon = Icons.Rounded.Link,
                contentDescription = entry.origin.displayLabel,
                gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
                size = 38.dp,
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
                  text = "${entry.origin.displayLabel} • ${entry.externalUrl.orEmpty()}",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
            }
            IconButton(onClick = { viewModel.removeMediaEntry(entry.id) }) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Remove URL",
                tint = ZuzuError,
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          StatusPill(
            label = entry.permission.status.displayLabel,
            tint = if (entry.isEligibleForProduction) ZuzuGreen else ZuzuAmber,
          )
        }
      }
    }
  }
}

internal fun mediaTypeIcon(type: MediaType) = when (type) {
  MediaType.Video -> Icons.Rounded.VideoFile
  MediaType.Image -> Icons.Rounded.Image
  MediaType.Audio -> Icons.Rounded.AudioFile
  MediaType.ExternalUrl -> Icons.Rounded.Link
}
