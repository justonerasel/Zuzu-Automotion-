package com.example.data.ai

import com.example.model.ImageProviderType
import com.example.model.StoryboardScene
import com.example.model.StudioAsset
import com.example.model.VideoAspectRatio
import com.example.model.VideoClipProviderType
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.model.VisualStudioStyle
import com.example.model.VoiceGender
import com.example.model.VoiceOption
import com.example.model.VoiceProviderType
import com.example.model.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Non-secret endpoint metadata for modular Voice/Image/Video providers.
 * API keys and server credentials must NEVER be hardcoded in Android source code.
 */
data class MediaProviderEndpointConfig(
  val backendProxyEndpoint: String = "",
  val providerModelId: String = "",
) {
  val isConfigured: Boolean
    get() = backendProxyEndpoint.trim().startsWith("https://")
}

sealed interface MediaProviderStatus {
  data class SetupRequired(
    val providerName: String,
    val message: String,
    val setupSteps: List<String>,
  ) : MediaProviderStatus

  data class Configured(
    val providerName: String,
    val modelLabel: String,
  ) : MediaProviderStatus
}

sealed interface ProviderActionResult<out T> {
  data class Success<T>(val value: T) : ProviderActionResult<T>
  data class SetupRequired(
    val providerName: String,
    val message: String,
    val setupSteps: List<String>,
  ) : ProviderActionResult<Nothing>
  data class Error(val message: String) : ProviderActionResult<Nothing>
}

interface VoiceTtsProvider {
  val providerType: VoiceProviderType
  val status: StateFlow<MediaProviderStatus>

  suspend fun getAvailableVoices(
    language: VideoLanguage,
    gender: VoiceGender,
  ): List<VoiceOption>

  suspend fun previewVoice(
    settings: VoiceSettings,
    sampleText: String,
  ): ProviderActionResult<StudioAsset>

  suspend fun generateSpeech(
    project: VideoProject,
    scriptText: String,
    settings: VoiceSettings,
    targetSceneId: String? = null,
  ): ProviderActionResult<StudioAsset>
}

interface VisualImageProvider {
  val providerType: ImageProviderType
  val status: StateFlow<MediaProviderStatus>

  suspend fun generateSceneImage(
    project: VideoProject,
    scene: StoryboardScene,
    style: VisualStudioStyle,
    prompt: String,
    aspectRatio: VideoAspectRatio,
  ): ProviderActionResult<StudioAsset>
}

interface VisualVideoProvider {
  val providerType: VideoClipProviderType
  val status: StateFlow<MediaProviderStatus>

  suspend fun generateSceneVideoClip(
    project: VideoProject,
    scene: StoryboardScene,
    style: VisualStudioStyle,
    prompt: String,
    aspectRatio: VideoAspectRatio,
  ): ProviderActionResult<StudioAsset>
}

class ModularVoiceTtsProvider(
  override val providerType: VoiceProviderType,
  private val config: MediaProviderEndpointConfig = MediaProviderEndpointConfig(),
) : VoiceTtsProvider {

  private val checklist = when (providerType) {
    VoiceProviderType.GeminiTts -> listOf(
      "Provision a secure HTTPS backend endpoint or Firebase AI Logic TTS bridge for Gemini TTS.",
      "Store server-side Gemini credentials in AI Studio Secrets (never in Android source code).",
      "Provide the HTTPS endpoint in MediaProviderEndpointConfig when backend synthesis activates.",
    )
    VoiceProviderType.ElevenLabs -> listOf(
      "Deploy a secure HTTPS backend proxy that authenticates with the ElevenLabs API server-side.",
      "Store the ElevenLabs API key in backend server secrets (never inside the Android APK).",
      "Return the synthesized audio stream URL or local file reference to the app.",
    )
  }

  private val _status = MutableStateFlow<MediaProviderStatus>(
    if (config.isConfigured) {
      MediaProviderStatus.Configured(providerType.displayName, config.providerModelId.ifBlank { "Standard" })
    } else {
      MediaProviderStatus.SetupRequired(
        providerName = providerType.displayName,
        message = "${providerType.displayName} requires backend API configuration before live speech synthesis. You can import local audio files manually right now.",
        setupSteps = checklist,
      )
    },
  )
  override val status: StateFlow<MediaProviderStatus> = _status.asStateFlow()

  override suspend fun getAvailableVoices(
    language: VideoLanguage,
    gender: VoiceGender,
  ): List<VoiceOption> {
    if (!config.isConfigured) return emptyList()
    return emptyList()
  }

  override suspend fun previewVoice(
    settings: VoiceSettings,
    sampleText: String,
  ): ProviderActionResult<StudioAsset> {
    return ProviderActionResult.SetupRequired(
      providerName = providerType.displayName,
      message = "${providerType.displayName} voice preview requires an active TTS backend configuration. Import a local audio sample to test real playback.",
      setupSteps = checklist,
    )
  }

  override suspend fun generateSpeech(
    project: VideoProject,
    scriptText: String,
    settings: VoiceSettings,
    targetSceneId: String?,
  ): ProviderActionResult<StudioAsset> {
    if (scriptText.isBlank()) {
      return ProviderActionResult.Error("Enter or select script narration text before generating voiceover.")
    }
    return ProviderActionResult.SetupRequired(
      providerName = providerType.displayName,
      message = "${providerType.displayName} speech generation is not configured yet. Use 'Import Audio' to attach a real voiceover file to this project or scene.",
      setupSteps = checklist,
    )
  }
}

