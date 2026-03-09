package com.focusghostmode.ui.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.focusghostmode.R
import com.focusghostmode.databinding.ActivityOnboardingBinding
import com.focusghostmode.ui.home.MainActivity
import com.focusghostmode.utils.GhostPreferences

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: GhostPreferences

    private val requestPhonePermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updatePermissionUI() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Setup Permissions"

        val isRevisit = prefs.hasCompletedOnboarding.also {
            prefs = GhostPreferences(this)
        }
        if (prefs.hasCompletedOnboarding) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        setupPermissionCards()
        updatePermissionUI()

        binding.btnContinue.setOnClickListener {
            prefs.hasCompletedOnboarding = true
            if (prefs.hasCompletedOnboarding) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    private fun setupPermissionCards() {
        // Notification Listener
        binding.btnGrantNotifListener.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Do Not Disturb
        binding.btnGrantDnd.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        // Phone state + contacts
        binding.btnGrantPhone.setOnClickListener {
            val permissions = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS
            )
            requestPhonePermission.launch(permissions.toTypedArray())
        }
    }

    private fun updatePermissionUI() {
        val notifListenerOk = isNotificationListenerEnabled()
        val dndOk = isDndGranted()
        val phoneOk = isPhonePermissionGranted()

        setPermissionStatus(
            binding.ivNotifListenerStatus,
            binding.tvNotifListenerStatus,
            binding.btnGrantNotifListener,
            notifListenerOk
        )
        setPermissionStatus(
            binding.ivDndStatus,
            binding.tvDndStatus,
            binding.btnGrantDnd,
            dndOk
        )
        setPermissionStatus(
            binding.ivPhoneStatus,
            binding.tvPhoneStatus,
            binding.btnGrantPhone,
            phoneOk
        )

        // Enable continue even if some optional permissions are missing
        binding.btnContinue.isEnabled = notifListenerOk
        binding.btnContinue.text = if (notifListenerOk) "Start Using Ghost Mode ✓" else "Grant Notification Access First"
    }

    private fun setPermissionStatus(icon: ImageView, label: TextView, button: Button, granted: Boolean) {
        if (granted) {
            icon.setImageResource(R.drawable.ic_check_circle)
            icon.setColorFilter(getColor(R.color.green_500))
            label.text = "Granted ✓"
            label.setTextColor(getColor(R.color.green_500))
            button.visibility = View.GONE
        } else {
            icon.setImageResource(R.drawable.ic_warning)
            icon.setColorFilter(getColor(R.color.orange_500))
            label.text = "Not granted"
            label.setTextColor(getColor(R.color.orange_500))
            button.visibility = View.VISIBLE
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun isDndGranted(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun isPhonePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
    }
}
