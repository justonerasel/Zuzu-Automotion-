package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.source.ContentSourceRepository
import com.example.data.source.ExternalUrlValidator
import com.example.data.source.InMemoryContentSourceRepository
import com.example.model.ContentOrigin
import com.example.model.ContentSourceSection
import com.example.model.CreatorSourceEntry
import com.example.model.ExternalPlatformCategory
import com.example.model.LibrarySortOrder
import com.example.model.LibraryViewMode
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.PermissionPolicy
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.ProductionSourceReference
import com.example.model.SourceActivityEntry
import com.example.model.SourceDashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContentSourceFilterState(
  val searchQuery: String = "",
  val mediaTypeFilter: MediaType? = null,
  val originFilter: ContentOrigin? = null,
  val sortOrder: LibrarySortOrder = LibrarySortOrder.NewestFirst,
  val viewMode: LibraryViewMode = LibraryViewMode.Grid,
)

data class ContentSourceUiState(
  val activeSection: ContentSourceSection = ContentSourceSection.SourceDashboard,
  val stats: SourceDashboardStats = SourceDashboardStats(),
  val allMediaEntries: List<MediaLibraryEntry> = emptyList(),
  val filteredMediaEntries: List<MediaLibraryEntry> = emptyList(),
  val creatorSources: List<CreatorSourceEntry> = emptyList(),
  val recentActivities: List<SourceActivityEntry> = emptyList(),
  val filterState: ContentSourceFilterState = ContentSourceFilterState(),
  val pendingUpload: PendingLocalUpload? = null,
  val selectedProductionIds: Set<String> = emptySet(),
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val eligibleProductionSources: List<ProductionSourceReference>
    get() = allMediaEntries
      .filter { it.isEligibleForProduction }
      .map { entry ->
        ProductionSourceReference(
          sourceId = entry.id,
          title = entry.title,
          mediaType = entry.mediaType,
          origin = entry.origin,
          localUriString = entry.localUriString,
          externalUrl = entry.externalUrl,
          permissionStatus = entry.permission.status,
        )
      }
}

