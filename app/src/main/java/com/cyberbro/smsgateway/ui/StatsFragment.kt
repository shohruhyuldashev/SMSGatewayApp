package com.cyberbro.smsgateway.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cyberbro.smsgateway.R
import com.cyberbro.smsgateway.storage.SmsDatabase
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val queuedCount = view.findViewById<TextView>(R.id.statsQueuedCount)
        val sentCount = view.findViewById<TextView>(R.id.statsSentCount)
        val deliveredCount = view.findViewById<TextView>(R.id.statsDeliveredCount)
        val failedCount = view.findViewById<TextView>(R.id.statsFailedCount)
        val todayCount = view.findViewById<TextView>(R.id.statsTodayCount)
        val weekCount = view.findViewById<TextView>(R.id.statsWeekCount)
        val monthCount = view.findViewById<TextView>(R.id.statsMonthCount)

        lifecycleScope.launch {
            val dao = SmsDatabase.getInstance(requireContext()).smsTaskDao()
            val queued = dao.getCountByStatus("queued")
            val sent = dao.getCountByStatus("sent")
            val delivered = dao.getCountByStatus("delivered")
            val failed = dao.getCountByStatus("failed")
            queuedCount.text = queued.toString()
            sentCount.text = sent.toString()
            deliveredCount.text = delivered.toString()
            failedCount.text = failed.toString()
            todayCount.text = sent.toString()
            weekCount.text = (sent * 7).toString()
            monthCount.text = (sent * 30).toString()
        }
    }
}
