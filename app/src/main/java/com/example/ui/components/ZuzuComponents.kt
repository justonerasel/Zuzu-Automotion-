package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PrimaryTab
import com.example.ui.theme.ZuzuBorderLight
import com.example.ui.theme.ZuzuCanvasBottom
import com.example.ui.theme.ZuzuCanvasMid
import com.example.ui.theme.ZuzuCanvasTop
import com.example.ui.theme.ZuzuCyan
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuGlassSurface
import com.example.ui.theme.ZuzuHeaderGradient
import com.example.ui.theme.ZuzuHeroGradient
import com.example.ui.theme.ZuzuInkMuted
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuMagenta
import com.example.ui.theme.ZuzuPink
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuPurpleSoft
import com.example.ui.theme.ZuzuSurfaceWhite
import com.example.ui.theme.ZuzuTurquoise
import com.example.ui.theme.ZuzuTurquoiseBright
import com.example.ui.theme.ZuzuViolet
import com.example.ui.theme.ZuzuWhite

@Composable
fun ZuzuBackground(
  glowEnabled: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val turquoiseWash = ZuzuTurquoiseBright.copy(alpha = if (glowEnabled) 0.17f else 0.08f)
  val electricBlueWash = ZuzuElectricBlue.copy(alpha = if (glowEnabled) 0.13f else 0.06f)
  val violetWash = ZuzuPurple.copy(alpha = if (glowEnabled) 0.15f else 0.07f)
  val magentaPinkWash = ZuzuMagenta.copy(alpha = if (glowEnabled) 0.14f else 0.06f)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(ZuzuCanvasTop, ZuzuCanvasMid, ZuzuCanvasBottom),
        ),
      )
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(turquoiseWash, Color.Transparent),
            center = Offset(size.width * 0.90f, size.height * 0.07f),
            radius = size.maxDimension * 0.52f,
          ),
          center = Offset(size.width * 0.90f, size.height * 0.07f),
          radius = size.maxDimension * 0.52f,
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(violetWash, Color.Transparent),
            center = Offset(size.width * 0.06f, size.height * 0.28f),
            radius = size.maxDimension * 0.48f,
          ),
          center = Offset(size.width * 0.06f, size.height * 0.28f),
          radius = size.maxDimension * 0.48f,
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(magentaPinkWash, Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.68f),
            radius = size.maxDimension * 0.48f,
          ),
          center = Offset(size.width * 0.88f, size.height * 0.68f),
          radius = size.maxDimension * 0.48f,
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(electricBlueWash, Color.Transparent),
            center = Offset(size.width * 0.16f, size.height * 0.90f),
            radius = size.maxDimension * 0.42f,
          ),
          center = Offset(size.width * 0.16f, size.height * 0.90f),
          radius = size.maxDimension * 0.42f,
        )
      },
    content = content,
  )
}

@Composable
fun ZuzuGlassCard(
  modifier: Modifier = Modifier,
  glowEnabled: Boolean = true,
  accentColors: List<Color> = listOf(ZuzuPurple, ZuzuCyan),
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val start = accentColors.firstOrNull() ?: ZuzuPurple
  val end = accentColors.lastOrNull() ?: ZuzuCyan

  val borderBrush = if (glowEnabled) {
    Brush.linearGradient(
      listOf(start.copy(alpha = 0.30f), ZuzuWhite.copy(alpha = 0.96f), end.copy(alpha = 0.28f)),
    )
  } else {
    Brush.linearGradient(listOf(ZuzuBorderLight, ZuzuBorderLight))
  }

  val tintWash = Brush.linearGradient(
    listOf(
      ZuzuSurfaceWhite.copy(alpha = 0.95f),
      start.copy(alpha = 0.045f),
      end.copy(alpha = 0.035f),
    ),
  )

  val shape = RoundedCornerShape(24.dp)
  val border = BorderStroke(1.dp, borderBrush)

  if (onClick != null) {
    Surface(
      onClick = onClick,
      modifier = modifier.minimumInteractiveComponentSize(),
      shape = shape,
      color = ZuzuGlassSurface,
      shadowElevation = if (glowEnabled) 8.dp else 3.dp,
      border = border,
    ) {
      Column(
        modifier = Modifier
          .background(tintWash)
          .padding(18.dp),
        content = content,
      )
    }
  } else {
    Surface(
      modifier = modifier,
      shape = shape,
      color = ZuzuGlassSurface,
      shadowElevation = if (glowEnabled) 6.dp else 2.dp,
      border = border,
    ) {
      Column(
        modifier = Modifier
          .background(tintWash)
          .padding(18.dp),
        content = content,
      )
    }
  }
}

