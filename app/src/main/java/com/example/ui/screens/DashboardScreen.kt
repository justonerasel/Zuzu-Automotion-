package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActivityItem
import com.example.model.AnalyticsMetricAvailability
import com.example.model.ConnectionStatus
import com.example.model.ContentPerformanceItem
import com.example.model.EngagementTrendPoint
import com.example.model.PlatformAnalyticsSummary
import com.example.model.PublishingHistoryStatus
import com.example.model.SocialAccountSummary
import com.example.model.SocialPlatform
import com.example.model.StudioModuleSpec
import com.example.model.StudioModuleType
import com.example.ui.components.GlassPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuBrandHeader
import com.example.ui.components.ZuzuCompactStatCard
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSegmentedTabStrip
import com.example.ui.theme.PlatformFacebookBlue
import com.example.ui.theme.PlatformTikTokCyan
import com.example.ui.theme.PlatformTikTokPink
import com.example.ui.theme.PlatformYouTubeCoral
import com.example.ui.theme.PlatformYouTubeRed
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuCyanSoft
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPinkOrangeGradient
import com.example.ui.theme.ZuzuPinkSoft
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuSoftSky
import com.example.ui.theme.ZuzuSuccess
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuTurquoiseBright
import com.example.ui.theme.ZuzuTurquoiseSoft
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.AnalyticsUiState
import com.example.viewmodel.ZuzuUiState
import kotlin.math.max

private enum class DashboardTab(val label: String) {
  Overview("Overview"),
  Platforms("Platforms"),
  Content("Content"),
}

