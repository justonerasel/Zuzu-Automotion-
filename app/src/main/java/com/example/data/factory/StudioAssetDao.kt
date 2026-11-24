package com.example.data.factory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioAssetDao {
  @Query("SELECT * FROM studio_assets ORDER BY createdAtMillis DESC")
  fun observeAllAssets(): Flow<List<StudioAssetEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAsset(asset: StudioAssetEntity)

  @Query("DELETE FROM studio_assets WHERE id = :assetId")
  suspend fun deleteAssetById(assetId: String)

  @Query("UPDATE studio_assets SET assignedSceneId = :sceneId, projectId = :projectId WHERE id = :assetId")
  suspend fun assignAssetToScene(assetId: String, projectId: String?, sceneId: String?)

  @Query("UPDATE studio_assets SET savedToMediaLibrary = 1 WHERE id = :assetId")
  suspend fun markSavedToMediaLibrary(assetId: String)
}
