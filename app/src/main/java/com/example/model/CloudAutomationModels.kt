package com.example.model

import com.squareup.moshi.JsonClass

enum class ApprovalSourceKind(val displayLabel: String) {
  VideoExport("Video Editor MP4"),
  MotionPost("Motion Studio Post"),
}

enum class ApprovalStatus(val displayLabel: String) {
  PendingReview("Pending Review"),
  Approved("Approved"),
  Rejected("Rejected"),
  Draft("Draft"),
}

enum class CloudSyncState(val displayLabel: String) {
  LocalOnly("Saved Locally"),
  SyncPendingSetupRequired("Cloud Setup Required"),
  Synced("Synced to Cloud"),
  SyncError("Sync Attention"),
}

@JsonClass(generateAdapter = true)
data class ApprovalContentItem(
  val id: String,
  val sourceKind: ApprovalSourceKind,
  val sourceId: String,
  val title: String,
  val description: String = "",
  val hashtagsCsv: String = "",
  val mediaUriString: String? = null,
  val mediaFilePath: String? = null,
  val sourceMediaEntryId: String? = null,
  val aspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
  val targetPlatforms: List<SocialPlatform> = listOf(SocialPlatform.YouTube),
  val scheduledAtMillis: Long? = null,
  val permissionOrigin: ContentOrigin = ContentOrigin.MyUploads,
  val permissionStatus: PermissionStatus = PermissionStatus.OriginalContent,
  val permissionOwner: String = "Zuzu Creator",
  val permissionEvidence: String = "Created inside Zuzu Studio",
  val permissionApprovedForReuse: Boolean = true,
  val status: ApprovalStatus = ApprovalStatus.PendingReview,
  val rejectionReason: String = "",
  val cloudSyncState: CloudSyncState = CloudSyncState.SyncPendingSetupRequired,
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  fun permissionRecord(): PermissionRecord = PermissionRecord(
    status = permissionStatus,
    contentOwner = permissionOwner,
    evidenceDescription = permissionEvidence,
    approvedForReuse = permissionApprovedForReuse,
  )

  fun permissionBlockingReason(): String? {
    if (!permissionApprovedForReuse) {
      return "External or linked media must be marked approved for reuse before content approval."
    }
    return PermissionPolicy.approvalBlockingReason(
      origin = permissionOrigin,
      permission = permissionRecord(),
    )
  }

  val isPermissionValidForApproval: Boolean
    get() = permissionBlockingReason() == null
}

enum class ScheduledJobStatus(val displayLabel: String) {
  QueuedLocalSetupRequired("Setup Required (Local Queue)"),
  ScheduledCloudPending("Scheduled in Cloud"),
  Running("Processing"),
  Retrying("Retry Scheduled"),
  Completed("Completed"),
  Cancelled("Cancelled"),
  Failed("Failed"),
}

@JsonClass(generateAdapter = true)
data class ScheduledPostJob(
  val id: String,
  val approvalItemId: String,
  val postTitle: String,
  val postDescription: String,
  val hashtagsCsv: String,
  val platforms: List<SocialPlatform>,
  val scheduledAtMillis: Long,
  val idempotencyKey: String,
  val cloudTaskName: String? = null,
  val cloudSchedulerJobId: String? = null,
  val status: ScheduledJobStatus = ScheduledJobStatus.QueuedLocalSetupRequired,
  val attemptCount: Int = 0,
  val maxRetries: Int = 3,
  val lastError: String? = null,
  val nextRetryAtMillis: Long? = null,
  val cloudSyncState: CloudSyncState = CloudSyncState.SyncPendingSetupRequired,
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  val isEditableOrCancellable: Boolean
    get() = status == ScheduledJobStatus.QueuedLocalSetupRequired ||
      status == ScheduledJobStatus.ScheduledCloudPending ||
      status == ScheduledJobStatus.Retrying ||
      status == ScheduledJobStatus.Failed

  val canRetrySafely: Boolean
    get() = (status == ScheduledJobStatus.Failed || status == ScheduledJobStatus.Retrying) &&
      attemptCount < maxRetries
}

enum class CloudServiceHealth(val displayLabel: String) {
  NotConfigured("Not Configured"),
  SetupRequired("Setup Required"),
  Connecting("Connecting"),
  Ready("Connected"),
  Error("Connection Error"),
}

data class CloudSubsystemStatus(
  val id: String,
  val name: String,
  val detail: String,
  val health: CloudServiceHealth,
  val missingRequirements: List<String> = emptyList(),
)

data class CloudBackendStatusReport(
  val overallHealth: CloudServiceHealth = CloudServiceHealth.SetupRequired,
  val firebaseProjectId: String? = null,
  val firestoreDatabaseId: String? = null,
  val storageBucket: String? = null,
  val automationEndpoint: String? = null,
  val authStateLabel: String = "Signed Out (Authentication-Ready)",
  val summaryMessage: String = "Cloud credentials are not configured yet. Local drafts, approvals, and queue records work offline without simulating cloud sync.",
  val subsystems: List<CloudSubsystemStatus> = emptyList(),
  val setupChecklist: List<String> = emptyList(),
  val lastCheckedAtMillis: Long = System.currentTimeMillis(),
)

sealed interface CloudOperationResult<out T> {
  data class Success<T>(val data: T) : CloudOperationResult<T>
  data class SetupRequired(
    val title: String,
    val reason: String,
    val missingItems: List<String>,
  ) : CloudOperationResult<Nothing>
  data class Failure(val message: String) : CloudOperationResult<Nothing>
}
