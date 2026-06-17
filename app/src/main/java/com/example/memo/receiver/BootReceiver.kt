package com.example.memo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.memo.service.ClipboardMonitorService

/**
 * Restarts the ClipboardMonitorService after the phone reboots.
 * This means clipboard tracking resumes automatically without needing to open the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val serviceIntent = Intent(context, ClipboardMonitorService::class.java)
            context.startService(serviceIntent)
        }
    }
}
