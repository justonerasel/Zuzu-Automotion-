package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.StudioModuleSpec
import com.example.model.StudioModuleType
import com.example.ui.components.GlassPill
import com.example.ui.components.ZuzuGlassCard
import com.example.ui.components.ZuzuGradientButton
import com.example.ui.components.ZuzuGradientCard
import com.example.ui.components.ZuzuSecondaryTopBar
import com.example.ui.theme.ZuzuDeepPurple
import com.example.ui.theme.ZuzuElectricBlue
import com.example.ui.theme.ZuzuInkPrimary
import com.example.ui.theme.ZuzuInkSecondary
import com.example.ui.theme.ZuzuPinkOrangeGradient
import com.example.ui.theme.ZuzuPurple
import com.example.ui.theme.ZuzuWhite

@Composable
fun StudioIntroScreen(
  module: StudioModuleSpec,
  glowEnabled: Boolean,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val isVideo = module.type == StudioModuleType.AiVideoFactory
  val gradientColors = if (isVideo) {
    listOf(ZuzuDeepPurple, ZuzuPurple, ZuzuElectricBlue)
  } else {
    ZuzuPinkOrangeGradient
  }
  val accentColor = gradientColors.first()
  val icon = if (isVideo) Icons.Rounded.MovieCreation else Icons.Rounded.Animation

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      item {
        ZuzuSecondaryTopBar(
          title = module.title,
          onBack = onBack,
          backButtonTag = "intro_back_button",
          badgeText = stringResource(R.string.phase_badge),
          badgeTint = accentColor,
        )
      }

      item {
        ZuzuGradientCard(
          gradientColors = gradientColors,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = ZuzuWhite.copy(alpha = 0.22f),
              border = BorderStroke(1.dp, ZuzuWhite.copy(alpha = 0.42f)),
              modifier = Modifier.size(62.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = icon,
                  contentDescription = module.title,
                  tint = ZuzuWhite,
                  modifier = Modifier.size(32.dp),
                )
              }
            }
            GlassPill(label = stringResource(R.string.intro_coming_soon_badge))
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = module.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = ZuzuWhite,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = module.overviewDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = ZuzuWhite.copy(alpha = 0.92f),
          )
        }
      }

      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = gradientColors,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.intro_planned_workflow),
            style = MaterialTheme.typography.titleMedium,
            color = ZuzuInkPrimary,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            module.plannedSteps.forEachIndexed { index, step ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors)),
                ) {
                  Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZuzuWhite,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = step,
                  style = MaterialTheme.typography.bodyMedium,
                  color = ZuzuInkPrimary,
                  modifier = Modifier.weight(1f),
                )
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = accentColor.copy(alpha = 0.72f),
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }

      item {
        ZuzuGlassCard(
          glowEnabled = glowEnabled,
          accentColors = gradientColors,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.intro_phase_note),
            style = MaterialTheme.typography.bodySmall,
            color = ZuzuInkSecondary,
          )
          Spacer(modifier = Modifier.height(14.dp))
          ZuzuGradientButton(
            text = stringResource(R.string.action_back),
            onClick = onBack,
            gradientColors = gradientColors,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("intro_return_button"),
          )
        }
      }
    }
  }
}
