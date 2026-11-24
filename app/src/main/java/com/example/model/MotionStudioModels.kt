package com.example.model

import com.squareup.moshi.JsonClass

enum class MotionTemplateCategory(
  val displayLabel: String,
  val tagline: String,
  val defaultHeadline: String,
  val defaultSubheadline: String,
  val defaultStyle: MotionAnimationStyle,
  val primaryArgb: Long,
  val secondaryArgb: Long,
) {
  PromotionalPosts(
    displayLabel = "Promotional Posts",
    tagline = "High-converting flash offers & campaign launches",
    defaultHeadline = "FLASH LAUNCH • 48H ONLY",
    defaultSubheadline = "Upgrade your workflow with Zuzu Creator Studio",
    defaultStyle = MotionAnimationStyle.ZoomInOut,
    primaryArgb = 0xFF7C3AED,
    secondaryArgb = 0xFFEC4899,
  ),
  AnimatedTypography(
    displayLabel = "Animated Typography",
    tagline = "Kinetic headlines synced with smooth motion sweeps",
    defaultHeadline = "CREATE FASTER. GROW SMARTER.",
    defaultSubheadline = "Kinetic storytelling designed for vertical feeds",
    defaultStyle = MotionAnimationStyle.TextReveal,
    primaryArgb = 0xFF2563EB,
    secondaryArgb = 0xFF06B6D4,
  ),
  SocialMediaAds(
    displayLabel = "Social Media Ads",
    tagline = "Thumb-stopping paid creative with clear CTA badge",
    defaultHeadline = "STOP SCROLLING • TRY IT TODAY",
    defaultSubheadline = "Built for YouTube Shorts, Reels & TikTok",
    defaultStyle = MotionAnimationStyle.Slide,
    primaryArgb = 0xFFEC4899,
    secondaryArgb = 0xFFF97316,
  ),
  MotivationalPosts(
    displayLabel = "Motivational Posts",
    tagline = "Daily mindset cards with serene parallax drift",
    defaultHeadline = "CONSISTENCY BEATS INTENSITY",
    defaultSubheadline = "Show up every day and let momentum compound",
    defaultStyle = MotionAnimationStyle.FadeInOut,
    primaryArgb = 0xFF6D28D9,
    secondaryArgb = 0xFF3B82F6,
  ),
  EducationalPosts(
    displayLabel = "Educational Posts",
    tagline = "Structured micro-lessons & quick 3-step tips",
    defaultHeadline = "3 STEPS TO BETTER RETENTION",
    defaultSubheadline = "Hook in 2 seconds • Deliver value • End with action",
    defaultStyle = MotionAnimationStyle.Parallax,
    primaryArgb = 0xFF0EA5E9,
    secondaryArgb = 0xFF10B981,
  ),
  ProductPromotions(
    displayLabel = "Product Promotions",
    tagline = "Hero product showcase with dynamic spotlight",
    defaultHeadline = "MEET THE NEW STUDIO EDITION",
    defaultSubheadline = "Precision engineering meets creator portability",
    defaultStyle = MotionAnimationStyle.ParticleEffects,
    primaryArgb = 0xFFD946EF,
    secondaryArgb = 0xFF7C3AED,
  ),
  AnimatedInfographics(
    displayLabel = "Animated Infographics",
    tagline = "Data-driven stat reveals with clean geometric motion",
    defaultHeadline = "3.4X HIGHER WATCH TIME",
    defaultSubheadline = "Visual hooks boost completion across short feeds",
    defaultStyle = MotionAnimationStyle.AnimatedBackground,
    primaryArgb = 0xFF10B981,
    secondaryArgb = 0xFF2563EB,
  ),
  PhotoMotion(
    displayLabel = "Photo Motion",
    tagline = "Ken Burns zoom & smooth depth drift for stills",
    defaultHeadline = "BEHIND THE SCENES",
    defaultSubheadline = "Bring static photography to life with camera motion",
    defaultStyle = MotionAnimationStyle.Parallax,
    primaryArgb = 0xFFF97316,
    secondaryArgb = 0xFFEC4899,
  ),
  LogoAnimations(
    displayLabel = "Logo Animations",
    tagline = "Clean brand bumper & rotating emblem reveal",
    defaultHeadline = "ZUZU CREATOR LABS",
    defaultSubheadline = "Your signature animated brand intro & outro",
    defaultStyle = MotionAnimationStyle.Rotate,
    primaryArgb = 0xFF4F46E5,
    secondaryArgb = 0xFF06B6D4,
  ),
}

enum class MotionAnimationStyle(val displayLabel: String) {
  FadeInOut("Fade In/Out"),
  ZoomInOut("Zoom In/Out"),
  Slide("Slide"),
  Rotate("Rotate"),
  Parallax("Parallax"),
  TextReveal("Text Reveal"),
  ParticleEffects("Particle Effects"),
  AnimatedBackground("Animated Background"),
}

