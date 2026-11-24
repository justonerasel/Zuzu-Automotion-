package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ConnectedSocialAccount
import com.example.model.PlatformConnectionPhase
import com.example.model.SocialPlatform
import com.example.model.SocialPlatformAccountState
import com.example.ui.components.StatusPill
import com.example.ui.components.ZuzuAlertBanner
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuGradientIconBadge
import com.example.ui.components.ZuzuHeroBanner
import com.example.ui.components.ZuzuSecondaryActionButton
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.theme.PlatformFacebookBlue
import com.example.ui.theme.PlatformTikTokCyan
import com.example.ui.theme.PlatformTikTokPink
import com.example.ui.theme.PlatformYouTubeCoral
import com.example.ui.theme.PlatformYouTubeRed
import com.example.ui.theme.ZuzuBlueTurquoiseGradient
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuError
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuOrange
import com.example.ui.theme.ZuzuSoftLavender
import com.example.ui.theme.ZuzuSuccess
import com.example.ui.theme.ZuzuWarning
import com.example.ui.theme.ZuzuWhite
import com.example.viewmodel.ZuzuUiState

@Composable
fun SocialAccountsScreen(
  uiState: ZuzuUiState,
  onBack: () -> Unit,
  onConnectPlatform: (SocialPlatform) -> Unit,
  onDisconnectAccount: (SocialPlatform, String) -> Unit,
  onDisconnectPlatform: (SocialPlatform) -> Unit,
  onDismissStatus: (SocialPlatform) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val glowEnabled = uiState.settings.ambientGlowEnabled
  var setupDialogPlatformState by remember { mutableStateOf<SocialPlatformAccountState?>(null) }

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
          title = stringResource(R.string.social_accounts_title),
          onBack = onBack,
          backButtonTag = "social_accounts_back_button",
          badgeText = "${uiState.connectedPlatformCount} / ${uiState.platformAccountStates.size}",
          badgeTint = ZuzuElectricBlue,
        )
      }

      item {
        ZuzuHeroBanner(
          badge = "PKCE OAuth 2.0 Architecture",
          title = stringResource(R.string.social_accounts_title),
          subtitle = "${stringResource(R.string.social_accounts_subtitle)}\n${stringResource(R.string.social_accounts_security_banner)}",
          icon = Icons.Rounded.Lock,
          gradientColors = ZuzuBlueTurquoiseGradient,
        )
      }

      items(uiState.platformAccountStates, key = { it.platform.id }) { platformState ->
        SocialPlatformCard(
          state = platformState,
          glowEnabled = glowEnabled,
          onConnect = { onConnectPlatform(platformState.platform) },
          onDisconnectAccount = { accountId ->
            onDisconnectAccount(platformState.platform, accountId)
          },
          onDisconnectAll = { onDisconnectPlatform(platformState.platform) },
          onShowSetupChecklist = { setupDialogPlatformState = platformState },
          onDismissStatus = { onDismissStatus(platformState.platform) },
        )
      }

      item {
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }

  setupDialogPlatformState?.let { platformState ->
    OAuthSetupRequirementsDialog(
      state = platformState,
      onDismiss = { setupDialogPlatformState = null },
    )
  }
}

