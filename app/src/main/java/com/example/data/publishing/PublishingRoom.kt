package com.example.data.publishing

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.model.PublishingJobRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Entity(tableName = "publishing_jobs")
data class PublishingJobEntity(
  @PrimaryKey val id: String,
  val approvalItemId: String,
  val platform: String,
  val idempotencyKey: String,
  val status: String,
  val payloadJson: String,
  val updatedAtMillis: Long,
)

@Dao
interface PublishingDao {
  @Query("SELECT * FROM publishing_jobs ORDER BY updatedAtMillis DESC")
  fun observePublishingJobs(): Flow<List<PublishingJobEntity>>

  @Query("SELECT * FROM publishing_jobs WHERE id = :id LIMIT 1")
  suspend fun getPublishingJobById(id: String): PublishingJobEntity?

  @Query("SELECT * FROM publishing_jobs WHERE idempotencyKey = :key LIMIT 1")
  suspend fun getPublishingJobByIdempotencyKey(key: String): PublishingJobEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPublishingJob(entity: PublishingJobEntity)

  @Query("DELETE FROM publishing_jobs WHERE id = :id")
  suspend fun deletePublishingJobById(id: String)
}

object PublishingCodec {
  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val jobAdapter = moshi.adapter(PublishingJobRecord::class.java)

  fun encodeJob(job: PublishingJobRecord): String = jobAdapter.toJson(job)
  fun decodeJob(json: String): PublishingJobRecord? = runCatching { jobAdapter.fromJson(json) }.getOrNull()
}

fun PublishingJobRecord.toEntity(): PublishingJobEntity = PublishingJobEntity(
  id = id,
  approvalItemId = approvalItemId,
  platform = platform.id,
  idempotencyKey = idempotencyKey,
  status = status.name,
  payloadJson = PublishingCodec.encodeJob(this),
  updatedAtMillis = updatedAtMillis,
)

fun PublishingJobEntity.toDomain(): PublishingJobRecord? = PublishingCodec.decodeJob(payloadJson)

interface PublishingRepository {
  val publishingJobs: Flow<List<PublishingJobRecord>>

  suspend fun getPublishingJob(id: String): PublishingJobRecord?
  suspend fun findJobByIdempotencyKey(key: String): PublishingJobRecord?
  suspend fun savePublishingJob(job: PublishingJobRecord): PublishingJobRecord
  suspend fun deletePublishingJob(id: String)
}

class RoomPublishingRepository(
  private val dao: PublishingDao,
) : PublishingRepository {
  override val publishingJobs: Flow<List<PublishingJobRecord>> =
    dao.observePublishingJobs().map { rows -> rows.mapNotNull { it.toDomain() } }

  override suspend fun getPublishingJob(id: String): PublishingJobRecord? =
    dao.getPublishingJobById(id)?.toDomain()

  override suspend fun findJobByIdempotencyKey(key: String): PublishingJobRecord? =
    dao.getPublishingJobByIdempotencyKey(key)?.toDomain()

  override suspend fun savePublishingJob(job: PublishingJobRecord): PublishingJobRecord {
    val stamped = job.copy(updatedAtMillis = System.currentTimeMillis())
    dao.upsertPublishingJob(stamped.toEntity())
    return stamped
  }

  override suspend fun deletePublishingJob(id: String) {
    dao.deletePublishingJobById(id)
  }
}

class InMemoryPublishingRepository : PublishingRepository {
  private val _jobs = MutableStateFlow<List<PublishingJobRecord>>(emptyList())
  override val publishingJobs: Flow<List<PublishingJobRecord>> = _jobs.asStateFlow()

  override suspend fun getPublishingJob(id: String): PublishingJobRecord? =
    _jobs.value.firstOrNull { it.id == id }

  override suspend fun findJobByIdempotencyKey(key: String): PublishingJobRecord? =
    _jobs.value.firstOrNull { it.idempotencyKey == key }

  override suspend fun savePublishingJob(job: PublishingJobRecord): PublishingJobRecord {
    val stamped = job.copy(updatedAtMillis = System.currentTimeMillis())
    _jobs.update { listOf(stamped) + it.filterNot { j -> j.id == stamped.id } }
    return stamped
  }

  override suspend fun deletePublishingJob(id: String) {
    _jobs.update { it.filterNot { j -> j.id == id } }
  }
}
