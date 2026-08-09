package com.example.armeasurement.analyzers

import android.graphics.PointF
import com.example.armeasurement.analyzers.Analyzer
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlin.math.hypot

data class CornerData(val worldPose: Pose, val screenPos: PointF, val plane: Plane)

data class CornerAnalysisResult(
    val polygons: List<List<PointF>>,
    val targetPoint: PointF,
    val isSnapped: Boolean,
    val snappedCorner: CornerData?
)

class CornerAnalyzer(
    private val snapRadiusPx: Float = 120f,
    private val getSession: () -> Session?,
    private val getScreenCenter: () -> PointF?,
    private val worldToScreen: (Float, Float, Float) -> PointF?,
    private val onResult: (CornerAnalysisResult) -> Unit
) : Analyzer {

    override fun analyze(frame: Frame) {
        if (frame.camera.trackingState != TrackingState.TRACKING) return
        val session = getSession() ?: return
        val screenCenter = getScreenCenter() ?: return

        val activePlanes = session.getAllTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }

        val polygons2D = mutableListOf<List<PointF>>()
        val allCorners = mutableListOf<CornerData>()

        for (plane in activePlanes) {
            val buffer = plane.polygon
            val centerPose = plane.centerPose
            val currentPolygon = mutableListOf<PointF>()

            var i = 0
            while (i < buffer.limit()) {
                val localPose = Pose.makeTranslation(buffer.get(i), 0f, buffer.get(i + 1))
                val worldPose = centerPose.compose(localPose)

                val screenPos = worldToScreen(worldPose.tx(), worldPose.ty(), worldPose.tz())
                if (screenPos != null) {
                    currentPolygon.add(screenPos)
                    allCorners.add(CornerData(worldPose, screenPos, plane))
                }
                i += 2
            }
            if (currentPolygon.isNotEmpty()) polygons2D.add(currentPolygon)
        }

        var closestCorner: CornerData? = null
        var minDistance = Float.MAX_VALUE

        for (corner in allCorners) {
            val dist = hypot(corner.screenPos.x - screenCenter.x, corner.screenPos.y - screenCenter.y)
            if (dist < minDistance) {
                minDistance = dist
                closestCorner = corner
            }
        }

        val isSnapped = closestCorner != null && minDistance <= snapRadiusPx
        val target = if (isSnapped) closestCorner.screenPos else screenCenter

        onResult(CornerAnalysisResult(polygons2D, target, isSnapped, if (isSnapped) closestCorner else null))
    }
}