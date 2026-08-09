package com.example.armeasurement.scenes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class CanvasOverlay(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val crosshairPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val snappedCrosshairPaint = Paint(crosshairPaint).apply {
        color = Color.GREEN
        strokeWidth = 8f
    }

    private val cornerPaint = Paint().apply {
        color = "#80FFEB3B".toColorInt() // Semi-transparent yellow
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    var crosshairPos: PointF? = null
    var isSnapped: Boolean = false
    var cornersToDraw: List<PointF> = emptyList()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw 2D corners
        cornersToDraw.forEach { point ->
            canvas.drawCircle(point.x, point.y, 12f, cornerPaint)
        }

        // 2. Draw Crosshair
        crosshairPos?.let { center ->
            val paint = if (isSnapped) snappedCrosshairPaint else crosshairPaint
            val radius = if (isSnapped) 45f else 35f

            // Draw center circle
            canvas.drawCircle(center.x, center.y, radius, paint)

            // Draw tick marks
            canvas.drawLine(center.x - radius - 15, center.y, center.x - radius + 5, center.y, paint) // Left
            canvas.drawLine(center.x + radius + 15, center.y, center.x + radius - 5, center.y, paint) // Right
            canvas.drawLine(center.x, center.y - radius - 15, center.x, center.y - radius + 5, paint) // Top
            canvas.drawLine(center.x, center.y + radius + 15, center.x, center.y + radius - 5, paint) // Bottom
        }
    }
}