package com.example.data.editor

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ExportedVideoRecord
import com.example.model.VideoAspectRatio
import com.example.model.VideoEditorDraft
import com.squareup.moshi.Moshi

@Entity(tableName = "video_editor_drafts")
data class VideoEditorDraftEntity(
  @PrimaryKey val id: String,
  val projectId: String?,
  val title: String,
  val payloadJson: String,
  val updatedAtMillis: Long,
)

@Entity(tableName = "exported_videos")
data class ExportedVideoEntity(
  @PrimaryKey val id: String,
  val projectId: String?,
  val fileName: String,
  val filePath: String,
  val fileUriString: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val fileSizeBytes: Long,
  val aspectRatio: String,
  val resolutionLabel: String,
  val savedMediaEntryId: String?,
  val createdAtMillis: Long,
)

object VideoEditorDraftCodec {
  private val moshi: Moshi = Moshi.Builder().build()
  private val adapter = moshi.adapter(VideoEditorDraft::class.java)

  fun encode(draft: VideoEditorDraft): String = adapter.toJson(draft)

  fun decode(json: String): VideoEditorDraft? = runCatching {
    adapter.fromJson(json)
  }.getOrNull()
}

fun VideoEditorDraft.toEntity(): VideoEditorDraftEntity = VideoEditorDraftEntity(
  id = id,
  projectId = projectId,
  title = title,
  payloadJson = VideoEditorDraftCodec.encode(this),
  updatedAtMillis = updatedAtMillis,
)

fun VideoEditorDraftEntity.toDomain(): VideoEditorDraft? =
  VideoEditorDraftCodec.decode(payloadJson)

fun ExportedVideoRecord.toEntity(): ExportedVideoEntity = ExportedVideoEntity(
  id = id,
  projectId = projectId,
  fileName = fileName,
  filePath = filePath,
  fileUriString = fileUriString,
  durationMs = durationMs,
  width = width,
  height = height,
  fileSizeBytes = fileSizeBytes,
  aspectRatio = aspectRatio.name,
  resolutionLabel = resolutionLabel,
  savedMediaEntryId = savedMediaEntryId,
  createdAtMillis = createdAtMillis,
)

fun ExportedVideoEntity.toDomain(): ExportedVideoRecord = ExportedVideoRecord(
  id = id,
  projectId = projectId,
  fileName = fileName,
  filePath = filePath,
  fileUriString = fileUriString,
  durationMs = durationMs,
  width = width,
  height = height,
  fileSizeBytes = fileSizeBytes,
  aspectRatio = enumValueOfOrDefault(aspectRatio, VideoAspectRatio.Vertical),
  resolutionLabel = resolutionLabel,
  savedMediaEntryId = savedMediaEntryId,
  createdAtMillis = createdAtMillis,
)

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(name: String, fallback: T): T =
  runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
