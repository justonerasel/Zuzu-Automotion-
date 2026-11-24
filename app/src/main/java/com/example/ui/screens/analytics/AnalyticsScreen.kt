package com.example.ui.screens.analytics

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AnalyticsContentSection
import com.example.model.AnalyticsDateRange
import com.example.model.AnalyticsMetricAvailability
import com.example.model.AnalyticsPlatformFilter
import com.example.model.ContentPerformanceItem
import com.example.model.PlatformAnalyticsSummary
import com.example.model.PublishingHistoryStatus
import com.example.model.SocialPlatform
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientCard
import com.example.ui.components.ZuzuGradientIconBadge
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
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPinkOrangeGradient
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.AnalyticsUiState
import com.example.viewmodel.AnalyticsViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun AnalyticsScreen(
  uiState: AnalyticsUiState,
  initialContentJobId: String?,
  viewModel: AnalyticsViewModel,
  glowEnabled: Boolean,
  onOpenSocialAccounts: () -> Unit,
  onOpenPublishingCenter: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  LaunchedEffect(initialContentJobId) {
    if (!initialContentJobId.isNullOrBlank()) {
      viewModel.openContentDetail(initialContentJobId)
    }
  }

  val visiblePlatformSummaries = remember(uiState.platformSummaries, uiState.platformFilter) {
    uiState.platformSummaries.filter { uiState.platformFilter.matches(it.platform) }
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
          title = stringResource(R.string.analytics_title),
          onBack = onBack,
          backButtonTag = "analytics_back_button",
          trailingContent = {
            ZuzuSecondaryActionButton(
              text = if (uiState.isSyncing) "Syncing…" else "Refresh",
              onClick = viewModel::syncRemoteAnalytics,
              enabled = !uiState.isSyncing,
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.Refresh,
              modifier = Modifier.testTag("analytics_refresh_button"),
            )
          },
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "Real Telemetry • Zero Fake Metrics",
          title = stringResource(R.string.analytics_title),
          subtitle = stringResource(R.string.analytics_subtitle),
          icon = Icons.Rounded.Analytics,
          gradientColors = ZuzuHeroGradient,
        )
      }

      if (uiState.isSyncing) {
        item {
          ZuzuLoadingBanner(
            label = "Refreshing official analytics telemetry…",
            accentColor = ZuzuElectricBlue,
          )
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
            modifier = Modifier.testTag("analytics_feedback_banner"),
          )
        }
      }

      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = ZuzuBlueTurquoiseGradient,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Date Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AnalyticsDateRange.entries.forEach { range ->
              ZuzuPillChip(
                label = range.displayLabel,
                selected = uiState.dateRange == range,
                onClick = { viewModel.selectDateRange(range) },
                gradientColors = ZuzuBlueTurquoiseGradient,
                modifier = Modifier.testTag("analytics_date_${range.name}"),
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text("Platform Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AnalyticsPlatformFilter.entries.forEach { pf ->
              ZuzuPillChip(
                label = pf.displayLabel,
                selected = uiState.platformFilter == pf,
                onClick = { viewModel.selectPlatformFilter(pf) },
                gradientColors = ZuzuVioletPinkGradient,
                modifier = Modifier.testTag("analytics_platform_${pf.name}"),
              )
            }
          }
        }
      }

      item {
        AnalyticsKpiGrid(
          uiState = uiState,
          glowEnabled = glowEnabled,
        )
      }

      if (!uiState.overallEngagement.hasLiveNumbers) {
        item {
          ZuzuAlertBanner(
            title = "${uiState.overallEngagement.availability.badgeLabel} — Live Social API Metrics",
            message = uiState.overallEngagement.availabilityNote,
            tint = ZuzuAmber,
            icon = Icons.Rounded.Info,
            modifier = Modifier.testTag("analytics_honesty_banner"),
          )
        }
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AnalyticsContentSection.entries.forEach { section ->
            ZuzuPillChip(
              label = section.displayLabel,
              selected = uiState.activeSection == section,
              onClick = { viewModel.selectSection(section) },
              gradientColors = listOf(ZuzuDeepPurple, ZuzuElectricBlue),
              modifier = Modifier.testTag("analytics_section_${section.name}"),
            )
          }
        }
      }

      when (uiState.activeSection) {
        AnalyticsContentSection.Overview -> {
          item {
            PublishingTrendsChartCard(
              trends = uiState.engagementTrends,
              glowEnabled = glowEnabled,
            )
          }
          item {
            PlatformComparisonChartCard(
              summaries = visiblePlatformSummaries,
              glowEnabled = glowEnabled,
            )
          }
          item {
            SectionHeader(
              title = "Platform-Wise Performance",
              gradientColors = ZuzuBlueTurquoiseGradient,
            )
          }
          items(visiblePlatformSummaries, key = { it.platform.id }) { summary ->
            PlatformSummaryCard(
              summary = summary,
              glowEnabled = glowEnabled,
              onOpenSocialAccounts = onOpenSocialAccounts,
            )
          }
          item {
            SectionHeader(
              title = "Recent Publishing Activity",
              gradientColors = ZuzuVioletPinkGradient,
            )
          }
          if (uiState.publishingHistoryItems.isEmpty()) {
            item {
              ZuzuEmptyStatePanel(
                title = "No Publishing Activity In Selected Range",
                description = "Publish or schedule approved videos or motion posts in Publishing Center to track status and official performance.",
                icon = Icons.Rounded.Publish,
                gradientColors = ZuzuHeroGradient,
                glowEnabled = glowEnabled,
                ctaLabel = "Open Publishing Center",
                ctaIcon = Icons.Rounded.Publish,
                onCtaClick = onOpenPublishingCenter,
              )
            }
          } else {
            items(uiState.publishingHistoryItems.take(5), key = { "act_${it.id}" }) { item ->
              ContentPerformanceCard(
                item = item,
                glowEnabled = glowEnabled,
                onClick = { viewModel.openContentDetail(item.id) },
              )
            }
          }
        }

        AnalyticsContentSection.BestPerforming -> {
          item {
            SectionHeader(
              title = "Best Performing Content",
              gradientColors = ZuzuPinkOrangeGradient,
            )
          }
          if (uiState.bestPerformingContent.isEmpty()) {
            item {
              ZuzuEmptyStatePanel(
                title = "No Published Posts Ranked Yet",
                description = "Only officially published posts appear in Best Performing rankings. Views and likes require live Analytics API access.",
                icon = Icons.Rounded.Analytics,
                gradientColors = ZuzuVioletPinkGradient,
                glowEnabled = glowEnabled,
                ctaLabel = "Go to Publishing Center",
                ctaIcon = Icons.Rounded.Publish,
                onCtaClick = onOpenPublishingCenter,
              )
            }
          } else {
            items(uiState.bestPerformingContent, key = { "best_${it.id}" }) { item ->
              ContentPerformanceCard(
                item = item,
                glowEnabled = glowEnabled,
                onClick = { viewModel.openContentDetail(item.id) },
              )
            }
          }
        }

        AnalyticsContentSection.RecentPosts -> {
          if (uiState.recentPosts.isEmpty()) {
            item {
              ZuzuEmptyStatePanel(
                title = "No Content Items Found",
                description = "Create videos, motion posts, or published jobs to review content performance here.",
                icon = Icons.Rounded.Publish,
                gradientColors = ZuzuBlueTurquoiseGradient,
                glowEnabled = glowEnabled,
                ctaLabel = "Open Publishing Center",
                ctaIcon = Icons.Rounded.Publish,
                onCtaClick = onOpenPublishingCenter,
              )
            }
          } else {
            items(uiState.recentPosts, key = { "recent_${it.id}" }) { item ->
              ContentPerformanceCard(
                item = item,
                glowEnabled = glowEnabled,
                onClick = { viewModel.openContentDetail(item.id) },
              )
            }
          }
        }

        AnalyticsContentSection.PlatformComparison -> {
          item {
            PlatformComparisonChartCard(
              summaries = visiblePlatformSummaries,
              glowEnabled = glowEnabled,
            )
          }
          items(visiblePlatformSummaries, key = { "cmp_${it.platform.id}" }) { summary ->
            PlatformSummaryCard(
              summary = summary,
              glowEnabled = glowEnabled,
              onOpenSocialAccounts = onOpenSocialAccounts,
            )
          }
        }

        AnalyticsContentSection.VideoPerformance -> {
          if (uiState.videoPerformanceItems.isEmpty()) {
            item {
              ZuzuEmptyStatePanel(
                title = "No Video Performance Records Yet",
                description = "Render MP4 videos in AI Video Factory or Auto Motion Studio and publish them to see video performance.",
                icon = Icons.Rounded.Analytics,
                gradientColors = ZuzuHeroGradient,
                glowEnabled = glowEnabled,
                ctaLabel = "Open Publishing Center",
                ctaIcon = Icons.Rounded.Publish,
                onCtaClick = onOpenPublishingCenter,
              )
            }
          } else {
            items(uiState.videoPerformanceItems, key = { "vid_${it.id}" }) { item ->
              ContentPerformanceCard(
                item = item,
                glowEnabled = glowEnabled,
                onClick = { viewModel.openContentDetail(item.id) },
              )
            }
          }
        }

        AnalyticsContentSection.EngagementTrends -> {
          item {
            PublishingTrendsChartCard(
              trends = uiState.engagementTrends,
              glowEnabled = glowEnabled,
            )
          }
          items(uiState.engagementTrends.reversed(), key = { "trend_${it.label}" }) { point ->
            ZuzuGlassCard(
              glowEnabled = glowEnabled,
              accentColors = ZuzuBlueTurquoiseGradient,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(point.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                StatusPill(
                  label = "${point.publishedCount} published • ${point.scheduledCount} scheduled",
                  tint = ZuzuElectricBlue,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (point.views != null) {
                  "Live views: ${point.views} • Likes: ${point.likes ?: 0}"
                } else {
                  "Official engagement telemetry: ${uiState.overallEngagement.availability.badgeLabel}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuInkSecondary,
              )
            }
          }
        }

        AnalyticsContentSection.PublishingHistory -> {
          if (uiState.publishingHistoryItems.isEmpty()) {
            item {
              ZuzuEmptyStatePanel(
                title = "No Publishing History Yet",
                description = "Drafts, scheduled posts, and official publish attempts appear here with exact API status.",
                icon = Icons.Rounded.Publish,
                gradientColors = ZuzuVioletPinkGradient,
                glowEnabled = glowEnabled,
                ctaLabel = "Open Publishing Center",
                ctaIcon = Icons.Rounded.Publish,
                onCtaClick = onOpenPublishingCenter,
              )
            }
          } else {
            items(uiState.publishingHistoryItems, key = { "hist_${it.id}" }) { item ->
              ContentPerformanceCard(
                item = item,
                glowEnabled = glowEnabled,
                onClick = { viewModel.openContentDetail(item.id) },
              )
            }
          }
        }
      }
    }
  }

  uiState.selectedContentDetail?.let { detail ->
    ContentPerformanceDetailDialog(
      item = detail,
      onDismiss = viewModel::closeContentDetail,
      onOpenPublishingCenter = {
        viewModel.closeContentDetail()
        onOpenPublishingCenter()
      },
    )
  }
}

