package com.focusghostmode.ui.timeline

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusghostmode.R
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.data.model.FocusSession
import com.focusghostmode.data.repository.GhostRepository
import com.focusghostmode.databinding.ActivityTimelineBinding
import com.focusghostmode.utils.ExportUtils
import com.focusghostmode.utils.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimelineBinding
    private lateinit var adapter: TimelineAdapter
    private lateinit var repository: GhostRepository

    private var sessionId: Long = -1L
    private var session: FocusSession? = null
    private var events: List<CapturedEvent> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Session Replay"

        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        repository = GhostRepository.getInstance(applicationContext)

        setupRecyclerView()
        loadData()

        binding.btnExport.setOnClickListener { showExportDialog() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = TimelineAdapter()
        binding.rvTimeline.layoutManager = LinearLayoutManager(this)
        binding.rvTimeline.adapter = adapter
    }

    private fun loadData() {
        if (sessionId == -1L) { finish(); return }
        CoroutineScope(Dispatchers.IO).launch {
            session = repository.getSessionById(sessionId)
            events = repository.getEventsForSession(sessionId)
            withContext(Dispatchers.Main) {
                session?.let { bindSession(it) }
                bindTimeline(events)
            }
        }
    }

    private fun bindSession(session: FocusSession) {
        binding.tvDate.text = TimeUtils.formatFull(session.startTime)
        binding.tvDuration.text = session.durationFormatted()
        binding.tvNotifCount.text = session.notificationCount.toString()
        binding.tvCallCount.text = session.callCount.toString()
        binding.tvMostActive.text = session.mostActiveApp ?: "—"

        // Mini bar chart for notifications vs calls
        val total = (session.notificationCount + session.callCount).coerceAtLeast(1)
        binding.progressNotifs.progress = (session.notificationCount * 100 / total)
        binding.progressCalls.progress = (session.callCount * 100 / total)
    }

    private fun bindTimeline(events: List<CapturedEvent>) {
        if (events.isEmpty()) {
            binding.tvEmptyTimeline.visibility = View.VISIBLE
            binding.rvTimeline.visibility = View.GONE
        } else {
            binding.tvEmptyTimeline.visibility = View.GONE
            binding.rvTimeline.visibility = View.VISIBLE
            adapter.submitList(buildTimelineItems(events))
        }
    }

    /**
     * Inserts date headers between events that span different hours,
     * creating a grouped timeline visual.
     */
    private fun buildTimelineItems(events: List<CapturedEvent>): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()
        var lastHour = -1
        for (event in events) {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = event.timestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            if (hour != lastHour) {
                items.add(TimelineItem.Header(TimeUtils.formatTime(event.timestamp)))
                lastHour = hour
            }
            items.add(TimelineItem.Event(event))
        }
        return items
    }

    private fun showExportDialog() {
        val s = session ?: return
        val options = arrayOf("Export as CSV", "Share as Text Report")
        MaterialAlertDialogBuilder(this)
            .setTitle("Export Session")
            .setItems(options) { _, which ->
                val shareIntent = when (which) {
                    0 -> ExportUtils.exportToCsv(this, s, events)
                    else -> ExportUtils.exportAsText(this, s, events)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Session Log"))
            }
            .show()
    }

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Timeline item types for the RecyclerView
// ─────────────────────────────────────────────────────────────────────────────

sealed class TimelineItem {
    data class Header(val label: String) : TimelineItem()
    data class Event(val event: CapturedEvent) : TimelineItem()
}
