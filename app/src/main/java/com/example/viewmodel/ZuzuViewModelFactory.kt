package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.FileProvider
import com.example.data.ai.GeminiVideoFactoryAiService
import com.example.data.ai.ModularVisualImageProvider
import com.example.data.ai.ModularVisualVideoProvider
import com.example.data.ai.ModularVoiceTtsProvider
import com.example.data.ai.VideoFactoryAiService
import com.example.data.analytics.AnalyticsBackendConfig
import com.example.data.analytics.AnalyticsBackendGateway
import com.example.data.analytics.DefaultAnalyticsBackendGateway
import com.example.data.cloud.CloudBackendConfig
import com.example.data.cloud.CloudBackendGateway
import com.example.data.cloud.DefaultCloudBackendGateway
import com.example.data.editor.AndroidMediaCodecMp4Renderer
import com.example.data.editor.RoomVideoEditorRepository
import com.example.data.editor.VideoEditorRepository
import com.example.data.factory.RoomStudioAssetRepository
import com.example.data.factory.RoomVideoFactoryRepository
import com.example.data.factory.StudioAssetRepository
import com.example.data.factory.VideoFactoryRepository
import com.example.data.motion.AndroidMotionPostExporter
import com.example.data.oauth.SocialAccountsRepository
import com.example.data.phase78.Phase78Repository
import com.example.data.phase78.RoomPhase78Repository
import com.example.data.publishing.DefaultPublishingBackendGateway
import com.example.data.publishing.PublishingBackendConfig
import com.example.data.publishing.PublishingBackendGateway
import com.example.data.publishing.PublishingRepository
import com.example.data.publishing.RoomPublishingRepository
import com.example.data.source.ContentSourceRepository
import com.example.data.source.RoomContentSourceRepository
import com.example.data.source.ZuzuDatabase
import com.example.model.ImageProviderType
import com.example.model.VideoClipProviderType
import com.example.model.VoiceProviderType
import java.io.File

