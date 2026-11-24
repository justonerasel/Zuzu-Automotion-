package com.example.data.analytics

import com.example.data.cloud.CloudAuthSession
import com.example.model.AnalyticsDateRange
import com.example.model.AnalyticsMetricAvailability
import com.example.model.CloudOperationResult
import com.example.model.EngagementMetricsSnapshot
import com.example.model.SocialPlatform
import com.example.security.SecuritySanitizer
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Non-secret configuration for the HTTPS analytics proxy backend.
 * Official platform analytics tokens (YouTube Analytics API, Meta Insights API,
 * TikTok Video Query API) never leave the backend vault.
 */
data class AnalyticsBackendConfig(
  val backendBaseUrl: String = "",
  val youtubeAnalyticsEnabled: Boolean = false,
  val facebookInsightsEnabled: Boolean = false,
  val tiktokAnalyticsEnabled: Boolean = false,
) {
  val hasSecureEndpoint: Boolean
    get() = SecuritySanitizer.isSafeHttpsUrl(backendBaseUrl)

  fun isEnabledFor(platform: SocialPlatform): Boolean = hasSecureEndpoint && when (platform) {
    SocialPlatform.YouTube -> youtubeAnalyticsEnabled
    SocialPlatform.Facebook -> facebookInsightsEnabled
    SocialPlatform.TikTok -> tiktokAnalyticsEnabled
  }
}

@JsonClass(generateAdapter = true)
data class RemotePlatformMetricsDto(
  val platform: String,
  val views: Long? = null,
  val likes: Long? = null,
  val comments: Long? = null,
  val shares: Long? = null,
  val engagementRatePercent: Double? = null,
  val dataAvailable: Boolean = true,
  val note: String = "",
)

@JsonClass(generateAdapter = true)
data class RemoteContentMetricsDto(
  val jobId: String,
  val platform: String,
  val remotePostId: String? = null,
  val views: Long? = null,
  val likes: Long? = null,
  val comments: Long? = null,
  val shares: Long? = null,
  val engagementRatePercent: Double? = null,
  val dataAvailable: Boolean = true,
  val note: String = "",
)

@JsonClass(generateAdapter = true)
data class RemoteAnalyticsResponse(
  val fetchedAtMillis: Long = System.currentTimeMillis(),
  val platformMetrics: List<RemotePlatformMetricsDto> = emptyList(),
  val contentMetrics: List<RemoteContentMetricsDto> = emptyList(),
)

data class RemoteAnalyticsSnapshot(
  val fetchedAtMillis: Long = 0L,
  val platformMetrics: Map<SocialPlatform, EngagementMetricsSnapshot> = emptyMap(),
  val contentMetricsByJobId: Map<String, EngagementMetricsSnapshot> = emptyMap(),
)

interface AnalyticsRetrofitApi {
  @GET("v1/analytics/overview")
  suspend fun fetchAnalyticsOverview(
    @Header("Authorization") bearerJwt: String,
    @Query("days") daysWindow: Int?,
  ): Response<RemoteAnalyticsResponse>
}

interface AnalyticsBackendGateway {
  val config: AnalyticsBackendConfig
  val cachedSnapshot: StateFlow<RemoteAnalyticsSnapshot>

  suspend fun syncPlatformAnalytics(
    session: CloudAuthSession?,
    connectedPlatforms: Set<SocialPlatform>,
    dateRange: AnalyticsDateRange,
  ): CloudOperationResult<RemoteAnalyticsSnapshot>
}

