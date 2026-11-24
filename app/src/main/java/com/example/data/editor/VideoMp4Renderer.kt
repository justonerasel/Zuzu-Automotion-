package com.example.data.editor

import com.example.model.VideoEditorDraft
import java.io.File

sealed interface RendererCapabilityStatus {
  data class Available(
    val engineName: String,
    val description: String,
  ) : RendererCapabilityStatus

  data class Unavailable(
    val engineName: String,
    val reason: String,
  ) : RendererCapabilityStatus
}

data class RenderProgress(
  val fraction: Float,
  val stageLabel: String,
)

sealed interface RenderOutcome {
  data class Success(
    val outputFile: File,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
  ) : RenderOutcome

  data object Cancelled : RenderOutcome

  data class Error(
    val message: String,
  ) : RenderOutcome
}

interface VideoMp4Renderer {
  fun checkCapability(): RendererCapabilityStatus

  suspend fun renderToMp4(
    draft: VideoEditorDraft,
    outputFile: File,
    onProgress: (RenderProgress) -> Unit,
  ): RenderOutcome
}