class ZuzuViewModelFactory(
  context: Context,
) : ViewModelProvider.Factory {

  private val appContext = context.applicationContext

  private val database: ZuzuDatabase by lazy {
    ZuzuDatabase.getInstance(appContext)
  }

  private val contentSourceRepository: ContentSourceRepository by lazy {
    RoomContentSourceRepository(database.contentSourceDao())
  }

  private val videoFactoryRepository: VideoFactoryRepository by lazy {
    RoomVideoFactoryRepository(database.videoFactoryDao())
  }

  private val studioAssetRepository: StudioAssetRepository by lazy {
    RoomStudioAssetRepository(database.studioAssetDao())
  }

  private val videoEditorRepository: VideoEditorRepository by lazy {
    RoomVideoEditorRepository(database.videoEditorDao())
  }

  private val mp4Renderer: AndroidMediaCodecMp4Renderer by lazy {
    AndroidMediaCodecMp4Renderer(appContext)
  }

  private val videoFactoryAiService: VideoFactoryAiService by lazy {
    GeminiVideoFactoryAiService()
  }

  private val socialAccountsRepository: SocialAccountsRepository by lazy {
    SocialAccountsRepository()
  }

  private val phase78Repository: Phase78Repository by lazy {
    RoomPhase78Repository(database.phase78Dao())
  }

  private val motionExporter: AndroidMotionPostExporter by lazy {
    AndroidMotionPostExporter(appContext)
  }

  private val cloudBackendGateway: CloudBackendGateway by lazy {
    DefaultCloudBackendGateway(config = CloudBackendConfig())
  }

  private val publishingRepository: PublishingRepository by lazy {
    RoomPublishingRepository(database.publishingDao())
  }

  private val publishingBackendGateway: PublishingBackendGateway by lazy {
    DefaultPublishingBackendGateway(config = PublishingBackendConfig())
  }

  private val analyticsBackendGateway: AnalyticsBackendGateway by lazy {
    DefaultAnalyticsBackendGateway(config = AnalyticsBackendConfig())
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return when {
      modelClass.isAssignableFrom(ZuzuAppViewModel::class.java) -> {
        ZuzuAppViewModel(
          socialAccountsRepository = socialAccountsRepository,
          contentSourceRepository = contentSourceRepository,
          videoFactoryRepository = videoFactoryRepository,
          videoEditorRepository = videoEditorRepository,
          phase78Repository = phase78Repository,
        ) as T
      }
      modelClass.isAssignableFrom(ContentSourceViewModel::class.java) -> {
        ContentSourceViewModel(repository = contentSourceRepository) as T
      }
      modelClass.isAssignableFrom(VideoFactoryViewModel::class.java) -> {
        VideoFactoryViewModel(
          repository = videoFactoryRepository,
          aiService = videoFactoryAiService,
        ) as T
      }
      modelClass.isAssignableFrom(VoiceVisualViewModel::class.java) -> {
        VoiceVisualViewModel(
          videoFactoryRepository = videoFactoryRepository,
          studioAssetRepository = studioAssetRepository,
          contentSourceRepository = contentSourceRepository,
          voiceProviders = mapOf(
            VoiceProviderType.GeminiTts to ModularVoiceTtsProvider(VoiceProviderType.GeminiTts),
            VoiceProviderType.ElevenLabs to ModularVoiceTtsProvider(VoiceProviderType.ElevenLabs),
          ),
          imageProviders = mapOf(
            ImageProviderType.GeminiImagen to ModularVisualImageProvider(ImageProviderType.GeminiImagen),
            ImageProviderType.ExternalImageApi to ModularVisualImageProvider(ImageProviderType.ExternalImageApi),
          ),
          videoProviders = mapOf(
            VideoClipProviderType.GoogleVeo to ModularVisualVideoProvider(VideoClipProviderType.GoogleVeo),
            VideoClipProviderType.ExternalVideoApi to ModularVisualVideoProvider(VideoClipProviderType.ExternalVideoApi),
          ),
        ) as T
      }
      modelClass.isAssignableFrom(VideoEditorViewModel::class.java) -> {
        VideoEditorViewModel(
          editorRepository = videoEditorRepository,
          contentSourceRepository = contentSourceRepository,
          mp4Renderer = mp4Renderer,
          exportsDirectoryProvider = { File(appContext.filesDir, "zuzu_exports") },
          shareUriResolver = { file ->
            runCatching {
              FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
              ).toString()
            }.getOrElse {
              android.net.Uri.fromFile(file).toString()
            }
          },
        ) as T
      }
      modelClass.isAssignableFrom(MotionStudioViewModel::class.java) -> {
        MotionStudioViewModel(
          repository = phase78Repository,
          contentSourceRepository = contentSourceRepository,
          exporter = motionExporter,
          exportsDirectoryProvider = { File(appContext.filesDir, "zuzu_exports") },
          shareUriResolver = { file ->
            runCatching {
              FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
              ).toString()
            }.getOrElse {
              android.net.Uri.fromFile(file).toString()
            }
          },
        ) as T
      }
      modelClass.isAssignableFrom(ApprovalSchedulerViewModel::class.java) -> {
        ApprovalSchedulerViewModel(
          repository = phase78Repository,
          contentSourceRepository = contentSourceRepository,
          cloudGateway = cloudBackendGateway,
        ) as T
      }
      modelClass.isAssignableFrom(PublishingCenterViewModel::class.java) -> {
        PublishingCenterViewModel(
          phase78Repository = phase78Repository,
          socialAccountsRepository = socialAccountsRepository,
          publishingRepository = publishingRepository,
          publishingBackend = publishingBackendGateway,
          cloudGateway = cloudBackendGateway,
        ) as T
      }
      modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
        AnalyticsViewModel(
          publishingRepository = publishingRepository,
          phase78Repository = phase78Repository,
          videoEditorRepository = videoEditorRepository,
          socialAccountsRepository = socialAccountsRepository,
          analyticsBackend = analyticsBackendGateway,
          cloudGateway = cloudBackendGateway,
        ) as T
      }
      else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
