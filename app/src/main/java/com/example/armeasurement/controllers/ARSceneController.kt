package com.example.armeasurement.controllers

import android.graphics.Color
import com.example.armeasurement.Geometry
import com.example.armeasurement.MeasurementMode
import com.example.armeasurement.MeasurementUpdate
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

class ARSceneController(private val arSceneView: ARSceneView) {

    private val pointNodes = mutableListOf<Node>()
    private val edgeNodes = mutableListOf<Node>()
    private val activeAnchors = mutableListOf<Anchor>()

    private val modeMaterials = mapOf(
        MeasurementMode.DISTANCE to arSceneView.materialLoader.createColorInstance(
            color = Color.rgb(0, 229, 255),
            metallic = 0.1f,
            roughness = 0.35f
        ),
        MeasurementMode.AREA to arSceneView.materialLoader.createColorInstance(
            color = Color.rgb(255, 179, 0),
            metallic = 0.1f,
            roughness = 0.35f
        )
    )
    private val pointMaterials = mapOf(
        Color.BLACK to createPointMaterial(Color.BLACK),
        Color.WHITE to createPointMaterial(Color.WHITE)
    )

    var mode = MeasurementMode.DISTANCE
        private set

    val pointCount: Int
        get() = activeAnchors.size

    fun setMode(newMode: MeasurementMode) {
        if (mode == newMode) return
        clear()
        mode = newMode
    }

    /**
     * Anchors and draw marker, return status of measurement state
     */
    fun handleHit(hitResult: HitResult, pointColor: Int): MeasurementUpdate {
        if (activeAnchors.size < mode.maximumPoints) {
            val anchor = hitResult.createAnchor()
            activeAnchors.add(anchor)
            drawPoint(anchor, hitResult.hitPose, pointColor)
            redrawEdges()
        }

        return currentMeasurement()
    }

    private fun drawPoint(anchor: Anchor, surfacePose: Pose, pointColor: Int) {
        val anchorNode = AnchorNode(arSceneView.engine, anchor)
        val surfaceSide = surfaceFacingSide(surfacePose)
        val pointNode = SphereNode(
            engine = arSceneView.engine,
            radius = POINT_RADIUS_METERS,
            stacks = 12,
            slices = 16,
            materialInstance = pointMaterials.getValue(pointColor)
        ).apply {
            position = Float3(0f, surfaceSide * POINT_OFFSET_METERS, 0f)
        }

        anchorNode.addChildNode(pointNode)
        arSceneView.addChildNode(anchorNode)
        pointNodes.add(anchorNode)
    }

    /**
     * Orient marker to camera (flip across surface if needed)
     * */
    private fun surfaceFacingSide(surfacePose: Pose): Float {
        val cameraPose = arSceneView.session?.frame?.camera?.pose ?: return 1f
        val normal = surfacePose.yAxis
        val cameraDirectionX = cameraPose.tx() - surfacePose.tx()
        val cameraDirectionY = cameraPose.ty() - surfacePose.ty()
        val cameraDirectionZ = cameraPose.tz() - surfacePose.tz()
        val cameraSide = cameraDirectionX * normal[0] +
            cameraDirectionY * normal[1] +
            cameraDirectionZ * normal[2]
        return if (cameraSide >= 0f) 1f else -1f
    }

    private fun createPointMaterial(color: Int) =
        arSceneView.materialLoader.createColorInstance(
            color = color,
            metallic = 0f,
            roughness = 0.15f,
            reflectance = 1f
        )

    private fun redrawEdges() {
        edgeNodes.forEach { arSceneView.removeChildNode(it) }
        edgeNodes.clear()

        val poses = activeAnchors.map { it.pose }
        when (mode) {
            MeasurementMode.DISTANCE -> {
                if (poses.size == 2) drawEdge(poses[0], poses[1])
            }

            MeasurementMode.AREA -> {
                poses.zipWithNext().forEach { (start, end) -> drawEdge(start, end) }
                if (poses.size >= mode.minimumPoints) drawEdge(poses.last(), poses.first())
            }
        }
    }

    /**
     * Draws edge as thin cylinder
     */
    private fun drawEdge(start: Pose, end: Pose) {
        val distance = Geometry.calculateDistance(start, end)
        if (distance <= 0.0001f) return

        val lineNode = CylinderNode(
            engine = arSceneView.engine,
            radius = 0.0015f,
            height = distance,
            materialInstance = modeMaterials.getValue(mode)
        ).apply {
            position = Geometry.getMidpoint(start, end)
            quaternion = Geometry.getRotationToAlign(start, end)
        }

        arSceneView.addChildNode(lineNode)
        edgeNodes.add(lineNode)
    }

    /*
     * Returns node and measurement details and if enough points
     */
    private fun currentMeasurement(): MeasurementUpdate {
        val poses = activeAnchors.map { it.pose }
        val value = when (mode) {
            MeasurementMode.DISTANCE -> poses.takeIf { it.size >= 2 }
                ?.let { Geometry.calculateDistance(it[0], it[1]) }

            MeasurementMode.AREA -> Geometry.calculateArea(poses)
        }

        return MeasurementUpdate(
            mode = mode,
            pointCount = poses.size,
            value = value,
            isComplete = poses.size >= mode.minimumPoints
        )
    }

    fun clear() {
        edgeNodes.forEach { arSceneView.removeChildNode(it) }
        edgeNodes.clear()

        pointNodes.forEach { arSceneView.removeChildNode(it) }
        pointNodes.clear()

        activeAnchors.forEach { it.detach() }
        activeAnchors.clear()
    }

    private companion object {
        const val POINT_RADIUS_METERS = 0.004f
        const val POINT_OFFSET_METERS = 0.0045f
    }
}
