package com.gezimos.katapult.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.provider.CallLog
import android.provider.Telephony
import androidx.core.content.ContextCompat

class DirectBadgeHelper(private val context: Context) {

    companion object {
        const val MUDITA_DIAL = "com.mudita.dial"
        const val MUDITA_MESSAGES = "com.mudita.messages"

        val DIRECT_PACKAGES = setOf(MUDITA_DIAL, MUDITA_MESSAGES)
    }

    var onCountsChanged: (() -> Unit)? = null

    private var missedCallCount = 0
    private var unreadSmsCount = 0

    private var callLogThread: HandlerThread? = null
    private var smsThread: HandlerThread? = null
    private var callLogObserver: ContentObserver? = null
    private var smsObserver: ContentObserver? = null

    fun getCounts(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        if (missedCallCount > 0) map[MUDITA_DIAL] = missedCallCount
        if (unreadSmsCount > 0) map[MUDITA_MESSAGES] = unreadSmsCount
        return map
    }

    private fun hasCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun start() {
        if (hasCallLogPermission()) queryMissedCalls()
        if (hasSmsPermission()) queryUnreadSms()
        registerObservers()
    }

    fun stop() {
        callLogObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        smsObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        callLogThread?.quitSafely()
        smsThread?.quitSafely()
        callLogObserver = null
        smsObserver = null
        callLogThread = null
        smsThread = null
    }

    private fun queryMissedCalls() {
        val count = try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.NEW} = ?",
                arrayOf(CallLog.Calls.MISSED_TYPE.toString(), "1"),
                null,
            )
            val c = cursor?.count ?: 0
            cursor?.close()
            c
        } catch (_: Exception) { 0 }

        if (count != missedCallCount) {
            missedCallCount = count
            onCountsChanged?.invoke()
        }
    }

    private fun queryUnreadSms() {
        val smsCount = try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.READ} = ?",
                arrayOf("0"),
                null,
            )
            val c = cursor?.count ?: 0
            cursor?.close()
            c
        } catch (_: Exception) { 0 }

        val mmsCount = try {
            val cursor = context.contentResolver.query(
                Telephony.Mms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Mms._ID),
                "${Telephony.Mms.READ} = ?",
                arrayOf("0"),
                null,
            )
            val c = cursor?.count ?: 0
            cursor?.close()
            c
        } catch (_: Exception) { 0 }

        val total = smsCount + mmsCount
        if (total != unreadSmsCount) {
            unreadSmsCount = total
            onCountsChanged?.invoke()
        }
    }

    private fun registerObservers() {
        if (hasCallLogPermission()) {
            callLogThread = HandlerThread("CallLogThread").apply { start() }
            callLogObserver = object : ContentObserver(Handler(callLogThread!!.looper)) {
                override fun onChange(selfChange: Boolean) {
                    queryMissedCalls()
                }
            }
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI, true, callLogObserver!!,
            )
        }

        if (hasSmsPermission()) {
            smsThread = HandlerThread("SmsThread").apply { start() }
            smsObserver = object : ContentObserver(Handler(smsThread!!.looper)) {
                override fun onChange(selfChange: Boolean) {
                    queryUnreadSms()
                }
            }
            context.contentResolver.registerContentObserver(
                Telephony.MmsSms.CONTENT_URI, true, smsObserver!!,
            )
        }
    }
}
