package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SecondActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()
    private lateinit var notificationManager: NotificationManager
    private val channelId = "sensor_alert_channel"
    private val channelName = "Sensor Alert Channel"

    // TextView 변수 선언
    private lateinit var blockArduino1: TextView
    private lateinit var blockArduino2: TextView
    private lateinit var blockArduino3: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second) // 새 페이지의 레이아웃 설정

        // TextView 초기화
        blockArduino1 = findViewById(R.id.arduino1TextView)
        blockArduino2 = findViewById(R.id.arduino2TextView)
        blockArduino3 = findViewById(R.id.arduino3TextView)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // WebSocket 연결
        connectWebSocket()
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
                    // JSON 메시지 파싱
                    val jsonObject = JSONObject(text)
                    val deviceId = jsonObject.optString("device_id", "Unknown")
                    var percent = jsonObject.optDouble("percent", Double.NaN)

                    val count = jsonObject.getInt("count")

                    val currentTimestamp = getCurrentTimestamp()

                    val dbHelper = AlarmDatabaseHelper(this@SecondActivity)
                    dbHelper.saveAlarm(deviceId, count, currentTimestamp)

                    val saros = when (deviceId) {
                        "Arduino_1" -> "1사로"
                        "Arduino_2" -> "2사로"
                        "Arduino_3" -> "3사로"
                        else -> "알 수 없는 사로"
                    }

                    // 음수 값 처리: 음수일 경우 0으로 변경
                    if (percent < 0) percent = 0.0
                    if (percent > 100) percent = 100.0
                    // 퍼센트가 5 이하일 경우 알림 표시
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

                    // UI 업데이트
                    runOnUiThread {
                        when (deviceId) {
                            "Arduino_1" -> {
                                blockArduino1.text = "변기 1: ${percent.toInt()}%"
                                updateBlockColor(blockArduino1, percent)
                            }
                            "Arduino_2" -> {
                                blockArduino2.text = "변기 2: ${percent.toInt()}%"
                                updateBlockColor(blockArduino2, percent)
                            }
                            "Arduino_3" -> {
                                blockArduino3.text = "변기 3: ${percent.toInt()}%"
                                updateBlockColor(blockArduino3, percent)
                            }
                            else -> Log.d("WebSocket", "Unknown device: $deviceId")
                        }
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

    // 블록 색상 업데이트 함수
    private fun updateBlockColor(block: TextView, percent: Double) {
        when {
            percent <= 5 -> block.setBackgroundColor(Color.RED) // 빨강색
            percent in 6.0..20.0 -> block.setBackgroundColor(Color.YELLOW) // 노란색
            percent > 20 -> block.setBackgroundColor(Color.GRAY) // 회색
            else -> block.setBackgroundColor(Color.WHITE) // 기본 색상
        }
    }

    private fun getCurrentTimestamp(): String {
        val currentTime = Calendar.getInstance().time
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(currentTime)
    }

    // 알림 표시 함수
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
