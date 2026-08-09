package com.example.armeasurement.controllers

import android.graphics.PointF
import com.example.armeasurement.scenes.CanvasOverlay

class CanvasController(private val overlayView: CanvasOverlay) {

    fun updateCrosshair(position: PointF, isSnapped: Boolean) {
        overlayView.crosshairPos = position
        overlayView.isSnapped = isSnapped
        overlayView.invalidate()
    }

    fun drawCorners(corners2D: List<PointF>) {
        overlayView.cornersToDraw = corners2D
        overlayView.invalidate()
    }

    fun clear() {
        overlayView.crosshairPos = null
        overlayView.cornersToDraw = emptyList()
        overlayView.invalidate()
    }
}