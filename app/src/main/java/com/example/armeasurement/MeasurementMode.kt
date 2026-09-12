package com.example.armeasurement

enum class MeasurementMode(
    val minimumPoints: Int,
    val maximumPoints: Int
) {
    DISTANCE(minimumPoints = 2, maximumPoints = 2),
    AREA(minimumPoints = 3, maximumPoints = 999) // if this is exceeded, this tool is not for you
}

data class MeasurementUpdate(
    val mode: MeasurementMode,
    val pointCount: Int,
    val value: Float?,
    val isComplete: Boolean
)
