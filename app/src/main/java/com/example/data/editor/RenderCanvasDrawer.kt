package com.example.data.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import com.example.model.EditorTimelineClip
import com.example.model.EditorTransition
import com.example.model.EditorVisualEffect
import com.example.model.OverlayPosition
import com.example.model.SubtitleBackgroundStyle
import com.example.model.SubtitleCue
import com.example.model.SubtitlePosition
import com.example.model.TextOverlay
import com.example.model.VideoEditorDraft
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders individual timeline frames onto an Android Canvas, combining visual clips,
 * transitions, color/vignette effects, text overlays, and styled Bangla/English subtitles.
 */
object RenderCanvasDrawer {

  private const val TRANSITION_WINDOW_MS = 550L

  fun drawFrame(
    canvas: Canvas,
    width: Int,
    height: Int,
    timeMs: Long,
    draft: VideoEditorDraft,
    bitmapProvider: (clip: EditorTimelineClip, localTimeMs: Long) -> Bitmap?,
  ) {
    canvas.drawColor(Color.BLACK)
    if (draft.clips.isEmpty()) return

    var cursor = 0L
    var activeIndex = draft.clips.lastIndex
    var activeStartMs = 0L

    for (i in draft.clips.indices) {
      val dur = draft.clips[i].durationMs.coerceAtLeast(500L)
      if (timeMs < cursor + dur || i == draft.clips.lastIndex) {
        activeIndex = i
        activeStartMs = cursor
        break
      }
      cursor += dur
    }

    val activeClip = draft.clips[activeIndex]
    val localTimeMs = (timeMs - activeStartMs).coerceAtLeast(0L)
    val prevClip = if (activeIndex > 0) draft.clips[activeIndex - 1] else null

    val transitionDuration = minOf(TRANSITION_WINDOW_MS, activeClip.durationMs / 3L)
    if (prevClip != null &&
      activeClip.transition != EditorTransition.None &&
      transitionDuration > 0L &&
      localTimeMs < transitionDuration
    ) {
      val progress = (localTimeMs.toFloat() / transitionDuration.toFloat()).coerceIn(0f, 1f)
      val prevBmp = bitmapProvider(prevClip, prevClip.durationMs)
      val curBmp = bitmapProvider(activeClip, localTimeMs)

      when (activeClip.transition) {
        EditorTransition.CrossFade -> {
          drawClipVisual(canvas, width, height, prevClip, prevBmp, 1f, 0f, 1f)
          drawClipVisual(canvas, width, height, activeClip, curBmp, progress, 0f, 1f)
        }
        EditorTransition.SlideLeft -> {
          val offsetPrev = -width * progress
          val offsetCur = width * (1f - progress)
          drawClipVisual(canvas, width, height, prevClip, prevBmp, 1f, offsetPrev, 1f)
          drawClipVisual(canvas, width, height, activeClip, curBmp, 1f, offsetCur, 1f)
        }
        EditorTransition.ZoomIn -> {
          drawClipVisual(canvas, width, height, prevClip, prevBmp, 1f - progress * 0.4f, 0f, 1f)
          val scale = 0.85f + (0.15f * progress)
          drawClipVisual(canvas, width, height, activeClip, curBmp, progress, 0f, scale)
        }
        EditorTransition.None -> {
          drawClipVisual(canvas, width, height, activeClip, curBmp, 1f, 0f, 1f)
        }
      }
    } else {
      val bmp = bitmapProvider(activeClip, localTimeMs)
      drawClipVisual(canvas, width, height, activeClip, bmp, 1f, 0f, 1f)
    }

    draft.textOverlays.forEach { overlay ->
      if (timeMs in overlay.startMs..overlay.endMs && overlay.text.isNotBlank()) {
        drawTextOverlay(canvas, width, height, overlay)
      }
    }

    draft.subtitles.forEach { cue ->
      if (timeMs in cue.startMs..cue.endMs && cue.text.isNotBlank()) {
        drawSubtitleCue(canvas, width, height, cue)
      }
    }
  }

