package com.example.data.motion

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.MotionAnimationStyle
import com.example.model.MotionPostProject
import com.example.model.MotionTransition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws individual frames for Auto Motion Post Studio (both live interactive preview thumbnails
 * and offline MP4/PNG/JPEG rendering) using deterministic normalized progress [0f..1f].
 */
object MotionCanvasDrawer {

  fun drawMotionFrame(
    canvas: Canvas,
    width: Int,
    height: Int,
    progress: Float,
    project: MotionPostProject,
    mediaBitmap: Bitmap? = null,
    logoBitmap: Bitmap? = null,
  ) {
    val p = progress.coerceIn(0f, 1f)
    val primary = project.primaryAccentArgb.toInt()
    val secondary = project.secondaryAccentArgb.toInt()

    // 1. Animated or Gradient Background
    drawBackgroundStage(canvas, width, height, p, primary, secondary, project.animationStyle)

    // 2. Background / Attached Image or Video Keyframe with Animation Style + Transition
    val globalAlpha = computeTransitionAlpha(p, project.transition, project.animationStyle)
    val slideOffsetX = computeTransitionOffsetX(width, p, project.transition, project.animationStyle)
    val scaleFactor = computeScale(p, project.transition, project.animationStyle)
    val rotationDeg = if (project.animationStyle == MotionAnimationStyle.Rotate) {
      sin(p * PI.toFloat() * 2f) * 6f
    } else {
      0f
    }

    canvas.save()
    canvas.translate(slideOffsetX, 0f)
    if (rotationDeg != 0f) {
      canvas.rotate(rotationDeg, width / 2f, height / 2f)
    }
    if (scaleFactor != 1f) {
      canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
    }

    if (mediaBitmap != null && !mediaBitmap.isRecycled) {
      val parallaxX = if (project.animationStyle == MotionAnimationStyle.Parallax) {
        (p - 0.5f) * width * 0.08f
      } else {
        0f
      }
      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = (globalAlpha * 235).roundToInt().coerceIn(0, 255)
      }
      val src = centerCropRect(mediaBitmap.width, mediaBitmap.height, width, height)
      val dst = RectF(-parallaxX, 0f, width.toFloat() - parallaxX, height.toFloat())
      canvas.drawBitmap(mediaBitmap, src, dst, paint)

      // Dark legibility scrim for typography over photo/video
      val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f,
          0f,
          0f,
          height.toFloat(),
          intArrayOf(Color.argb(95, 10, 12, 26), Color.argb(170, 10, 12, 26)),
          null,
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
    }
    canvas.restore()

    // 3. Particle / Decorative Motion Orbs
    drawParticles(canvas, width, height, p, project.animationStyle)

    // 4. Template Category Pill Badge + Brand Logo
    drawTopHeader(canvas, width, height, p, project, logoBitmap, globalAlpha)

    // 5. Kinetic Headline & Subheadline (with TextReveal support)
    drawTypographyBlock(canvas, width, height, p, project, globalAlpha)

