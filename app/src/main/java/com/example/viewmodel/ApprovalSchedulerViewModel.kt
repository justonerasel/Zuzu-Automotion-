package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudBackendGateway
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.phase78.Phase78Repository
import com.example.data.source.ContentSourceRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.ApprovalContentItem
import com.example.model.ApprovalSourceKind
import com.example.model.ApprovalStatus
import com.example.model.CloudBackendStatusReport
import com.example.model.CloudOperationResult
import com.example.model.CloudSyncState
import com.example.model.ContentOrigin
import com.example.model.ExportedVideoRecord
import com.example.model.MediaLibraryEntry
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.ScheduledJobStatus
import com.example.model.ScheduledPostJob
import com.example.model.SocialPlatform
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ApprovalFilterTab(val displayLabel: String) {
  PendingReviews("Pending"),
  Approved("Approved"),
  Rejected("Rejected"),
  Drafts("Drafts"),
}

enum class AutomationScreenTab(val displayLabel: String) {
  Scheduler("Scheduler"),
  CloudStatus("Cloud Automation"),
}

data class ApprovalSchedulerUiState(
  val selectedApprovalTab: ApprovalFilterTab = ApprovalFilterTab.PendingReviews,
  val selectedAutomationTab: AutomationScreenTab = AutomationScreenTab.Scheduler,
  val approvalItems: List<ApprovalContentItem> = emptyList(),
  val scheduledJobs: List<ScheduledPostJob> = emptyList(),
  val cloudStatus: CloudBackendStatusReport = CloudBackendStatusReport(),
  val isSyncingCloud: Boolean = false,
  val isProcessingSchedule: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class ApprovalSchedulerViewModel(
  private val repository: Phase78Repository = InMemoryPhase78Repository(),
  private val contentSourceRepository: ContentSourceRepository = InMemoryContentSourceRepository(),
  private val cloudGateway: CloudBackendGateway = DefaultCloudBackendGateway(),
  private val clockMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

  private val selectedApprovalTab = MutableStateFlow(ApprovalFilterTab.PendingReviews)
  private val selectedAutomationTab = MutableStateFlow(AutomationScreenTab.Scheduler)
  private val isSyncingCloud = MutableStateFlow(false)
  private val isProcessingSchedule = MutableStateFlow(false)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private val tabAndProgress = combine(
    selectedApprovalTab,
    selectedAutomationTab,
    isSyncingCloud,
    isProcessingSchedule,
  ) { aprTab, autoTab, syncing, scheduling ->
    TabProgressSnapshot(aprTab, autoTab, syncing, scheduling)
  }

  private val messages = combine(feedbackMessage, errorMessage) { f, e -> f to e }

  val uiState: StateFlow<ApprovalSchedulerUiState> = combine(
    repository.approvalItems,
    repository.scheduledJobs,
    cloudGateway.statusReport,
    tabAndProgress,
    messages,
  ) { approvals, jobs, cloudReport, snap, (feedback, error) ->
    ApprovalSchedulerUiState(
      selectedApprovalTab = snap.approvalTab,
      selectedAutomationTab = snap.automationTab,
      approvalItems = approvals,
      scheduledJobs = jobs,
      cloudStatus = cloudReport,
      isSyncingCloud = snap.isSyncing,
      isProcessingSchedule = snap.isScheduling,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ApprovalSchedulerUiState(cloudStatus = cloudGateway.statusReport.value),
  )

  fun selectApprovalTab(tab: ApprovalFilterTab) {
    selectedApprovalTab.value = tab
    clearMessages()
  }

  fun selectAutomationTab(tab: AutomationScreenTab) {
    selectedAutomationTab.value = tab
    clearMessages()
  }

  fun submitExportedVideoForApproval(
    exportRecord: ExportedVideoRecord,
    linkedMediaEntry: MediaLibraryEntry? = null,
  ) {
    viewModelScope.launch {
      val resolvedEntry = linkedMediaEntry
        ?: exportRecord.savedMediaEntryId?.let { id ->
          contentSourceRepository.mediaEntries.first().firstOrNull { it.id == id }
        }
      val origin = resolvedEntry?.origin ?: ContentOrigin.MyUploads
      val permission = resolvedEntry?.permission ?: PermissionRecord(
        status = PermissionStatus.OriginalContent,
        contentOwner = "Zuzu Creator",
        evidenceDescription = "Rendered in Zuzu Video Editor",
        approvedForReuse = true,
      )
      val item = ApprovalContentItem(
        id = "apr_video_${exportRecord.id}",
        sourceKind = ApprovalSourceKind.VideoExport,
        sourceId = exportRecord.id,
        title = exportRecord.fileName.substringBeforeLast('.'),
        description = "Exported ${exportRecord.resolutionLabel} MP4 (${exportRecord.aspectRatio.label})",
        hashtagsCsv = "#ZuzuAI #Shorts #Creator",
        mediaUriString = exportRecord.fileUriString,
        mediaFilePath = exportRecord.filePath,
        sourceMediaEntryId = resolvedEntry?.id,
        aspectRatio = exportRecord.aspectRatio,
        targetPlatforms = listOf(SocialPlatform.YouTube),
        permissionOrigin = origin,
        permissionStatus = permission.status,
        permissionOwner = permission.contentOwner,
        permissionEvidence = permission.evidenceDescription.ifBlank { permission.licenseReference },
        permissionApprovedForReuse = permission.approvedForReuse,
        status = ApprovalStatus.PendingReview,
      )
      repository.saveApprovalItem(item)
      feedbackMessage.value = "Submitted '${item.title}' to Approval Center."
      errorMessage.value = null
    }
  }

  fun submitExternalItemForApproval(item: ApprovalContentItem) {
    viewModelScope.launch {
      repository.saveApprovalItem(item)
      feedbackMessage.value = "Submitted '${item.title}' for approval review."
    }
  }

  fun updateApprovalItemDetails(
    itemId: String,
    title: String,
    description: String,
    hashtagsCsv: String,
    targetPlatforms: List<SocialPlatform>,
    scheduledAtMillis: Long?,
  ) {
    if (title.isBlank()) {
      errorMessage.value = "Title cannot be empty."
      return
    }
    viewModelScope.launch {
      val existing = repository.getApprovalItem(itemId) ?: return@launch
      repository.saveApprovalItem(
        existing.copy(
          title = title.trim(),
          description = description.trim(),
          hashtagsCsv = hashtagsCsv.trim(),
          targetPlatforms = targetPlatforms.distinct(),
          scheduledAtMillis = scheduledAtMillis,
        ),
      )
      feedbackMessage.value = "Saved metadata edits for '${title.trim()}'."
      errorMessage.value = null
    }
  }

  fun updateItemPermissionEvidence(
    itemId: String,
    status: PermissionStatus,
    owner: String,
    evidence: String,
    approvedForReuse: Boolean,
  ) {
    viewModelScope.launch {
      val existing = repository.getApprovalItem(itemId) ?: return@launch
      repository.saveApprovalItem(
        existing.copy(
          permissionStatus = status,
          permissionOwner = owner.trim(),
          permissionEvidence = evidence.trim(),
          permissionApprovedForReuse = approvedForReuse,
        ),
      )
      feedbackMessage.value = "Updated permission record for '${existing.title}'."
      errorMessage.value = null
    }
  }

  fun saveApprovalDraft(itemId: String) {
    viewModelScope.launch {
      val existing = repository.getApprovalItem(itemId) ?: return@launch
      val updated = repository.saveApprovalItem(existing.copy(status = ApprovalStatus.Draft))
      feedbackMessage.value = "Saved '${updated.title}' to Approval Drafts."
      errorMessage.value = null
    }
  }

  /**
   * Explicitly approves content ONLY if external/internal permission validation passes.
   */
  fun approveItem(itemId: String) {
    viewModelScope.launch {
      val existing = repository.getApprovalItem(itemId) ?: run {
        errorMessage.value = "Approval item not found."
        return@launch
      }
      if (existing.targetPlatforms.isEmpty()) {
        errorMessage.value = "Select at least one target platform before approving."
        return@launch
      }
      val blockingReason = existing.permissionBlockingReason()
      if (blockingReason != null) {
        errorMessage.value = "Permission Check Blocked Approval: $blockingReason"
        return@launch
      }

      val approvedLocal = existing.copy(
        status = ApprovalStatus.Approved,
        rejectionReason = "",
      )
      val cloudRes = cloudGateway.syncApprovalRecord(approvedLocal)
      val syncedState = when (cloudRes) {
        is CloudOperationResult.Success -> CloudSyncState.Synced
        is CloudOperationResult.SetupRequired -> CloudSyncState.SyncPendingSetupRequired
        is CloudOperationResult.Failure -> CloudSyncState.SyncError
      }
      val persisted = repository.saveApprovalItem(approvedLocal.copy(cloudSyncState = syncedState))
      feedbackMessage.value = when (cloudRes) {
        is CloudOperationResult.Success ->
          "Approved '${persisted.title}' and synced to Firestore."
        is CloudOperationResult.SetupRequired ->
          "Approved '${persisted.title}' locally (Cloud sync status: Setup Required)."
        is CloudOperationResult.Failure ->
          "Approved '${persisted.title}' locally; cloud sync error: ${cloudRes.message}"
      }
      errorMessage.value = null
    }
  }

  fun rejectItem(itemId: String, reason: String) {
    viewModelScope.launch {
      val existing = repository.getApprovalItem(itemId) ?: return@launch
      val cleanReason = reason.trim().ifEmpty { "Rejected during creator review" }
      val rejectedLocal = existing.copy(
        status = ApprovalStatus.Rejected,
        rejectionReason = cleanReason,
      )
      val cloudRes = cloudGateway.syncApprovalRecord(rejectedLocal)
      val syncState = if (cloudRes is CloudOperationResult.Success) {
        CloudSyncState.Synced
      } else {
        CloudSyncState.SyncPendingSetupRequired
      }
      repository.saveApprovalItem(rejectedLocal.copy(cloudSyncState = syncState))
      feedbackMessage.value = "Rejected '${existing.title}'."
      errorMessage.value = null
    }
  }

  fun deleteApprovalItem(itemId: String) {
    viewModelScope.launch {
      repository.deleteApprovalItem(itemId)
      feedbackMessage.value = "Removed approval item."
    }
  }

  /**
   * Schedules an explicitly Approved post with duplicate-publishing idempotency enforcement.
   * Never publishes or claims Cloud Tasks activation if cloud credentials are missing.
   */
  fun scheduleApprovedPost(
    approvalItemId: String,
    platforms: List<SocialPlatform>,
    scheduledAtMillis: Long,
  ) {
    viewModelScope.launch {
      val item = repository.getApprovalItem(approvalItemId) ?: run {
        errorMessage.value = "Select an approved item before scheduling."
        return@launch
      }
      if (item.status != ApprovalStatus.Approved) {
        errorMessage.value = "Explicit user approval is required before scheduling '${item.title}'."
        return@launch
      }
      val permissionReason = item.permissionBlockingReason()
      if (permissionReason != null) {
        errorMessage.value = "Cannot schedule: $permissionReason"
        return@launch
      }
      val distinctPlatforms = platforms.distinct()
      if (distinctPlatforms.isEmpty()) {
        errorMessage.value = "Select at least one connected platform for scheduling."
        return@launch
      }
      if (scheduledAtMillis <= clockMs()) {
        errorMessage.value = "Choose a future date and time for scheduled publishing."
        return@launch
      }

      val sortedPlatformIds = distinctPlatforms.sortedBy { it.id }.joinToString("-") { it.id }
      val idempotencyKey = "zuzu_${item.id}_${sortedPlatformIds}_$scheduledAtMillis"
      val duplicate = repository.findScheduledJobByIdempotencyKey(idempotencyKey)
      if (duplicate != null && duplicate.status != ScheduledJobStatus.Cancelled) {
        errorMessage.value = "Duplicate publish blocked: identical schedule already exists (${duplicate.status.displayLabel})."
        return@launch
      }

      isProcessingSchedule.value = true
      val baseJob = ScheduledPostJob(
        id = UUID.randomUUID().toString(),
        approvalItemId = item.id,
        postTitle = item.title,
        postDescription = item.description,
        hashtagsCsv = item.hashtagsCsv,
        platforms = distinctPlatforms,
        scheduledAtMillis = scheduledAtMillis,
        idempotencyKey = idempotencyKey,
        status = ScheduledJobStatus.QueuedLocalSetupRequired,
        attemptCount = 0,
        maxRetries = 3,
      )

      when (val result = cloudGateway.enqueueScheduledPost(baseJob)) {
        is CloudOperationResult.Success -> {
          val saved = repository.saveScheduledJob(
            baseJob.copy(
              cloudTaskName = result.data.cloudTaskName,
              cloudSchedulerJobId = result.data.cloudSchedulerJobId,
              status = ScheduledJobStatus.ScheduledCloudPending,
              cloudSyncState = CloudSyncState.Synced,
            ),
          )
          feedbackMessage.value = "Scheduled '${saved.postTitle}' in Cloud Tasks (${result.data.cloudTaskName})."
          errorMessage.value = null
        }
        is CloudOperationResult.SetupRequired -> {
          repository.saveScheduledJob(
            baseJob.copy(
              status = ScheduledJobStatus.QueuedLocalSetupRequired,
              cloudSyncState = CloudSyncState.SyncPendingSetupRequired,
              lastError = result.reason,
            ),
          )
          feedbackMessage.value = "Saved schedule locally. Cloud Tasks dispatch requires cloud credentials."
          errorMessage.value = null
        }
        is CloudOperationResult.Failure -> {
          repository.saveScheduledJob(
            baseJob.copy(
              status = ScheduledJobStatus.Failed,
              cloudSyncState = CloudSyncState.SyncError,
              lastError = result.message,
              attemptCount = 1,
              nextRetryAtMillis = clockMs() + 5 * 60_000L,
            ),
          )
          errorMessage.value = "Schedule saved with error: ${result.message}"
        }
      }
      isProcessingSchedule.value = false
    }
  }

  fun editScheduledJob(
    jobId: String,
    newPlatforms: List<SocialPlatform>,
    newScheduledAtMillis: Long,
  ) {
    viewModelScope.launch {
      val existing = repository.getScheduledJob(jobId) ?: return@launch
      if (!existing.isEditableOrCancellable) {
        errorMessage.value = "Completed or cancelled jobs cannot be edited."
        return@launch
      }
      if (newPlatforms.isEmpty()) {
        errorMessage.value = "Keep at least one platform selected."
        return@launch
      }
      if (newScheduledAtMillis <= clockMs()) {
        errorMessage.value = "Updated schedule must be in the future."
        return@launch
      }

      val distinct = newPlatforms.distinct()
      val newKey = "zuzu_${existing.approvalItemId}_${distinct.sortedBy { it.id }.joinToString("-") { it.id }}_$newScheduledAtMillis"
      val duplicate = repository.findScheduledJobByIdempotencyKey(newKey)
      if (duplicate != null && duplicate.id != existing.id && duplicate.status != ScheduledJobStatus.Cancelled) {
        errorMessage.value = "Duplicate schedule prevented: another active job matches this target."
        return@launch
      }

      val updated = existing.copy(
        platforms = distinct,
        scheduledAtMillis = newScheduledAtMillis,
        idempotencyKey = newKey,
      )
      when (val cloudRes = cloudGateway.updateScheduledPost(updated)) {
        is CloudOperationResult.Success -> {
          repository.saveScheduledJob(
            updated.copy(
              cloudTaskName = cloudRes.data.cloudTaskName,
              status = ScheduledJobStatus.ScheduledCloudPending,
              cloudSyncState = CloudSyncState.Synced,
              lastError = null,
            ),
          )
          feedbackMessage.value = "Updated Cloud Tasks schedule for '${updated.postTitle}'."
        }
        is CloudOperationResult.SetupRequired -> {
          repository.saveScheduledJob(
            updated.copy(
              status = ScheduledJobStatus.QueuedLocalSetupRequired,
              cloudSyncState = CloudSyncState.SyncPendingSetupRequired,
            ),
          )
          feedbackMessage.value = "Updated local schedule (Cloud Tasks setup still required)."
        }
        is CloudOperationResult.Failure -> {
          repository.saveScheduledJob(updated.copy(lastError = cloudRes.message))
          errorMessage.value = cloudRes.message
        }
      }
    }
  }

  fun cancelScheduledJob(jobId: String) {
    viewModelScope.launch {
      val existing = repository.getScheduledJob(jobId) ?: return@launch
      val cloudRes = cloudGateway.cancelScheduledPost(jobId)
      val nextSync = if (cloudRes is CloudOperationResult.Success) CloudSyncState.Synced else existing.cloudSyncState
      repository.saveScheduledJob(
        existing.copy(
          status = ScheduledJobStatus.Cancelled,
          cloudSyncState = nextSync,
        ),
      )
      feedbackMessage.value = "Cancelled scheduled job '${existing.postTitle}'."
      errorMessage.value = null
    }
  }

  fun retryScheduledJob(jobId: String) {
    viewModelScope.launch {
      val existing = repository.getScheduledJob(jobId) ?: return@launch
      if (existing.attemptCount >= existing.maxRetries) {
        errorMessage.value = "Maximum safe retry limit (${existing.maxRetries}) reached for '${existing.postTitle}'."
        return@launch
      }
      val nextAttempt = existing.attemptCount + 1
      when (val res = cloudGateway.retryScheduledPost(jobId)) {
        is CloudOperationResult.Success -> {
          repository.saveScheduledJob(
            existing.copy(
              status = ScheduledJobStatus.ScheduledCloudPending,
              attemptCount = nextAttempt,
              lastError = null,
              nextRetryAtMillis = null,
              cloudSyncState = CloudSyncState.Synced,
            ),
          )
          feedbackMessage.value = "Safely re-enqueued '${existing.postTitle}' (attempt $nextAttempt/${existing.maxRetries})."
        }
        is CloudOperationResult.SetupRequired -> {
          errorMessage.value = res.reason
        }
        is CloudOperationResult.Failure -> {
          repository.saveScheduledJob(
            existing.copy(
              status = ScheduledJobStatus.Failed,
              attemptCount = nextAttempt,
              lastError = res.message,
              nextRetryAtMillis = clockMs() + (nextAttempt * 5 * 60_000L),
            ),
          )
          errorMessage.value = "Retry failed: ${res.message}"
        }
      }
    }
  }

  fun markJobFailedForRetryTest(jobId: String, errorReason: String) {
    viewModelScope.launch {
      val existing = repository.getScheduledJob(jobId) ?: return@launch
      repository.saveScheduledJob(
        existing.copy(
          status = ScheduledJobStatus.Failed,
          lastError = errorReason,
          attemptCount = existing.attemptCount.coerceAtLeast(1),
          nextRetryAtMillis = clockMs() + 300_000L,
        ),
      )
    }
  }

  fun deleteScheduledJob(jobId: String) {
    viewModelScope.launch {
      repository.deleteScheduledJob(jobId)
      feedbackMessage.value = "Removed scheduled job record."
    }
  }

  fun refreshCloudStatusAndAttemptSync() {
    viewModelScope.launch {
      isSyncingCloud.value = true
      clearMessages()
      val report = cloudGateway.refreshStatus()
      val latestApproval = repository.approvalItems.first().firstOrNull()
      if (latestApproval != null) {
        when (val res = cloudGateway.syncApprovalRecord(latestApproval)) {
          is CloudOperationResult.Success -> {
            feedbackMessage.value = "Cloud synchronization verified for '${report.firebaseProjectId ?: "project"}'."
          }
          is CloudOperationResult.SetupRequired -> {
            feedbackMessage.value = null
            errorMessage.value = "${res.title}: ${res.reason}"
          }
          is CloudOperationResult.Failure -> {
            errorMessage.value = res.message
          }
        }
      } else {
        feedbackMessage.value = report.summaryMessage
      }
      isSyncingCloud.value = false
    }
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private data class TabProgressSnapshot(
    val approvalTab: ApprovalFilterTab,
    val automationTab: AutomationScreenTab,
    val isSyncing: Boolean,
    val isScheduling: Boolean,
  )
}
