package com.example.data.factory

import com.example.model.IdeaCategory
import com.example.model.ResearchWorkspace
import com.example.model.StoryboardScene
import com.example.model.VideoIdea
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.model.VideoProjectConfigDraft
import com.example.model.VideoProjectStage
import com.example.model.VideoScript
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface VideoFactoryRepository {
  val projects: Flow<List<VideoProject>>
  val ideas: Flow<List<VideoIdea>>

  suspend fun createOrUpdateProject(draft: VideoProjectConfigDraft): Result<VideoProject>
  suspend fun saveResearch(projectId: String, research: ResearchWorkspace): Result<VideoProject>
  suspend fun saveScript(projectId: String, script: VideoScript): Result<VideoProject>
  suspend fun saveStoryboard(projectId: String, scenes: List<StoryboardScene>): Result<VideoProject>
  suspend fun deleteProject(projectId: String)

  suspend fun saveIdea(
    editingId: String?,
    title: String,
    topic: String,
    hookConcept: String,
    category: IdeaCategory,
    targetAudience: String,
    language: VideoLanguage,
    isAiSuggested: Boolean = false,
    linkedProjectId: String? = null,
  ): Result<VideoIdea>

  suspend fun deleteIdea(ideaId: String)
}

class RoomVideoFactoryRepository(
  private val dao: VideoFactoryDao,
) : VideoFactoryRepository {

  override val projects: Flow<List<VideoProject>> = combine(
    dao.observeVideoProjects(),
    dao.observeAllScenes(),
  ) { projectEntities, sceneEntities ->
    val scenesByProject = sceneEntities.groupBy { it.projectId }
    projectEntities.map { entity ->
      val domainScenes = scenesByProject[entity.id].orEmpty().map { it.toDomain() }
      entity.toDomain(domainScenes)
    }
  }

  override val ideas: Flow<List<VideoIdea>> =
    dao.observeVideoIdeas().map { list -> list.map { it.toDomain() } }

  override suspend fun createOrUpdateProject(draft: VideoProjectConfigDraft): Result<VideoProject> {
    val cleanTopic = draft.topic.trim()
    if (cleanTopic.isEmpty()) {
      return Result.failure(IllegalArgumentException("Video Topic is required."))
    }
    val cleanCategory = draft.category.trim().ifEmpty { draft.videoType.displayLabel }
    val now = System.currentTimeMillis()
    val existing = draft.editingProjectId?.let { id ->
      projects.first().firstOrNull { it.id == id }
    }

    val updated = if (existing != null) {
      existing.copy(
        topic = cleanTopic,
        category = cleanCategory,
        language = draft.language,
        duration = draft.duration,
        videoType = draft.videoType,
        audience = draft.audience.trim(),
        tone = draft.tone,
        visualStyle = draft.visualStyle,
        aspectRatio = draft.aspectRatio,
        updatedAtMillis = now,
      )
    } else {
      VideoProject(
        id = UUID.randomUUID().toString(),
        topic = cleanTopic,
        category = cleanCategory,
        language = draft.language,
        duration = draft.duration,
        videoType = draft.videoType,
        audience = draft.audience.trim(),
        tone = draft.tone,
        visualStyle = draft.visualStyle,
        aspectRatio = draft.aspectRatio,
        stage = VideoProjectStage.Configured,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    }
    dao.upsertProject(updated.toEntity())
    return Result.success(updated)
  }

  override suspend fun saveResearch(
    projectId: String,
    research: ResearchWorkspace,
  ): Result<VideoProject> {
    val current = projects.first().firstOrNull { it.id == projectId }
      ?: return Result.failure(IllegalStateException("Video project not found."))
    val now = System.currentTimeMillis()
    val nextStage = computeStage(research, current.script, current.scenes)
    val updated = current.copy(
      research = research.copy(lastUpdatedMillis = now),
      stage = nextStage,
      updatedAtMillis = now,
    )
    dao.upsertProject(updated.toEntity())
    return Result.success(updated)
  }

  override suspend fun saveScript(
    projectId: String,
    script: VideoScript,
  ): Result<VideoProject> {
    val current = projects.first().firstOrNull { it.id == projectId }
      ?: return Result.failure(IllegalStateException("Video project not found."))
    val now = System.currentTimeMillis()
    val nextStage = computeStage(current.research, script, current.scenes)
    val updated = current.copy(
      script = script.copy(lastSavedMillis = now),
      stage = nextStage,
      updatedAtMillis = now,
    )
    dao.upsertProject(updated.toEntity())
    return Result.success(updated)
  }

  override suspend fun saveStoryboard(
    projectId: String,
    scenes: List<StoryboardScene>,
  ): Result<VideoProject> {
    val current = projects.first().firstOrNull { it.id == projectId }
      ?: return Result.failure(IllegalStateException("Video project not found."))
    val now = System.currentTimeMillis()
    val normalizedScenes = scenes.mapIndexed { idx, scene ->
      scene.copy(projectId = projectId, sceneNumber = idx + 1)
    }
    val nextStage = computeStage(current.research, current.script, normalizedScenes)
    val updated = current.copy(
      scenes = normalizedScenes,
      stage = nextStage,
      updatedAtMillis = now,
    )
    dao.replaceScenesForProject(projectId, normalizedScenes.map { it.toEntity() })
    dao.upsertProject(updated.toEntity())
    return Result.success(updated)
  }

  override suspend fun deleteProject(projectId: String) {
    dao.deleteProjectAndScenes(projectId)
  }

  override suspend fun saveIdea(
    editingId: String?,
    title: String,
    topic: String,
    hookConcept: String,
    category: IdeaCategory,
    targetAudience: String,
    language: VideoLanguage,
    isAiSuggested: Boolean,
    linkedProjectId: String?,
  ): Result<VideoIdea> {
    val cleanTitle = title.trim()
    val cleanTopic = topic.trim().ifEmpty { cleanTitle }
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Idea Title is required."))
    }
    val idea = VideoIdea(
      id = editingId ?: UUID.randomUUID().toString(),
      title = cleanTitle,
      topic = cleanTopic,
      hookConcept = hookConcept.trim(),
      category = category,
      targetAudience = targetAudience.trim(),
      language = language,
      isAiSuggested = isAiSuggested,
      linkedProjectId = linkedProjectId,
      createdAtMillis = System.currentTimeMillis(),
    )
    dao.upsertIdea(idea.toEntity())
    return Result.success(idea)
  }

  override suspend fun deleteIdea(ideaId: String) {
    dao.deleteIdeaById(ideaId)
  }
}

