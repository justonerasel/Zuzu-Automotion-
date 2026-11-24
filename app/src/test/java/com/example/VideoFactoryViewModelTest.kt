package com.example

import com.example.data.ai.AiServiceStatus
import com.example.data.ai.GeminiVideoFactoryAiService
import com.example.data.ai.ScriptAiAction
import com.example.data.factory.InMemoryVideoFactoryRepository
import com.example.model.IdeaCategory
import com.example.model.ResearchWorkspace
import com.example.model.StoryboardScene
import com.example.model.VideoAspectRatio
import com.example.model.VideoDurationPreset
import com.example.model.VideoFactorySection
import com.example.model.VideoLanguage
import com.example.model.VideoProjectStage
import com.example.model.VideoScript
import com.example.model.VideoTone
import com.example.model.VideoType
import com.example.model.VisualStyle
import com.example.viewmodel.VideoFactoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoFactoryViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state starts empty with default Medium duration and Gemini setup required`() = runTest {
    val viewModel = VideoFactoryViewModel(
      repository = InMemoryVideoFactoryRepository(),
      aiService = GeminiVideoFactoryAiService(),
    )
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    val state = viewModel.uiState.value
    assertTrue(state.projects.isEmpty())
    assertTrue(state.ideas.isEmpty())
    assertEquals(VideoDurationPreset.Medium, state.configDraft.duration)
    assertTrue(state.aiStatus is AiServiceStatus.SetupRequired)

    collector.cancel()
  }

  @Test
  fun `project creation, editing, reopening, and deletion work reliably`() = runTest {
    val viewModel = VideoFactoryViewModel(InMemoryVideoFactoryRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.startNewProjectConfig()
    viewModel.updateDraftTopic("স্বপ্নের প্রযুক্তি")
    viewModel.updateDraftCategory("Technology")
    viewModel.updateDraftLanguage(VideoLanguage.Bangla)
    viewModel.updateDraftDuration(VideoDurationPreset.Medium)
    viewModel.updateDraftVideoType(VideoType.Documentary)
    viewModel.updateDraftAudience("Bangla Learners")
    viewModel.updateDraftTone(VideoTone.Educational)
    viewModel.updateDraftVisualStyle(VisualStyle.Cinematic)
    viewModel.updateDraftAspectRatio(VideoAspectRatio.Landscape)

    viewModel.saveProjectConfig()

    assertEquals(1, viewModel.uiState.value.projects.size)
    val created = viewModel.uiState.value.activeProject
    assertNotNull(created)
    assertEquals("স্বপ্নের প্রযুক্তি", created!!.topic)
    assertEquals(VideoLanguage.Bangla, created.language)
    assertEquals(VideoDurationPreset.Medium, created.duration)
    assertEquals(VideoProjectStage.Configured, created.stage)

    viewModel.editProjectConfig(created)
    viewModel.updateDraftAspectRatio(VideoAspectRatio.Vertical)
    viewModel.saveProjectConfig(navigateToSection = VideoFactorySection.Projects)
    assertEquals(VideoAspectRatio.Vertical, viewModel.uiState.value.activeProject!!.aspectRatio)

    viewModel.deleteProject(created.id)
    assertTrue(viewModel.uiState.value.projects.isEmpty())

    collector.cancel()
  }

  @Test
  fun `ideas can be saved, edited, turned into project, and deleted`() = runTest {
    val viewModel = VideoFactoryViewModel(InMemoryVideoFactoryRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.saveIdea(
      editingId = null,
      title = "5 Daily Habits That Save 2 Hours",
      topic = "Time Management",
      hookConcept = "Start with a timer visual",
      category = IdeaCategory.ShortConcept,
      targetAudience = "Students",
      language = VideoLanguage.English,
    )
    assertEquals(1, viewModel.uiState.value.ideas.size)
    val savedIdea = viewModel.uiState.value.ideas.first()

    viewModel.saveIdea(
      editingId = savedIdea.id,
      title = "5 Morning Habits for Focus",
      topic = "Focus & Study",
      hookConcept = "Clean desk shot",
      category = IdeaCategory.EducationalEntertainment,
      targetAudience = "College Students",
      language = VideoLanguage.English,
    )
    assertEquals("5 Morning Habits for Focus", viewModel.uiState.value.ideas.first().title)

    viewModel.turnIdeaIntoProject(viewModel.uiState.value.ideas.first())
    assertEquals(VideoFactorySection.Configure, viewModel.uiState.value.activeSection)
    assertEquals("Focus & Study", viewModel.uiState.value.configDraft.topic)

    viewModel.deleteIdea(savedIdea.id)
    assertTrue(viewModel.uiState.value.ideas.isEmpty())

    collector.cancel()
  }

  @Test
  fun `research, script draft editing, and storyboard conversion & reordering update stage`() = runTest {
    val viewModel = VideoFactoryViewModel(InMemoryVideoFactoryRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.updateDraftTopic("Solar Energy Basics")
    viewModel.saveProjectConfig()
    val projectId = viewModel.uiState.value.activeProject!!.id

    viewModel.saveResearch(
      projectId,
      ResearchWorkspace(
        topicOverview = "Overview of photovoltaic cells.",
        keyFacts = "Sunlight photons knock electrons loose.",
        contentOutline = "1. Hook 2. How panels work 3. Summary",
      ),
    )
    assertEquals(VideoProjectStage.Researched, viewModel.uiState.value.activeProject!!.stage)

    viewModel.saveScriptDraft(
      projectId,
      VideoScript(
        openingHook = "Every hour, the sun sends more energy than Earth uses in a year.",
        introduction = "Let us explore how solar panels turn light into electricity.",
        mainContent = "Silicon layers create an electric field.",
        ending = "Clean energy starts on rooftops.",
        callToAction = "Subscribe for more science shorts.",
      ),
    )
    assertEquals(VideoProjectStage.ScriptDrafted, viewModel.uiState.value.activeProject!!.stage)
    assertTrue(viewModel.uiState.value.activeProject!!.script.wordCount > 10)

    viewModel.convertScriptToStoryboardScenes(viewModel.uiState.value.activeProject!!)
    val afterConvert = viewModel.uiState.value.activeProject!!
    assertEquals(VideoProjectStage.StoryboardReady, afterConvert.stage)
    assertEquals(4, afterConvert.scenes.size)
    assertEquals("Opening Hook", afterConvert.scenes.first().sceneTitle)

    viewModel.moveScene(afterConvert, 0, 1)
    assertEquals("Introduction", viewModel.uiState.value.activeProject!!.scenes.first().sceneTitle)

    val customScene = StoryboardScene(
      id = "",
      projectId = projectId,
      sceneNumber = 5,
      sceneTitle = "B-Roll Outro",
      narration = "Final shot",
      visualDescription = "Sunset rooftop",
      imageGenerationPrompt = "Warm sunset solar panels",
      videoGenerationPrompt = "Slow drone pull back",
      suggestedDurationSeconds = 10,
      transitionSuggestion = "Fade Out",
    )
    viewModel.upsertScene(viewModel.uiState.value.activeProject!!, customScene)
    assertEquals(5, viewModel.uiState.value.activeProject!!.scenes.size)

    val lastSceneId = viewModel.uiState.value.activeProject!!.scenes.last().id
    viewModel.deleteScene(viewModel.uiState.value.activeProject!!, lastSceneId)
    assertEquals(4, viewModel.uiState.value.activeProject!!.scenes.size)

    collector.cancel()
  }

  @Test
  fun `unconfigured Gemini AI actions surface setupNotice without faking results`() = runTest {
    val viewModel = VideoFactoryViewModel(
      repository = InMemoryVideoFactoryRepository(),
      aiService = GeminiVideoFactoryAiService(),
    )
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.updateDraftTopic("AI Literacy")
    viewModel.saveProjectConfig()
    val project = viewModel.uiState.value.activeProject!!

    viewModel.requestAiIdeas("AI", "Beginners", IdeaCategory.OriginalTopic, VideoLanguage.English)
    assertNotNull(viewModel.uiState.value.setupNotice)
    assertTrue(viewModel.uiState.value.ideas.isEmpty())
    viewModel.dismissSetupNotice()
    assertNull(viewModel.uiState.value.setupNotice)

    viewModel.requestAiResearch(project)
    assertNotNull(viewModel.uiState.value.setupNotice)
    viewModel.dismissSetupNotice()

    viewModel.requestAiScriptAction(project, ScriptAiAction.Regenerate)
    assertNotNull(viewModel.uiState.value.setupNotice)
    viewModel.dismissSetupNotice()

    viewModel.requestAiStoryboard(project)
    assertNotNull(viewModel.uiState.value.setupNotice)

    collector.cancel()
  }
}
