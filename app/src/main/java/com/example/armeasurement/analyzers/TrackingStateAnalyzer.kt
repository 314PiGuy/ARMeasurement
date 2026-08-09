package com.example.armeasurement.analyzers

import com.google.ar.core.Frame
import com.google.ar.core.TrackingState

class TrackingStateAnalyzer(
    private val onTrackingChanged: (Boolean) -> Unit
) : Analyzer {

    private var lastState: TrackingState = TrackingState.STOPPED

    override fun analyze(frame: Frame) {
        val currentState = frame.camera.trackingState
        if (currentState != lastState) {
            lastState = currentState
            onTrackingChanged(currentState == TrackingState.TRACKING)
        }
    }
}