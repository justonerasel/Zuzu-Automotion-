package com.example.data.factory

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.IdeaCategory
import com.example.model.ResearchWorkspace
import com.example.model.StoryboardScene
import com.example.model.VideoAspectRatio
import com.example.model.VideoDurationPreset
import com.example.model.VideoIdea
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.model.VideoProjectStage
import com.example.model.VideoScript
import com.example.model.VideoTone
import com.example.model.VideoType
import com.example.model.VisualStyle

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
  @PrimaryKey val id: String,
  val topic: String,
  val category: String,
  val language: String,
  val duration: String,
  val videoType: String,
  val audience: String,
  val tone: String,
  val visualStyle: String,
  val aspectRatio: String,
  val stage: String,
  val researchOverview: String,
  val researchKeyFacts: String,
  val researchImportantPoints: String,
  val researchSuggestedSources: String,
  val researchNotes: String,
  val researchOutline: String,
  val researchAiUnverified: Boolean,
  val researchUpdatedMillis: Long,
  val scriptHook: String,
  val scriptIntro: String,
  val scriptMainContent: String,
  val scriptSceneNarration: String,
  val scriptTransitions: String,
  val scriptEnding: String,
  val scriptCallToAction: String,
  val scriptIsAiDraft: Boolean,
  val scriptUpdatedMillis: Long,
  val createdAtMillis: Long,
  val updatedAtMillis: Long,
)

@Entity(tableName = "video_ideas")
data class VideoIdeaEntity(
  @PrimaryKey val id: String,
  val title: String,
  val topic: String,
  val hookConcept: String,
  val category: String,
  val targetAudience: String,
  val language: String,
  val isAiSuggested: Boolean,
  val linkedProjectId: String?,
  val createdAtMillis: Long,
)

@Entity(tableName = "storyboard_scenes")
data class StoryboardSceneEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val sceneNumber: Int,
  val sceneTitle: String,
  val narration: String,
  val visualDescription: String,
  val imageGenerationPrompt: String,
  val videoGenerationPrompt: String,
  val suggestedDurationSeconds: Int,
  val transitionSuggestion: String,
  val assignedVoiceAssetId: String? = null,
  val assignedVisualAssetId: String? = null,
)

fun VideoProject.toEntity(): VideoProjectEntity = VideoProjectEntity(
  id = id,
  topic = topic,
  category = category,
  language = language.name,
  duration = duration.name,
  videoType = videoType.name,
  audience = audience,
  tone = tone.name,
  visualStyle = visualStyle.name,
  aspectRatio = aspectRatio.name,
  stage = stage.name,
  researchOverview = research.topicOverview,
  researchKeyFacts = research.keyFacts,
  researchImportantPoints = research.importantPoints,
  researchSuggestedSources = research.suggestedSources,
  researchNotes = research.researchNotes,
  researchOutline = research.contentOutline,
  researchAiUnverified = research.isAiSuggestedUnverified,
  researchUpdatedMillis = research.lastUpdatedMillis,
  scriptHook = script.openingHook,
  scriptIntro = script.introduction,
  scriptMainContent = script.mainContent,
  scriptSceneNarration = script.sceneNarration,
  scriptTransitions = script.transitions,
  scriptEnding = script.ending,
  scriptCallToAction = script.callToAction,
  scriptIsAiDraft = script.isAiDraft,
  scriptUpdatedMillis = script.lastSavedMillis,
  createdAtMillis = createdAtMillis,
  updatedAtMillis = updatedAtMillis,
)

fun VideoProjectEntity.toDomain(scenes: List<StoryboardScene>): VideoProject = VideoProject(
  id = id,
  topic = topic,
  category = category,
  language = enumValueOrDefault(language, VideoLanguage.English),
  duration = enumValueOrDefault(duration, VideoDurationPreset.Medium),
  videoType = enumValueOrDefault(videoType, VideoType.Educational),
  audience = audience,
  tone = enumValueOrDefault(tone, VideoTone.Friendly),
  visualStyle = enumValueOrDefault(visualStyle, VisualStyle.Cinematic),
  aspectRatio = enumValueOrDefault(aspectRatio, VideoAspectRatio.Vertical),
  stage = enumValueOrDefault(stage, VideoProjectStage.Configured),
  research = ResearchWorkspace(
    topicOverview = researchOverview,
    keyFacts = researchKeyFacts,
    importantPoints = researchImportantPoints,
    suggestedSources = researchSuggestedSources,
    researchNotes = researchNotes,
    contentOutline = researchOutline,
    isAiSuggestedUnverified = researchAiUnverified,
    lastUpdatedMillis = researchUpdatedMillis,
  ),
  script = VideoScript(
    openingHook = scriptHook,
    introduction = scriptIntro,
    mainContent = scriptMainContent,
    sceneNarration = scriptSceneNarration,
    transitions = scriptTransitions,
    ending = scriptEnding,
    callToAction = scriptCallToAction,
    isAiDraft = scriptIsAiDraft,
    lastSavedMillis = scriptUpdatedMillis,
  ),
  scenes = scenes.sortedBy { it.sceneNumber },
  createdAtMillis = createdAtMillis,
  updatedAtMillis = updatedAtMillis,
)

fun VideoIdea.toEntity(): VideoIdeaEntity = VideoIdeaEntity(
  id = id,
  title = title,
  topic = topic,
  hookConcept = hookConcept,
  category = category.name,
  targetAudience = targetAudience,
  language = language.name,
  isAiSuggested = isAiSuggested,
  linkedProjectId = linkedProjectId,
  createdAtMillis = createdAtMillis,
)

fun VideoIdeaEntity.toDomain(): VideoIdea = VideoIdea(
  id = id,
  title = title,
  topic = topic,
  hookConcept = hookConcept,
  category = enumValueOrDefault(category, IdeaCategory.OriginalTopic),
  targetAudience = targetAudience,
  language = enumValueOrDefault(language, VideoLanguage.English),
  isAiSuggested = isAiSuggested,
  linkedProjectId = linkedProjectId,
  createdAtMillis = createdAtMillis,
)

fun StoryboardScene.toEntity(): StoryboardSceneEntity = StoryboardSceneEntity(
  id = id,
  projectId = projectId,
  sceneNumber = sceneNumber,
  sceneTitle = sceneTitle,
  narration = narration,
  visualDescription = visualDescription,
  imageGenerationPrompt = imageGenerationPrompt,
  videoGenerationPrompt = videoGenerationPrompt,
  suggestedDurationSeconds = suggestedDurationSeconds,
  transitionSuggestion = transitionSuggestion,
  assignedVoiceAssetId = assignedVoiceAssetId,
  assignedVisualAssetId = assignedVisualAssetId,
)

fun StoryboardSceneEntity.toDomain(): StoryboardScene = StoryboardScene(
  id = id,
  projectId = projectId,
  sceneNumber = sceneNumber,
  sceneTitle = sceneTitle,
  narration = narration,
  visualDescription = visualDescription,
  imageGenerationPrompt = imageGenerationPrompt,
  videoGenerationPrompt = videoGenerationPrompt,
  suggestedDurationSeconds = suggestedDurationSeconds,
  transitionSuggestion = transitionSuggestion,
  assignedVoiceAssetId = assignedVoiceAssetId,
  assignedVisualAssetId = assignedVisualAssetId,
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, fallback: T): T {
  return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
}
