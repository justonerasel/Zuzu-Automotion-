package com.example

import com.example.data.source.ExternalUrlValidator
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.ContentOrigin
import com.example.model.ExternalPlatformCategory
import com.example.model.LibrarySortOrder
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.viewmodel.ContentSourceViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContentSourceViewModelTest {

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
  fun `initial state starts empty with zero dashboard statistics`() = runTest {
    val viewModel = ContentSourceViewModel(InMemoryContentSourceRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    val stats = viewModel.uiState.value.stats
    assertEquals(0, stats.totalMediaFiles)
    assertEquals(0, stats.uploadedVideos)
    assertEquals(0, stats.uploadedImages)
    assertEquals(0, stats.audioFiles)
    assertEquals(0, stats.savedUrls)
    assertEquals(0, stats.creatorSources)
    assertEquals(0, stats.permissionVerified)
    assertEquals(0, stats.permissionPending)
    assertTrue(viewModel.uiState.value.allMediaEntries.isEmpty())

    collector.cancel()
  }

  @Test
  fun `local upload supports preview staging, cancel, save, and removal without claiming cloud upload`() = runTest {
    val viewModel = ContentSourceViewModel(InMemoryContentSourceRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    val pending = PendingLocalUpload(
      uriString = "content://media/external/video/101",
      fileName = "studio_clip.mp4",
      mimeType = "video/mp4",
      mediaType = MediaType.Video,
      fileSizeBytes = 2_500_000L,
      durationMs = 15_000L,
    )

    viewModel.setPendingUpload(pending)
    assertNotNull(viewModel.uiState.value.pendingUpload)

    viewModel.cancelPendingUpload()
    assertNull(viewModel.uiState.value.pendingUpload)

    viewModel.setPendingUpload(pending)
    viewModel.savePendingUpload(
      title = "Studio Intro Clip",
      description = "B-roll intro",
      permissionStatus = PermissionStatus.OriginalContent,
      notes = "Recorded on phone",
    )

    assertNull(viewModel.uiState.value.pendingUpload)
    assertEquals(1, viewModel.uiState.value.stats.totalMediaFiles)
    assertEquals(1, viewModel.uiState.value.stats.uploadedVideos)
    assertEquals(1, viewModel.uiState.value.stats.permissionVerified)

    val saved = viewModel.uiState.value.allMediaEntries.first()
    assertFalse(saved.isCloudUploaded)
    assertTrue(saved.isEligibleForProduction)
    assertEquals(1, viewModel.uiState.value.eligibleProductionSources.size)

    viewModel.removeMediaEntry(saved.id)
    assertEquals(0, viewModel.uiState.value.stats.totalMediaFiles)

    collector.cancel()
  }

  @Test
  fun `url validation rejects invalid urls and saves valid urls as metadata only`() = runTest {
    val viewModel = ContentSourceViewModel(InMemoryContentSourceRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    assertNotNull(ExternalUrlValidator.validate("not-a-url"))
    assertNull(viewModel.validateUrlDraft("ftp://invalid.host/video.mp4"))
    assertNotNull(viewModel.urlDraftError.value)

    val detected = viewModel.validateUrlDraft("https://www.tiktok.com/@creator/video/12345")
    assertEquals(ExternalPlatformCategory.TikTok, detected)
    assertNull(viewModel.urlDraftError.value)

    viewModel.importExternalUrl(
      url = "https://www.tiktok.com/@creator/video/12345",
      platform = ExternalPlatformCategory.TikTok,
      title = "Partner Dance Clip",
      description = "Authorized reference",
      permissionStatus = PermissionStatus.PermissionPending,
      notes = "Waiting on signed email",
    )

    assertEquals(1, viewModel.uiState.value.stats.savedUrls)
    assertEquals(1, viewModel.uiState.value.stats.permissionPending)
    val savedUrl = viewModel.uiState.value.allMediaEntries.first()
    assertEquals(MediaType.ExternalUrl, savedUrl.mediaType)
    assertEquals(ContentOrigin.TikTok, savedUrl.origin)
    assertFalse(savedUrl.isEligibleForProduction)

    collector.cancel()
  }

  @Test
  fun `permission manager blocks approving reuse when status or evidence is missing and allows verified records`() = runTest {
    val viewModel = ContentSourceViewModel(InMemoryContentSourceRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.importExternalUrl(
      url = "https://youtube.com/watch?v=abc123",
      platform = ExternalPlatformCategory.YouTube,
      title = "Partner Tutorial",
      description = "",
      permissionStatus = PermissionStatus.PermissionPending,
      notes = "",
    )
    val entryId = viewModel.uiState.value.allMediaEntries.first().id

    // Attempt to approve while status is still PermissionPending
    viewModel.updateMediaPermission(
      id = entryId,
      permission = PermissionRecord(
        status = PermissionStatus.PermissionPending,
        contentOwner = "Creator Co",
        evidenceDescription = "DM confirmation",
        approvedForReuse = true,
      ),
    )
    assertNotNull(viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.allMediaEntries.first().isEligibleForProduction)

    // Attempt to approve PermissionGranted without owner/evidence
    viewModel.updateMediaPermission(
      id = entryId,
      permission = PermissionRecord(
        status = PermissionStatus.PermissionGranted,
        contentOwner = "",
        evidenceDescription = "",
        approvedForReuse = true,
      ),
    )
    assertNotNull(viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.allMediaEntries.first().isEligibleForProduction)

    // Valid verified record with owner and license reference succeeds
    viewModel.updateMediaPermission(
      id = entryId,
      permission = PermissionRecord(
        status = PermissionStatus.PermissionGranted,
        contentOwner = "Creator Co",
        evidenceDescription = "Signed creator agreement",
        licenseReference = "LIC-2026-09",
        permissionDate = "2026-11-22",
        approvedForReuse = true,
      ),
    )
    assertNull(viewModel.uiState.value.errorMessage)
    assertTrue(viewModel.uiState.value.allMediaEntries.first().isEligibleForProduction)
    assertEquals(1, viewModel.uiState.value.stats.permissionVerified)

    collector.cancel()
  }

  @Test
  fun `creator sources support add, edit, and remove`() = runTest {
    val viewModel = ContentSourceViewModel(InMemoryContentSourceRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.saveCreatorSource(
      editingId = null,
      creatorName = "Maya Creates",
      platform = ExternalPlatformCategory.Instagram,
      profileUrl = "https://instagram.com/mayacreates",
      contentCategory = "Lifestyle",
      permissionStatus = PermissionStatus.LicensedContent,
      notes = "Annual partner",
    )

    assertEquals(1, viewModel.uiState.value.creatorSources.size)
    val createdId = viewModel.uiState.value.creatorSources.first().id

    viewModel.saveCreatorSource(
      editingId = createdId,
      creatorName = "Maya Studio",
      platform = ExternalPlatformCategory.Instagram,
      profileUrl = "https://instagram.com/mayastudio",
      contentCategory = "Design",
      permissionStatus = PermissionStatus.PermissionGranted,
      notes = "Updated handle",
    )

    assertEquals(1, viewModel.uiState.value.creatorSources.size)
    assertEquals("Maya Studio", viewModel.uiState.value.creatorSources.first().creatorName)

    viewModel.removeCreatorSource(createdId)
    assertTrue(viewModel.uiState.value.creatorSources.isEmpty())

    collector.cancel()
  }

  @Test
  fun `media library filters by search, media type, origin, sort order, and updates metadata`() = runTest {
    val viewModel = ContentSourceViewModel(InMemoryContentSourceRepository())
    val collector = launch(testDispatcher) { viewModel.uiState.collect() }

    viewModel.setPendingUpload(
      PendingLocalUpload(
        uriString = "content://img/1",
        fileName = "banner.png",
        mimeType = "image/png",
        mediaType = MediaType.Image,
        fileSizeBytes = 500_000L,
      ),
    )
    viewModel.savePendingUpload("Product Banner", "", PermissionStatus.OriginalContent, "")

    viewModel.importExternalUrl(
      url = "https://youtube.com/watch?v=xyz",
      platform = ExternalPlatformCategory.YouTube,
      title = "Launch Trailer",
      description = "",
      permissionStatus = PermissionStatus.PermissionPending,
      notes = "",
    )

    viewModel.updateSearchQuery("Banner")
    assertEquals(1, viewModel.uiState.value.filteredMediaEntries.size)
    assertEquals("Product Banner", viewModel.uiState.value.filteredMediaEntries.first().title)

    viewModel.updateSearchQuery("")
    viewModel.updateMediaTypeFilter(MediaType.ExternalUrl)
    assertEquals(1, viewModel.uiState.value.filteredMediaEntries.size)
    assertEquals("Launch Trailer", viewModel.uiState.value.filteredMediaEntries.first().title)

    viewModel.updateMediaTypeFilter(null)
    viewModel.updateOriginFilter(ContentOrigin.MyUploads)
    assertEquals(1, viewModel.uiState.value.filteredMediaEntries.size)

    viewModel.updateOriginFilter(null)
    assertEquals(LibrarySortOrder.NewestFirst, viewModel.uiState.value.filterState.sortOrder)
    viewModel.toggleSortOrder()
    assertEquals(LibrarySortOrder.OldestFirst, viewModel.uiState.value.filterState.sortOrder)

    val bannerId = viewModel.uiState.value.allMediaEntries.first { it.title == "Product Banner" }.id
    viewModel.updateMediaMetadata(bannerId, "Hero Banner v2", "Updated caption", "Ready")
    assertTrue(viewModel.uiState.value.allMediaEntries.any { it.title == "Hero Banner v2" })

    collector.cancel()
  }
}
