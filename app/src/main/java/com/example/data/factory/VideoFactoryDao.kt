package com.example.data.factory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoFactoryDao {
  @Query("SELECT * FROM video_projects ORDER BY updatedAtMillis DESC")
  fun observeVideoProjects(): Flow<List<VideoProjectEntity>>

  @Query("SELECT * FROM video_ideas ORDER BY createdAtMillis DESC")
  fun observeVideoIdeas(): Flow<List<VideoIdeaEntity>>

  @Query("SELECT * FROM storyboard_scenes ORDER BY projectId ASC, sceneNumber ASC")
  fun observeAllScenes(): Flow<List<StoryboardSceneEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProject(project: VideoProjectEntity)

  @Query("DELETE FROM video_projects WHERE id = :projectId")
  suspend fun deleteProjectById(projectId: String)

  @Query("DELETE FROM storyboard_scenes WHERE projectId = :projectId")
  suspend fun deleteScenesForProject(projectId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertScenes(scenes: List<StoryboardSceneEntity>)

  @Transaction
  suspend fun replaceScenesForProject(projectId: String, scenes: List<StoryboardSceneEntity>) {
    deleteScenesForProject(projectId)
    if (scenes.isNotEmpty()) {
      insertScenes(scenes)
    }
  }

  @Transaction
  suspend fun deleteProjectAndScenes(projectId: String) {
    deleteScenesForProject(projectId)
    deleteProjectById(projectId)
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertIdea(idea: VideoIdeaEntity)

  @Query("DELETE FROM video_ideas WHERE id = :ideaId")
  suspend fun deleteIdeaById(ideaId: String)
}
