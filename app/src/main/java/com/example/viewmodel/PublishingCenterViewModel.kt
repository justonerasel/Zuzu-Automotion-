package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudBackendGateway
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.oauth.SocialAccountsRepository
import com.example.data.phase78.InMemoryPhase78Repository
import com.example.data.phase78.Phase78Repository
import com.example.data.publishing.DefaultPublishingBackendGateway
import com.example.data.publishing.InMemoryPublishingRepository
import com.example.data.publishing.PublishingBackendGateway
import com.example.data.publishing.PublishingRepository
import com.example.data.publishing.PublishingSecurityValidator
import com.example.data.publishing.PublishingValidationResult
import com.example.data.publishing.toPublishingHistoryStatus
import com.example.model.ApprovalContentItem
import com.example.model.ApprovalStatus
import com.example.model.CloudOperationResult
import com.example.model.ConnectedSocialAccount
import com.example.model.PlatformPostCustomization
import com.example.model.PublishDispatchMode
import com.example.model.PublishingAuthErrorCode
import com.example.model.PublishingBackendStatusReport
import com.example.model.PublishingHistoryStatus
import com.example.model.PublishingJobRecord
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.model.TikTokPrivacyLevel
import com.example.model.TikTokPublishMode
import com.example.model.YouTubePrivacyStatus
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PublishingCenterTab(val displayLabel: String) {
  Compose("Publish & Preview"),
  Scheduled("Scheduled"),
  History("History"),
  BackendApi("API & Security"),
}

enum class PublishingHistoryFilter(val displayLabel: String) {
  All("All"),
  Published("Published"),
  Scheduled("Scheduled"),
  Processing("Processing"),
  Failed("Failed"),
  Draft("Draft"),
  Cancelled("Cancelled"),
}

data class PublishingCenterUiState(
  val activeTab: PublishingCenterTab = PublishingCenterTab.Compose,
  val historyFilter: PublishingHistoryFilter = PublishingHistoryFilter.All,
  val approvedItems: List<ApprovalContentItem> = emptyList(),
  val allApprovalItemsCount: Int = 0,
  val selectedApprovalId: String? = null,
  val platformAccountStates: List<SocialPlatformAccountState> = emptyList(),
  val selectedPlatforms: Set<SocialPlatform> = setOf(SocialPlatform.YouTube),
  val selectedAccountIds: Map<SocialPlatform, String> = emptyMap(),
  val customizations: Map<SocialPlatform, PlatformPostCustomization> = defaultCustomizations(),
  val dispatchMode: PublishDispatchMode = PublishDispatchMode.PublishNow,
  val scheduledAtMillis: Long = System.currentTimeMillis() + 3_600_000L,
  val publishingJobs: List<PublishingJobRecord> = emptyList(),
  val backendReport: PublishingBackendStatusReport = PublishingBackendStatusReport(),
  val isSubmitting: Boolean = false,
  val progressStageLabel: String? = null,
  val feedbackMessage: String? = null,
  val errorMessage: String? = null,
) {
  val selectedApprovedItem: ApprovalContentItem?
    get() = approvedItems.firstOrNull { it.id == selectedApprovalId } ?: approvedItems.firstOrNull()

  val nonApprovedBlockedCount: Int
    get() = (allApprovalItemsCount - approvedItems.size).coerceAtLeast(0)
}

private fun defaultCustomizations(): Map<SocialPlatform, PlatformPostCustomization> = mapOf(
  SocialPlatform.YouTube to PlatformPostCustomization(
    platform = SocialPlatform.YouTube,
    youtubePrivacy = YouTubePrivacyStatus.Private,
  ),
  SocialPlatform.Facebook to PlatformPostCustomization(
    platform = SocialPlatform.Facebook,
  ),
  SocialPlatform.TikTok to PlatformPostCustomization(
    platform = SocialPlatform.TikTok,
    tiktokMode = TikTokPublishMode.InboxDraft,
    tiktokPrivacyLevel = TikTokPrivacyLevel.OnlyMe,
  ),
)

