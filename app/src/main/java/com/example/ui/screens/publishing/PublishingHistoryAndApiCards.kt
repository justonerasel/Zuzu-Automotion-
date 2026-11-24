package com.example.ui.screens.publishing

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PublishingAuthErrorCode
import com.example.model.PublishingHistoryStatus
import com.example.model.PublishingJobRecord
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
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuOrangeSoft
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuSoftRose
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.PublishingCenterUiState
import com.example.viewmodel.PublishingCenterViewModel
import com.example.viewmodel.PublishingHistoryFilter
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun PublishingScheduleQueueSection(
  uiState: PublishingCenterUiState,
  viewModel: PublishingCenterViewModel,
  glowEnabled: Boolean,
) {
  val scheduledJobs = remember(uiState.publishingJobs) {
    uiState.publishingJobs
      .filter { it.status == PublishingHistoryStatus.Scheduled }
      .sortedBy { it.scheduledAtMillis ?: it.createdAtMillis }
  }
  var editingJob by remember { mutableStateOf<PublishingJobRecord?>(null) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (scheduledJobs.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Scheduled Posts Queued",
        description = "Select an approved post in Publish & Preview, choose Schedule, pick a future date/time, and enqueue a backend publishing job.",
        icon = Icons.Rounded.Schedule,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
        modifier = Modifier.testTag("publishing_scheduled_empty"),
      )
    } else {
      scheduledJobs.forEach { job ->
        PublishingHistoryItemCard(
          job = job,
          glowEnabled = glowEnabled,
          onEdit = { editingJob = job },
          onCancel = { viewModel.cancelScheduledPublishJob(job.id) },
          onRetry = { viewModel.retryFailedPublishJob(job.id) },
          onDelete = { viewModel.deleteHistoryRecord(job.id) },
        )
      }
    }
  }

  editingJob?.let { job ->
    EditScheduledPublishDialog(
      job = job,
      onDismiss = { editingJob = null },
      onSave = { newTitle, newCaption, newTime ->
        viewModel.editScheduledPublishJob(job.id, newTitle, newCaption, newTime)
        editingJob = null
      },
    )
  }
}

@Composable
fun PublishingHistorySection(
  uiState: PublishingCenterUiState,
  viewModel: PublishingCenterViewModel,
  glowEnabled: Boolean,
) {
  var editingJob by remember { mutableStateOf<PublishingJobRecord?>(null) }

  val filtered = remember(uiState.publishingJobs, uiState.historyFilter) {
    when (uiState.historyFilter) {
      PublishingHistoryFilter.All -> uiState.publishingJobs
      PublishingHistoryFilter.Published -> uiState.publishingJobs.filter { it.status == PublishingHistoryStatus.Published }
      PublishingHistoryFilter.Scheduled -> uiState.publishingJobs.filter { it.status == PublishingHistoryStatus.Scheduled }
      PublishingHistoryFilter.Processing -> uiState.publishingJobs.filter { it.status == PublishingHistoryStatus.Processing }
      PublishingHistoryFilter.Failed -> uiState.publishingJobs.filter { it.status == PublishingHistoryStatus.Failed }
      PublishingHistoryFilter.Draft -> uiState.publishingJobs.filter { it.status == PublishingHistoryStatus.Draft }
      PublishingHistoryFilter.Cancelled -> uiState.publishingJobs.filter { it.status == PublishingHistoryStatus.Cancelled }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PublishingHistoryFilter.entries.forEach { filter ->
        val count = when (filter) {
          PublishingHistoryFilter.All -> uiState.publishingJobs.size
          PublishingHistoryFilter.Published -> uiState.publishingJobs.count { it.status == PublishingHistoryStatus.Published }
          PublishingHistoryFilter.Scheduled -> uiState.publishingJobs.count { it.status == PublishingHistoryStatus.Scheduled }
          PublishingHistoryFilter.Processing -> uiState.publishingJobs.count { it.status == PublishingHistoryStatus.Processing }
          PublishingHistoryFilter.Failed -> uiState.publishingJobs.count { it.status == PublishingHistoryStatus.Failed }
          PublishingHistoryFilter.Draft -> uiState.publishingJobs.count { it.status == PublishingHistoryStatus.Draft }
          PublishingHistoryFilter.Cancelled -> uiState.publishingJobs.count { it.status == PublishingHistoryStatus.Cancelled }
        }
        ZuzuPillChip(
          label = "${filter.displayLabel} ($count)",
          selected = uiState.historyFilter == filter,
          onClick = { viewModel.selectHistoryFilter(filter) },
          gradientColors = ZuzuHeroGradient,
          modifier = Modifier.testTag("publishing_history_filter_${filter.name}"),
        )
      }
    }

    if (filtered.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No ${uiState.historyFilter.displayLabel} Records",
        description = "All publishing attempts, scheduled runs, drafts, failures, and cancelled jobs appear here with exact status and account metadata.",
        icon = Icons.Rounded.History,
        gradientColors = ZuzuVioletPinkGradient,
        glowEnabled = glowEnabled,
        modifier = Modifier.testTag("publishing_history_empty"),
      )
    } else {
      filtered.forEach { job ->
        PublishingHistoryItemCard(
          job = job,
          glowEnabled = glowEnabled,
          onEdit = { editingJob = job },
          onCancel = { viewModel.cancelScheduledPublishJob(job.id) },
          onRetry = { viewModel.retryFailedPublishJob(job.id) },
          onDelete = { viewModel.deleteHistoryRecord(job.id) },
        )
      }
    }
  }

  editingJob?.let { job ->
    EditScheduledPublishDialog(
      job = job,
      onDismiss = { editingJob = null },
      onSave = { newTitle, newCaption, newTime ->
        viewModel.editScheduledPublishJob(job.id, newTitle, newCaption, newTime)
        editingJob = null
      },
    )
  }
}

