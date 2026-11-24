package com.example.ui.screens.publishing

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ApprovalContentItem
import com.example.model.PlatformPostCustomization
import com.example.model.PublishDispatchMode
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.model.TikTokPrivacyLevel
import com.example.model.TikTokPublishMode
import com.example.model.YouTubePrivacyStatus
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuLoadingBanner
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.theme.PlatformFacebookBlue
import com.example.ui.theme.PlatformTikTokCyan
import com.example.ui.theme.PlatformTikTokPink
import com.example.ui.theme.PlatformYouTubeCoral
import com.example.ui.theme.PlatformYouTubeRed
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuOrangeSoft
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPinkOrangeGradient
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.PublishingCenterTab
import com.example.viewmodel.PublishingCenterUiState
import com.example.viewmodel.PublishingCenterViewModel
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun PublishingCenterScreen(
  uiState: PublishingCenterUiState,
  initialApprovalId: String?,
  initialSectionName: String?,
  viewModel: PublishingCenterViewModel,
  glowEnabled: Boolean,
  onOpenApprovalCenter: () -> Unit,
  onOpenSocialAccounts: () -> Unit,
  onOpenAnalytics: () -> Unit = {},
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  LaunchedEffect(initialApprovalId, initialSectionName) {
    viewModel.openWithContext(initialApprovalId, initialSectionName)
  }

  val context = LocalContext.current
  val thumbnailPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      viewModel.setYouTubeThumbnailUri(uri.toString())
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZuzuSecondaryTopBar(
          title = stringResource(R.string.publishing_center_title),
          onBack = onBack,
          backButtonTag = "publishing_back_button",
          trailingContent = {
            ZuzuSecondaryActionButton(
              text = "Analytics",
              onClick = onOpenAnalytics,
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.Analytics,
              modifier = Modifier.testTag("publishing_open_analytics_btn"),
            )
          },
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "YouTube Data API • Meta Pages • TikTok Content Posting",
          title = stringResource(R.string.publishing_center_title),
          subtitle = stringResource(R.string.publishing_center_subtitle),
          icon = Icons.Rounded.Publish,
          gradientColors = ZuzuHeroGradient,
        )
      }

      item {
        ZuzuAlertBanner(
          message = stringResource(R.string.publishing_security_note),
          tint = ZuzuAmber,
          icon = Icons.Rounded.VerifiedUser,
          modifier = Modifier.testTag("publishing_security_banner"),
        )
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PublishingCenterTab.entries.forEach { tab ->
            ZuzuPillChip(
              label = tab.displayLabel,
              selected = uiState.activeTab == tab,
              onClick = { viewModel.selectTab(tab) },
              gradientColors = ZuzuVioletPinkGradient,
              modifier = Modifier.testTag("publishing_tab_${tab.name}"),
            )
          }
        }
      }

      if (!uiState.feedbackMessage.isNullOrBlank() || !uiState.errorMessage.isNullOrBlank()) {
        item {
          val isErr = !uiState.errorMessage.isNullOrBlank()
          ZuzuAlertBanner(
            message = uiState.errorMessage ?: uiState.feedbackMessage.orEmpty(),
            tint = if (isErr) ZuzuError else ZuzuGreen,
            icon = if (isErr) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
            onDismiss = viewModel::clearMessages,
            modifier = Modifier.testTag("publishing_feedback_banner"),
          )
        }
      }

      when (uiState.activeTab) {
        PublishingCenterTab.Compose -> {
          item {
            ApprovedPostSelectorCard(
              uiState = uiState,
              glowEnabled = glowEnabled,
              onSelectApproved = viewModel::selectApprovedItem,
              onOpenApprovalCenter = onOpenApprovalCenter,
            )
          }

          item {
            PlatformSelectionCard(
              platformStates = uiState.platformAccountStates,
              selectedPlatforms = uiState.selectedPlatforms,
              selectedAccountIds = uiState.selectedAccountIds,
              glowEnabled = glowEnabled,
              onTogglePlatform = viewModel::togglePlatform,
              onSelectAccount = viewModel::selectAccountForPlatform,
              onOpenSocialAccounts = onOpenSocialAccounts,
            )
          }

          if (uiState.selectedApprovedItem != null && uiState.selectedPlatforms.isNotEmpty()) {
            uiState.selectedPlatforms.forEach { platform ->
              item(key = "custom_${platform.id}") {
                PlatformCustomizationCard(
                  platform = platform,
                  customization = uiState.customizations[platform] ?: PlatformPostCustomization(platform = platform),
                  dispatchMode = uiState.dispatchMode,
                  glowEnabled = glowEnabled,
                  onUpdate = { transform -> viewModel.updateCustomization(platform, transform) },
                  onPickYouTubeThumbnail = {
                    thumbnailPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  },
                )
              }
            }

            item {
              FinalPostPreviewCard(
                approvedItem = uiState.selectedApprovedItem!!,
                uiState = uiState,
                glowEnabled = glowEnabled,
              )
            }

            item {
              DispatchControlsCard(
                uiState = uiState,
                viewModel = viewModel,
                glowEnabled = glowEnabled,
              )
            }
          }
        }

        PublishingCenterTab.Scheduled -> {
          item {
            PublishingScheduleQueueSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
          }
        }

        PublishingCenterTab.History -> {
          item {
            PublishingHistorySection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
          }
        }

        PublishingCenterTab.BackendApi -> {
          item {
            PublishingBackendApiSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ApprovedPostSelectorCard(
  uiState: PublishingCenterUiState,
  glowEnabled: Boolean,
  onSelectApproved: (String) -> Unit,
  onOpenApprovalCenter: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuPurple, ZuzuElectricBlue),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("publishing_approved_selector"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("1. Approved Content", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      StatusPill(label = "${uiState.approvedItems.size} Approved", tint = if (uiState.approvedItems.isNotEmpty()) ZuzuGreen else ZuzuAmber)
    }
    if (uiState.nonApprovedBlockedCount > 0) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${uiState.nonApprovedBlockedCount} pending/rejected/unverified item(s) are locked out until approved in Approval Center.",
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.approvedItems.isEmpty()) {
      Text(
        text = "No approved videos or motion posts ready to publish yet.",
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(10.dp))
      ZuzuGradientButton(
        text = "Review & Approve in Approval Center",
        onClick = onOpenApprovalCenter,
        gradientColors = ZuzuVioletPinkGradient,
        icon = Icons.Rounded.Approval,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("publishing_goto_approval_btn"),
      )
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        uiState.approvedItems.forEach { item ->
          ZuzuPillChip(
            label = "${item.title} (${item.sourceKind.displayLabel})",
            selected = uiState.selectedApprovedItem?.id == item.id,
            onClick = { onSelectApproved(item.id) },
            gradientColors = ZuzuHeroGradient,
            modifier = Modifier.testTag("pub_approved_chip_${item.id}"),
          )
        }
      }
    }
  }
}

