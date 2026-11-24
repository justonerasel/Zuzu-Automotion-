package com.example.ui.screens.sources

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ContentSourceSection
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuPillChip
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuTurquoise
import com.example.viewmodel.ContentSourceUiState
import com.example.viewmodel.ContentSourceViewModel

@Composable
fun ContentSourcesScreen(
  uiState: ContentSourceUiState,
  viewModel: ContentSourceViewModel,
  glowEnabled: Boolean,
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
        top = 14.dp,
        bottom = bottomContentPadding,
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZuzuHeroBanner(
          badge = "Source Vault • Local & Verified",
          title = stringResource(R.string.sources_title),
          subtitle = stringResource(R.string.sources_subtitle),
          icon = Icons.Rounded.PermMedia,
          gradientColors = ZuzuBlueTurquoiseGradient,
        )
      }

      item {
        SectionPillStrip(
          activeSection = uiState.activeSection,
          onSelectSection = viewModel::selectSection,
        )
      }

      item {
        AnimatedVisibility(visible = !uiState.feedbackMessage.isNullOrBlank()) {
          ZuzuAlertBanner(
            message = uiState.feedbackMessage.orEmpty(),
            tint = ZuzuGreen,
            icon = Icons.Rounded.CheckCircle,
            onDismiss = viewModel::clearMessages,
          )
        }
        AnimatedVisibility(visible = !uiState.errorMessage.isNullOrBlank()) {
          ZuzuAlertBanner(
            message = uiState.errorMessage.orEmpty(),
            tint = ZuzuError,
            icon = Icons.Rounded.ErrorOutline,
            onDismiss = viewModel::clearMessages,
          )
        }
      }

      item {
        AnimatedContent(
          targetState = uiState.activeSection,
          transitionSpec = {
            fadeIn(tween(180)) togetherWith fadeOut(tween(120))
          },
          label = "ContentSourceSectionContent",
        ) { section ->
          when (section) {
            ContentSourceSection.SourceDashboard -> SourceDashboardSection(
              uiState = uiState,
              glowEnabled = glowEnabled,
              onNavigateSection = viewModel::selectSection,
            )
            ContentSourceSection.MyUploads -> MyUploadsSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            ContentSourceSection.UrlImport -> VideoUrlImportSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            ContentSourceSection.CreatorSources -> CreatorSourcesSection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            ContentSourceSection.MediaLibrary -> MediaLibrarySection(
              uiState = uiState,
              viewModel = viewModel,
              glowEnabled = glowEnabled,
            )
            ContentSourceSection.PermissionManager -> PermissionManagerSection(
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
private fun SectionPillStrip(
  activeSection: ContentSourceSection,
  onSelectSection: (ContentSourceSection) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ContentSourceSection.entries.forEach { section ->
      ZuzuPillChip(
        label = section.label,
        selected = activeSection == section,
        onClick = { onSelectSection(section) },
        gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan, ZuzuTurquoise),
        leadingIcon = sectionIcon(section),
        modifier = Modifier.testTag(section.testTag),
      )
    }
  }
}

private fun sectionIcon(section: ContentSourceSection): ImageVector = when (section) {
  ContentSourceSection.SourceDashboard -> Icons.Rounded.Dashboard
  ContentSourceSection.MyUploads -> Icons.Rounded.UploadFile
  ContentSourceSection.UrlImport -> Icons.Rounded.Link
  ContentSourceSection.CreatorSources -> Icons.Rounded.People
  ContentSourceSection.MediaLibrary -> Icons.Rounded.PermMedia
  ContentSourceSection.PermissionManager -> Icons.Rounded.VerifiedUser
}
