package com.example.data.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentSourceDao {
  @Query("SELECT * FROM media_library_entries ORDER BY createdAtMillis DESC")
  fun observeMediaEntries(): Flow<List<MediaLibraryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMediaEntry(entity: MediaLibraryEntity)

  @Query("DELETE FROM media_library_entries WHERE id = :id")
  suspend fun deleteMediaEntry(id: String)

  @Query("SELECT * FROM creator_sources ORDER BY createdAtMillis DESC")
  fun observeCreatorSources(): Flow<List<CreatorSourceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCreatorSource(entity: CreatorSourceEntity)

  @Query("DELETE FROM creator_sources WHERE id = :id")
  suspend fun deleteCreatorSource(id: String)

  @Query("SELECT * FROM source_activities ORDER BY timestampMillis DESC LIMIT 20")
  fun observeRecentActivities(): Flow<List<SourceActivityEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivity(entity: SourceActivityEntity)
}
