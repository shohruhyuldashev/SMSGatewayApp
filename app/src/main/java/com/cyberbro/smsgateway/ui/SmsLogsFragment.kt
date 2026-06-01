package com.cyberbro.smsgateway.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cyberbro.smsgateway.R
import com.cyberbro.smsgateway.storage.SmsDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsLogsFragment : Fragment() {
    private lateinit var logsText: TextView
    private var currentFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_sms_logs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logsText = view.findViewById(R.id.smsLogsText)

        view.findViewById<TextView>(R.id.filterAllButton).setOnClickListener {
            currentFilter = null; showMessages(null)
        }
        view.findViewById<TextView>(R.id.filterSentButton).setOnClickListener {
            currentFilter = "sent"; showMessages("sent")
        }
        // "Done" button filters "delivered"
        view.findViewById<TextView>(R.id.filterDeliveredButton).setOnClickListener {
            currentFilter = "delivered"; showMessages("delivered")
        }
        view.findViewById<TextView>(R.id.filterFailedButton).setOnClickListener {
            currentFilter = "failed"; showMessages("failed")
        }

        showMessages(null)
    }

    override fun onResume() {
        super.onResume()
        showMessages(currentFilter)
    }

    private fun showMessages(status: String?) {
        lifecycleScope.launch {
            val dao = SmsDatabase.getInstance(requireContext()).smsTaskDao()
            val tasks = if (status == null) {
                dao.getAllTasks()
            } else {
                dao.getTasksByStatus(status)
            }

            logsText.text = if (tasks.isEmpty()) {
                getString(R.string.sms_logs_empty)
            } else {
                tasks.joinToString(separator = "\n─────────────────\n") { task ->
                    val time      = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(task.createdAt))
                    val taskStatus = task.status.uppercase()
                    val prio      = "[${task.priority.uppercase()}]"
                    val sim       = "SIM${task.simSlot}"
                    "$time  $taskStatus  $prio  $sim\n${task.phoneNumber}\n${task.message}"
                }
            }
        }
    }
}
