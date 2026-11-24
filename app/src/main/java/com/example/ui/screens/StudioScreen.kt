package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.StudioModuleSpec
import com.example.model.StudioModuleType
import com.example.ui.components.GlassPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.ZuzuGradientCard
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPinkOrangeGradient
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ZuzuUiState

@Composable
fun StudioScreen(
  uiState: ZuzuUiState,
  onOpenModuleIntro: (StudioModuleType) -> Unit,
  onOpenApprovalCenter: () -> Unit = {},
  onOpenScheduler: () -> Unit = {},
  onOpenCloudStatus: () -> Unit = {},
  onOpenPublishingCenter: () -> Unit = {},
  onOpenAnalytics: () -> Unit = {},
  modifier: Modifier = Modifier,
  bottomContentPadding: Dp = 108.dp,
) {
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
        top = 16.dp,
        bottom = bottomContentPadding,
      ),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      item {
        ZuzuHeroBanner(
          badge = "Zuzu Creator Studio",
          title = stringResource(R.string.studio_title),
          subtitle = stringResource(R.string.studio_subtitle),
          icon = Icons.Rounded.AutoAwesome,
          gradientColors = ZuzuHeroGradient,
        )
      }

      item {
        SectionHeader(
          title = "AI Production Suites",
          subtitle = "Create long-form or vertical reels, voiceovers, motion posts, and MP4 exports.",
          gradientColors = ZuzuVioletPinkGradient,
        )
      }

      items(uiState.studioModules, key = { it.type.name }) { module ->
        StudioModuleFeatureCard(
          module = module,
          onOpenModuleIntro = { onOpenModuleIntro(module.type) },
        )
      }

      item {
        SectionHeader(
          title = "Approval, Publishing & Analytics",
          subtitle = "Verify permissions, approve content, schedule cloud tasks, and track real telemetry.",
          gradientColors = ZuzuBlueTurquoiseGradient,
        )
      }

      item {
        StudioWorkflowDestinationCard(
          title = stringResource(R.string.approval_center_title),
          description = stringResource(R.string.approval_center_subtitle),
          badge = "Human Gate",
          ctaLabel = "Open Approval Center",
          gradientColors = listOf(ZuzuDeepPurple, ZuzuElectricBlue),
          icon = Icons.Rounded.Approval,
          cardTag = "studio_card_approval_center",
          onClick = onOpenApprovalCenter,
        )
      }

      item {
        StudioWorkflowDestinationCard(
          title = stringResource(R.string.scheduler_title),
          description = stringResource(R.string.scheduler_subtitle),
          badge = "Cloud Tasks",
          ctaLabel = "Open Scheduler",
          gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan, ZuzuTurquoise),
          icon = Icons.Rounded.Schedule,
          cardTag = "studio_card_scheduler",
          onClick = onOpenScheduler,
        )
      }

      item {
        StudioWorkflowDestinationCard(
          title = stringResource(R.string.cloud_status_title),
          description = stringResource(R.string.cloud_status_subtitle),
          badge = "Cloud Health",
          ctaLabel = "Check Cloud Status",
          gradientColors = ZuzuVioletPinkGradient,
          icon = Icons.Rounded.CloudQueue,
          cardTag = "studio_card_cloud_status",
          onClick = onOpenCloudStatus,
        )
      }

      item {
        StudioWorkflowDestinationCard(
          title = stringResource(R.string.publishing_center_title),
          description = stringResource(R.string.publishing_center_subtitle),
          badge = stringResource(R.string.publishing_badge_label),
          ctaLabel = stringResource(R.string.publishing_open_cta),
          gradientColors = listOf(ZuzuViolet, ZuzuMagenta, ZuzuPink),
          icon = Icons.Rounded.Publish,
          cardTag = "studio_card_publishing_center",
          onClick = onOpenPublishingCenter,
        )
      }

      item {
        StudioWorkflowDestinationCard(
          title = stringResource(R.string.analytics_title),
          description = stringResource(R.string.analytics_subtitle),
          badge = stringResource(R.string.analytics_badge_label),
          ctaLabel = stringResource(R.string.analytics_open_cta),
          gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan, ZuzuTurquoise),
          icon = Icons.Rounded.Analytics,
          cardTag = "studio_card_analytics",
          onClick = onOpenAnalytics,
        )
      }
    }
  }
}

