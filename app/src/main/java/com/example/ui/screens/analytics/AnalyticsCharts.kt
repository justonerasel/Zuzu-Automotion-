package com.example.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AnalyticsMetricAvailability
import com.example.model.EngagementTrendPoint
import com.example.model.PlatformAnalyticsSummary
import com.example.model.SocialPlatform
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.theme.PlatformFacebookBlue
import com.example.ui.theme.PlatformTikTokCyan
import com.example.ui.theme.PlatformTikTokPink
import com.example.ui.theme.PlatformYouTubeCoral
import com.example.ui.theme.PlatformYouTubeRed
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuTurquoiseBright
import kotlin.math.max

@Composable
fun PublishingTrendsChartCard(
  trends: List<EngagementTrendPoint>,
  glowEnabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val hasActivity = remember(trends) {
    trends.any { it.publishedCount > 0 || it.scheduledCount > 0 || (it.views ?: 0L) > 0L }
  }
  val maxCount = remember(trends) {
    max(1, trends.maxOfOrNull { max(it.publishedCount, it.scheduledCount) } ?: 1)
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuDeepPurple, ZuzuElectricBlue, ZuzuTurquoise),
    modifier = modifier
      .fillMaxWidth()
      .testTag("analytics_trends_chart"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Publishing & Engagement Trends",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = ZuzuInkPrimary,
      )
      StatusPill(
        label = if (hasActivity) "Active Window" else "No Posts Yet",
        tint = if (hasActivity) ZuzuTurquoise else ZuzuInkMuted,
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
      ChartLegendDot(color = ZuzuMagenta, label = "Published")
      ChartLegendDot(color = ZuzuTurquoiseBright, label = "Scheduled")
    }
    Spacer(modifier = Modifier.height(14.dp))

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(138.dp),
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
        val barWidth = (slotWidth * 0.24f).coerceAtLeast(6.dp.toPx())
        val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

        trends.forEachIndexed { index, point ->
          val centerX = slotWidth * index + slotWidth / 2f
          val pubHeight = (point.publishedCount.toFloat() / maxCount.toFloat()) * (size.height - 12.dp.toPx())
          val schedHeight = (point.scheduledCount.toFloat() / maxCount.toFloat()) * (size.height - 12.dp.toPx())

          if (pubHeight > 0f) {
            drawRoundRect(
              brush = Brush.verticalGradient(listOf(ZuzuPurple, ZuzuMagenta, ZuzuPink)),
              topLeft = Offset(centerX - barWidth - 2.dp.toPx(), size.height - pubHeight),
              size = Size(barWidth, pubHeight),
              cornerRadius = radius,
            )
          } else {
            drawRoundRect(
              color = ZuzuPurple.copy(alpha = 0.14f),
              topLeft = Offset(centerX - barWidth - 2.dp.toPx(), size.height - 6.dp.toPx()),
              size = Size(barWidth, 6.dp.toPx()),
              cornerRadius = radius,
            )
          }

          if (schedHeight > 0f) {
            drawRoundRect(
              brush = Brush.verticalGradient(listOf(ZuzuElectricBlue, ZuzuCyan, ZuzuTurquoiseBright)),
              topLeft = Offset(centerX + 2.dp.toPx(), size.height - schedHeight),
              size = Size(barWidth, schedHeight),
              cornerRadius = radius,
            )
          } else {
            drawRoundRect(
              color = ZuzuTurquoise.copy(alpha = 0.14f),
              topLeft = Offset(centerX + 2.dp.toPx(), size.height - 6.dp.toPx()),
              size = Size(barWidth, 6.dp.toPx()),
              cornerRadius = radius,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      trends.forEach { point ->
        Text(
          text = point.label,
          style = MaterialTheme.typography.labelSmall,
          color = ZuzuInkSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun PlatformComparisonChartCard(
  summaries: List<PlatformAnalyticsSummary>,
  glowEnabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val maxTotal = remember(summaries) {
    max(1, summaries.maxOfOrNull { it.publishedCount + it.scheduledCount } ?: 1)
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuElectricBlue, ZuzuMagenta),
    modifier = modifier
      .fillMaxWidth()
      .testTag("analytics_platform_comparison_chart"),
  ) {
    Text(
      text = "Platform Comparison",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold,
      color = ZuzuInkPrimary,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Published & scheduled counts from verified app records; engagement displays live API data only when connected.",
      style = MaterialTheme.typography.bodySmall,
      color = ZuzuInkSecondary,
    )
    Spacer(modifier = Modifier.height(12.dp))

    summaries.forEach { summary ->
      val barGradient = when (summary.platform) {
        SocialPlatform.YouTube -> listOf(PlatformYouTubeRed, PlatformYouTubeCoral)
        SocialPlatform.Facebook -> listOf(PlatformFacebookBlue, ZuzuCyan)
        SocialPlatform.TikTok -> listOf(PlatformTikTokCyan, PlatformTikTokPink)
      }
      val barBaseColor = barGradient.first()
      val totalPosts = summary.publishedCount + summary.scheduledCount
      val ratio = (totalPosts.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f)

      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = summary.platform.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Text(
            text = "${summary.publishedCount} published • ${summary.scheduledCount} scheduled • ${
              if (summary.metrics.availability == AnalyticsMetricAvailability.Available) {
                "${summary.metrics.formattedViews()} views"
              } else {
                summary.metrics.availability.badgeLabel
              }
            }",
            style = MaterialTheme.typography.labelSmall,
            color = ZuzuInkSecondary,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
        ) {
          val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
          drawRoundRect(
            color = barBaseColor.copy(alpha = 0.14f),
            size = size,
            cornerRadius = radius,
          )
          if (ratio > 0f) {
            drawRoundRect(
              brush = Brush.horizontalGradient(barGradient),
              size = Size(width = (size.width * ratio).coerceAtLeast(10.dp.toPx()), height = size.height),
              cornerRadius = radius,
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun ChartLegendDot(
  color: Color,
  label: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(9.dp)
        .clip(CircleShape)
        .background(color),
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = ZuzuInkSecondary,
    )
  }
}
