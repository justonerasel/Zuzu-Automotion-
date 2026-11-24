package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.motion.MotionExportOutcome
import com.example.data.motion.MotionPostExporter
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.phase78.Phase78Repository
import com.example.data.source.ContentSourceRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.ApprovalContentItem
import com.example.model.ApprovalSourceKind
import com.example.model.ApprovalStatus
import com.example.model.ContentOrigin
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.MotionAnimationStyle
import com.example.model.MotionExportFormat
import com.example.model.MotionMediaSlot
import com.example.model.MotionPlatformPreset
import com.example.model.MotionPostProject
import com.example.model.MotionStickerItem
import com.example.model.MotionTemplateCategory
import com.example.model.MotionTransition
import com.example.model.PendingLocalUpload
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.VideoAspectRatio
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MotionExportUiState(
  val isExporting: Boolean = false,
  val progress: Float = 0f,
  val stageLabel: String = "",
)

data class MotionStudioUiState(
  val activeProject: MotionPostProject = MotionPostProject(
    id = "motion_default",
    title = "Flash Launch Campaign",
  ),
  val savedProjects: List<MotionPostProject> = emptyList(),
  val exportState: MotionExportUiState = MotionExportUiState(),
  val canEncodeMp4: Boolean = true,
  val isPreviewPlaying: Boolean = true,
  val previewProgress: Float = 0.35f,
  val hasUnsavedChanges: Boolean = false,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
)

