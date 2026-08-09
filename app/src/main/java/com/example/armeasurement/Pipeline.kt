package com.example.armeasurement

import com.example.armeasurement.analyzers.Analyzer
import com.google.ar.core.Frame

class Pipeline {
    private val analyzers = mutableListOf<Analyzer>()

    fun addAnalyzer(analyzer: Analyzer): Pipeline {
        analyzers.add(analyzer)
        return this
    }

    fun removeAnalyzer(analyzer: Analyzer) {
        analyzers.remove(analyzer)
    }

    fun process(frame: Frame) {
        analyzers.forEach { analyzer ->
            analyzer.analyze(frame)
        }
    }
}