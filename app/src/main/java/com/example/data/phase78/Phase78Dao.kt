package com.example.data.phase78

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface Phase78Dao {
  @Query("SELECT * FROM motion_post_projects ORDER BY updatedAtMillis DESC")
  fun observeMotionProjects(): Flow<List<MotionPostEntity>>

  @Query("SELECT * FROM motion_post_projects WHERE id = :id LIMIT 1")
  suspend fun getMotionProjectById(id: String): MotionPostEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMotionProject(entity: MotionPostEntity)

  @Query("DELETE FROM motion_post_projects WHERE id = :id")
  suspend fun deleteMotionProjectById(id: String)

  @Query("SELECT * FROM approval_items ORDER BY updatedAtMillis DESC")
  fun observeApprovalItems(): Flow<List<ApprovalItemEntity>>

  @Query("SELECT * FROM approval_items WHERE id = :id LIMIT 1")
  suspend fun getApprovalItemById(id: String): ApprovalItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertApprovalItem(entity: ApprovalItemEntity)

  @Query("DELETE FROM approval_items WHERE id = :id")
  suspend fun deleteApprovalItemById(id: String)

  @Query("SELECT * FROM scheduled_post_jobs ORDER BY scheduledAtMillis ASC")
  fun observeScheduledJobs(): Flow<List<ScheduledPostJobEntity>>

  @Query("SELECT * FROM scheduled_post_jobs WHERE id = :id LIMIT 1")
  suspend fun getScheduledJobById(id: String): ScheduledPostJobEntity?

  @Query("SELECT * FROM scheduled_post_jobs WHERE idempotencyKey = :key LIMIT 1")
  suspend fun getScheduledJobByIdempotencyKey(key: String): ScheduledPostJobEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertScheduledJob(entity: ScheduledPostJobEntity)

  @Query("DELETE FROM scheduled_post_jobs WHERE id = :id")
  suspend fun deleteScheduledJobById(id: String)
}
