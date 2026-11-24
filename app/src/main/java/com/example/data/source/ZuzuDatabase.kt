package com.example.data.source

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.editor.ExportedVideoEntity
import com.example.data.editor.VideoEditorDao
import com.example.data.editor.VideoEditorDraftEntity
import com.example.data.factory.StoryboardSceneEntity
import com.example.data.factory.StudioAssetDao
import com.example.data.factory.StudioAssetEntity
import com.example.data.factory.VideoFactoryDao
import com.example.data.factory.VideoIdeaEntity
import com.example.data.factory.VideoProjectEntity
import com.example.data.phase78.ApprovalItemEntity
import com.example.data.phase78.MotionPostEntity
import com.example.data.phase78.Phase78Dao
import com.example.data.phase78.ScheduledPostJobEntity
import com.example.data.publishing.PublishingDao
import com.example.data.publishing.PublishingJobEntity

@Database(
  entities = [
    MediaLibraryEntity::class,
    CreatorSourceEntity::class,
    SourceActivityEntity::class,
    VideoProjectEntity::class,
    VideoIdeaEntity::class,
    StoryboardSceneEntity::class,
    StudioAssetEntity::class,
    VideoEditorDraftEntity::class,
    ExportedVideoEntity::class,
    MotionPostEntity::class,
    ApprovalItemEntity::class,
    ScheduledPostJobEntity::class,
    PublishingJobEntity::class,
  ],
  version = 6,
  exportSchema = false,
)
abstract class ZuzuDatabase : RoomDatabase() {
  abstract fun contentSourceDao(): ContentSourceDao
  abstract fun videoFactoryDao(): VideoFactoryDao
  abstract fun studioAssetDao(): StudioAssetDao
  abstract fun videoEditorDao(): VideoEditorDao
  abstract fun phase78Dao(): Phase78Dao
  abstract fun publishingDao(): PublishingDao

  companion object {
    @Volatile
    private var INSTANCE: ZuzuDatabase? = null

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `video_projects` (
            `id` TEXT NOT NULL,
            `topic` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `language` TEXT NOT NULL,
            `duration` TEXT NOT NULL,
            `videoType` TEXT NOT NULL,
            `audience` TEXT NOT NULL,
            `tone` TEXT NOT NULL,
            `visualStyle` TEXT NOT NULL,
            `aspectRatio` TEXT NOT NULL,
            `stage` TEXT NOT NULL,
            `researchOverview` TEXT NOT NULL,
            `researchKeyFacts` TEXT NOT NULL,
            `researchImportantPoints` TEXT NOT NULL,
            `researchSuggestedSources` TEXT NOT NULL,
            `researchNotes` TEXT NOT NULL,
            `researchOutline` TEXT NOT NULL,
            `researchAiUnverified` INTEGER NOT NULL,
            `researchUpdatedMillis` INTEGER NOT NULL,
            `scriptHook` TEXT NOT NULL,
            `scriptIntro` TEXT NOT NULL,
            `scriptMainContent` TEXT NOT NULL,
            `scriptSceneNarration` TEXT NOT NULL,
            `scriptTransitions` TEXT NOT NULL,
            `scriptEnding` TEXT NOT NULL,
            `scriptCallToAction` TEXT NOT NULL,
            `scriptIsAiDraft` INTEGER NOT NULL,
            `scriptUpdatedMillis` INTEGER NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `video_ideas` (
            `id` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `topic` TEXT NOT NULL,
            `hookConcept` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `targetAudience` TEXT NOT NULL,
            `language` TEXT NOT NULL,
            `isAiSuggested` INTEGER NOT NULL,
            `linkedProjectId` TEXT,
            `createdAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `storyboard_scenes` (
            `id` TEXT NOT NULL,
            `projectId` TEXT NOT NULL,
            `sceneNumber` INTEGER NOT NULL,
            `sceneTitle` TEXT NOT NULL,
            `narration` TEXT NOT NULL,
            `visualDescription` TEXT NOT NULL,
            `imageGenerationPrompt` TEXT NOT NULL,
            `videoGenerationPrompt` TEXT NOT NULL,
            `suggestedDurationSeconds` INTEGER NOT NULL,
            `transitionSuggestion` TEXT NOT NULL,
            `assignedVoiceAssetId` TEXT,
            `assignedVisualAssetId` TEXT,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `storyboard_scenes` ADD COLUMN `assignedVoiceAssetId` TEXT")
        db.execSQL("ALTER TABLE `storyboard_scenes` ADD COLUMN `assignedVisualAssetId` TEXT")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `studio_assets` (
            `id` TEXT NOT NULL,
            `projectId` TEXT,
            `assignedSceneId` TEXT,
            `title` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `mediaType` TEXT NOT NULL,
            `localUriString` TEXT,
            `fileName` TEXT,
            `mimeType` TEXT,
            `fileSizeBytes` INTEGER,
            `durationMs` INTEGER,
            `promptUsed` TEXT NOT NULL,
            `visualStyle` TEXT,
            `aspectRatio` TEXT,
            `language` TEXT,
            `voiceTone` TEXT,
            `providerLabel` TEXT NOT NULL,
            `isAiGenerated` INTEGER NOT NULL,
            `savedToMediaLibrary` INTEGER NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `video_editor_drafts` (
            `id` TEXT NOT NULL,
            `projectId` TEXT,
            `title` TEXT NOT NULL,
            `payloadJson` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `exported_videos` (
            `id` TEXT NOT NULL,
            `projectId` TEXT,
            `fileName` TEXT NOT NULL,
            `filePath` TEXT NOT NULL,
            `fileUriString` TEXT NOT NULL,
            `durationMs` INTEGER NOT NULL,
            `width` Int NOT NULL,
            `height` Int NOT NULL,
            `fileSizeBytes` INTEGER NOT NULL,
            `aspectRatio` TEXT NOT NULL,
            `resolutionLabel` TEXT NOT NULL,
            `savedMediaEntryId` TEXT,
            `createdAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent().replace("Int NOT NULL", "INTEGER NOT NULL"),
        )
      }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `motion_post_projects` (
            `id` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `templateCategory` TEXT NOT NULL,
            `payloadJson` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `approval_items` (
            `id` TEXT NOT NULL,
            `sourceId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `payloadJson` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `scheduled_post_jobs` (
            `id` TEXT NOT NULL,
            `approvalItemId` TEXT NOT NULL,
            `idempotencyKey` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `scheduledAtMillis` INTEGER NOT NULL,
            `payloadJson` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `publishing_jobs` (
            `id` TEXT NOT NULL,
            `approvalItemId` TEXT NOT NULL,
            `platform` TEXT NOT NULL,
            `idempotencyKey` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `payloadJson` TEXT NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
      }
    }

    fun getInstance(context: Context): ZuzuDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          ZuzuDatabase::class.java,
          "zuzu_social_ai.db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
