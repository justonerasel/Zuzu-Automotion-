package com.example.ui.screens.automation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.cloud.FirestoreSchemaContract
import com.example.model.ApprovalStatus
import com.example.model.CloudServiceHealth
import com.example.model.ScheduledJobStatus
import com.example.model.ScheduledPostJob
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuLoadingBanner
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.components.ZuzuSegmentedTabStrip
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuOrangeSoft
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ApprovalSchedulerUiState
import com.example.viewmodel.ApprovalSchedulerViewModel
import com.example.viewmodel.AutomationScreenTab
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun SchedulerAutomationScreen(
  uiState: ApprovalSchedulerUiState,
  platformAccounts: List<SocialPlatformAccountState>,
  showCloudStatusFirst: Boolean,
  viewModel: ApprovalSchedulerViewModel,
  glowEnabled: Boolean,
  onOpenApprovalCenter: () -> Unit,
  onOpenSocialAccounts: () -> Unit,
  onOpenPublishingCenter: () -> Unit = {},
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  LaunchedEffect(showCloudStatusFirst) {
    if (showCloudStatusFirst) {
      viewModel.selectAutomationTab(AutomationScreenTab.CloudStatus)
    }
  }

  val approvedItems = remember(uiState.approvalItems) {
    uiState.approvalItems.filter { it.status == ApprovalStatus.Approved }
  }
  var selectedApprovalId by remember(approvedItems) {
    mutableStateOf(approvedItems.firstOrNull()?.id)
  }
  var selectedPlatforms by remember { mutableStateOf(setOf(SocialPlatform.YouTube)) }
  var scheduledTimestamp by remember { mutableStateOf(System.currentTimeMillis() + 3_600_000L) }
  var editingJob by remember { mutableStateOf<ScheduledPostJob?>(null) }

  val context = LocalContext.current

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
          title = stringResource(R.string.scheduler_title),
          onBack = onBack,
          backButtonTag = "scheduler_back_button",
          badgeText = uiState.cloudStatus.overallHealth.displayLabel,
          badgeTint = if (uiState.cloudStatus.overallHealth == CloudServiceHealth.Ready) ZuzuGreen else ZuzuAmber,
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "Cloud Tasks + Cloud Scheduler • Duplicate-Safe Queue",
          title = if (uiState.selectedAutomationTab == AutomationScreenTab.Scheduler) {
            stringResource(R.string.scheduler_title)
          } else {
            stringResource(R.string.cloud_status_title)
          },
          subtitle = "Once Cloud credentials & HTTPS workers are configured, scheduled Cloud Tasks execute even when your phone is closed.",
          icon = Icons.Rounded.Schedule,
          gradientColors = ZuzuBlueTurquoiseGradient,
        ) {
          Button(
            onClick = onOpenPublishingCenter,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ZuzuWhite, contentColor = ZuzuElectricBlue),
            modifier = Modifier.testTag("scheduler_open_publishing_btn"),
          ) {
            Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.publishing_open_cta), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          }
        }
      }

      item {
        ZuzuSegmentedTabStrip(
          items = AutomationScreenTab.entries,
          selectedItem = uiState.selectedAutomationTab,
          labelFor = { it.displayLabel },
          onSelect = viewModel::selectAutomationTab,
          gradientColors = ZuzuBlueTurquoiseGradient,
          testTagFor = { "automation_tab_${it.name}" },
        )
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

      if (uiState.selectedAutomationTab == AutomationScreenTab.Scheduler) {
        item {
          ZuzuGlassCard(
            glowEnabled = glowEnabled,
            accentColors = ZuzuBlueTurquoiseGradient,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Schedule Approved Content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            if (approvedItems.isEmpty()) {
              Text(
                text = "No approved posts available yet. Review and explicitly approve a video or motion post in Approval Center first.",
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuInkSecondary,
              )
              Spacer(modifier = Modifier.height(10.dp))
              ZuzuSecondaryActionButton(
                text = "Go to Approval Center",
                onClick = onOpenApprovalCenter,
                accentColor = ZuzuPurple,
              )
            } else {
              Text("1. Select Approved Item", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))
              Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                approvedItems.forEach { apr ->
                  ZuzuPillChip(
                    label = apr.title,
                    selected = selectedApprovalId == apr.id,
                    onClick = { selectedApprovalId = apr.id },
                    gradientColors = ZuzuHeroGradient,
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text("2. Select Target Platforms", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))
              Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialPlatform.entries.forEach { platform ->
                  val acct = platformAccounts.firstOrNull { it.platform == platform }
                  val connectedTag = if (acct?.isConnected == true) "Connected" else "Setup Req"
                  val isSelected = selectedPlatforms.contains(platform)
                  ZuzuPillChip(
                    label = "${platform.displayName} ($connectedTag)",
                    selected = isSelected,
                    onClick = {
                      selectedPlatforms = if (isSelected) selectedPlatforms - platform else selectedPlatforms + platform
                    },
                    gradientColors = ZuzuBlueTurquoiseGradient,
                  )
                }
              }
              TextButton(onClick = onOpenSocialAccounts) {
                Text("Manage OAuth Social Connections", style = MaterialTheme.typography.labelSmall, color = ZuzuElectricBlue)
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "3. Scheduled Time: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(scheduledTimestamp))}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ZuzuSecondaryActionButton(
                  text = "+1 Hour",
                  onClick = { scheduledTimestamp = System.currentTimeMillis() + 3_600_000L },
                  accentColor = ZuzuElectricBlue,
                )
                ZuzuSecondaryActionButton(
                  text = "+1 Day",
                  onClick = { scheduledTimestamp = System.currentTimeMillis() + 86_400_000L },
                  accentColor = ZuzuPurple,
                )
                ZuzuSecondaryActionButton(
                  text = "Pick Time",
                  onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = scheduledTimestamp }
                    DatePickerDialog(
                      context,
                      { _, y, m, d ->
                        TimePickerDialog(
                          context,
                          { _, h, min ->
                            cal.set(y, m, d, h, min, 0)
                            scheduledTimestamp = cal.timeInMillis
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
                  accentColor = ZuzuTurquoise,
                  icon = Icons.Rounded.CalendarMonth,
                )
              }

              Spacer(modifier = Modifier.height(12.dp))
              if (uiState.isProcessingSchedule) {
                ZuzuLoadingBanner(label = "Scheduling cloud job…", accentColor = ZuzuElectricBlue)
              } else {
                ZuzuGradientButton(
                  text = "Schedule Post",
                  onClick = {
                    selectedApprovalId?.let { aprId ->
                      viewModel.scheduleApprovedPost(aprId, selectedPlatforms.toList(), scheduledTimestamp)
                    }
                  },
                  gradientColors = ZuzuBlueTurquoiseGradient,
                  icon = Icons.Rounded.Schedule,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_create_job_btn"),
                )
              }
            }
          }
        }

        if (uiState.scheduledJobs.isEmpty()) {
          item {
            ZuzuEmptyStatePanel(
              title = "No Scheduled Posts Yet",
              description = "Scheduled jobs appear here with their idempotency key, Cloud Task ID, retry counters, and Edit/Cancel actions.",
              icon = Icons.Rounded.Schedule,
              gradientColors = ZuzuVioletPinkGradient,
              glowEnabled = glowEnabled,
            )
          }
        } else {
          items(uiState.scheduledJobs, key = { it.id }) { job ->
            ScheduledJobCard(
              job = job,
              glowEnabled = glowEnabled,
              onEdit = { editingJob = job },
              onCancel = { viewModel.cancelScheduledJob(job.id) },
              onRetry = { viewModel.retryScheduledJob(job.id) },
              onDelete = { viewModel.deleteScheduledJob(job.id) },
            )
          }
        }
      } else {
        item {
          CloudBackendStatusSection(
            uiState = uiState,
            viewModel = viewModel,
            glowEnabled = glowEnabled,
          )
        }
      }
    }
  }

  editingJob?.let { job ->
    EditScheduledJobDialog(
      job = job,
      onDismiss = { editingJob = null },
      onSave = { platforms, ts ->
        viewModel.editScheduledJob(job.id, platforms, ts)
        editingJob = null
      },
    )
  }
}