class ModularVisualImageProvider(
  override val providerType: ImageProviderType,
  private val config: MediaProviderEndpointConfig = MediaProviderEndpointConfig(),
) : VisualImageProvider {

  private val checklist = listOf(
    "Connect an authorized HTTPS image generation backend (${providerType.displayName}).",
    "Keep provider API keys on the backend server or AI Studio Secrets—never in Android code.",
    "Configure App Check / authentication before calling remote image generation.",
  )

  private val _status = MutableStateFlow<MediaProviderStatus>(
    if (config.isConfigured) {
      MediaProviderStatus.Configured(providerType.displayName, config.providerModelId.ifBlank { "Image Gen" })
    } else {
      MediaProviderStatus.SetupRequired(
        providerName = providerType.displayName,
        message = "${providerType.displayName} is not configured. Import local scene images manually without losing storyboard progress.",
        setupSteps = checklist,
      )
    },
  )
  override val status: StateFlow<MediaProviderStatus> = _status.asStateFlow()

  override suspend fun generateSceneImage(
    project: VideoProject,
    scene: StoryboardScene,
    style: VisualStudioStyle,
    prompt: String,
    aspectRatio: VideoAspectRatio,
  ): ProviderActionResult<StudioAsset> {
    if (prompt.isBlank()) {
      return ProviderActionResult.Error("Provide an Image Prompt for Scene ${scene.sceneNumber} first.")
    }
    return ProviderActionResult.SetupRequired(
      providerName = providerType.displayName,
      message = "${providerType.displayName} image generation requires backend API setup. Use 'Import Visual' to attach an image to Scene ${scene.sceneNumber}.",
      setupSteps = checklist,
    )
  }
}

class ModularVisualVideoProvider(
  override val providerType: VideoClipProviderType,
  private val config: MediaProviderEndpointConfig = MediaProviderEndpointConfig(),
) : VisualVideoProvider {

  private val checklist = listOf(
    "Connect a dedicated HTTPS video-clip generation backend (${providerType.displayName}).",
    "Keep provider credentials server-side and handle async job polling cleanly.",
    "Never embed video provider API tokens in the client APK.",
  )

  private val _status = MutableStateFlow<MediaProviderStatus>(
    if (config.isConfigured) {
      MediaProviderStatus.Configured(providerType.displayName, config.providerModelId.ifBlank { "Video Clip" })
    } else {
      MediaProviderStatus.SetupRequired(
        providerName = providerType.displayName,
        message = "${providerType.displayName} is not configured. You can import MP4/MOV clips manually for any storyboard scene.",
        setupSteps = checklist,
      )
    },
  )
  override val status: StateFlow<MediaProviderStatus> = _status.asStateFlow()

  override suspend fun generateSceneVideoClip(
    project: VideoProject,
    scene: StoryboardScene,
    style: VisualStudioStyle,
    prompt: String,
    aspectRatio: VideoAspectRatio,
  ): ProviderActionResult<StudioAsset> {
    if (prompt.isBlank()) {
      return ProviderActionResult.Error("Provide a Video Prompt for Scene ${scene.sceneNumber} first.")
    }
    return ProviderActionResult.SetupRequired(
      providerName = providerType.displayName,
      message = "${providerType.displayName} video clip generation requires provider setup. Import a local MP4/MOV clip manually for Scene ${scene.sceneNumber}.",
      setupSteps = checklist,
    )
  }
}
