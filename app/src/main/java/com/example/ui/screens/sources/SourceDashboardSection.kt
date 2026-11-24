package com.example.ui.screens.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ContentSourceSection
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuCompactStatCard
import com.example.ui.components.ZuzuEmptyStatePanel
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuTurquoiseBright
import com.example.ui.theme.ZuzuViolet
import com.example.viewmodel.ContentSourceUiState

@Composable
fun SourceDashboardSection(
  uiState: ContentSourceUiState,
  glowEnabled: Boolean,
  onNavigateSection: (ContentSourceSection) -> Unit,
) {
  val stats = uiState.stats
  val statItems = listOf(
    SourceStatCardSpec("Total Media Files", stats.totalMediaFiles, "Vault", listOf(ZuzuViolet, ZuzuPurple), Icons.Rounded.PermMedia),
    SourceStatCardSpec("Uploaded Videos", stats.uploadedVideos, "Video", listOf(ZuzuElectricBlue, ZuzuCyan), Icons.Rounded.VideoFile),
    SourceStatCardSpec("Uploaded Images", stats.uploadedImages, "Image", listOf(ZuzuPink, ZuzuMagenta), Icons.Rounded.Image),
    SourceStatCardSpec("Audio Files", stats.audioFiles, "Audio", listOf(ZuzuTurquoise, ZuzuTurquoiseBright), Icons.Rounded.AudioFile),
    SourceStatCardSpec("Saved URLs", stats.savedUrls, "URL", listOf(ZuzuCyan, ZuzuElectricBlue), Icons.Rounded.Link),
    SourceStatCardSpec("Creator Sources", stats.creatorSources, "Creators", listOf(ZuzuPurple, ZuzuPink), Icons.Rounded.People),
    SourceStatCardSpec("Permission Verified", stats.permissionVerified, "Ready", listOf(ZuzuGreen, ZuzuTurquoise), Icons.Rounded.VerifiedUser),
    SourceStatCardSpec("Permission Pending", stats.permissionPending, "Review", listOf(ZuzuAmber, ZuzuPink), Icons.Rounded.HourglassTop),
  )

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionHeader(
      title = "Source Statistics",
      subtitle = "Live counts from your local Zuzu source database",
      gradientColors = ZuzuBlueTurquoiseGradient,
    )

    statItems.chunked(2).forEach { pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        pair.forEach { spec ->
          ZuzuCompactStatCard(
            label = spec.label,
            value = spec.count.toString(),
            icon = spec.icon,
            gradientColors = spec.gradient,
            statusLabel = spec.badge,
            statusTint = spec.gradient.first(),
            glowEnabled = glowEnabled,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    SectionHeader(
      title = "Quick Actions",
      gradientColors = listOf(ZuzuPink, ZuzuMagenta),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      QuickSourceActionRow(
        title = "Upload Local Media",
        subtitle = "Pick MP4, MOV, JPG, PNG, MP3, or WAV from your device",
        icon = Icons.Rounded.UploadFile,
        gradient = listOf(ZuzuViolet, ZuzuElectricBlue),
        glowEnabled = glowEnabled,
        testTag = "source_quick_uploads",
        onClick = { onNavigateSection(ContentSourceSection.MyUploads) },
      )
      QuickSourceActionRow(
        title = "Import Video URL",
        subtitle = "Save authorized YouTube, TikTok, Facebook, or web metadata",
        icon = Icons.Rounded.Link,
        gradient = listOf(ZuzuElectricBlue, ZuzuCyan),
        glowEnabled = glowEnabled,
        testTag = "source_quick_url",
        onClick = { onNavigateSection(ContentSourceSection.UrlImport) },
      )
      QuickSourceActionRow(
        title = "Manage Creator Sources",
        subtitle = "Catalog approved creators and channel references",
        icon = Icons.Rounded.People,
        gradient = listOf(ZuzuPink, ZuzuMagenta),
        glowEnabled = glowEnabled,
        testTag = "source_quick_creators",
        onClick = { onNavigateSection(ContentSourceSection.CreatorSources) },
      )
      QuickSourceActionRow(
        title = "Open Media Library",
        subtitle = "Search, filter, preview, and edit your saved media",
        icon = Icons.Rounded.PermMedia,
        gradient = listOf(ZuzuTurquoise, ZuzuCyan),
        glowEnabled = glowEnabled,
        testTag = "source_quick_library",
        onClick = { onNavigateSection(ContentSourceSection.MediaLibrary) },
      )
      QuickSourceActionRow(
        title = "Review Permissions",
        subtitle = "Verify copyright ownership and approve production reuse",
        icon = Icons.Rounded.VerifiedUser,
        gradient = listOf(ZuzuDeepPurple, ZuzuMagenta),
        glowEnabled = glowEnabled,
        testTag = "source_quick_permissions",
        onClick = { onNavigateSection(ContentSourceSection.PermissionManager) },
      )
    }

    SectionHeader(
      title = "Recent Source Activity",
      gradientColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
    )

    if (uiState.recentActivities.isEmpty()) {
      ZuzuEmptyStatePanel(
        title = "No Source Activity Yet",
        description = "Upload a local file, import a URL, or add a creator source to record activity here.",
        icon = Icons.Rounded.PermMedia,
        gradientColors = ZuzuBlueTurquoiseGradient,
        glowEnabled = glowEnabled,
      )
    } else {
      ZuzuGlassCard(
        glowEnabled = glowEnabled,
        accentColors = listOf(ZuzuTurquoise, ZuzuCyan),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          uiState.recentActivities.take(5).forEach { activity ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = activity.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = ZuzuInkPrimary,
                )
                Text(
                  text = activity.detail,
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkSecondary,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              StatusPill(label = "Saved", tint = ZuzuGreen)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickSourceActionRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  gradient: List<Color>,
  glowEnabled: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradient,
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ZuzuGradientIconBadge(
        icon = icon,
        contentDescription = title,
        gradientColors = gradient,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ZuzuInkPrimary,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
      Surface(
        shape = CircleShape,
        color = gradient.first().copy(alpha = 0.12f),
        modifier = Modifier.size(30.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = gradient.first(),
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

private data class SourceStatCardSpec(
  val label: String,
  val count: Int,
  val badge: String,
  val gradient: List<Color>,
  val icon: ImageVector,
)
