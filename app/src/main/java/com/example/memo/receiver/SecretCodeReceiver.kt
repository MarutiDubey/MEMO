package com.example.memo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.memo.MainActivity

/**
 * Receives the dialer secret code *#*#4556#*#*
 * and opens the hidden app.
 *
 * Note: Some Realme UI versions may block this broadcast.
 * If dialer doesn't work, use @@4556 or #*#*4556*#*# typed in any app.
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SECRET_CODE") {
            val i = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(i)
        }
    }
}
