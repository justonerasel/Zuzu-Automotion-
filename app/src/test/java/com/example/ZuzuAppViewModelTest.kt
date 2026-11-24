package com.example

import com.example.model.AppDestination
import com.example.model.ConnectionStatus
import com.example.model.PlatformConnectionPhase
import com.example.model.PrimaryTab
import com.example.model.SocialPlatform
import com.example.model.StudioModuleType
import com.example.viewmodel.ZuzuAppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZuzuAppViewModelTest {

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
  fun `initial state starts on Dashboard with zero counts and disconnected placeholders`() {
    val viewModel = ZuzuAppViewModel()
    val state = viewModel.uiState.value

    assertEquals(AppDestination.Tab(PrimaryTab.Dashboard), state.destination)
    assertEquals(3, state.socialAccounts.size)
    assertTrue(state.socialAccounts.all { it.status == ConnectionStatus.NotConnected })
    assertEquals(4, state.metrics.size)
    assertTrue(state.metrics.all { it.count == 0 })
    assertTrue(state.projects.isEmpty())
    assertEquals(2, state.studioModules.size)
  }

  @Test
  fun `all four primary tabs, studio intro screens, and social accounts navigate cleanly`() {
    val viewModel = ZuzuAppViewModel()

    viewModel.openSocialAccounts(fromTab = PrimaryTab.Dashboard)
    assertEquals(
      AppDestination.SocialAccounts(PrimaryTab.Dashboard),
      viewModel.uiState.value.destination,
    )
    viewModel.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Dashboard), viewModel.uiState.value.destination)

    viewModel.selectTab(PrimaryTab.Sources)
    assertEquals(AppDestination.Tab(PrimaryTab.Sources), viewModel.uiState.value.destination)

    viewModel.selectTab(PrimaryTab.Studio)
    assertEquals(AppDestination.Tab(PrimaryTab.Studio), viewModel.uiState.value.destination)

    viewModel.openModuleIntro(StudioModuleType.AiVideoFactory)
    assertTrue(viewModel.uiState.value.destination is AppDestination.ModuleIntro)

    viewModel.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Studio), viewModel.uiState.value.destination)

    viewModel.selectTab(PrimaryTab.Projects)
    assertTrue(viewModel.uiState.value.projects.isEmpty())

    viewModel.selectTab(PrimaryTab.Settings)
    viewModel.toggleWorkflowAlerts(false)
    assertFalse(viewModel.uiState.value.settings.workflowAlertsEnabled)
  }

  @Test
  fun `sourceStats starts at zero and updates from ContentSourceRepository`() = runTest {
    val sourceRepo = com.example.data.source.InMemoryContentSourceRepository()
    val viewModel = ZuzuAppViewModel(contentSourceRepository = sourceRepo)

    assertEquals(0, viewModel.uiState.value.sourceStats.totalMediaFiles)
    assertEquals(0, viewModel.uiState.value.sourceStats.creatorSources)

    sourceRepo.upsertCreatorSource(
      id = null,
      creatorName = "Studio Partner",
      platform = com.example.model.ExternalPlatformCategory.YouTube,
      profileUrl = "https://youtube.com/@studiopartner",
      contentCategory = "Tech",
      permission = com.example.model.PermissionRecord(
        status = com.example.model.PermissionStatus.PermissionPending,
      ),
      notes = "",
    )

    assertEquals(1, viewModel.uiState.value.sourceStats.creatorSources)
    assertEquals(1, viewModel.uiState.value.sourceStats.permissionPending)
  }

  @Test
  fun `video factory navigation and saved video projects sync with Dashboard and Projects`() = runTest {
    val factoryRepo = com.example.data.factory.InMemoryVideoFactoryRepository()
    val viewModel = ZuzuAppViewModel(videoFactoryRepository = factoryRepo)

    viewModel.openVideoFactory(fromTab = PrimaryTab.Studio)
    assertTrue(viewModel.uiState.value.destination is AppDestination.AiVideoFactory)

    factoryRepo.createOrUpdateProject(
      com.example.model.VideoProjectConfigDraft(
        topic = "Creator Workflow Guide",
        language = com.example.model.VideoLanguage.Bangla,
      ),
    )

    assertEquals(1, viewModel.uiState.value.projects.size)
    assertEquals("Creator Workflow Guide", viewModel.uiState.value.projects.first().title)
    assertEquals(
      1,
      viewModel.uiState.value.metrics.first { it.id == "total_projects" }.count,
    )

    viewModel.navigateBack()
    assertEquals(AppDestination.Tab(PrimaryTab.Studio), viewModel.uiState.value.destination)
  }

  @Test
  fun `connecting unconfigured platform transitions to SetupRequired without faking connection`() = runTest {
    val viewModel = ZuzuAppViewModel()

    viewModel.connectPlatform(SocialPlatform.YouTube)
    val youtubeState = viewModel.uiState.value.platformAccountStates
      .first { it.platform == SocialPlatform.YouTube }

    assertEquals(PlatformConnectionPhase.SetupRequired, youtubeState.phase)
    assertTrue(youtubeState.connectedAccounts.isEmpty())
    assertEquals(0, viewModel.uiState.value.connectedPlatformCount)
  }
}
