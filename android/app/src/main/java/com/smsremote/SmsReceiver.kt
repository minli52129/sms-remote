package com.smsremote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages != null && messages.isNotEmpty()) {
                val sender = messages[0].displayOriginatingAddress
                val body = messages.joinToString("") { it.messageBody ?: "" }
                val timestamp = messages[0].timestampMillis

                // 获取 SIM 卡信息
                val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    messages[0].subscriptionId
                } else {
                    -1
                }
                val simSlot = SimManager.getSimSlotBySubscriptionId(context, subscriptionId)

                // 上报到服务器
                val webSocketManager = WebSocketManager(context)
                webSocketManager.reportSmsReceived(
                    from = sender,
                    content = body,
                    timestamp = timestamp,
                    simSlot = simSlot
                )
            }
        }
    }
}
