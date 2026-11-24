package com.example.ui.screens.motion

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.source.MediaFileHelper
import com.example.model.MediaLibraryEntry
import com.example.model.MediaType
import com.example.model.MotionAnimationStyle
import com.example.model.MotionPlatformPreset
import com.example.model.MotionTemplateCategory
import com.example.model.MotionTransition
import com.example.model.VideoAspectRatio
import com.example.ui.components.SectionHeader
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuPillChip
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPinkOrangeGradient
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.MotionStudioUiState
import com.example.viewmodel.MotionStudioViewModel
import kotlin.math.roundToInt

@Composable
fun MotionStudioScreen(
  uiState: MotionStudioUiState,
  mediaLibraryEntries: List<MediaLibraryEntry>,
  viewModel: MotionStudioViewModel,
  glowEnabled: Boolean,
  onOpenApprovalCenter: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val project = uiState.activeProject

  var showLibraryDialog by remember { mutableStateOf(false) }
  var showStickerDialog by remember { mutableStateOf(false) }
  var showSavedProjectsDialog by remember { mutableStateOf(false) }

  val mediaPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { meta -> viewModel.addMediaFromUpload(meta) }
        .onFailure { err -> viewModel.setError(err.message ?: "Unable to read selected photo/video file.") }
    }
  }

  val logoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      viewModel.setLogoBadge(project.logoLabel, uri.toString())
    }
  }

  val musicPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      MediaFileHelper.extractPendingUpload(context, uri)
        .onSuccess { meta ->
          val title = meta.fileName.substringBeforeLast('.')
          viewModel.setBackgroundMusic(title, uri.toString())
        }
        .onFailure {
          viewModel.setBackgroundMusic("Music Track", uri.toString())
        }
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZuzuSecondaryTopBar(
          title = stringResource(R.string.motion_studio_title),
          onBack = onBack,
          backButtonTag = "motion_back_button",
          badgeText = stringResource(R.string.motion_studio_badge_active),
          badgeTint = ZuzuPink,
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "9 Templates • 8 Motion Styles • MP4 / PNG / JPG",
          title = project.title.ifBlank { "Auto Motion Studio" },
          subtitle = stringResource(R.string.motion_studio_subtitle),
          icon = Icons.Rounded.Animation,
          gradientColors = ZuzuVioletPinkGradient,
          bottomContent = if (uiState.savedProjects.isNotEmpty()) {
            {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                  onClick = { showSavedProjectsDialog = true },
                  shape = RoundedCornerShape(14.dp),
                  color = ZuzuWhite.copy(alpha = 0.22f),
                  border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.55f)),
                ) {
                  Text(
                    text = "Saved Projects (${uiState.savedProjects.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ZuzuWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  )
                }
                Surface(
                  onClick = { viewModel.createNewProject(project.templateCategory) },
                  shape = RoundedCornerShape(14.dp),
                  color = ZuzuWhite.copy(alpha = 0.22f),
                  border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.55f)),
                ) {
                  Text(
                    text = "New Post",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ZuzuWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  )
                }
              }
            }
          } else {
            null
          },
        )
      }

      if (!uiState.feedbackMessage.isNullOrBlank() || !uiState.errorMessage.isNullOrBlank()) {
        item {
          val isErr = !uiState.errorMessage.isNullOrBlank()
          val msg = uiState.errorMessage ?: uiState.feedbackMessage.orEmpty()
          ZuzuAlertBanner(
            message = msg,
            tint = if (isErr) ZuzuError else ZuzuTurquoise,
            icon = if (isErr) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
            onDismiss = viewModel::clearMessages,
          )
        }
      }

      // Live Motion Preview Card
      item {
        MotionPreviewPlayerCard(
          project = project,
          uiState = uiState,
          viewModel = viewModel,
          glowEnabled = glowEnabled,
        )
      }

      // 1. Motion Templates (9 templates)
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuPink, ZuzuPurple),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SectionHeader(
            title = "1. Motion Templates",
            subtitle = project.templateCategory.tagline,
            gradientColors = ZuzuVioletPinkGradient,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MotionTemplateCategory.entries.forEach { category ->
              ZuzuPillChip(
                label = category.displayLabel,
                selected = project.templateCategory == category,
                onClick = { viewModel.applyTemplate(category) },
                gradientColors = ZuzuVioletPinkGradient,
                modifier = Modifier.testTag("motion_template_${category.name}"),
              )
            }
          }
        }
      }

      // 2. Social Platform Presets & Format (9:16, 16:9, 1:1)
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuElectricBlue, ZuzuCyan),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SectionHeader(
            title = "2. Social Formats & Platform Presets",
            subtitle = project.platformPreset.safeZoneHint,
            gradientColors = ZuzuBlueTurquoiseGradient,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MotionPlatformPreset.entries.forEach { preset ->
              ZuzuPillChip(
                label = preset.displayLabel,
                selected = project.platformPreset == preset,
                onClick = { viewModel.selectPlatformPreset(preset) },
                gradientColors = ZuzuBlueTurquoiseGradient,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            VideoAspectRatio.entries.forEach { ratio ->
              ZuzuPillChip(
                label = ratio.label,
                selected = project.aspectRatio == ratio,
                onClick = { viewModel.setAspectRatio(ratio) },
                gradientColors = listOf(ZuzuElectricBlue, ZuzuPurple),
              )
            }
          }
        }
      }

      // 3. Motion Styles (all 8), Transitions & Duration
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuOrange, ZuzuPink),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SectionHeader(
            title = "3. Animation Styles & Timing",
            subtitle = "${project.animationStyle.displayLabel} • ${project.transition.displayLabel} • ${project.durationSeconds}s",
            gradientColors = ZuzuPinkOrangeGradient,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MotionAnimationStyle.entries.forEach { style ->
              ZuzuPillChip(
                label = style.displayLabel,
                selected = project.animationStyle == style,
                onClick = { viewModel.setAnimationStyle(style) },
                gradientColors = ZuzuPinkOrangeGradient,
                modifier = Modifier.testTag("motion_style_${style.name}"),
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MotionTransition.entries.forEach { tr ->
              ZuzuPillChip(
                label = tr.displayLabel,
                selected = project.transition == tr,
                onClick = { viewModel.setTransition(tr) },
                gradientColors = listOf(ZuzuPurple, ZuzuElectricBlue),
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Duration: ${project.durationSeconds} seconds",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Slider(
            value = project.durationSeconds.toFloat(),
            onValueChange = { viewModel.setDurationSeconds(it.roundToInt()) },
            valueRange = 3f..30f,
            colors = SliderDefaults.colors(
              thumbColor = ZuzuPink,
              activeTrackColor = ZuzuMagenta,
              inactiveTrackColor = ZuzuPurpleSoft,
            ),
          )
        }
      }

      // 4. Typography, Brand Logo & Stickers
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuViolet, ZuzuMagenta),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SectionHeader(
            title = "4. Animated Text, Logo & Stickers",
            gradientColors = ZuzuVioletPinkGradient,
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = project.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Project Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = project.headlineText,
            onValueChange = { viewModel.updateCopy(it, project.subheadlineText) },
            label = { Text("Animated Headline") },
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = project.subheadlineText,
            onValueChange = { viewModel.updateCopy(project.headlineText, it) },
            label = { Text("Subheadline / Call to Action") },
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Headline Size: ${project.textFontSizeSp}sp",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Slider(
            value = project.textFontSizeSp.toFloat(),
            onValueChange = { viewModel.updateTextStyle(project.textColorArgb, it.roundToInt()) },
            valueRange = 16f..40f,
            colors = SliderDefaults.colors(
              thumbColor = ZuzuMagenta,
              activeTrackColor = ZuzuPurple,
              inactiveTrackColor = ZuzuPurpleSoft,
            ),
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf(
              "White" to 0xFFFFFFFFL,
              "Gold" to 0xFFFDE047L,
              "Cyan" to 0xFF67E8F9L,
              "Ink" to 0xFF0F172AL,
            ).forEach { (label, argb) ->
              ZuzuPillChip(
                label = label,
                selected = project.textColorArgb == argb,
                onClick = { viewModel.updateTextStyle(argb, project.textFontSizeSp) },
                gradientColors = ZuzuVioletPinkGradient,
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = project.logoLabel,
              onValueChange = { viewModel.setLogoBadge(it) },
              label = { Text("Logo Badge Text") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            ZuzuSecondaryActionButton(
              text = "Logo Image",
              onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.PhotoLibrary,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Stickers (${project.stickers.size})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = ZuzuInkPrimary,
            )
            ZuzuSecondaryActionButton(
              text = "Add Sticker",
              onClick = { showStickerDialog = true },
              accentColor = ZuzuPink,
              icon = Icons.Rounded.Add,
            )
          }
          if (project.stickers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              project.stickers.forEach { st ->
                ZuzuPillChip(
                  label = "${st.badgeSymbol} ${st.label} ✕",
                  selected = true,
                  onClick = { viewModel.removeSticker(st.id) },
                  gradientColors = ZuzuPinkOrangeGradient,
                )
              }
            }
          }
        }
      }

      // 5. Attached Media & Background Music
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuCyan, ZuzuPurple),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SectionHeader(
            title = "5. Media Assets & Background Music",
            gradientColors = ZuzuBlueTurquoiseGradient,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ZuzuSecondaryActionButton(
              text = "Pick Media",
              onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
              accentColor = ZuzuElectricBlue,
              icon = Icons.Rounded.PhotoLibrary,
              modifier = Modifier.weight(1f),
            )
            ZuzuSecondaryActionButton(
              text = "From Library",
              onClick = { showLibraryDialog = true },
              accentColor = ZuzuTurquoise,
              icon = Icons.Rounded.LibraryAdd,
              modifier = Modifier.weight(1f),
            )
          }
          project.mediaSlots.forEach { slot ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ZuzuWhite,
              border = BorderStroke(1.dp, ZuzuBorderLight),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "${slot.mediaType.displayLabel}: ${slot.title}",
                  style = MaterialTheme.typography.bodySmall,
                  color = ZuzuInkPrimary,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.removeMediaSlot(slot.id) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove media",
                    tint = ZuzuError,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = project.backgroundMusicTitle?.let { "Music: $it" } ?: "No background music",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkPrimary,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            ZuzuSecondaryActionButton(
              text = "Add Music",
              onClick = { musicPicker.launch(arrayOf("audio/*")) },
              accentColor = ZuzuPurple,
              icon = Icons.Rounded.MusicNote,
            )
          }
          if (!project.backgroundMusicUriString.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Slider(
              value = project.backgroundMusicVolume,
              onValueChange = viewModel::setMusicVolume,
              colors = SliderDefaults.colors(
                thumbColor = ZuzuMagenta,
                activeTrackColor = ZuzuPurple,
                inactiveTrackColor = ZuzuPurpleSoft,
              ),
            )
            TextButton(onClick = viewModel::removeBackgroundMusic) {
              Text("Remove Audio Track", color = ZuzuError, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 6. Export MP4 / PNG / JPEG + Send to Approval Center
      item {
        MotionExportAndApprovalCard(
          uiState = uiState,
          viewModel = viewModel,
          onSendToApproval = onOpenApprovalCenter,
          glowEnabled = glowEnabled,
        )
      }
    }
  }

  if (showStickerDialog) {
    var label by remember { mutableStateOf("NEW DROP") }
    var symbol by remember { mutableStateOf("⚡") }
    AlertDialog(
      onDismissRequest = { showStickerDialog = false },
      containerColor = ZuzuWhite,
      title = {
        Text("Add Motion Sticker", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Sticker Text") },
            modifier = Modifier.fillMaxWidth(),
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf("⚡", "🔥", "★", "NEW", "▶").forEach { badge ->
              ZuzuPillChip(
                label = badge,
                selected = symbol == badge,
                onClick = { symbol = badge },
                gradientColors = ZuzuPinkOrangeGradient,
              )
            }
          }
        }
      },
      confirmButton = {
        ZuzuGradientButton(
          text = "Add",
          onClick = {
            viewModel.addSticker(label, symbol)
            showStickerDialog = false
          },
          gradientColors = ZuzuVioletPinkGradient,
        )
      },
      dismissButton = {
        TextButton(onClick = { showStickerDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showLibraryDialog) {
    val compatible = mediaLibraryEntries.filter { it.mediaType == MediaType.Image || it.mediaType == MediaType.Video }
    AlertDialog(
      onDismissRequest = { showLibraryDialog = false },
      containerColor = ZuzuWhite,
      title = {
        Text("Select From Media Library", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (compatible.isEmpty()) {
            Text(
              text = "No local image or video assets saved yet. Add files in Sources or export from Video Editor.",
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkSecondary,
            )
          } else {
            compatible.forEach { entry ->
              ZuzuSecondaryActionButton(
                text = "${entry.title} (${entry.permission.status.displayLabel})",
                onClick = {
                  viewModel.addMediaFromLibrary(entry)
                  showLibraryDialog = false
                },
                accentColor = ZuzuElectricBlue,
                icon = Icons.Rounded.PhotoLibrary,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLibraryDialog = false }) {
          Text("Close", color = ZuzuPurple, fontWeight = FontWeight.Bold)
        }
      },
    )
  }

  if (showSavedProjectsDialog) {
    AlertDialog(
      onDismissRequest = { showSavedProjectsDialog = false },
      containerColor = ZuzuWhite,
      title = {
        Text("Saved Motion Projects", fontWeight = FontWeight.ExtraBold, color = ZuzuInkPrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          uiState.savedProjects.forEach { saved ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              ZuzuSecondaryActionButton(
                text = "${saved.title} • ${saved.templateCategory.displayLabel}",
                onClick = {
                  viewModel.loadSavedProject(saved)
                  showSavedProjectsDialog = false
                },
                accentColor = ZuzuPurple,
                modifier = Modifier.weight(1f),
              )
              IconButton(onClick = { viewModel.deleteSavedProject(saved.id) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = ZuzuError)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSavedProjectsDialog = false }) {
          Text("Close", color = ZuzuPurple, fontWeight = FontWeight.Bold)
        }
      },
    )
  }
}
