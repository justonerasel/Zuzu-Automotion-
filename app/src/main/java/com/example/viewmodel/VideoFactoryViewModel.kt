package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiGenerationResult
import com.example.data.ai.AiServiceStatus
import com.example.data.ai.GeminiVideoFactoryAiService
import com.example.data.ai.IdeaGenerationRequest
import com.example.data.ai.ScriptAiAction
import com.example.data.ai.VideoFactoryAiService
import com.example.data.factory.InMemoryVideoFactoryRepository
import com.example.data.factory.ScriptToStoryboardConverter
import com.example.data.factory.VideoFactoryRepository
import com.example.model.IdeaCategory
import com.example.model.ResearchWorkspace
import com.example.model.StoryboardScene
import com.example.model.VideoAspectRatio
import com.example.model.VideoDurationPreset
import com.example.model.VideoFactorySection
import com.example.model.VideoIdea
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.model.VideoProjectConfigDraft
import com.example.model.VideoScript
import com.example.model.VideoTone
import com.example.model.VideoType
import com.example.model.VisualStyle
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiSetupNotice(
  val title: String,
  val message: String,
  val setupSteps: List<String>,
)

data class VideoFactoryUiState(
  val activeSection: VideoFactorySection = VideoFactorySection.Dashboard,
  val projects: List<VideoProject> = emptyList(),
  val ideas: List<VideoIdea> = emptyList(),
  val selectedProjectId: String? = null,
  val configDraft: VideoProjectConfigDraft = VideoProjectConfigDraft(),
  val aiStatus: AiServiceStatus = AiServiceStatus.SetupRequired(
    title = "Gemini AI Setup Required",
    message = "Configure a Gemini backend endpoint to enable AI generation.",
    setupSteps = emptyList(),
  ),
  val setupNotice: AiSetupNotice? = null,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val activeProject: VideoProject?
    get() = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

  val totalScenesCount: Int
    get() = projects.sumOf { it.scenes.size }
}