@Composable
private fun PublishingHistoryItemCard(
  job: PublishingJobRecord,
  glowEnabled: Boolean,
  onEdit: () -> Unit,
  onCancel: () -> Unit,
  onRetry: () -> Unit,
  onDelete: () -> Unit,
) {
  val statusTint = when (job.status) {
    PublishingHistoryStatus.Published -> ZuzuGreen
    PublishingHistoryStatus.Scheduled -> ZuzuElectricBlue
    PublishingHistoryStatus.Processing -> ZuzuCyan
    PublishingHistoryStatus.Failed -> ZuzuError
    PublishingHistoryStatus.Draft -> ZuzuAmber
    PublishingHistoryStatus.Cancelled -> ZuzuInkMuted
  }
  val displayTs = job.scheduledAtMillis ?: job.updatedAtMillis
  val formattedTime = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(displayTs))

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuElectricBlue, ZuzuPurple),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("publishing_job_card_${job.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusPill(label = "${job.platform.displayName} • ${job.status.displayLabel}", tint = statusTint)
      Text(
        text = "Attempt ${job.attemptCount}/${job.maxRetries}",
        style = MaterialTheme.typography.labelSmall,
        color = ZuzuInkSecondary,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(job.postTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
    Text(
      text = "Account: ${job.accountDisplayName} (${job.accountHandle})",
      style = MaterialTheme.typography.bodySmall,
      color = ZuzuInkPrimary,
    )
    Text(
      text = "${if (job.scheduledAtMillis != null) "Scheduled For" else "Updated"}: $formattedTime",
      style = MaterialTheme.typography.bodySmall,
      color = ZuzuTurquoise,
    )
    if (job.statusDetail.isNotBlank()) {
      Text(job.statusDetail, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
    }
    job.remotePlatformPostId?.let { postId ->
      Text("Platform Post ID: $postId", style = MaterialTheme.typography.labelSmall, color = ZuzuGreen)
    }

    if (!job.errorMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = ZuzuSoftRose,
        border = BorderStroke(1.dp, ZuzuError.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          if (job.authErrorCode != PublishingAuthErrorCode.None) {
            Text(
              text = job.authErrorCode.displayLabel,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = ZuzuError,
            )
          }
          Text(job.errorMessage, style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      if (job.isEditableOrCancellableSchedule) {
        ZuzuSecondaryActionButton(
          text = "Edit",
          onClick = onEdit,
          accentColor = ZuzuElectricBlue,
          icon = Icons.Rounded.Edit,
          modifier = Modifier.testTag("pub_edit_${job.id}"),
        )
        ZuzuSecondaryActionButton(
          text = "Cancel",
          onClick = onCancel,
          accentColor = ZuzuError,
          icon = Icons.Rounded.Cancel,
          modifier = Modifier.testTag("pub_cancel_${job.id}"),
        )
      }
      if (job.canRetrySafely) {
        ZuzuGradientButton(
          text = "Safe Retry",
          onClick = onRetry,
          gradientColors = ZuzuBlueTurquoiseGradient,
          icon = Icons.Rounded.Replay,
          modifier = Modifier.testTag("pub_retry_${job.id}"),
        )
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete record", tint = ZuzuInkSecondary)
      }
    }
  }
}

@Composable
fun PublishingBackendApiSection(
  uiState: PublishingCenterUiState,
  viewModel: PublishingCenterViewModel,
  glowEnabled: Boolean,
) {
  val report = uiState.backendReport
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuPurple, ZuzuElectricBlue),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("publishing_backend_overview_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (report.anyPlatformApiReady) Icons.Rounded.CloudDone else Icons.Rounded.CloudQueue,
            contentDescription = null,
            tint = if (report.anyPlatformApiReady) ZuzuGreen else ZuzuAmber,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Official Platform API Readiness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        StatusPill(
          label = if (report.anyPlatformApiReady) "API Ready" else "Setup Required",
          tint = if (report.anyPlatformApiReady) ZuzuGreen else ZuzuAmber,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = if (report.backendEndpointConfigured) {
          "HTTPS publishing backend configured."
        } else {
          "HTTPS publishing backend & platform developer approvals are not configured yet. Publishing safely returns Setup Required without simulating uploads."
        },
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
      )
      Spacer(modifier = Modifier.height(10.dp))
      ZuzuSecondaryActionButton(
        text = "Re-check Backend Configuration",
        onClick = viewModel::refreshBackendStatus,
        accentColor = ZuzuElectricBlue,
        icon = Icons.Rounded.Refresh,
      )
    }

    report.platformCapabilities.forEach { cap ->
      val ready = cap.credentialsReady && cap.apiReviewApproved
      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = listOf(ZuzuCyan, ZuzuPurple),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(cap.apiName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          StatusPill(label = cap.statusLabel, tint = if (ready) ZuzuGreen else ZuzuAmber)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(cap.officialRestrictionsNote, style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Required Scopes: ${cap.requiredScopes.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = ZuzuPurple)
        if (cap.missingSetupChecklist.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(shape = RoundedCornerShape(10.dp), color = ZuzuOrangeSoft, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              cap.missingSetupChecklist.forEachIndexed { idx, step ->
                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
              }
            }
          }
        }
      }
    }

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuGreen, ZuzuCyan),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = ZuzuGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Token & OAuth Security Architecture", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      report.securityComplianceNotes.forEach { note ->
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ZuzuGreen, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(note, style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
        }
      }
    }
  }
}

@Composable
private fun EditScheduledPublishDialog(
  job: PublishingJobRecord,
  onDismiss: () -> Unit,
  onSave: (String, String, Long) -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf(job.customization.title) }
  var caption by remember { mutableStateOf(job.customization.caption) }
  var ts by remember { mutableStateOf(job.scheduledAtMillis ?: (System.currentTimeMillis() + 3_600_000L)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text("Edit Scheduled ${job.platform.displayName} Post") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Post Title") })
        OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Caption / Message") })
        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                TimePickerDialog(
                  context,
                  { _, h, min ->
                    cal.set(y, m, d, h, min, 0)
                    ts = cal.timeInMillis
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
          Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts)))
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(title, caption, ts) }) { Text("Save Schedule") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