@Composable
fun DashboardScreen(
  uiState: ZuzuUiState,
  onOpenModuleIntro: (StudioModuleType) -> Unit,
  onOpenSocialAccounts: () -> Unit,
  analyticsUiState: AnalyticsUiState = AnalyticsUiState(),
  onOpenContentSources: () -> Unit = {},
  onOpenApprovalCenter: () -> Unit = {},
  onOpenScheduler: () -> Unit = {},
  onOpenPublishingCenter: () -> Unit = {},
  onOpenAnalytics: () -> Unit = {},
  modifier: Modifier = Modifier,
  bottomContentPadding: Dp = 108.dp,
) {
  val glowEnabled = uiState.settings.ambientGlowEnabled
  var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.Overview) }
  var showNotificationsDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(
        start = 18.dp,
        end = 18.dp,
        top = 14.dp,
        bottom = bottomContentPadding,
      ),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // 1. Design 2 Gradient Brand Header with Logo & Notification Icon
      item {
        ZuzuBrandHeader(
          notificationCount = uiState.recentActivity.size + analyticsUiState.totalScheduledPosts,
          onNotificationClick = { showNotificationsDialog = true },
        )
      }

      // 2. Design 2 Growth Summary Hero Banner
      item {
        DashboardGrowthHeroCard(
          uiState = uiState,
          analyticsUiState = analyticsUiState,
          onOpenAnalytics = onOpenAnalytics,
          onOpenVideoFactory = { onOpenModuleIntro(StudioModuleType.AiVideoFactory) },
        )
      }

      // 3. Overview / Platforms / Content Segmented Pill Tabs
      item {
        ZuzuSegmentedTabStrip(
          items = DashboardTab.entries,
          selectedItem = selectedTab,
          labelFor = { it.label },
          onSelect = { selectedTab = it },
          gradientColors = listOf(ZuzuElectricBlue, ZuzuPurple, ZuzuMagenta),
          testTagFor = { "dashboard_tab_${it.name.lowercase()}" },
        )
      }

      when (selectedTab) {
        DashboardTab.Overview -> {
          // 4. 4 Compact Statistics Cards (Total Posts, Total Views, Total Likes, Total Shares)
          item {
            SectionHeader(
              title = stringResource(R.string.dashboard_metrics_title),
              subtitle = analyticsUiState.overallEngagement.availabilityNote,
              gradientColors = ZuzuHeroGradient,
            )
          }

          item {
            DashboardCompactStatsGrid(
              analyticsUiState = analyticsUiState,
              glowEnabled = glowEnabled,
              onOpenAnalytics = onOpenAnalytics,
            )
          }

          // 5. Performance Overview Gradient Chart
          item {
            PerformanceOverviewChartCard(
              trends = analyticsUiState.engagementTrends,
              analyticsUiState = analyticsUiState,
              glowEnabled = glowEnabled,
              onOpenAnalytics = onOpenAnalytics,
            )
          }

          // 6. Top Platforms + Social Accounts Shortcut
          item {
            SectionHeader(
              title = "Top Platforms",
              subtitle = "${uiState.connectedPlatformCount} of 3 channels connected • ${stringResource(R.string.dashboard_connections_summary)}",
              gradientColors = ZuzuBlueTurquoiseGradient,
            )
          }

          item {
            TopPlatformsSection(
              accounts = uiState.socialAccounts,
              platformSummaries = analyticsUiState.platformSummaries,
              glowEnabled = glowEnabled,
              onManageAccounts = onOpenSocialAccounts,
            )
          }

          // 7. Creative Production & Automation Shortcuts
          item {
            SectionHeader(
              title = stringResource(R.string.dashboard_quick_access_title),
              subtitle = stringResource(R.string.studio_subtitle),
              gradientColors = ZuzuVioletPinkGradient,
            )
          }

          item {
            QuickAccessSection(
              modules = uiState.studioModules,
              sourceCount = uiState.sourceStats.totalMediaFiles,
              verifiedSourceCount = uiState.sourceStats.permissionVerified,
              glowEnabled = glowEnabled,
              onOpenModuleIntro = onOpenModuleIntro,
              onOpenContentSources = onOpenContentSources,
              onOpenApprovalCenter = onOpenApprovalCenter,
              onOpenScheduler = onOpenScheduler,
              onOpenPublishingCenter = onOpenPublishingCenter,
              onOpenAnalytics = onOpenAnalytics,
            )
          }

          // 8. Recent Activity
          item {
            SectionHeader(
              title = stringResource(R.string.dashboard_recent_activity_title),
              gradientColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
            )
          }

          item {
            RecentActivityCard(
              activities = uiState.recentActivity,
              glowEnabled = glowEnabled,
            )
          }
        }

        DashboardTab.Platforms -> {
          item {
            SectionHeader(
              title = stringResource(R.string.dashboard_connections_title),
              subtitle = "Real OAuth status, connected handles, and official API telemetry.",
              gradientColors = ZuzuBlueTurquoiseGradient,
            )
          }

          item {
            TopPlatformsSection(
              accounts = uiState.socialAccounts,
              platformSummaries = analyticsUiState.platformSummaries,
              glowEnabled = glowEnabled,
              onManageAccounts = onOpenSocialAccounts,
            )
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              ZuzuSecondaryActionButton(
                text = "Publishing Center",
                onClick = onOpenPublishingCenter,
                accentColor = ZuzuPurple,
                icon = Icons.Rounded.Publish,
                modifier = Modifier.weight(1f),
              )
              ZuzuSecondaryActionButton(
                text = "Full Analytics",
                onClick = onOpenAnalytics,
                accentColor = ZuzuTurquoise,
                icon = Icons.Rounded.Analytics,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        DashboardTab.Content -> {
          item {
            SectionHeader(
              title = "Content & Production",
              subtitle = "Recent videos, motion drafts, publishing items, and creation studios.",
              gradientColors = ZuzuPinkOrangeGradient,
            )
          }

          item {
            QuickAccessSection(
              modules = uiState.studioModules,
              sourceCount = uiState.sourceStats.totalMediaFiles,
              verifiedSourceCount = uiState.sourceStats.permissionVerified,
              glowEnabled = glowEnabled,
              onOpenModuleIntro = onOpenModuleIntro,
              onOpenContentSources = onOpenContentSources,
              onOpenApprovalCenter = onOpenApprovalCenter,
              onOpenScheduler = onOpenScheduler,
              onOpenPublishingCenter = onOpenPublishingCenter,
              onOpenAnalytics = onOpenAnalytics,
            )
          }

          if (analyticsUiState.recentPosts.isEmpty()) {
            item {
              ZuzuEmptyStatePanel(
                title = "No Created Videos or Posts Yet",
                description = "Start in AI Video Factory or Auto Motion Studio to build content for approval and official publishing.",
                icon = Icons.Rounded.MovieCreation,
                gradientColors = ZuzuVioletPinkGradient,
                glowEnabled = glowEnabled,
                ctaLabel = stringResource(R.string.vf_open_factory),
                ctaIcon = Icons.Rounded.AutoAwesome,
                onCtaClick = { onOpenModuleIntro(StudioModuleType.AiVideoFactory) },
              )
            }
          } else {
            items(analyticsUiState.recentPosts.take(6), key = { "dash_content_${it.id}" }) { item ->
              DashboardContentSummaryRow(
                item = item,
                glowEnabled = glowEnabled,
                onClick = onOpenAnalytics,
              )
            }
          }
        }
      }
    }
  }

  if (showNotificationsDialog) {
    DashboardNotificationsDialog(
      uiState = uiState,
      analyticsUiState = analyticsUiState,
      onDismiss = { showNotificationsDialog = false },
      onOpenApprovalCenter = {
        showNotificationsDialog = false
        onOpenApprovalCenter()
      },
      onOpenScheduler = {
        showNotificationsDialog = false
        onOpenScheduler()
      },
    )
  }
}

