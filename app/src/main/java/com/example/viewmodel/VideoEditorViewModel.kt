package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.editor.InMemoryVideoEditorRepository
import com.example.data.editor.ProjectToEditorImporter
import com.example.data.editor.RenderOutcome
import com.example.data.editor.RendererCapabilityStatus
import com.example.data.editor.VideoEditorRepository
import com.example.data.editor.VideoMp4Renderer
import com.example.data.source.ContentSourceRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.EditorAudioTrack
import com.example.model.EditorAudioTrackType
import com.example.model.EditorClipType
import com.example.model.EditorTimelineClip
import com.example.model.EditorTransition
import com.example.model.EditorVisualEffect
import com.example.model.ExportQuality
import com.example.model.ExportResolution
import com.example.model.ExportedVideoRecord
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.StudioAsset
import com.example.model.SubtitleCue
import com.example.model.TextOverlay
import com.example.model.VideoAspectRatio
import com.example.model.VideoEditorDraft
import com.example.model.VideoEditorTab
import com.example.model.VideoProject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RenderUiState(
  val isRendering: Boolean = false,
  val progressFraction: Float = 0f,
  val stageLabel: String = "",
  val lastExportedVideo: ExportedVideoRecord? = null,
)

data class VideoEditorUiState(
  val activeTab: VideoEditorTab = VideoEditorTab.Timeline,
  val currentDraft: VideoEditorDraft? = null,
  val savedDrafts: List<VideoEditorDraft> = emptyList(),
  val exportedVideos: List<ExportedVideoRecord> = emptyList(),
  val rendererCapability: RendererCapabilityStatus = RendererCapabilityStatus.Available(
    engineName = "Android MediaCodec H.264 / AAC",
    description = "Hardware/software MP4 muxing engine",
  ),
  val renderState: RenderUiState = RenderUiState(),
  val hasUnsavedChanges: Boolean = false,
  val playheadMs: Long = 0L,
  val isPreviewPlaying: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val estimatedFileSizeBytes: Long?
    get() {
      val draft = currentDraft ?: return null
      val hasAudio = draft.audioTracks.any { !it.isMuted && it.volume > 0.01f }
      return draft.exportSettings.estimateFileSizeBytes(draft.totalDurationMs, hasAudio)
    }
}