class ContentSourceViewModel(
  private val repository: ContentSourceRepository = InMemoryContentSourceRepository(),
) : ViewModel() {

  private val activeSection = MutableStateFlow(ContentSourceSection.SourceDashboard)
  private val filterState = MutableStateFlow(ContentSourceFilterState())
  private val pendingUpload = MutableStateFlow<PendingLocalUpload?>(null)
  private val selectedProductionIds = MutableStateFlow<Set<String>>(emptySet())
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private val baseStreams = combine(
    repository.mediaEntries,
    repository.creatorSources,
    repository.recentActivities,
  ) { media, creators, activities ->
    Triple(media, creators, activities)
  }

  private val uiControlStreams = combine(
    activeSection,
    filterState,
    pendingUpload,
    selectedProductionIds,
  ) { section, filter, pending, selectedIds ->
    ControlSnapshot(section, filter, pending, selectedIds)
  }

  private val messageStreams = combine(feedbackMessage, errorMessage) { feedback, error ->
    feedback to error
  }

  val uiState: StateFlow<ContentSourceUiState> = combine(
    baseStreams,
    uiControlStreams,
    messageStreams,
  ) { (media, creators, activities), controls, (feedback, error) ->
    val stats = computeStats(media, creators)
    val filtered = applyFilters(media, controls.filter)
    ContentSourceUiState(
      activeSection = controls.section,
      stats = stats,
      allMediaEntries = media,
      filteredMediaEntries = filtered,
      creatorSources = creators,
      recentActivities = activities,
      filterState = controls.filter,
      pendingUpload = controls.pendingUpload,
      selectedProductionIds = controls.selectedIds,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ContentSourceUiState(),
  )

  private val _rawUrlDraftError = MutableStateFlow<String?>(null)
  val urlDraftError: StateFlow<String?> = _rawUrlDraftError.asStateFlow()

  fun selectSection(section: ContentSourceSection) {
    activeSection.value = section
    clearMessages()
  }

  fun setPendingUpload(upload: PendingLocalUpload) {
    pendingUpload.value = upload
    clearMessages()
  }

  fun setPickerError(message: String) {
    errorMessage.value = message
    feedbackMessage.value = null
  }

  fun cancelPendingUpload() {
    pendingUpload.value = null
  }

  fun savePendingUpload(
    title: String,
    description: String,
    permissionStatus: PermissionStatus,
    notes: String,
  ) {
    val upload = pendingUpload.value ?: return
    val canDefaultApprove = PermissionPolicy.isVerifiedStatus(permissionStatus)
    val permission = PermissionRecord(
      status = permissionStatus,
      contentOwner = if (permissionStatus == PermissionStatus.OriginalContent) "Local Creator" else "",
      approvedForReuse = canDefaultApprove && permissionStatus == PermissionStatus.OriginalContent,
    )

    viewModelScope.launch {
      repository.saveLocalUpload(
        upload = upload,
        title = title,
        description = description,
        permission = permission,
        notes = notes,
      ).onSuccess { saved ->
        pendingUpload.value = null
        feedbackMessage.value = "Saved '${saved.title}' locally to your Media Library."
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to save selected file."
      }
    }
  }

  fun validateUrlDraft(url: String): ExternalPlatformCategory? {
    if (url.isBlank()) {
      _rawUrlDraftError.value = null
      return null
    }
    _rawUrlDraftError.value = ExternalUrlValidator.validate(url)
    return if (_rawUrlDraftError.value == null) {
      ExternalUrlValidator.detectPlatform(url)
    } else {
      null
    }
  }

  fun importExternalUrl(
    url: String,
    platform: ExternalPlatformCategory,
    title: String,
    description: String,
    permissionStatus: PermissionStatus,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.importExternalUrl(
        url = url,
        platform = platform,
        title = title,
        description = description,
        permission = PermissionRecord(
          status = permissionStatus,
          approvedForReuse = false,
        ),
        notes = notes,
      ).onSuccess { saved ->
        _rawUrlDraftError.value = null
        feedbackMessage.value =
          "Saved URL metadata for '${saved.title}'. Review permission before production reuse."
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to save URL."
      }
    }
  }

  fun saveCreatorSource(
    editingId: String?,
    creatorName: String,
    platform: ExternalPlatformCategory,
    profileUrl: String,
    contentCategory: String,
    permissionStatus: PermissionStatus,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.upsertCreatorSource(
        id = editingId,
        creatorName = creatorName,
        platform = platform,
        profileUrl = profileUrl,
        contentCategory = contentCategory,
        permission = PermissionRecord(
          status = permissionStatus,
          contentOwner = creatorName.trim(),
          approvedForReuse = false,
        ),
        notes = notes,
      ).onSuccess { creator ->
        feedbackMessage.value =
          if (editingId == null) "Added creator source '${creator.creatorName}'."
          else "Updated creator source '${creator.creatorName}'."
        errorMessage.value = null
      }.onFailure { err ->
        errorMessage.value = err.message ?: "Unable to save creator source."
      }
    }
  }

  fun removeCreatorSource(id: String) {
    viewModelScope.launch {
      repository.removeCreatorSource(id)
      feedbackMessage.value = "Creator source removed."
    }
  }

  fun updateMediaMetadata(
    id: String,
    title: String,
    description: String,
    notes: String,
  ) {
    viewModelScope.launch {
      repository.updateMediaMetadata(id, title, description, notes)
        .onSuccess {
          feedbackMessage.value = "Media metadata updated."
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Unable to update media metadata."
        }
    }
  }

  fun removeMediaEntry(id: String) {
    viewModelScope.launch {
      repository.removeMediaEntry(id)
      selectedProductionIds.update { it - id }
      feedbackMessage.value = "Media entry removed from library."
    }
  }

  fun updateMediaPermission(
    id: String,
    permission: PermissionRecord,
  ) {
    viewModelScope.launch {
      repository.updateMediaPermission(id, permission)
        .onSuccess {
          feedbackMessage.value = "Permission record saved."
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Permission update rejected."
        }
    }
  }

  fun updateCreatorPermission(
    id: String,
    permission: PermissionRecord,
  ) {
    viewModelScope.launch {
      repository.updateCreatorPermission(id, permission)
        .onSuccess {
          feedbackMessage.value = "Creator permission record saved."
          errorMessage.value = null
        }
        .onFailure { err ->
          errorMessage.value = err.message ?: "Creator permission update rejected."
        }
    }
  }

  fun toggleProductionSelection(id: String) {
    val entry = uiState.value.allMediaEntries.firstOrNull { it.id == id } ?: return
    if (!entry.isEligibleForProduction) {
      errorMessage.value = "Only permission-approved sources can be selected for AI production."
      return
    }
    selectedProductionIds.update { current ->
      if (id in current) current - id else current + id
    }
  }

  fun updateSearchQuery(query: String) {
    filterState.update { it.copy(searchQuery = query) }
  }

  fun updateMediaTypeFilter(type: MediaType?) {
    filterState.update { it.copy(mediaTypeFilter = type) }
  }

  fun updateOriginFilter(origin: ContentOrigin?) {
    filterState.update { it.copy(originFilter = origin) }
  }

  fun toggleSortOrder() {
    filterState.update {
      val next = if (it.sortOrder == LibrarySortOrder.NewestFirst) {
        LibrarySortOrder.OldestFirst
      } else {
        LibrarySortOrder.NewestFirst
      }
      it.copy(sortOrder = next)
    }
  }

  fun setViewMode(mode: LibraryViewMode) {
    filterState.update { it.copy(viewMode = mode) }
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private fun computeStats(
    media: List<MediaLibraryEntry>,
    creators: List<CreatorSourceEntry>,
  ): SourceDashboardStats {
    val verifiedMedia = media.count { PermissionPolicy.isVerifiedStatus(it.permission.status) }
    val pendingMedia = media.count { !PermissionPolicy.isVerifiedStatus(it.permission.status) }
    val verifiedCreators = creators.count { PermissionPolicy.isVerifiedStatus(it.permission.status) }
    val pendingCreators = creators.count { !PermissionPolicy.isVerifiedStatus(it.permission.status) }

    return SourceDashboardStats(
      totalMediaFiles = media.size,
      uploadedVideos = media.count { it.mediaType == MediaType.Video },
      uploadedImages = media.count { it.mediaType == MediaType.Image },
      audioFiles = media.count { it.mediaType == MediaType.Audio },
      savedUrls = media.count { it.mediaType == MediaType.ExternalUrl },
      creatorSources = creators.size,
      permissionVerified = verifiedMedia + verifiedCreators,
      permissionPending = pendingMedia + pendingCreators,
    )
  }

  private fun applyFilters(
    media: List<MediaLibraryEntry>,
    filter: ContentSourceFilterState,
  ): List<MediaLibraryEntry> {
    val query = filter.searchQuery.trim().lowercase()
    val filtered = media.filter { item ->
      val matchesQuery = query.isEmpty() ||
        item.title.lowercase().contains(query) ||
        (item.fileName?.lowercase()?.contains(query) == true) ||
        item.origin.displayLabel.lowercase().contains(query)
      val matchesType = filter.mediaTypeFilter == null || item.mediaType == filter.mediaTypeFilter
      val matchesOrigin = filter.originFilter == null || item.origin == filter.originFilter
      matchesQuery && matchesType && matchesOrigin
    }

    return when (filter.sortOrder) {
      LibrarySortOrder.NewestFirst -> filtered.sortedByDescending { it.createdAtMillis }
      LibrarySortOrder.OldestFirst -> filtered.sortedBy { it.createdAtMillis }
    }
  }

  private data class ControlSnapshot(
    val section: ContentSourceSection,
    val filter: ContentSourceFilterState,
    val pendingUpload: PendingLocalUpload?,
    val selectedIds: Set<String>,
  )
}
