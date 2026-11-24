package com.example.data.cloud

import com.example.model.ApprovalContentItem
import com.example.model.CloudBackendStatusReport
import com.example.model.CloudOperationResult
import com.example.model.MediaLibraryEntry
import com.example.model.MotionPostProject
import com.example.model.ScheduledPostJob
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class CloudAuthSession(
  val uid: String = "",
  val email: String? = null,
  val displayName: String? = null,
  val idToken: String = "",
) {
  val isAuthenticated: Boolean
    get() = uid.isNotBlank() && idToken.isNotBlank()
}

@JsonClass(generateAdapter = true)
data class CloudProjectSyncPayload(
  val uid: String,
  val collectionPath: String,
  val projectId: String,
  val title: String,
  val kind: String,
  val updatedAtMillis: Long,
)

@JsonClass(generateAdapter = true)
data class CloudMediaMetadataPayload(
  val uid: String,
  val collectionPath: String,
  val mediaId: String,
  val title: String,
  val mediaType: String,
  val mimeType: String?,
  val fileSizeBytes: Long?,
  val permissionStatus: String,
  val approvedForReuse: Boolean,
  val storageObjectPath: String,
)

@JsonClass(generateAdapter = true)
data class CloudApprovalSyncPayload(
  val uid: String,
  val collectionPath: String,
  val approvalId: String,
  val sourceId: String,
  val title: String,
  val status: String,
  val targetPlatforms: List<String>,
  val permissionVerified: Boolean,
  val scheduledAtMillis: Long?,
)

@JsonClass(generateAdapter = true)
data class CloudScheduleTaskRequest(
  val uid: String,
  val jobId: String,
  val approvalItemId: String,
  val idempotencyKey: String,
  val postTitle: String,
  val postDescription: String,
  val hashtagsCsv: String,
  val platforms: List<String>,
  val scheduledAtMillis: Long,
  val maxRetries: Int,
)

@JsonClass(generateAdapter = true)
data class CloudScheduleTaskResponse(
  val jobId: String,
  val cloudTaskName: String,
  val cloudSchedulerJobId: String?,
  val status: String,
)

@JsonClass(generateAdapter = true)
data class CloudStorageSignedUrlResponse(
  val signedUploadUrl: String,
  val gsObjectUri: String,
)

/**
 * Retrofit contract for server-side Cloud Functions / Cloud Run backend orchestration
 * connected to Firestore, Cloud Storage, Google Cloud Tasks, and Cloud Scheduler.
 */
interface CloudAutomationRetrofitApi {
  @POST("v1/sync/projects")
  suspend fun syncProject(
    @Header("Authorization") bearerToken: String,
    @Body payload: CloudProjectSyncPayload,
  ): Response<Unit>

  @POST("v1/sync/media-metadata")
  suspend fun syncMediaMetadata(
    @Header("Authorization") bearerToken: String,
    @Body payload: CloudMediaMetadataPayload,
  ): Response<Unit>

  @POST("v1/sync/approvals")
  suspend fun syncApproval(
    @Header("Authorization") bearerToken: String,
    @Body payload: CloudApprovalSyncPayload,
  ): Response<Unit>

  @POST("v1/storage/signed-upload")
  suspend fun requestSignedUploadUrl(
    @Header("Authorization") bearerToken: String,
    @Body payload: CloudMediaMetadataPayload,
  ): Response<CloudStorageSignedUrlResponse>

  @PUT
  suspend fun uploadMediaBinary(
    @Url signedUrl: String,
    @Body binaryBody: RequestBody,
  ): Response<Unit>

  @POST("v1/automation/schedules")
  suspend fun createCloudTaskSchedule(
    @Header("Authorization") bearerToken: String,
    @Body request: CloudScheduleTaskRequest,
  ): Response<CloudScheduleTaskResponse>

  @PATCH("v1/automation/schedules/{jobId}")
  suspend fun updateCloudTaskSchedule(
    @Header("Authorization") bearerToken: String,
    @Path("jobId") jobId: String,
    @Body request: CloudScheduleTaskRequest,
  ): Response<CloudScheduleTaskResponse>

  @DELETE("v1/automation/schedules/{jobId}")
  suspend fun cancelCloudTaskSchedule(
    @Header("Authorization") bearerToken: String,
    @Path("jobId") jobId: String,
  ): Response<Unit>

  @POST("v1/automation/schedules/{jobId}/retry")
  suspend fun retryCloudTaskSchedule(
    @Header("Authorization") bearerToken: String,
    @Path("jobId") jobId: String,
  ): Response<CloudScheduleTaskResponse>

  @GET("v1/automation/jobs/{jobId}")
  suspend fun getJobStatus(
    @Header("Authorization") bearerToken: String,
    @Path("jobId") jobId: String,
  ): Response<CloudScheduleTaskResponse>
}

interface CloudBackendGateway {
  val statusReport: StateFlow<CloudBackendStatusReport>
  val authSession: StateFlow<CloudAuthSession?>

