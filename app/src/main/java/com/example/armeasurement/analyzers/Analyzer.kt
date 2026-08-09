package com.example.armeasurement.analyzers

import com.google.ar.core.Frame

interface Analyzer {
    /**
     * Called on every AR frame update.
     * Keeps logic lightweight or dispatch to background threads.
     */
    fun analyze(frame: Frame)
}