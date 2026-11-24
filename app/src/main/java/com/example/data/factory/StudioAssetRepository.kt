package com.example.data.factory

import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.StudioAsset
import com.example.model.StudioAssetCategory
import com.example.model.VideoAspectRatio
import com.example.model.VideoLanguage
import com.example.model.VisualStudioStyle
import com.example.model.VoiceStudioTone
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface StudioAssetRepository {
  val assets: Flow<List<StudioAsset>>

  suspend fun saveAsset(asset: StudioAsset): StudioAsset
  suspend fun importLocalAudio(
    upload: PendingLocalUpload,
    projectId: String?,
    assignedSceneId: String?,
    language: VideoLanguage,
    tone: VoiceStudioTone,
    title: String = "",
  ): Result<StudioAsset>

  suspend fun importLocalVisual(
    upload: PendingLocalUpload,
    projectId: String?,
    assignedSceneId: String?,
    style: VisualStudioStyle,
    aspectRatio: VideoAspectRatio,
    promptUsed: String = "",
    title: String = "",
  ): Result<StudioAsset>

  suspend fun assignAssetToScene(assetId: String, projectId: String?, sceneId: String?)
  suspend fun markSavedToMediaLibrary(assetId: String)
  suspend fun deleteAsset(assetId: String)
}

class RoomStudioAssetRepository(
  private val dao: StudioAssetDao,
) : StudioAssetRepository {

  override val assets: Flow<List<StudioAsset>> =
    dao.observeAllAssets().map { list -> list.map { it.toDomain() } }

  override suspend fun saveAsset(asset: StudioAsset): StudioAsset {
    dao.upsertAsset(asset.toEntity())
    return asset
  }

  override suspend fun importLocalAudio(
    upload: PendingLocalUpload,
    projectId: String?,
    assignedSceneId: String?,
    language: VideoLanguage,
    tone: VoiceStudioTone,
    title: String,
  ): Result<StudioAsset> {
    if (upload.mediaType != MediaType.Audio) {
      return Result.failure(IllegalArgumentException("Please select a valid audio file (MP3 or WAV)."))
    }
    val asset = StudioAsset(
      id = UUID.randomUUID().toString(),
      projectId = projectId,
      assignedSceneId = assignedSceneId,
      title = title.trim().ifEmpty { upload.fileName.substringBeforeLast('.') },
      category = StudioAssetCategory.ImportedAudio,
      mediaType = MediaType.Audio,
      localUriString = upload.uriString,
      fileName = upload.fileName,
      mimeType = upload.mimeType,
      fileSizeBytes = upload.fileSizeBytes,
      durationMs = upload.durationMs,
      language = language,
      voiceTone = tone,
      providerLabel = "Local Device Audio",
      isAiGenerated = false,
    )
    dao.upsertAsset(asset.toEntity())
    return Result.success(asset)
  }

  override suspend fun importLocalVisual(
    upload: PendingLocalUpload,
    projectId: String?,
    assignedSceneId: String?,
    style: VisualStudioStyle,
    aspectRatio: VideoAspectRatio,
    promptUsed: String,
    title: String,
  ): Result<StudioAsset> {
    if (upload.mediaType != MediaType.Image && upload.mediaType != MediaType.Video) {
      return Result.failure(IllegalArgumentException("Please select an image (JPG/PNG) or video clip (MP4/MOV)."))
    }
    val asset = StudioAsset(
      id = UUID.randomUUID().toString(),
      projectId = projectId,
      assignedSceneId = assignedSceneId,
      title = title.trim().ifEmpty { upload.fileName.substringBeforeLast('.') },
      category = StudioAssetCategory.ImportedVisual,
      mediaType = upload.mediaType,
      localUriString = upload.uriString,
      fileName = upload.fileName,
      mimeType = upload.mimeType,
      fileSizeBytes = upload.fileSizeBytes,
      durationMs = upload.durationMs,
      promptUsed = promptUsed.trim(),
      visualStyle = style,
      aspectRatio = aspectRatio,
      providerLabel = "Local Device Visual",
      isAiGenerated = false,
    )
    dao.upsertAsset(asset.toEntity())
    return Result.success(asset)
  }

  override suspend fun assignAssetToScene(assetId: String, projectId: String?, sceneId: String?) {
    dao.assignAssetToScene(assetId, projectId, sceneId)
  }

  override suspend fun markSavedToMediaLibrary(assetId: String) {
    dao.markSavedToMediaLibrary(assetId)
  }

  override suspend fun deleteAsset(assetId: String) {
    dao.deleteAssetById(assetId)
  }
}

