package com.example.data.editor

import com.example.model.EditorAudioTrack
import com.example.model.EditorAudioTrackType
import com.example.model.EditorClipType
import com.example.model.EditorTimelineClip
import com.example.model.EditorTransition
import com.example.model.ExportSettings
import com.example.model.ExportedVideoRecord
import com.example.model.MediaType
import com.example.model.StudioAsset
import com.example.model.SubtitleCue
import com.example.model.SubtitlePosition
import com.example.model.VideoEditorDraft
import com.example.model.VideoProject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface VideoEditorRepository {
  val drafts: Flow<List<VideoEditorDraft>>
  val exportedVideos: Flow<List<ExportedVideoRecord>>

  suspend fun getDraft(draftId: String): VideoEditorDraft?
  suspend fun saveDraft(draft: VideoEditorDraft): VideoEditorDraft
  suspend fun deleteDraft(draftId: String)

  suspend fun saveExportedVideo(record: ExportedVideoRecord): ExportedVideoRecord
  suspend fun renameExportedVideo(id: String, requestedName: String): Result<ExportedVideoRecord>
  suspend fun deleteExportedVideo(id: String)
}

object ProjectToEditorImporter {
  fun buildDraftFromProject(
    project: VideoProject,
    studioAssets: List<StudioAsset>,
  ): VideoEditorDraft {
    val assetById = studioAssets.associateBy { it.id }

    val clips = project.scenes.map { scene ->
      val visualAsset = scene.assignedVisualAssetId?.let { assetById[it] }
      val clipType = when (visualAsset?.mediaType) {
        MediaType.Image -> EditorClipType.Image
        MediaType.Video -> EditorClipType.Video
        else -> EditorClipType.TitleCard
      }
      val duration = (scene.suggestedDurationSeconds.coerceIn(1, 120)) * 1000L
      EditorTimelineClip(
        id = UUID.randomUUID().toString(),
        sourceSceneId = scene.id,
        title = "Scene ${scene.sceneNumber}: ${scene.sceneTitle}",
        clipType = clipType,
        mediaUriString = visualAsset?.localUriString,
        assetId = visualAsset?.id,
        trimStartMs = 0L,
        durationMs = duration,
        transition = EditorTransition.CrossFade,
        notes = scene.visualDescription,
      )
    }

    var cursorMs = 0L
    val subtitles = mutableListOf<SubtitleCue>()
    val audioTracks = mutableListOf<EditorAudioTrack>()
    val seenAudioIds = mutableSetOf<String>()

    project.scenes.forEach { scene ->
      val sceneDurationMs = (scene.suggestedDurationSeconds.coerceIn(1, 120)) * 1000L
      if (scene.narration.isNotBlank()) {
        subtitles += SubtitleCue(
          id = UUID.randomUUID().toString(),
          text = scene.narration.trim(),
          language = project.language,
          startMs = cursorMs,
          endMs = cursorMs + sceneDurationMs,
          position = SubtitlePosition.Bottom,
        )
      }
      val voiceAsset = scene.assignedVoiceAssetId?.let { assetById[it] }
      if (voiceAsset != null && !voiceAsset.localUriString.isNullOrBlank() && seenAudioIds.add(voiceAsset.id)) {
        audioTracks += EditorAudioTrack(
          id = UUID.randomUUID().toString(),
          trackType = EditorAudioTrackType.Voiceover,
          title = voiceAsset.title,
          uriString = voiceAsset.localUriString,
          startOffsetMs = cursorMs,
          trimStartMs = 0L,
          trimEndMs = voiceAsset.durationMs ?: 0L,
          sourceDurationMs = voiceAsset.durationMs ?: 0L,
          volume = 1.0f,
        )
      }
      cursorMs += sceneDurationMs
    }

    return VideoEditorDraft(
      id = project.id,
      projectId = project.id,
      title = project.topic,
      clips = clips,
      audioTracks = audioTracks,
      subtitles = subtitles,
      exportSettings = ExportSettings(aspectRatio = project.aspectRatio),
      updatedAtMillis = System.currentTimeMillis(),
    )
  }
}