class VideoFactoryViewModel(
  private val repository: VideoFactoryRepository = InMemoryVideoFactoryRepository(),
  private val aiService: VideoFactoryAiService = GeminiVideoFactoryAiService(),
) : ViewModel() {

  private val activeSection = MutableStateFlow(VideoFactorySection.Dashboard)
  private val selectedProjectId = MutableStateFlow<String?>(null)
  private val configDraft = MutableStateFlow(VideoProjectConfigDraft())
  private val setupNotice = MutableStateFlow<AiSetupNotice?>(null)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private val controlStreams = combine(
    activeSection,
    selectedProjectId,
    configDraft,
    setupNotice,
  ) { section, selectedId, draft, notice ->
    ControlState(section, selectedId, draft, notice)
  }

  private val messageStreams = combine(feedbackMessage, errorMessage) { feedback, error ->
    feedback to error
  }

  val uiState: StateFlow<VideoFactoryUiState> = combine(
    repository.projects,
    repository.ideas,
    aiService.status,
    controlStreams,
    messageStreams,
  ) { projects, ideas, aiStatus, controls, (feedback, error) ->
    val resolvedSelectedId = controls.selectedId?.takeIf { id -> projects.any { it.id == id } }
      ?: projects.firstOrNull()?.id
    VideoFactoryUiState(
      activeSection = controls.section,
      projects = projects,
      ideas = ideas,
      selectedProjectId = resolvedSelectedId,
      configDraft = controls.draft,
      aiStatus = aiStatus,
      setupNotice = controls.notice,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = VideoFactoryUiState(),
  )

  fun selectSection(section: VideoFactorySection) {
    activeSection.value = section
    clearMessages()
  }

  fun startNewProjectConfig() {
    configDraft.value = VideoProjectConfigDraft()
    activeSection.value = VideoFactorySection.Configure
    clearMessages()
  }

  fun editProjectConfig(project: VideoProject) {
    selectedProjectId.value = project.id
    configDraft.value = VideoProjectConfigDraft(
      editingProjectId = project.id,
      topic = project.topic,
      category = project.category,
      language = project.language,
      duration = project.duration,
      videoType = project.videoType,
      audience = project.audience,
      tone = project.tone,
      visualStyle = project.visualStyle,
      aspectRatio = project.aspectRatio,
    )
    activeSection.value = VideoFactorySection.Configure
    clearMessages()
  }

  fun updateConfigDraft(transform: (VideoProjectConfigDraft) -> VideoProjectConfigDraft) {
    configDraft.update(transform)
  }

  fun updateDraftTopic(topic: String) = updateConfigDraft { it.copy(topic = topic) }
  fun updateDraftCategory(category: String) = updateConfigDraft { it.copy(category = category) }
  fun updateDraftLanguage(language: VideoLanguage) = updateConfigDraft { it.copy(language = language) }
  fun updateDraftDuration(duration: VideoDurationPreset) = updateConfigDraft { it.copy(duration = duration) }
  fun updateDraftVideoType(type: VideoType) = updateConfigDraft { it.copy(videoType = type) }
  fun updateDraftAudience(audience: String) = updateConfigDraft { it.copy(audience = audience) }
  fun updateDraftTone(tone: VideoTone) = updateConfigDraft { it.copy(tone = tone) }
  fun updateDraftVisualStyle(style: VisualStyle) = updateConfigDraft { it.copy(visualStyle = style) }
  fun updateDraftAspectRatio(ratio: VideoAspectRatio) = updateConfigDraft { it.copy(aspectRatio = ratio) }

  fun saveProjectConfig(navigateToSection: VideoFactorySection? = VideoFactorySection.ResearchStudio) {
    val draft = configDraft.value
    viewModelScope.launch {
      repository.createOrUpdateProject(draft)
        .onSuccess { saved ->
          selectedProjectId.value = saved.id
          configDraft.value = VideoProjectConfigDraft()
          feedbackMessage.value = if (draft.editingProjectId == null) {
            "Created video project '${saved.topic}'."
          } else {
            "Updated configuration for '${saved.topic}'."
          }
          errorMessage.value = null
          if (navigateToSection != null) {
            activeSection.value = navigateToSection
          }
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to save project configuration."
        }
    }
  }

  fun openProject(
    projectId: String,
    targetSection: VideoFactorySection = VideoFactorySection.Dashboard,
  ) {
    selectedProjectId.value = projectId
    activeSection.value = targetSection
    clearMessages()
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      repository.deleteProject(projectId)
      if (selectedProjectId.value == projectId) {
        selectedProjectId.value = null
      }
      feedbackMessage.value = "Video project deleted."
    }
  }

  // --- Idea Generator ---

  fun requestAiIdeas(
    nicheOrTopic: String,
    audience: String,
    category: IdeaCategory,
    language: VideoLanguage,
  ) {
    viewModelScope.launch {
      when (
        val result = aiService.generateIdeas(
          IdeaGenerationRequest(
            nicheOrTopic = nicheOrTopic,
            audience = audience,
            category = category,
            language = language,
          ),
        )
      ) {
        is AiGenerationResult.Success -> {
          result.data.forEach { idea ->
            repository.saveIdea(
              editingId = null,
              title = idea.title,
              topic = idea.topic,
              hookConcept = idea.hookConcept,
              category = idea.category,
              targetAudience = idea.targetAudience,
              language = idea.language,
              isAiSuggested = true,
            )
          }
          feedbackMessage.value = "Generated ${result.data.size} ideas."
        }
        is AiGenerationResult.SetupRequired -> {
          setupNotice.value = AiSetupNotice(result.title, result.message, result.setupSteps)
        }
        is AiGenerationResult.Error -> {
          errorMessage.value = result.message
        }
      }
    }
  }

  fun saveIdea(
    editingId: String?,
    title: String,
    topic: String,
    hookConcept: String,
    category: IdeaCategory,
    targetAudience: String,
    language: VideoLanguage,
  ) {
    viewModelScope.launch {
      repository.saveIdea(
        editingId = editingId,
        title = title,
        topic = topic,
        hookConcept = hookConcept,
        category = category,
        targetAudience = targetAudience,
        language = language,
      ).onSuccess { saved ->
        feedbackMessage.value = if (editingId == null) {
          "Saved idea '${saved.title}'."
        } else {
          "Updated idea '${saved.title}'."
        }
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to save idea."
      }
    }
  }

  fun deleteIdea(ideaId: String) {
    viewModelScope.launch {
      repository.deleteIdea(ideaId)
      feedbackMessage.value = "Idea removed."
    }
  }

  fun turnIdeaIntoProject(idea: VideoIdea) {
    configDraft.value = VideoProjectConfigDraft(
      topic = idea.topic.ifBlank { idea.title },
      category = idea.category.displayLabel,
      language = idea.language,
      duration = VideoDurationPreset.Medium,
      audience = idea.targetAudience,
    )
    activeSection.value = VideoFactorySection.Configure
    feedbackMessage.value = "Loaded '${idea.title}' into Video Configuration. Customize and create project."
    errorMessage.value = null
  }

  // --- Research Studio ---

  fun requestAiResearch(project: VideoProject) {
    viewModelScope.launch {
      when (val result = aiService.generateResearchSuggestions(project)) {
        is AiGenerationResult.Success -> {
          repository.saveResearch(
            project.id,
            result.data.copy(isAiSuggestedUnverified = true),
          )
          feedbackMessage.value = "AI research draft added (marked Unverified — check sources before publishing)."
        }
        is AiGenerationResult.SetupRequired -> {
          setupNotice.value = AiSetupNotice(result.title, result.message, result.setupSteps)
        }
        is AiGenerationResult.Error -> {
          errorMessage.value = result.message
        }
      }
    }
  }

  fun saveResearch(projectId: String, research: ResearchWorkspace) {
    viewModelScope.launch {
      repository.saveResearch(projectId, research)
        .onSuccess {
          feedbackMessage.value = "Research workspace saved locally."
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to save research."
        }
    }
  }

  // --- Script Writer ---

  fun requestAiScriptAction(
    project: VideoProject,
    action: ScriptAiAction,
    selectedText: String? = null,
    targetTone: VideoTone = project.tone,
  ) {
    viewModelScope.launch {
      when (
        val result = aiService.generateOrTransformScript(
          project = project,
          action = action,
          selectedText = selectedText,
          targetTone = targetTone,
        )
      ) {
        is AiGenerationResult.Success -> {
          repository.saveScript(project.id, result.data.copy(isAiDraft = true))
          feedbackMessage.value = "${action.displayLabel} completed."
        }
        is AiGenerationResult.SetupRequired -> {
          setupNotice.value = AiSetupNotice(result.title, result.message, result.setupSteps)
        }
        is AiGenerationResult.Error -> {
          errorMessage.value = result.message
        }
      }
    }
  }

  fun saveScriptDraft(projectId: String, script: VideoScript) {
    viewModelScope.launch {
      repository.saveScript(projectId, script)
        .onSuccess {
          feedbackMessage.value = "Script draft saved locally."
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to save script draft."
        }
    }
  }

  // --- Storyboard Studio ---

  fun convertScriptToStoryboardScenes(project: VideoProject) {
    val converted = ScriptToStoryboardConverter.convertSectionsToScenes(project)
    if (converted.isEmpty()) {
      errorMessage.value = "Write at least one script section first before converting to storyboard scenes."
      return
    }
    saveStoryboardScenes(
      projectId = project.id,
      scenes = converted,
      successMessage = "Converted script sections into ${converted.size} editable storyboard scenes.",
    )
  }

  fun requestAiStoryboard(project: VideoProject) {
    viewModelScope.launch {
      when (val result = aiService.generateStoryboardFromScript(project)) {
        is AiGenerationResult.Success -> {
          saveStoryboardScenes(project.id, result.data, "AI storyboard scenes generated.")
        }
        is AiGenerationResult.SetupRequired -> {
          setupNotice.value = AiSetupNotice(result.title, result.message, result.setupSteps)
        }
        is AiGenerationResult.Error -> {
          errorMessage.value = result.message
        }
      }
    }
  }

  fun upsertScene(project: VideoProject, scene: StoryboardScene) {
    val current = project.scenes.toMutableList()
    val existingIndex = current.indexOfFirst { it.id == scene.id }
    val normalizedScene = if (scene.id.isBlank()) {
      scene.copy(id = UUID.randomUUID().toString(), projectId = project.id)
    } else {
      scene.copy(projectId = project.id)
    }
    if (existingIndex >= 0) {
      current[existingIndex] = normalizedScene
    } else {
      current.add(normalizedScene)
    }
    saveStoryboardScenes(
      projectId = project.id,
      scenes = current,
      successMessage = if (existingIndex >= 0) "Scene updated." else "Scene added to storyboard.",
    )
  }

  fun deleteScene(project: VideoProject, sceneId: String) {
    val updated = project.scenes.filterNot { it.id == sceneId }
    saveStoryboardScenes(project.id, updated, "Scene removed from storyboard.")
  }

  fun moveScene(project: VideoProject, sceneIndex: Int, delta: Int) {
    val targetIndex = sceneIndex + delta
    if (sceneIndex !in project.scenes.indices || targetIndex !in project.scenes.indices) return
    val reordered = project.scenes.toMutableList()
    val item = reordered.removeAt(sceneIndex)
    reordered.add(targetIndex, item)
    saveStoryboardScenes(project.id, reordered, "Storyboard order updated.")
  }

  private fun saveStoryboardScenes(
    projectId: String,
    scenes: List<StoryboardScene>,
    successMessage: String,
  ) {
    viewModelScope.launch {
      repository.saveStoryboard(projectId, scenes)
        .onSuccess {
          feedbackMessage.value = successMessage
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to save storyboard."
        }
    }
  }

  fun dismissSetupNotice() {
    setupNotice.value = null
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private data class ControlState(
    val section: VideoFactorySection,
    val selectedId: String?,
    val draft: VideoProjectConfigDraft,
    val notice: AiSetupNotice?,
  )
}
