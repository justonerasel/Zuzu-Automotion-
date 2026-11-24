package com.example.data.cloud

import com.example.model.CloudBackendStatusReport
import com.example.model.CloudServiceHealth
import com.example.model.CloudSubsystemStatus

/**
 * Non-secret cloud infrastructure configuration for Firebase + Google Cloud Automation.
 * Server-side credentials, OAuth client secrets, and service account keys MUST NEVER live
 * in Android source files — they stay on the HTTPS backend (Cloud Run / Cloud Functions).
 */
data class CloudBackendConfig(
  val firebaseProjectId: String = "",
  val firestoreDatabaseId: String = "",
  val cloudStorageBucket: String = "",
  val cloudAutomationBaseUrl: String = "",
  val cloudTasksQueueRegion: String = "asia-east1",
  val cloudTasksQueueName: String = "zuzu-social-publish-queue",
  val cloudSchedulerJobPrefix: String = "zuzu-post-dispatcher",
) {
  val hasFirebaseProject: Boolean
    get() = firebaseProjectId.isNotBlank() && firebaseProjectId != "remixed-project-id"

  val hasFirestoreDatabase: Boolean
    get() = firestoreDatabaseId.isNotBlank()

  val hasStorageBucket: Boolean
    get() = cloudStorageBucket.isNotBlank()

  val hasAutomationEndpoint: Boolean
    get() = cloudAutomationBaseUrl.trim().startsWith("https://")

  val isCloudReady: Boolean
    get() = hasFirebaseProject && hasFirestoreDatabase && hasStorageBucket && hasAutomationEndpoint

  fun missingRequirements(): List<String> = buildList {
    if (!hasFirebaseProject) {
      add("Configure Firebase project & google-services.json (no API secrets in code).")
    }
    if (!hasFirestoreDatabase) {
      add("Configure Firestore database ID for user projects, media metadata, approvals & jobs.")
    }
    if (!hasStorageBucket) {
      add("Configure Firebase Cloud Storage bucket & signed upload policy for MP4/PNG assets.")
    }
    if (!hasAutomationEndpoint) {
      add("Deploy HTTPS Cloud Functions / Cloud Run automation API wired to Cloud Tasks & Cloud Scheduler.")
    }
  }
}

/**
 * Canonical Firestore collection hierarchy & document schemas for multi-user isolation.
 */
object FirestoreSchemaContract {
  const val USERS_COLLECTION = "users"
  const val PROJECTS_SUBCOLLECTION = "projects"
  const val MEDIA_METADATA_SUBCOLLECTION = "media_metadata"
  const val APPROVALS_SUBCOLLECTION = "approvals"
  const val SCHEDULED_POSTS_SUBCOLLECTION = "scheduled_posts"
  const val AUTOMATION_JOBS_SUBCOLLECTION = "automation_jobs"

  fun userProjectsPath(uid: String): String = "$USERS_COLLECTION/$uid/$PROJECTS_SUBCOLLECTION"
  fun mediaMetadataPath(uid: String): String = "$USERS_COLLECTION/$uid/$MEDIA_METADATA_SUBCOLLECTION"
  fun approvalsPath(uid: String): String = "$USERS_COLLECTION/$uid/$APPROVALS_SUBCOLLECTION"
  fun scheduledPostsPath(uid: String): String = "$USERS_COLLECTION/$uid/$SCHEDULED_POSTS_SUBCOLLECTION"
  fun automationJobsPath(uid: String): String = "$USERS_COLLECTION/$uid/$AUTOMATION_JOBS_SUBCOLLECTION"
}