@Composable
fun ZuzuGradientCard(
  gradientColors: List<Color>,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(26.dp)
  val border = BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(
      listOf(ZuzuWhite.copy(alpha = 0.50f), ZuzuWhite.copy(alpha = 0.14f)),
    ),
  )
  val brush = Brush.linearGradient(colors = gradientColors)

  if (onClick != null) {
    Surface(
      onClick = onClick,
      modifier = modifier.minimumInteractiveComponentSize(),
      shape = shape,
      color = Color.Transparent,
      shadowElevation = 12.dp,
      border = border,
    ) {
      Column(
        modifier = Modifier
          .background(brush)
          .padding(20.dp),
        content = content,
      )
    }
  } else {
    Surface(
      modifier = modifier,
      shape = shape,
      color = Color.Transparent,
      shadowElevation = 10.dp,
      border = border,
    ) {
      Column(
        modifier = Modifier
          .background(brush)
          .padding(20.dp),
        content = content,
      )
    }
  }
}

@Composable
fun ZuzuBrandHeader(
  modifier: Modifier = Modifier,
  notificationCount: Int = 0,
  onNotificationClick: (() -> Unit)? = null,
) {
  ZuzuGradientCard(
    gradientColors = ZuzuHeaderGradient,
    modifier = modifier.fillMaxWidth(),
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
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = ZuzuWhite,
          shadowElevation = 8.dp,
          modifier = Modifier.size(54.dp),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(3.dp)
              .clip(RoundedCornerShape(15.dp)),
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
        Column {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = ZuzuWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuWhite.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      if (onNotificationClick != null) {
        Box {
          Surface(
            shape = CircleShape,
            color = ZuzuWhite.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.44f)),
          ) {
            IconButton(
              onClick = onNotificationClick,
              modifier = Modifier
                .size(44.dp)
                .testTag("dashboard_notifications_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = stringResource(R.string.settings_section_notifications),
                tint = ZuzuWhite,
                modifier = Modifier.size(21.dp),
              )
            }
          }
          if (notificationCount > 0) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .size(16.dp)
                .clip(CircleShape)
                .background(ZuzuPink),
            ) {
              Text(
                text = notificationCount.coerceAtMost(9).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = ZuzuWhite,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      } else {
        GlassPill(label = stringResource(R.string.phase_badge))
      }
    }
  }
}

@Composable
fun ZuzuSecondaryTopBar(
  title: String,
  onBack: () -> Unit,
  backButtonTag: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  badgeText: String? = null,
  badgeTint: Color = ZuzuElectricBlue,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      Surface(
        shape = CircleShape,
        color = ZuzuWhite,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, ZuzuPurple.copy(alpha = 0.16f)),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag(backButtonTag),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = ZuzuDeepPurple,
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = ZuzuInkPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = ZuzuInkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    if (trailingContent != null) {
      trailingContent()
    } else if (!badgeText.isNullOrBlank()) {
      StatusPill(label = badgeText, tint = badgeTint)
    }
  }
}

@Composable
fun ZuzuHeroBanner(
  badge: String,
  title: String,
  subtitle: String,
  icon: ImageVector,
  gradientColors: List<Color> = ZuzuHeroGradient,
  modifier: Modifier = Modifier,
  trailingContent: (@Composable () -> Unit)? = null,
  bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
  ZuzuGradientCard(
    gradientColors = gradientColors,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      GlassPill(label = badge)
      if (trailingContent != null) {
        trailingContent()
      } else {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = ZuzuWhite.copy(alpha = 0.22f),
          border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.38f)),
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = ZuzuWhite,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.ExtraBold,
      color = ZuzuWhite,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = ZuzuWhite.copy(alpha = 0.92f),
    )
    if (bottomContent != null) {
      Spacer(modifier = Modifier.height(12.dp))
      bottomContent()
    }
  }
}

