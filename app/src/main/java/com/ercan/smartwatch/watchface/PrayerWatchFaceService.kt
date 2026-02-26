package com.ercan.smartwatch.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.ercan.smartwatch.ServiceLocator
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrayerWatchFaceService : WatchFaceService() {
    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = ComplicationSlotsManager(emptyList(), currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = PrayerWatchFaceRenderer(
            surfaceHolder = surfaceHolder,
            currentUserStyleRepository = currentUserStyleRepository,
            watchState = watchState,
            useCase = PrayerWatchFaceUseCase(
                settingsStore = ServiceLocator.settingsStore(this),
                repository = ServiceLocator.prayerRepository(this)
            )
        )

        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}

private class PrayerWatchFaceRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val useCase: PrayerWatchFaceUseCase
) : Renderer.CanvasRenderer2<PrayerWatchFaceSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    INTERACTIVE_UPDATE_RATE_MS,
    false
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var state: PrayerWatchFaceUiState = PrayerWatchFaceUiState.Error(
        now = ZonedDateTime.now(),
        message = "Loading"
    )

    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 224, 224)
        textAlign = Paint.Align.CENTER
    }

    private val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(204, 204, 204)
        textAlign = Paint.Align.CENTER
    }

    init {
        startStateUpdates()
    }

    override suspend fun createSharedAssets(): PrayerWatchFaceSharedAssets = PrayerWatchFaceSharedAssets

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: PrayerWatchFaceSharedAssets
    ) {
        val isAmbient = renderParameters.drawMode == DrawMode.AMBIENT
        val (shiftX, shiftY) = if (isAmbient) {
            burnInShift(bounds, zonedDateTime)
        } else {
            0f to 0f
        }

        applyPalette(isAmbient)

        canvas.drawRect(
            0f,
            0f,
            bounds.width().toFloat(),
            bounds.height().toFloat(),
            backgroundPaint
        )

        val width = bounds.width().toFloat()
        val centerX = bounds.exactCenterX() + shiftX
        val topY = (bounds.height() * 0.30f) + shiftY
        val lineGap = bounds.height() * 0.12f

        timePaint.textSize = width * 0.20f
        datePaint.textSize = width * 0.09f
        infoPaint.textSize = width * 0.075f

        val locale = Locale.getDefault()
        val timeText = zonedDateTime.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        )
        val dateText = zonedDateTime.format(
            DateTimeFormatter.ofPattern("EEE, dd MMM", locale)
        )

        canvas.drawText(timeText, centerX, topY, timePaint)
        canvas.drawText(dateText, centerX, topY + lineGap, datePaint)

        val infoStartY = topY + (lineGap * 4f)
        when (val current = state) {
            is PrayerWatchFaceUiState.SetupRequired -> {
                if (!isAmbient) {
                    canvas.drawText("Setup required", centerX, infoStartY, infoPaint)
                    canvas.drawText("Open app to configure", centerX, infoStartY + lineGap * 0.70f, infoPaint)
                }
            }

            is PrayerWatchFaceUiState.Content -> {
                canvas.drawText(
                    "${current.nextPrayerName} ${current.nextPrayerTimeText}",
                    centerX,
                    infoStartY,
                    infoPaint
                )
                if (!isAmbient) {
                    canvas.drawText(current.countdownText, centerX, infoStartY + lineGap * 0.70f, infoPaint)
                }
                if (current.isStale && !isAmbient) {
                    canvas.drawText("Offline cache", centerX, infoStartY + lineGap * 1.40f, infoPaint)
                }
            }

            is PrayerWatchFaceUiState.Error -> {
                if (!isAmbient) {
                    canvas.drawText(current.message, centerX, infoStartY, infoPaint)
                    canvas.drawText("Open app to retry", centerX, infoStartY + lineGap * 0.70f, infoPaint)
                }
            }
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: PrayerWatchFaceSharedAssets
    ) {
        renderParameters.highlightLayer?.let { highlight ->
            canvas.drawColor(highlight.backgroundTint)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startStateUpdates() {
        scope.launch {
            while (isActive) {
                val now = ZonedDateTime.now()
                state = runCatching {
                    useCase.loadState(now)
                }.getOrElse {
                    PrayerWatchFaceUiState.Error(
                        now = now,
                        message = "Unable to load"
                    )
                }

                invalidate()
                delay(millisUntilNextMinute(now))
            }
        }
    }

    private fun millisUntilNextMinute(now: ZonedDateTime): Long {
        val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
        return Duration.between(now, nextMinute)
            .toMillis()
            .coerceAtLeast(1_000L)
    }

    private fun applyPalette(isAmbient: Boolean) {
        if (isAmbient) {
            timePaint.color = Color.rgb(220, 220, 220)
            datePaint.color = Color.rgb(160, 160, 160)
            infoPaint.color = Color.rgb(150, 150, 150)
            timePaint.isAntiAlias = false
            datePaint.isAntiAlias = false
            infoPaint.isAntiAlias = false
        } else {
            timePaint.color = Color.WHITE
            datePaint.color = Color.rgb(224, 224, 224)
            infoPaint.color = Color.rgb(204, 204, 204)
            timePaint.isAntiAlias = true
            datePaint.isAntiAlias = true
            infoPaint.isAntiAlias = true
        }
    }

    private fun burnInShift(bounds: Rect, now: ZonedDateTime): Pair<Float, Float> {
        val index = ((now.toEpochSecond() / 60L) % SHIFT_PATTERN.size).toInt()
        val (gridX, gridY) = SHIFT_PATTERN[index]
        val shiftStepPx = (bounds.width().coerceAtMost(bounds.height()) * SHIFT_RATIO)
            .coerceIn(MIN_SHIFT_PX, MAX_SHIFT_PX)
        return (gridX * shiftStepPx) to (gridY * shiftStepPx)
    }

    private companion object {
        const val INTERACTIVE_UPDATE_RATE_MS = 60_000L
        const val SHIFT_RATIO = 0.008f
        const val MIN_SHIFT_PX = 2f
        const val MAX_SHIFT_PX = 6f

        val SHIFT_PATTERN = arrayOf(
            0f to 0f,
            1f to 0f,
            0f to 1f,
            -1f to 0f,
            0f to -1f,
            1f to 1f,
            -1f to 1f,
            -1f to -1f,
            1f to -1f
        )
    }
}

private object PrayerWatchFaceSharedAssets : Renderer.SharedAssets {
    override fun onDestroy() = Unit
}
