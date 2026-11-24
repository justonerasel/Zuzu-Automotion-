package com.example.data.source

import com.example.model.ContentOrigin
import com.example.model.CreatorSourceEntry
import com.example.model.ExternalPlatformCategory
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.PendingLocalUpload
import com.example.model.PermissionPolicy
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.ProductionSourceReference
import com.example.model.SourceActivityEntry
import com.example.model.toContentOrigin
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface ContentSourceRepository {
  val mediaEntries: Flow<List<MediaLibraryEntry>>
  val creatorSources: Flow<List<CreatorSourceEntry>>
  val recentActivities: Flow<List<SourceActivityEntry>>

  suspend fun saveLocalUpload(
    upload: PendingLocalUpload,
    title: String,
    description: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<MediaLibraryEntry>

  suspend fun importExternalUrl(
    url: String,
    platform: ExternalPlatformCategory,
    title: String,
    description: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<MediaLibraryEntry>

  suspend fun updateMediaMetadata(
    id: String,
    title: String,
    description: String,
    notes: String,
  ): Result<Unit>

  suspend fun updateMediaPermission(
    id: String,
    permission: PermissionRecord,
  ): Result<Unit>

  suspend fun removeMediaEntry(id: String)

  suspend fun upsertCreatorSource(
    id: String?,
    creatorName: String,
    platform: ExternalPlatformCategory,
    profileUrl: String,
    contentCategory: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<CreatorSourceEntry>

  suspend fun updateCreatorPermission(
    id: String,
    permission: PermissionRecord,
  ): Result<Unit>

  suspend fun removeCreatorSource(id: String)

  suspend fun getEligibleProductionSources(): List<ProductionSourceReference>
}

object ExternalUrlValidator {
  fun validate(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) {
      return "Please enter a valid URL."
    }
    return try {
      val uri = URI(trimmed)
      val scheme = uri.scheme?.lowercase()
      val host = uri.host?.lowercase()
      if (scheme != "https" && scheme != "http") {
        "URL must begin with https:// or http://."
      } else if (host.isNullOrBlank() || !host.contains('.')) {
        "Please enter a complete web URL with a valid domain."
      } else {
        null
      }
    } catch (_: Exception) {
      "URL format is invalid. Example: https://youtube.com/watch?v=..."
    }
  }

  fun detectPlatform(rawUrl: String): ExternalPlatformCategory? {
    val host = runCatching { URI(rawUrl.trim()).host?.lowercase() }.getOrNull() ?: return null
    return when {
      host.contains("youtube.com") || host.contains("youtu.be") -> ExternalPlatformCategory.YouTube
      host.contains("tiktok.com") -> ExternalPlatformCategory.TikTok
      host.contains("facebook.com") || host.contains("fb.watch") -> ExternalPlatformCategory.Facebook
      host.contains("instagram.com") -> ExternalPlatformCategory.Instagram
      else -> ExternalPlatformCategory.OtherWebsites
    }
  }
}

class RoomContentSourceRepository(
  private val dao: ContentSourceDao,
) : ContentSourceRepository {

  override val mediaEntries: Flow<List<MediaLibraryEntry>> =
    dao.observeMediaEntries().map { list -> list.map { it.toDomain() } }

  override val creatorSources: Flow<List<CreatorSourceEntry>> =
    dao.observeCreatorSources().map { list -> list.map { it.toDomain() } }

  override val recentActivities: Flow<List<SourceActivityEntry>> =
    dao.observeRecentActivities().map { list -> list.map { it.toDomain() } }

  override suspend fun saveLocalUpload(
    upload: PendingLocalUpload,
    title: String,
    description: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<MediaLibraryEntry> {
    val cleanTitle = title.trim().ifEmpty { upload.fileName }
    val sanitizedPermission = enforceApprovalPolicy(ContentOrigin.MyUploads, permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }

    val entry = MediaLibraryEntry(
      id = UUID.randomUUID().toString(),
      title = cleanTitle,
      description = description.trim(),
      mediaType = upload.mediaType,
      origin = ContentOrigin.MyUploads,
      localUriString = upload.uriString,
      fileName = upload.fileName,
      mimeType = upload.mimeType,
      fileSizeBytes = upload.fileSizeBytes,
      durationMs = upload.durationMs,
      permission = sanitizedPermission,
      notes = notes.trim(),
    )
    dao.upsertMediaEntry(entry.toEntity())
    logActivity(
      title = "Local Media Saved",
      detail = "${entry.title} (${entry.mediaType.displayLabel}) added to Media Library.",
    )
    return Result.success(entry)
  }

  override suspend fun importExternalUrl(
    url: String,
    platform: ExternalPlatformCategory,
    title: String,
    description: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<MediaLibraryEntry> {
    ExternalUrlValidator.validate(url)?.let { error ->
      return Result.failure(IllegalArgumentException(error))
    }
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Content Title is required."))
    }
    val origin = platform.toContentOrigin()
    val sanitizedPermission = enforceApprovalPolicy(origin, permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }

    val entry = MediaLibraryEntry(
      id = UUID.randomUUID().toString(),
      title = cleanTitle,
      description = description.trim(),
      mediaType = MediaType.ExternalUrl,
      origin = origin,
      externalUrl = url.trim(),
      permission = sanitizedPermission,
      notes = notes.trim(),
    )
    dao.upsertMediaEntry(entry.toEntity())
    logActivity(
      title = "External URL Saved",
      detail = "${entry.title} (${platform.displayLabel}) • ${sanitizedPermission.status.displayLabel}",
    )
    return Result.success(entry)
  }

  override suspend fun updateMediaMetadata(
    id: String,
    title: String,
    description: String,
    notes: String,
  ): Result<Unit> {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Title cannot be empty."))
    }
    val current = dao.observeMediaEntries().first().firstOrNull { it.id == id }?.toDomain()
      ?: return Result.failure(IllegalStateException("Media entry not found."))
    val updated = current.copy(
      title = cleanTitle,
      description = description.trim(),
      notes = notes.trim(),
    )
    dao.upsertMediaEntry(updated.toEntity())
    logActivity("Metadata Updated", "Updated details for ${updated.title}.")
    return Result.success(Unit)
  }

  override suspend fun updateMediaPermission(
    id: String,
    permission: PermissionRecord,
  ): Result<Unit> {
    val current = dao.observeMediaEntries().first().firstOrNull { it.id == id }?.toDomain()
      ?: return Result.failure(IllegalStateException("Media entry not found."))
    val validated = enforceApprovalPolicy(current.origin, permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    dao.upsertMediaEntry(current.copy(permission = validated).toEntity())
    logActivity(
      title = "Permission Updated",
      detail = "${current.title} set to ${validated.status.displayLabel}.",
    )
    return Result.success(Unit)
  }

  override suspend fun removeMediaEntry(id: String) {
    val existing = dao.observeMediaEntries().first().firstOrNull { it.id == id }
    dao.deleteMediaEntry(id)
    if (existing != null) {
      logActivity("Media Removed", "Removed ${existing.title} from local library.")
    }
  }

  override suspend fun upsertCreatorSource(
    id: String?,
    creatorName: String,
    platform: ExternalPlatformCategory,
    profileUrl: String,
    contentCategory: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<CreatorSourceEntry> {
    val cleanName = creatorName.trim()
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("Creator Name is required."))
    }
    ExternalUrlValidator.validate(profileUrl)?.let { error ->
      return Result.failure(IllegalArgumentException(error))
    }
    val cleanCategory = contentCategory.trim()
    if (cleanCategory.isEmpty()) {
      return Result.failure(IllegalArgumentException("Content Category is required."))
    }
    val validatedPermission = enforceApprovalPolicy(platform.toContentOrigin(), permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }

    val entry = CreatorSourceEntry(
      id = id ?: UUID.randomUUID().toString(),
      creatorName = cleanName,
      platform = platform,
      profileUrl = profileUrl.trim(),
      contentCategory = cleanCategory,
      permission = validatedPermission,
      notes = notes.trim(),
    )
    dao.upsertCreatorSource(entry.toEntity())
    logActivity(
      title = if (id == null) "Creator Source Added" else "Creator Source Updated",
      detail = "${entry.creatorName} (${platform.displayLabel}) • ${validatedPermission.status.displayLabel}",
    )
    return Result.success(entry)
  }

  override suspend fun updateCreatorPermission(
    id: String,
    permission: PermissionRecord,
  ): Result<Unit> {
    val current = dao.observeCreatorSources().first().firstOrNull { it.id == id }?.toDomain()
      ?: return Result.failure(IllegalStateException("Creator source not found."))
    val validated = enforceApprovalPolicy(current.platform.toContentOrigin(), permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    dao.upsertCreatorSource(current.copy(permission = validated).toEntity())
    logActivity(
      title = "Creator Permission Updated",
      detail = "${current.creatorName} set to ${validated.status.displayLabel}.",
    )
    return Result.success(Unit)
  }

  override suspend fun removeCreatorSource(id: String) {
    val existing = dao.observeCreatorSources().first().firstOrNull { it.id == id }
    dao.deleteCreatorSource(id)
    if (existing != null) {
      logActivity("Creator Removed", "Removed creator source ${existing.creatorName}.")
    }
  }

  override suspend fun getEligibleProductionSources(): List<ProductionSourceReference> {
    return dao.observeMediaEntries().first()
      .map { it.toDomain() }
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

  private suspend fun logActivity(title: String, detail: String) {
    dao.insertActivity(
      SourceActivityEntry(
        id = UUID.randomUUID().toString(),
        title = title,
        detail = detail,
      ).toEntity(),
    )
  }
}

/**
 * Pure Kotlin in-memory implementation used for fast JVM unit tests and previews.
 */
class InMemoryContentSourceRepository : ContentSourceRepository {
  private val _media = MutableStateFlow<List<MediaLibraryEntry>>(emptyList())
  private val _creators = MutableStateFlow<List<CreatorSourceEntry>>(emptyList())
  private val _activities = MutableStateFlow<List<SourceActivityEntry>>(emptyList())

  override val mediaEntries: Flow<List<MediaLibraryEntry>> = _media.asStateFlow()
  override val creatorSources: Flow<List<CreatorSourceEntry>> = _creators.asStateFlow()
  override val recentActivities: Flow<List<SourceActivityEntry>> = _activities.asStateFlow()

  override suspend fun saveLocalUpload(
    upload: PendingLocalUpload,
    title: String,
    description: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<MediaLibraryEntry> {
    val sanitizedPermission = enforceApprovalPolicy(ContentOrigin.MyUploads, permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    val entry = MediaLibraryEntry(
      id = UUID.randomUUID().toString(),
      title = title.trim().ifEmpty { upload.fileName },
      description = description.trim(),
      mediaType = upload.mediaType,
      origin = ContentOrigin.MyUploads,
      localUriString = upload.uriString,
      fileName = upload.fileName,
      mimeType = upload.mimeType,
      fileSizeBytes = upload.fileSizeBytes,
      durationMs = upload.durationMs,
      permission = sanitizedPermission,
      notes = notes.trim(),
    )
    _media.update { listOf(entry) + it }
    addActivity("Local Media Saved", "${entry.title} saved locally.")
    return Result.success(entry)
  }

  override suspend fun importExternalUrl(
    url: String,
    platform: ExternalPlatformCategory,
    title: String,
    description: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<MediaLibraryEntry> {
    ExternalUrlValidator.validate(url)?.let { return Result.failure(IllegalArgumentException(it)) }
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Content Title is required."))
    }
    val origin = platform.toContentOrigin()
    val sanitizedPermission = enforceApprovalPolicy(origin, permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    val entry = MediaLibraryEntry(
      id = UUID.randomUUID().toString(),
      title = cleanTitle,
      description = description.trim(),
      mediaType = MediaType.ExternalUrl,
      origin = origin,
      externalUrl = url.trim(),
      permission = sanitizedPermission,
      notes = notes.trim(),
    )
    _media.update { listOf(entry) + it }
    addActivity("External URL Saved", "${entry.title} (${platform.displayLabel}) saved.")
    return Result.success(entry)
  }

  override suspend fun updateMediaMetadata(
    id: String,
    title: String,
    description: String,
    notes: String,
  ): Result<Unit> {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Title cannot be empty."))
    _media.update { list ->
      list.map { item ->
        if (item.id == id) {
          item.copy(title = cleanTitle, description = description.trim(), notes = notes.trim())
        } else {
          item
        }
      }
    }
    return Result.success(Unit)
  }

  override suspend fun updateMediaPermission(
    id: String,
    permission: PermissionRecord,
  ): Result<Unit> {
    val target = _media.value.firstOrNull { it.id == id }
      ?: return Result.failure(IllegalStateException("Media entry not found."))
    val validated = enforceApprovalPolicy(target.origin, permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    _media.update { list -> list.map { if (it.id == id) it.copy(permission = validated) else it } }
    addActivity("Permission Updated", "${target.title}: ${validated.status.displayLabel}")
    return Result.success(Unit)
  }

  override suspend fun removeMediaEntry(id: String) {
    _media.update { list -> list.filterNot { it.id == id } }
  }

  override suspend fun upsertCreatorSource(
    id: String?,
    creatorName: String,
    platform: ExternalPlatformCategory,
    profileUrl: String,
    contentCategory: String,
    permission: PermissionRecord,
    notes: String,
  ): Result<CreatorSourceEntry> {
    if (creatorName.trim().isEmpty()) {
      return Result.failure(IllegalArgumentException("Creator Name is required."))
    }
    ExternalUrlValidator.validate(profileUrl)?.let {
      return Result.failure(IllegalArgumentException(it))
    }
    if (contentCategory.trim().isEmpty()) {
      return Result.failure(IllegalArgumentException("Content Category is required."))
    }
    val validated = enforceApprovalPolicy(platform.toContentOrigin(), permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    val entry = CreatorSourceEntry(
      id = id ?: UUID.randomUUID().toString(),
      creatorName = creatorName.trim(),
      platform = platform,
      profileUrl = profileUrl.trim(),
      contentCategory = contentCategory.trim(),
      permission = validated,
      notes = notes.trim(),
    )
    _creators.update { list ->
      val filtered = list.filterNot { it.id == entry.id }
      listOf(entry) + filtered
    }
    addActivity("Creator Saved", "${entry.creatorName} (${platform.displayLabel})")
    return Result.success(entry)
  }

  override suspend fun updateCreatorPermission(
    id: String,
    permission: PermissionRecord,
  ): Result<Unit> {
    val target = _creators.value.firstOrNull { it.id == id }
      ?: return Result.failure(IllegalStateException("Creator source not found."))
    val validated = enforceApprovalPolicy(target.platform.toContentOrigin(), permission)
      .getOrElse { return Result.failure(IllegalArgumentException(it.message)) }
    _creators.update { list -> list.map { if (it.id == id) it.copy(permission = validated) else it } }
    return Result.success(Unit)
  }

  override suspend fun removeCreatorSource(id: String) {
    _creators.update { list -> list.filterNot { it.id == id } }
  }

  override suspend fun getEligibleProductionSources(): List<ProductionSourceReference> {
    return _media.value.filter { it.isEligibleForProduction }.map { entry ->
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

  private fun addActivity(title: String, detail: String) {
    _activities.update {
      (listOf(SourceActivityEntry(UUID.randomUUID().toString(), title, detail)) + it).take(20)
    }
  }
}

private fun enforceApprovalPolicy(
  origin: ContentOrigin,
  permission: PermissionRecord,
): Result<PermissionRecord> {
  if (permission.approvedForReuse) {
    val blockingReason = PermissionPolicy.approvalBlockingReason(origin, permission)
    if (blockingReason != null) {
      return Result.failure(IllegalArgumentException(blockingReason))
    }
  }
  return Result.success(permission)
}