object CloudStatusInspector {
  fun buildReport(
    config: CloudBackendConfig,
    isUserSignedIn: Boolean,
    signedInEmail: String?,
  ): CloudBackendStatusReport {
    val authHealth = when {
      !config.hasFirebaseProject -> CloudServiceHealth.SetupRequired
      isUserSignedIn -> CloudServiceHealth.Ready
      else -> CloudServiceHealth.NotConfigured
    }
    val firestoreHealth = if (config.hasFirebaseProject && config.hasFirestoreDatabase && isUserSignedIn) {
      CloudServiceHealth.Ready
    } else {
      CloudServiceHealth.SetupRequired
    }
    val storageHealth = if (config.hasFirebaseProject && config.hasStorageBucket && isUserSignedIn) {
      CloudServiceHealth.Ready
    } else {
      CloudServiceHealth.SetupRequired
    }
    val tasksHealth = if (config.hasAutomationEndpoint && isUserSignedIn) {
      CloudServiceHealth.Ready
    } else {
      CloudServiceHealth.SetupRequired
    }
    val schedulerHealth = if (config.hasAutomationEndpoint && config.cloudTasksQueueName.isNotBlank()) {
      CloudServiceHealth.Ready
    } else {
      CloudServiceHealth.SetupRequired
    }

    val subsystems = listOf(
      CloudSubsystemStatus(
        id = "auth",
        name = "Firebase Auth (Google Sign-In)",
        detail = when {
          isUserSignedIn -> "Signed in as ${signedInEmail ?: "Verified Creator"}"
          config.hasFirebaseProject -> "Firebase project linked; interactive Google Sign-In ready"
          else -> "Authentication-ready architecture; awaiting google-services.json & Web Client ID"
        },
        health = authHealth,
        missingRequirements = if (config.hasFirebaseProject) emptyList() else listOf("Firebase project & Google Sign-In client registration"),
      ),
      CloudSubsystemStatus(
        id = "firestore",
        name = "Cloud Firestore Schema",
        detail = "Collections: users/{uid}/{projects, media_metadata, approvals, scheduled_posts, automation_jobs}",
        health = firestoreHealth,
        missingRequirements = if (config.hasFirestoreDatabase) emptyList() else listOf("Firestore Database ID & security rules deployment"),
      ),
      CloudSubsystemStatus(
        id = "storage",
        name = "Cloud Storage Media Gateway",
        detail = if (config.hasStorageBucket) {
          "Bucket: gs://${config.cloudStorageBucket}"
        } else {
          "Upload path template: users/{uid}/media/{mediaId} (Setup Required)"
        },
        health = storageHealth,
        missingRequirements = if (config.hasStorageBucket) emptyList() else listOf("Firebase Cloud Storage bucket & IAM upload rules"),
      ),
      CloudSubsystemStatus(
        id = "cloud_tasks",
        name = "Google Cloud Tasks Queue",
        detail = "Queue: ${config.cloudTasksQueueRegion}/${config.cloudTasksQueueName} • Server-side dispatch with idempotency keys",
        health = tasksHealth,
        missingRequirements = if (config.hasAutomationEndpoint) emptyList() else listOf("HTTPS Cloud Run / Cloud Functions task dispatcher endpoint"),
      ),
      CloudSubsystemStatus(
        id = "cloud_scheduler",
        name = "Cloud Scheduler Cron Worker",
        detail = "Runs independently of phone power/connection once backend credentials are active",
        health = schedulerHealth,
        missingRequirements = if (config.hasAutomationEndpoint) emptyList() else listOf("Cloud Scheduler job & server-side OAuth token vault"),
      ),
    )

    val allReady = subsystems.all { it.health == CloudServiceHealth.Ready }
    val missing = config.missingRequirements() + if (!isUserSignedIn) listOf("User Google Sign-In session required before cloud sync.") else emptyList()

    return CloudBackendStatusReport(
      overallHealth = if (allReady) CloudServiceHealth.Ready else CloudServiceHealth.SetupRequired,
      firebaseProjectId = config.firebaseProjectId.takeIf { it.isNotBlank() },
      firestoreDatabaseId = config.firestoreDatabaseId.takeIf { it.isNotBlank() },
      storageBucket = config.cloudStorageBucket.takeIf { it.isNotBlank() },
      automationEndpoint = config.cloudAutomationBaseUrl.takeIf { it.isNotBlank() },
      authStateLabel = if (isUserSignedIn) {
        "Signed in (${signedInEmail ?: "Creator UID"})"
      } else {
        "Signed Out (Authentication-Ready)"
      },
      summaryMessage = if (allReady) {
        "Cloud Backend connected. Projects, media metadata, approvals, and Cloud Tasks schedules synchronize securely."
      } else {
        "Cloud credentials unavailable. Local persistence & validation work reliably offline; cloud sync/publishing remain paused in Setup Required state."
      },
      subsystems = subsystems,
      setupChecklist = missing.distinct(),
      lastCheckedAtMillis = System.currentTimeMillis(),
    )
  }
}
