package com.smsremote

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

data class SimInfo(
    val slot: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val number: String?
)

object SimManager {

    @SuppressLint("MissingPermission")
    fun getSimCards(context: Context): List<SimInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return emptyList()

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as? SubscriptionManager ?: return emptyList()

        val list: List<SubscriptionInfo>? = try {
            subscriptionManager.activeSubscriptionInfoList
        } catch (e: Exception) {
            null
        }

        return list?.map { info ->
            SimInfo(
                slot = info.simSlotIndex,
                subscriptionId = info.subscriptionId,
                carrierName = info.carrierName?.toString() ?: "未知",
                number = readNumber(info)
            )
        } ?: emptyList()
    }

    /**
     * 读取 SIM 卡号码。
     * getNumber() 从 API 29 起公开，低版本为隐藏 API，故使用反射兜底。
     */
    private fun readNumber(info: SubscriptionInfo): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.number?.let { return it }
        }
        return try {
            val method = SubscriptionInfo::class.java.getMethod("getNumber")
            val value = method.invoke(info) as? String
            value?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    fun getSubscriptionIdBySlot(context: Context, slot: Int): Int {
        return getSimCards(context).find { it.slot == slot }?.subscriptionId ?: -1
    }

    fun getSimSlotBySubscriptionId(context: Context, subscriptionId: Int): Int {
        if (subscriptionId < 0) return 0
        return getSimCards(context).find { it.subscriptionId == subscriptionId }?.slot ?: 0
    }

    @SuppressLint("MissingPermission")
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
