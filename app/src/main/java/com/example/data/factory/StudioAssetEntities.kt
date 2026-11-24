package com.example.data.factory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.MediaType
import com.example.model.StudioAsset
import com.example.model.StudioAssetCategory
import com.example.model.VideoAspectRatio
import com.example.model.VideoLanguage
import com.example.model.VisualStudioStyle
import com.example.model.VoiceStudioTone

@Entity(tableName = "studio_assets")
data class StudioAssetEntity(
  @PrimaryKey val id: String,
  val projectId: String?,
  val assignedSceneId: String?,
  val title: String,
  val category: String,
  val mediaType: String,
  val localUriString: String?,
  val fileName: String?,
  val mimeType: String?,
  val fileSizeBytes: Long?,
  val durationMs: Long?,
  val promptUsed: String,
  val visualStyle: String?,
  val aspectRatio: String?,
  val language: String?,
  val voiceTone: String?,
  val providerLabel: String,
  val isAiGenerated: Boolean,
  val savedToMediaLibrary: Boolean,
  val createdAtMillis: Long,
)

fun StudioAsset.toEntity(): StudioAssetEntity = StudioAssetEntity(
  id = id,
  projectId = projectId,
  assignedSceneId = assignedSceneId,
  title = title,
  category = category.name,
  mediaType = mediaType.name,
  localUriString = localUriString,
  fileName = fileName,
  mimeType = mimeType,
  fileSizeBytes = fileSizeBytes,
  durationMs = durationMs,
  promptUsed = promptUsed,
  visualStyle = visualStyle?.name,
  aspectRatio = aspectRatio?.name,
  language = language?.name,
  voiceTone = voiceTone?.name,
  providerLabel = providerLabel,
  isAiGenerated = isAiGenerated,
  savedToMediaLibrary = savedToMediaLibrary,
  createdAtMillis = createdAtMillis,
)

fun StudioAssetEntity.toDomain(): StudioAsset = StudioAsset(
  id = id,
  projectId = projectId,
  assignedSceneId = assignedSceneId,
  title = title,
  category = enumOrNull<StudioAssetCategory>(category) ?: StudioAssetCategory.ImportedVisual,
  mediaType = enumOrNull<MediaType>(mediaType) ?: MediaType.Image,
  localUriString = localUriString,
  fileName = fileName,
  mimeType = mimeType,
  fileSizeBytes = fileSizeBytes,
  durationMs = durationMs,
  promptUsed = promptUsed,
  visualStyle = visualStyle?.let { enumOrNull<VisualStudioStyle>(it) },
  aspectRatio = aspectRatio?.let { enumOrNull<VideoAspectRatio>(it) },
  language = language?.let { enumOrNull<VideoLanguage>(it) },
  voiceTone = voiceTone?.let { enumOrNull<VoiceStudioTone>(it) },
  providerLabel = providerLabel,
  isAiGenerated = isAiGenerated,
  savedToMediaLibrary = savedToMediaLibrary,
  createdAtMillis = createdAtMillis,
)

private inline fun <reified T : Enum<T>> enumOrNull(raw: String): T? =
  runCatching { enumValueOf<T>(raw) }.getOrNull()
