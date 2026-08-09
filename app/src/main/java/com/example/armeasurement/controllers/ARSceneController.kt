package com.example.armeasurement.controllers

import com.example.armeasurement.Geometry
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode

class ARSceneController(private val arSceneView: ARSceneView) {

    private val activeNodes = mutableListOf<Node>()
    private val activeAnchors = mutableListOf<Anchor>()

    /**
     * Handles an AR HitResult: creates an anchor, draws the 3D point,
     * and if 2+ points exist, draws the edge line and returns the calculated distance in meters.
     */
    fun handleHit(hitResult: HitResult): Float? {
        val anchor = hitResult.createAnchor()
        activeAnchors.add(anchor)

        // Create a point anchored in "real" space and add a sphere to it
        val anchorNode = AnchorNode(arSceneView.engine, anchor)
        val sphereNode = SphereNode(
            engine = arSceneView.engine,
            radius = 0.003f
        )
        anchorNode.addChildNode(sphereNode)
        arSceneView.addChildNode(anchorNode)
        activeNodes.add(anchorNode)

        if (activeAnchors.size >= 2) {
            val anchor1 = activeAnchors[activeAnchors.size - 2]
            val anchor2 = activeAnchors[activeAnchors.size - 1]

            val distance = Geometry.calculateDistance(anchor1.pose, anchor2.pose)
            drawEdge(anchor1, anchor2, distance)

            return distance
        }

        return null
    }

    /**
     * Draw 3d cylinder in 3D space between points
     */
    private fun drawEdge(anchor1: Anchor, anchor2: Anchor, distance: Float) {
        val lineNode = CylinderNode(
            engine = arSceneView.engine,
            radius = 0.002f,
            height = distance
        )

        lineNode.position = Geometry.getMidpoint(anchor1.pose, anchor2.pose)
        lineNode.quaternion = Geometry.getRotationToAlign(anchor1.pose, anchor2.pose)

        arSceneView.addChildNode(lineNode)
        activeNodes.add(lineNode)
    }

    /**
     * Clears all 3D nodes from the scene and detaches ARCore anchors.
     */
    fun clear() {
        activeNodes.forEach { arSceneView.removeChildNode(it) }
        activeNodes.clear()

        // Detach anchors to free up ARCore memory
        activeAnchors.forEach { it.detach() }
        activeAnchors.clear()
    }
}