@Composable
private fun AnalyticsKpiGrid(
  uiState: AnalyticsUiState,
  glowEnabled: Boolean,
) {
  val engagement = uiState.overallEngagement
  val availabilityTint = when (engagement.availability) {
    AnalyticsMetricAvailability.Available -> ZuzuGreen
    AnalyticsMetricAvailability.NotConnected -> ZuzuInkMuted
    AnalyticsMetricAvailability.SetupRequired -> ZuzuAmber
    AnalyticsMetricAvailability.DataUnavailable -> ZuzuElectricBlue
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      PipelineCountCard(
        label = "Published Posts",
        value = uiState.totalPublishedPosts.toString(),
        gradient = listOf(ZuzuDeepPurple, ZuzuPurple),
        modifier = Modifier
          .weight(1f)
          .testTag("kpi_published_posts"),
      )
      PipelineCountCard(
        label = "Scheduled Posts",
        value = uiState.totalScheduledPosts.toString(),
        gradient = ZuzuBlueTurquoiseGradient,
        modifier = Modifier
          .weight(1f)
          .testTag("kpi_scheduled_posts"),
      )
      PipelineCountCard(
        label = "Videos Created",
        value = uiState.totalVideosCreated.toString(),
        gradient = ZuzuPinkOrangeGradient,
        modifier = Modifier
          .weight(1f)
          .testTag("kpi_videos_created"),
      )
    }

    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = ZuzuHeroGradient,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("analytics_engagement_summary_card"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Official Engagement Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        StatusPill(label = engagement.availability.badgeLabel, tint = availabilityTint)
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricMiniBox(label = "Total Views", value = engagement.formattedViews(), modifier = Modifier.weight(1f))
        MetricMiniBox(label = "Likes", value = engagement.formattedLikes(), modifier = Modifier.weight(1f))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricMiniBox(label = "Comments", value = engagement.formattedComments(), modifier = Modifier.weight(1f))
        MetricMiniBox(label = "Shares", value = engagement.formattedShares(), modifier = Modifier.weight(1f))
        MetricMiniBox(label = "Engagement Rate", value = engagement.formattedEngagementRate(), modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun PipelineCountCard(
  label: String,
  value: String,
  gradient: List<Color>,
  modifier: Modifier = Modifier,
) {
  ZuzuGradientCard(
    gradientColors = gradient,
    modifier = modifier,
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      color = ZuzuWhite,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = ZuzuWhite.copy(alpha = 0.92f),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MetricMiniBox(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = ZuzuWhite,
    border = BorderStroke(1.dp, ZuzuBorderLight),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = ZuzuInkSecondary)
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PlatformSummaryCard(
  summary: PlatformAnalyticsSummary,
  glowEnabled: Boolean,
  onOpenSocialAccounts: () -> Unit,
) {
  val (gradient, icon) = when (summary.platform) {
    SocialPlatform.YouTube -> listOf(PlatformYouTubeRed, PlatformYouTubeCoral) to Icons.Rounded.PlayCircle
    SocialPlatform.Facebook -> listOf(PlatformFacebookBlue, ZuzuCyan) to Icons.Rounded.Public
    SocialPlatform.TikTok -> listOf(PlatformTikTokCyan, PlatformTikTokPink) to Icons.Rounded.MusicNote
  }
  val statusTint = when (summary.metrics.availability) {
    AnalyticsMetricAvailability.Available -> ZuzuGreen
    AnalyticsMetricAvailability.NotConnected -> ZuzuInkMuted
    AnalyticsMetricAvailability.SetupRequired -> ZuzuAmber
    AnalyticsMetricAvailability.DataUnavailable -> ZuzuElectricBlue
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradient,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("analytics_platform_summary_${summary.platform.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        ZuzuGradientIconBadge(
          icon = icon,
          contentDescription = summary.platform.displayName,
          gradientColors = gradient,
          size = 42.dp,
          iconSize = 21.dp,
          cornerRadius = 13.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(summary.platform.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(
            text = if (summary.connectedHandles.isNotEmpty()) {
              summary.connectedHandles.joinToString(", ")
            } else {
              summary.apiCapabilityNote
            },
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      StatusPill(label = summary.metrics.availability.badgeLabel, tint = statusTint)
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      MetricMiniBox("Published", summary.publishedCount.toString(), Modifier.weight(1f))
      MetricMiniBox("Scheduled", summary.scheduledCount.toString(), Modifier.weight(1f))
      MetricMiniBox("Views", summary.metrics.formattedViews(), Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(summary.metrics.availabilityNote, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
    if (!summary.isAccountConnected) {
      Spacer(modifier = Modifier.height(10.dp))
      ZuzuSecondaryActionButton(
        text = "Connect ${summary.platform.displayName}",
        onClick = onOpenSocialAccounts,
        accentColor = gradient.first(),
        icon = Icons.Rounded.Link,
      )
    }
  }
}

@Composable
private fun ContentPerformanceCard(
  item: ContentPerformanceItem,
  glowEnabled: Boolean,
  onClick: () -> Unit,
) {
  val statusTint = when (item.status) {
    PublishingHistoryStatus.Published -> ZuzuGreen
    PublishingHistoryStatus.Scheduled -> ZuzuElectricBlue
    PublishingHistoryStatus.Processing -> ZuzuAmber
    PublishingHistoryStatus.Failed -> ZuzuError
    PublishingHistoryStatus.Draft -> ZuzuPurple
    PublishingHistoryStatus.Cancelled -> ZuzuInkMuted
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = ZuzuVioletPinkGradient,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("analytics_content_card_${item.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = buildString {
            append(item.platform?.displayName ?: "Local Studio")
            item.accountHandle?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            append(" • ${item.sourceKindLabel}")
          },
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      StatusPill(label = item.status.displayLabel, tint = statusTint)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      MetricMiniBox("Views", item.metrics.formattedViews(), Modifier.weight(1f))
      MetricMiniBox("Likes", item.metrics.formattedLikes(), Modifier.weight(1f))
      MetricMiniBox("Eng. Rate", item.metrics.formattedEngagementRate(), Modifier.weight(1f))
    }
    if (item.statusDetail.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(item.statusDetail, style = MaterialTheme.typography.labelSmall, color = ZuzuInkMuted)
    }
  }
}

@Composable
private fun ContentPerformanceDetailDialog(
  item: ContentPerformanceItem,
  onDismiss: () -> Unit,
  onOpenPublishingCenter: () -> Unit,
) {
  val previewBitmap = remember(item.mediaFilePath) {
    val path = item.mediaFilePath
    if (path != null && (path.endsWith(".png", true) || path.endsWith(".jpg", true) || path.endsWith(".jpeg", true))) {
      File(path).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
    } else {
      null
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (previewBitmap != null) {
          Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .clip(RoundedCornerShape(12.dp)),
          )
        }
        StatusPill(
          label = "${item.platform?.displayName ?: "Studio Asset"} • ${item.status.displayLabel}",
          tint = ZuzuPurple,
        )
        Text(
          text = "Format: ${item.sourceKindLabel} (${item.aspectRatioLabel})",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
        Text(
          text = "Updated: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.updatedAtMillis))}",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
        item.remotePlatformPostId?.let { remoteId ->
          Text("Remote Post ID: $remoteId", style = MaterialTheme.typography.labelSmall, color = ZuzuElectricBlue)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MetricMiniBox("Views", item.metrics.formattedViews(), Modifier.weight(1f))
          MetricMiniBox("Likes", item.metrics.formattedLikes(), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MetricMiniBox("Comments", item.metrics.formattedComments(), Modifier.weight(1f))
          MetricMiniBox("Shares", item.metrics.formattedShares(), Modifier.weight(1f))
        }
        Text(
          text = item.metrics.availabilityNote,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onOpenPublishingCenter) {
        Text("Publishing Center", fontWeight = FontWeight.Bold, color = ZuzuElectricBlue)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.dialog_close))
      }
    },
  )
}
