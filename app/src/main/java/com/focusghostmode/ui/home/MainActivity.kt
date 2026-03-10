package com.focusghostmode.ui.home

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.focusghostmode.R
import com.focusghostmode.databinding.ActivityMainBinding
import com.focusghostmode.service.GhostForegroundService
import com.focusghostmode.ui.history.HistoryActivity
import com.focusghostmode.ui.onboarding.OnboardingActivity
import com.focusghostmode.ui.timeline.TimelineActivity
import com.focusghostmode.utils.GhostPreferences
import com.focusghostmode.utils.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var prefs: GhostPreferences

    // Permission launcher for Android 13+ notifications
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Snackbar.make(binding.root, "Notification permission needed for full functionality", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        prefs = GhostPreferences(this)

        // Show onboarding if first run
        if (!prefs.hasCompletedOnboarding) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setupUI()
        observeViewModel()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }

    // ── UI Setup ──────────────────────────────────────────────────────────────

    private fun setupUI() {
        binding.btnGhostToggle.setOnClickListener {
            if (prefs.isGhostModeActive) {
                stopGhostMode()
            } else {
                startGhostMode()
            }
        }

        binding.btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.isGhostModeActive.observe(this) { active ->
            updateGhostModeUI(active)
        }

        viewModel.elapsedMillis.observe(this) { millis ->
            if (millis > 0) {
                binding.tvTimer.text = TimeUtils.formatDuration(millis)
                binding.tvTimer.visibility = View.VISIBLE
            } else {
                binding.tvTimer.visibility = View.GONE
            }
        }

        viewModel.justEndedSessionId.observe(this) { sessionId ->
            if (sessionId != null) {
                openTimeline(sessionId)
                viewModel.clearJustEndedSession()
            }
        }
    }

    private fun updateGhostModeUI(active: Boolean) {
        if (active) {
            binding.tvAppTitle.text = getString(R.string.ghost_mode_active)
            binding.tvSubtitle.text = getString(R.string.ghost_mode_subtitle_active)
            binding.btnGhostToggle.text = getString(R.string.end_ghost_mode)
            binding.btnGhostToggle.setBackgroundColor(getColor(R.color.red_500))
            binding.ivGhostIcon.setImageResource(R.drawable.ic_ghost_active)
            binding.ghostActiveIndicator.visibility = View.VISIBLE
            binding.tvTimer.visibility = View.VISIBLE
            // Pulse animation on ghost icon
            val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
            binding.ivGhostIcon.startAnimation(pulse)
        } else {
            binding.tvAppTitle.text = getString(R.string.app_name)
            binding.tvSubtitle.text = getString(R.string.ghost_mode_subtitle_idle)
            binding.btnGhostToggle.text = getString(R.string.activate_ghost_mode)
            binding.btnGhostToggle.setBackgroundColor(getColor(R.color.purple_700))
            binding.ivGhostIcon.setImageResource(R.drawable.ic_ghost)
            binding.ghostActiveIndicator.visibility = View.GONE
            binding.tvTimer.visibility = View.GONE
            binding.ivGhostIcon.clearAnimation()
        }
    }

    // ── Ghost Mode Control ────────────────────────────────────────────────────

    private fun startGhostMode() {
    if (!isNotificationListenerEnabled()) {
        showNotificationListenerDialog()
        return
    }
    launchGhostMode()
}

private fun launchGhostMode() {
    startForegroundService(this, GhostForegroundService.startIntent(this))
    prefs.isGhostModeActive = true
    prefs.sessionStartTime = System.currentTimeMillis()
    viewModel.setGhostModeActive(true)
    Snackbar.make(binding.root, "👻 Ghost Mode activated. Focus up!", Snackbar.LENGTH_SHORT).show()
}
    private fun stopGhostMode() {
        val sessionId = prefs.activeSessionId
        prefs.lastEndedSessionId = sessionId
        startService(GhostForegroundService.stopIntent(this))
        prefs.isGhostModeActive = false
        viewModel.setGhostModeActive(false)
        // Navigate to timeline after short delay for DB write to complete
        binding.root.postDelayed({
            if (sessionId != -1L) openTimeline(sessionId)
        }, 800)
    }

    private fun openTimeline(sessionId: Long) {
        val intent = Intent(this, TimelineActivity::class.java).apply {
            putExtra(TimelineActivity.EXTRA_SESSION_ID, sessionId)
        }
        startActivity(intent)
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun checkPermissions() {
        // Android 13+ post notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }

    private fun showNotificationListenerDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Notification Access Required")
            .setMessage("Focus Ghost Mode needs Notification Access to silently capture notifications during focus sessions.\n\nTap 'Open Settings', find 'Focus Ghost Mode', and enable it.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Options Menu ──────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_permissions -> {
                startActivity(Intent(this, OnboardingActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