class VideoEditorViewModel(
  private val editorRepository: VideoEditorRepository = InMemoryVideoEditorRepository(),
  private val contentSourceRepository: ContentSourceRepository = InMemoryContentSourceRepository(),
  private val mp4Renderer: VideoMp4Renderer,
  private val exportsDirectoryProvider: () -> File = { File(System.getProperty("java.io.tmpdir"), "zuzu_exports") },
  private val shareUriResolver: (File) -> String = { file -> runCatching { Uri.fromFile(file).toString() }.getOrElse { "file://${file.absolutePath}" } },
) : ViewModel() {

  private val activeTab = MutableStateFlow(VideoEditorTab.Timeline)
  private val currentDraft = MutableStateFlow<VideoEditorDraft?>(null)
  private val renderState = MutableStateFlow(RenderUiState())
  private val hasUnsavedChanges = MutableStateFlow(false)
  private val playheadMs = MutableStateFlow(0L)
  private val isPreviewPlaying = MutableStateFlow(false)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private var renderJob: Job? = null
  private val capability by lazy { mp4Renderer.checkCapability() }

  private val playbackControlsStream = combine(
    activeTab,
    playheadMs,
    isPreviewPlaying,
    hasUnsavedChanges,
  ) { tab, playhead, playing, dirty ->
    PlaybackSnapshot(tab, playhead, playing, dirty)
  }

  private val statusStream = combine(
    renderState,
    feedbackMessage,
    errorMessage,
  ) { render, feedback, error ->
    Triple(render, feedback, error)
  }

  val uiState: StateFlow<VideoEditorUiState> = combine(
    currentDraft,
    editorRepository.drafts,
    editorRepository.exportedVideos,
    playbackControlsStream,
    statusStream,
  ) { draft, savedDrafts, exports, playback, (render, feedback, error) ->
    VideoEditorUiState(
      activeTab = playback.tab,
      currentDraft = draft,
      savedDrafts = savedDrafts,
      exportedVideos = exports,
      rendererCapability = capability,
      renderState = render,
      hasUnsavedChanges = playback.dirty,
      playheadMs = playback.playheadMs.coerceIn(0L, draft?.totalDurationMs ?: 0L),
      isPreviewPlaying = playback.playing,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = VideoEditorUiState(rendererCapability = capability),
  )

  fun selectTab(tab: VideoEditorTab) {
    activeTab.value = tab
    clearMessages()
  }

  fun openProjectInEditor(
    project: VideoProject?,
    studioAssets: List<StudioAsset>,
    forceReloadFromProject: Boolean = false,
  ) {
    viewModelScope.launch {
      if (project == null) {
        if (currentDraft.value == null) {
          currentDraft.value = VideoEditorDraft(
            id = "standalone_draft",
            projectId = null,
            title = "Untitled Zuzu Edit",
          )
        }
        return@launch
      }

      val existingCurrent = currentDraft.value
      if (!forceReloadFromProject && existingCurrent?.id == project.id) {
        return@launch
      }

      val persisted = if (!forceReloadFromProject) editorRepository.getDraft(project.id) else null
      if (persisted != null) {
        currentDraft.value = persisted
        hasUnsavedChanges.value = false
        playheadMs.value = 0L
        isPreviewPlaying.value = false
        feedbackMessage.value = "Resumed saved draft for '${persisted.title}'."
      } else {
        val imported = ProjectToEditorImporter.buildDraftFromProject(project, studioAssets)
        currentDraft.value = imported
        hasUnsavedChanges.value = true
        playheadMs.value = 0L
        isPreviewPlaying.value = false
        if (forceReloadFromProject) {
          feedbackMessage.value = "Synced timeline from storyboard scenes & studio assets."
        }
      }
    }
  }

  fun resumeSavedDraft(draft: VideoEditorDraft) {
    currentDraft.value = draft
    hasUnsavedChanges.value = false
    playheadMs.value = 0L
    isPreviewPlaying.value = false
    feedbackMessage.value = "Resumed draft '${draft.title}'."
  }

  fun saveDraft() {
    val draft = currentDraft.value ?: return
    viewModelScope.launch {
      val saved = editorRepository.saveDraft(draft)
      currentDraft.value = saved
      hasUnsavedChanges.value = false
      feedbackMessage.value = "Editor draft '${saved.title}' saved locally."
      errorMessage.value = null
    }
  }

  fun setPlayheadMs(ms: Long) {
    val maxMs = currentDraft.value?.totalDurationMs ?: 0L
    playheadMs.value = ms.coerceIn(0L, maxMs)
  }

  fun setPreviewPlaying(playing: Boolean) {
    isPreviewPlaying.value = playing
  }

  // --- Timeline Clip Editing ---

  fun addTitleCardClip(title: String, durationSeconds: Int = 5) {
    val clean = title.trim().ifEmpty { "Scene ${(currentDraft.value?.clips?.size ?: 0) + 1}" }
    mutateDraft { draft ->
      val clip = EditorTimelineClip(
        id = UUID.randomUUID().toString(),
        title = clean,
        clipType = EditorClipType.TitleCard,
        durationMs = durationSeconds.coerceIn(1, 120) * 1000L,
      )
      draft.copy(clips = draft.clips + clip)
    }
    feedbackMessage.value = "Added scene '$clean' to timeline."
  }

  fun addMediaClipFromUpload(upload: PendingLocalUpload) {
    val type = when (upload.mediaType) {
      MediaType.Image -> EditorClipType.Image
      MediaType.Video -> EditorClipType.Video
      else -> {
        errorMessage.value = "Select an image (JPG/PNG) or video clip (MP4/MOV) for the visual timeline."
        return
      }
    }
    val defaultDuration = if (type == EditorClipType.Video && (upload.durationMs ?: 0L) > 0L) {
      upload.durationMs!!.coerceIn(1_000L, 120_000L)
    } else {
      5_000L
    }
    addMediaClip(
      title = upload.fileName.substringBeforeLast('.'),
      clipType = type,
      uriString = upload.uriString,
      durationMs = defaultDuration,
    )
  }

  fun addMediaClip(
    title: String,
    clipType: EditorClipType,
    uriString: String?,
    assetId: String? = null,
    durationMs: Long = 5_000L,
  ) {
    mutateDraft { draft ->
      val clip = EditorTimelineClip(
        id = UUID.randomUUID().toString(),
        title = title.trim().ifEmpty { "Clip ${draft.clips.size + 1}" },
        clipType = clipType,
        mediaUriString = uriString,
        assetId = assetId,
        durationMs = durationMs.coerceIn(1_000L, 120_000L),
      )
      draft.copy(clips = draft.clips + clip)
    }
    feedbackMessage.value = "Added '${title}' to timeline."
    errorMessage.value = null
  }

  fun updateClip(
    clipId: String,
    title: String? = null,
    trimStartMs: Long? = null,
    durationMs: Long? = null,
    transition: EditorTransition? = null,
    visualEffect: EditorVisualEffect? = null,
  ) {
    mutateDraft { draft ->
      draft.copy(
        clips = draft.clips.map { clip ->
          if (clip.id == clipId) {
            clip.copy(
              title = title?.trim()?.ifEmpty { clip.title } ?: clip.title,
              trimStartMs = (trimStartMs ?: clip.trimStartMs).coerceAtLeast(0L),
              durationMs = (durationMs ?: clip.durationMs).coerceIn(1_000L, 120_000L),
              transition = transition ?: clip.transition,
              visualEffect = visualEffect ?: clip.visualEffect,
            )
          } else {
            clip
          }
        },
      )
    }
  }

  fun moveClip(index: Int, delta: Int) {
    mutateDraft { draft ->
      val target = index + delta
      if (index !in draft.clips.indices || target !in draft.clips.indices) return@mutateDraft draft
      val mutable = draft.clips.toMutableList()
      val item = mutable.removeAt(index)
      mutable.add(target, item)
      draft.copy(clips = mutable)
    }
  }

  fun removeClip(clipId: String) {
    mutateDraft { draft ->
      draft.copy(clips = draft.clips.filterNot { it.id == clipId })
    }
    feedbackMessage.value = "Clip removed from timeline."
  }

  // --- Text Overlays ---

  fun addTextOverlay(overlay: TextOverlay) {
    if (overlay.text.isBlank()) {
      errorMessage.value = "Enter overlay text before adding."
      return
    }
    mutateDraft { draft ->
      val existing = draft.textOverlays.any { it.id == overlay.id }
      val updated = if (existing) {
        draft.textOverlays.map { if (it.id == overlay.id) overlay else it }
      } else {
        draft.textOverlays + overlay
      }
      draft.copy(textOverlays = updated)
    }
    feedbackMessage.value = "Text overlay saved."
    errorMessage.value = null
  }

  fun removeTextOverlay(overlayId: String) {
    mutateDraft { draft ->
      draft.copy(textOverlays = draft.textOverlays.filterNot { it.id == overlayId })
    }
  }

  // --- Subtitles ---

  fun upsertSubtitle(cue: SubtitleCue) {
    if (cue.text.isBlank()) {
      errorMessage.value = "Subtitle text cannot be empty."
      return
    }
    val safeEnd = maxOf(cue.endMs, cue.startMs + 500L)
    val normalized = cue.copy(
      text = cue.text.trim(),
      startMs = cue.startMs.coerceAtLeast(0L),
      endMs = safeEnd,
      fontSizeSp = cue.fontSizeSp.coerceIn(12, 36),
    )
    mutateDraft { draft ->
      val exists = draft.subtitles.any { it.id == normalized.id }
      val next = if (exists) {
        draft.subtitles.map { if (it.id == normalized.id) normalized else it }
      } else {
        draft.subtitles + normalized
      }
      draft.copy(subtitles = next.sortedBy { it.startMs })
    }
    feedbackMessage.value = "Subtitle saved."
    errorMessage.value = null
  }

  fun removeSubtitle(cueId: String) {
    mutateDraft { draft ->
      draft.copy(subtitles = draft.subtitles.filterNot { it.id == cueId })
    }
  }

  fun importSubtitlesFromProject(project: VideoProject) {
    if (project.scenes.isEmpty()) {
      errorMessage.value = "No storyboard scenes found to generate subtitle cues."
      return
    }
    var cursor = 0L
    val cues = project.scenes.mapNotNull { scene ->
      val dur = scene.suggestedDurationSeconds.coerceIn(1, 120) * 1000L
      val text = scene.narration.trim()
      val cue = if (text.isNotEmpty()) {
        SubtitleCue(
          id = UUID.randomUUID().toString(),
          text = text,
          language = project.language,
          startMs = cursor,
          endMs = cursor + dur,
        )
      } else {
        null
      }
      cursor += dur
      cue
    }
    if (cues.isEmpty()) {
      errorMessage.value = "Storyboard scenes do not contain narration text yet."
      return
    }
    mutateDraft { it.copy(subtitles = cues) }
    feedbackMessage.value = "Imported ${cues.size} ${project.language.displayLabel} subtitle cues."
    errorMessage.value = null
  }

  // --- Audio Tracks ---

  fun addAudioTrack(
    trackType: EditorAudioTrackType,
    title: String,
    uriString: String,
    durationMs: Long? = null,
  ) {
    val track = EditorAudioTrack(
      id = UUID.randomUUID().toString(),
      trackType = trackType,
      title = title.trim().ifEmpty { trackType.displayLabel },
      uriString = uriString,
      startOffsetMs = 0L,
      trimStartMs = 0L,
      trimEndMs = durationMs ?: 0L,
      sourceDurationMs = durationMs ?: 0L,
      volume = if (trackType == EditorAudioTrackType.Voiceover) 1.0f else 0.35f,
    )
    mutateDraft { draft ->
      draft.copy(audioTracks = draft.audioTracks + track)
    }
    feedbackMessage.value = "Added ${trackType.displayLabel} track '${track.title}'."
    errorMessage.value = null
  }

  fun updateAudioVolume(trackId: String, volume: Float) {
    mutateDraft { draft ->
      draft.copy(
        audioTracks = draft.audioTracks.map { t ->
          if (t.id == trackId) t.copy(volume = volume.coerceIn(0f, 1f)) else t
        },
      )
    }
  }

  fun updateAudioTrim(trackId: String, trimStartMs: Long, trimEndMs: Long) {
    val cleanStart = trimStartMs.coerceAtLeast(0L)
    val cleanEnd = if (trimEndMs > 0L) maxOf(trimEndMs, cleanStart + 500L) else 0L
    mutateDraft { draft ->
      draft.copy(
        audioTracks = draft.audioTracks.map { t ->
          if (t.id == trackId) t.copy(trimStartMs = cleanStart, trimEndMs = cleanEnd) else t
        },
      )
    }
  }

  fun toggleAudioMute(trackId: String) {
    mutateDraft { draft ->
      draft.copy(
        audioTracks = draft.audioTracks.map { t ->
          if (t.id == trackId) t.copy(isMuted = !t.isMuted) else t
        },
      )
    }
  }

  fun removeAudioTrack(trackId: String) {
    mutateDraft { draft ->
      draft.copy(audioTracks = draft.audioTracks.filterNot { it.id == trackId })
    }
    feedbackMessage.value = "Audio track removed."
  }

  // --- Export Settings ---

  fun setExportAspectRatio(ratio: VideoAspectRatio) {
    mutateDraft { it.copy(exportSettings = it.exportSettings.copy(aspectRatio = ratio)) }
  }

  fun setExportResolution(resolution: ExportResolution) {
    mutateDraft { it.copy(exportSettings = it.exportSettings.copy(resolution = resolution)) }
  }

  fun setExportQuality(quality: ExportQuality) {
    mutateDraft { it.copy(exportSettings = it.exportSettings.copy(quality = quality)) }
  }

  // --- MP4 Rendering & Export Library ---

  fun startRender() {
    val draft = currentDraft.value
    if (draft == null || draft.clips.isEmpty()) {
      errorMessage.value = "Add at least one clip to the timeline before rendering MP4."
      return
    }
    if (renderJob?.isActive == true) return

    clearMessages()
    renderJob = viewModelScope.launch {
      val savedDraft = editorRepository.saveDraft(draft)
      currentDraft.value = savedDraft
      hasUnsavedChanges.value = false

      renderState.value = RenderUiState(
        isRendering = true,
        progressFraction = 0.01f,
        stageLabel = "Initializing MP4 encoder...",
      )

      val exportDir = exportsDirectoryProvider().apply { mkdirs() }
      val safeBase = savedDraft.title
        .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        .trim('_')
        .take(28)
        .ifEmpty { "zuzu_video" }
      val fileName = "${safeBase}_${savedDraft.exportSettings.resolution.shortSidePx}p_${System.currentTimeMillis() % 10_000}.mp4"
      val outFile = File(exportDir, fileName)

      when (
        val outcome = mp4Renderer.renderToMp4(
          draft = savedDraft,
          outputFile = outFile,
          onProgress = { prog ->
            renderState.update { state ->
              state.copy(
                isRendering = true,
                progressFraction = prog.fraction.coerceIn(0f, 1f),
                stageLabel = prog.stageLabel,
              )
            }
          },
        )
      ) {
        is RenderOutcome.Success -> {
          val fileUri = shareUriResolver(outcome.outputFile)
          val mediaResult = contentSourceRepository.saveLocalUpload(
            upload = PendingLocalUpload(
              uriString = fileUri,
              fileName = outcome.outputFile.name,
              mimeType = "video/mp4",
              mediaType = MediaType.Video,
              fileSizeBytes = outcome.fileSizeBytes,
              durationMs = outcome.durationMs,
            ),
            title = outcome.outputFile.nameWithoutExtension,
            description = "Rendered MP4 (${outcome.width}x${outcome.height})",
            permission = PermissionRecord(
              status = PermissionStatus.OriginalContent,
              contentOwner = "Zuzu Social AI Creator",
              evidenceDescription = "Rendered in Zuzu Video Editor",
              approvedForReuse = true,
            ),
            notes = "Exported ${savedDraft.exportSettings.quality.displayLabel} ${savedDraft.exportSettings.resolution.displayLabel}",
          )

          val record = ExportedVideoRecord(
            id = UUID.randomUUID().toString(),
            projectId = savedDraft.projectId,
            fileName = outcome.outputFile.name,
            filePath = outcome.outputFile.absolutePath,
            fileUriString = fileUri,
            durationMs = outcome.durationMs,
            width = outcome.width,
            height = outcome.height,
            fileSizeBytes = outcome.fileSizeBytes,
            aspectRatio = savedDraft.exportSettings.aspectRatio,
            resolutionLabel = savedDraft.exportSettings.resolution.displayLabel,
            savedMediaEntryId = mediaResult.getOrNull()?.id,
          )
          editorRepository.saveExportedVideo(record)
          renderState.value = RenderUiState(
            isRendering = false,
            progressFraction = 1f,
            stageLabel = "Completed",
            lastExportedVideo = record,
          )
          feedbackMessage.value = "MP4 exported & saved to Media Library: ${record.fileName}"
        }
        is RenderOutcome.Cancelled -> {
          renderState.value = RenderUiState(isRendering = false)
          feedbackMessage.value = "MP4 rendering cancelled."
        }
        is RenderOutcome.Error -> {
          renderState.value = RenderUiState(isRendering = false)
          errorMessage.value = outcome.message
        }
      }
    }
  }

  fun cancelRender() {
    renderJob?.cancel()
    renderJob = null
    renderState.value = RenderUiState(isRendering = false)
    feedbackMessage.value = "Rendering cancelled."
  }

  fun renameExportedVideo(id: String, newName: String) {
    viewModelScope.launch {
      editorRepository.renameExportedVideo(id, newName)
        .onSuccess { updated ->
          feedbackMessage.value = "Renamed video to ${updated.fileName}."
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to rename video."
        }
    }
  }

  fun deleteExportedVideo(id: String) {
    viewModelScope.launch {
      editorRepository.deleteExportedVideo(id)
      if (renderState.value.lastExportedVideo?.id == id) {
        renderState.update { it.copy(lastExportedVideo = null) }
      }
      feedbackMessage.value = "Exported video deleted."
    }
  }

  fun setError(message: String) {
    errorMessage.value = message
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private fun mutateDraft(transform: (VideoEditorDraft) -> VideoEditorDraft) {
    val current = currentDraft.value ?: VideoEditorDraft(
      id = "standalone_draft",
      projectId = null,
      title = "Untitled Zuzu Edit",
    )
    currentDraft.value = transform(current).copy(updatedAtMillis = System.currentTimeMillis())
    hasUnsavedChanges.value = true
  }

  private data class PlaybackSnapshot(
    val tab: VideoEditorTab,
    val playheadMs: Long,
    val playing: Boolean,
    val dirty: Boolean,
  )
}