    // 6. Custom Stickers
    drawStickers(canvas, width, height, p, project, globalAlpha)
  }

  private fun drawBackgroundStage(
    canvas: Canvas,
    width: Int,
    height: Int,
    progress: Float,
    primary: Int,
    secondary: Int,
    style: MotionAnimationStyle,
  ) {
    val angleShift = if (style == MotionAnimationStyle.AnimatedBackground) {
      sin(progress * PI.toFloat() * 2f) * width * 0.25f
    } else {
      0f
    }
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        angleShift,
        0f,
        width.toFloat() - angleShift,
        height.toFloat(),
        intArrayOf(primary, secondary, Color.rgb(30, 27, 75)),
        floatArrayOf(0f, 0.58f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Soft ambient radial orb
    val orbX = width * (0.25f + 0.5f * cos(progress * PI.toFloat()))
    val orbY = height * (0.30f + 0.35f * sin(progress * PI.toFloat()))
    val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        orbX,
        orbY,
        max(width, height) * 0.45f,
        intArrayOf(Color.argb(100, 255, 255, 255), Color.TRANSPARENT),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(orbX, orbY, max(width, height) * 0.45f, orbPaint)
  }

  private fun computeTransitionAlpha(
    progress: Float,
    transition: MotionTransition,
    style: MotionAnimationStyle,
  ): Float {
    val fadeStyle = style == MotionAnimationStyle.FadeInOut || transition == MotionTransition.CrossFade
    if (!fadeStyle) return 1f
    return when {
      progress < 0.14f -> (progress / 0.14f).coerceIn(0.15f, 1f)
      progress > 0.88f -> ((1f - progress) / 0.12f).coerceIn(0.20f, 1f)
      else -> 1f
    }
  }

  private fun computeTransitionOffsetX(
    width: Int,
    progress: Float,
    transition: MotionTransition,
    style: MotionAnimationStyle,
  ): Float {
    if (style != MotionAnimationStyle.Slide && transition != MotionTransition.SlideSweep) return 0f
    return if (progress < 0.2f) {
      width * (1f - progress / 0.2f) * 0.35f
    } else {
      0f
    }
  }

  private fun computeScale(
    progress: Float,
    transition: MotionTransition,
    style: MotionAnimationStyle,
  ): Float {
    return when {
      style == MotionAnimationStyle.ZoomInOut -> 1.0f + 0.10f * sin(progress * PI.toFloat())
      transition == MotionTransition.ScalePop && progress < 0.2f -> 0.88f + (0.12f * (progress / 0.2f))
      else -> 1f
    }
  }

  private fun drawParticles(
    canvas: Canvas,
    width: Int,
    height: Int,
    progress: Float,
    style: MotionAnimationStyle,
  ) {
    val count = if (style == MotionAnimationStyle.ParticleEffects) 16 else 6
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    for (i in 0 until count) {
      val seed = i * 1.37f
      val x = ((sin(seed + progress * 2.4f) * 0.42f) + 0.5f) * width
      val y = (((i / count.toFloat()) - progress * 0.6f + 1f) % 1f) * height
      val radius = (width * (0.008f + (i % 3) * 0.006f))
      paint.color = Color.argb(if (style == MotionAnimationStyle.ParticleEffects) 165 else 70, 255, 255, 255)
      canvas.drawCircle(x, y, radius, paint)
    }
  }

  private fun drawTopHeader(
    canvas: Canvas,
    width: Int,
    height: Int,
    progress: Float,
    project: MotionPostProject,
    logoBitmap: Bitmap?,
    alpha: Float,
  ) {
    val scale = width / 360f
    val pad = 18f * scale
    val alphaInt = (alpha * 255).roundToInt().coerceIn(0, 255)

    // Template pill top-left
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb((alphaInt * 0.45f).roundToInt(), 255, 255, 255)
    }
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      this.alpha = alphaInt
      textSize = 11f * scale
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val label = "${project.templateCategory.displayLabel.uppercase()} • ${project.platformPreset.platform.displayName}"
    val pillW = min(width * 0.68f, textPaint.measureText(label) + 20f * scale)
    val pillH = 26f * scale
    canvas.drawRoundRect(RectF(pad, pad, pad + pillW, pad + pillH), pillH / 2f, pillH / 2f, pillPaint)
    canvas.drawText(label, pad + 10f * scale, pad + 17f * scale, textPaint)

    // Logo badge top-right (supports Rotate / Scale motion)
    val logoSize = 40f * scale
    val cx = width - pad - logoSize / 2f
    val cy = pad + logoSize / 2f
    canvas.save()
    if (project.animationStyle == MotionAnimationStyle.Rotate) {
      canvas.rotate(progress * 360f, cx, cy)
    }
    if (logoBitmap != null && !logoBitmap.isRecycled) {
      val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        this.alpha = alphaInt
      }
      val src = Rect(0, 0, logoBitmap.width, logoBitmap.height)
      val dst = RectF(cx - logoSize / 2f, cy - logoSize / 2f, cx + logoSize / 2f, cy + logoSize / 2f)
      canvas.drawBitmap(logoBitmap, src, dst, logoPaint)
    } else if (project.logoLabel.isNotBlank()) {
      val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb((alphaInt * 0.85f).roundToInt(), 15, 23, 42)
      }
      canvas.drawCircle(cx, cy, logoSize / 2f, badgePaint)
      val logoTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        this.alpha = alphaInt
        textSize = 10f * scale
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      canvas.drawText(project.logoLabel.take(6), cx, cy + 4f * scale, logoTextPaint)
    }
    canvas.restore()
  }

  private fun drawTypographyBlock(
    canvas: Canvas,
    width: Int,
    height: Int,
    progress: Float,
    project: MotionPostProject,
    alpha: Float,
  ) {
    val scale = width / 360f
    val maxW = (width * 0.84f).roundToInt().coerceAtLeast(100)
    val rawHeadline = project.headlineText.ifBlank { project.title }
    val headlineToDraw = if (project.animationStyle == MotionAnimationStyle.TextReveal) {
      val revealFrac = (progress * 1.8f).coerceIn(0.15f, 1f)
      val charCount = (rawHeadline.length * revealFrac).roundToInt().coerceIn(1, rawHeadline.length)
      rawHeadline.take(charCount)
    } else {
      rawHeadline
    }

    val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = project.textColorArgb.toInt()
      this.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
      textSize = (project.textFontSizeSp * scale).coerceAtLeast(18f)
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      setShadowLayer(4f * scale, 0f, 2f * scale, Color.argb(150, 0, 0, 0))
    }
    val headLayout = StaticLayout.Builder.obtain(headlineToDraw, 0, headlineToDraw.length, headPaint, maxW)
      .setAlignment(Layout.Alignment.ALIGN_CENTER)
      .build()

    val subText = project.subheadlineText.trim()
    val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      this.alpha = (alpha * 225).roundToInt().coerceIn(0, 255)
      textSize = (14f * scale).coerceAtLeast(12f)
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      setShadowLayer(2f * scale, 0f, 1f * scale, Color.argb(140, 0, 0, 0))
    }
    val subLayout = if (subText.isNotEmpty()) {
      StaticLayout.Builder.obtain(subText, 0, subText.length, subPaint, maxW)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .build()
    } else {
      null
    }

    val totalTextH = headLayout.height + (subLayout?.height?.plus(12f * scale) ?: 0f)
    val yMotionShift = if (project.animationStyle == MotionAnimationStyle.TextReveal) {
      (1f - min(1f, progress * 2.5f)) * 18f * scale
    } else {
      0f
    }
    val startY = (height - totalTextH) / 2f + yMotionShift

    canvas.save()
    canvas.translate((width - maxW) / 2f, startY)
    headLayout.draw(canvas)
    if (subLayout != null) {
      canvas.translate(0f, headLayout.height + 12f * scale)
      subLayout.draw(canvas)
    }
    canvas.restore()
  }

  private fun drawStickers(
    canvas: Canvas,
    width: Int,
    height: Int,
    progress: Float,
    project: MotionPostProject,
    alpha: Float,
  ) {
    if (project.stickers.isEmpty()) return
    val scale = width / 360f
    val alphaInt = (alpha * 255).roundToInt().coerceIn(0, 255)
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb((alphaInt * 0.85f).roundToInt(), 255, 255, 255)
    }
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(15, 23, 42)
      this.alpha = alphaInt
      textSize = 11f * scale
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bounce = sin(progress * PI.toFloat() * 3f) * 3f * scale

    project.stickers.forEachIndexed { idx, sticker ->
      val text = "${sticker.badgeSymbol} ${sticker.label}".trim()
      val textW = textPaint.measureText(text)
      val pillW = textW + 16f * scale
      val pillH = 24f * scale
      val cx = (sticker.xFraction * width).coerceIn(pillW / 2f + 8f, width - pillW / 2f - 8f)
      val cy = ((sticker.yFraction + idx * 0.08f).coerceAtMost(0.88f) * height) + bounce
      canvas.drawRoundRect(
        RectF(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f),
        pillH / 2f,
        pillH / 2f,
        pillPaint,
      )
      canvas.drawText(text, cx - textW / 2f, cy + 4f * scale, textPaint)
    }
  }

  private fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
    val srcRatio = srcW.toFloat() / srcH.toFloat()
    val dstRatio = dstW.toFloat() / dstH.toFloat()
    return if (srcRatio > dstRatio) {
      val cropW = (srcH * dstRatio).roundToInt()
      val left = (srcW - cropW) / 2
      Rect(left, 0, left + cropW, srcH)
    } else {
      val cropH = (srcW / dstRatio).roundToInt()
      val top = (srcH - cropH) / 2
      Rect(0, top, srcW, top + cropH)
    }
  }
}
