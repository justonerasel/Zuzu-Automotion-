package com.example.data.source

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ContentOrigin
import com.example.model.CreatorSourceEntry
import com.example.model.ExternalPlatformCategory
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.PermissionRecord
import com.example.model.PermissionStatus
import com.example.model.SourceActivityEntry

data class EmbeddedPermission(
  val permissionStatus: String,
  val contentOwner: String,
  val evidenceDescription: String,
  val licenseReference: String,
  val permissionDate: String,
  val additionalNotes: String,
  val approvedForReuse: Boolean,
)

@Entity(tableName = "media_library_entries")
data class MediaLibraryEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val mediaType: String,
  val origin: String,
  val localUriString: String?,
  val externalUrl: String?,
  val fileName: String?,
  val mimeType: String?,
  val fileSizeBytes: Long?,
  val durationMs: Long?,
  @Embedded val permission: EmbeddedPermission,
  val notes: String,
  val createdAtMillis: Long,
)

@Entity(tableName = "creator_sources")
data class CreatorSourceEntity(
  @PrimaryKey val id: String,
  val creatorName: String,
  val platform: String,
  val profileUrl: String,
  val contentCategory: String,
  @Embedded val permission: EmbeddedPermission,
  val notes: String,
  val createdAtMillis: Long,
)

@Entity(tableName = "source_activities")
data class SourceActivityEntity(
  @PrimaryKey val id: String,
  val title: String,
  val detail: String,
  val timestampMillis: Long,
)

internal fun PermissionRecord.toEmbedded(): EmbeddedPermission = EmbeddedPermission(
  permissionStatus = status.name,
  contentOwner = contentOwner,
  evidenceDescription = evidenceDescription,
  licenseReference = licenseReference,
  permissionDate = permissionDate,
  additionalNotes = additionalNotes,
  approvedForReuse = approvedForReuse,
)

internal fun EmbeddedPermission.toDomain(): PermissionRecord = PermissionRecord(
  status = runCatching { PermissionStatus.valueOf(permissionStatus) }
    .getOrDefault(PermissionStatus.PermissionPending),
  contentOwner = contentOwner,
  evidenceDescription = evidenceDescription,
  licenseReference = licenseReference,
  permissionDate = permissionDate,
  additionalNotes = additionalNotes,
  approvedForReuse = approvedForReuse,
)

internal fun MediaLibraryEntity.toDomain(): MediaLibraryEntry = MediaLibraryEntry(
  id = id,
  title = title,
  description = description,
  mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.ExternalUrl),
  origin = runCatching { ContentOrigin.valueOf(origin) }.getOrDefault(ContentOrigin.OtherWebsites),
  localUriString = localUriString,
  externalUrl = externalUrl,
  fileName = fileName,
  mimeType = mimeType,
  fileSizeBytes = fileSizeBytes,
  durationMs = durationMs,
  permission = permission.toDomain(),
  notes = notes,
  createdAtMillis = createdAtMillis,
)

internal fun MediaLibraryEntry.toEntity(): MediaLibraryEntity = MediaLibraryEntity(
  id = id,
  title = title,
  description = description,
  mediaType = mediaType.name,
  origin = origin.name,
  localUriString = localUriString,
  externalUrl = externalUrl,
  fileName = fileName,
  mimeType = mimeType,
  fileSizeBytes = fileSizeBytes,
  durationMs = durationMs,
  permission = permission.toEmbedded(),
  notes = notes,
  createdAtMillis = createdAtMillis,
)

internal fun CreatorSourceEntity.toDomain(): CreatorSourceEntry = CreatorSourceEntry(
  id = id,
  creatorName = creatorName,
  platform = runCatching { ExternalPlatformCategory.valueOf(platform) }
    .getOrDefault(ExternalPlatformCategory.OtherWebsites),
  profileUrl = profileUrl,
  contentCategory = contentCategory,
  permission = permission.toDomain(),
  notes = notes,
  createdAtMillis = createdAtMillis,
)

internal fun CreatorSourceEntry.toEntity(): CreatorSourceEntity = CreatorSourceEntity(
  id = id,
  creatorName = creatorName,
  platform = platform.name,
  profileUrl = profileUrl,
  contentCategory = contentCategory,
  permission = permission.toEmbedded(),
  notes = notes,
  createdAtMillis = createdAtMillis,
)

internal fun SourceActivityEntity.toDomain(): SourceActivityEntry = SourceActivityEntry(
  id = id,
  title = title,
  detail = detail,
  timestampMillis = timestampMillis,
)

internal fun SourceActivityEntry.toEntity(): SourceActivityEntity = SourceActivityEntity(
  id = id,
  title = title,
  detail = detail,
  timestampMillis = timestampMillis,
)