@Composable
private fun StudioModuleFeatureCard(
  module: StudioModuleSpec,
  onOpenModuleIntro: () -> Unit,
) {
  val isVideo = module.type == StudioModuleType.AiVideoFactory
  val gradientColors = if (isVideo) {
    listOf(ZuzuDeepPurple, ZuzuPurple, ZuzuElectricBlue)
  } else {
    ZuzuPinkOrangeGradient
  }
  val ctaTextColor = if (isVideo) ZuzuDeepPurple else ZuzuMagenta
  val icon = if (isVideo) Icons.Rounded.MovieCreation else Icons.Rounded.Animation
  val cardTag = if (isVideo) "studio_card_video_factory" else "studio_card_motion_studio"
  val buttonTag = if (isVideo) "studio_btn_video_factory" else "studio_btn_motion_studio"

  ZuzuGradientCard(
    gradientColors = gradientColors,
    onClick = onOpenModuleIntro,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(cardTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = ZuzuWhite.copy(alpha = 0.22f),
          border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.44f)),
          modifier = Modifier.size(56.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = module.title,
              tint = ZuzuWhite,
              modifier = Modifier.size(29.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(14.dp))
        StudioVisualMotif(
          isVideo = isVideo,
          modifier = Modifier.size(50.dp),
        )
      }

      GlassPill(
        label = if (isVideo) stringResource(R.string.vf_badge_active) else stringResource(R.string.motion_studio_badge_active),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = module.title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.ExtraBold,
      color = ZuzuWhite,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = module.overviewDescription,
      style = MaterialTheme.typography.bodyMedium,
      color = ZuzuWhite.copy(alpha = 0.92f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      GlassPill(label = if (isVideo) "Reels & Shorts" else "9 Templates • 8 Styles")
      GlassPill(label = if (isVideo) "Script • Voice • Visuals" else "MP4 • PNG • JPG")
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
      onClick = onOpenModuleIntro,
      modifier = Modifier
        .fillMaxWidth()
        .testTag(buttonTag),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = ZuzuWhite,
        contentColor = ctaTextColor,
      ),
      contentPadding = PaddingValues(vertical = 13.dp),
    ) {
      Text(
        text = if (isVideo) stringResource(R.string.vf_open_factory) else stringResource(R.string.motion_studio_open_cta),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun StudioVisualMotif(
  isVideo: Boolean,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val lineColor = ZuzuWhite.copy(alpha = 0.58f)
    if (isVideo) {
      val barWidth = 4.5.dp.toPx()
      val ratios = listOf(0.45f, 0.85f, 0.60f, 0.95f)
      ratios.forEachIndexed { idx, ratio ->
        val x = idx * (size.width / 4.0f) + 5.dp.toPx()
        val barHeight = size.height * ratio
        val top = (size.height - barHeight) / 2f
        drawLine(
          color = lineColor,
          start = Offset(x, top),
          end = Offset(x, top + barHeight),
          strokeWidth = barWidth,
          cap = StrokeCap.Round,
        )
      }
    } else {
      val center = Offset(size.width / 2f, size.height / 2f)
      drawCircle(color = lineColor, radius = size.minDimension * 0.44f, center = center, style = Stroke(2.5.dp.toPx()))
      drawCircle(color = ZuzuWhite.copy(alpha = 0.35f), radius = size.minDimension * 0.27f, center = center, style = Stroke(2.dp.toPx()))
      drawCircle(color = ZuzuCyan.copy(alpha = 0.85f), radius = 4.dp.toPx(), center = Offset(center.x + size.minDimension * 0.32f, center.y - size.minDimension * 0.25f))
    }
  }
}

@Composable
private fun StudioWorkflowDestinationCard(
  title: String,
  description: String,
  badge: String,
  ctaLabel: String,
  gradientColors: List<Color>,
  icon: ImageVector,
  cardTag: String,
  onClick: () -> Unit,
) {
  ZuzuGradientCard(
    gradientColors = gradientColors,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(cardTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = ZuzuWhite.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.38f)),
        modifier = Modifier.size(46.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = title, tint = ZuzuWhite, modifier = Modifier.size(24.dp))
        }
      }
      GlassPill(label = badge)
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ZuzuWhite)
    Spacer(modifier = Modifier.height(4.dp))
    Text(description, style = MaterialTheme.typography.bodySmall, color = ZuzuWhite.copy(alpha = 0.92f))
    Spacer(modifier = Modifier.height(14.dp))
    Button(
      onClick = onClick,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = ZuzuWhite,
        contentColor = gradientColors.first(),
      ),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(ctaLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
      Spacer(modifier = Modifier.width(6.dp))
      Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
    }
  }
}
