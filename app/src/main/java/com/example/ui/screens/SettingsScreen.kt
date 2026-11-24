package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.security.SecuritySanitizer
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientCard
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.theme.ZuzuAmber
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuGreen
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuVioletPinkGradient
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ZuzuUiState
import java.io.File

@Composable
fun SettingsScreen(
  uiState: ZuzuUiState,
  onToggleAmbientGlow: (Boolean) -> Unit,
  onSelectLanguage: (String) -> Unit,
  onToggleWorkflowAlerts: (Boolean) -> Unit,
  onToggleScheduleReminders: (Boolean) -> Unit,
  onRefreshStorage: (exportsDir: File, cacheDir: File, databaseDir: File?) -> Unit = { _, _, _ -> },
  onClearTemporaryCache: (exportsDir: File, cacheDir: File, databaseDir: File?) -> Unit = { _, _, _ -> },
  onClearStorageMessage: () -> Unit = {},
  onOpenSocialAccounts: () -> Unit = {},
  onOpenCloudStatus: () -> Unit = {},
  onOpenPublishingApiStatus: () -> Unit = {},
  modifier: Modifier = Modifier,
  bottomContentPadding: Dp = 108.dp,
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val exportsDir = remember(appContext) { File(appContext.filesDir, "zuzu_exports") }
  val cacheDir = remember(appContext) { appContext.cacheDir }
  val dbDir = remember(appContext) { appContext.getDatabasePath("zuzu_social_db").parentFile }

  LaunchedEffect(exportsDir, cacheDir, dbDir) {
    onRefreshStorage(exportsDir, cacheDir, dbDir)
  }

  val settings = uiState.settings
  val glowEnabled = settings.ambientGlowEnabled
  val storage = uiState.storageSummary

  var showLanguageDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZuzuHeroBanner(
          badge = stringResource(R.string.phase_badge),
          title = stringResource(R.string.settings_title),
          subtitle = stringResource(R.string.settings_subtitle),
          icon = Icons.Rounded.Settings,
          gradientColors = listOf(ZuzuDeepPurple, ZuzuElectricBlue, ZuzuTurquoise),
        )
      }

      // 1. Account Settings
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuDeepPurple, ZuzuViolet),
          onClick = onOpenCloudStatus,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_account_card"),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.AccountCircle,
            title = stringResource(R.string.settings_section_account),
            gradientColors = listOf(ZuzuDeepPurple, ZuzuViolet),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(
            label = "Creator Cloud Account",
            value = "Setup Required",
            valueColor = ZuzuAmber,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Local creative workspace works immediately. Google Sign-In & Firebase Auth activate once backend Firebase Auth is deployed.",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Review Cloud Auth & Sync Status",
              style = MaterialTheme.typography.labelMedium,
              color = ZuzuPurple,
              fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = ZuzuPurple)
          }
        }
      }

      // 2. Connected Social Accounts
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          onClick = onOpenSocialAccounts,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_connected_social_card"),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Link,
            title = stringResource(R.string.settings_section_connected_social),
            gradientColors = listOf(ZuzuElectricBlue, ZuzuTurquoise),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(
            label = "YouTube • Facebook Pages • TikTok",
            value = "${uiState.connectedPlatformCount} of 3 connected",
            valueColor = if (uiState.connectedPlatformCount > 0) ZuzuGreen else ZuzuElectricBlue,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Manage official PKCE OAuth authorization, active channels, or disconnect social accounts.",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.social_accounts_manage_cta),
              style = MaterialTheme.typography.labelMedium,
              color = ZuzuElectricBlue,
              fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = ZuzuElectricBlue)
          }
        }
      }

      // 3. API & Service Configuration Status
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuViolet, ZuzuPink),
          onClick = onOpenPublishingApiStatus,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_api_status_card"),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.CloudDone,
            title = stringResource(R.string.settings_section_api_status),
            gradientColors = listOf(ZuzuViolet, ZuzuPink),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(label = "Gemini AI Studio & Voice/Visual APIs", value = "Backend Setup Required", valueColor = ZuzuAmber)
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "YouTube, Meta Pages & TikTok Publishing APIs", value = "Backend Setup Required", valueColor = ZuzuAmber)
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Official Platform Analytics APIs", value = "Backend Setup Required", valueColor = ZuzuAmber)
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Local Room Storage & MP4 Renderer", value = "Active On-Device", valueColor = ZuzuGreen)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "All OAuth secrets and API keys live on the HTTPS backend — zero private credentials are stored in this APK.",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }

      // 4. Storage Information
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuTurquoise, ZuzuCyan),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_storage_card"),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Storage,
            title = stringResource(R.string.settings_section_storage),
            gradientColors = listOf(ZuzuTurquoise, ZuzuCyan),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(
            label = "Exported Videos & Motion Assets (${storage.exportedFilesCount} files)",
            value = SecuritySanitizer.formatBytesReadable(storage.exportsSizeBytes),
            valueColor = ZuzuTurquoise,
          )
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(
            label = "Projects, Library & Publishing Database",
            value = SecuritySanitizer.formatBytesReadable(storage.databaseSizeBytes),
          )
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(
            label = "Temporary App Cache",
            value = SecuritySanitizer.formatBytesReadable(storage.cacheSizeBytes),
          )
          uiState.storageActionMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = ZuzuGreen)
          }
          Spacer(modifier = Modifier.height(12.dp))
          ZuzuSecondaryActionButton(
            text = "Clear Temporary Cache (Keep Projects & Exports)",
            onClick = { onClearTemporaryCache(exportsDir, cacheDir, dbDir) },
            accentColor = ZuzuTurquoise,
            icon = Icons.Rounded.DeleteSweep,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_clear_cache_button"),
          )
        }
      }

      // 5. Privacy & Security
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuPurple, ZuzuElectricBlue),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_privacy_security_card"),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.settings_section_privacy_security),
            gradientColors = listOf(ZuzuPurple, ZuzuElectricBlue),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(label = "OAuth & API Secrets", value = "Backend Vault Only", valueColor = ZuzuGreen)
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Publishing Safety", value = "Mandatory User Approval", valueColor = ZuzuGreen)
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Media Access", value = "Android Photo Picker", valueColor = ZuzuGreen)
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 8.dp))
          InfoRow(label = "Device Auto-Backup", value = "Disabled (Protected)", valueColor = ZuzuGreen)
        }
      }

      // 6. Notifications
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuMagenta, ZuzuPink),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Notifications,
            title = stringResource(R.string.settings_section_notifications),
            gradientColors = listOf(ZuzuMagenta, ZuzuPink),
          )
          Spacer(modifier = Modifier.height(14.dp))
          SwitchSettingRow(
            label = stringResource(R.string.settings_notif_workflow_label),
            description = stringResource(R.string.settings_notif_workflow_desc),
            checked = settings.workflowAlertsEnabled,
            accentColor = ZuzuOrange,
            onCheckedChange = onToggleWorkflowAlerts,
            testTag = "settings_workflow_alerts_switch",
          )
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 12.dp))
          SwitchSettingRow(
            label = stringResource(R.string.settings_notif_schedule_label),
            description = stringResource(R.string.settings_notif_schedule_desc),
            checked = settings.scheduleRemindersEnabled,
            accentColor = ZuzuPink,
            onCheckedChange = onToggleScheduleReminders,
            testTag = "settings_schedule_reminders_switch",
          )
        }
      }

      // 7. Appearance & Language
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = ZuzuVioletPinkGradient,
          modifier = Modifier.fillMaxWidth(),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Palette,
            title = stringResource(R.string.settings_section_appearance),
            gradientColors = listOf(ZuzuViolet, ZuzuPurple),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(
            label = stringResource(R.string.settings_theme_label),
            value = "Design 2 Active",
            valueColor = ZuzuPurple,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.settings_theme_desc),
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 12.dp))
          SwitchSettingRow(
            label = stringResource(R.string.settings_glow_label),
            description = stringResource(R.string.settings_glow_desc),
            checked = settings.ambientGlowEnabled,
            accentColor = ZuzuPurple,
            onCheckedChange = onToggleAmbientGlow,
            testTag = "settings_glow_switch",
          )
        }
      }

      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
          onClick = { showLanguageDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_language_card"),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Language,
            title = stringResource(R.string.settings_section_language),
            gradientColors = listOf(ZuzuTurquoise, ZuzuElectricBlue),
          )
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ZuzuInkPrimary,
              )
              Text(
                text = settings.selectedLanguage,
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuTurquoise,
                fontWeight = FontWeight.SemiBold,
              )
            }
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = stringResource(R.string.settings_language_label),
              tint = ZuzuTurquoise,
            )
          }
        }
      }

      // 8. App Information & Version
      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = listOf(ZuzuElectricBlue, ZuzuCyan),
          modifier = Modifier.fillMaxWidth(),
        ) {
          SettingsSectionTitle(
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.settings_section_app_info),
            gradientColors = listOf(ZuzuElectricBlue, ZuzuCyan),
          )
          Spacer(modifier = Modifier.height(14.dp))
          InfoRow(
            label = stringResource(R.string.app_name),
            value = stringResource(R.string.phase_badge),
            valueColor = ZuzuElectricBlue,
          )
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 10.dp))
          InfoRow(
            label = stringResource(R.string.settings_app_version_label),
            value = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
          )
          HorizontalDivider(color = ZuzuBorderLight, modifier = Modifier.padding(vertical = 10.dp))
          InfoRow(
            label = stringResource(R.string.settings_min_android_label),
            value = stringResource(R.string.settings_min_android_value),
          )
        }
      }

      // 9. About Zuzu Social AI
      item {
        ZuzuGradientCard(
          gradientColors = ZuzuVioletPinkGradient,
          onClick = { showAboutDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_about_card"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = ZuzuWhite,
              shadowElevation = 4.dp,
              modifier = Modifier.size(50.dp),
            ) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(2.5.dp)
                  .clip(RoundedCornerShape(12.dp)),
              ) {
                Image(
                  painter = painterResource(id = R.drawable.img_zuzu_logo),
                  contentDescription = stringResource(R.string.app_name),
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.settings_section_about),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ZuzuWhite,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = stringResource(R.string.settings_about_summary),
                style = MaterialTheme.typography.bodySmall,
                color = ZuzuWhite.copy(alpha = 0.90f),
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Rounded.ChevronRight,
              contentDescription = stringResource(R.string.settings_about_cta),
              tint = ZuzuWhite,
            )
          }
        }
      }
    }
  }

  if (showLanguageDialog) {
    val languages = listOf("English (US)", "English (UK)", "Bangla (বাংলা) Content Support")
    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      containerColor = ZuzuWhite,
      title = { Text(text = stringResource(R.string.settings_language_label)) },
      text = {
        Column {
          languages.forEach { lang ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = settings.selectedLanguage == lang,
                onClick = {
                  onSelectLanguage(lang)
                  showLanguageDialog = false
                },
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = lang, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLanguageDialog = false }) {
          Text(text = stringResource(R.string.dialog_close))
        }
      },
    )
  }

  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      containerColor = ZuzuWhite,
      title = { Text(text = stringResource(R.string.app_name)) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusPill(
            label = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
            tint = ZuzuPurple,
          )
          Text(
            text = stringResource(R.string.settings_about_summary),
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            text = stringResource(R.string.intro_phase_note),
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showAboutDialog = false }) {
          Text(text = stringResource(R.string.dialog_close))
        }
      },
    )
  }
}

@Composable
private fun SettingsSectionTitle(
  icon: ImageVector,
  title: String,
  gradientColors: List<Color>,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    ZuzuGradientIconBadge(
      icon = icon,
      contentDescription = null,
      gradientColors = gradientColors,
      size = 36.dp,
      iconSize = 18.dp,
      cornerRadius = 11.dp,
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = ZuzuInkPrimary,
    )
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
  valueColor: Color = ZuzuInkSecondary,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = ZuzuInkPrimary,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = valueColor,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun SwitchSettingRow(
  label: String,
  description: String,
  checked: Boolean,
  accentColor: Color,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = ZuzuInkPrimary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = ZuzuWhite,
        checkedTrackColor = accentColor,
      ),
      modifier = Modifier.testTag(testTag),
    )
  }
}
