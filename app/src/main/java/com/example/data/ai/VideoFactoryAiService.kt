package com.example.data.ai

import com.example.model.IdeaCategory
import com.example.model.ResearchWorkspace
import com.example.model.StoryboardScene
import com.example.model.VideoIdea
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.model.VideoScript
import com.example.model.VideoTone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds ONLY non-secret endpoint/configuration metadata for future Gemini API integration.
 * API keys and server credentials must NEVER be hardcoded into Android source files.
 */
data class GeminiServiceConfig(
  val backendProxyEndpoint: String = "",
  val modelIdentifier: String = "gemini-2.5-flash",
  val appCheckRequired: Boolean = true,
) {
  val isConfigured: Boolean
    get() = backendProxyEndpoint.trim().startsWith("https://")
}

sealed interface AiServiceStatus {
  data class SetupRequired(
    val title: String,
    val message: String,
    val setupSteps: List<String>,
  ) : AiServiceStatus

  data class Ready(
    val providerLabel: String,
    val modelIdentifier: String,
  ) : AiServiceStatus
}

sealed interface AiGenerationResult<out T> {
  data class Success<T>(val data: T) : AiGenerationResult<T>
  data class SetupRequired(
    val title: String,
    val message: String,
    val setupSteps: List<String>,
  ) : AiGenerationResult<Nothing>
  data class Error(val message: String) : AiGenerationResult<Nothing>
}

data class IdeaGenerationRequest(
  val nicheOrTopic: String,
  val audience: String,
  val category: IdeaCategory,
  val language: VideoLanguage,
)

enum class ScriptAiAction(val displayLabel: String) {
  GenerateFull("Generate Script"),
  Regenerate("Regenerate Script"),
  RewriteSelection("Rewrite Selected Text"),
  ChangeTone("Change Tone"),
  Shorten("Shorten Script"),
  Expand("Expand Script"),
}

/**
 * Modular AI Service interface for Phase 04 (Idea, Research, Script, Storyboard)
 * designed for future Gemini API / Firebase AI Logic integration.
 */
interface VideoFactoryAiService {
  val status: StateFlow<AiServiceStatus>

  suspend fun generateIdeas(request: IdeaGenerationRequest): AiGenerationResult<List<VideoIdea>>

  suspend fun generateResearchSuggestions(project: VideoProject): AiGenerationResult<ResearchWorkspace>

  suspend fun generateOrTransformScript(
    project: VideoProject,
    action: ScriptAiAction,
    selectedText: String? = null,
    targetTone: VideoTone = project.tone,
  ): AiGenerationResult<VideoScript>

  suspend fun generateStoryboardFromScript(project: VideoProject): AiGenerationResult<List<StoryboardScene>>
}

/**
 * Default modular implementation for Phase 04 when no Gemini backend/API key is configured.
 * Never fabricates fake AI output or hardcodes credentials — returns an explicit SetupRequired
 * result so users can configure Gemini later while using manual editing immediately.
 */
class GeminiVideoFactoryAiService(
  private val config: GeminiServiceConfig = GeminiServiceConfig(),
) : VideoFactoryAiService {

  private val setupChecklist = listOf(
    "Provision Firebase AI Logic (Gemini API) or an HTTPS Gemini backend proxy endpoint.",
    "Configure App Check / server-side API credentials via AI Studio Secrets (never in source code).",
    "Supply a valid HTTPS proxy endpoint in GeminiServiceConfig when backend integration activates.",
  )

  private val setupStatus = AiServiceStatus.SetupRequired(
    title = "Gemini AI Setup Required",
    message = "No Gemini API backend or Firebase AI Logic configuration is active in Phase 04. Use the full manual editors below—your drafts save locally.",
    setupSteps = setupChecklist,
  )

  private val _status = MutableStateFlow<AiServiceStatus>(
    if (config.isConfigured) {
      AiServiceStatus.Ready("Gemini API (Pending Integration)", config.modelIdentifier)
    } else {
      setupStatus
    },
  )
  override val status: StateFlow<AiServiceStatus> = _status.asStateFlow()

  private fun unconfiguredResult(featureName: String): AiGenerationResult.SetupRequired {
    return AiGenerationResult.SetupRequired(
      title = "$featureName Requires Gemini API Setup",
      message = "Live AI $featureName is waiting on Gemini API configuration. You can create and edit everything manually right now without losing work.",
      setupSteps = setupChecklist,
    )
  }

  override suspend fun generateIdeas(request: IdeaGenerationRequest): AiGenerationResult<List<VideoIdea>> {
    return unconfiguredResult("Idea Generation")
  }

  override suspend fun generateResearchSuggestions(project: VideoProject): AiGenerationResult<ResearchWorkspace> {
    return unconfiguredResult("Research Suggestions")
  }

  override suspend fun generateOrTransformScript(
    project: VideoProject,
    action: ScriptAiAction,
    selectedText: String?,
    targetTone: VideoTone,
  ): AiGenerationResult<VideoScript> {
    return unconfiguredResult(action.displayLabel)
  }

  override suspend fun generateStoryboardFromScript(project: VideoProject): AiGenerationResult<List<StoryboardScene>> {
    return unconfiguredResult("AI Storyboard Conversion")
  }
}