class PublishingCenterViewModel(
  private val phase78Repository: Phase78Repository = InMemoryPhase78Repository(),
  private val socialAccountsRepository: SocialAccountsRepository = SocialAccountsRepository(),
  private val publishingRepository: PublishingRepository = InMemoryPublishingRepository(),
  private val publishingBackend: PublishingBackendGateway = DefaultPublishingBackendGateway(),
  private val cloudGateway: CloudBackendGateway = DefaultCloudBackendGateway(),
  private val clockMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

  private val activeTab = MutableStateFlow(PublishingCenterTab.Compose)
  private val historyFilter = MutableStateFlow(PublishingHistoryFilter.All)
  private val selectedApprovalId = MutableStateFlow<String?>(null)
  private val selectedPlatforms = MutableStateFlow(setOf(SocialPlatform.YouTube))
  private val selectedAccountIds = MutableStateFlow<Map<SocialPlatform, String>>(emptyMap())
  private val customizations = MutableStateFlow(defaultCustomizations())
  private val dispatchMode = MutableStateFlow(PublishDispatchMode.PublishNow)
  private val scheduledAtMillis = MutableStateFlow(clockMs() + 3_600_000L)
  private val isSubmitting = MutableStateFlow(false)
  private val progressStageLabel = MutableStateFlow<String?>(null)
  private val feedbackMessage = MutableStateFlow<String?>(null)
  private val errorMessage = MutableStateFlow<String?>(null)

  private val editorStream = combine(
    activeTab,
    historyFilter,
    selectedApprovalId,
    selectedPlatforms,
    selectedAccountIds,
  ) { tab, filter, aprId, platforms, accounts ->
    EditorSelectionSnapshot(tab, filter, aprId, platforms, accounts)
  }

  private val configStream = combine(
    customizations,
    dispatchMode,
    scheduledAtMillis,
    isSubmitting,
    progressStageLabel,
  ) { customMap, mode, schedTs, submitting, stage ->
    ComposerConfigSnapshot(customMap, mode, schedTs, submitting, stage)
  }

  private val messageStream = combine(feedbackMessage, errorMessage) { f, e -> f to e }

  private val dataStream = combine(
    phase78Repository.approvalItems,
    socialAccountsRepository.platformStates,
    publishingRepository.publishingJobs,
    publishingBackend.statusReport,
  ) { approvals, platformStates, jobs, backendReport ->
    DataSnapshot(approvals, platformStates, jobs, backendReport)
  }

  val uiState: StateFlow<PublishingCenterUiState> = combine(
    dataStream,
    editorStream,
    configStream,
    messageStream,
  ) { data, editor, cfg, (feedback, error) ->
    val approved = data.approvals.filter {
      it.status == ApprovalStatus.Approved && it.permissionBlockingReason() == null
    }
    val chosenId = editor.selectedApprovalId?.takeIf { id -> approved.any { it.id == id } }
      ?: approved.firstOrNull()?.id

    // Auto-select first connected account per platform when none explicitly picked yet
    val resolvedAccounts = SocialPlatform.entries.mapNotNull { platform ->
      val pState = data.platformStates.firstOrNull { it.platform == platform }
      val currentId = editor.selectedAccountIds[platform]
      val matched = pState?.connectedAccounts?.firstOrNull { it.accountId == currentId }
        ?: pState?.connectedAccounts?.firstOrNull()
      matched?.let { platform to it.accountId }
    }.toMap()

    PublishingCenterUiState(
      activeTab = editor.activeTab,
      historyFilter = editor.historyFilter,
      approvedItems = approved,
      allApprovalItemsCount = data.approvals.size,
      selectedApprovalId = chosenId,
      platformAccountStates = data.platformStates,
      selectedPlatforms = editor.selectedPlatforms,
      selectedAccountIds = resolvedAccounts,
      customizations = cfg.customizations,
      dispatchMode = cfg.dispatchMode,
      scheduledAtMillis = cfg.scheduledAtMillis,
      publishingJobs = data.jobs,
      backendReport = data.backendReport,
      isSubmitting = cfg.isSubmitting,
      progressStageLabel = cfg.progressStageLabel,
      feedbackMessage = feedback,
      errorMessage = error,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = PublishingCenterUiState(backendReport = publishingBackend.statusReport.value),
  )

  fun openWithContext(approvalId: String? = null, sectionName: String? = null) {
    if (!approvalId.isNullOrBlank()) {
      selectApprovedItem(approvalId)
    }
    sectionName?.let { name ->
      PublishingCenterTab.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?.let { activeTab.value = it }
    }
    refreshBackendStatus()
  }

  fun selectTab(tab: PublishingCenterTab) {
    activeTab.value = tab
    clearMessages()
  }

  fun selectHistoryFilter(filter: PublishingHistoryFilter) {
    historyFilter.value = filter
  }

  fun selectApprovedItem(approvalId: String) {
    selectedApprovalId.value = approvalId
    viewModelScope.launch {
      val item = phase78Repository.getApprovalItem(approvalId) ?: return@launch
      hydrateCustomizationsFromApproval(item)
    }
    clearMessages()
  }

  fun hydrateCustomizationsFromApproval(item: ApprovalContentItem) {
    val cleanTitle = item.title.take(100)
    val baseDesc = item.description.ifBlank { item.title }
    val tags = item.hashtagsCsv
    customizations.update { current ->
      mapOf(
        SocialPlatform.YouTube to (current[SocialPlatform.YouTube] ?: PlatformPostCustomization(SocialPlatform.YouTube)).copy(
          title = cleanTitle,
          description = baseDesc,
          hashtagsCsv = tags,
        ),
        SocialPlatform.Facebook to (current[SocialPlatform.Facebook] ?: PlatformPostCustomization(SocialPlatform.Facebook)).copy(
          title = cleanTitle,
          caption = baseDesc,
          description = baseDesc,
          hashtagsCsv = tags,
        ),
        SocialPlatform.TikTok to (current[SocialPlatform.TikTok] ?: PlatformPostCustomization(SocialPlatform.TikTok)).copy(
          title = cleanTitle,
          caption = baseDesc.take(150),
          hashtagsCsv = tags,
        ),
      )
    }
    if (item.targetPlatforms.isNotEmpty()) {
      selectedPlatforms.value = item.targetPlatforms.toSet()
    }
    item.scheduledAtMillis?.let { ts ->
      if (ts > clockMs()) {
        scheduledAtMillis.value = ts
        dispatchMode.value = PublishDispatchMode.Schedule
      }
    }
  }

  fun togglePlatform(platform: SocialPlatform) {
    selectedPlatforms.update { current ->
      if (current.contains(platform)) current - platform else current + platform
    }
  }

  fun selectAccountForPlatform(platform: SocialPlatform, accountId: String) {
    selectedAccountIds.update { current -> current + (platform to accountId) }
    val account = socialAccountsRepository.platformStates.value
      .firstOrNull { it.platform == platform }
      ?.connectedAccounts
      ?.firstOrNull { it.accountId == accountId }
    if (platform == SocialPlatform.Facebook && account != null) {
      updateCustomization(SocialPlatform.Facebook) {
        it.copy(facebookPageId = account.accountId, facebookPageName = account.displayName)
      }
    }
  }

  fun updateCustomization(
    platform: SocialPlatform,
    transform: (PlatformPostCustomization) -> PlatformPostCustomization,
  ) {
    customizations.update { current ->
      val existing = current[platform] ?: PlatformPostCustomization(platform = platform)
      current + (platform to transform(existing))
    }
  }

  fun setYouTubeThumbnailUri(uriString: String?) {
    updateCustomization(SocialPlatform.YouTube) { it.copy(youtubeThumbnailUriString = uriString) }
  }

  fun setDispatchMode(mode: PublishDispatchMode) {
    dispatchMode.value = mode
    clearMessages()
  }

  fun setScheduledTimestamp(timestampMillis: Long) {
    scheduledAtMillis.value = timestampMillis
  }

  fun refreshBackendStatus() {
    publishingBackend.refreshBackendReport()
  }

  fun savePublishingDraft() {
    val snap = uiState.value
    val item = snap.selectedApprovedItem
    if (item == null) {
      errorMessage.value = "Approve a video or motion post first before saving a publishing draft."
      return
    }
    val platforms = snap.selectedPlatforms.ifEmpty { setOf(SocialPlatform.YouTube) }

    viewModelScope.launch {
      clearMessages()
      val customSnapshot = snap.customizations
      platforms.forEach { platform ->
        val custom = resolveCustomization(platform, customSnapshot, item, null)
        val account = resolveSelectedAccount(platform, snap)
        val jobId = UUID.randomUUID().toString()
        val draft = PublishingJobRecord(
          id = jobId,
          approvalItemId = item.id,
          sourceKind = item.sourceKind,
          postTitle = custom.effectiveSummaryTitle().ifBlank { item.title },
          mediaUriString = item.mediaUriString,
          mediaFilePath = item.mediaFilePath,
          aspectRatio = item.aspectRatio,
          platform = platform,
          accountId = account?.accountId ?: "",
          accountDisplayName = account?.displayName ?: "Unassigned Account",
          accountHandle = account?.handle ?: "Not connected",
          customization = custom,
          dispatchMode = PublishDispatchMode.Draft,
          scheduledAtMillis = null,
          status = PublishingHistoryStatus.Draft,
          idempotencyKey = "draft_${item.id}_${platform.id}_$jobId",
          statusDetail = "Saved locally as Publishing Draft.",
          createdAtMillis = clockMs(),
        )
        publishingRepository.savePublishingJob(draft)
      }
      feedbackMessage.value = "Saved publishing draft for ${platforms.joinToString { it.displayName }}."
    }
  }

  /**
   * Dispatches Publish Now or Schedule across selected platforms.
   * Strictly enforces Mandatory Approval Security + OAuth connection + duplicate-safe idempotency
   * and NEVER claims publishing succeeded without a real backend API confirmation.
   */
  fun executePublishOrSchedule(approvalIdOverride: String? = null) {
    val snap = uiState.value
    val item = approvalIdOverride?.let { id ->
      snap.approvedItems.firstOrNull { it.id == id }
    } ?: snap.selectedApprovedItem

    val targetPlatforms = snap.selectedPlatforms
    if (targetPlatforms.isEmpty()) {
      errorMessage.value = "Select at least one platform to publish."
      return
    }
    if (snap.dispatchMode == PublishDispatchMode.Draft) {
      savePublishingDraft()
      return
    }

    val mode = snap.dispatchMode
    val schedTs = if (mode == PublishDispatchMode.Schedule) snap.scheduledAtMillis else null
    val nowMs = clockMs()

    // Re-fetch authoritative approval item (never trust stale UI selection for pending/rejected items)
    viewModelScope.launch {
      clearMessages()
      val freshApproval = item?.let { phase78Repository.getApprovalItem(it.id) } ?: run {
        val rawSelected = snap.selectedApprovalId?.let { phase78Repository.getApprovalItem(it) }
        rawSelected
      }

      // 1. Validate EVERY selected platform strictly BEFORE starting any dispatch
      val validatedPlans = mutableListOf<ValidatedPlatformPlan>()
      for (platform in targetPlatforms) {
        val pState = snap.platformAccountStates.firstOrNull { it.platform == platform }
        val account = resolveSelectedAccount(platform, snap)
        val custom = resolveCustomization(platform, snap.customizations, freshApproval, account)

        when (
          val validation = PublishingSecurityValidator.validatePreconditions(
            item = freshApproval,
            platformState = pState,
            account = account,
            customization = custom,
            mode = mode,
            scheduledAtMillis = schedTs,
            nowMillis = nowMs,
          )
        ) {
          is PublishingValidationResult.Blocked -> {
            errorMessage.value = validation.reason
            return@launch
          }
          is PublishingValidationResult.Valid -> {
            validatedPlans.add(
              ValidatedPlatformPlan(
                item = freshApproval!!,
                platform = platform,
                account = account!!,
                customization = custom,
              ),
            )
          }
        }
      }

      isSubmitting.value = true
      val session = cloudGateway.authSession.value
      val successMessages = mutableListOf<String>()
      val setupMessages = mutableListOf<String>()
      val failureMessages = mutableListOf<String>()

      for (plan in validatedPlans) {
        val timeKey = if (mode == PublishDispatchMode.Schedule) {
          schedTs.toString()
        } else {
          // 2-minute bucket prevents accidental double-tap duplicate immediate publish
          (nowMs / 120_000L).toString()
        }
        val idempotencyKey = "pub_${plan.item.id}_${plan.platform.id}_${plan.account.accountId}_${mode.name}_$timeKey"
        val existing = publishingRepository.findJobByIdempotencyKey(idempotencyKey)
        if (existing != null && (existing.status == PublishingHistoryStatus.Published ||
            existing.status == PublishingHistoryStatus.Scheduled ||
            existing.status == PublishingHistoryStatus.Processing)
        ) {
          failureMessages.add(
            "${plan.platform.displayName}: Duplicate post blocked (${existing.status.displayLabel} with identical idempotency key).",
          )
          continue
        }

        progressStageLabel.value = "Contacting secure ${plan.platform.displayName} publishing worker..."
        val initialJob = PublishingJobRecord(
          id = UUID.randomUUID().toString(),
          approvalItemId = plan.item.id,
          sourceKind = plan.item.sourceKind,
          postTitle = plan.customization.effectiveSummaryTitle().ifBlank { plan.item.title },
          mediaUriString = plan.item.mediaUriString,
          mediaFilePath = plan.item.mediaFilePath,
          aspectRatio = plan.item.aspectRatio,
          platform = plan.platform,
          accountId = plan.account.accountId,
          accountDisplayName = plan.account.displayName,
          accountHandle = plan.account.handle,
          customization = plan.customization,
          dispatchMode = mode,
          scheduledAtMillis = schedTs,
          status = PublishingHistoryStatus.Processing,
          idempotencyKey = idempotencyKey,
          statusDetail = "Submitting job to HTTPS backend...",
          attemptCount = 1,
          createdAtMillis = nowMs,
        )
        publishingRepository.savePublishingJob(initialJob)

        val outcome = if (mode == PublishDispatchMode.Schedule) {
          publishingBackend.schedulePublishJob(session, initialJob)
        } else {
          publishingBackend.dispatchPublishNow(session, initialJob)
        }

        when (outcome) {
          is CloudOperationResult.Success -> {
            val resp = outcome.data
            val nextStatus = resp.status.toPublishingHistoryStatus()
            val saved = publishingRepository.savePublishingJob(
              initialJob.copy(
                backendJobId = resp.backendJobId,
                remotePlatformPostId = resp.remotePlatformPostId,
                remotePostUrl = resp.remotePostUrl,
                status = nextStatus,
                statusDetail = resp.detailMessage.ifBlank { "Confirmed by official ${plan.platform.displayName} API." },
                errorMessage = null,
                authErrorCode = PublishingAuthErrorCode.None,
              ),
            )
            successMessages.add("${plan.platform.displayName}: ${saved.status.displayLabel}")
          }
          is CloudOperationResult.SetupRequired -> {
            publishingRepository.savePublishingJob(
              initialJob.copy(
                status = PublishingHistoryStatus.Failed,
                authErrorCode = PublishingAuthErrorCode.SetupRequired,
                statusDetail = "Setup Required — publishing blocked without fake success.",
                errorMessage = outcome.missingItems.joinToString(" • "),
              ),
            )
            setupMessages.add("${plan.platform.displayName}: Setup Required")
          }
          is CloudOperationResult.Failure -> {
            val errCode = classifyErrorCode(outcome.message)
            publishingRepository.savePublishingJob(
              initialJob.copy(
                status = PublishingHistoryStatus.Failed,
                authErrorCode = errCode,
                statusDetail = "Backend publishing error",
                errorMessage = outcome.message,
                nextRetryAtMillis = clockMs() + 5 * 60_000L,
              ),
            )
            failureMessages.add("${plan.platform.displayName}: ${outcome.message}")
          }
        }
      }

      isSubmitting.value = false
      progressStageLabel.value = null
      if (successMessages.isNotEmpty() && setupMessages.isEmpty() && failureMessages.isEmpty()) {
        feedbackMessage.value = successMessages.joinToString(" | ")
      } else {
        val combinedErrors = (setupMessages + failureMessages).joinToString("\n")
        if (successMessages.isNotEmpty()) {
          feedbackMessage.value = successMessages.joinToString(" | ")
        }
        errorMessage.value = combinedErrors.ifBlank { null }
      }
    }
  }

  fun editScheduledPublishJob(
    jobId: String,
    newTitle: String,
    newCaption: String,
    newScheduledAtMillis: Long,
  ) {
    viewModelScope.launch {
      clearMessages()
      val existing = publishingRepository.getPublishingJob(jobId) ?: return@launch
      if (!existing.isEditableOrCancellableSchedule) {
        errorMessage.value = "Only scheduled jobs can be edited before publishing."
        return@launch
      }
      if (newScheduledAtMillis <= clockMs()) {
        errorMessage.value = "Updated publishing schedule must be in the future."
        return@launch
      }

      val updatedCustom = existing.customization.copy(
        title = newTitle.trim().ifBlank { existing.customization.title },
        caption = newCaption.trim(),
      )
      val newKey = "pub_${existing.approvalItemId}_${existing.platform.id}_${existing.accountId}_Schedule_$newScheduledAtMillis"
      val duplicate = publishingRepository.findJobByIdempotencyKey(newKey)
      if (duplicate != null && duplicate.id != existing.id && duplicate.status != PublishingHistoryStatus.Cancelled) {
        errorMessage.value = "Duplicate scheduled post prevented for that time."
        return@launch
      }

      val candidate = existing.copy(
        postTitle = updatedCustom.effectiveSummaryTitle().ifBlank { existing.postTitle },
        customization = updatedCustom,
        scheduledAtMillis = newScheduledAtMillis,
        idempotencyKey = newKey,
      )

      when (val res = publishingBackend.updateScheduledPublishJob(cloudGateway.authSession.value, candidate)) {
        is CloudOperationResult.Success -> {
          publishingRepository.savePublishingJob(
            candidate.copy(
              status = PublishingHistoryStatus.Scheduled,
              backendJobId = res.data.backendJobId,
              statusDetail = res.data.detailMessage.ifBlank { "Updated backend scheduled job." },
              errorMessage = null,
            ),
          )
          feedbackMessage.value = "Updated scheduled ${existing.platform.displayName} post."
        }
        is CloudOperationResult.SetupRequired -> {
          errorMessage.value = "${res.title}: ${res.missingItems.firstOrNull().orEmpty()}"
        }
        is CloudOperationResult.Failure -> {
          errorMessage.value = res.message
        }
      }
    }
  }

  fun cancelScheduledPublishJob(jobId: String) {
    viewModelScope.launch {
      clearMessages()
      val existing = publishingRepository.getPublishingJob(jobId) ?: return@launch
      if (!existing.isEditableOrCancellableSchedule) {
        errorMessage.value = "Job is no longer in Scheduled state."
        return@launch
      }
      val res = publishingBackend.cancelScheduledPublishJob(
        session = cloudGateway.authSession.value,
        jobId = existing.id,
        platform = existing.platform,
      )
      when (res) {
        is CloudOperationResult.Success, is CloudOperationResult.SetupRequired -> {
          publishingRepository.savePublishingJob(
            existing.copy(
              status = PublishingHistoryStatus.Cancelled,
              statusDetail = "Cancelled before publishing.",
              errorMessage = null,
            ),
          )
          feedbackMessage.value = "Cancelled scheduled post for ${existing.platform.displayName}."
        }
        is CloudOperationResult.Failure -> {
          errorMessage.value = res.message
        }
      }
    }
  }

  /**
   * Safe retry for Failed publishing jobs:
   * - Enforces attemptCount < maxRetries
   * - Re-validates ApprovalStatus.Approved, PermissionPolicy, and live connected OAuth account
   * - Reuses identical idempotencyKey so the backend never duplicates platform posts.
   */
  fun retryFailedPublishJob(jobId: String) {
    viewModelScope.launch {
      clearMessages()
      val existing = publishingRepository.getPublishingJob(jobId) ?: return@launch
      if (existing.status != PublishingHistoryStatus.Failed) {
        errorMessage.value = "Only Failed publishing jobs can be retried."
        return@launch
      }
      if (existing.attemptCount >= existing.maxRetries) {
        errorMessage.value = "Maximum safe retry attempts (${existing.maxRetries}) reached for '${existing.postTitle}'."
        return@launch
      }

      val approval = phase78Repository.getApprovalItem(existing.approvalItemId)
      val pState = socialAccountsRepository.platformStates.value.firstOrNull { it.platform == existing.platform }
      val account = pState?.connectedAccounts?.firstOrNull { it.accountId == existing.accountId }

      val check = PublishingSecurityValidator.validatePreconditions(
        item = approval,
        platformState = pState,
        account = account,
        customization = existing.customization,
        mode = existing.dispatchMode,
        scheduledAtMillis = existing.scheduledAtMillis,
        nowMillis = clockMs(),
      )
      if (check is PublishingValidationResult.Blocked) {
        publishingRepository.savePublishingJob(
          existing.copy(
            authErrorCode = check.code,
            errorMessage = check.reason,
          ),
        )
        errorMessage.value = check.reason
        return@launch
      }

      val nextAttempt = existing.attemptCount + 1
      publishingRepository.savePublishingJob(
        existing.copy(
          status = PublishingHistoryStatus.Processing,
          attemptCount = nextAttempt,
          statusDetail = "Retrying safely (attempt $nextAttempt/${existing.maxRetries})...",
          errorMessage = null,
        ),
      )

      when (val res = publishingBackend.retryPublishJob(cloudGateway.authSession.value, existing)) {
        is CloudOperationResult.Success -> {
          val nextStatus = res.data.status.toPublishingHistoryStatus()
          publishingRepository.savePublishingJob(
            existing.copy(
              status = nextStatus,
              backendJobId = res.data.backendJobId,
              remotePlatformPostId = res.data.remotePlatformPostId,
              remotePostUrl = res.data.remotePostUrl,
              attemptCount = nextAttempt,
              authErrorCode = PublishingAuthErrorCode.None,
              statusDetail = res.data.detailMessage.ifBlank { "Retry succeeded (${nextStatus.displayLabel})." },
              errorMessage = null,
            ),
          )
          feedbackMessage.value = "Retry succeeded for ${existing.platform.displayName} (${nextStatus.displayLabel})."
        }
        is CloudOperationResult.SetupRequired -> {
          publishingRepository.savePublishingJob(
            existing.copy(
              status = PublishingHistoryStatus.Failed,
              attemptCount = nextAttempt,
              authErrorCode = PublishingAuthErrorCode.SetupRequired,
              errorMessage = res.missingItems.joinToString(" • "),
            ),
          )
          errorMessage.value = "${res.title}: ${res.reason}"
        }
        is CloudOperationResult.Failure -> {
          publishingRepository.savePublishingJob(
            existing.copy(
              status = PublishingHistoryStatus.Failed,
              attemptCount = nextAttempt,
              authErrorCode = classifyErrorCode(res.message),
              errorMessage = res.message,
              nextRetryAtMillis = clockMs() + (nextAttempt * 5 * 60_000L),
            ),
          )
          errorMessage.value = "Retry failed: ${res.message}"
        }
      }
    }
  }

  fun deleteHistoryRecord(jobId: String) {
    viewModelScope.launch {
      publishingRepository.deletePublishingJob(jobId)
      feedbackMessage.value = "Removed publishing history entry."
    }
  }

  fun clearMessages() {
    feedbackMessage.value = null
    errorMessage.value = null
  }

  private fun resolveSelectedAccount(
    platform: SocialPlatform,
    snap: PublishingCenterUiState,
  ): ConnectedSocialAccount? {
    val pState = snap.platformAccountStates.firstOrNull { it.platform == platform } ?: return null
    val chosenId = snap.selectedAccountIds[platform]
    return pState.connectedAccounts.firstOrNull { it.accountId == chosenId }
      ?: pState.connectedAccounts.firstOrNull()
  }

  private fun resolveCustomization(
    platform: SocialPlatform,
    map: Map<SocialPlatform, PlatformPostCustomization>,
    item: ApprovalContentItem?,
    account: ConnectedSocialAccount?,
  ): PlatformPostCustomization {
    val base = map[platform] ?: PlatformPostCustomization(platform = platform)
    return base.copy(
      title = base.title.ifBlank { item?.title.orEmpty() },
      caption = base.caption.ifBlank { item?.description ?: item?.title.orEmpty() },
      description = base.description.ifBlank { item?.description.orEmpty() },
      hashtagsCsv = base.hashtagsCsv.ifBlank { item?.hashtagsCsv.orEmpty() },
      facebookPageId = if (platform == SocialPlatform.Facebook) {
        base.facebookPageId.ifBlank { account?.accountId.orEmpty() }
      } else {
        base.facebookPageId
      },
      facebookPageName = if (platform == SocialPlatform.Facebook) {
        base.facebookPageName.ifBlank { account?.displayName.orEmpty() }
      } else {
        base.facebookPageName
      },
    )
  }

  private fun classifyErrorCode(message: String): PublishingAuthErrorCode {
    val lower = message.lowercase()
    return when {
      lower.contains("expired") || lower.contains("re-authorize") -> PublishingAuthErrorCode.TokenExpired
      lower.contains("scope") || lower.contains("permission") -> PublishingAuthErrorCode.InsufficientScopes
      lower.contains("rate limit") -> PublishingAuthErrorCode.RateLimited
      lower.contains("setup") -> PublishingAuthErrorCode.SetupRequired
      else -> PublishingAuthErrorCode.ApiRejected
    }
  }

  private data class ValidatedPlatformPlan(
    val item: ApprovalContentItem,
    val platform: SocialPlatform,
    val account: ConnectedSocialAccount,
    val customization: PlatformPostCustomization,
  )

  private data class EditorSelectionSnapshot(
    val activeTab: PublishingCenterTab,
    val historyFilter: PublishingHistoryFilter,
    val selectedApprovalId: String?,
    val selectedPlatforms: Set<SocialPlatform>,
    val selectedAccountIds: Map<SocialPlatform, String>,
  )

  private data class ComposerConfigSnapshot(
    val customizations: Map<SocialPlatform, PlatformPostCustomization>,
    val dispatchMode: PublishDispatchMode,
    val scheduledAtMillis: Long,
    val isSubmitting: Boolean,
    val progressStageLabel: String?,
  )

  private data class DataSnapshot(
    val approvals: List<ApprovalContentItem>,
    val platformStates: List<SocialPlatformAccountState>,
    val jobs: List<PublishingJobRecord>,
    val backendReport: PublishingBackendStatusReport,
  )
}
