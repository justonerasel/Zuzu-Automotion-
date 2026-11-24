package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.MediaProviderStatus
import com.example.data.ai.ModularVisualImageProvider
import com.example.data.ai.ModularVisualVideoProvider
import com.example.data.ai.ModularVoiceTtsProvider
import com.example.data.ai.ProviderActionResult
import com.example.data.ai.VisualImageProvider
import com.example.data.ai.VisualVideoProvider
import com.example.data.ai.VoiceTtsProvider
import com.example.data.factory.InMemoryStudioAssetRepository
import com.example.data.factory.InMemoryVideoFactoryRepository
import com.example.data.factory.StudioAssetRepository
import com.example.data.factory.VideoFactoryRepository
import com.example.data.source.ContentSourceRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.ImageProviderType
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.StoryboardScene
import com.example.model.StudioAsset
import com.example.model.StudioAssetCategory
import com.example.model.VideoAspectRatio
import com.example.model.VideoClipProviderType
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.model.VisualAssetStatus
import com.example.model.VisualStudioStyle
import com.example.model.VoiceGender
import com.example.model.VoiceOption
import com.example.model.VoiceProviderType
import com.example.model.VoiceSettings
import com.example.model.VoiceStudioTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderSetupNotice(
  val providerName: String,
  val message: String,
  val setupSteps: List<String>,
)

data class VoiceVisualUiState(
  val selectedVoiceProvider: VoiceProviderType = VoiceProviderType.GeminiTts,
  val voiceProviderStatus: MediaProviderStatus = MediaProviderStatus.SetupRequired(
    providerName = VoiceProviderType.GeminiTts.displayName,
    message = "",
    setupSteps = emptyList(),
  ),
  val availableVoices: List<VoiceOption> = emptyList(),
  val voiceSettings: VoiceSettings = VoiceSettings(),
  val selectedImageProvider: ImageProviderType = ImageProviderType.GeminiImagen,
  val imageProviderStatus: MediaProviderStatus = MediaProviderStatus.SetupRequired(
    providerName = ImageProviderType.GeminiImagen.displayName,
    message = "",
    setupSteps = emptyList(),
  ),
  val selectedVideoProvider: VideoClipProviderType = VideoClipProviderType.GoogleVeo,
  val videoProviderStatus: MediaProviderStatus = MediaProviderStatus.SetupRequired(
    providerName = VideoClipProviderType.GoogleVeo.displayName,
    message = "",
    setupSteps = emptyList(),
  ),
  val selectedVisualStyle: VisualStudioStyle = VisualStudioStyle.Cinematic,
  val selectedAspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
  val sceneStatusOverrides: Map<String, VisualAssetStatus> = emptyMap(),
  val allAssets: List<StudioAsset> = emptyList(),
  val assetSearchQuery: String = "",
  val assetCategoryFilter: StudioAssetCategory? = null,
  val setupNotice: ProviderSetupNotice? = null,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val filteredAssets: List<StudioAsset>
    get() {
      val q = assetSearchQuery.trim().lowercase()
      return allAssets.filter { asset ->
        val matchesCategory = assetCategoryFilter == null || asset.category == assetCategoryFilter
        val matchesQuery = q.isEmpty() ||
          asset.title.lowercase().contains(q) ||
          asset.providerLabel.lowercase().contains(q) ||
          (asset.fileName?.lowercase()?.contains(q) == true)
        matchesCategory && matchesQuery
      }
    }

  val audioAssets: List<StudioAsset>
    get() = allAssets.filter { it.mediaType == MediaType.Audio }

  val visualAssets: List<StudioAsset>
    get() = allAssets.filter { it.mediaType == MediaType.Image || it.mediaType == MediaType.Video }
}

