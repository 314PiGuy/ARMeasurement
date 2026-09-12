package com.example.armeasurement

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.armeasurement.controllers.ARSceneController
import com.example.armeasurement.controllers.CanvasController
import com.example.armeasurement.scenes.CanvasOverlay
import com.example.armeasurement.scenes.Settings
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.transform
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.material.setParameter
import io.github.sceneview.node.PlaneNode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var arSceneView: ARSceneView
    private lateinit var canvasController: CanvasController
    private lateinit var sceneController: ARSceneController
    private lateinit var targetPlaneGuide: PlaneNode
    private lateinit var resultView: TextView
    private lateinit var trackingAlert: TextView
    private lateinit var placeButton: Button

    private var planeGuide = true
    private var instantPlacement = false
    private var depthOcclusion = false
    private var depthOcclusionSupport = false
    private var activeTargetPlane: Plane? = null
    private var activeTargetPlaneSinceNs = 0L
    private var lastTargetUpdateNs = 0L
    private var currentTarget = SurfaceTarget()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setupAR()
    }

    private fun initViews() {
        arSceneView = findViewById(R.id.arSceneView)
        resultView = findViewById(R.id.tvDistance)
        trackingAlert = findViewById(R.id.tvTrackingAlert)
        placeButton = findViewById(R.id.btnPlace)
        canvasController = CanvasController(findViewById<CanvasOverlay>(R.id.canvasOverlay))

        findViewById<Button>(R.id.btnReset).setOnClickListener { resetMeasurement() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
        placeButton.setOnClickListener { placePoint() }
        findViewById<MaterialButtonToggleGroup>(R.id.modeToggleGroup)
            .addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked || !::sceneController.isInitialized) return@addOnButtonCheckedListener
                sceneController.setMode(
                    if (checkedId == R.id.btnModeArea) MeasurementMode.AREA
                    else MeasurementMode.DISTANCE
                )
                resetMeasurement()
            }
    }

    private fun setupAR() {
        arSceneView.configureSession { session, config ->
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            depthOcclusionSupport =
                session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            if (depthOcclusionSupport) config.depthMode = Config.DepthMode.AUTOMATIC
        }

        val planeRenderer = arSceneView.planeRenderer
        planeRenderer.isEnabled = false
        val textureRatio =
            planeRenderer.planeTexture.getWidth(0).toFloat() /
                planeRenderer.planeTexture.getHeight(0)
        planeRenderer.planeMaterial.defaultInstance.apply {
            setParameter(
                PlaneRenderer.MATERIAL_UV_SCALE,
                PLANE_DOT_UV_SCALE,
                PLANE_DOT_UV_SCALE * textureRatio
            )
            setParameter(PlaneRenderer.MATERIAL_COLOR, 0.05f, 0.8f, 1f)
            setParameter(PlaneRenderer.MATERIAL_SPOTLIGHT_RADIUS, GUIDE_SPOTLIGHT_RADIUS)
        }
        targetPlaneGuide = PlaneNode(
            engine = arSceneView.engine,
            size = Float3(GUIDE_SIZE_METERS, 0f, GUIDE_SIZE_METERS),
            materialInstance = planeRenderer.planeMaterial.defaultInstance
        ).apply {
            isVisible = false
            arSceneView.addChildNode(this)
        }

        sceneController = ARSceneController(arSceneView)
        resetMeasurement()
        arSceneView.onSessionUpdated = { _, frame -> updateFrame(frame) }
    }

    private fun updateFrame(frame: Frame) {
        val center = PointF(arSceneView.width / 2f, arSceneView.height / 2f)
        val now = System.nanoTime()
        if (now - lastTargetUpdateNs >= TARGET_UPDATE_INTERVAL_NS) {
            currentTarget = findTarget(frame, center.x, center.y, now)
            updatePlaneGuide(currentTarget.guideHit)
            lastTargetUpdateNs = now
        }

        runOnUiThread {
            canvasController.update(center, currentTarget.placementHit != null)
            placeButton.isEnabled = currentTarget.placementHit != null &&
                sceneController.pointCount < sceneController.mode.maximumPoints
            trackingAlert.visibility = if (currentTarget.hasTrackedPlane) View.GONE else View.VISIBLE
        }
    }

    private fun findTarget(frame: Frame, x: Float, y: Float, now: Long): SurfaceTarget {
        val trackedPlanes = arSceneView.session?.getAllTrackables(Plane::class.java)
            ?.filter { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
            .orEmpty()
        val hits = frame.hitTest(x, y)
        val planeHits = hits.filter { hit ->
            val plane = hit.trackable as? Plane ?: return@filter false
            plane in trackedPlanes && plane.isPoseInPolygon(hit.hitPose)
        }
        val depthHit = hits.firstOrNull {
            it.trackable is DepthPoint && it.trackable.trackingState == TrackingState.TRACKING
        }
        val planeHit = if (depthHit == null) {
            planeHits.minByOrNull { it.distance }
        } else {
            planeHits.minByOrNull { abs(it.distance - depthHit.distance) }
        }

        if (planeGuide && planeHit != null) {
            val plane = planeHit.trackable as Plane
            if (plane != activeTargetPlane) {
                activeTargetPlane = plane
                activeTargetPlaneSinceNs = now
            }
            val blocked = depthOcclusion && depthHit != null &&
                depthHit.distance + OCCLUSION_MARGIN_METERS < planeHit.distance
            val stable = now - activeTargetPlaneSinceNs >= PLANE_STABILIZATION_NS
            return SurfaceTarget(
                placementHit = planeHit.takeIf { stable && !blocked },
                guideHit = planeHit,
                hasTrackedPlane = trackedPlanes.isNotEmpty()
            )
        }

        activeTargetPlane = null
        if (instantPlacement) {
            frame.hitTestInstantPlacement(x, y, INSTANT_PLACEMENT_DISTANCE_METERS)
                .firstOrNull {
                    val point = it.trackable as? InstantPlacementPoint
                    point?.trackingState == TrackingState.TRACKING
                }
                ?.let {
                    return SurfaceTarget(
                        placementHit = it,
                        hasTrackedPlane = trackedPlanes.isNotEmpty()
                    )
                }
        }
        return SurfaceTarget(hasTrackedPlane = trackedPlanes.isNotEmpty())
    }

    private fun updatePlaneGuide(hit: HitResult?) {
        targetPlaneGuide.isVisible = planeGuide && hit != null
        if (hit != null) targetPlaneGuide.worldTransform = hit.hitPose.transform
    }

    private fun placePoint() {
        val frame = arSceneView.session?.frame ?: return
        val x = arSceneView.width / 2f
        val y = arSceneView.height / 2f
        val hit = findTarget(frame, x, y, System.nanoTime()).placementHit ?: return
        val update = sceneController.handleHit(hit, surfaceContrast(frame, x, y))

        update.value?.let { value ->
            resultView.text = when (update.mode) {
                MeasurementMode.DISTANCE ->
                    getString(R.string.distance_result, format(value * 100f))
                MeasurementMode.AREA ->
                    getString(R.string.area_result, format(value * 10_000f))
            }
            resultView.visibility = View.VISIBLE
        }
        placeButton.text = if (update.pointCount >= update.mode.maximumPoints) {
            getString(R.string.measurement_complete)
        } else {
            getString(
                R.string.place_point_count,
                update.pointCount + 1,
                update.mode.maximumPoints
            )
        }
    }

    private fun surfaceContrast(frame: Frame, viewX: Float, viewY: Float): Int {
        val imagePoint = FloatArray(2)
        frame.transformCoordinates2d(
            Coordinates2d.VIEW,
            floatArrayOf(viewX, viewY),
            Coordinates2d.IMAGE_PIXELS,
            imagePoint
        )
        return try {
            frame.acquireCameraImage().use { image ->
                val crop = image.cropRect
                val x = imagePoint[0].roundToInt().coerceIn(crop.left, crop.right - 1)
                val y = imagePoint[1].roundToInt().coerceIn(crop.top, crop.bottom - 1)
                val plane = image.planes[0]
                val index = y * plane.rowStride + x * plane.pixelStride
                val luminance = plane.buffer.get(index).toInt() and 0xff
                if (luminance < DARK_SURFACE_LUMINANCE) Color.WHITE else Color.BLACK
            }
        } catch (_: Exception) {
            Color.WHITE
        }
    }

    private fun resetMeasurement() {
        if (::sceneController.isInitialized) sceneController.clear()
        resultView.visibility = View.GONE
        placeButton.isEnabled = false
        val mode = if (::sceneController.isInitialized) {
            sceneController.mode
        } else {
            MeasurementMode.DISTANCE
        }
        placeButton.text = getString(R.string.place_point_count, 1, mode.maximumPoints)
    }

    private fun showSettings() {
        Settings(
            planeGuide = planeGuide,
            instantPlacement = instantPlacement,
            depthOcclusion = depthOcclusion,
            depthOcclusionSupport = depthOcclusionSupport,
            onPlaneGuideToggle = {
                planeGuide = it
                activeTargetPlane = null
                if (!it) targetPlaneGuide.isVisible = false
            },
            onInstantPlacementToggle = {
                instantPlacement = it
                arSceneView.session?.let { session ->
                    val config = session.config
                    config.instantPlacementMode = if (it) {
                        Config.InstantPlacementMode.LOCAL_Y_UP
                    } else {
                        Config.InstantPlacementMode.DISABLED
                    }
                    session.configure(config)
                }
            },
            onDepthOcclusionToggle = {
                depthOcclusion = it && depthOcclusionSupport
                arSceneView.cameraStream?.isDepthOcclusionEnabled = depthOcclusion
            }
        ).show(supportFragmentManager, "settings")
    }

    private fun format(value: Float) = String.format(Locale.getDefault(), "%,.1f", value)

    private data class SurfaceTarget(
        val placementHit: HitResult? = null,
        val guideHit: HitResult? = null,
        val hasTrackedPlane: Boolean = false
    )

    /**
     * Configs for plane dots and settings whatnot
     */
    private companion object {
        const val PLANE_DOT_UV_SCALE = 32f
        const val GUIDE_SIZE_METERS = 0.35f
        const val GUIDE_SPOTLIGHT_RADIUS = 100f
        const val TARGET_UPDATE_INTERVAL_NS = 100_000_000L
        const val PLANE_STABILIZATION_NS = 650_000_000L
        const val OCCLUSION_MARGIN_METERS = 0.04f
        const val INSTANT_PLACEMENT_DISTANCE_METERS = 1.5f
        const val DARK_SURFACE_LUMINANCE = 140
    }
}