class InMemoryVideoFactoryRepository : VideoFactoryRepository {
  private val _projects = MutableStateFlow<List<VideoProject>>(emptyList())
  private val _ideas = MutableStateFlow<List<VideoIdea>>(emptyList())

  override val projects: Flow<List<VideoProject>> = _projects.asStateFlow()
  override val ideas: Flow<List<VideoIdea>> = _ideas.asStateFlow()

  override suspend fun createOrUpdateProject(draft: VideoProjectConfigDraft): Result<VideoProject> {
    val cleanTopic = draft.topic.trim()
    if (cleanTopic.isEmpty()) {
      return Result.failure(IllegalArgumentException("Video Topic is required."))
    }
    val cleanCategory = draft.category.trim().ifEmpty { draft.videoType.displayLabel }
    val now = System.currentTimeMillis()
    val existing = draft.editingProjectId?.let { id ->
      _projects.value.firstOrNull { it.id == id }
    }
    val project = if (existing != null) {
      existing.copy(
        topic = cleanTopic,
        category = cleanCategory,
        language = draft.language,
        duration = draft.duration,
        videoType = draft.videoType,
        audience = draft.audience.trim(),
        tone = draft.tone,
        visualStyle = draft.visualStyle,
        aspectRatio = draft.aspectRatio,
        updatedAtMillis = now,
      )
    } else {
      VideoProject(
        id = UUID.randomUUID().toString(),
        topic = cleanTopic,
        category = cleanCategory,
        language = draft.language,
        duration = draft.duration,
        videoType = draft.videoType,
        audience = draft.audience.trim(),
        tone = draft.tone,
        visualStyle = draft.visualStyle,
        aspectRatio = draft.aspectRatio,
        stage = VideoProjectStage.Configured,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    }
    _projects.update { list ->
      listOf(project) + list.filterNot { it.id == project.id }
    }
    return Result.success(project)
  }

  override suspend fun saveResearch(
    projectId: String,
    research: ResearchWorkspace,
  ): Result<VideoProject> {
    return updateProject(projectId) { current, now ->
      val nextStage = computeStage(research, current.script, current.scenes)
      current.copy(
        research = research.copy(lastUpdatedMillis = now),
        stage = nextStage,
        updatedAtMillis = now,
      )
    }
  }

  override suspend fun saveScript(
    projectId: String,
    script: VideoScript,
  ): Result<VideoProject> {
    return updateProject(projectId) { current, now ->
      val nextStage = computeStage(current.research, script, current.scenes)
      current.copy(
        script = script.copy(lastSavedMillis = now),
        stage = nextStage,
        updatedAtMillis = now,
      )
    }
  }

  override suspend fun saveStoryboard(
    projectId: String,
    scenes: List<StoryboardScene>,
  ): Result<VideoProject> {
    return updateProject(projectId) { current, now ->
      val normalized = scenes.mapIndexed { idx, s -> s.copy(projectId = projectId, sceneNumber = idx + 1) }
      val nextStage = computeStage(current.research, current.script, normalized)
      current.copy(
        scenes = normalized,
        stage = nextStage,
        updatedAtMillis = now,
      )
    }
  }

  override suspend fun deleteProject(projectId: String) {
    _projects.update { list -> list.filterNot { it.id == projectId } }
  }

  override suspend fun saveIdea(
    editingId: String?,
    title: String,
    topic: String,
    hookConcept: String,
    category: IdeaCategory,
    targetAudience: String,
    language: VideoLanguage,
    isAiSuggested: Boolean,
    linkedProjectId: String?,
  ): Result<VideoIdea> {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Idea Title is required."))
    }
    val idea = VideoIdea(
      id = editingId ?: UUID.randomUUID().toString(),
      title = cleanTitle,
      topic = topic.trim().ifEmpty { cleanTitle },
      hookConcept = hookConcept.trim(),
      category = category,
      targetAudience = targetAudience.trim(),
      language = language,
      isAiSuggested = isAiSuggested,
      linkedProjectId = linkedProjectId,
      createdAtMillis = System.currentTimeMillis(),
    )
    _ideas.update { list ->
      listOf(idea) + list.filterNot { it.id == idea.id }
    }
    return Result.success(idea)
  }

