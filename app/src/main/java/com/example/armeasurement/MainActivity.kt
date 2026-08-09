package com.example.armeasurement

import android.graphics.PointF
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color as AndroidColor

import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState

import io.github.sceneview.ar.ARSceneView

import com.example.armeasurement.analyzers.*
import com.example.armeasurement.controllers.*
import com.example.armeasurement.scenes.*

class MainActivity : AppCompatActivity() { // No longer needs MeasurementController.Listener

    private lateinit var arSceneView: ARSceneView
    private lateinit var canvasOverlay: CanvasOverlay
    private lateinit var tvDistance: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlace: Button
    private lateinit var btnReset: Button
    private lateinit var btnSettings: Button
    private lateinit var sceneManager: ARSceneController
    private lateinit var canvasController: CanvasController
    private var isInstantPlacementEnabled = false

    private val framePipeline = Pipeline()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupARSession()
        setupArchitecture()
    }

    private fun initViews() {
        tvDistance = findViewById(R.id.tvDistance)
        tvStatus = findViewById(R.id.tvStatus)
        btnPlace = findViewById(R.id.btnPlace)
        btnReset = findViewById(R.id.btnReset)
        arSceneView = findViewById(R.id.arSceneView)
        canvasOverlay = findViewById(R.id.canvasOverlay)
        btnSettings = findViewById(R.id.btnSettings)

        btnPlace.setOnClickListener { performCenterHitTest() }
        btnReset.setOnClickListener { resetMeasurement() }
        btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun setupARSession() {
        arSceneView.configureSession { session, config ->
            config.instantPlacementMode = if (isInstantPlacementEnabled) {
                Config.InstantPlacementMode.LOCAL_Y_UP
            } else {
                Config.InstantPlacementMode.DISABLED
            }
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
        }
        arSceneView.planeRenderer.isVisible = true
    }

    private fun setupArchitecture() {
        sceneManager = ARSceneController(arSceneView)
        canvasController = CanvasController(canvasOverlay)

        framePipeline.addAnalyzer(TrackingStateAnalyzer { isTracking ->
            runOnUiThread {
                tvStatus.text = if (isTracking) "Status: TRACKING" else "Status: LOST TRACKING"
                tvStatus.setTextColor(if (isTracking) AndroidColor.GREEN else AndroidColor.RED)
            }
        })

        arSceneView.onSessionUpdated = { _, frame ->
            framePipeline.process(frame)

            val centerX = arSceneView.width / 2f + 10f
            val centerY = arSceneView.height / 2f
            val crosshairPoint = PointF(centerX, centerY)

            // hit test to check if snapped to a valid surface
            val hitResults = frame.hitTest(centerX, centerY)
            val isSnapped = hitResults.any { hit ->
                when (hit.trackable) {
                    is Plane, is DepthPoint, is InstantPlacementPoint -> true
                    else -> false
                }
            }

            // draw crosshair and potentially whatever else in 2d
            runOnUiThread {
                canvasController.updateCrosshair(crosshairPoint, isSnapped)
            }
        }
    }

    /**
     * Performs a hit test at the center of the screen and handles the result.
     * If a valid hit is found, it will either place a point or calculate the distance between two points.
     */
    private fun performCenterHitTest() {
        val frame = arSceneView.session?.frame ?: return
        val centerX = arSceneView.width / 2f
        val centerY = arSceneView.height / 2f

        val hitResults = frame.hitTest(centerX, centerY)

        val validHit = hitResults.firstOrNull { hit ->
            val trackable = hit.trackable
            when (trackable) {
                is Plane -> trackable.trackingState == TrackingState.TRACKING && trackable.isPoseInPolygon(hit.hitPose)
                is InstantPlacementPoint -> isInstantPlacementEnabled && trackable.trackingState == TrackingState.TRACKING
                is DepthPoint -> {
                    val isDepthEnabled = arSceneView.session?.config?.depthMode != Config.DepthMode.DISABLED
                    isDepthEnabled && isInstantPlacementEnabled && trackable.trackingState == TrackingState.TRACKING
                }
                else -> false
            }
        }

        validHit?.let { hit ->
            val distanceInMeters = sceneManager.handleHit(hit)
            if (distanceInMeters != null) {
                tvDistance.text = String.format("Distance: %.1f cm", distanceInMeters * 100)
            } else {
                tvDistance.text = "Point 1 placed. Tap to place Point 2."
            }
        }
    }

    /**
     * Resets measurement & points
     */
    private fun resetMeasurement() {
        sceneManager.clear()
        tvDistance.text = "Aim and place two points"
    }

    private fun showSettingsDialog() {
        val dialog = Settings(
            sessionGetter = { arSceneView.session },
            isPlaneVisible = arSceneView.planeRenderer.isVisible,
            isInstantPlacementEnabled = isInstantPlacementEnabled,
            onPlaneToggle = { isVisible -> arSceneView.planeRenderer.isVisible = isVisible },
            onInstantPlacementToggle = { isEnabled ->
                isInstantPlacementEnabled = isEnabled
                arSceneView.session?.let { session ->
                    val config = session.config
                    config.instantPlacementMode = if (isEnabled) {
                        Config.InstantPlacementMode.LOCAL_Y_UP
                    } else {
                        Config.InstantPlacementMode.DISABLED
                    }
                    session.configure(config)
                }
            }
        )
        dialog.show(supportFragmentManager, "settings")
    }
}