class DefaultAnalyticsBackendGateway(
  override val config: AnalyticsBackendConfig = AnalyticsBackendConfig(),
  private val apiProvider: ((String) -> AnalyticsRetrofitApi)? = null,
) : AnalyticsBackendGateway {

  private val _cachedSnapshot = MutableStateFlow(RemoteAnalyticsSnapshot())
  override val cachedSnapshot: StateFlow<RemoteAnalyticsSnapshot> = _cachedSnapshot.asStateFlow()

  override suspend fun syncPlatformAnalytics(
    session: CloudAuthSession?,
    connectedPlatforms: Set<SocialPlatform>,
    dateRange: AnalyticsDateRange,
  ): CloudOperationResult<RemoteAnalyticsSnapshot> {
    if (connectedPlatforms.isEmpty()) {
      return CloudOperationResult.SetupRequired(
        title = "No Social Accounts Connected",
        reason = "Connect YouTube, Facebook Pages, or TikTok in Social Accounts to retrieve official analytics.",
        missingItems = listOf("Connect at least one authorized social account."),
      )
    }

    val enabledPlatforms = connectedPlatforms.filter { config.isEnabledFor(it) }
    if (!config.hasSecureEndpoint || enabledPlatforms.isEmpty()) {
      return CloudOperationResult.SetupRequired(
        title = "Analytics API Backend Setup Required",
        reason = "Official YouTube Analytics, Meta Graph Insights, or TikTok Video Query API credentials are not configured on the HTTPS backend. Showing verified local pipeline counts only.",
        missingItems = buildList {
          if (!config.hasSecureEndpoint) add("Deploy HTTPS analytics backend endpoint.")
          if (connectedPlatforms.contains(SocialPlatform.YouTube) && !config.youtubeAnalyticsEnabled) {
            add("Enable YouTube Analytics API + yt-analytics.readonly scope on backend.")
          }
          if (connectedPlatforms.contains(SocialPlatform.Facebook) && !config.facebookInsightsEnabled) {
            add("Enable Meta Graph Page Insights + read_insights permission on backend.")
          }
          if (connectedPlatforms.contains(SocialPlatform.TikTok) && !config.tiktokAnalyticsEnabled) {
            add("Enable TikTok Video Query API + video.list scope on backend.")
          }
        },
      )
    }

    if (session == null || !session.isAuthenticated) {
      return CloudOperationResult.SetupRequired(
        title = "Creator Authentication Required",
        reason = "Sign in with your authenticated creator session to query backend analytics.",
        missingItems = listOf("Sign in via Cloud Backend Authentication."),
      )
    }

    val api = apiProvider?.invoke(config.backendBaseUrl)
      ?: return CloudOperationResult.SetupRequired(
        title = "Analytics Network Client Not Bound",
        reason = "Configure the Retrofit analytics client for the deployed HTTPS backend.",
        missingItems = listOf("Bind AnalyticsRetrofitApi implementation."),
      )

    return runCatching {
      val resp = api.fetchAnalyticsOverview(
        bearerJwt = "Bearer ${session.idToken}",
        daysWindow = dateRange.daysWindow,
      )
      val body = resp.body()
      if (!resp.isSuccessful || body == null) {
        val codeMessage = when (resp.code()) {
          401 -> "Analytics session expired. Re-authenticate your account."
          403 -> "Platform rejected analytics request due to missing read/insights OAuth permissions."
          429 -> "Platform analytics API rate limit reached. Try refreshing shortly."
          else -> "Analytics backend returned HTTP ${resp.code()}."
        }
        return@runCatching CloudOperationResult.Failure(SecuritySanitizer.redactSensitiveTokens(codeMessage))
      }

      val platformMap = body.platformMetrics.mapNotNull { dto ->
        val platform = SocialPlatform.entries.firstOrNull { it.id.equals(dto.platform, ignoreCase = true) }
          ?: return@mapNotNull null
        platform to EngagementMetricsSnapshot(
          views = dto.views,
          likes = dto.likes,
          comments = dto.comments,
          shares = dto.shares,
          engagementRatePercent = dto.engagementRatePercent,
          availability = if (dto.dataAvailable) {
            AnalyticsMetricAvailability.Available
          } else {
            AnalyticsMetricAvailability.DataUnavailable
          },
          availabilityNote = SecuritySanitizer.redactSensitiveTokens(dto.note).ifBlank {
            if (dto.dataAvailable) {
              "Retrieved from official ${platform.displayName} API."
            } else {
              "${platform.displayName} reported no analytics data for this range."
            }
          },
        )
      }.toMap()

      val contentMap = body.contentMetrics.associate { dto ->
        dto.jobId to EngagementMetricsSnapshot(
          views = dto.views,
          likes = dto.likes,
          comments = dto.comments,
          shares = dto.shares,
          engagementRatePercent = dto.engagementRatePercent,
          availability = if (dto.dataAvailable) {
            AnalyticsMetricAvailability.Available
          } else {
            AnalyticsMetricAvailability.DataUnavailable
          },
          availabilityNote = SecuritySanitizer.redactSensitiveTokens(dto.note).ifBlank {
            if (dto.dataAvailable) "Official API post metrics" else "Post telemetry not yet returned by API"
          },
        )
      }

      val snapshot = RemoteAnalyticsSnapshot(
        fetchedAtMillis = body.fetchedAtMillis,
        platformMetrics = platformMap,
        contentMetricsByJobId = contentMap,
      )
      _cachedSnapshot.value = snapshot
      CloudOperationResult.Success(snapshot)
    }.getOrElse { err ->
      CloudOperationResult.Failure(
        SecuritySanitizer.redactSensitiveTokens(err.message)
          .ifBlank { "Unable to reach HTTPS analytics backend." },
      )
    }
  }
}
