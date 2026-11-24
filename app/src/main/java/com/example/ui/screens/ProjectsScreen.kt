package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Approval
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ProjectItem
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.viewmodel.ZuzuUiState

@Composable
fun ProjectsScreen(
  uiState: ZuzuUiState,
  onExploreStudio: () -> Unit,
  onOpenProject: (ProjectItem) -> Unit = {},
  onOpenMotionStudio: () -> Unit = {},
  onOpenApprovalCenter: () -> Unit = {},
  onOpenScheduler: () -> Unit = {},
  onOpenPublishingCenter: () -> Unit = {},
  onOpenAnalytics: () -> Unit = {},
  modifier: Modifier = Modifier,
  bottomContentPadding: Dp = 108.dp,
) {
  val glowEnabled = uiState.settings.ambientGlowEnabled

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
          badge = "${uiState.projects.size} Active Projects",
          title = stringResource(R.string.projects_title),
          subtitle = stringResource(R.string.projects_subtitle),
          icon = Icons.Rounded.FolderCopy,
          gradientColors = ZuzuVioletPinkGradient,
        )
      }

      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuPink, ZuzuElectricBlue),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Production & Automation Pipelines",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = ZuzuInkPrimary,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ZuzuSecondaryActionButton(
              text = "Motion",
              onClick = onOpenMotionStudio,
              accentColor = ZuzuPink,
              icon = Icons.Rounded.Animation,
              modifier = Modifier
                .weight(1f)
                .testTag("projects_open_motion_btn"),
            )
            ZuzuSecondaryActionButton(
              text = "Approval",
              onClick = onOpenApprovalCenter,
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.Approval,
              modifier = Modifier
                .weight(1f)
                .testTag("projects_open_approval_btn"),
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ZuzuSecondaryActionButton(
              text = "Schedule",
              onClick = onOpenScheduler,
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.Schedule,
              modifier = Modifier
                .weight(1f)
                .testTag("projects_open_scheduler_btn"),
            )
            ZuzuSecondaryActionButton(
              text = "Publish",
              onClick = onOpenPublishingCenter,
              accentColor = ZuzuMagenta,
              icon = Icons.Rounded.Publish,
              modifier = Modifier
                .weight(1f)
                .testTag("projects_open_publishing_btn"),
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          ZuzuGradientButton(
            text = "Open Analytics Dashboard",
            onClick = onOpenAnalytics,
            gradientColors = ZuzuBlueTurquoiseGradient,
            icon = Icons.Rounded.Analytics,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("projects_open_analytics_btn"),
          )
        }
      }

      if (uiState.projects.isEmpty()) {
        item {
          ProjectsEmptyStateCard(
            glowEnabled = glowEnabled,
            onExploreStudio = onExploreStudio,
          )
        }
      } else {
        items(uiState.projects, key = { it.id }) { project ->
          ProjectRowCard(
            project = project,
            glowEnabled = glowEnabled,
            onClick = { onOpenProject(project) },
          )
        }
      }
    }
  }
}

@Composable
private fun ProjectsEmptyStateCard(
  glowEnabled: Boolean,
  onExploreStudio: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuPink, ZuzuPurple, ZuzuTurquoise),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("projects_empty_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
          1.5.dp,
          androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(ZuzuPurple.copy(alpha = 0.25f), ZuzuCyan.copy(alpha = 0.25f)),
          ),
        ),
        shadowElevation = 6.dp,
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_empty_projects),
          contentDescription = stringResource(R.string.projects_empty_title),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = stringResource(R.string.projects_empty_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = ZuzuInkPrimary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.projects_empty_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = ZuzuInkSecondary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(20.dp))

      ZuzuGradientButton(
        text = stringResource(R.string.projects_empty_cta),
        onClick = onExploreStudio,
        gradientColors = ZuzuHeroGradient,
        icon = Icons.Rounded.AutoAwesome,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("projects_explore_studio_button"),
      )
    }
  }
}

@Composable
private fun ProjectRowCard(
  project: ProjectItem,
  glowEnabled: Boolean,
  onClick: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = listOf(ZuzuPink, ZuzuPurple),
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = project.title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = ZuzuInkPrimary,
    )
    Spacer(modifier = Modifier.height(6.dp))
    StatusPill(label = "${project.typeLabel} • ${project.statusLabel}", tint = ZuzuTurquoise)
  }
}