@Composable
private fun DashboardGrowthHeroCard(
  uiState: ZuzuUiState,
  analyticsUiState: AnalyticsUiState,
  onOpenAnalytics: () -> Unit,
  onOpenVideoFactory: () -> Unit,
) {
  ZuzuHeroBanner(
    badge = "Growth Summary • ${uiState.connectedPlatformCount}/3 Channels",
    title = stringResource(R.string.dashboard_welcome_title),
    subtitle = stringResource(R.string.dashboard_welcome_subtitle),
    icon = Icons.AutoMirrored.Rounded.TrendingUp,
    gradientColors = ZuzuHeroGradient,
    trailingContent = {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = ZuzuWhite.copy(alpha = 0.22f),
        border = BorderStroke(1.5.dp, ZuzuWhite.copy(alpha = 0.48f)),
        shadowElevation = 6.dp,
        modifier = Modifier.size(64.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_hero_creative_ai),
          contentDescription = stringResource(R.string.dashboard_welcome_title),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp)),
        )
      }
    },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      GlassPill(label = "${analyticsUiState.totalVideosCreated} Videos")
      GlassPill(label = "${analyticsUiState.totalScheduledPosts} Scheduled")
      GlassPill(label = analyticsUiState.overallEngagement.availability.badgeLabel)
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = onOpenVideoFactory,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ZuzuWhite,
          contentColor = ZuzuDeepPurple,
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_hero_create_video_btn"),
      ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Create Video", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
      }
      Surface(
        onClick = onOpenAnalytics,
        shape = RoundedCornerShape(14.dp),
        color = ZuzuWhite.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.45f)),
        modifier = Modifier.weight(1f),
      ) {
        Row(
          modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Analytics, contentDescription = null, tint = ZuzuWhite, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Analytics",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ZuzuWhite,
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardCompactStatsGrid(
  analyticsUiState: AnalyticsUiState,
  glowEnabled: Boolean,
  onOpenAnalytics: () -> Unit,
) {
  val engagement = analyticsUiState.overallEngagement
  val totalPosts = analyticsUiState.totalPublishedPosts + analyticsUiState.totalScheduledPosts
  val availabilityBadge = when (engagement.availability) {
    AnalyticsMetricAvailability.Available -> "Live"
    AnalyticsMetricAvailability.NotConnected -> "Not Connected"
    AnalyticsMetricAvailability.SetupRequired -> "Setup"
    AnalyticsMetricAvailability.DataUnavailable -> "Pending"
  }
  val availabilityTint = when (engagement.availability) {
    AnalyticsMetricAvailability.Available -> ZuzuGreen
    AnalyticsMetricAvailability.NotConnected -> ZuzuInkMuted
    AnalyticsMetricAvailability.SetupRequired -> ZuzuAmber
    AnalyticsMetricAvailability.DataUnavailable -> ZuzuElectricBlue
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ZuzuCompactStatCard(
        label = "Total Posts",
        value = totalPosts.toString(),
        icon = Icons.Rounded.PostAdd,
        gradientColors = listOf(ZuzuDeepPurple, ZuzuPurple),
        statusLabel = "${analyticsUiState.totalPublishedPosts} live",
        statusTint = ZuzuPurple,
        glowEnabled = glowEnabled,
        onClick = onOpenAnalytics,
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_stat_total_posts"),
      )
      ZuzuCompactStatCard(
        label = "Total Views",
        value = engagement.formattedViews(),
        icon = Icons.Rounded.Visibility,
        gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
        statusLabel = availabilityBadge,
        statusTint = availabilityTint,
        glowEnabled = glowEnabled,
        onClick = onOpenAnalytics,
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_stat_total_views"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ZuzuCompactStatCard(
        label = "Total Likes",
        value = engagement.formattedLikes(),
        icon = Icons.Rounded.Favorite,
        gradientColors = listOf(ZuzuMagenta, ZuzuPink),
        statusLabel = availabilityBadge,
        statusTint = availabilityTint,
        glowEnabled = glowEnabled,
        onClick = onOpenAnalytics,
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_stat_total_likes"),
      )
      ZuzuCompactStatCard(
        label = "Total Shares",
        value = engagement.formattedShares(),
        icon = Icons.Rounded.Share,
        gradientColors = listOf(ZuzuCyan, ZuzuTurquoise),
        statusLabel = availabilityBadge,
        statusTint = availabilityTint,
        glowEnabled = glowEnabled,
        onClick = onOpenAnalytics,
        modifier = Modifier
          .weight(1f)
          .testTag("dashboard_stat_total_shares"),
      )
    }
  }
}

@Composable
private fun PerformanceOverviewChartCard(
  trends: List<EngagementTrendPoint>,
  analyticsUiState: AnalyticsUiState,
  glowEnabled: Boolean,
  onOpenAnalytics: () -> Unit,
) {
  val hasActivity = remember(trends) {
    trends.any { it.publishedCount > 0 || it.scheduledCount > 0 || (it.views ?: 0L) > 0L }
  }
  val maxCount = remember(trends) {
    max(1, trends.maxOfOrNull { max(it.publishedCount + it.scheduledCount, 1) } ?: 1)
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuElectricBlue, ZuzuMagenta, ZuzuTurquoise),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_performance_overview_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Performance Overview",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = ZuzuInkPrimary,
        )
        Text(
          text = if (hasActivity) {
            "Published & scheduled output across recent periods"
          } else {
            "Real chart activates automatically as posts publish or sync"
          },
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
      StatusPill(
        label = analyticsUiState.overallEngagement.availability.badgeLabel,
        tint = if (hasActivity) ZuzuTurquoise else ZuzuElectricBlue,
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp),
    ) {
      val gridColor = ZuzuBorderLight.copy(alpha = 0.72f)
      for (line in 0..3) {
        val y = size.height * (line / 3f)
        drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.dp.toPx(),
        )
      }

      if (trends.isNotEmpty()) {
        val slotWidth = size.width / trends.size.toFloat()
        val barWidth = (slotWidth * 0.28f).coerceAtLeast(8.dp.toPx())
        val radius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
        val linePoints = mutableListOf<Offset>()

        trends.forEachIndexed { index, point ->
          val centerX = slotWidth * index + slotWidth / 2f
          val combined = point.publishedCount + point.scheduledCount
          val ratio = (combined.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
          val barHeight = if (combined > 0) {
            (ratio * (size.height - 16.dp.toPx())).coerceAtLeast(10.dp.toPx())
          } else {
            8.dp.toPx()
          }

          drawRoundRect(
            brush = if (combined > 0) {
              Brush.verticalGradient(listOf(ZuzuElectricBlue, ZuzuPurple, ZuzuPink))
            } else {
              Brush.verticalGradient(listOf(ZuzuPurple.copy(alpha = 0.16f), ZuzuTurquoise.copy(alpha = 0.14f)))
            },
            topLeft = Offset(centerX - barWidth / 2f, size.height - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = radius,
          )
          linePoints += Offset(centerX, (size.height - barHeight).coerceAtLeast(6.dp.toPx()))
        }

        if (linePoints.size >= 2) {
          val path = Path().apply {
            moveTo(linePoints.first().x, linePoints.first().y)
            linePoints.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
          }
          drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(ZuzuTurquoiseBright, ZuzuElectricBlue, ZuzuMagenta)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
          )
          linePoints.forEach { pt ->
            drawCircle(color = ZuzuWhite, radius = 4.5.dp.toPx(), center = pt)
            drawCircle(color = ZuzuMagenta, radius = 2.8.dp.toPx(), center = pt)
          }
        }
      }
    }

    if (trends.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        trends.forEach { point ->
          Text(
            text = point.label,
            style = MaterialTheme.typography.labelSmall,
            color = ZuzuInkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    ZuzuSecondaryActionButton(
      text = stringResource(R.string.analytics_open_cta),
      onClick = onOpenAnalytics,
      accentColor = ZuzuElectricBlue,
      icon = Icons.Rounded.Analytics,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun TopPlatformsSection(
  accounts: List<SocialAccountSummary>,
  platformSummaries: List<PlatformAnalyticsSummary>,
  glowEnabled: Boolean,
  onManageAccounts: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    ZuzuGlassCard(
      glowEnabled = glowEnabled,
      accentColors = ZuzuBlueTurquoiseGradient,
      onClick = onManageAccounts,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("dashboard_social_accounts_shortcut"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.social_accounts_shortcut_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.social_accounts_shortcut_desc),
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = CircleShape,
          color = ZuzuTurquoiseSoft,
          border = BorderStroke(1.dp, ZuzuTurquoise.copy(alpha = 0.35f)),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          ) {
            Text(
              text = stringResource(R.string.social_accounts_manage_cta),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = ZuzuTurquoise,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = null,
              tint = ZuzuTurquoise,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    accounts.forEach { account ->
      val platformSummary = platformSummaries.firstOrNull { it.platform.id == account.id }
      val (bgTint, gradientColors, icon) = when (account.id) {
        "youtube" -> Triple(
          Color(0xFFFFF1F3),
          listOf(PlatformYouTubeRed, PlatformYouTubeCoral),
          Icons.Rounded.PlayCircle,
        )
        "facebook" -> Triple(
          ZuzuSoftSky,
          listOf(PlatformFacebookBlue, ZuzuCyan),
          Icons.Rounded.Public,
        )
        else -> Triple(
          ZuzuCyanSoft,
          listOf(PlatformTikTokCyan, PlatformTikTokPink),
          Icons.Rounded.MusicNote,
        )
      }
      val (statusText, statusColor) = when (account.status) {
        ConnectionStatus.NotConnected -> stringResource(R.string.status_not_connected) to ZuzuInkMuted
        ConnectionStatus.Connected -> stringResource(R.string.status_connected) to ZuzuSuccess
        ConnectionStatus.Error -> stringResource(R.string.status_error) to ZuzuError
      }

      Surface(
        shape = RoundedCornerShape(22.dp),
        color = ZuzuWhite,
        shadowElevation = if (glowEnabled) 6.dp else 2.dp,
        border = BorderStroke(
          1.dp,
          Brush.horizontalGradient(
            listOf(gradientColors.first().copy(alpha = 0.32f), gradientColors.last().copy(alpha = 0.22f)),
          ),
        ),
        onClick = onManageAccounts,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(bgTint.copy(alpha = 0.72f), ZuzuWhite)))
            .padding(horizontal = 14.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            ZuzuGradientIconBadge(
              icon = icon,
              contentDescription = account.platformName,
              gradientColors = gradientColors,
              size = 44.dp,
              iconSize = 23.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = account.platformName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ZuzuInkPrimary,
              )
              val subText = if (platformSummary != null && platformSummary.metrics.hasLiveNumbers) {
                "${account.handlePlaceholder} • ${platformSummary.metrics.formattedViews()} views"
              } else {
                account.handlePlaceholder
              }
              Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuInkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          StatusPill(label = statusText, tint = statusColor)
        }
      }
    }
  }
}

@Composable
private fun QuickAccessSection(
  modules: List<StudioModuleSpec>,
  sourceCount: Int,
  verifiedSourceCount: Int,
  glowEnabled: Boolean,
  onOpenModuleIntro: (StudioModuleType) -> Unit,
  onOpenContentSources: () -> Unit,
  onOpenApprovalCenter: () -> Unit,
  onOpenScheduler: () -> Unit,
  onOpenPublishingCenter: () -> Unit,
  onOpenAnalytics: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    DashboardUtilityShortcutCard(
      title = stringResource(R.string.sources_shortcut_title),
      subtitle = "${stringResource(R.string.sources_shortcut_desc)} • $sourceCount saved ($verifiedSourceCount verified)",
      gradientColors = listOf(ZuzuTurquoise, ZuzuCyan),
      tintBg = ZuzuTurquoiseSoft,
      icon = Icons.Rounded.PermMedia,
      tag = "dashboard_content_sources_shortcut",
      glowEnabled = glowEnabled,
      onClick = onOpenContentSources,
    )

    modules.forEach { module ->
      val isVideoFactory = module.type == StudioModuleType.AiVideoFactory
      val (gradientColors, tintBg, icon) = if (isVideoFactory) {
        Triple(listOf(ZuzuDeepPurple, ZuzuElectricBlue), ZuzuPurpleSoft, Icons.Rounded.MovieCreation)
      } else {
        Triple(listOf(ZuzuMagenta, ZuzuPink), ZuzuPinkSoft, Icons.Rounded.AutoAwesome)
      }
      val tag = if (isVideoFactory) {
        "quick_access_video_factory"
      } else {
        "quick_access_motion_studio"
      }

      DashboardUtilityShortcutCard(
        title = module.title,
        subtitle = module.shortDescription,
        gradientColors = gradientColors,
        tintBg = tintBg,
        icon = icon,
        tag = tag,
        glowEnabled = glowEnabled,
        onClick = { onOpenModuleIntro(module.type) },
      )
    }

    DashboardUtilityShortcutCard(
      title = stringResource(R.string.approval_center_title),
      subtitle = stringResource(R.string.approval_center_subtitle),
      gradientColors = listOf(ZuzuViolet, ZuzuPurple),
      tintBg = ZuzuPurpleSoft,
      icon = Icons.Rounded.Approval,
      tag = "dashboard_approval_center_shortcut",
      glowEnabled = glowEnabled,
      onClick = onOpenApprovalCenter,
    )

    DashboardUtilityShortcutCard(
      title = stringResource(R.string.scheduler_title),
      subtitle = stringResource(R.string.scheduler_subtitle),
      gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
      tintBg = ZuzuCyanSoft,
      icon = Icons.Rounded.Schedule,
      tag = "dashboard_scheduler_shortcut",
      glowEnabled = glowEnabled,
      onClick = onOpenScheduler,
    )

    DashboardUtilityShortcutCard(
      title = stringResource(R.string.publishing_center_title),
      subtitle = stringResource(R.string.publishing_center_subtitle),
      gradientColors = listOf(ZuzuPink, ZuzuPurple),
      tintBg = ZuzuPinkSoft,
      icon = Icons.Rounded.Publish,
      tag = "dashboard_publishing_center_shortcut",
      glowEnabled = glowEnabled,
      onClick = onOpenPublishingCenter,
    )

    DashboardUtilityShortcutCard(
      title = stringResource(R.string.analytics_title),
      subtitle = stringResource(R.string.analytics_subtitle),
      gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
      tintBg = ZuzuTurquoiseSoft,
      icon = Icons.Rounded.Analytics,
      tag = "dashboard_analytics_shortcut",
      glowEnabled = glowEnabled,
      onClick = onOpenAnalytics,
    )
  }
}

@Composable
private fun DashboardUtilityShortcutCard(
  title: String,
  subtitle: String,
  gradientColors: List<Color>,
  tintBg: Color,
  icon: ImageVector,
  tag: String,
  glowEnabled: Boolean,
  onClick: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradientColors,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(tag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ZuzuGradientIconBadge(
        icon = icon,
        contentDescription = title,
        gradientColors = gradientColors,
        size = 50.dp,
        iconSize = 25.dp,
        cornerRadius = 16.dp,
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Surface(shape = CircleShape, color = tintBg, modifier = Modifier.size(34.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = title,
            tint = gradientColors.first(),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardContentSummaryRow(
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
    accentColors = listOf(ZuzuPurple, ZuzuPink),
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
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
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${item.platform?.displayName ?: "Studio"} • ${item.sourceKindLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
      StatusPill(label = item.status.displayLabel, tint = statusTint)
    }
  }
}

@Composable
private fun RecentActivityCard(
  activities: List<ActivityItem>,
  glowEnabled: Boolean,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
    modifier = Modifier.fillMaxWidth(),
  ) {
    activities.forEachIndexed { index, activity ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(ZuzuTurquoise, ZuzuElectricBlue))),
          ) {
            Icon(
              imageVector = Icons.Rounded.RocketLaunch,
              contentDescription = null,
              tint = ZuzuWhite,
              modifier = Modifier.size(19.dp),
            )
          }
          if (index < activities.lastIndex) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(Brush.verticalGradient(listOf(ZuzuTurquoise, ZuzuMagenta))),
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = activity.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ZuzuInkPrimary,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(label = activity.timeLabel, tint = ZuzuTurquoise)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = activity.description,
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }
      if (index < activities.lastIndex) {
        Spacer(modifier = Modifier.height(14.dp))
      }
    }
  }
}

@Composable
private fun DashboardNotificationsDialog(
  uiState: ZuzuUiState,
  analyticsUiState: AnalyticsUiState,
  onDismiss: () -> Unit,
  onOpenApprovalCenter: () -> Unit,
  onOpenScheduler: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ZuzuGradientIconBadge(
          icon = Icons.Rounded.Notifications,
          contentDescription = null,
          gradientColors = ZuzuHeroGradient,
          size = 36.dp,
          iconSize = 18.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Workspace Alerts",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusPill(
          label = "${uiState.connectedPlatformCount}/3 Channels Connected • ${analyticsUiState.totalScheduledPosts} Scheduled",
          tint = ZuzuElectricBlue,
        )
        uiState.recentActivity.take(4).forEach { act ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ZuzuSoftSky,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(act.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ZuzuInkPrimary)
              Text(act.description, style = MaterialTheme.typography.bodySmall, color = ZuzuInkSecondary)
            }
          }
        }
        Text(
          text = "Workflow Alerts: ${if (uiState.settings.workflowAlertsEnabled) "Enabled" else "Paused"} • Schedule Reminders: ${if (uiState.settings.scheduleRemindersEnabled) "Enabled" else "Paused"}",
          style = MaterialTheme.typography.labelSmall,
          color = ZuzuInkMuted,
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onOpenApprovalCenter) {
        Text("Approval Center", fontWeight = FontWeight.Bold, color = ZuzuPurple)
      }
    },
    dismissButton = {
      Row {
        TextButton(onClick = onOpenScheduler) {
          Text("Scheduler", color = ZuzuElectricBlue)
        }
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.dialog_close))
        }
      }
    },
  )
}
