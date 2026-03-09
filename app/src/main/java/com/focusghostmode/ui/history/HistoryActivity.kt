package com.focusghostmode.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusghostmode.data.model.FocusSession
import com.focusghostmode.data.repository.GhostRepository
import com.focusghostmode.databinding.ActivityHistoryBinding
import com.focusghostmode.ui.timeline.TimelineActivity
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var repository: GhostRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Focus History"

        repository = GhostRepository.getInstance(applicationContext)

        adapter = HistoryAdapter(
            onSessionClick = { session -> openTimeline(session) },
            onDeleteClick = { session -> deleteSession(session) }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        observeSessions()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun observeSessions() {
        lifecycleScope.launch {
            repository.getAllSessionsFlow().collect { sessions ->
                val completedSessions = sessions.filter { !it.isActive }
                if (completedSessions.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    adapter.submitList(completedSessions)
                }
            }
        }
    }

    private fun openTimeline(session: FocusSession) {
        val intent = Intent(this, TimelineActivity::class.java).apply {
            putExtra(TimelineActivity.EXTRA_SESSION_ID, session.id)
        }
        startActivity(intent)
    }

    private fun deleteSession(session: FocusSession) {
        lifecycleScope.launch {
            repository.deleteSession(session.id)
        }
    }
}