class VoiceVisualViewModel(
  private val videoFactoryRepository: VideoFactoryRepository = InMemoryVideoFactoryRepository(),
  private val studioAssetRepository: StudioAssetRepository = InMemoryStudioAssetRepository(),
  private val contentSourceRepository: ContentSourceRepository = InMemoryContentSourceRepository(),
  private val voiceProviders: Map<VoiceProviderType, VoiceTtsProvider> = mapOf(
    VoiceProviderType.GeminiTts to ModularVoiceTtsProvider(VoiceProviderType.GeminiTts),
    VoiceProviderType.ElevenLabs to ModularVoiceTtsProvider(VoiceProviderType.ElevenLabs),
  ),
  private val imageProviders: Map<ImageProviderType, VisualImageProvider> = mapOf(
    ImageProviderType.GeminiImagen to ModularVisualImageProvider(ImageProviderType.GeminiImagen),
    ImageProviderType.ExternalImageApi to ModularVisualImageProvider(ImageProviderType.ExternalImageApi),
  ),
  private val videoProviders: Map<VideoClipProviderType, VisualVideoProvider> = mapOf(
    VideoClipProviderType.GoogleVeo to ModularVisualVideoProvider(VideoClipProviderType.GoogleVeo),
    VideoClipProviderType.ExternalVideoApi to ModularVisualVideoProvider(VideoClipProviderType.ExternalVideoApi),
  ),
) : ViewModel() {

  private val selectedVoiceProvider = MutableStateFlow(VoiceProviderType.GeminiTts)
  private val voiceSettings = MutableStateFlow(VoiceSettings())
  private val availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())

  private val selectedImageProvider = MutableStateFlow(ImageProviderType.GeminiImagen)
  private val selectedVideoProvider = MutableStateFlow(VideoClipProviderType.GoogleVeo)
  private val selectedVisualStyle = MutableStateFlow(VisualStudioStyle.Cinematic)
  private val selectedAspectRatio = MutableStateFlow(VideoAspectRatio.Vertical)
  private val sceneStatusOverrides = MutableStateFlow<Map<String, VisualAssetStatus>>(emptyMap())

  private val assetSearchQuery = MutableStateFlow("")
  private val assetCategoryFilter = MutableStateFlow<StudioAssetCategory?>(null)
  private val setupNotice = MutableStateFlow<ProviderSetupNotice?>(null)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private val voiceConfigStream = combine(
    selectedVoiceProvider,
    voiceSettings,
    availableVoices,
  ) { provider, settings, voices ->
    Triple(provider, settings, voices)
  }

  private val visualConfigStream = combine(
    selectedImageProvider,
    selectedVideoProvider,
    selectedVisualStyle,
    selectedAspectRatio,
    sceneStatusOverrides,
  ) { imgProv, vidProv, style, ratio, overrides ->
    VisualConfigSnapshot(imgProv, vidProv, style, ratio, overrides)
  }

  private val libraryStream = combine(
    studioAssetRepository.assets,
    assetSearchQuery,
    assetCategoryFilter,
  ) { assets, query, cat ->
    Triple(assets, query, cat)
  }

  private val messageStream = combine(
    setupNotice,
    feedbackMessage,
    errorMessage,
  ) { setup, feedback, error ->
    Triple(setup, feedback, error)
  }

  val uiState: StateFlow<VoiceVisualUiState> = combine(
    voiceConfigStream,
    visualConfigStream,
    libraryStream,
    messageStream,
  ) { (vProvider, vSettings, voices), visConfig, (assets, query, category), (setup, feedback, error) ->
    val currentVoiceProvider = voiceProviders[vProvider]
    val currentImageProvider = imageProviders[visConfig.imageProvider]
    val currentVideoProvider = videoProviders[visConfig.videoProvider]
    VoiceVisualUiState(
      selectedVoiceProvider = vProvider,
      voiceProviderStatus = currentVoiceProvider?.status?.value ?: unconfiguredStatus(vProvider.displayName),
      availableVoices = voices,
      voiceSettings = vSettings,
      selectedImageProvider = visConfig.imageProvider,
      imageProviderStatus = currentImageProvider?.status?.value
        ?: unconfiguredStatus(visConfig.imageProvider.displayName),
      selectedVideoProvider = visConfig.videoProvider,
      videoProviderStatus = currentVideoProvider?.status?.value
        ?: unconfiguredStatus(visConfig.videoProvider.displayName),
      selectedVisualStyle = visConfig.style,
      selectedAspectRatio = visConfig.aspectRatio,
      sceneStatusOverrides = visConfig.sceneStatusOverrides,
      allAssets = assets,
      assetSearchQuery = query,
      assetCategoryFilter = category,
      setupNotice = setup,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = VoiceVisualUiState(),
  )

  // --- Voice Controls ---

  fun selectVoiceProvider(providerType: VoiceProviderType) {
    selectedVoiceProvider.value = providerType
    refreshVoices()
  }

  fun setVoiceLanguage(language: VideoLanguage) {
    voiceSettings.update { it.copy(language = language, selectedVoiceId = null) }
    refreshVoices()
  }

  fun setVoiceGender(gender: VoiceGender) {
    voiceSettings.update { it.copy(gender = gender, selectedVoiceId = null) }
    refreshVoices()
  }

  fun setVoiceSpeed(speed: Float) {
    voiceSettings.update { it.copy(speed = speed.coerceIn(0.5f, 2.0f)) }
  }

  fun setVoicePitch(pitch: Float) {
    voiceSettings.update { it.copy(pitch = pitch.coerceIn(0.5f, 2.0f)) }
  }

  fun setVoiceTone(tone: VoiceStudioTone) {
    voiceSettings.update { it.copy(tone = tone) }
  }

  fun selectVoiceOption(voiceId: String?) {
    voiceSettings.update { it.copy(selectedVoiceId = voiceId) }
  }

  private fun refreshVoices() {
    val provider = voiceProviders[selectedVoiceProvider.value] ?: return
    val settings = voiceSettings.value
    viewModelScope.launch {
      availableVoices.value = provider.getAvailableVoices(settings.language, settings.gender)
    }
  }

  fun previewVoice(sampleText: String) {
    val provider = voiceProviders[selectedVoiceProvider.value] ?: return
    viewModelScope.launch {
      when (val res = provider.previewVoice(voiceSettings.value, sampleText)) {
        is ProviderActionResult.Success -> {
          feedbackMessage.value = "Voice preview ready."
        }
        is ProviderActionResult.SetupRequired -> {
          setupNotice.value = ProviderSetupNotice(res.providerName, res.message, res.setupSteps)
        }
        is ProviderActionResult.Error -> {
          errorMessage.value = res.message
        }
      }
    }
  }

  fun generateVoiceFromScript(
    project: VideoProject,
    scriptText: String,
    targetSceneId: String? = null,
    isRegenerate: Boolean = false,
  ) {
    val provider = voiceProviders[selectedVoiceProvider.value] ?: return
    viewModelScope.launch {
      when (
        val res = provider.generateSpeech(
          project = project,
          scriptText = scriptText,
          settings = voiceSettings.value,
          targetSceneId = targetSceneId,
        )
      ) {
        is ProviderActionResult.Success -> {
          val saved = studioAssetRepository.saveAsset(res.value)
          if (targetSceneId != null) {
            assignVoiceAssetToScene(project, targetSceneId, saved.id)
          }
          feedbackMessage.value = if (isRegenerate) "Voiceover regenerated and saved." else "Voiceover generated and saved."
        }
        is ProviderActionResult.SetupRequired -> {
          setupNotice.value = ProviderSetupNotice(res.providerName, res.message, res.setupSteps)
        }
        is ProviderActionResult.Error -> {
          errorMessage.value = res.message
        }
      }
    }
  }

  fun importAudioFile(
    upload: PendingLocalUpload,
    project: VideoProject?,
    targetSceneId: String?,
    title: String = "",
  ) {
    val settings = voiceSettings.value
    viewModelScope.launch {
      studioAssetRepository.importLocalAudio(
        upload = upload,
        projectId = project?.id,
        assignedSceneId = targetSceneId,
        language = settings.language,
        tone = settings.tone,
        title = title,
      ).onSuccess { saved ->
        if (project != null && targetSceneId != null) {
          assignVoiceAssetToScene(project, targetSceneId, saved.id)
        }
        feedbackMessage.value = "Imported audio '${saved.title}' into Asset Library."
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to import audio file."
      }
    }
  }

  fun assignVoiceAssetToScene(
    project: VideoProject,
    sceneId: String,
    assetId: String,
  ) {
    val updatedScenes = project.scenes.map { scene ->
      if (scene.id == sceneId) scene.copy(assignedVoiceAssetId = assetId) else scene
    }
    viewModelScope.launch {
      studioAssetRepository.assignAssetToScene(assetId, project.id, sceneId)
      videoFactoryRepository.saveStoryboard(project.id, updatedScenes)
      feedbackMessage.value = "Assigned voiceover to scene."
      errorMessage.value = null
    }
  }

  // --- Visual Studio Controls ---

  fun selectImageProvider(providerType: ImageProviderType) {
    selectedImageProvider.value = providerType
  }

  fun selectVideoProvider(providerType: VideoClipProviderType) {
    selectedVideoProvider.value = providerType
  }

  fun selectVisualStyle(style: VisualStudioStyle) {
    selectedVisualStyle.value = style
  }

  fun selectAspectRatio(ratio: VideoAspectRatio) {
    selectedAspectRatio.value = ratio
  }

  fun updateScenePrompts(
    project: VideoProject,
    sceneId: String,
    imagePrompt: String,
    videoPrompt: String,
  ) {
    val updated = project.scenes.map { scene ->
      if (scene.id == sceneId) {
        scene.copy(
          imageGenerationPrompt = imagePrompt.trim(),
          videoGenerationPrompt = videoPrompt.trim(),
        )
      } else {
        scene
      }
    }
    viewModelScope.launch {
      videoFactoryRepository.saveStoryboard(project.id, updated)
      feedbackMessage.value = "Updated prompts for scene."
    }
  }

  fun generateSceneImage(
    project: VideoProject,
    scene: StoryboardScene,
    promptOverride: String? = null,
  ) {
    val provider = imageProviders[selectedImageProvider.value] ?: return
    val prompt = (promptOverride ?: scene.imageGenerationPrompt).trim()
    viewModelScope.launch {
      when (
        val res = provider.generateSceneImage(
          project = project,
          scene = scene,
          style = selectedVisualStyle.value,
          prompt = prompt,
          aspectRatio = selectedAspectRatio.value,
        )
      ) {
        is ProviderActionResult.Success -> {
          val saved = studioAssetRepository.saveAsset(res.value)
          assignVisualAssetToScene(project, scene.id, saved.id)
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.Generated) }
          feedbackMessage.value = "Scene ${scene.sceneNumber} image generated."
        }
        is ProviderActionResult.SetupRequired -> {
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.SetupRequired) }
          setupNotice.value = ProviderSetupNotice(res.providerName, res.message, res.setupSteps)
        }
        is ProviderActionResult.Error -> {
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.Error) }
          errorMessage.value = res.message
        }
      }
    }
  }

  fun generateSceneVideoClip(
    project: VideoProject,
    scene: StoryboardScene,
    promptOverride: String? = null,
  ) {
    val provider = videoProviders[selectedVideoProvider.value] ?: return
    val prompt = (promptOverride ?: scene.videoGenerationPrompt).trim()
    viewModelScope.launch {
      when (
        val res = provider.generateSceneVideoClip(
          project = project,
          scene = scene,
          style = selectedVisualStyle.value,
          prompt = prompt,
          aspectRatio = selectedAspectRatio.value,
        )
      ) {
        is ProviderActionResult.Success -> {
          val saved = studioAssetRepository.saveAsset(res.value)
          assignVisualAssetToScene(project, scene.id, saved.id)
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.Generated) }
          feedbackMessage.value = "Scene ${scene.sceneNumber} video clip generated."
        }
        is ProviderActionResult.SetupRequired -> {
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.SetupRequired) }
          setupNotice.value = ProviderSetupNotice(res.providerName, res.message, res.setupSteps)
        }
        is ProviderActionResult.Error -> {
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.Error) }
          errorMessage.value = res.message
        }
      }
    }
  }

  fun importVisualAsset(
    upload: PendingLocalUpload,
    project: VideoProject?,
    scene: StoryboardScene?,
  ) {
    viewModelScope.launch {
      studioAssetRepository.importLocalVisual(
        upload = upload,
        projectId = project?.id,
        assignedSceneId = scene?.id,
        style = selectedVisualStyle.value,
        aspectRatio = project?.aspectRatio ?: selectedAspectRatio.value,
        promptUsed = scene?.imageGenerationPrompt.orEmpty(),
        title = if (scene != null) "Scene ${scene.sceneNumber} Visual" else "",
      ).onSuccess { saved ->
        if (project != null && scene != null) {
          assignVisualAssetToScene(project, scene.id, saved.id)
          sceneStatusOverrides.update { it + (scene.id to VisualAssetStatus.Imported) }
        }
        feedbackMessage.value = "Imported visual '${saved.title}' into Asset Library."
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to import visual asset."
      }
    }
  }

  fun assignVisualAssetToScene(
    project: VideoProject,
    sceneId: String,
    assetId: String,
  ) {
    val updatedScenes = project.scenes.map { scene ->
      if (scene.id == sceneId) scene.copy(assignedVisualAssetId = assetId) else scene
    }
    viewModelScope.launch {
      studioAssetRepository.assignAssetToScene(assetId, project.id, sceneId)
      videoFactoryRepository.saveStoryboard(project.id, updatedScenes)
      feedbackMessage.value = "Assigned visual asset to scene."
      errorMessage.value = null
    }
  }

  fun saveAssetToMediaLibrary(asset: StudioAsset) {
    val uriStr = asset.localUriString
    if (uriStr.isNullOrBlank()) {
      errorMessage.value = "No local media file reference available to save to Media Library."
      return
    }
    viewModelScope.launch {
      contentSourceRepository.saveLocalUpload(
        upload = PendingLocalUpload(
          uriString = uriStr,
          fileName = asset.fileName ?: "${asset.title}.dat",
          mimeType = asset.mimeType ?: "application/octet-stream",
          mediaType = asset.mediaType,
          fileSizeBytes = asset.fileSizeBytes ?: 0L,
          durationMs = asset.durationMs,
        ),
        title = asset.title,
        description = "${asset.category.displayLabel} • ${asset.providerLabel}",
        permission = PermissionRecord(
          status = PermissionStatus.OriginalContent,
          contentOwner = "Zuzu Studio Creator",
          evidenceDescription = "Created or imported inside Zuzu AI Studio",
          approvedForReuse = true,
        ),
        notes = asset.promptUsed,
      ).onSuccess {
        studioAssetRepository.markSavedToMediaLibrary(asset.id)
        feedbackMessage.value = "Saved '${asset.title}' to Content Source Media Library."
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to save asset to Media Library."
      }
    }
  }

  // --- Asset Library Controls ---

  fun updateAssetSearchQuery(query: String) {
    assetSearchQuery.value = query
  }

  fun updateAssetCategoryFilter(category: StudioAssetCategory?) {
    assetCategoryFilter.value = category
  }

  fun deleteAsset(assetId: String, activeProject: VideoProject?) {
    viewModelScope.launch {
      studioAssetRepository.deleteAsset(assetId)
      if (activeProject != null) {
        val cleaned = activeProject.scenes.map { scene ->
          scene.copy(
            assignedVoiceAssetId = if (scene.assignedVoiceAssetId == assetId) null else scene.assignedVoiceAssetId,
            assignedVisualAssetId = if (scene.assignedVisualAssetId == assetId) null else scene.assignedVisualAssetId,
          )
        }
        if (cleaned != activeProject.scenes) {
          videoFactoryRepository.saveStoryboard(activeProject.id, cleaned)
        }
      }
      feedbackMessage.value = "Asset deleted."
    }
  }

  fun setError(message: String) {
    errorMessage.value = message
  }

  fun dismissSetupNotice() {
    setupNotice.value = null
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private fun unconfiguredStatus(name: String): MediaProviderStatus =
    MediaProviderStatus.SetupRequired(
      providerName = name,
      message = "$name requires API backend configuration.",
      setupSteps = emptyList(),
    )

  private data class VisualConfigSnapshot(
    val imageProvider: ImageProviderType,
    val videoProvider: VideoClipProviderType,
    val style: VisualStudioStyle,
    val aspectRatio: VideoAspectRatio,
    val sceneStatusOverrides: Map<String, VisualAssetStatus>,
  )
}
