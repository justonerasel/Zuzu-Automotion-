package com.example.data.phase78

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ApprovalContentItem
import com.example.model.MotionPostProject
import com.example.model.ScheduledPostJob
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "motion_post_projects")
data class MotionPostEntity(
  @PrimaryKey val id: String,
  val title: String,
  val templateCategory: String,
  val payloadJson: String,
  val updatedAtMillis: Long,
)

@Entity(tableName = "approval_items")
data class ApprovalItemEntity(
  @PrimaryKey val id: String,
  val sourceId: String,
  val title: String,
  val status: String,
  val payloadJson: String,
  val updatedAtMillis: Long,
)

@Entity(tableName = "scheduled_post_jobs")
data class ScheduledPostJobEntity(
  @PrimaryKey val id: String,
  val approvalItemId: String,
  val idempotencyKey: String,
  val status: String,
  val scheduledAtMillis: Long,
  val payloadJson: String,
  val updatedAtMillis: Long,
)

object Phase78Codec {
  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val motionAdapter = moshi.adapter(MotionPostProject::class.java)
  private val approvalAdapter = moshi.adapter(ApprovalContentItem::class.java)
  private val jobAdapter = moshi.adapter(ScheduledPostJob::class.java)

  fun encodeMotion(project: MotionPostProject): String = motionAdapter.toJson(project)
  fun decodeMotion(json: String): MotionPostProject? = runCatching { motionAdapter.fromJson(json) }.getOrNull()

  fun encodeApproval(item: ApprovalContentItem): String = approvalAdapter.toJson(item)
  fun decodeApproval(json: String): ApprovalContentItem? = runCatching { approvalAdapter.fromJson(json) }.getOrNull()

  fun encodeJob(job: ScheduledPostJob): String = jobAdapter.toJson(job)
  fun decodeJob(json: String): ScheduledPostJob? = runCatching { jobAdapter.fromJson(json) }.getOrNull()
}

fun MotionPostProject.toEntity(): MotionPostEntity = MotionPostEntity(
  id = id,
  title = title,
  templateCategory = templateCategory.name,
  payloadJson = Phase78Codec.encodeMotion(this),
  updatedAtMillis = updatedAtMillis,
)

fun MotionPostEntity.toDomain(): MotionPostProject? = Phase78Codec.decodeMotion(payloadJson)

fun ApprovalContentItem.toEntity(): ApprovalItemEntity = ApprovalItemEntity(
  id = id,
  sourceId = sourceId,
  title = title,
  status = status.name,
  payloadJson = Phase78Codec.encodeApproval(this),
  updatedAtMillis = updatedAtMillis,
)

fun ApprovalItemEntity.toDomain(): ApprovalContentItem? = Phase78Codec.decodeApproval(payloadJson)

fun ScheduledPostJob.toEntity(): ScheduledPostJobEntity = ScheduledPostJobEntity(
  id = id,
  approvalItemId = approvalItemId,
  idempotencyKey = idempotencyKey,
  status = status.name,
  scheduledAtMillis = scheduledAtMillis,
  payloadJson = Phase78Codec.encodeJob(this),
  updatedAtMillis = updatedAtMillis,
)

fun ScheduledPostJobEntity.toDomain(): ScheduledPostJob? = Phase78Codec.decodeJob(payloadJson)
