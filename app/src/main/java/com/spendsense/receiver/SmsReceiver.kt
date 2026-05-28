package com.spendsense.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.spendsense.agents.TransactionPipeline
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires on every incoming SMS. Registered in AndroidManifest for SMS_RECEIVED.
 *
 * Android gives BroadcastReceivers ~10 seconds of execution.
 * goAsync() extends this while still not blocking the main thread.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var pipeline: TransactionPipeline

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val result = goAsync()
        scope.launch {
            try {
                messages.forEach { smsMessage ->
                    val body = smsMessage.messageBody ?: return@forEach
                    pipeline.process(body, smsMessage.timestampMillis)
                }
            } finally {
                result.finish()
            }
        }
    }
}
