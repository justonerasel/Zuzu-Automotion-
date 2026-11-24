package com.example.data.phase78

import com.example.model.ApprovalContentItem
import com.example.model.MotionPostProject
import com.example.model.ScheduledPostJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface Phase78Repository {
  val motionProjects: Flow<List<MotionPostProject>>
  val approvalItems: Flow<List<ApprovalContentItem>>
  val scheduledJobs: Flow<List<ScheduledPostJob>>

  suspend fun getMotionProject(id: String): MotionPostProject?
  suspend fun saveMotionProject(project: MotionPostProject): MotionPostProject
  suspend fun deleteMotionProject(id: String)

  suspend fun getApprovalItem(id: String): ApprovalContentItem?
  suspend fun saveApprovalItem(item: ApprovalContentItem): ApprovalContentItem
  suspend fun deleteApprovalItem(id: String)

  suspend fun getScheduledJob(id: String): ScheduledPostJob?
  suspend fun findScheduledJobByIdempotencyKey(key: String): ScheduledPostJob?
  suspend fun saveScheduledJob(job: ScheduledPostJob): ScheduledPostJob
  suspend fun deleteScheduledJob(id: String)
}

class RoomPhase78Repository(
  private val dao: Phase78Dao,
) : Phase78Repository {

  override val motionProjects: Flow<List<MotionPostProject>> =
    dao.observeMotionProjects().map { rows -> rows.mapNotNull { it.toDomain() } }

  override val approvalItems: Flow<List<ApprovalContentItem>> =
    dao.observeApprovalItems().map { rows -> rows.mapNotNull { it.toDomain() } }

  override val scheduledJobs: Flow<List<ScheduledPostJob>> =
    dao.observeScheduledJobs().map { rows -> rows.mapNotNull { it.toDomain() } }

  override suspend fun getMotionProject(id: String): MotionPostProject? =
    dao.getMotionProjectById(id)?.toDomain()

  override suspend fun saveMotionProject(project: MotionPostProject): MotionPostProject {
    val stamped = project.copy(updatedAtMillis = System.currentTimeMillis())
    dao.upsertMotionProject(stamped.toEntity())
    return stamped
  }

  override suspend fun deleteMotionProject(id: String) {
    dao.deleteMotionProjectById(id)
  }

  override suspend fun getApprovalItem(id: String): ApprovalContentItem? =
    dao.getApprovalItemById(id)?.toDomain()

  override suspend fun saveApprovalItem(item: ApprovalContentItem): ApprovalContentItem {
    val stamped = item.copy(updatedAtMillis = System.currentTimeMillis())
    dao.upsertApprovalItem(stamped.toEntity())
    return stamped
  }

  override suspend fun deleteApprovalItem(id: String) {
    dao.deleteApprovalItemById(id)
  }

  override suspend fun getScheduledJob(id: String): ScheduledPostJob? =
    dao.getScheduledJobById(id)?.toDomain()

  override suspend fun findScheduledJobByIdempotencyKey(key: String): ScheduledPostJob? =
    dao.getScheduledJobByIdempotencyKey(key)?.toDomain()

  override suspend fun saveScheduledJob(job: ScheduledPostJob): ScheduledPostJob {
    val stamped = job.copy(updatedAtMillis = System.currentTimeMillis())
    dao.upsertScheduledJob(stamped.toEntity())
    return stamped
  }

  override suspend fun deleteScheduledJob(id: String) {
    dao.deleteScheduledJobById(id)
  }
}

class InMemoryPhase78Repository : Phase78Repository {
  private val _motions = MutableStateFlow<List<MotionPostProject>>(emptyList())
  private val _approvals = MutableStateFlow<List<ApprovalContentItem>>(emptyList())
  private val _jobs = MutableStateFlow<List<ScheduledPostJob>>(emptyList())

  override val motionProjects: Flow<List<MotionPostProject>> = _motions.asStateFlow()
  override val approvalItems: Flow<List<ApprovalContentItem>> = _approvals.asStateFlow()
  override val scheduledJobs: Flow<List<ScheduledPostJob>> = _jobs.asStateFlow()

  override suspend fun getMotionProject(id: String): MotionPostProject? =
    _motions.value.firstOrNull { it.id == id }

  override suspend fun saveMotionProject(project: MotionPostProject): MotionPostProject {
    val stamped = project.copy(updatedAtMillis = System.currentTimeMillis())
    _motions.update { listOf(stamped) + it.filterNot { m -> m.id == stamped.id } }
    return stamped
  }

  override suspend fun deleteMotionProject(id: String) {
    _motions.update { it.filterNot { m -> m.id == id } }
  }

  override suspend fun getApprovalItem(id: String): ApprovalContentItem? =
    _approvals.value.firstOrNull { it.id == id }

  override suspend fun saveApprovalItem(item: ApprovalContentItem): ApprovalContentItem {
    val stamped = item.copy(updatedAtMillis = System.currentTimeMillis())
    _approvals.update { listOf(stamped) + it.filterNot { a -> a.id == stamped.id } }
    return stamped
  }

  override suspend fun deleteApprovalItem(id: String) {
    _approvals.update { it.filterNot { a -> a.id == id } }
  }

  override suspend fun getScheduledJob(id: String): ScheduledPostJob? =
    _jobs.value.firstOrNull { it.id == id }

  override suspend fun findScheduledJobByIdempotencyKey(key: String): ScheduledPostJob? =
    _jobs.value.firstOrNull { it.idempotencyKey == key }

  override suspend fun saveScheduledJob(job: ScheduledPostJob): ScheduledPostJob {
    val stamped = job.copy(updatedAtMillis = System.currentTimeMillis())
    _jobs.update { (it.filterNot { j -> j.id == stamped.id } + stamped).sortedBy { j -> j.scheduledAtMillis } }
    return stamped
  }

  override suspend fun deleteScheduledJob(id: String) {
    _jobs.update { it.filterNot { j -> j.id == id } }
  }
}
