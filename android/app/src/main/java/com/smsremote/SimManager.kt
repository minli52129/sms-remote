package com.smsremote

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi

data class SimInfo(
    val slot: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val number: String?
)

object SimManager {

    fun getSimCards(context: Context): List<SimInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return emptyList()
        }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager

        return try {
            subscriptionManager.activeSubscriptionInfoList?.map { info ->
                SimInfo(
                    slot = info.simSlotIndex,
                    subscriptionId = info.subscriptionId,
                    carrierName = info.carrierName?.toString() ?: "未知",
                    number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        info.number
                    } else {
                        @Suppress("DEPRECATION")
                        info.number
                    }
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun getSubscriptionIdBySlot(context: Context, slot: Int): Int {
        val simCards = getSimCards(context)
        return simCards.find { it.slot == slot }?.subscriptionId ?: -1
    }

    fun getSimSlotBySubscriptionId(context: Context, subscriptionId: Int): Int {
        val simCards = getSimCards(context)
        return simCards.find { it.subscriptionId == subscriptionId }?.slot ?: 0
    }

    fun sendSms(context: Context, subscriptionId: Int, phoneNumber: String, content: String): Boolean {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
                    .createForSubscriptionId(subscriptionId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
            smsManager.sendTextMessage(phoneNumber, null, content, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