  private fun drawClipVisual(
    canvas: Canvas,
    width: Int,
    height: Int,
    clip: EditorTimelineClip,
    bitmap: Bitmap?,
    alpha: Float,
    translateX: Float,
    scale: Float,
  ) {
    canvas.save()
    canvas.translate(translateX, 0f)
    if (scale != 1f) {
      canvas.scale(scale, scale, width / 2f, height / 2f)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      this.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
      colorFilter = createEffectColorFilter(clip.visualEffect)
    }

    if (bitmap != null && !bitmap.isRecycled) {
      val dest = RectF(0f, 0f, width.toFloat(), height.toFloat())
      val src = centerCropRect(bitmap.width, bitmap.height, width, height)
      canvas.drawBitmap(bitmap, src, dest, paint)
    } else {
      drawStyledTitleCard(canvas, width, height, clip, paint.alpha)
    }

    if (clip.visualEffect == EditorVisualEffect.CinematicVignette) {
      val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
          width / 2f,
          height / 2f,
          max(width, height) * 0.72f,
          intArrayOf(Color.TRANSPARENT, Color.argb(175, 8, 8, 18)),
          floatArrayOf(0.45f, 1.0f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    canvas.restore()
  }

  private fun drawStyledTitleCard(
    canvas: Canvas,
    width: Int,
    height: Int,
    clip: EditorTimelineClip,
    alphaInt: Int,
  ) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      alpha = alphaInt
      shader = LinearGradient(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        intArrayOf(
          Color.rgb(91, 33, 182),
          Color.rgb(59, 130, 246),
          Color.rgb(236, 72, 153),
        ),
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      alpha = alphaInt
      textSize = width * 0.062f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val maxTextW = (width * 0.82f).roundToInt().coerceAtLeast(100)
    val label = clip.title
    val layout = StaticLayout.Builder.obtain(label, 0, label.length, titlePaint, maxTextW)
      .setAlignment(Layout.Alignment.ALIGN_CENTER)
      .build()
    canvas.save()
    canvas.translate((width - maxTextW) / 2f, (height - layout.height) / 2f)
    layout.draw(canvas)
    canvas.restore()
  }

  private fun drawTextOverlay(
    canvas: Canvas,
    width: Int,
    height: Int,
    overlay: TextOverlay,
  ) {
    val scaleFactor = width / 360f
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = overlay.colorArgb.toInt()
      textSize = (overlay.fontSizeSp * scaleFactor).coerceAtLeast(18f)
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      setShadowLayer(4f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(170, 0, 0, 0))
    }
    val maxW = (width * 0.86f).roundToInt().coerceAtLeast(100)
    val layout = StaticLayout.Builder.obtain(overlay.text, 0, overlay.text.length, paint, maxW)
      .setAlignment(Layout.Alignment.ALIGN_CENTER)
      .build()
    val y = when (overlay.position) {
      OverlayPosition.Top -> height * 0.10f
      OverlayPosition.Center -> (height - layout.height) / 2f
      OverlayPosition.Bottom -> height * 0.78f - layout.height
    }
    canvas.save()
    canvas.translate((width - maxW) / 2f, y)
    layout.draw(canvas)
    canvas.restore()
  }

  private fun drawSubtitleCue(
    canvas: Canvas,
    width: Int,
    height: Int,
    cue: SubtitleCue,
  ) {
    val scaleFactor = width / 360f
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = cue.textColorArgb.toInt()
      textSize = (cue.fontSizeSp * scaleFactor).coerceAtLeast(16f)
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      setShadowLayer(2f * scaleFactor, 0f, 1.5f * scaleFactor, Color.argb(140, 0, 0, 0))
    }
    val maxW = (width * 0.84f).roundToInt().coerceAtLeast(100)
    val layout = StaticLayout.Builder.obtain(cue.text, 0, cue.text.length, textPaint, maxW)
      .setAlignment(Layout.Alignment.ALIGN_CENTER)
      .build()

    val boxPadX = 14f * scaleFactor
    val boxPadY = 8f * scaleFactor
    val y = when (cue.position) {
      SubtitlePosition.Top -> height * 0.08f
      SubtitlePosition.Middle -> (height - layout.height) / 2f
      SubtitlePosition.Bottom -> height * 0.88f - layout.height
    }

    val bgColor = when (cue.backgroundStyle) {
      SubtitleBackgroundStyle.None -> null
      SubtitleBackgroundStyle.DarkTranslucent -> Color.argb(185, 16, 16, 26)
      SubtitleBackgroundStyle.PurpleGlass -> Color.argb(200, 109, 40, 217)
      SubtitleBackgroundStyle.SolidBlack -> Color.BLACK
      SubtitleBackgroundStyle.SolidWhite -> Color.WHITE
    }

    if (bgColor != null) {
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
      val rect = RectF(
        (width - maxW) / 2f - boxPadX,
        y - boxPadY,
        (width + maxW) / 2f + boxPadX,
        y + layout.height + boxPadY,
      )
      canvas.drawRoundRect(rect, 12f * scaleFactor, 12f * scaleFactor, bgPaint)
    }

    canvas.save()
    canvas.translate((width - maxW) / 2f, y)
    layout.draw(canvas)
    canvas.restore()
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

  private fun createEffectColorFilter(effect: EditorVisualEffect): ColorMatrixColorFilter? {
    val matrix = ColorMatrix()
    when (effect) {
      EditorVisualEffect.None, EditorVisualEffect.CinematicVignette -> return null
      EditorVisualEffect.WarmGlow -> {
        matrix.setScale(1.08f, 1.02f, 0.90f, 1f)
      }
      EditorVisualEffect.HighContrast -> {
        val c = 1.28f
        val t = (-0.5f * c + 0.5f) * 255f
        matrix.set(
          floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f,
          ),
        )
      }
      EditorVisualEffect.SoftMono -> {
        matrix.setSaturation(0.18f)
      }
    }
    return ColorMatrixColorFilter(matrix)
  }
}
