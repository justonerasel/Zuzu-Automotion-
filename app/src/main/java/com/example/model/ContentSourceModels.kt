package com.example.model

enum class ContentSourceSection(val label: String, val testTag: String) {
  SourceDashboard("Overview", "source_tab_dashboard"),
  MyUploads("My Uploads", "source_tab_uploads"),
  UrlImport("URL Import", "source_tab_url_import"),
  CreatorSources("Creators", "source_tab_creators"),
  MediaLibrary("Library", "source_tab_library"),
  PermissionManager("Permissions", "source_tab_permissions"),
}

enum class MediaType(val displayLabel: String) {
  Video("Video"),
  Image("Image"),
  Audio("Audio"),
  ExternalUrl("External URL"),
}

enum class ContentOrigin(val displayLabel: String) {
  MyUploads("My Uploads"),
  YouTube("YouTube"),
  TikTok("TikTok"),
  Facebook("Facebook"),
  Instagram("Instagram"),
  OtherWebsites("Other Websites"),
}

enum class ExternalPlatformCategory(val displayLabel: String) {
  YouTube("YouTube"),
  TikTok("TikTok"),
  Facebook("Facebook"),
  Instagram("Instagram"),
  OtherWebsites("Other Websites"),
}

enum class PermissionStatus(val displayLabel: String) {
  OriginalContent("Original Content"),
  LicensedContent("Licensed Content"),
  PermissionGranted("Permission Granted"),
  PermissionPending("Permission Pending"),
  PermissionNotGranted("Permission Not Granted"),
}

enum class LibrarySortOrder(val displayLabel: String) {
  NewestFirst("Newest First"),
  OldestFirst("Oldest First"),
}

enum class LibraryViewMode {
  Grid,
  List,
}

data class PermissionRecord(
  val status: PermissionStatus = PermissionStatus.PermissionPending,
  val contentOwner: String = "",
  val evidenceDescription: String = "",
  val licenseReference: String = "",
  val permissionDate: String = "",
  val additionalNotes: String = "",
  val approvedForReuse: Boolean = false,
)

data class MediaLibraryEntry(
  val id: String,
  val title: String,
  val description: String = "",
  val mediaType: MediaType,
  val origin: ContentOrigin,
  val localUriString: String? = null,
  val externalUrl: String? = null,
  val fileName: String? = null,
  val mimeType: String? = null,
  val fileSizeBytes: Long? = null,
  val durationMs: Long? = null,
  val permission: PermissionRecord = PermissionRecord(),
  val notes: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  val isCloudUploaded: Boolean
    get() = false

  val isEligibleForProduction: Boolean
    get() = permission.approvedForReuse && PermissionPolicy.canApproveForReuse(
      origin = origin,
      permission = permission,
    )
}

data class CreatorSourceEntry(
  val id: String,
  val creatorName: String,
  val platform: ExternalPlatformCategory,
  val profileUrl: String,
  val contentCategory: String,
  val permission: PermissionRecord = PermissionRecord(status = PermissionStatus.PermissionPending),
  val notes: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  val isEligibleForProduction: Boolean
    get() = permission.approvedForReuse && PermissionPolicy.canApproveForReuse(
      origin = platform.toContentOrigin(),
      permission = permission,
    )
}

data class SourceActivityEntry(
  val id: String,
  val title: String,
  val detail: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)

data class SourceDashboardStats(
  val totalMediaFiles: Int = 0,
  val uploadedVideos: Int = 0,
  val uploadedImages: Int = 0,
  val audioFiles: Int = 0,
  val savedUrls: Int = 0,
  val creatorSources: Int = 0,
  val permissionVerified: Int = 0,
  val permissionPending: Int = 0,
)

data class PendingLocalUpload(
  val uriString: String,
  val fileName: String,
  val mimeType: String,
  val mediaType: MediaType,
  val fileSizeBytes: Long,
  val durationMs: Long? = null,
)

/**
 * Reusable production-source descriptor prepared for Phase 04+ modules:
 * AI Video Factory, Auto Motion Post Studio, Video Editor, and Content Approval Center.
 */
data class ProductionSourceReference(
  val sourceId: String,
  val title: String,
  val mediaType: MediaType,
  val origin: ContentOrigin,
  val localUriString: String?,
  val externalUrl: String?,
  val permissionStatus: PermissionStatus,
)

object PermissionPolicy {
  fun isVerifiedStatus(status: PermissionStatus): Boolean =
    status == PermissionStatus.OriginalContent ||
      status == PermissionStatus.LicensedContent ||
      status == PermissionStatus.PermissionGranted

  fun approvalBlockingReason(
    origin: ContentOrigin,
    permission: PermissionRecord,
  ): String? {
    if (!isVerifiedStatus(permission.status)) {
      return "Cannot approve for reuse while permission status is '${permission.status.displayLabel}'."
    }
    if (origin != ContentOrigin.MyUploads && permission.status != PermissionStatus.OriginalContent) {
      if (permission.contentOwner.isBlank()) {
        return "Content Owner is required before approving external or licensed content."
      }
      if (permission.evidenceDescription.isBlank() && permission.licenseReference.isBlank()) {
        return "Provide a Permission Evidence description or License Reference before approving."
      }
    }
    return null
  }

  fun canApproveForReuse(
    origin: ContentOrigin,
    permission: PermissionRecord,
  ): Boolean = approvalBlockingReason(origin, permission) == null
}

fun ExternalPlatformCategory.toContentOrigin(): ContentOrigin = when (this) {
  ExternalPlatformCategory.YouTube -> ContentOrigin.YouTube
  ExternalPlatformCategory.TikTok -> ContentOrigin.TikTok
  ExternalPlatformCategory.Facebook -> ContentOrigin.Facebook
  ExternalPlatformCategory.Instagram -> ContentOrigin.Instagram
  ExternalPlatformCategory.OtherWebsites -> ContentOrigin.OtherWebsites
}
