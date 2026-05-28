package com.spendsense.ui.debug

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.agents.TransactionPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val pipeline: TransactionPipeline
) : ViewModel() {

    fun injectSms(text: String) {
        viewModelScope.launch {
            pipeline.process(text, System.currentTimeMillis())
        }
    }
}

// Sample SMS strings covering different banks and scenarios
private val SAMPLE_SMS = listOf(
    "SBI" to "Your a/c no. XX1234 is debited for Rs.450.00 on 26-05-26. Info: UPI/swiggy/912837@oksbi Bal:Rs.12000",
    "HDFC debit" to "Rs.1200.00 debited from a/c **5678 on 26-05-26:14:32:00 IST. UPI Ref:912837. Info: zomato@oksbi",
    "ICICI credit" to "ICICI Bank Acct XX9012 credited for Rs 50000.00 on 26-May-26; UPI:salary@hdfcbank",
    "Axis" to "INR 800.00 debited from Axis Bank A/c XX3456 for UPI txn. UPI Ref: 654321. Merchant: ola@icici",
    "Paytm" to "Paytm: Rs.150 paid to BigBasket. Txn ID: 112233. Your Paytm wallet bal: Rs.5000",
    "PhonePe" to "PhonePe: Rs.299 sent to netflix@icici on 26-05-26",
    "Google Pay" to "You paid Rs.3500 to OYO Rooms via GPay on 26 May 26",
    "Unknown UPI" to "Rs.500 debited from your account. UPI Ref: 778899@ybl. Avl Bal: Rs.8000",
    "Airtel bill" to "Rs.999 debited from a/c XX1234. Info: airtel@axisbank. Bal: Rs.5000",
    "Rent" to "Your a/c no. XX1234 is debited for Rs.15000.00 on 01-05-26. Info: rent/landlord/9876@oksbi",
)

@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var customSms by remember { mutableStateOf("") }
    var lastInjected by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SMS Injection (Debug)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap any sample to inject it through the full pipeline (Ingest → Parse → Classify → Save → Budget check).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Ping notification — verify permission works before testing budget alerts
        OutlinedButton(
            onClick = { fireTestNotification(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fire test notification (permission check)")
        }

        if (lastInjected.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    "Injected: $lastInjected",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalDivider()
        Text("Sample SMS", style = MaterialTheme.typography.labelLarge)

        SAMPLE_SMS.forEach { (label, sms) ->
            OutlinedButton(
                onClick = {
                    viewModel.injectSms(sms)
                    lastInjected = label
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        sms.take(60) + if (sms.length > 60) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()
        Text("Custom SMS", style = MaterialTheme.typography.labelLarge)

        OutlinedTextField(
            value = customSms,
            onValueChange = { customSms = it },
            label = { Text("Paste any bank SMS here") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(
            onClick = {
                if (customSms.isNotBlank()) {
                    viewModel.injectSms(customSms)
                    lastInjected = "custom"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inject Custom SMS")
        }
    }
}

private fun fireTestNotification(context: Context) {
    val channelId = "debug_test"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Debug Test", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("SpendSense notification works!")
        .setContentText("Permission is granted. Budget alerts will appear here.")
        .setAutoCancel(true)
        .build()

    manager.notify(99999, notification)
}
