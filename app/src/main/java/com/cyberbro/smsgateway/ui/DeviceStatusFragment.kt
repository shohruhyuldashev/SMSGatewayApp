package com.cyberbro.smsgateway.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cyberbro.smsgateway.R
import com.cyberbro.smsgateway.device.DeviceInfo
import com.cyberbro.smsgateway.device.SignalMonitor

class DeviceStatusFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.deviceNameText).text = DeviceInfo(requireContext()).getDeviceSummary()
        view.findViewById<TextView>(R.id.signalStrengthText).text = SignalMonitor(requireContext()).getSignalStrength()
    }
}
