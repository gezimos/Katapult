package com.gezimos.katapult.service

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.provider.CallLog
import android.provider.Telephony
import androidx.core.content.ContextCompat
import java.util.concurrent.CopyOnWriteArrayList

class DirectBadgeHelper private constructor(private val context: Context) {

    companion object {
        const val MUDITA_DIAL = "com.mudita.dial"
        const val MUDITA_MESSAGES = "com.mudita.messages"

        val DIRECT_PACKAGES = setOf(MUDITA_DIAL, MUDITA_MESSAGES)

        private const val MISSED_SELECTION =
            "((${CallLog.Calls.TYPE} IN (?, ?) AND ${CallLog.Calls.NEW} = 1) OR " +
                "(${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.IS_READ} = 0))"

        private val MISSED_ARGS = arrayOf(
            CallLog.Calls.MISSED_TYPE.toString(),
            CallLog.Calls.REJECTED_TYPE.toString(),
            CallLog.Calls.MISSED_TYPE.toString(),
        )

        private const val DEBOUNCE_MS = 300L

        @Volatile
        private var INSTANCE: DirectBadgeHelper? = null

        fun getInstance(context: Context): DirectBadgeHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DirectBadgeHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    private var missedCallCount = 0
    private var unreadSmsCount = 0
    private var refCount = 0

    private var callLogThread: HandlerThread? = null
    private var smsThread: HandlerThread? = null
    private var callLogHandler: Handler? = null
    private var smsHandler: Handler? = null
    private var callLogObserver: ContentObserver? = null
    private var smsObserver: ContentObserver? = null

    private val callLogQuery = Runnable { queryMissedCalls() }
    private val smsQuery = Runnable { queryUnreadSms() }

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyChanged() {
        listeners.forEach { it.invoke() }
    }

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

    fun hasWriteCallLogPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun hasMissedCalls(): Boolean = missedCallCount > 0

    fun clearMissedCalls() {
        if (!hasWriteCallLogPermission()) return
        try {
            val values = ContentValues(2).apply {
                put(CallLog.Calls.NEW, 0)
                put(CallLog.Calls.IS_READ, 1)
            }
            context.contentResolver.update(
                CallLog.Calls.CONTENT_URI,
                values,
                MISSED_SELECTION,
                MISSED_ARGS,
            )
        } catch (_: Exception) {}
        queryMissedCalls()
    }

    @Synchronized
    fun acquire() {
        refCount++
        registerObservers()
        if (hasCallLogPermission()) queryMissedCalls()
        if (hasSmsPermission()) queryUnreadSms()
    }

    @Synchronized
    fun release() {
        refCount = (refCount - 1).coerceAtLeast(0)
        if (refCount > 0) return
        callLogHandler?.removeCallbacks(callLogQuery)
        smsHandler?.removeCallbacks(smsQuery)
        callLogObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        smsObserver?.let { context.contentResolver.unregisterContentObserver(it) }
        callLogThread?.quitSafely()
        smsThread?.quitSafely()
        callLogObserver = null
        smsObserver = null
        callLogHandler = null
        smsHandler = null
        callLogThread = null
        smsThread = null
    }

    private fun queryMissedCalls() {
        val count = try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                MISSED_SELECTION,
                MISSED_ARGS,
                null,
            )
            val c = cursor?.count ?: 0
            cursor?.close()
            c
        } catch (_: Exception) { 0 }

        if (count != missedCallCount) {
            missedCallCount = count
            notifyChanged()
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
            notifyChanged()
        }
    }

    private fun registerObservers() {
        if (callLogObserver == null && hasCallLogPermission()) {
            val thread = HandlerThread("CallLogThread").apply { start() }
            val handler = Handler(thread.looper)
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    handler.removeCallbacks(callLogQuery)
                    handler.postDelayed(callLogQuery, DEBOUNCE_MS)
                }
            }
            callLogThread = thread
            callLogHandler = handler
            callLogObserver = observer
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI, true, observer,
            )
        }

        if (smsObserver == null && hasSmsPermission()) {
            val thread = HandlerThread("SmsThread").apply { start() }
            val handler = Handler(thread.looper)
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    handler.removeCallbacks(smsQuery)
                    handler.postDelayed(smsQuery, DEBOUNCE_MS)
                }
            }
            smsThread = thread
            smsHandler = handler
            smsObserver = observer
            context.contentResolver.registerContentObserver(
                Telephony.MmsSms.CONTENT_URI, true, observer,
            )
        }
    }
}
