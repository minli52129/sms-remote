package com.smsremote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        // 通过反射获取 subscriptionId（API 19+ 公开，SDK 中部分版本隐藏）
        val subscriptionId = messages[0].getSubscriptionIdCompat()
        val simSlot = SimManager.getSimSlotBySubscriptionId(context, subscriptionId)

        WebSocketManager(context).reportSmsReceived(
            from = sender,
            content = body,
            timestamp = timestamp,
            simSlot = simSlot
        )
    }
}

/**
 * 兼容获取 SmsMessage 的 subscriptionId。
 * 该方法在不同 Android 版本上的可见性不一致，故使用反射兜底。
 */
private fun android.telephony.SmsMessage.getSubscriptionIdCompat(): Int {
    return try {
        val method = android.telephony.SmsMessage::class.java
            .getMethod("getSubscriptionId")
        method.invoke(this) as? Int ?: -1
    } catch (e: Exception) {
        -1
    }
}
