package com.smsremote

import android.content.Context
import android.content.SharedPreferences
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class WebSocketManager(private val context: Context) {

    private var socket: Socket? = null
    private val prefs: SharedPreferences = context.getSharedPreferences("sms_remote", Context.MODE_PRIVATE)

    companion object {
        // 修改为你的服务器地址
        private const val SERVER_URL = "http://120.48.127.198:3000"
        private const val AUTH_TOKEN = "change-me-to-a-secure-token"
        private const val DEVICE_ID_KEY = "device_id"
    }

    fun connect() {
        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 5000
                timeout = 10000
            }

            socket = IO.socket(SERVER_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                // 发送认证
                val deviceId = getDeviceId()
                socket?.emit("auth", JSONObject().apply {
                    put("token", AUTH_TOKEN)
                    put("deviceId", deviceId)
                })
            }

            socket?.on("auth_success") {
                // 认证成功，发送 SIM 卡信息
                sendSimInfo()
            }

            socket?.on("send_sms") { args ->
                val data = args[0] as JSONObject
                val to = data.getString("to")
                val content = data.getString("content")
                val msgId = data.getString("msgId")
                val simSlot = data.optInt("simSlot", 0)

                sendSms(to, content, msgId, simSlot)
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                // 断开连接，尝试重连
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                // 连接错误
            }

            socket?.connect()

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    private fun getDeviceId(): String {
        var deviceId = prefs.getString(DEVICE_ID_KEY, null)
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(DEVICE_ID_KEY, deviceId).apply()
        }
        return deviceId
    }

    private fun sendSimInfo() {
        val sims = SimManager.getSimCards(context)
        val simsArray = org.json.JSONArray()
        sims.forEach { sim ->
            simsArray.put(JSONObject().apply {
                put("slot", sim.slot)
                put("subscriptionId", sim.subscriptionId)
                put("carrierName", sim.carrierName)
                put("number", sim.number ?: "")
            })
        }

        socket?.emit("sim_info", JSONObject().apply {
            put("sims", simsArray)
        })
    }

    private fun sendSms(to: String, content: String, msgId: String, simSlot: Int) {
        try {
            val subscriptionId = SimManager.getSubscriptionIdBySlot(context, simSlot)
            val success = SimManager.sendSms(context, subscriptionId, to, content)

            socket?.emit("sms_result", JSONObject().apply {
                put("msgId", msgId)
                put("success", success)
                put("error", if (!success) "发送失败" else "")
            })
        } catch (e: Exception) {
            socket?.emit("sms_result", JSONObject().apply {
                put("msgId", msgId)
                put("success", false)
                put("error", e.message ?: "未知错误")
            })
        }
    }

    fun reportSmsReceived(from: String, content: String, timestamp: Long, simSlot: Int) {
        socket?.emit("sms_received", JSONObject().apply {
            put("from", from)
            put("content", content)
            put("timestamp", timestamp)
            put("simSlot", simSlot)
        })
    }
}