  fun refreshStatus(): CloudBackendStatusReport

  suspend fun signInPromptResult(session: CloudAuthSession?): CloudOperationResult<CloudAuthSession>
  suspend fun signOut()

  suspend fun syncMotionProject(project: MotionPostProject): CloudOperationResult<Unit>
  suspend fun syncMediaMetadata(entry: MediaLibraryEntry): CloudOperationResult<String>
  suspend fun syncApprovalRecord(item: ApprovalContentItem): CloudOperationResult<Unit>

  suspend fun enqueueScheduledPost(job: ScheduledPostJob): CloudOperationResult<CloudScheduleTaskResponse>
  suspend fun updateScheduledPost(job: ScheduledPostJob): CloudOperationResult<CloudScheduleTaskResponse>
  suspend fun cancelScheduledPost(jobId: String): CloudOperationResult<Unit>
  suspend fun retryScheduledPost(jobId: String): CloudOperationResult<CloudScheduleTaskResponse>
}

/**
 * Modular Cloud Backend gateway. Validates real credentials & endpoint availability before
 * performing network/Cloud calls; returns explicit SetupRequired states when unconfigured
 * and never claims cloud synchronization succeeded without a verified server response.
 */
class DefaultCloudBackendGateway(
  private val config: CloudBackendConfig = CloudBackendConfig(),
  private val apiProvider: ((String) -> CloudAutomationRetrofitApi)? = null,
) : CloudBackendGateway {

  private val _authSession = MutableStateFlow<CloudAuthSession?>(null)
  override val authSession: StateFlow<CloudAuthSession?> = _authSession.asStateFlow()

  private val _statusReport = MutableStateFlow(
    CloudStatusInspector.buildReport(config, isUserSignedIn = false, signedInEmail = null),
  )
  override val statusReport: StateFlow<CloudBackendStatusReport> = _statusReport.asStateFlow()

  override fun refreshStatus(): CloudBackendStatusReport {
    val session = _authSession.value
    val report = CloudStatusInspector.buildReport(
      config = config,
      isUserSignedIn = session?.isAuthenticated == true,
      signedInEmail = session?.email,
    )
    _statusReport.value = report
    return report
  }

  override suspend fun signInPromptResult(
    session: CloudAuthSession?,
  ): CloudOperationResult<CloudAuthSession> {
    if (!config.hasFirebaseProject) {
      refreshStatus()
      return CloudOperationResult.SetupRequired(
        title = "Firebase Auth Setup Required",
        reason = "Firebase project configuration and Google Sign-In Web Client ID are not configured yet.",
        missingItems = config.missingRequirements(),
      )
    }
    if (session == null || !session.isAuthenticated) {
      return CloudOperationResult.Failure("Google Sign-In did not return a verified Firebase session.")
    }
    _authSession.value = session
    refreshStatus()
    return CloudOperationResult.Success(session)
  }

  override suspend fun signOut() {
    _authSession.value = null
    refreshStatus()
  }

  private fun requireReadyContext(): Result<Pair<CloudAuthSession, CloudAutomationRetrofitApi>> {
    val session = _authSession.value
    if (!config.isCloudReady || session == null || !session.isAuthenticated) {
      return Result.failure(SetupNotReadyException())
    }
    val api = apiProvider?.invoke(config.cloudAutomationBaseUrl)
      ?: return Result.failure(SetupNotReadyException())
    return Result.success(session to api)
  }

  private fun setupRequiredResult(feature: String): CloudOperationResult.SetupRequired {
    refreshStatus()
    val missing = buildList {
      addAll(config.missingRequirements())
      if (_authSession.value?.isAuthenticated != true) {
        add("Sign in with Google to authorize cloud requests for $feature.")
      }
    }.distinct()
    return CloudOperationResult.SetupRequired(
      title = "$feature Requires Cloud Setup",
      reason = "Cloud credentials or automation endpoint are unavailable. Your data remains safely saved on-device without fake sync claims.",
      missingItems = missing,
    )
  }

  override suspend fun syncMotionProject(project: MotionPostProject): CloudOperationResult<Unit> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Project Sync") }
    return runCatching {
      val (session, api) = ctx
      val resp = api.syncProject(
        bearerToken = "Bearer ${session.idToken}",
        payload = CloudProjectSyncPayload(
          uid = session.uid,
          collectionPath = FirestoreSchemaContract.userProjectsPath(session.uid),
          projectId = project.id,
          title = project.title,
          kind = "motion_post_${project.templateCategory.name}",
          updatedAtMillis = project.updatedAtMillis,
        ),
      )
      if (resp.isSuccessful) {
        CloudOperationResult.Success(Unit)
      } else {
        CloudOperationResult.Failure("Cloud project sync failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to sync project to Firestore.")
    }
  }

  override suspend fun syncMediaMetadata(entry: MediaLibraryEntry): CloudOperationResult<String> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Cloud Storage & Media Sync") }
    return runCatching {
      val (session, api) = ctx
      val path = "users/${session.uid}/media/${entry.id}"
      val resp = api.syncMediaMetadata(
        bearerToken = "Bearer ${session.idToken}",
        payload = CloudMediaMetadataPayload(
          uid = session.uid,
          collectionPath = FirestoreSchemaContract.mediaMetadataPath(session.uid),
          mediaId = entry.id,
          title = entry.title,
          mediaType = entry.mediaType.name,
          mimeType = entry.mimeType,
          fileSizeBytes = entry.fileSizeBytes,
          permissionStatus = entry.permission.status.name,
          approvedForReuse = entry.permission.approvedForReuse,
          storageObjectPath = path,
        ),
      )
      if (resp.isSuccessful) {
        CloudOperationResult.Success("gs://${config.cloudStorageBucket}/$path")
      } else {
        CloudOperationResult.Failure("Media metadata sync failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to upload media metadata.")
    }
  }

  override suspend fun syncApprovalRecord(item: ApprovalContentItem): CloudOperationResult<Unit> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Approval Record Sync") }
    return runCatching {
      val (session, api) = ctx
      val resp = api.syncApproval(
        bearerToken = "Bearer ${session.idToken}",
        payload = CloudApprovalSyncPayload(
          uid = session.uid,
          collectionPath = FirestoreSchemaContract.approvalsPath(session.uid),
          approvalId = item.id,
          sourceId = item.sourceId,
          title = item.title,
          status = item.status.name,
          targetPlatforms = item.targetPlatforms.map { it.id },
          permissionVerified = item.isPermissionValidForApproval,
          scheduledAtMillis = item.scheduledAtMillis,
        ),
      )
      if (resp.isSuccessful) {
        CloudOperationResult.Success(Unit)
      } else {
        CloudOperationResult.Failure("Approval record sync failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to sync approval record.")
    }
  }

  override suspend fun enqueueScheduledPost(
    job: ScheduledPostJob,
  ): CloudOperationResult<CloudScheduleTaskResponse> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Cloud Tasks Scheduling") }
    return runCatching {
      val (session, api) = ctx
      val resp = api.createCloudTaskSchedule(
        bearerToken = "Bearer ${session.idToken}",
        request = job.toCloudRequest(session.uid),
      )
      val body = resp.body()
      if (resp.isSuccessful && body != null) {
        CloudOperationResult.Success(body)
      } else {
        CloudOperationResult.Failure("Cloud Task scheduling failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to enqueue Cloud Task.")
    }
  }

  override suspend fun updateScheduledPost(
    job: ScheduledPostJob,
  ): CloudOperationResult<CloudScheduleTaskResponse> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Cloud Tasks Update") }
    return runCatching {
      val (session, api) = ctx
      val resp = api.updateCloudTaskSchedule(
        bearerToken = "Bearer ${session.idToken}",
        jobId = job.id,
        request = job.toCloudRequest(session.uid),
      )
      val body = resp.body()
      if (resp.isSuccessful && body != null) {
        CloudOperationResult.Success(body)
      } else {
        CloudOperationResult.Failure("Cloud Task update failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to update Cloud Task.")
    }
  }

  override suspend fun cancelScheduledPost(jobId: String): CloudOperationResult<Unit> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Cloud Task Cancellation") }
    return runCatching {
      val (session, api) = ctx
      val resp = api.cancelCloudTaskSchedule(bearerToken = "Bearer ${session.idToken}", jobId = jobId)
      if (resp.isSuccessful) {
        CloudOperationResult.Success(Unit)
      } else {
        CloudOperationResult.Failure("Cloud Task cancellation failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to cancel Cloud Task.")
    }
  }

  override suspend fun retryScheduledPost(
    jobId: String,
  ): CloudOperationResult<CloudScheduleTaskResponse> {
    val ctx = requireReadyContext().getOrElse { return setupRequiredResult("Cloud Task Retry") }
    return runCatching {
      val (session, api) = ctx
      val resp = api.retryCloudTaskSchedule(bearerToken = "Bearer ${session.idToken}", jobId = jobId)
      val body = resp.body()
      if (resp.isSuccessful && body != null) {
        CloudOperationResult.Success(body)
      } else {
        CloudOperationResult.Failure("Cloud Task retry failed (${resp.code()}).")
      }
    }.getOrElse { err ->
      CloudOperationResult.Failure(err.message ?: "Unable to retry Cloud Task.")
    }
  }

  private fun ScheduledPostJob.toCloudRequest(uid: String) = CloudScheduleTaskRequest(
    uid = uid,
    jobId = id,
    approvalItemId = approvalItemId,
    idempotencyKey = idempotencyKey,
    postTitle = postTitle,
    postDescription = postDescription,
    hashtagsCsv = hashtagsCsv,
    platforms = platforms.map { it.id },
    scheduledAtMillis = scheduledAtMillis,
    maxRetries = maxRetries,
  )

  private class SetupNotReadyException : Exception()
}
