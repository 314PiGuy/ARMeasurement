package com.example.armeasurement.scenes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class CanvasOverlay(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val crosshairPaint = Paint().apply {
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val centerPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    var crosshairPos: PointF? = null
    var isReady = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        crosshairPos?.let { center ->
            val color = if (isReady) Color.rgb(0, 230, 118) else Color.WHITE
            crosshairPaint.color = color
            crosshairPaint.strokeWidth = if (isReady) 8f else 6f
            centerPaint.color = color
            val radius = if (isReady) 45f else 38f

            canvas.drawCircle(center.x, center.y, radius, crosshairPaint)
            canvas.drawCircle(center.x, center.y, 6f, centerPaint)

            canvas.drawLine(center.x - radius - 15, center.y, center.x - radius + 5, center.y, crosshairPaint)
            canvas.drawLine(center.x + radius + 15, center.y, center.x + radius - 5, center.y, crosshairPaint)
            canvas.drawLine(center.x, center.y - radius - 15, center.x, center.y - radius + 5, crosshairPaint)
            canvas.drawLine(center.x, center.y + radius + 15, center.x, center.y + radius - 5, crosshairPaint)

        }
    }
}
