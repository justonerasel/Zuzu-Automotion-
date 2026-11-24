package com.example.model

import com.squareup.moshi.JsonClass

enum class VideoEditorTab(val displayLabel: String, val testTag: String) {
  Timeline("Timeline", "editor_tab_timeline"),
  Subtitles("Subtitles", "editor_tab_subtitles"),
  Audio("Audio", "editor_tab_audio"),
  Export("Export", "editor_tab_export"),
}

enum class EditorClipType(val displayLabel: String) {
  Image("Image"),
  Video("Video Clip"),
  TitleCard("Scene Card"),
}

enum class EditorTransition(val displayLabel: String) {
  None("Cut (None)"),
  CrossFade("Cross Fade"),
  SlideLeft("Slide Left"),
  ZoomIn("Zoom In"),
}

enum class EditorVisualEffect(val displayLabel: String) {
  None("Original"),
  WarmGlow("Warm Glow"),
  HighContrast("High Contrast"),
  SoftMono("Soft Mono"),
  CinematicVignette("Vignette"),
}

@JsonClass(generateAdapter = true)
data class EditorTimelineClip(
  val id: String,
  val sourceSceneId: String? = null,
  val title: String,
  val clipType: EditorClipType = EditorClipType.TitleCard,
  val mediaUriString: String? = null,
  val assetId: String? = null,
  val trimStartMs: Long = 0L,
  val durationMs: Long = 5_000L,
  val transition: EditorTransition = EditorTransition.CrossFade,
  val visualEffect: EditorVisualEffect = EditorVisualEffect.None,
  val notes: String = "",
)

enum class EditorAudioTrackType(val displayLabel: String) {
  Voiceover("Voiceover"),
  BackgroundMusic("Background Music"),
}

@JsonClass(generateAdapter = true)
data class EditorAudioTrack(
  val id: String,
  val trackType: EditorAudioTrackType,
  val title: String,
  val uriString: String,
  val startOffsetMs: Long = 0L,
  val trimStartMs: Long = 0L,
  val trimEndMs: Long = 0L,
  val sourceDurationMs: Long = 0L,
  val volume: Float = if (trackType == EditorAudioTrackType.Voiceover) 1.0f else 0.35f,
  val isMuted: Boolean = false,
)

enum class SubtitlePosition(val displayLabel: String) {
  Top("Top"),
  Middle("Middle"),
  Bottom("Bottom"),
}

enum class SubtitleBackgroundStyle(val displayLabel: String) {
  None("None"),
  DarkTranslucent("Dark Pill"),
  PurpleGlass("Zuzu Purple"),
  SolidBlack("Solid Black"),
  SolidWhite("Solid White"),
}

@JsonClass(generateAdapter = true)
data class SubtitleCue(
  val id: String,
  val text: String,
  val language: VideoLanguage = VideoLanguage.English,
  val startMs: Long = 0L,
  val endMs: Long = 4_000L,
  val fontSizeSp: Int = 18,
  val position: SubtitlePosition = SubtitlePosition.Bottom,
  val textColorArgb: Long = 0xFFFFFFFF,
  val backgroundStyle: SubtitleBackgroundStyle = SubtitleBackgroundStyle.DarkTranslucent,
)

enum class OverlayPosition(val displayLabel: String) {
  Top("Top"),
  Center("Center"),
  Bottom("Bottom"),
}

@JsonClass(generateAdapter = true)
data class TextOverlay(
  val id: String,
  val text: String,
  val startMs: Long = 0L,
  val endMs: Long = 5_000L,
  val fontSizeSp: Int = 22,
  val position: OverlayPosition = OverlayPosition.Top,
  val colorArgb: Long = 0xFFFFFFFF,
)

enum class ExportResolution(val displayLabel: String, val shortSidePx: Int) {
  Hd720("720p HD", 720),
  FullHd1080("1080p Full HD", 1080),
}

enum class ExportQuality(
  val displayLabel: String,
  val bitrate720Bps: Int,
  val bitrate1080Bps: Int,
  val audioBitrateBps: Int,
  val fps: Int,
) {
  Standard(
    displayLabel = "Standard",
    bitrate720Bps = 3_500_000,
    bitrate1080Bps = 6_000_000,
    audioBitrateBps = 128_000,
    fps = 24,
  ),
  High(
    displayLabel = "High",
    bitrate720Bps = 6_000_000,
    bitrate1080Bps = 10_000_000,
    audioBitrateBps = 192_000,
    fps = 24,
  ),
  Ultra(
    displayLabel = "Ultra",
    bitrate720Bps = 8_000_000,
    bitrate1080Bps = 14_000_000,
    audioBitrateBps = 192_000,
    fps = 30,
  ),
}

@JsonClass(generateAdapter = true)
data class ExportSettings(
  val aspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
  val resolution: ExportResolution = ExportResolution.Hd720,
  val quality: ExportQuality = ExportQuality.High,
) {
  val outputWidth: Int
    get() = when (aspectRatio) {
      VideoAspectRatio.Vertical -> resolution.shortSidePx
      VideoAspectRatio.Square -> resolution.shortSidePx
      VideoAspectRatio.Landscape -> if (resolution == ExportResolution.Hd720) 1280 else 1920
    }

  val outputHeight: Int
    get() = when (aspectRatio) {
      VideoAspectRatio.Vertical -> if (resolution == ExportResolution.Hd720) 1280 else 1920
      VideoAspectRatio.Square -> resolution.shortSidePx
      VideoAspectRatio.Landscape -> resolution.shortSidePx
    }

  val videoBitrateBps: Int
    get() = if (resolution == ExportResolution.Hd720) {
      quality.bitrate720Bps
    } else {
      quality.bitrate1080Bps
    }

  fun estimateFileSizeBytes(totalDurationMs: Long, hasAudio: Boolean): Long? {
    if (totalDurationMs <= 0L) return null
    val seconds = totalDurationMs / 1000.0
    val totalBps = videoBitrateBps.toLong() + if (hasAudio) quality.audioBitrateBps.toLong() else 0L
    return ((totalBps / 8.0) * seconds).toLong().coerceAtLeast(1024L)
  }
}

@JsonClass(generateAdapter = true)
data class VideoEditorDraft(
  val id: String,
  val projectId: String? = null,
  val title: String,
  val clips: List<EditorTimelineClip> = emptyList(),
  val audioTracks: List<EditorAudioTrack> = emptyList(),
  val subtitles: List<SubtitleCue> = emptyList(),
  val textOverlays: List<TextOverlay> = emptyList(),
  val exportSettings: ExportSettings = ExportSettings(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  val totalDurationMs: Long
    get() = clips.sumOf { it.durationMs.coerceAtLeast(500L) }
}

data class ExportedVideoRecord(
  val id: String,
  val projectId: String? = null,
  val fileName: String,
  val filePath: String,
  val fileUriString: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val fileSizeBytes: Long,
  val aspectRatio: VideoAspectRatio,
  val resolutionLabel: String,
  val savedMediaEntryId: String? = null,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