class RoomVideoEditorRepository(
  private val dao: VideoEditorDao,
) : VideoEditorRepository {

  override val drafts: Flow<List<VideoEditorDraft>> =
    dao.observeAllDrafts().map { list -> list.mapNotNull { it.toDomain() } }

  override val exportedVideos: Flow<List<ExportedVideoRecord>> =
    dao.observeExportedVideos().map { list -> list.map { it.toDomain() } }

  override suspend fun getDraft(draftId: String): VideoEditorDraft? =
    dao.getDraftById(draftId)?.toDomain()

  override suspend fun saveDraft(draft: VideoEditorDraft): VideoEditorDraft {
    val stamped = draft.copy(updatedAtMillis = System.currentTimeMillis())
    dao.upsertDraft(stamped.toEntity())
    return stamped
  }

  override suspend fun deleteDraft(draftId: String) {
    dao.deleteDraftById(draftId)
  }

  override suspend fun saveExportedVideo(record: ExportedVideoRecord): ExportedVideoRecord {
    dao.upsertExportedVideo(record.toEntity())
    return record
  }

  override suspend fun renameExportedVideo(
    id: String,
    requestedName: String,
  ): Result<ExportedVideoRecord> {
    val sanitizedBase = requestedName.trim()
      .replace(Regex("[^A-Za-z0-9_\\- ]"), "_")
      .trim()
      .ifEmpty { return Result.failure(IllegalArgumentException("Enter a valid file name.")) }
    val finalName = if (sanitizedBase.lowercase().endsWith(".mp4")) sanitizedBase else "$sanitizedBase.mp4"

    val existing = dao.getExportedVideoById(id)?.toDomain()
      ?: return Result.failure(IllegalStateException("Exported video not found."))

    val oldFile = File(existing.filePath)
    var updatedPath = existing.filePath
    var updatedUri = existing.fileUriString
    if (oldFile.exists()) {
      val targetFile = File(oldFile.parentFile, finalName)
      if (targetFile.absolutePath != oldFile.absolutePath && !targetFile.exists()) {
        if (oldFile.renameTo(targetFile)) {
          updatedPath = targetFile.absolutePath
          updatedUri = android.net.Uri.fromFile(targetFile).toString()
        }
      }
    }

    dao.updateExportedVideoFile(
      id = id,
      newFileName = finalName,
      newFilePath = updatedPath,
      newUriString = updatedUri,
    )
    return Result.success(
      existing.copy(
        fileName = finalName,
        filePath = updatedPath,
        fileUriString = updatedUri,
      ),
    )
  }

  override suspend fun deleteExportedVideo(id: String) {
    val existing = dao.getExportedVideoById(id)?.toDomain()
    if (existing != null) {
      runCatching { File(existing.filePath).delete() }
    }
    dao.deleteExportedVideoById(id)
  }
}

class InMemoryVideoEditorRepository : VideoEditorRepository {
  private val _drafts = MutableStateFlow<List<VideoEditorDraft>>(emptyList())
  private val _exports = MutableStateFlow<List<ExportedVideoRecord>>(emptyList())

  override val drafts: Flow<List<VideoEditorDraft>> = _drafts.asStateFlow()
  override val exportedVideos: Flow<List<ExportedVideoRecord>> = _exports.asStateFlow()

  override suspend fun getDraft(draftId: String): VideoEditorDraft? =
    _drafts.value.firstOrNull { it.id == draftId }

  override suspend fun saveDraft(draft: VideoEditorDraft): VideoEditorDraft {
    val stamped = draft.copy(updatedAtMillis = System.currentTimeMillis())
    _drafts.update { listOf(stamped) + it.filterNot { d -> d.id == stamped.id } }
    return stamped
  }

  override suspend fun deleteDraft(draftId: String) {
    _drafts.update { list -> list.filterNot { it.id == draftId } }
  }

  override suspend fun saveExportedVideo(record: ExportedVideoRecord): ExportedVideoRecord {
    _exports.update { listOf(record) + it.filterNot { e -> e.id == record.id } }
    return record
  }

  override suspend fun renameExportedVideo(
    id: String,
    requestedName: String,
  ): Result<ExportedVideoRecord> {
    val sanitizedBase = requestedName.trim()
      .replace(Regex("[^A-Za-z0-9_\\- ]"), "_")
      .trim()
      .ifEmpty { return Result.failure(IllegalArgumentException("Enter a valid file name.")) }
    val finalName = if (sanitizedBase.lowercase().endsWith(".mp4")) sanitizedBase else "$sanitizedBase.mp4"
    val target = _exports.value.firstOrNull { it.id == id }
      ?: return Result.failure(IllegalStateException("Exported video not found."))
    val updated = target.copy(fileName = finalName)
    _exports.update { list -> list.map { if (it.id == id) updated else it } }
    return Result.success(updated)
  }

  override suspend fun deleteExportedVideo(id: String) {
    _exports.update { list -> list.filterNot { it.id == id } }
  }
}
