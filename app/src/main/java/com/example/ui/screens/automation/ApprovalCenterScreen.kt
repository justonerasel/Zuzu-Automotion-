package com.example.ui.screens.automation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.ApprovalContentItem
import com.example.model.ApprovalStatus
import com.example.model.PermissionStatus
import com.example.model.SocialPlatform
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuGreenSoft
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrangeSoft
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ApprovalFilterTab
import com.example.viewmodel.ApprovalSchedulerUiState
import com.example.viewmodel.ApprovalSchedulerViewModel
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun ApprovalCenterScreen(
  uiState: ApprovalSchedulerUiState,
  viewModel: ApprovalSchedulerViewModel,
  glowEnabled: Boolean,
  onOpenScheduler: () -> Unit,
  onOpenPublishingCenter: (String) -> Unit = {},
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  var previewingItem by remember { mutableStateOf<ApprovalContentItem?>(null) }
  var editingItem by remember { mutableStateOf<ApprovalContentItem?>(null) }
  var rejectingItem by remember { mutableStateOf<ApprovalContentItem?>(null) }
  var permissionItem by remember { mutableStateOf<ApprovalContentItem?>(null) }

  val filteredItems = remember(uiState.approvalItems, uiState.selectedApprovalTab) {
    when (uiState.selectedApprovalTab) {
      ApprovalFilterTab.PendingReviews -> uiState.approvalItems.filter { it.status == ApprovalStatus.PendingReview }
      ApprovalFilterTab.Approved -> uiState.approvalItems.filter { it.status == ApprovalStatus.Approved }
      ApprovalFilterTab.Rejected -> uiState.approvalItems.filter { it.status == ApprovalStatus.Rejected }
      ApprovalFilterTab.Drafts -> uiState.approvalItems.filter { it.status == ApprovalStatus.Draft }
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZuzuSecondaryTopBar(
          title = stringResource(R.string.approval_center_title),
          onBack = onBack,
          backButtonTag = "approval_back_button",
          badgeText = "${uiState.approvalItems.count { it.status == ApprovalStatus.PendingReview }} Pending",
          badgeTint = ZuzuViolet,
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "Human-in-the-Loop Gate • Zero Unapproved Publishing",
          title = stringResource(R.string.approval_center_title),
          subtitle = stringResource(R.string.approval_center_subtitle),
          icon = Icons.Rounded.Approval,
          gradientColors = listOf(ZuzuDeepPurple, ZuzuPurple, ZuzuElectricBlue),
        ) {
          Button(
            onClick = onOpenScheduler,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ZuzuWhite, contentColor = ZuzuDeepPurple),
          ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Scheduler & Cloud Automation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          }
        }
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ApprovalFilterTab.entries.forEach { tab ->
            val count = when (tab) {
              ApprovalFilterTab.PendingReviews -> uiState.approvalItems.count { it.status == ApprovalStatus.PendingReview }
              ApprovalFilterTab.Approved -> uiState.approvalItems.count { it.status == ApprovalStatus.Approved }
              ApprovalFilterTab.Rejected -> uiState.approvalItems.count { it.status == ApprovalStatus.Rejected }
              ApprovalFilterTab.Drafts -> uiState.approvalItems.count { it.status == ApprovalStatus.Draft }
            }
            ZuzuPillChip(
              label = "${tab.displayLabel} ($count)",
              selected = uiState.selectedApprovalTab == tab,
              onClick = { viewModel.selectApprovalTab(tab) },
              gradientColors = ZuzuHeroGradient,
              modifier = Modifier.testTag("approval_tab_${tab.name}"),
            )
          }
        }
      }

      if (!uiState.feedbackMessage.isNullOrBlank() || !uiState.errorMessage.isNullOrBlank()) {
        item {
          val isErr = !uiState.errorMessage.isNullOrBlank()
          ZuzuAlertBanner(
            message = uiState.errorMessage ?: uiState.feedbackMessage.orEmpty(),
            tint = if (isErr) ZuzuError else ZuzuGreen,
            icon = if (isErr) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
            onDismiss = viewModel::clearMessages,
          )
        }
      }

      if (filteredItems.isEmpty()) {
        item {
          ZuzuEmptyStatePanel(
            title = "No items in ${uiState.selectedApprovalTab.displayLabel}",
            description = "Export a video from AI Video Factory or a motion graphic from Auto Motion Post Studio and send it here for human approval.",
            icon = Icons.Rounded.Approval,
            gradientColors = ZuzuBlueTurquoiseGradient,
            glowEnabled = glowEnabled,
          )
        }
      } else {
        items(filteredItems, key = { it.id }) { item ->
          ApprovalItemCard(
            item = item,
            glowEnabled = glowEnabled,
            onPreview = { previewingItem = item },
            onEditMetadata = { editingItem = item },
            onEditPermission = { permissionItem = item },
            onApprove = { viewModel.approveItem(item.id) },
            onReject = { rejectingItem = item },
            onSaveDraft = { viewModel.saveApprovalDraft(item.id) },
            onSchedule = onOpenScheduler,
            onOpenPublishingCenter = { onOpenPublishingCenter(item.id) },
          )
        }
      }
    }
  }

  previewingItem?.let { item ->
    ApprovalMediaPreviewDialog(
      item = item,
      onDismiss = { previewingItem = null },
    )
  }

  editingItem?.let { item ->
    EditApprovalMetadataDialog(
      item = item,
      onDismiss = { editingItem = null },
      onSave = { title, desc, tags, platforms, scheduledAt ->
        viewModel.updateApprovalItemDetails(item.id, title, desc, tags, platforms, scheduledAt)
        editingItem = null
      },
    )
  }

  permissionItem?.let { item ->
    EditApprovalPermissionDialog(
      item = item,
      onDismiss = { permissionItem = null },
      onSave = { status, owner, evidence, approved ->
        viewModel.updateItemPermissionEvidence(item.id, status, owner, evidence, approved)
        permissionItem = null
      },
    )
  }

  rejectingItem?.let { item ->
    var reason by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { rejectingItem = null },
      containerColor = ZuzuWhite,
      title = { Text("Reject Content") },
      text = {
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Rejection Note") },
          placeholder = { Text("e.g., Update headline contrast or hook pacing") },
        )
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.rejectItem(item.id, reason)
          rejectingItem = null
        }) { Text("Reject", color = ZuzuError, fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { rejectingItem = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun ApprovalItemCard(
  item: ApprovalContentItem,
  glowEnabled: Boolean,
  onPreview: () -> Unit,
  onEditMetadata: () -> Unit,
  onEditPermission: () -> Unit,
  onApprove: () -> Unit,
  onReject: () -> Unit,
  onSaveDraft: () -> Unit,
  onSchedule: () -> Unit,
  onOpenPublishingCenter: () -> Unit,
) {
  val permError = item.permissionBlockingReason()
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = ZuzuVioletPinkGradient,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("approval_card_${item.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusPill(label = item.sourceKind.displayLabel, tint = ZuzuElectricBlue)
      StatusPill(label = item.cloudSyncState.displayLabel, tint = ZuzuAmber)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
    if (item.description.isNotBlank()) {
      Text(item.description, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
    }
    if (item.hashtagsCsv.isNotBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(item.hashtagsCsv, style = MaterialTheme.typography.labelSmall, color = ZuzuPurple)
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Platforms: ${item.targetPlatforms.joinToString { it.displayName }.ifEmpty { "None selected" }}",
      style = MaterialTheme.typography.labelSmall,
      color = ZuzuInkPrimary,
    )
    item.scheduledAtMillis?.let { ts ->
      Text(
        text = "Target Date/Time: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ts))}",
        style = MaterialTheme.typography.labelSmall,
        color = ZuzuTurquoise,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = if (permError == null) ZuzuGreenSoft else ZuzuOrangeSoft,
      border = BorderStroke(1.dp, (if (permError == null) ZuzuGreen else ZuzuAmber).copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = if (permError == null) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
          contentDescription = null,
          tint = if (permError == null) ZuzuGreen else ZuzuAmber,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = permError ?: "Permission verified (${item.permissionStatus.displayLabel} • ${item.permissionOrigin.displayLabel})",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkPrimary,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onEditPermission) {
          Text("Verify", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (item.rejectionReason.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text("Rejection Note: ${item.rejectionReason}", style = MaterialTheme.typography.bodySmall, color = ZuzuError)
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ZuzuSecondaryActionButton(text = "Preview", onClick = onPreview, accentColor = ZuzuElectricBlue, icon = Icons.Rounded.PlayCircle)
      ZuzuSecondaryActionButton(text = "Edit", onClick = onEditMetadata, accentColor = ZuzuPurple, icon = Icons.Rounded.Edit)
      ZuzuSecondaryActionButton(text = "Save Draft", onClick = onSaveDraft, accentColor = ZuzuTurquoise, icon = Icons.Rounded.Save)
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      if (item.status != ApprovalStatus.Approved) {
        ZuzuGradientButton(
          text = "Approve",
          onClick = onApprove,
          gradientColors = listOf(ZuzuGreen, ZuzuTurquoise),
          icon = Icons.Rounded.CheckCircle,
          modifier = Modifier
            .weight(1f)
            .testTag("approve_btn_${item.id}"),
        )
      } else {
        ZuzuGradientButton(
          text = "Schedule",
          onClick = onSchedule,
          gradientColors = ZuzuBlueTurquoiseGradient,
          icon = Icons.Rounded.Schedule,
          modifier = Modifier.weight(1f),
        )
      }

      ZuzuSecondaryActionButton(
        text = "Reject",
        onClick = onReject,
        accentColor = ZuzuError,
        icon = Icons.Rounded.ThumbDown,
        modifier = Modifier.weight(1f),
      )
    }
    if (item.status == ApprovalStatus.Approved) {
      Spacer(modifier = Modifier.height(8.dp))
      ZuzuGradientButton(
        text = "Open in Publishing Center",
        onClick = onOpenPublishingCenter,
        gradientColors = listOf(ZuzuPurple, ZuzuMagenta),
        icon = Icons.Rounded.Publish,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pub_open_from_approval_${item.id}"),
      )
    }
  }
}

@Composable
private fun ApprovalMediaPreviewDialog(
  item: ApprovalContentItem,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val file = item.mediaFilePath?.let { File(it) }?.takeIf { it.exists() }
  val isImage = item.mediaFilePath?.lowercase()?.let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") } == true

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text(item.title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (file != null && isImage) {
          val bmp = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath) }
          if (bmp != null) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = item.title,
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            )
          }
        } else if (file != null) {
          AndroidView(
            factory = { ctx ->
              VideoView(ctx).apply {
                val videoView = this
                setMediaController(MediaController(ctx).apply { setAnchorView(videoView) })
                setVideoPath(file.absolutePath)
                setOnPreparedListener { mp ->
                  mp.isLooping = true
                  start()
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp),
          )
        } else if (!item.mediaUriString.isNullOrBlank()) {
          val bmp = remember(item.mediaUriString) {
            runCatching {
              context.contentResolver.openInputStream(Uri.parse(item.mediaUriString))?.use {
                BitmapFactory.decodeStream(it)
              }
            }.getOrNull()
          }
          if (bmp != null) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = item.title,
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            )
          } else {
            Text("Media linked via URI: ${item.mediaUriString}")
          }
        } else {
          Text("No exported file attached yet. Export an MP4 or PNG first for full playback preview.")
        }
        Text(item.description, style = MaterialTheme.typography.bodySmall)
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
  )
}

@Composable
private fun EditApprovalMetadataDialog(
  item: ApprovalContentItem,
  onDismiss: () -> Unit,
  onSave: (String, String, String, List<SocialPlatform>, Long?) -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf(item.title) }
  var description by remember { mutableStateOf(item.description) }
  var hashtags by remember { mutableStateOf(item.hashtagsCsv) }
  var selectedPlatforms by remember { mutableStateOf(item.targetPlatforms.toSet()) }
  var scheduledAtMillis by remember { mutableStateOf(item.scheduledAtMillis) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text("Edit Post Metadata") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
        OutlinedTextField(value = hashtags, onValueChange = { hashtags = it }, label = { Text("Hashtags") })
        Text("Target Platforms", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SocialPlatform.entries.forEach { platform ->
            val checked = selectedPlatforms.contains(platform)
            ZuzuPillChip(
              label = platform.displayName,
              selected = checked,
              onClick = {
                selectedPlatforms = if (checked) selectedPlatforms - platform else selectedPlatforms + platform
              },
              gradientColors = ZuzuBlueTurquoiseGradient,
            )
          }
        }
        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply {
              timeInMillis = scheduledAtMillis ?: (System.currentTimeMillis() + 3_600_000L)
            }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                TimePickerDialog(
                  context,
                  { _, h, min ->
                    cal.set(y, m, d, h, min, 0)
                    scheduledAtMillis = cal.timeInMillis
                  },
                  cal.get(Calendar.HOUR_OF_DAY),
                  cal.get(Calendar.MINUTE),
                  false,
                ).show()
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH),
            ).show()
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            scheduledAtMillis?.let {
              DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
            } ?: "Pick Publishing Date & Time",
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        onSave(title, description, hashtags, selectedPlatforms.toList(), scheduledAtMillis)
      }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun EditApprovalPermissionDialog(
  item: ApprovalContentItem,
  onDismiss: () -> Unit,
  onSave: (PermissionStatus, String, String, Boolean) -> Unit,
) {
  var status by remember { mutableStateOf(item.permissionStatus) }
  var owner by remember { mutableStateOf(item.permissionOwner) }
  var evidence by remember { mutableStateOf(item.permissionEvidence) }
  var approved by remember { mutableStateOf(item.permissionApprovedForReuse) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text("Content Permission Verification") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Origin: ${item.permissionOrigin.displayLabel}", style = MaterialTheme.typography.labelMedium)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PermissionStatus.entries.forEach { st ->
            ZuzuPillChip(
              label = st.displayLabel,
              selected = status == st,
              onClick = { status = st },
              gradientColors = listOf(ZuzuPurple, ZuzuElectricBlue),
            )
          }
        }
        OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Content Owner") })
        OutlinedTextField(value = evidence, onValueChange = { evidence = it }, label = { Text("License / Permission Evidence") })
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = approved, onCheckedChange = { approved = it })
          Text("Approved for commercial/social reuse", style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(status, owner, evidence, approved) }) { Text("Save Permission") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
