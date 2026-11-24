package com.example

import com.example.data.editor.InMemoryVideoEditorRepository
import com.example.data.editor.RenderOutcome
import com.example.data.editor.RenderProgress
import com.example.data.editor.RendererCapabilityStatus
import com.example.data.editor.VideoEditorDraftCodec
import com.example.data.editor.VideoMp4Renderer
import com.example.data.factory.InMemoryVideoFactoryRepository
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.EditorAudioTrackType
import com.example.model.EditorClipType
import com.example.model.EditorTransition
import com.example.model.EditorVisualEffect
import com.example.model.ExportQuality
import com.example.model.ExportResolution
import com.example.model.MediaType
import com.example.model.OverlayPosition
import com.example.model.StoryboardScene
import com.example.model.StudioAsset
import com.example.model.StudioAssetCategory
import com.example.model.SubtitleBackgroundStyle
import com.example.model.SubtitleCue
import com.example.model.SubtitlePosition
import com.example.model.TextOverlay
import com.example.model.VideoAspectRatio
import com.example.model.VideoEditorDraft
import com.example.model.VideoEditorTab
import com.example.model.VideoLanguage
import com.example.model.VideoProject
import com.example.viewmodel.VideoEditorViewModel
import com.example.viewmodel.ZuzuAppViewModel
import java.io.File
import kotlinx.coroutines.CompletableDeferred
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
class VideoEditorViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun sampleProject(): VideoProject {
    return VideoProject(
      id = "proj-bn-1",
      topic = "বাংলায় মহাকাশ বিজ্ঞান",
      category = "Science",
      language = VideoLanguage.Bangla,
      aspectRatio = VideoAspectRatio.Vertical,
      scenes = listOf(
        StoryboardScene(
          id = "scene-1",
          projectId = "proj-bn-1",
          sceneNumber = 1,
          sceneTitle = "Opening Hook",
          narration = "মহাকাশে সময় কীভাবে কাজ করে?",
          visualDescription = "Glowing galaxy spiral",
          suggestedDurationSeconds = 6,
          assignedVoiceAssetId = "asset-voice-1",
          assignedVisualAssetId = "asset-img-1",
        ),
        StoryboardScene(
          id = "scene-2",
          projectId = "proj-bn-1",
          sceneNumber = 2,
          sceneTitle = "Core Fact",
          narration = "Gravity bends light and slows clocks.",
          visualDescription = "Black hole ring",
          suggestedDurationSeconds = 8,
        ),
      ),
    )
  }

  private fun sampleAssets(): List<StudioAsset> = listOf(
    StudioAsset(
      id = "asset-voice-1",
      projectId = "proj-bn-1",
      assignedSceneId = "scene-1",
      title = "Bangla Hook Voice",
      category = StudioAssetCategory.ImportedAudio,
      mediaType = MediaType.Audio,
      localUriString = "content://media/audio/hook.mp3",
      durationMs = 6_000L,
    ),
    StudioAsset(
      id = "asset-img-1",
      projectId = "proj-bn-1",
      assignedSceneId = "scene-1",
      title = "Galaxy Still",
      category = StudioAssetCategory.ImportedVisual,
      mediaType = MediaType.Image,
      localUriString = "content://media/image/galaxy.png",
    ),
  )

  private class StubRenderer(
    private val block: suspend (VideoEditorDraft, File, (RenderProgress) -> Unit) -> RenderOutcome,
  ) : VideoMp4Renderer {
    override fun checkCapability(): RendererCapabilityStatus =
      RendererCapabilityStatus.Available("Test Renderer", "Unit test stub")

    override suspend fun renderToMp4(
      draft: VideoEditorDraft,
      outputFile: File,
      onProgress: (RenderProgress) -> Unit,
    ): RenderOutcome = block(draft, outputFile, onProgress)
  }

  @Test
  fun `project imports into editor, edits timeline, subtitles, audio, overlays, and persists draft`() = runTest {
    val editorRepo = InMemoryVideoEditorRepository()
    val viewModel = VideoEditorViewModel(
      editorRepository = editorRepo,
      contentSourceRepository = InMemoryContentSourceRepository(),
      mp4Renderer = StubRenderer { _, _, _ -> RenderOutcome.Error("Not used") },
    )
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    val project = sampleProject()
    viewModel.openProjectInEditor(project, sampleAssets())

    val initialDraft = viewModel.uiState.value.currentDraft!!
    assertEquals(2, initialDraft.clips.size)
    assertEquals(EditorClipType.Image, initialDraft.clips[0].clipType)
    assertEquals(EditorClipType.TitleCard, initialDraft.clips[1].clipType)
    assertEquals(14_000L, initialDraft.totalDurationMs)
    assertEquals(2, initialDraft.subtitles.size)
    assertEquals("মহাকাশে সময় কীভাবে কাজ করে?", initialDraft.subtitles[0].text)
    assertEquals(1, initialDraft.audioTracks.size)

    // Reorder clips & adjust trim/transition/effect
    viewModel.moveClip(0, 1)
    assertEquals("Scene 2: Core Fact", viewModel.uiState.value.currentDraft!!.clips[0].title)
    val firstClipId = viewModel.uiState.value.currentDraft!!.clips[0].id
    viewModel.updateClip(
      clipId = firstClipId,
      trimStartMs = 1_500L,
      durationMs = 10_000L,
      transition = EditorTransition.SlideLeft,
      visualEffect = EditorVisualEffect.WarmGlow,
    )
    val editedClip = viewModel.uiState.value.currentDraft!!.clips[0]
    assertEquals(1_500L, editedClip.trimStartMs)
    assertEquals(10_000L, editedClip.durationMs)
    assertEquals(EditorTransition.SlideLeft, editedClip.transition)
    assertEquals(EditorVisualEffect.WarmGlow, editedClip.visualEffect)

    // Add a media clip + text overlay
    viewModel.addMediaClip("Extra Shot", EditorClipType.Video, "content://media/video/clip.mp4", durationMs = 4_000L)
    assertEquals(3, viewModel.uiState.value.currentDraft!!.clips.size)
    viewModel.addTextOverlay(TextOverlay(id = "ov-1", text = "Zuzu Science", position = OverlayPosition.Top))
    assertEquals(1, viewModel.uiState.value.currentDraft!!.textOverlays.size)

    // Subtitle edits (Bangla & styling)
    viewModel.selectTab(VideoEditorTab.Subtitles)
    assertEquals(VideoEditorTab.Subtitles, viewModel.uiState.value.activeTab)
    viewModel.upsertSubtitle(
      SubtitleCue(
        id = "sub-custom",
        text = "নতুন সাবটাইটেল",
        language = VideoLanguage.Bangla,
        startMs = 0L,
        endMs = 3_000L,
        fontSizeSp = 24,
        position = SubtitlePosition.Top,
        backgroundStyle = SubtitleBackgroundStyle.PurpleGlass,
      ),
    )
    assertEquals(3, viewModel.uiState.value.currentDraft!!.subtitles.size)

    // Audio controls: add background music, adjust volume, trim, mute, remove
    viewModel.addAudioTrack(EditorAudioTrackType.BackgroundMusic, "Ambient LoFi", "content://media/audio/bg.mp3", 30_000L)
    val bgTrack = viewModel.uiState.value.currentDraft!!.audioTracks.last()
    viewModel.updateAudioVolume(bgTrack.id, 0.25f)
    viewModel.updateAudioTrim(bgTrack.id, 2_000L, 12_000L)
    viewModel.toggleAudioMute(bgTrack.id)
    val updatedBg = viewModel.uiState.value.currentDraft!!.audioTracks.last()
    assertEquals(0.25f, updatedBg.volume, 0.001f)
    assertEquals(2_000L, updatedBg.trimStartMs)
    assertEquals(12_000L, updatedBg.trimEndMs)
    assertTrue(updatedBg.isMuted)

    // Export settings & reliable file size estimation
    viewModel.setExportAspectRatio(VideoAspectRatio.Landscape)
    viewModel.setExportResolution(ExportResolution.FullHd1080)
    viewModel.setExportQuality(ExportQuality.Ultra)
    val settings = viewModel.uiState.value.currentDraft!!.exportSettings
    assertEquals(1920, settings.outputWidth)
    assertEquals(1080, settings.outputHeight)
    assertNotNull(viewModel.uiState.value.estimatedFileSizeBytes)

    // Save Draft & Resume
    viewModel.saveDraft()
    assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    assertEquals(1, viewModel.uiState.value.savedDrafts.size)

    // Moshi JSON round-trip check
    val json = VideoEditorDraftCodec.encode(viewModel.uiState.value.currentDraft!!)
    val decoded = VideoEditorDraftCodec.decode(json)
    assertNotNull(decoded)
    assertEquals("বাংলায় মহাকাশ বিজ্ঞান", decoded!!.title)
    assertEquals(3, decoded.subtitles.size)

    // Remove clip & track cleanly
    viewModel.removeClip(firstClipId)
    viewModel.removeAudioTrack(bgTrack.id)
    assertEquals(2, viewModel.uiState.value.currentDraft!!.clips.size)
    assertEquals(1, viewModel.uiState.value.currentDraft!!.audioTracks.size)

    collector.cancel()
  }

  @Test
  fun `render saves completed MP4 to export library and supports rename and delete`() = runTest {
    val editorRepo = InMemoryVideoEditorRepository()
    val sourceRepo = InMemoryContentSourceRepository()
    val dashboardVm = ZuzuAppViewModel(
      contentSourceRepository = sourceRepo,
      videoFactoryRepository = InMemoryVideoFactoryRepository(),
      videoEditorRepository = editorRepo,
    )
    val tempDir = kotlin.io.path.createTempDirectory("zuzu_test_exports").toFile()
    try {
      val editorVm = VideoEditorViewModel(
        editorRepository = editorRepo,
        contentSourceRepository = sourceRepo,
        mp4Renderer = StubRenderer { draft, outFile, onProgress ->
          onProgress(RenderProgress(0.5f, "Encoding"))
          outFile.writeBytes(byteArrayOf(0, 0, 0, 24, 102, 116, 121, 112))
          RenderOutcome.Success(
            outputFile = outFile,
            durationMs = draft.totalDurationMs,
            width = draft.exportSettings.outputWidth,
            height = draft.exportSettings.outputHeight,
            fileSizeBytes = outFile.length(),
          )
        },
        exportsDirectoryProvider = { tempDir },
        shareUriResolver = { file -> "content://com.example.fileprovider/zuzu_exports/${file.name}" },
      )

      val c1 = launch(testDispatcher) { editorVm.uiState.collect() }
      val c2 = launch(testDispatcher) { dashboardVm.uiState.collect() }

      // Empty timeline estimate is null and render fails gracefully
      editorVm.openProjectInEditor(null, emptyList())
      assertNull(editorVm.uiState.value.estimatedFileSizeBytes)
      editorVm.startRender()
      assertNotNull(editorVm.uiState.value.errorMessage)
      assertTrue(editorVm.uiState.value.exportedVideos.isEmpty())

      // Add a scene and render
      editorVm.addTitleCardClip("Launch Scene", durationSeconds = 5)
      editorVm.startRender()

      assertFalse(editorVm.uiState.value.renderState.isRendering)
      assertEquals(1, editorVm.uiState.value.exportedVideos.size)
      val exported = editorVm.uiState.value.exportedVideos.first()
      assertNotNull(exported.savedMediaEntryId)
      assertEquals(1, sourceRepo.mediaEntries.first().size)
      assertEquals(1, dashboardVm.uiState.value.metrics.first { it.id == "videos_created" }.count)

      // Rename exported MP4
      editorVm.renameExportedVideo(exported.id, "Solar_Final_Cut")
      assertEquals("Solar_Final_Cut.mp4", editorVm.uiState.value.exportedVideos.first().fileName)

      // Delete exported MP4
      editorVm.deleteExportedVideo(exported.id)
      assertTrue(editorVm.uiState.value.exportedVideos.isEmpty())
      assertEquals(0, dashboardVm.uiState.value.metrics.first { it.id == "videos_created" }.count)

      c1.cancel()
      c2.cancel()
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun `render error and cancellation never fabricate exported videos`() = runTest {
    val editorRepo = InMemoryVideoEditorRepository()
    val gate = CompletableDeferred<Unit>()

    val cancellingVm = VideoEditorViewModel(
      editorRepository = editorRepo,
      contentSourceRepository = InMemoryContentSourceRepository(),
      mp4Renderer = StubRenderer { _, _, _ ->
        gate.await()
        RenderOutcome.Cancelled
      },
    )
    val c1 = launch(testDispatcher) { cancellingVm.uiState.collect() }
    cancellingVm.openProjectInEditor(null, emptyList())
    cancellingVm.addTitleCardClip("Clip 1", 4)
    cancellingVm.startRender()
    assertTrue(cancellingVm.uiState.value.renderState.isRendering)
    cancellingVm.cancelRender()
    assertFalse(cancellingVm.uiState.value.renderState.isRendering)
    assertTrue(cancellingVm.uiState.value.exportedVideos.isEmpty())
    c1.cancel()

    val failingVm = VideoEditorViewModel(
      editorRepository = editorRepo,
      contentSourceRepository = InMemoryContentSourceRepository(),
      mp4Renderer = StubRenderer { _, _, _ ->
        RenderOutcome.Error("Hardware AVC encoder unavailable")
      },
    )
    val c2 = launch(testDispatcher) { failingVm.uiState.collect() }
    failingVm.openProjectInEditor(null, emptyList())
    failingVm.addTitleCardClip("Clip 1", 4)
    failingVm.startRender()
    assertFalse(failingVm.uiState.value.renderState.isRendering)
    assertEquals("Hardware AVC encoder unavailable", failingVm.uiState.value.errorMessage)
    assertTrue(failingVm.uiState.value.exportedVideos.isEmpty())
    c2.cancel()
  }
}
