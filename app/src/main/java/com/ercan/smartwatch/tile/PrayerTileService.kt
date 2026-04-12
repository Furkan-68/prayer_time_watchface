@file:Suppress("DEPRECATION")

package com.ercan.smartwatch.tile

import android.graphics.Color
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.ercan.smartwatch.ServiceLocator
import com.ercan.smartwatch.watchface.PrayerWatchFaceUiState
import com.ercan.smartwatch.watchface.PrayerWatchFaceUseCase
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking

class PrayerTileService : TileService() {
    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val state = runBlocking {
            val useCase = PrayerWatchFaceUseCase(
                settingsStore = ServiceLocator.settingsStore(this@PrayerTileService),
                repository = ServiceLocator.prayerRepository(this@PrayerTileService)
            )
            useCase.loadState(now = ZonedDateTime.now())
        }

        val tile = Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layoutFor(state))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> {
        val resources = Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        return Futures.immediateFuture(resources)
    }

    private fun layoutFor(state: PrayerWatchFaceUiState): LayoutElementBuilders.LayoutElement {
        val subtitle = subtitleFor(state)
        val secondary = secondaryLineFor(state)

        val content = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text(titleFor(state), titleStyle(), maxLines = 1))
            .addContent(spacer(6f))
            .addContent(text(subtitle, bodyStyle(), maxLines = 2))

        if (secondary != null) {
            content
                .addContent(spacer(4f))
                .addContent(text(secondary, captionStyle(), maxLines = 1))
        }

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setAll(dp(14f))
                            .build()
                    )
                    .build()
            )
            .addContent(content.build())
            .build()
    }

    private fun text(
        value: String,
        style: LayoutElementBuilders.FontStyle,
        maxLines: Int
    ): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setFontStyle(style)
            .setMaxLines(maxLines)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .build()
    }

    private fun spacer(heightDp: Float): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Spacer.Builder()
            .setHeight(dp(heightDp))
            .build()
    }

    private fun titleStyle(): LayoutElementBuilders.FontStyle {
        return LayoutElementBuilders.FontStyle.Builder()
            .setSize(sp(20f))
            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
            .setColor(argb(Color.WHITE))
            .build()
    }

    private fun bodyStyle(): LayoutElementBuilders.FontStyle {
        return LayoutElementBuilders.FontStyle.Builder()
            .setSize(sp(15f))
            .setColor(argb(Color.rgb(235, 235, 235)))
            .build()
    }

    private fun captionStyle(): LayoutElementBuilders.FontStyle {
        return LayoutElementBuilders.FontStyle.Builder()
            .setSize(sp(13f))
            .setColor(argb(Color.rgb(170, 170, 170)))
            .build()
    }

    private fun titleFor(state: PrayerWatchFaceUiState): String {
        return when (state) {
            is PrayerWatchFaceUiState.SetupRequired -> "Setup required"
            is PrayerWatchFaceUiState.Content -> state.nextPrayerName
            is PrayerWatchFaceUiState.Error -> "Unable to load"
        }
    }

    private fun subtitleFor(state: PrayerWatchFaceUiState): String {
        return when (state) {
            is PrayerWatchFaceUiState.SetupRequired -> "Open app to configure"
            is PrayerWatchFaceUiState.Content -> state.nextPrayerTimeText
            is PrayerWatchFaceUiState.Error -> "Open app to retry"
        }
    }

    private fun secondaryLineFor(state: PrayerWatchFaceUiState): String? {
        return when (state) {
            is PrayerWatchFaceUiState.Content -> {
                if (state.isStale) "${state.countdownText}  Offline cache" else state.countdownText
            }

            is PrayerWatchFaceUiState.SetupRequired,
            is PrayerWatchFaceUiState.Error -> null
        }
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val FRESHNESS_INTERVAL_MS = 60_000L
    }
}