@Composable
private fun ScheduledJobCard(
  job: ScheduledPostJob,
  glowEnabled: Boolean,
  onEdit: () -> Unit,
  onCancel: () -> Unit,
  onRetry: () -> Unit,
  onDelete: () -> Unit,
) {
  val statusTint = when (job.status) {
    ScheduledJobStatus.ScheduledCloudPending, ScheduledJobStatus.Completed -> ZuzuGreen
    ScheduledJobStatus.QueuedLocalSetupRequired, ScheduledJobStatus.Retrying -> ZuzuAmber
    ScheduledJobStatus.Running -> ZuzuElectricBlue
    ScheduledJobStatus.Cancelled, ScheduledJobStatus.Failed -> ZuzuError
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = ZuzuBlueTurquoiseGradient,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("scheduled_job_${job.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusPill(label = job.status.displayLabel, tint = statusTint)
      Text(
        text = "Attempts: ${job.attemptCount}/${job.maxRetries}",
        style = MaterialTheme.typography.labelSmall,
        color = ZuzuInkSecondary,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(job.postTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
    Text(
      text = "Platforms: ${job.platforms.joinToString { it.displayName }}",
      style = MaterialTheme.typography.bodySmall,
      color = ZuzuInkPrimary,
    )
    Text(
      text = "Scheduled: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(job.scheduledAtMillis))}",
      style = MaterialTheme.typography.bodySmall,
      color = ZuzuTurquoise,
    )
    Text(
      text = "Idempotency Key: ${job.idempotencyKey.take(36)}",
      style = MaterialTheme.typography.labelSmall,
      color = ZuzuInkSecondary,
    )
    job.cloudTaskName?.let { task ->
      Text("Cloud Task: $task", style = MaterialTheme.typography.labelSmall, color = ZuzuGreen)
    }
    job.lastError?.let { err ->
      Spacer(modifier = Modifier.height(4.dp))
      Text(err, style = MaterialTheme.typography.bodySmall, color = ZuzuAmber)
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      if (job.isEditableOrCancellable) {
        ZuzuSecondaryActionButton(text = "Edit", onClick = onEdit, accentColor = ZuzuElectricBlue, icon = Icons.Rounded.Edit)
        ZuzuSecondaryActionButton(text = "Cancel", onClick = onCancel, accentColor = ZuzuError, icon = Icons.Rounded.Cancel)
      }
      if (job.canRetrySafely) {
        ZuzuSecondaryActionButton(text = "Retry", onClick = onRetry, accentColor = ZuzuPurple, icon = Icons.Rounded.Replay)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete job", tint = ZuzuInkSecondary)
      }
    }
  }
}

@Composable
private fun CloudBackendStatusSection(
  uiState: ApprovalSchedulerUiState,
  viewModel: ApprovalSchedulerViewModel,
  glowEnabled: Boolean,
) {
  val report = uiState.cloudStatus
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuDeepPurple, ZuzuElectricBlue),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("cloud_status_overview_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (report.overallHealth == CloudServiceHealth.Ready) Icons.Rounded.CloudDone else Icons.Rounded.CloudQueue,
            contentDescription = null,
            tint = if (report.overallHealth == CloudServiceHealth.Ready) ZuzuGreen else ZuzuAmber,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Backend Integration Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        StatusPill(
          label = report.overallHealth.displayLabel,
          tint = if (report.overallHealth == CloudServiceHealth.Ready) ZuzuGreen else ZuzuAmber,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("Auth State: ${report.authStateLabel}", style = MaterialTheme.typography.labelMedium, color = ZuzuPurple)
      Spacer(modifier = Modifier.height(4.dp))
      Text(report.summaryMessage, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)

      Spacer(modifier = Modifier.height(10.dp))
      if (uiState.isSyncingCloud) {
        ZuzuLoadingBanner(label = "Checking cloud automation subsystems…", accentColor = ZuzuPurple)
      } else {
        ZuzuGradientButton(
          text = "Verify Cloud Connection & Sync",
          onClick = viewModel::refreshCloudStatusAndAttemptSync,
          gradientColors = ZuzuHeroGradient,
          icon = Icons.Rounded.Refresh,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_verify_sync_btn"),
        )
      }
    }

    report.subsystems.forEach { sub ->
      val tint = if (sub.health == CloudServiceHealth.Ready) ZuzuGreen else ZuzuAmber
      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = ZuzuBlueTurquoiseGradient,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(sub.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
          StatusPill(label = sub.health.displayLabel, tint = tint)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(sub.detail, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
      }
    }

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = listOf(ZuzuAmber, ZuzuPurple),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Firestore Schema & Server Setup Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = listOf(
          FirestoreSchemaContract.userProjectsPath("{uid}"),
          FirestoreSchemaContract.mediaMetadataPath("{uid}"),
          FirestoreSchemaContract.approvalsPath("{uid}"),
          FirestoreSchemaContract.scheduledPostsPath("{uid}"),
          FirestoreSchemaContract.automationJobsPath("{uid}"),
        ).joinToString("\n") { "• $it" },
        style = MaterialTheme.typography.labelSmall,
        color = ZuzuInkPrimary,
      )
      if (report.setupChecklist.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ZuzuOrangeSoft,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            report.setupChecklist.forEachIndexed { idx, step ->
              Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = ZuzuInkPrimary)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EditScheduledJobDialog(
  job: ScheduledPostJob,
  onDismiss: () -> Unit,
  onSave: (List<SocialPlatform>, Long) -> Unit,
) {
  val context = LocalContext.current
  var platforms by remember { mutableStateOf(job.platforms.toSet()) }
  var ts by remember { mutableStateOf(job.scheduledAtMillis) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = { Text("Edit Scheduled Post") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(job.postTitle, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SocialPlatform.entries.forEach { p ->
            val sel = platforms.contains(p)
            ZuzuPillChip(
              label = p.displayName,
              selected = sel,
              onClick = { platforms = if (sel) platforms - p else platforms + p },
              gradientColors = ZuzuBlueTurquoiseGradient,
            )
          }
        }
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
        ) {
          Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts)))
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(platforms.toList(), ts) }) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
