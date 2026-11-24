package com.example.ui.screens.sources

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ContentOrigin
import com.example.model.CreatorSourceEntry
import com.example.model.ExternalPlatformCategory
import com.example.model.MediaLibraryEntry
import com.example.model.PermissionPolicy
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.toContentOrigin
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
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftRose
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ContentSourceUiState
import com.example.viewmodel.ContentSourceViewModel

@Composable
fun CreatorSourcesSection(
  uiState: ContentSourceUiState,
  viewModel: ContentSourceViewModel,
  glowEnabled: Boolean,
) {
  var showCreatorFormDialog by remember { mutableStateOf(false) }
  var editingCreator by remember { mutableStateOf<CreatorSourceEntry?>(null) }
  var viewingCreator by remember { mutableStateOf<CreatorSourceEntry?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Creator Sources",
      subtitle = "Catalog authorized creators and channel permissions",
      gradientColors = ZuzuVioletPinkGradient,
    )

    ZuzuAlertBanner(
      message = stringResource(R.string.sources_creator_compliance_note),
      tint = ZuzuMagenta,
      icon = Icons.Rounded.Policy,
    )

    ZuzuGradientButton(
      text = "Add Creator Source",
      onClick = {
        editingCreator = null
        showCreatorFormDialog = true
      },
      gradientColors = ZuzuVioletPinkGradient,
      icon = Icons.Rounded.Add,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("add_creator_source_button"),
    )

    if (uiState.creatorSources.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = stringResource(R.string.sources_empty_creators_title),
        description = stringResource(R.string.sources_empty_creators_desc),
        icon = Icons.Rounded.People,
        gradientColors = ZuzuVioletPinkGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      uiState.creatorSources.forEach { creator ->
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuPink, ZuzuMagenta),
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
                icon = Icons.Rounded.People,
                contentDescription = creator.creatorName,
                gradientColors = listOf(ZuzuViolet, ZuzuPink),
                size = 38.dp,
                iconSize = 20.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = creator.creatorName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = ZuzuInkPrimary,
                )
                Text(
                  text = "${creator.platform.displayLabel} • ${creator.contentCategory}",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
            }
            Row {
              IconButton(onClick = { viewingCreator = creator }) {
                Icon(
                  imageVector = Icons.Rounded.Visibility,
                  contentDescription = "View creator",
                  tint = ZuzuElectricBlue,
                )
              }
              IconButton(
                onClick = {
                  editingCreator = creator
                  showCreatorFormDialog = true
                },
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Edit creator",
                  tint = ZuzuPurple,
                )
              }
              IconButton(onClick = { viewModel.removeCreatorSource(creator.id) }) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "Remove creator",
                  tint = ZuzuError,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          StatusPill(
            label = creator.permission.status.displayLabel,
            tint = if (PermissionPolicy.isVerifiedStatus(creator.permission.status)) ZuzuGreen else ZuzuAmber,
          )
        }
      }
    }
  }

  if (showCreatorFormDialog) {
    CreatorFormDialog(
      initial = editingCreator,
      onDismiss = { showCreatorFormDialog = false },
      onSave = { name, platform, url, category, status, notes ->
        viewModel.saveCreatorSource(
          editingId = editingCreator?.id,
          creatorName = name,
          platform = platform,
          profileUrl = url,
          contentCategory = category,
          permissionStatus = status,
          notes = notes,
        )
        showCreatorFormDialog = false
      },
    )
  }

  viewingCreator?.let { creator ->
    AlertDialog(
      onDismissRequest = { viewingCreator = null },
      containerColor = ZuzuWhite,
      title = { Text(creator.creatorName, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Platform: ${creator.platform.displayLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
          Text("Category: ${creator.contentCategory}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
          Text("Profile URL: ${creator.profileUrl}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
          Text("Permission: ${creator.permission.status.displayLabel}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
          if (creator.notes.isNotBlank()) {
            Text("Notes: ${creator.notes}", style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { viewingCreator = null }) {
          Text(stringResource(R.string.dialog_close), color = ZuzuPurple, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}

@Composable
private fun CreatorFormDialog(
  initial: CreatorSourceEntry?,
  onDismiss: () -> Unit,
  onSave: (String, ExternalPlatformCategory, String, String, PermissionStatus, String) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.creatorName ?: "") }
  var platform by remember { mutableStateOf(initial?.platform ?: ExternalPlatformCategory.YouTube) }
  var profileUrl by remember { mutableStateOf(initial?.profileUrl ?: "") }
  var category by remember { mutableStateOf(initial?.contentCategory ?: "") }
  var permissionStatus by remember {
    mutableStateOf(initial?.permission?.status ?: PermissionStatus.PermissionPending)
  }
  var notes by remember { mutableStateOf(initial?.notes ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = if (initial == null) "Add Creator Source" else "Edit Creator Source",
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Creator Name") },
          singleLine = true,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ExternalPlatformCategory.entries.forEach { cat ->
            ZuzuPillChip(
              label = cat.displayLabel,
              selected = platform == cat,
              onClick = { platform = cat },
              gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
            )
          }
        }
        OutlinedTextField(
          value = profileUrl,
          onValueChange = { profileUrl = it },
          label = { Text("Channel or Profile URL") },
          singleLine = true,
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Content Category") },
          singleLine = true,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PermissionStatus.entries.forEach { st ->
            ZuzuPillChip(
              label = st.displayLabel,
              selected = permissionStatus == st,
              onClick = { permissionStatus = st },
              gradientColors = listOf(ZuzuViolet, ZuzuPink),
            )
          }
        }
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
        onClick = { onSave(name, platform, profileUrl, category, permissionStatus, notes) },
        enabled = name.isNotBlank() && profileUrl.isNotBlank() && category.isNotBlank(),
        gradientColors = ZuzuVioletPinkGradient,
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", color = ZuzuInkSecondary) }
    },
  )
}

@Composable
fun PermissionManagerSection(
  uiState: ContentSourceUiState,
  viewModel: ContentSourceViewModel,
  glowEnabled: Boolean,
) {
  var editingMedia by remember { mutableStateOf<MediaLibraryEntry?>(null) }
  var editingCreator by remember { mutableStateOf<CreatorSourceEntry?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Copyright & Permission Manager",
      subtitle = "Verify content ownership and license records before approving AI production reuse",
      gradientColors = listOf(ZuzuMagenta, ZuzuPink),
    )

    if (uiState.allMediaEntries.isEmpty() && uiState.creatorSources.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = stringResource(R.string.sources_empty_permissions_title),
        description = stringResource(R.string.sources_empty_permissions_desc),
        icon = Icons.Rounded.VerifiedUser,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
      )
    }

    uiState.allMediaEntries.forEach { entry ->
      val blockingReason = PermissionPolicy.approvalBlockingReason(entry.origin, entry.permission)
      PermissionCardItem(
        title = entry.title,
        subtitle = "${entry.mediaType.displayLabel} • ${entry.origin.displayLabel}",
        permission = entry.permission,
        blockingReason = blockingReason,
        isApproved = entry.isEligibleForProduction,
        glowEnabled = glowEnabled,
        onReviewClick = { editingMedia = entry },
      )
    }

    uiState.creatorSources.forEach { creator ->
      val origin = creator.platform.toContentOrigin()
      val blockingReason = PermissionPolicy.approvalBlockingReason(origin, creator.permission)
      PermissionCardItem(
        title = creator.creatorName,
        subtitle = "Creator • ${creator.platform.displayLabel}",
        permission = creator.permission,
        blockingReason = blockingReason,
        isApproved = creator.isEligibleForProduction,
        glowEnabled = glowEnabled,
        onReviewClick = { editingCreator = creator },
      )
    }
  }

  editingMedia?.let { media ->
    PermissionEditDialog(
      title = media.title,
      origin = media.origin,
      initial = media.permission,
      onDismiss = { editingMedia = null },
      onSave = { updated ->
        viewModel.updateMediaPermission(media.id, updated)
        editingMedia = null
      },
    )
  }

  editingCreator?.let { creator ->
    PermissionEditDialog(
      title = creator.creatorName,
      origin = creator.platform.toContentOrigin(),
      initial = creator.permission,
      onDismiss = { editingCreator = null },
      onSave = { updated ->
        viewModel.updateCreatorPermission(creator.id, updated)
        editingCreator = null
      },
    )
  }
}

@Composable
private fun PermissionCardItem(
  title: String,
  subtitle: String,
  permission: PermissionRecord,
  blockingReason: String?,
  isApproved: Boolean,
  glowEnabled: Boolean,
  onReviewClick: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = if (isApproved) listOf(ZuzuGreen, ZuzuTurquoise) else listOf(ZuzuMagenta, ZuzuPink),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
      StatusPill(
        label = permission.status.displayLabel,
        tint = if (isApproved) ZuzuGreen else ZuzuAmber,
      )
    }
    if (permission.contentOwner.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Owner: ${permission.contentOwner}",
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkPrimary,
      )
    }
    if (blockingReason != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = ZuzuSoftRose,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = blockingReason,
          style = MaterialTheme.typography.labelSmall,
          color = ZuzuError,
          modifier = Modifier.padding(8.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    ZuzuSecondaryActionButton(
      text = if (isApproved) "Edit Permission Record" else "Verify & Update Permission",
      onClick = onReviewClick,
      accentColor = if (isApproved) ZuzuTurquoise else ZuzuPink,
      icon = Icons.Rounded.VerifiedUser,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun PermissionEditDialog(
  title: String,
  origin: ContentOrigin,
  initial: PermissionRecord,
  onDismiss: () -> Unit,
  onSave: (PermissionRecord) -> Unit,
) {
  var status by remember { mutableStateOf(initial.status) }
  var owner by remember { mutableStateOf(initial.contentOwner) }
  var evidence by remember { mutableStateOf(initial.evidenceDescription) }
  var reference by remember { mutableStateOf(initial.licenseReference) }
  var date by remember { mutableStateOf(initial.permissionDate) }
  var notes by remember { mutableStateOf(initial.additionalNotes) }
  var approveReuse by remember { mutableStateOf(initial.approvedForReuse) }

  val draftRecord = PermissionRecord(
    status = status,
    contentOwner = owner.trim(),
    evidenceDescription = evidence.trim(),
    licenseReference = reference.trim(),
    permissionDate = date.trim(),
    additionalNotes = notes.trim(),
    approvedForReuse = approveReuse,
  )
  val blockingReason = PermissionPolicy.approvalBlockingReason(origin, draftRecord)

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text("Permission: $title", fontWeight = FontWeight.Bold, color = ZuzuInkPrimary) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PermissionStatus.entries.forEach { item ->
            ZuzuPillChip(
              label = item.displayLabel,
              selected = status == item,
              onClick = {
                status = item
                if (!PermissionPolicy.isVerifiedStatus(item)) {
                  approveReuse = false
                }
              },
              gradientColors = listOf(ZuzuViolet, ZuzuPink),
            )
          }
        }
        OutlinedTextField(
          value = owner,
          onValueChange = { owner = it },
          label = { Text("Content Owner") },
          singleLine = true,
        )
        OutlinedTextField(
          value = evidence,
          onValueChange = { evidence = it },
          label = { Text("Permission Evidence Description") },
        )
        OutlinedTextField(
          value = reference,
          onValueChange = { reference = it },
          label = { Text("License or Permission Reference") },
          singleLine = true,
        )
        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Permission Date (e.g. 2026-11-22)") },
          singleLine = true,
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Additional Notes") },
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Approve for AI Production",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Switch(
            checked = approveReuse && blockingReason == null,
            onCheckedChange = { wantApprove ->
              if (blockingReason == null) {
                approveReuse = wantApprove
              }
            },
            enabled = blockingReason == null,
          )
        }
        if (blockingReason != null) {
          Text(
            text = blockingReason,
            style = MaterialTheme.typography.labelSmall,
            color = ZuzuError,
          )
        }
      }
    },
    confirmButton = {
      ZuzuGradientButton(
        text = "Save",
        onClick = {
          onSave(
            draftRecord.copy(
              approvedForReuse = approveReuse && blockingReason == null,
            ),
          )
        },
        gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel", color = ZuzuInkSecondary) }
    },
  )
}
