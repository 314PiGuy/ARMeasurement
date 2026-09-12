package com.example.armeasurement

import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.normalize
import kotlin.math.sqrt

object Geometry {

    fun calculateDistance(pose1: Pose, pose2: Pose): Float {
        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    fun getMidpoint(pose1: Pose, pose2: Pose): Float3 {
        return Float3(
            (pose1.tx() + pose2.tx()) / 2f,
            (pose1.ty() + pose2.ty()) / 2f,
            (pose1.tz() + pose2.tz()) / 2f
        )
    }

    /**
     * Calculates the quaternion rotation needed to align an object with the direction between two poses.
     */
    fun getRotationToAlign(pose1: Pose, pose2: Pose): Quaternion {
        val direction = normalize(
            Float3(
                pose2.tx() - pose1.tx(),
                pose2.ty() - pose1.ty(),
                pose2.tz() - pose1.tz()
            )
        )

        // Cylinder default alignment points positive y
        val up = Float3(0f, 1f, 0f)

        val dotProduct = dot(up, direction)

        if (dotProduct < -0.9999f) {
            return Quaternion.fromAxisAngle(Float3(1f, 0f, 0f), 180f)
        } else if (dotProduct > 0.9999f) {
            return Quaternion()
        }

        val axis = cross(up, direction)
        val s = sqrt((1f + dotProduct) * 2f)
        val inverseS = 1f / s

        return normalize(
            Quaternion(axis.x * inverseS, axis.y * inverseS, axis.z * inverseS, s * 0.5f)
        )
    }

    /**
     * Polygon with vertices in PERIMETER ORDER (nothing stopping crossing over)
     */
    fun calculateArea(poses: List<Pose>): Float? {
        if (poses.size < 3) return null

        var normalX = 0f
        var normalY = 0f
        var normalZ = 0f

        poses.forEachIndexed { index, current ->
            val next = poses[(index + 1) % poses.size]
            normalX += (current.ty() - next.ty()) * (current.tz() + next.tz())
            normalY += (current.tz() - next.tz()) * (current.tx() + next.tx())
            normalZ += (current.tx() - next.tx()) * (current.ty() + next.ty())
        }

        return 0.5f * sqrt(
            (normalX * normalX + normalY * normalY + normalZ * normalZ).toDouble()
        ).toFloat()
    }

}
