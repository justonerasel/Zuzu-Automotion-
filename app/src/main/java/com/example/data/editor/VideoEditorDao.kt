package com.example.data.editor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoEditorDao {
  @Query("SELECT * FROM video_editor_drafts ORDER BY updatedAtMillis DESC")
  fun observeAllDrafts(): Flow<List<VideoEditorDraftEntity>>

  @Query("SELECT * FROM video_editor_drafts WHERE id = :draftId LIMIT 1")
  suspend fun getDraftById(draftId: String): VideoEditorDraftEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDraft(draft: VideoEditorDraftEntity)

  @Query("DELETE FROM video_editor_drafts WHERE id = :draftId")
  suspend fun deleteDraftById(draftId: String)

  @Query("SELECT * FROM exported_videos ORDER BY createdAtMillis DESC")
  fun observeExportedVideos(): Flow<List<ExportedVideoEntity>>

  @Query("SELECT * FROM exported_videos WHERE id = :id LIMIT 1")
  suspend fun getExportedVideoById(id: String): ExportedVideoEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExportedVideo(video: ExportedVideoEntity)

  @Query("UPDATE exported_videos SET fileName = :newFileName, filePath = :newFilePath, fileUriString = :newUriString WHERE id = :id")
  suspend fun updateExportedVideoFile(
    id: String,
    newFileName: String,
    newFilePath: String,
    newUriString: String,
  )

  @Query("DELETE FROM exported_videos WHERE id = :id")
  suspend fun deleteExportedVideoById(id: String)
}