@Composable
fun ZuzuCompactStatCard(
  label: String,
  value: String,
  icon: ImageVector,
  gradientColors: List<Color>,
  modifier: Modifier = Modifier,
  statusLabel: String? = null,
  statusTint: Color = gradientColors.first(),
  glowEnabled: Boolean = true,
  onClick: (() -> Unit)? = null,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradientColors,
    onClick = onClick,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ZuzuGradientIconBadge(
        icon = icon,
        contentDescription = label,
        gradientColors = gradientColors,
        size = 38.dp,
        iconSize = 20.dp,
      )
      if (!statusLabel.isNullOrBlank()) {
        StatusPill(label = statusLabel, tint = statusTint)
      }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
      color = ZuzuInkPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = ZuzuInkSecondary,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun ZuzuGradientIconBadge(
  icon: ImageVector,
  contentDescription: String?,
  gradientColors: List<Color>,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  iconSize: Dp = 22.dp,
  cornerRadius: Dp = 14.dp,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(cornerRadius))
      .background(Brush.linearGradient(gradientColors)),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = ZuzuWhite,
      modifier = Modifier.size(iconSize),
    )
  }
}

@Composable
fun ZuzuGradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  gradientColors: List<Color> = listOf(ZuzuElectricBlue, ZuzuPurple, ZuzuMagenta),
  icon: ImageVector? = null,
  enabled: Boolean = true,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = ZuzuWhite,
      disabledContainerColor = Color.Transparent,
      disabledContentColor = ZuzuWhite.copy(alpha = 0.72f),
    ),
    contentPadding = PaddingValues(0.dp),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(
          if (enabled) {
            Brush.horizontalGradient(gradientColors)
          } else {
            Brush.horizontalGradient(listOf(ZuzuInkMuted.copy(alpha = 0.65f), ZuzuInkMuted.copy(alpha = 0.50f)))
          },
        )
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        if (icon != null) {
          Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ZuzuSecondaryActionButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accentColor: Color = ZuzuElectricBlue,
  icon: ImageVector? = null,
  enabled: Boolean = true,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(14.dp),
    color = if (enabled) accentColor.copy(alpha = 0.11f) else ZuzuInkMuted.copy(alpha = 0.08f),
    border = BorderStroke(
      1.dp,
      if (enabled) accentColor.copy(alpha = 0.34f) else ZuzuBorderLight,
    ),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) accentColor else ZuzuInkMuted,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (enabled) accentColor else ZuzuInkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ZuzuPillChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  gradientColors: List<Color> = listOf(ZuzuElectricBlue, ZuzuPurple),
  leadingIcon: ImageVector? = null,
) {
  val accent = gradientColors.first()
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = Color.Transparent,
    shadowElevation = if (selected) 5.dp else 0.dp,
    border = BorderStroke(
      1.dp,
      if (selected) Color.Transparent else accent.copy(alpha = 0.24f),
    ),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .clip(CircleShape)
        .background(
          if (selected) {
            Brush.horizontalGradient(gradientColors)
          } else {
            Brush.horizontalGradient(listOf(ZuzuWhite.copy(alpha = 0.90f), ZuzuWhite.copy(alpha = 0.95f)))
          },
        )
        .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
          Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = if (selected) ZuzuWhite else accent,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
          color = if (selected) ZuzuWhite else ZuzuInkPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun <T> ZuzuSegmentedTabStrip(
  items: List<T>,
  selectedItem: T,
  labelFor: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  gradientColors: List<Color> = listOf(ZuzuElectricBlue, ZuzuPurple, ZuzuMagenta),
  testTagFor: (T) -> String = { "" },
  scrollable: Boolean = false,
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = ZuzuWhite.copy(alpha = 0.92f),
    shadowElevation = 4.dp,
    border = BorderStroke(1.dp, ZuzuPurple.copy(alpha = 0.14f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    val rowModifier = if (scrollable) {
      Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(5.dp)
    } else {
      Modifier
        .fillMaxWidth()
        .padding(5.dp)
    }
    Row(
      modifier = rowModifier,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEach { item ->
        val selected = item == selectedItem
        val tag = testTagFor(item)
        val itemModifier = if (scrollable) {
          Modifier
            .minimumInteractiveComponentSize()
            .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier)
        } else {
          Modifier
            .weight(1f)
            .minimumInteractiveComponentSize()
            .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier)
        }
        Surface(
          onClick = { onSelect(item) },
          shape = RoundedCornerShape(17.dp),
          color = Color.Transparent,
          shadowElevation = if (selected) 4.dp else 0.dp,
          modifier = itemModifier,
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .clip(RoundedCornerShape(17.dp))
              .then(
                if (selected) {
                  Modifier.background(Brush.horizontalGradient(gradientColors))
                } else {
                  Modifier.background(Color.Transparent)
                },
              )
              .padding(horizontal = 12.dp, vertical = 9.dp),
          ) {
            Text(
              text = labelFor(item),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
              color = if (selected) ZuzuWhite else ZuzuInkSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ZuzuAlertBanner(
  message: String,
  tint: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  title: String? = null,
  onDismiss: (() -> Unit)? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = ZuzuWhite,
    shadowElevation = 3.dp,
    border = BorderStroke(1.dp, tint.copy(alpha = 0.36f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(tint.copy(alpha = 0.08f))
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(11.dp))
          .background(tint.copy(alpha = 0.16f)),
      ) {
        Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(19.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        if (!title.isNullOrBlank()) {
          Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ZuzuInkPrimary,
          )
          Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
          Spacer(modifier = Modifier.height(6.dp))
          TextButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
          ) {
            Text(
              text = actionLabel,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = tint,
            )
          }
        }
      }
      if (onDismiss != null) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.social_action_dismiss),
            tint = ZuzuInkMuted,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
fun ZuzuEmptyStatePanel(
  title: String,
  description: String,
  icon: ImageVector,
  gradientColors: List<Color> = listOf(ZuzuElectricBlue, ZuzuPurple, ZuzuPink),
  modifier: Modifier = Modifier,
  glowEnabled: Boolean = true,
  ctaLabel: String? = null,
  ctaIcon: ImageVector? = null,
  onCtaClick: (() -> Unit)? = null,
) {
  ZuzuGlassCard(
    glowEnabled = glowEnabled,
    accentColors = gradientColors,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(62.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(Brush.linearGradient(gradientColors)),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = ZuzuWhite,
          modifier = Modifier.size(30.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = ZuzuInkPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = ZuzuInkSecondary,
        textAlign = TextAlign.Center,
      )
      if (!ctaLabel.isNullOrBlank() && onCtaClick != null) {
        Spacer(modifier = Modifier.height(16.dp))
        ZuzuGradientButton(
          text = ctaLabel,
          onClick = onCtaClick,
          gradientColors = gradientColors,
          icon = ctaIcon,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
fun ZuzuLoadingBanner(
  label: String,
  modifier: Modifier = Modifier,
  accentColor: Color = ZuzuPurple,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = ZuzuWhite,
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = accentColor,
      )
      Spacer(modifier = Modifier.height(8.dp))
      LinearProgressIndicator(
        color = accentColor,
        trackColor = accentColor.copy(alpha = 0.16f),
        strokeCap = StrokeCap.Round,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(CircleShape),
      )
    }
  }
}

@Composable
fun GlassPill(
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = ZuzuWhite.copy(alpha = 0.22f),
    border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.44f)),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = ZuzuWhite,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
    )
  }
}

@Composable
fun StatusPill(
  label: String,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = tint.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.30f)),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = tint,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  gradientColors: List<Color> = listOf(ZuzuPurple, ZuzuTurquoise),
  modifier: Modifier = Modifier,
  trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .width(5.dp)
        .height(if (subtitle != null) 38.dp else 24.dp)
        .clip(CircleShape)
        .background(Brush.verticalGradient(gradientColors)),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = ZuzuInkPrimary,
        fontWeight = FontWeight.ExtraBold,
      )
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = ZuzuInkSecondary,
        )
      }
    }
    if (trailingContent != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        content = trailingContent,
      )
    }
  }
}

