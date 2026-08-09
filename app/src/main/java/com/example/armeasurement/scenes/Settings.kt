package com.example.armeasurement.scenes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.example.armeasurement.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.ar.core.Config
import com.google.ar.core.Session

class Settings(
    private val sessionGetter: () -> Session?,
    private val isPlaneVisible: Boolean,
    private val isInstantPlacementEnabled: Boolean,
    private val onPlaneToggle: (Boolean) -> Unit,
    private val onInstantPlacementToggle: (Boolean) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_settings, container, false)

        val switchPlanes = view.findViewById<SwitchCompat>(R.id.switchPlaneDots)
        val switchInstant = view.findViewById<SwitchCompat>(R.id.switchInstantPlacement)
        val capabilitiesContainer = view.findViewById<LinearLayout>(R.id.capabilitiesContainer)

        switchPlanes.isChecked = isPlaneVisible
        switchInstant.isChecked = isInstantPlacementEnabled

        switchPlanes.setOnCheckedChangeListener { _, isChecked -> onPlaneToggle(isChecked) }
        switchInstant.setOnCheckedChangeListener { _, isChecked -> onInstantPlacementToggle(isChecked) }

        populateCapabilities(capabilitiesContainer)

        return view
    }

    private fun populateCapabilities(container: LinearLayout) {
        val session = sessionGetter() ?: return

        val capabilities = listOf(
            "Automatic Depth API" to session.isDepthModeSupported(Config.DepthMode.AUTOMATIC),
            "Raw Depth API" to session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY),
            "Semantic Mode" to session.isSemanticModeSupported(Config.SemanticMode.ENABLED),
            "Geospatial API" to session.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)
        )

        capabilities.forEach { (name, isSupported) ->
            val tv = TextView(context).apply {
                text = "$name: ${if (isSupported) "SUPPORTED" else "UNSUPPORTED"}"
                setPadding(0, 8, 0, 8)
                textSize = 14f
                setTextColor(if (isSupported) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
            }
            container.addView(tv)
        }
    }
}