@Composable
private fun PlatformSelectionCard(
  platformStates: List<SocialPlatformAccountState>,
  selectedPlatforms: Set<SocialPlatform>,
  selectedAccountIds: Map<SocialPlatform, String>,
  glowEnabled: Boolean,
  onTogglePlatform: (SocialPlatform) -> Unit,
  onSelectAccount: (SocialPlatform, String) -> Unit,
  onOpenSocialAccounts: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuElectricBlue, ZuzuPink),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("publishing_platforms_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("2. Target Platforms & Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      ZuzuSecondaryActionButton(
        text = "Accounts",
        onClick = onOpenSocialAccounts,
        accentColor = ZuzuElectricBlue,
        icon = Icons.Rounded.Link,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))

    platformStates.forEach { pState ->
      val selected = selectedPlatforms.contains(pState.platform)
      val gradient = when (pState.platform) {
        SocialPlatform.YouTube -> listOf(PlatformYouTubeRed, PlatformYouTubeCoral)
        SocialPlatform.Facebook -> listOf(PlatformFacebookBlue, ZuzuCyan)
        SocialPlatform.TikTok -> listOf(PlatformTikTokCyan, PlatformTikTokPink)
      }
      val icon = when (pState.platform) {
        SocialPlatform.YouTube -> Icons.Rounded.PlayCircle
        SocialPlatform.Facebook -> Icons.Rounded.Public
        SocialPlatform.TikTok -> Icons.Rounded.MusicNote
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ZuzuWhite else ZuzuWhite.copy(alpha = 0.72f),
        border = BorderStroke(
          width = if (selected) 1.5.dp else 1.dp,
          color = if (selected) gradient.first() else ZuzuBorderLight,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(16.dp))
          .clickable { onTogglePlatform(pState.platform) }
          .testTag("pub_platform_card_${pState.platform.id}"),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Brush.linearGradient(gradient)),
              ) {
                Icon(icon, contentDescription = pState.platform.displayName, tint = ZuzuWhite, modifier = Modifier.size(21.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = if (pState.platform == SocialPlatform.Facebook) "Facebook Pages" else pState.platform.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = if (pState.isConnected) {
                    val acct = pState.connectedAccounts.firstOrNull { it.accountId == selectedAccountIds[pState.platform] }
                      ?: pState.connectedAccounts.first()
                    "${acct.displayName} (${acct.handle})"
                  } else {
                    "Not Connected • OAuth Setup Required"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = if (pState.isConnected) ZuzuGreen else ZuzuInkMuted,
                )
              }
            }
            Checkbox(
              checked = selected,
              onCheckedChange = { onTogglePlatform(pState.platform) },
            )
          }

          if (pState.isConnected && pState.connectedAccounts.size > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              pState.connectedAccounts.forEach { acct ->
                ZuzuPillChip(
                  label = acct.handle,
                  selected = selectedAccountIds[pState.platform] == acct.accountId,
                  onClick = { onSelectAccount(pState.platform, acct.accountId) },
                  gradientColors = gradient,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlatformCustomizationCard(
  platform: SocialPlatform,
  customization: PlatformPostCustomization,
  dispatchMode: PublishDispatchMode,
  glowEnabled: Boolean,
  onUpdate: ((PlatformPostCustomization) -> PlatformPostCustomization) -> Unit,
  onPickYouTubeThumbnail: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = ZuzuVioletPinkGradient,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pub_custom_card_${platform.id}"),
  ) {
    Text(
      text = when (platform) {
        SocialPlatform.YouTube -> "3A. YouTube Data API Metadata & Thumbnail"
        SocialPlatform.Facebook -> "3B. Facebook Pages Graph API Post"
        SocialPlatform.TikTok -> "3C. TikTok Content Posting API"
      },
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))

    when (platform) {
      SocialPlatform.YouTube -> {
        OutlinedTextField(
          value = customization.title,
          onValueChange = { txt -> onUpdate { it.copy(title = txt.take(100)) } },
          label = { Text("YouTube Video Title (${customization.title.length}/100)") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.description,
          onValueChange = { txt -> onUpdate { it.copy(description = txt) } },
          label = { Text("Video Description") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.hashtagsCsv,
          onValueChange = { txt -> onUpdate { it.copy(hashtagsCsv = txt) } },
          label = { Text("Tags & Hashtags") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Privacy Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          YouTubePrivacyStatus.entries.forEach { priv ->
            ZuzuPillChip(
              label = priv.displayLabel,
              selected = customization.youtubePrivacy == priv,
              onClick = { onUpdate { it.copy(youtubePrivacy = priv) } },
              gradientColors = listOf(PlatformYouTubeRed, PlatformYouTubeCoral),
            )
          }
        }
        if (dispatchMode == PublishDispatchMode.Schedule && customization.youtubePrivacy != YouTubePrivacyStatus.Private) {
          Text(
            text = "Note: YouTube scheduled publishAt requires initial privacy to be Private.",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuAmber,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ZuzuSecondaryActionButton(
          text = if (customization.youtubeThumbnailUriString != null) "Custom Thumbnail Selected ✓" else "Select Custom Thumbnail",
          onClick = onPickYouTubeThumbnail,
          accentColor = PlatformYouTubeRed,
          icon = Icons.Rounded.Image,
        )
      }

      SocialPlatform.Facebook -> {
        Surface(shape = RoundedCornerShape(10.dp), color = ZuzuOrangeSoft, modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = ZuzuAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.publishing_fb_pages_only_note), style = MaterialTheme.typography.bodySmall)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.caption,
          onValueChange = { txt -> onUpdate { it.copy(caption = txt) } },
          label = { Text("Page Post Caption / Status") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.description,
          onValueChange = { txt -> onUpdate { it.copy(description = txt) } },
          label = { Text("Video Description") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.hashtagsCsv,
          onValueChange = { txt -> onUpdate { it.copy(hashtagsCsv = txt) } },
          label = { Text("Hashtags") },
          modifier = Modifier.fillMaxWidth(),
        )
      }

      SocialPlatform.TikTok -> {
        Surface(shape = RoundedCornerShape(10.dp), color = ZuzuOrangeSoft, modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = ZuzuAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.publishing_tiktok_inbox_note), style = MaterialTheme.typography.bodySmall)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TikTokPublishMode.entries.forEach { ttMode ->
            ZuzuPillChip(
              label = ttMode.displayLabel,
              selected = customization.tiktokMode == ttMode,
              onClick = { onUpdate { it.copy(tiktokMode = ttMode) } },
              gradientColors = listOf(PlatformTikTokCyan, PlatformTikTokPink),
            )
          }
        }
        Text(customization.tiktokMode.description, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.caption,
          onValueChange = { txt -> onUpdate { it.copy(caption = txt.take(2200)) } },
          label = { Text("TikTok Caption") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = customization.hashtagsCsv,
          onValueChange = { txt -> onUpdate { it.copy(hashtagsCsv = txt) } },
          label = { Text("TikTok Hashtags") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          TikTokPrivacyLevel.entries.forEach { priv ->
            ZuzuPillChip(
              label = priv.displayLabel,
              selected = customization.tiktokPrivacyLevel == priv,
              onClick = { onUpdate { it.copy(tiktokPrivacyLevel = priv) } },
              gradientColors = listOf(PlatformTikTokCyan, PlatformTikTokPink),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FinalPostPreviewCard(
  approvedItem: ApprovalContentItem,
  uiState: PublishingCenterUiState,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  val ytThumbUri = uiState.customizations[SocialPlatform.YouTube]?.youtubeThumbnailUriString
  val previewBitmap = remember(ytThumbUri, approvedItem.mediaFilePath) {
    when {
      !ytThumbUri.isNullOrBlank() -> runCatching {
        context.contentResolver.openInputStream(Uri.parse(ytThumbUri))?.use { BitmapFactory.decodeStream(it) }
      }.getOrNull()
      approvedItem.mediaFilePath?.lowercase()?.let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") } == true ->
        File(approvedItem.mediaFilePath).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
      else -> null
    }
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = ZuzuBlueTurquoiseGradient,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("publishing_final_preview_card"),
  ) {
    Text("4. Final Post Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (previewBitmap != null) {
      Image(
        bitmap = previewBitmap.asImageBitmap(),
        contentDescription = approvedItem.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(12.dp)),
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.selectedPlatforms.forEach { platform ->
      val custom = uiState.customizations[platform] ?: PlatformPostCustomization(platform = platform)
      val pState = uiState.platformAccountStates.firstOrNull { it.platform == platform }
      val account = pState?.connectedAccounts?.firstOrNull { it.accountId == uiState.selectedAccountIds[platform] }
        ?: pState?.connectedAccounts?.firstOrNull()

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = ZuzuWhite,
        border = BorderStroke(1.dp, ZuzuBorderLight),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "${platform.displayName} • ${account?.handle ?: "Not Connected"}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = ZuzuPurple,
            )
            StatusPill(
              label = when (platform) {
                SocialPlatform.YouTube -> custom.youtubePrivacy.displayLabel
                SocialPlatform.Facebook -> "Page Post"
                SocialPlatform.TikTok -> custom.tiktokMode.displayLabel
              },
              tint = ZuzuElectricBlue,
            )
          }
          Text(custom.effectiveSummaryTitle().ifBlank { approvedItem.title }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
          if (custom.description.isNotBlank()) {
            Text(custom.description, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
          }
          if (custom.hashtagsCsv.isNotBlank()) {
            Text(custom.hashtagsCsv, style = MaterialTheme.typography.labelSmall, color = ZuzuTurquoise)
          }
        }
      }
    }
  }
}

@Composable
private fun DispatchControlsCard(
  uiState: PublishingCenterUiState,
  viewModel: PublishingCenterViewModel,
  glowEnabled: Boolean,
) {
  val context = LocalContext.current
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = ZuzuPinkOrangeGradient,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("5. Publish or Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(PublishDispatchMode.PublishNow, PublishDispatchMode.Schedule).forEach { mode ->
        ZuzuPillChip(
          label = mode.displayLabel,
          selected = uiState.dispatchMode == mode,
          onClick = { viewModel.setDispatchMode(mode) },
          gradientColors = ZuzuHeroGradient,
          modifier = Modifier.testTag("pub_mode_${mode.name}"),
        )
      }
    }

    if (uiState.dispatchMode == PublishDispatchMode.Schedule) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Target Publish Time: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(uiState.scheduledAtMillis))}",
        style = MaterialTheme.typography.labelMedium,
        color = ZuzuTurquoise,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ZuzuSecondaryActionButton(
          text = "+1 Hour",
          onClick = { viewModel.setScheduledTimestamp(System.currentTimeMillis() + 3_600_000L) },
          accentColor = ZuzuElectricBlue,
        )
        ZuzuSecondaryActionButton(
          text = "+1 Day",
          onClick = { viewModel.setScheduledTimestamp(System.currentTimeMillis() + 86_400_000L) },
          accentColor = ZuzuPurple,
        )
        ZuzuSecondaryActionButton(
          text = "Pick Date/Time",
          onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = uiState.scheduledAtMillis }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                TimePickerDialog(
                  context,
                  { _, h, min ->
                    cal.set(y, m, d, h, min, 0)
                    viewModel.setScheduledTimestamp(cal.timeInMillis)
                  },
                  cal.get(Calendar.HOUR_OF_DAY),
                  cal.get(Calendar.MINUTE),
                  false,
                ).show()
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH),
            ).show()
          },
          accentColor = ZuzuTurquoise,
          icon = Icons.Rounded.CalendarMonth,
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    if (uiState.isSubmitting) {
      ZuzuLoadingBanner(
        label = uiState.progressStageLabel ?: "Dispatching publishing request…",
        accentColor = ZuzuElectricBlue,
      )
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ZuzuSecondaryActionButton(
          text = "Save Draft",
          onClick = viewModel::savePublishingDraft,
          accentColor = ZuzuPurple,
          icon = Icons.Rounded.Save,
          modifier = Modifier
            .weight(0.9f)
            .testTag("pub_save_draft_btn"),
        )
        ZuzuGradientButton(
          text = if (uiState.dispatchMode == PublishDispatchMode.Schedule) "Schedule Post" else "Publish Now",
          onClick = { viewModel.executePublishOrSchedule() },
          gradientColors = ZuzuHeroGradient,
          icon = if (uiState.dispatchMode == PublishDispatchMode.Schedule) Icons.Rounded.Schedule else Icons.Rounded.Publish,
          modifier = Modifier
            .weight(1.3f)
            .testTag("pub_dispatch_btn"),
        )
      }
    }
  }
}