@Composable
fun ZuzuFloatingBottomBar(
  activeTab: PrimaryTab,
  onSelectTab: (PrimaryTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = ZuzuSurfaceWhite.copy(alpha = 0.96f),
      shadowElevation = 18.dp,
      border = BorderStroke(
        1.dp,
        Brush.horizontalGradient(
          listOf(
            ZuzuElectricBlue.copy(alpha = 0.28f),
            ZuzuPurple.copy(alpha = 0.28f),
            ZuzuPink.copy(alpha = 0.26f),
            ZuzuTurquoise.copy(alpha = 0.26f),
          ),
        ),
      ),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PrimaryTab.entries.forEach { tab ->
          val selected = activeTab == tab
          val gradient = tabGradient(tab)
          val primaryAccent = gradient.first()
          val labelColor by animateColorAsState(
            targetValue = if (selected) primaryAccent else ZuzuInkMuted,
            animationSpec = tween(durationMillis = 200),
            label = "NavLabelColor",
          )

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(22.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = primaryAccent),
                onClick = { onSelectTab(tab) },
              )
              .padding(vertical = 4.dp)
              .testTag(tab.testTag),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .clip(CircleShape)
                .then(
                  if (selected) {
                    Modifier.background(Brush.horizontalGradient(gradient))
                  } else {
                    Modifier
                  },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = tabIcon(tab, selected),
                contentDescription = tabLabel(tab),
                tint = if (selected) ZuzuWhite else ZuzuInkMuted,
                modifier = Modifier.size(22.dp),
              )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = tabLabel(tab),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = labelColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ZuzuNavRail(
  activeTab: PrimaryTab,
  onSelectTab: (PrimaryTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationRail(
    containerColor = ZuzuSurfaceWhite.copy(alpha = 0.94f),
    modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
  ) {
    Spacer(modifier = Modifier.height(16.dp))
    PrimaryTab.entries.forEach { tab ->
      val selected = activeTab == tab
      val accent = tabGradient(tab).first()
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(tab) },
        modifier = Modifier.testTag(tab.testTag),
        icon = {
          Icon(
            imageVector = tabIcon(tab, selected),
            contentDescription = tabLabel(tab),
            tint = if (selected) accent else ZuzuInkMuted,
          )
        },
        label = {
          Text(
            text = tabLabel(tab),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
          )
        },
        colors = NavigationRailItemDefaults.colors(
          selectedIconColor = accent,
          selectedTextColor = accent,
          indicatorColor = ZuzuPurpleSoft,
          unselectedIconColor = ZuzuInkMuted,
          unselectedTextColor = ZuzuInkMuted,
        ),
      )
    }
  }
}

private fun tabGradient(tab: PrimaryTab): List<Color> = when (tab) {
  PrimaryTab.Dashboard -> listOf(ZuzuElectricBlue, ZuzuCyan, ZuzuTurquoiseBright)
  PrimaryTab.Sources -> listOf(ZuzuTurquoise, ZuzuCyan)
  PrimaryTab.Studio -> listOf(ZuzuViolet, ZuzuPurple, ZuzuMagenta)
  PrimaryTab.Projects -> listOf(ZuzuPink, ZuzuMagenta)
  PrimaryTab.Settings -> listOf(ZuzuDeepPurple, ZuzuElectricBlue)
}

@Composable
private fun tabLabel(tab: PrimaryTab): String = when (tab) {
  PrimaryTab.Dashboard -> stringResource(R.string.nav_dashboard)
  PrimaryTab.Sources -> stringResource(R.string.nav_sources)
  PrimaryTab.Studio -> stringResource(R.string.nav_studio)
  PrimaryTab.Projects -> stringResource(R.string.nav_projects)
  PrimaryTab.Settings -> stringResource(R.string.nav_settings)
}

private fun tabIcon(tab: PrimaryTab, selected: Boolean): ImageVector = when (tab) {
  PrimaryTab.Dashboard -> if (selected) Icons.Rounded.Dashboard else Icons.Outlined.Dashboard
  PrimaryTab.Sources -> if (selected) Icons.Rounded.PermMedia else Icons.Outlined.PermMedia
  PrimaryTab.Studio -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
  PrimaryTab.Projects -> if (selected) Icons.Rounded.Folder else Icons.Outlined.Folder
  PrimaryTab.Settings -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
