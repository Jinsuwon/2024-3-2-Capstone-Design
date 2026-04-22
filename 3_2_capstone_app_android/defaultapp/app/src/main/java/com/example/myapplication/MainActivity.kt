package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()
    private lateinit var notificationManager: NotificationManager
    private val channelId = "sensor_alert_channel"
    private val channelName = "Sensor Alert Channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connectButton)
        val openPageButton = findViewById<Button>(R.id.button_open_page)
        val openThirdPageButton = findViewById<Button>(R.id.button_open_third_page) // ThirdActivity로 이동하는 버튼 참조

        // 푸시 알림 매니저 초기화
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 안드로이드 8.0 이상에서 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        connectWebSocket()

        connectButton.setOnClickListener {
            connectWebSocket()
        }

        // SecondActivity로 이동
        openPageButton.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }

        // ThirdActivity로 이동
        openThirdPageButton.setOnClickListener {
            val intent = Intent(this, ThirdActivity::class.java)
            startActivity(intent)
        }
    }

    private fun connectWebSocket() {
        val request = Request.Builder().url("ws://192.168.0.155:8080").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                super.onOpen(webSocket, response)
                Log.d("WebSocket", "Connected to WebSocket Server")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                Log.d("WebSocket", "Received: $text")

                try {
                    val jsonObject = JSONObject(text)
                    val deviceId = jsonObject.optString("device_id", "Unknown")
                    val percent = jsonObject.optDouble("percent", Double.NaN)
                    val count = jsonObject.getInt("count")

                    val currentTimestamp = getCurrentTimestamp()

                    val dbHelper = AlarmDatabaseHelper(this@MainActivity)
                    dbHelper.saveAlarm(deviceId, count, currentTimestamp)


                    val saros = when (deviceId) {
                        "Arduino_1" -> "1사로"
                        "Arduino_2" -> "2사로"
                        "Arduino_3" -> "3사로"
                        else -> "알 수 없는 사로"
                    }

                    if (!percent.isNaN() && percent <= 5) {
                        val title = "휴지 잔량 알림"
                        val message = "$saros: 잔량이 5% 이하입니다."
                        val notificationId = when (deviceId) {
                            "Arduino_1" -> 1
                            "Arduino_2" -> 2
                            "Arduino_3" -> 3
                            else -> 0
                        }

                        showNotification(title, message, notificationId)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error parsing JSON: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                super.onFailure(webSocket, t, response)
                Log.e("WebSocket", "Error: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                webSocket.close(1000, null)
                Log.d("WebSocket", "WebSocket Closing: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Log.d("WebSocket", "WebSocket Closed: $reason")
            }
        })
    }

    private fun getCurrentTimestamp(): String {
        val currentTime = Calendar.getInstance().time
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(currentTime)
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