  override suspend fun deleteIdea(ideaId: String) {
    _ideas.update { list -> list.filterNot { it.id == ideaId } }
  }

  private fun updateProject(
    projectId: String,
    transform: (VideoProject, Long) -> VideoProject,
  ): Result<VideoProject> {
    val existing = _projects.value.firstOrNull { it.id == projectId }
      ?: return Result.failure(IllegalStateException("Video project not found."))
    val updated = transform(existing, System.currentTimeMillis())
    _projects.update { list ->
      listOf(updated) + list.filterNot { it.id == projectId }
    }
    return Result.success(updated)
  }
}

private fun computeStage(
  research: ResearchWorkspace,
  script: VideoScript,
  scenes: List<StoryboardScene>,
): VideoProjectStage {
  return when {
    scenes.isNotEmpty() -> VideoProjectStage.StoryboardReady
    script.hasContent -> VideoProjectStage.ScriptDrafted
    research.hasAnyContent -> VideoProjectStage.Researched
    else -> VideoProjectStage.Configured
  }
}

/**
 * Pure structural helper that converts the user's written script sections into editable
 * storyboard scene slots without fabricating unconfigured AI content.
 */
object ScriptToStoryboardConverter {
  fun convertSectionsToScenes(project: VideoProject): List<StoryboardScene> {
    val script = project.script
    val styleHint = "${project.visualStyle.displayLabel}, ${project.aspectRatio.label}"
    val rawBlocks = listOf(
      "Opening Hook" to script.openingHook,
      "Introduction" to script.introduction,
      "Main Content" to script.mainContent,
      "Scene Narration" to script.sceneNarration,
      "Ending & CTA" to listOf(script.ending, script.callToAction).filter { it.isNotBlank() }.joinToString("\n"),
    ).filter { it.second.isNotBlank() }

    if (rawBlocks.isEmpty()) return emptyList()

    val transitionLines = script.transitions.lines().map { it.trim() }.filter { it.isNotBlank() }

    return rawBlocks.mapIndexed { index, (title, narration) ->
      val transition = transitionLines.getOrNull(index) ?: if (index == rawBlocks.lastIndex) "Fade Out" else "Smooth Cut"
      StoryboardScene(
        id = UUID.randomUUID().toString(),
        projectId = project.id,
        sceneNumber = index + 1,
        sceneTitle = title,
        narration = narration.trim(),
        visualDescription = "Visual scene for $title (${project.topic})",
        imageGenerationPrompt = "$styleHint keyframe representing: $title — ${project.topic}",
        videoGenerationPrompt = "$styleHint camera motion for $title (${project.tone.displayLabel} mood)",
        suggestedDurationSeconds = if (index == 0) 8 else 15,
        transitionSuggestion = transition,
      )
    }
  }
}