@Composable
private fun SocialPlatformCard(
  state: SocialPlatformAccountState,
  glowEnabled: Boolean,
  onConnect: () -> Unit,
  onDisconnectAccount: (String) -> Unit,
  onDisconnectAll: () -> Unit,
  onShowSetupChecklist: () -> Unit,
  onDismissStatus: () -> Unit,
) {
  val (gradientColors, icon) = platformVisuals(state.platform)
  val (statusLabel, statusTint) = when (state.phase) {
    PlatformConnectionPhase.NotConnected -> stringResource(R.string.status_not_connected) to ZuzuInkMuted
    PlatformConnectionPhase.Connecting -> stringResource(R.string.social_status_connecting) to ZuzuElectricBlue
    PlatformConnectionPhase.SetupRequired -> stringResource(R.string.social_status_setup_required) to ZuzuWarning
    PlatformConnectionPhase.Connected -> stringResource(R.string.status_connected) to ZuzuSuccess
    PlatformConnectionPhase.Error -> stringResource(R.string.status_error) to ZuzuError
  }

  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradientColors,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("platform_card_${state.platform.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        ZuzuGradientIconBadge(
          icon = icon,
          contentDescription = state.platform.displayName,
          gradientColors = gradientColors,
          size = 50.dp,
          iconSize = 27.dp,
          cornerRadius = 16.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = state.platform.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = ZuzuInkPrimary,
          )
          Text(
            text = state.platform.oauthProtocolLabel,
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      StatusPill(label = statusLabel, tint = statusTint)
    }

    AnimatedVisibility(visible = state.phase == PlatformConnectionPhase.Connecting) {
      Column {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = ZuzuSoftLavender,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.5.dp,
              color = gradientColors.first(),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = state.statusMessage ?: stringResource(R.string.social_status_connecting),
              style = MaterialTheme.typography.bodySmall,
              color = ZuzuInkPrimary,
            )
          }
        }
      }
    }

    AnimatedVisibility(visible = state.phase == PlatformConnectionPhase.SetupRequired) {
      Column {
        Spacer(modifier = Modifier.height(14.dp))
        ZuzuAlertBanner(
          title = stringResource(R.string.social_status_setup_required),
          message = state.statusMessage.orEmpty(),
          tint = ZuzuOrange,
          icon = Icons.Rounded.SettingsApplications,
          onDismiss = onDismissStatus,
          actionLabel = stringResource(R.string.social_action_setup_details),
          onAction = onShowSetupChecklist,
          modifier = Modifier.testTag("setup_required_banner_${state.platform.id}"),
        )
      }
    }

    AnimatedVisibility(
      visible = state.phase == PlatformConnectionPhase.Error && !state.errorMessage.isNullOrBlank(),
    ) {
      Column {
        Spacer(modifier = Modifier.height(14.dp))
        ZuzuAlertBanner(
          message = state.errorMessage.orEmpty(),
          tint = ZuzuError,
          icon = Icons.Rounded.ErrorOutline,
          onDismiss = onDismissStatus,
          actionLabel = stringResource(R.string.social_action_retry),
          onAction = onConnect,
          modifier = Modifier.testTag("error_banner_${state.platform.id}"),
        )
      }
    }

    if (state.connectedAccounts.isNotEmpty()) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(R.string.social_connected_accounts_label),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = ZuzuInkSecondary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.connectedAccounts.forEach { account ->
          ConnectedAccountRow(
            account = account,
            accentColor = gradientColors.first(),
            onDisconnect = { onDisconnectAccount(account.accountId) },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val isConnecting = state.phase == PlatformConnectionPhase.Connecting
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ZuzuGradientButton(
        text = if (state.isConnected) {
          stringResource(R.string.social_action_add_account)
        } else {
          "${stringResource(R.string.social_action_connect)} ${state.platform.displayName}"
        },
        onClick = onConnect,
        enabled = !isConnecting,
        gradientColors = gradientColors,
        icon = if (state.isConnected && state.supportsMultiAccount) Icons.Rounded.Add else null,
        modifier = Modifier
          .weight(1f)
          .testTag("connect_btn_${state.platform.id}"),
      )

      if (state.isConnected) {
        ZuzuSecondaryActionButton(
          text = if (state.connectedAccounts.size > 1) {
            stringResource(R.string.social_action_disconnect_all)
          } else {
            stringResource(R.string.social_action_disconnect)
          },
          onClick = onDisconnectAll,
          enabled = !isConnecting,
          accentColor = ZuzuError,
          icon = Icons.Rounded.LinkOff,
          modifier = Modifier.testTag("disconnect_btn_${state.platform.id}"),
        )
      }
    }
  }
}

@Composable
private fun ConnectedAccountRow(
  account: ConnectedSocialAccount,
  accentColor: Color,
  onDisconnect: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = ZuzuSoftLavender.copy(alpha = 0.7f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = stringResource(R.string.status_connected),
          tint = ZuzuSuccess,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = account.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Text(
            text = "${account.handle} • ${account.connectedAtLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
        }
      }
      TextButton(
        onClick = onDisconnect,
        modifier = Modifier.testTag("disconnect_account_${account.accountId}"),
      ) {
        Text(
          text = stringResource(R.string.social_action_disconnect),
          style = MaterialTheme.typography.labelMedium,
          color = ZuzuError,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun OAuthSetupRequirementsDialog(
  state: SocialPlatformAccountState,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ZuzuWhite,
    title = {
      Text(
        text = "${state.platform.displayName} — ${stringResource(R.string.social_setup_dialog_title)}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.social_setup_endpoint_label),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = state.metadata.authorizationEndpoint,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuElectricBlue,
        )

        Text(
          text = stringResource(R.string.social_setup_scopes_label),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = state.metadata.requiredScopes.joinToString("\n• ", prefix = "• "),
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )

        if (state.missingSetupItems.isNotEmpty()) {
          Text(
            text = stringResource(R.string.social_setup_missing_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ZuzuWarning,
          )
          Text(
            text = state.missingSetupItems.joinToString("\n• ", prefix = "• "),
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkPrimary,
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(R.string.dialog_close))
      }
    },
  )
}

private fun platformVisuals(platform: SocialPlatform): Pair<List<Color>, ImageVector> =
  when (platform) {
    SocialPlatform.YouTube -> listOf(PlatformYouTubeRed, PlatformYouTubeCoral) to Icons.Rounded.PlayCircle
    SocialPlatform.Facebook -> listOf(PlatformFacebookBlue, ZuzuCyan) to Icons.Rounded.Public
    SocialPlatform.TikTok -> listOf(PlatformTikTokCyan, PlatformTikTokPink) to Icons.Rounded.MusicNote
  }
