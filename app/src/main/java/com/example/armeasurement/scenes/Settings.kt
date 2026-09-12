package com.example.armeasurement.scenes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import com.example.armeasurement.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class Settings(
    private val planeGuide: Boolean,
    private val instantPlacement: Boolean,
    private val depthOcclusion: Boolean,
    private val depthOcclusionSupport: Boolean,
    private val onPlaneGuideToggle: (Boolean) -> Unit,
    private val onInstantPlacementToggle: (Boolean) -> Unit,
    private val onDepthOcclusionToggle: (Boolean) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_settings, container, false).apply {
            bindSwitch(R.id.switchPlaneDots, planeGuide, onPlaneGuideToggle)
            bindSwitch(R.id.switchInstantPlacement, instantPlacement, onInstantPlacementToggle)
            bindSwitch(
                R.id.switchDepthOcclusion,
                depthOcclusion,
                onDepthOcclusionToggle,
                depthOcclusionSupport
            )
        }
    }

    private fun View.bindSwitch(
        id: Int,
        checked: Boolean,
        onChanged: (Boolean) -> Unit,
        enabled: Boolean = true
    ) {
        findViewById<SwitchCompat>(id).apply {
            isChecked = checked
            isEnabled = enabled
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        }
    }
}
