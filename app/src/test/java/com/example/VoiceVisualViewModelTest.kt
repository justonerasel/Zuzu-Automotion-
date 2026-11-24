package com.example

import com.example.data.ai.MediaProviderStatus
import com.example.data.ai.ModularVisualImageProvider
import com.example.data.ai.ModularVisualVideoProvider
import com.example.data.ai.ModularVoiceTtsProvider
import com.example.data.factory.InMemoryStudioAssetRepository
import com.example.data.factory.InMemoryVideoFactoryRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.ImageProviderType
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.PermissionStatus
import com.example.model.StoryboardScene
import com.example.model.StudioAssetCategory
import com.example.model.VideoAspectRatio
import com.example.model.VideoClipProviderType
import com.example.model.VideoLanguage
import com.example.model.VideoProjectConfigDraft
import com.example.model.VisualAssetStatus
import com.example.model.VisualStudioStyle
import com.example.model.VoiceGender
import com.example.model.VoiceProviderType
import com.example.model.VoiceStudioTone
import com.example.viewmodel.VoiceVisualViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceVisualViewModelTest {

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
  fun `voice settings support Bangla and English, gender, speed clamping, pitch, and 5 tones`() = runTest {
    val viewModel = VoiceVisualViewModel()
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.setVoiceLanguage(VideoLanguage.Bangla)
    viewModel.setVoiceGender(VoiceGender.Male)
    viewModel.setVoiceSpeed(3.5f)
    viewModel.setVoicePitch(0.2f)
    viewModel.setVoiceTone(VoiceStudioTone.Emotional)

    val settings = viewModel.uiState.value.voiceSettings
    assertEquals(VideoLanguage.Bangla, settings.language)
    assertEquals(VoiceGender.Male, settings.gender)
    assertEquals(2.0f, settings.speed, 0.001f)
    assertEquals(0.5f, settings.pitch, 0.001f)
    assertEquals(VoiceStudioTone.Emotional, settings.tone)

    // Verify provider-specific pitch support flags
    assertTrue(VoiceProviderType.GeminiTts.supportsPitch)
    assertFalse(VoiceProviderType.ElevenLabs.supportsPitch)

    viewModel.selectVoiceProvider(VoiceProviderType.ElevenLabs)
    assertEquals(VoiceProviderType.ElevenLabs, viewModel.uiState.value.selectedVoiceProvider)
    assertTrue(viewModel.uiState.value.voiceProviderStatus is MediaProviderStatus.SetupRequired)

    collector.cancel()
  }

  @Test
  fun `unconfigured TTS, Image, and Video providers surface SetupRequired without faking assets`() = runTest {
    val videoRepo = InMemoryVideoFactoryRepository()
    val assetRepo = InMemoryStudioAssetRepository()
    val project = videoRepo.createOrUpdateProject(VideoProjectConfigDraft(topic = "Space Tech")).getOrThrow()
    val scene = StoryboardScene(
      id = "scene-1",
      projectId = project.id,
      sceneNumber = 1,
      sceneTitle = "Opening Hook",
      narration = "Watch what happens in low Earth orbit.",
      imageGenerationPrompt = "Astronaut over glowing blue Earth",
      videoGenerationPrompt = "Slow orbital camera glide",
    )
    videoRepo.saveStoryboard(project.id, listOf(scene))
    val savedProject = videoRepo.projects.first().first()

    val viewModel = VoiceVisualViewModel(
      videoFactoryRepository = videoRepo,
      studioAssetRepository = assetRepo,
      contentSourceRepository = InMemoryContentSourceRepository(),
      voiceProviders = mapOf(
        VoiceProviderType.GeminiTts to ModularVoiceTtsProvider(VoiceProviderType.GeminiTts),
        VoiceProviderType.ElevenLabs to ModularVoiceTtsProvider(VoiceProviderType.ElevenLabs),
      ),
      imageProviders = mapOf(
        ImageProviderType.GeminiImagen to ModularVisualImageProvider(ImageProviderType.GeminiImagen),
      ),
      videoProviders = mapOf(
        VideoClipProviderType.GoogleVeo to ModularVisualVideoProvider(VideoClipProviderType.GoogleVeo),
      ),
    )
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.previewVoice("Sample preview")
    assertNotNull(viewModel.uiState.value.setupNotice)
    viewModel.dismissSetupNotice()

    viewModel.generateVoiceFromScript(savedProject, scene.narration, scene.id)
    assertNotNull(viewModel.uiState.value.setupNotice)
    assertTrue(viewModel.uiState.value.allAssets.isEmpty())
    viewModel.dismissSetupNotice()

    viewModel.generateSceneImage(savedProject, scene)
    assertNotNull(viewModel.uiState.value.setupNotice)
    assertEquals(VisualAssetStatus.SetupRequired, viewModel.uiState.value.sceneStatusOverrides[scene.id])
    viewModel.dismissSetupNotice()

    viewModel.generateSceneVideoClip(savedProject, scene)
    assertNotNull(viewModel.uiState.value.setupNotice)
    assertTrue(viewModel.uiState.value.allAssets.isEmpty())

    collector.cancel()
  }

  @Test
  fun `manual audio and visual imports assign to scenes, update prompts, save to Media Library, filter, and delete cleanly`() = runTest {
    val videoRepo = InMemoryVideoFactoryRepository()
    val assetRepo = InMemoryStudioAssetRepository()
    val sourceRepo = InMemoryContentSourceRepository()

    val project = videoRepo.createOrUpdateProject(
      VideoProjectConfigDraft(
        topic = "Bangla Storytelling",
        language = VideoLanguage.Bangla,
        aspectRatio = VideoAspectRatio.Vertical,
      ),
    ).getOrThrow()
    val scene = StoryboardScene(
      id = "scene-1",
      projectId = project.id,
      sceneNumber = 1,
      sceneTitle = "Intro",
      narration = "একটি সুন্দর সকাল।",
      imageGenerationPrompt = "Morning sunlight village",
      videoGenerationPrompt = "Gentle zoom on river",
    )
    videoRepo.saveStoryboard(project.id, listOf(scene))
    val activeProject = videoRepo.projects.first().first()

    val viewModel = VoiceVisualViewModel(
      videoFactoryRepository = videoRepo,
      studioAssetRepository = assetRepo,
      contentSourceRepository = sourceRepo,
    )
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    // 1. Update visual style, aspect ratio, and scene prompts
    viewModel.selectVisualStyle(VisualStudioStyle.MotionGraphics)
    viewModel.selectAspectRatio(VideoAspectRatio.Landscape)
    viewModel.updateScenePrompts(activeProject, scene.id, "2D vector city sunrise", "Slow pan across skyline")
    val afterPrompts = videoRepo.projects.first().first().scenes.first()
    assertEquals("2D vector city sunrise", afterPrompts.imageGenerationPrompt)
    assertEquals("Slow pan across skyline", afterPrompts.videoGenerationPrompt)

    // 2. Import local audio + assign to Scene 1
    viewModel.importAudioFile(
      upload = PendingLocalUpload(
        uriString = "content://media/audio/narration_bn.mp3",
        fileName = "narration_bn.mp3",
        mimeType = "audio/mpeg",
        mediaType = MediaType.Audio,
        fileSizeBytes = 102_400L,
        durationMs = 8_500L,
      ),
      project = videoRepo.projects.first().first(),
      targetSceneId = scene.id,
      title = "Bangla Intro Voice",
    )
    assertEquals(1, viewModel.uiState.value.audioAssets.size)
    val importedAudio = viewModel.uiState.value.audioAssets.first()
    assertEquals(StudioAssetCategory.ImportedAudio, importedAudio.category)
    assertEquals(importedAudio.id, videoRepo.projects.first().first().scenes.first().assignedVoiceAssetId)

    // 3. Import local visual + assign to Scene 1
    viewModel.importVisualAsset(
      upload = PendingLocalUpload(
        uriString = "content://media/image/scene1_bg.png",
        fileName = "scene1_bg.png",
        mimeType = "image/png",
        mediaType = MediaType.Image,
        fileSizeBytes = 250_000L,
      ),
      project = videoRepo.projects.first().first(),
      scene = videoRepo.projects.first().first().scenes.first(),
    )
    assertEquals(1, viewModel.uiState.value.visualAssets.size)
    val importedVisual = viewModel.uiState.value.visualAssets.first()
    assertEquals(StudioAssetCategory.ImportedVisual, importedVisual.category)
    assertEquals(importedVisual.id, videoRepo.projects.first().first().scenes.first().assignedVisualAssetId)

    // 4. Filter & Search in Asset Library
    viewModel.updateAssetCategoryFilter(StudioAssetCategory.ImportedAudio)
    assertEquals(1, viewModel.uiState.value.filteredAssets.size)
    viewModel.updateAssetCategoryFilter(null)
    viewModel.updateAssetSearchQuery("scene1")
    assertEquals(1, viewModel.uiState.value.filteredAssets.size)
    viewModel.updateAssetSearchQuery("")
    assertEquals(2, viewModel.uiState.value.filteredAssets.size)

    // 5. Save asset to Content Source Media Library
    viewModel.saveAssetToMediaLibrary(importedVisual)
    val updatedVisual = viewModel.uiState.value.allAssets.first { it.id == importedVisual.id }
    assertTrue(updatedVisual.savedToMediaLibrary)
    val mediaEntries = sourceRepo.mediaEntries.first()
    assertEquals(1, mediaEntries.size)
    assertEquals(PermissionStatus.OriginalContent, mediaEntries.first().permission.status)

    // 6. Delete asset clears scene assignment reference cleanly
    viewModel.deleteAsset(importedAudio.id, videoRepo.projects.first().first())
    assertEquals(1, viewModel.uiState.value.allAssets.size)
    assertNull(videoRepo.projects.first().first().scenes.first().assignedVoiceAssetId)

    collector.cancel()
  }
}