class InMemoryStudioAssetRepository : StudioAssetRepository {
  private val _assets = MutableStateFlow<List<StudioAsset>>(emptyList())
  override val assets: Flow<List<StudioAsset>> = _assets.asStateFlow()

  override suspend fun saveAsset(asset: StudioAsset): StudioAsset {
    _assets.update { listOf(asset) + it.filterNot { existing -> existing.id == asset.id } }
    return asset
  }

  override suspend fun importLocalAudio(
    upload: PendingLocalUpload,
    projectId: String?,
    assignedSceneId: String?,
    language: VideoLanguage,
    tone: VoiceStudioTone,
    title: String,
  ): Result<StudioAsset> {
    if (upload.mediaType != MediaType.Audio) {
      return Result.failure(IllegalArgumentException("Please select a valid audio file (MP3 or WAV)."))
    }
    val asset = StudioAsset(
      id = UUID.randomUUID().toString(),
      projectId = projectId,
      assignedSceneId = assignedSceneId,
      title = title.trim().ifEmpty { upload.fileName.substringBeforeLast('.') },
      category = StudioAssetCategory.ImportedAudio,
      mediaType = MediaType.Audio,
      localUriString = upload.uriString,
      fileName = upload.fileName,
      mimeType = upload.mimeType,
      fileSizeBytes = upload.fileSizeBytes,
      durationMs = upload.durationMs,
      language = language,
      voiceTone = tone,
      providerLabel = "Local Device Audio",
      isAiGenerated = false,
    )
    saveAsset(asset)
    return Result.success(asset)
  }

  override suspend fun importLocalVisual(
    upload: PendingLocalUpload,
    projectId: String?,
    assignedSceneId: String?,
    style: VisualStudioStyle,
    aspectRatio: VideoAspectRatio,
    promptUsed: String,
    title: String,
  ): Result<StudioAsset> {
    if (upload.mediaType != MediaType.Image && upload.mediaType != MediaType.Video) {
      return Result.failure(IllegalArgumentException("Please select an image (JPG/PNG) or video clip (MP4/MOV)."))
    }
    val asset = StudioAsset(
      id = UUID.randomUUID().toString(),
      projectId = projectId,
      assignedSceneId = assignedSceneId,
      title = title.trim().ifEmpty { upload.fileName.substringBeforeLast('.') },
      category = StudioAssetCategory.ImportedVisual,
      mediaType = upload.mediaType,
      localUriString = upload.uriString,
      fileName = upload.fileName,
      mimeType = upload.mimeType,
      fileSizeBytes = upload.fileSizeBytes,
      durationMs = upload.durationMs,
      promptUsed = promptUsed.trim(),
      visualStyle = style,
      aspectRatio = aspectRatio,
      providerLabel = "Local Device Visual",
      isAiGenerated = false,
    )
    saveAsset(asset)
    return Result.success(asset)
  }

  override suspend fun assignAssetToScene(assetId: String, projectId: String?, sceneId: String?) {
    _assets.update { list ->
      list.map { item ->
        if (item.id == assetId) item.copy(projectId = projectId, assignedSceneId = sceneId) else item
      }
    }
  }

  override suspend fun markSavedToMediaLibrary(assetId: String) {
    _assets.update { list ->
      list.map { item ->
        if (item.id == assetId) item.copy(savedToMediaLibrary = true) else item
      }
    }
  }

  override suspend fun deleteAsset(assetId: String) {
    _assets.update { list -> list.filterNot { it.id == assetId } }
  }
}
