package com.example.model

enum class VoiceGender(val displayLabel: String) {
  Female("Female"),
  Male("Male"),
}

enum class VoiceStudioTone(val displayLabel: String) {
  Professional("Professional"),
  Friendly("Friendly"),
  Emotional("Emotional"),
  Narration("Narration"),
  Energetic("Energetic"),
}

enum class VoiceProviderType(
  val displayName: String,
  val supportsPitch: Boolean,
  val supportsMultipleVoices: Boolean,
) {
  GeminiTts("Gemini TTS", supportsPitch = true, supportsMultipleVoices = true),
  ElevenLabs("ElevenLabs", supportsPitch = false, supportsMultipleVoices = true),
}

data class VoiceOption(
  val id: String,
  val displayName: String,
  val gender: VoiceGender,
  val language: VideoLanguage,
  val provider: VoiceProviderType,
)

data class VoiceSettings(
  val language: VideoLanguage = VideoLanguage.English,
  val gender: VoiceGender = VoiceGender.Female,
  val selectedVoiceId: String? = null,
  val speed: Float = 1.0f,
  val pitch: Float = 1.0f,
  val tone: VoiceStudioTone = VoiceStudioTone.Narration,
)

enum class VisualStudioStyle(val displayLabel: String) {
  TwoDCartoon("2D Cartoon"),
  ThreeDAnimation("3D Animation"),
  Realistic("Realistic"),
  Cinematic("Cinematic"),
  Documentary("Documentary"),
  MotionGraphics("Motion Graphics"),
}

enum class ImageProviderType(val displayName: String) {
  GeminiImagen("Gemini / Imagen"),
  ExternalImageApi("External Image Provider"),
}

enum class VideoClipProviderType(val displayName: String) {
  GoogleVeo("Google Veo"),
  ExternalVideoApi("External Video Clip Provider"),
}

enum class VisualAssetStatus(val displayLabel: String) {
  NotGenerated("Not Generated"),
  SetupRequired("Setup Required"),
  Imported("Imported"),
  Generated("Generated"),
  Error("Error"),
}

enum class StudioAssetCategory(val displayLabel: String) {
  GeneratedVoiceover("Generated Voiceovers"),
  GeneratedImage("Generated Images"),
  GeneratedVideoClip("Generated Video Clips"),
  ImportedAudio("Imported Audio"),
  ImportedVisual("Imported Visual Assets"),
}

data class StudioAsset(
  val id: String,
  val projectId: String? = null,
  val assignedSceneId: String? = null,
  val title: String,
  val category: StudioAssetCategory,
  val mediaType: MediaType,
  val localUriString: String? = null,
  val fileName: String? = null,
  val mimeType: String? = null,
  val fileSizeBytes: Long? = null,
  val durationMs: Long? = null,
  val promptUsed: String = "",
  val visualStyle: VisualStudioStyle? = null,
  val aspectRatio: VideoAspectRatio? = null,
  val language: VideoLanguage? = null,
  val voiceTone: VoiceStudioTone? = null,
  val providerLabel: String = "Local Import",
  val isAiGenerated: Boolean = false,
  val savedToMediaLibrary: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

fun VisualStyle.toStudioStyle(): VisualStudioStyle = when (this) {
  VisualStyle.Realistic -> VisualStudioStyle.Realistic
  VisualStyle.TwoDCartoon -> VisualStudioStyle.TwoDCartoon
  VisualStyle.ThreeDAnimation -> VisualStudioStyle.ThreeDAnimation
  VisualStyle.Cinematic -> VisualStudioStyle.Cinematic
  VisualStyle.Documentary -> VisualStudioStyle.Documentary
}
