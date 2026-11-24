package com.example.model

enum class VideoFactorySection(val label: String, val testTag: String) {
  Dashboard("Factory", "vf_tab_dashboard"),
  Configure("Configure", "vf_tab_configure"),
  IdeaGenerator("Ideas", "vf_tab_ideas"),
  ResearchStudio("Research", "vf_tab_research"),
  ScriptWriter("Script", "vf_tab_script"),
  StoryboardStudio("Storyboard", "vf_tab_storyboard"),
  VoiceStudio("Voice", "vf_tab_voice"),
  VisualStudio("Visuals", "vf_tab_visuals"),
  AssetLibrary("Assets", "vf_tab_assets"),
  VideoEditor("Editor", "vf_tab_editor"),
  ExportedLibrary("Exports", "vf_tab_exports"),
  Projects("Projects", "vf_tab_projects"),
}

enum class VideoLanguage(val displayLabel: String, val nativeLabel: String) {
  Bangla("Bangla", "বাংলা"),
  English("English", "English"),
}

enum class VideoDurationPreset(val displayLabel: String, val rangeLabel: String) {
  Short("Short", "Under 60 sec"),
  Medium("Medium", "1–3 minutes"),
  Long("Long", "3–10+ minutes"),
}

enum class VideoType(val displayLabel: String) {
  Cartoon("Cartoon"),
  Storytelling("Storytelling"),
  Facts("Facts"),
  Motivation("Motivation"),
  Documentary("Documentary"),
  Educational("Educational"),
  Technology("Technology"),
  Science("Science"),
  Creative("Creative"),
}

enum class VideoTone(val displayLabel: String) {
  Professional("Professional"),
  Friendly("Friendly"),
  Emotional("Emotional"),
  Educational("Educational"),
  Entertaining("Entertaining"),
}

enum class VisualStyle(val displayLabel: String) {
  Realistic("Realistic"),
  TwoDCartoon("2D Cartoon"),
  ThreeDAnimation("3D Animation"),
  Cinematic("Cinematic"),
  Documentary("Documentary"),
}

enum class VideoAspectRatio(val label: String, val description: String) {
  Vertical("9:16", "Shorts & Reels"),
  Landscape("16:9", "YouTube Widescreen"),
  Square("1:1", "Feed Post"),
}

enum class VideoProjectStage(val label: String, val stepNumber: Int) {
  Configured("Configured", 1),
  Researched("Research Drafted", 2),
  ScriptDrafted("Script Drafted", 3),
  StoryboardReady("Storyboard Ready", 4),
}

enum class IdeaCategory(val displayLabel: String) {
  OriginalTopic("Original Video Topic"),
  AttentionTitle("Attention-Grabbing Title"),
  AudienceSpecific("Audience-Specific Idea"),
  ShortConcept("Short Video Concept"),
  EducationalEntertainment("Educational & Entertainment"),
  TrendingCreative("Trending-Style Concept"),
}

data class VideoIdea(
  val id: String,
  val title: String,
  val topic: String,
  val hookConcept: String = "",
  val category: IdeaCategory = IdeaCategory.OriginalTopic,
  val targetAudience: String = "",
  val language: VideoLanguage = VideoLanguage.English,
  val isAiSuggested: Boolean = false,
  val linkedProjectId: String? = null,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

data class ResearchWorkspace(
  val topicOverview: String = "",
  val keyFacts: String = "",
  val importantPoints: String = "",
  val suggestedSources: String = "",
  val researchNotes: String = "",
  val contentOutline: String = "",
  val isAiSuggestedUnverified: Boolean = false,
  val lastUpdatedMillis: Long = System.currentTimeMillis(),
) {
  val hasAnyContent: Boolean
    get() = topicOverview.isNotBlank() ||
      keyFacts.isNotBlank() ||
      importantPoints.isNotBlank() ||
      suggestedSources.isNotBlank() ||
      researchNotes.isNotBlank() ||
      contentOutline.isNotBlank()
}

data class VideoScript(
  val openingHook: String = "",
  val introduction: String = "",
  val mainContent: String = "",
  val sceneNarration: String = "",
  val transitions: String = "",
  val ending: String = "",
  val callToAction: String = "",
  val isAiDraft: Boolean = false,
  val lastSavedMillis: Long = System.currentTimeMillis(),
) {
  val hasContent: Boolean
    get() = openingHook.isNotBlank() ||
      introduction.isNotBlank() ||
      mainContent.isNotBlank() ||
      sceneNarration.isNotBlank() ||
      transitions.isNotBlank() ||
      ending.isNotBlank() ||
      callToAction.isNotBlank()

  fun toCombinedText(): String = buildList {
    if (openingHook.isNotBlank()) add("[Opening Hook]\n$openingHook")
    if (introduction.isNotBlank()) add("[Introduction]\n$introduction")
    if (mainContent.isNotBlank()) add("[Main Content]\n$mainContent")
    if (sceneNarration.isNotBlank()) add("[Scene Narration]\n$sceneNarration")
    if (transitions.isNotBlank()) add("[Transitions]\n$transitions")
    if (ending.isNotBlank()) add("[Ending]\n$ending")
    if (callToAction.isNotBlank()) add("[Call To Action]\n$callToAction")
  }.joinToString("\n\n")

  val wordCount: Int
    get() = toCombinedText()
      .trim()
      .split(Regex("\\s+"))
      .count { it.isNotBlank() }
}

data class StoryboardScene(
  val id: String,
  val projectId: String,
  val sceneNumber: Int,
  val sceneTitle: String,
  val narration: String = "",
  val visualDescription: String = "",
  val imageGenerationPrompt: String = "",
  val videoGenerationPrompt: String = "",
  val suggestedDurationSeconds: Int = 15,
  val transitionSuggestion: String = "Smooth Cut",
  val assignedVoiceAssetId: String? = null,
  val assignedVisualAssetId: String? = null,
)

data class VideoProjectConfigDraft(
  val editingProjectId: String? = null,
  val topic: String = "",
  val category: String = "",
  val language: VideoLanguage = VideoLanguage.English,
  val duration: VideoDurationPreset = VideoDurationPreset.Medium,
  val videoType: VideoType = VideoType.Educational,
  val audience: String = "",
  val tone: VideoTone = VideoTone.Friendly,
  val visualStyle: VisualStyle = VisualStyle.Cinematic,
  val aspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
)

data class VideoProject(
  val id: String,
  val topic: String,
  val category: String,
  val language: VideoLanguage = VideoLanguage.English,
  val duration: VideoDurationPreset = VideoDurationPreset.Medium,
  val videoType: VideoType = VideoType.Educational,
  val audience: String = "",
  val tone: VideoTone = VideoTone.Friendly,
  val visualStyle: VisualStyle = VisualStyle.Cinematic,
  val aspectRatio: VideoAspectRatio = VideoAspectRatio.Vertical,
  val stage: VideoProjectStage = VideoProjectStage.Configured,
  val research: ResearchWorkspace = ResearchWorkspace(),
  val script: VideoScript = VideoScript(),
  val scenes: List<StoryboardScene> = emptyList(),
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  val totalStoryboardSeconds: Int
    get() = scenes.sumOf { it.suggestedDurationSeconds.coerceAtLeast(1) }

  val completedStepsCount: Int
    get() {
      var steps = 1 // Configured
      if (research.hasAnyContent) steps++
      if (script.hasContent) steps++
      if (scenes.isNotEmpty()) steps++
      return steps
    }

  val progressFraction: Float
    get() = completedStepsCount / 4f
}