enum class MotionTransition(val displayLabel: String) {
  None("Clean Cut"),
  CrossFade("Cross Fade"),
  SlideSweep("Slide Sweep"),
  ScalePop("Scale Pop"),
}

enum class MotionExportFormat(val displayLabel: String, val extension: String, val mimeType: String) {
  Mp4Video("MP4 Video", "mp4", "video/mp4"),
  PngImage("PNG Poster", "png", "image/png"),
  JpegImage("JPEG Graphic", "jpg", "image/jpeg"),
}

enum class MotionPlatformPreset(
  val displayLabel: String,
  val platform: SocialPlatform,
  val defaultAspectRatio: VideoAspectRatio,
  val safeZoneHint: String,
  val defaultHashtags: String,
) {
  YouTubeShorts(
    displayLabel = "YouTube Shorts / 16:9",
    platform = SocialPlatform.YouTube,
    defaultAspectRatio = VideoAspectRatio.Vertical,
    safeZoneHint = "Keep key text above bottom 18% channel bar",
    defaultHashtags = "#YouTubeShorts #ZuzuAI #CreatorStudio",
  ),
  YouTubeLandscape(
    displayLabel = "YouTube Widescreen",
    platform = SocialPlatform.YouTube,
    defaultAspectRatio = VideoAspectRatio.Landscape,
    safeZoneHint = "Centered 16:9 broadcast safe frame",
    defaultHashtags = "#YouTube #MotionDesign #ZuzuAI",
  ),
  FacebookFeed(
    displayLabel = "Facebook Square Post",
    platform = SocialPlatform.Facebook,
    defaultAspectRatio = VideoAspectRatio.Square,
    safeZoneHint = "Balanced 1:1 feed promo framing",
    defaultHashtags = "#FacebookReels #SocialMarketing #ZuzuAI",
  ),
  TikTokVertical(
    displayLabel = "TikTok Vertical 9:16",
    platform = SocialPlatform.TikTok,
    defaultAspectRatio = VideoAspectRatio.Vertical,
    safeZoneHint = "Avoid right-side 15% action rail & bottom captions",
    defaultHashtags = "#TikTokCreator #MotionPost #ZuzuAI",
  ),
}

@JsonClass(generateAdapter = true)
data class MotionStickerItem(
  val id: String,
  val label: String,
  val badgeSymbol: String,
  val xFraction: Float = 0.82f,
  val yFraction: Float = 0.16f,
)

@JsonClass(generateAdapter = true)
data class MotionMediaSlot(
  val id: String,
  val title: String,
  val mediaType: MediaType = MediaType.Image,
  val uriString: String,
  val sourceMediaId: String? = null,
)

@JsonClass(generateAdapter = true)
data class MotionPostProject(
  val id: String,
  val title: String,
  val templateCategory: MotionTemplateCategory = MotionTemplateCategory.PromotionalPosts,
  val platformPreset: MotionPlatformPreset = MotionPlatformPreset.TikTokVertical,
  val aspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
  val animationStyle: MotionAnimationStyle = MotionAnimationStyle.ZoomInOut,
  val transition: MotionTransition = MotionTransition.CrossFade,
  val durationSeconds: Int = 8,
  val headlineText: String = MotionTemplateCategory.PromotionalPosts.defaultHeadline,
  val subheadlineText: String = MotionTemplateCategory.PromotionalPosts.defaultSubheadline,
  val textColorArgb: Long = 0xFFFFFFFF,
  val textFontSizeSp: Int = 26,
  val primaryAccentArgb: Long = MotionTemplateCategory.PromotionalPosts.primaryArgb,
  val secondaryAccentArgb: Long = MotionTemplateCategory.PromotionalPosts.secondaryArgb,
  val mediaSlots: List<MotionMediaSlot> = emptyList(),
  val stickers: List<MotionStickerItem> = emptyList(),
  val logoUriString: String? = null,
  val logoLabel: String = "ZUZU",
  val backgroundMusicTitle: String? = null,
  val backgroundMusicUriString: String? = null,
  val backgroundMusicVolume: Float = 0.45f,
  val exportedFilePath: String? = null,
  val exportedFileUriString: String? = null,
  val exportedFormat: MotionExportFormat? = null,
  val updatedAtMillis: Long = System.currentTimeMillis(),
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  val durationMs: Long
    get() = durationSeconds.coerceIn(3, 30) * 1000L

  val outputDimensions: Pair<Int, Int>
    get() = when (aspectRatio) {
      VideoAspectRatio.Vertical -> 720 to 1280
      VideoAspectRatio.Landscape -> 1280 to 720
      VideoAspectRatio.Square -> 720 to 720
    }
}