class MotionStudioViewModel(
  private val repository: Phase78Repository = InMemoryPhase78Repository(),
  private val contentSourceRepository: ContentSourceRepository = InMemoryContentSourceRepository(),
  private val exporter: MotionPostExporter,
  private val exportsDirectoryProvider: () -> File = { File(System.getProperty("java.io.tmpdir"), "zuzu_exports") },
  private val shareUriResolver: (File) -> String = { f -> runCatching { Uri.fromFile(f).toString() }.getOrElse { "file://${f.absolutePath}" } },
) : ViewModel() {

  private val activeProject = MutableStateFlow(
    MotionPostProject(
      id = UUID.randomUUID().toString(),
      title = "Zuzu Promo Launch",
    ),
  )
  private val exportState = MutableStateFlow(MotionExportUiState())
  private val isPreviewPlaying = MutableStateFlow(false)
  private val previewProgress = MutableStateFlow(0.42f)
  private val hasUnsavedChanges = MutableStateFlow(true)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private var exportJob: Job? = null
  private val mp4Capable by lazy { exporter.canEncodeMp4() }

  private val previewStream = combine(
    isPreviewPlaying,
    previewProgress,
    hasUnsavedChanges,
  ) { playing, progress, dirty -> Triple(playing, progress, dirty) }

  private val statusStream = combine(
    exportState,
    feedbackMessage,
    errorMessage,
  ) { exportUi, feedback, error -> Triple(exportUi, feedback, error) }

  val uiState: StateFlow<MotionStudioUiState> = combine(
    activeProject,
    repository.motionProjects,
    previewStream,
    statusStream,
  ) { project, saved, (playing, progress, dirty), (exportUi, feedback, error) ->
    MotionStudioUiState(
      activeProject = project,
      savedProjects = saved,
      exportState = exportUi,
      canEncodeMp4 = mp4Capable,
      isPreviewPlaying = playing,
      previewProgress = progress.coerceIn(0f, 1f),
      hasUnsavedChanges = dirty,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = MotionStudioUiState(canEncodeMp4 = mp4Capable),
  )

  fun applyTemplate(category: MotionTemplateCategory) {
    mutate { p ->
      p.copy(
        templateCategory = category,
        animationStyle = category.defaultStyle,
        headlineText = category.defaultHeadline,
        subheadlineText = category.defaultSubheadline,
        primaryAccentArgb = category.primaryArgb,
        secondaryAccentArgb = category.secondaryArgb,
      )
    }
    feedbackMessage.value = "Applied '${category.displayLabel}' motion template."
  }

  fun createNewProject(category: MotionTemplateCategory = MotionTemplateCategory.PromotionalPosts) {
    activeProject.value = MotionPostProject(
      id = UUID.randomUUID().toString(),
      title = "${category.displayLabel} Post",
      templateCategory = category,
      animationStyle = category.defaultStyle,
      headlineText = category.defaultHeadline,
      subheadlineText = category.defaultSubheadline,
      primaryAccentArgb = category.primaryArgb,
      secondaryAccentArgb = category.secondaryArgb,
    )
    hasUnsavedChanges.value = true
    previewProgress.value = 0.4f
    feedbackMessage.value = "Started fresh motion project (${category.displayLabel})."
    errorMessage.value = null
  }

  fun loadSavedProject(project: MotionPostProject) {
    activeProject.value = project
    hasUnsavedChanges.value = false
    previewProgress.value = 0.4f
    feedbackMessage.value = "Loaded motion project '${project.title}'."
    errorMessage.value = null
  }

  fun updateTitle(title: String) {
    mutate { it.copy(title = title) }
  }

  fun updateCopy(headline: String, subheadline: String) {
    mutate { it.copy(headlineText = headline, subheadlineText = subheadline) }
  }

  fun updateTextStyle(colorArgb: Long, fontSizeSp: Int) {
    mutate { it.copy(textColorArgb = colorArgb, textFontSizeSp = fontSizeSp.coerceIn(14, 44)) }
  }

  fun selectPlatformPreset(preset: MotionPlatformPreset) {
    mutate {
      it.copy(
        platformPreset = preset,
        aspectRatio = preset.defaultAspectRatio,
      )
    }
    feedbackMessage.value = "Formatted for ${preset.displayLabel} (${preset.defaultAspectRatio.label})."
  }

  fun setAspectRatio(ratio: VideoAspectRatio) {
    mutate { it.copy(aspectRatio = ratio) }
  }

  fun setAnimationStyle(style: MotionAnimationStyle) {
    mutate { it.copy(animationStyle = style) }
  }

  fun setTransition(transition: MotionTransition) {
    mutate { it.copy(transition = transition) }
  }

  fun setDurationSeconds(seconds: Int) {
    mutate { it.copy(durationSeconds = seconds.coerceIn(3, 30)) }
  }

  fun setLogoBadge(label: String, uriString: String? = activeProject.value.logoUriString) {
    mutate { it.copy(logoLabel = label.take(12), logoUriString = uriString) }
  }

  fun addMediaSlot(
    title: String,
    mediaType: MediaType,
    uriString: String,
    sourceMediaId: String? = null,
  ) {
    if (mediaType != MediaType.Image && mediaType != MediaType.Video) {
      errorMessage.value = "Select an image (JPG/PNG) or video clip (MP4/MOV) for motion framing."
      return
    }
    val slot = MotionMediaSlot(
      id = UUID.randomUUID().toString(),
      title = title.ifBlank { "Visual Asset" },
      mediaType = mediaType,
      uriString = uriString,
      sourceMediaId = sourceMediaId,
    )
    mutate { it.copy(mediaSlots = it.mediaSlots + slot) }
    feedbackMessage.value = "Added '$title' to motion project."
    errorMessage.value = null
  }

  fun addMediaFromUpload(upload: PendingLocalUpload) {
    addMediaSlot(
      title = upload.fileName.substringBeforeLast('.'),
      mediaType = upload.mediaType,
      uriString = upload.uriString,
      sourceMediaId = null,
    )
  }

  fun addMediaFromLibrary(entry: MediaLibraryEntry) {
    val uri = entry.localUriString
    if (uri.isNullOrBlank()) {
      errorMessage.value = "Selected library item has no local image or video file URI."
      return
    }
    addMediaSlot(
      title = entry.title,
      mediaType = entry.mediaType,
      uriString = uri,
      sourceMediaId = entry.id,
    )
  }

  fun removeMediaSlot(slotId: String) {
    mutate { it.copy(mediaSlots = it.mediaSlots.filterNot { s -> s.id == slotId }) }
  }

  fun addSticker(label: String, badgeSymbol: String) {
    val cleanLabel = label.trim().ifEmpty { return }
    val sticker = MotionStickerItem(
      id = UUID.randomUUID().toString(),
      label = cleanLabel,
      badgeSymbol = badgeSymbol.ifBlank { "★" },
    )
    mutate { it.copy(stickers = it.stickers + sticker) }
    feedbackMessage.value = "Added sticker '$cleanLabel'."
  }

  fun removeSticker(stickerId: String) {
    mutate { it.copy(stickers = it.stickers.filterNot { s -> s.id == stickerId }) }
  }

  fun setBackgroundMusic(title: String, uriString: String) {
    mutate { it.copy(backgroundMusicTitle = title, backgroundMusicUriString = uriString) }
    feedbackMessage.value = "Attached background audio '$title'."
  }

  fun setMusicVolume(volume: Float) {
    mutate { it.copy(backgroundMusicVolume = volume.coerceIn(0f, 1f)) }
  }

  fun removeBackgroundMusic() {
    mutate { it.copy(backgroundMusicTitle = null, backgroundMusicUriString = null) }
  }

  fun setPreviewPlaying(playing: Boolean) {
    isPreviewPlaying.value = playing
  }

  fun setPreviewProgress(progress: Float) {
    previewProgress.value = progress.coerceIn(0f, 1f)
  }

  fun saveProject() {
    val project = activeProject.value
    if (project.title.isBlank()) {
      errorMessage.value = "Enter a motion project title before saving."
      return
    }
    viewModelScope.launch {
      val saved = repository.saveMotionProject(project)
      activeProject.value = saved
      hasUnsavedChanges.value = false
      feedbackMessage.value = "Motion post '${saved.title}' saved locally."
      errorMessage.value = null
    }
  }

  fun deleteSavedProject(projectId: String) {
    viewModelScope.launch {
      repository.deleteMotionProject(projectId)
      feedbackMessage.value = "Motion project deleted."
    }
  }

  fun exportMotionPost(format: MotionExportFormat) {
    if (exportJob?.isActive == true) return
    val current = activeProject.value
    if (current.headlineText.isBlank() && current.mediaSlots.isEmpty()) {
      errorMessage.value = "Add headline text or media before exporting."
      return
    }

    clearMessages()
    exportJob = viewModelScope.launch {
      val saved = repository.saveMotionProject(current)
      activeProject.value = saved
      hasUnsavedChanges.value = false

      exportState.value = MotionExportUiState(
        isExporting = true,
        progress = 0.02f,
        stageLabel = "Preparing ${format.displayLabel} export...",
      )

      val dir = exportsDirectoryProvider().apply { mkdirs() }
      val safeName = saved.title.replace(Regex("[^A-Za-z0-9_\\-]"), "_").trim('_').take(24).ifEmpty { "zuzu_motion" }
      val outFile = File(dir, "${safeName}_motion_${System.currentTimeMillis() % 10_000}.${format.extension}")

      when (
        val outcome = exporter.exportMotionPost(
          project = saved,
          format = format,
          outputFile = outFile,
          onProgress = { frac, stage ->
            exportState.value = MotionExportUiState(
              isExporting = true,
              progress = frac.coerceIn(0f, 1f),
              stageLabel = stage,
            )
          },
        )
      ) {
        is MotionExportOutcome.Success -> {
          val uriStr = shareUriResolver(outcome.outputFile)
          val mediaType = if (format == MotionExportFormat.Mp4Video) MediaType.Video else MediaType.Image
          val permissionStatus = resolveInheritedPermission(saved)
          contentSourceRepository.saveLocalUpload(
            upload = PendingLocalUpload(
              uriString = uriStr,
              fileName = outcome.outputFile.name,
              mimeType = format.mimeType,
              mediaType = mediaType,
              fileSizeBytes = outcome.fileSizeBytes,
              durationMs = outcome.durationMs,
            ),
            title = saved.title,
            description = "Auto Motion Post (${saved.templateCategory.displayLabel})",
            permission = permissionStatus,
            notes = "Exported ${format.displayLabel} • ${outcome.width}x${outcome.height}",
          )

          val updated = repository.saveMotionProject(
            saved.copy(
              exportedFilePath = outcome.outputFile.absolutePath,
              exportedFileUriString = uriStr,
              exportedFormat = format,
            ),
          )
          activeProject.value = updated
          exportState.value = MotionExportUiState(isExporting = false, progress = 1f, stageLabel = "Exported")
          feedbackMessage.value = "Exported ${outcome.outputFile.name} & saved to Media Library."
        }
        is MotionExportOutcome.Cancelled -> {
          exportState.value = MotionExportUiState(isExporting = false)
          feedbackMessage.value = "Motion export cancelled."
        }
        is MotionExportOutcome.Error -> {
          exportState.value = MotionExportUiState(isExporting = false)
          errorMessage.value = outcome.message
        }
      }
    }
  }

  fun cancelExport() {
    exportJob?.cancel()
    exportJob = null
    exportState.value = MotionExportUiState(isExporting = false)
    feedbackMessage.value = "Motion export cancelled."
  }

  /**
   * Sends the current Motion Studio project to Content Approval Center with explicit user review required.
   */
  fun sendToApprovalCenter(onQueued: () -> Unit = {}) {
    viewModelScope.launch {
      val current = repository.saveMotionProject(activeProject.value)
      activeProject.value = current
      hasUnsavedChanges.value = false

      val linkedMediaId = current.mediaSlots.firstOrNull { !it.sourceMediaId.isNullOrBlank() }?.sourceMediaId
      val linkedEntry = linkedMediaId?.let { id ->
        contentSourceRepository.mediaEntries.first().firstOrNull { it.id == id }
      }

      val origin = linkedEntry?.origin ?: ContentOrigin.MyUploads
      val perm = linkedEntry?.permission ?: PermissionRecord(
        status = PermissionStatus.OriginalContent,
        contentOwner = "Zuzu Creator",
        evidenceDescription = "Created in Auto Motion Post Studio",
        approvedForReuse = true,
      )

      val item = ApprovalContentItem(
        id = "apr_motion_${current.id}",
        sourceKind = ApprovalSourceKind.MotionPost,
        sourceId = current.id,
        title = current.title,
        description = "${current.headlineText}\n${current.subheadlineText}".trim(),
        hashtagsCsv = current.platformPreset.defaultHashtags,
        mediaUriString = current.exportedFileUriString ?: current.mediaSlots.firstOrNull()?.uriString,
        mediaFilePath = current.exportedFilePath,
        sourceMediaEntryId = linkedEntry?.id,
        aspectRatio = current.aspectRatio,
        targetPlatforms = listOf(current.platformPreset.platform),
        permissionOrigin = origin,
        permissionStatus = perm.status,
        permissionOwner = perm.contentOwner,
        permissionEvidence = perm.evidenceDescription.ifBlank { perm.licenseReference },
        permissionApprovedForReuse = perm.approvedForReuse,
        status = ApprovalStatus.PendingReview,
      )
      repository.saveApprovalItem(item)
      feedbackMessage.value = "Submitted '${current.title}' to Approval Center for review."
      onQueued()
    }
  }

  private suspend fun resolveInheritedPermission(project: MotionPostProject): PermissionRecord {
    val linkedMediaId = project.mediaSlots.firstOrNull { !it.sourceMediaId.isNullOrBlank() }?.sourceMediaId
    val entry = linkedMediaId?.let { id ->
      contentSourceRepository.mediaEntries.first().firstOrNull { it.id == id }
    }
    return entry?.permission ?: PermissionRecord(
      status = PermissionStatus.OriginalContent,
      contentOwner = "Zuzu Creator",
      evidenceDescription = "Generated in Auto Motion Post Studio",
      approvedForReuse = true,
    )
  }

  fun setError(message: String) {
    errorMessage.value = message
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private fun mutate(transform: (MotionPostProject) -> MotionPostProject) {
    activeProject.value = transform(activeProject.value).copy(updatedAtMillis = System.currentTimeMillis())
    hasUnsavedChanges.value = true
  }
}
