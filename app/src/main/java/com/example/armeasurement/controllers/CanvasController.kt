package com.example.armeasurement.controllers

import android.graphics.PointF
import com.example.armeasurement.scenes.CanvasOverlay

class CanvasController(private val overlayView: CanvasOverlay) {

    fun update(crosshair: PointF, isReady: Boolean) {
        val previousCrosshair = overlayView.crosshairPos
        val isUnchanged = previousCrosshair != null &&
            previousCrosshair.x == crosshair.x &&
            previousCrosshair.y == crosshair.y &&
            overlayView.isReady == isReady
        if (isUnchanged) return

        overlayView.crosshairPos = crosshair
        overlayView.isReady = isReady
        overlayView.invalidate()
    }

    fun clear() {
        overlayView.crosshairPos = null
        overlayView.invalidate()
    }
}
