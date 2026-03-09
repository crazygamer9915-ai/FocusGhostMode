package com.focusghostmode.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.focusghostmode.data.model.CapturedEvent
import com.focusghostmode.data.repository.GhostRepository
import com.focusghostmode.utils.GhostPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver that captures phone call events during Ghost Mode.
 * Logs incoming and missed calls without interfering with the call itself.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    private val TAG = "PhoneStateReceiver"

    // Track ringing state to detect missed calls
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var lastIncomingNumber: String? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val prefs = GhostPreferences(context)
        if (!prefs.isGhostModeActive) return

        val sessionId = prefs.activeSessionId
        if (sessionId == -1L) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Phone state: $state, number: $incomingNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                lastState = TelephonyManager.CALL_STATE_RINGING
                lastIncomingNumber = incomingNumber
                // Capture incoming call event
                captureCall(context, sessionId, incomingNumber, CapturedEvent.CallType.INCOMING)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    lastState = TelephonyManager.CALL_STATE_OFFHOOK
                    // Call was answered — already logged as incoming
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    // Phone went from ringing → idle without offhook = missed call
                    captureCall(context, sessionId, lastIncomingNumber, CapturedEvent.CallType.MISSED)
                }
                lastState = TelephonyManager.CALL_STATE_IDLE
                lastIncomingNumber = null
            }
        }
    }

    private fun captureCall(
        context: Context,
        sessionId: Long,
        number: String?,
        callType: CapturedEvent.CallType
    ) {
        val repository = GhostRepository.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val contactName = number?.let { lookupContact(context, it) }
            repository.captureCall(
                sessionId = sessionId,
                number = number,
                name = contactName,
                callType = callType
            )
            Log.d(TAG, "Captured $callType call from $contactName ($number)")
        }
    }

    private fun lookupContact(context: Context, phoneNumber: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            Log.e("PhoneStateReceiver", "Error looking up contact", e)
            null
        }
    }
}
