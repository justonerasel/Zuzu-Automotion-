package com.example.model

import java.util.Locale

enum class AnalyticsDateRange(
  val displayLabel: String,
  val daysWindow: Int?,
) {
  Last7Days("7 Days", 7),
  Last30Days("30 Days", 30),
  Last90Days("90 Days", 90),
  AllTime("All Time", null),
}

enum class AnalyticsPlatformFilter(val displayLabel: String) {
  All("All Platforms"),
  YouTube("YouTube"),
  Facebook("Facebook"),
  TikTok("TikTok");

  fun matches(platform: SocialPlatform?): Boolean = when (this) {
    All -> true
    YouTube -> platform == SocialPlatform.YouTube
    Facebook -> platform == SocialPlatform.Facebook
    TikTok -> platform == SocialPlatform.TikTok
  }
}

enum class AnalyticsContentSection(val displayLabel: String) {
  Overview("Overview"),
  BestPerforming("Best Content"),
  RecentPosts("Recent Posts"),
  PlatformComparison("Platforms"),
  VideoPerformance("Videos"),
  EngagementTrends("Trends"),
  PublishingHistory("History"),
}

enum class AnalyticsMetricAvailability(val badgeLabel: String) {
  Available("Live Metrics"),
  NotConnected("Not Connected"),
  SetupRequired("Setup Required"),
  DataUnavailable("Data Unavailable"),
}

data class EngagementMetricsSnapshot(
  val views: Long? = null,
  val likes: Long? = null,
  val comments: Long? = null,
  val shares: Long? = null,
  val engagementRatePercent: Double? = null,
  val availability: AnalyticsMetricAvailability = AnalyticsMetricAvailability.DataUnavailable,
  val availabilityNote: String = "Official platform analytics API setup is required to retrieve engagement metrics.",
) {
  val hasLiveNumbers: Boolean
    get() = availability == AnalyticsMetricAvailability.Available &&
      (views != null || likes != null || comments != null || shares != null)

  fun formattedViews(): String = views?.let { formatCompactNumber(it) } ?: availability.badgeLabel
  fun formattedLikes(): String = likes?.let { formatCompactNumber(it) } ?: availability.badgeLabel
  fun formattedComments(): String = comments?.let { formatCompactNumber(it) } ?: availability.badgeLabel
  fun formattedShares(): String = shares?.let { formatCompactNumber(it) } ?: availability.badgeLabel
  fun formattedEngagementRate(): String =
    engagementRatePercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: availability.badgeLabel
}

data class PlatformAnalyticsSummary(
  val platform: SocialPlatform,
  val isAccountConnected: Boolean,
  val connectedHandles: List<String> = emptyList(),
  val publishedCount: Int = 0,
  val scheduledCount: Int = 0,
  val failedCount: Int = 0,
  val apiCapabilityNote: String = "",
  val metrics: EngagementMetricsSnapshot = EngagementMetricsSnapshot(),
)

data class ContentPerformanceItem(
  val id: String,
  val title: String,
  val platform: SocialPlatform? = null,
  val accountHandle: String? = null,
  val status: PublishingHistoryStatus,
  val sourceKindLabel: String,
  val aspectRatioLabel: String,
  val mediaFilePath: String? = null,
  val mediaUriString: String? = null,
  val remotePlatformPostId: String? = null,
  val remotePostUrl: String? = null,
  val statusDetail: String = "",
  val createdAtMillis: Long = 0L,
  val updatedAtMillis: Long = 0L,
  val metrics: EngagementMetricsSnapshot = EngagementMetricsSnapshot(),
)

data class EngagementTrendPoint(
  val label: String,
  val publishedCount: Int,
  val scheduledCount: Int,
  val views: Long? = null,
  val likes: Long? = null,
)

fun formatCompactNumber(value: Long): String {
  return when {
    value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
  }
}
