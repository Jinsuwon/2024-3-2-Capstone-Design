#include <Arduino.h>
#include <SPI.h>
#include <WiFiNINA.h>
#include <WebSocketsClient.h>
#include <Arduino_LSM6DS3.h>

// WiFi 설정 (여기에 자신의 네트워크 정보를 입력하세요)
#define WIFI_SSID "ESW"     // WiFi SSID
#define WIFI_PASS "85428542" // WiFi 비밀번호

// 고유 장치 식별자 설정
#define DEVICE_ID "Arduino_3" // 각 Arduino에 고유 ID를 부여

// WiFi 및 WebSocket 관련 변수 설정
int status = WL_IDLE_STATUS;
WiFiClient client;
WebSocketsClient webSocket;

// 센서 값 변수
float acc_x, acc_y, acc_z;
float acc_pit, acc_roll;
float lenght;
float a = 29.0;
float b = (24 - 8) / 2;
float percent;
int num;
float now_percent = 0.0;
float last_percent = 0.0;
int count = 0;

unsigned long lastStatusSendTime = 0;

// WebSocket 이벤트 처리 함수
void webSocketEvent(WStype_t type, uint8_t *payload, size_t length) {
  switch (type) {
    case WStype_CONNECTED:
      Serial.println("[WSc] Connected to WebSocket Server");
      break;
    case WStype_DISCONNECTED:
      Serial.println("[WSc] Disconnected from server");
      break;
    default:
      break;
  }
}

void setup() {
  Serial.begin(115200);

  // WiFi 연결 시도
  while (status != WL_CONNECTED) {
    Serial.print("SSID에 연결 시도 중: ");
    Serial.println(WIFI_SSID);
    
    // WPA/WPA2 네트워크 연결
    status = WiFi.begin(WIFI_SSID, WIFI_PASS);
    
    // 연결 대기 (10초)
    delay(5000);
  }

  Serial.println("WiFi에 연결됨!");

  // IP 주소 출력
  IPAddress ip = WiFi.localIP();
  Serial.print("IP 주소: ");
  Serial.println(ip);

  // WebSocket 서버 주소, 포트 설정
  webSocket.begin("192.168.0.155", 8083);  // WebSocket 서버 IP와 포트 설정

  // WebSocket 이벤트 핸들러 등록
  webSocket.onEvent(webSocketEvent);

  // 연결 실패 시 5초마다 재시도
  webSocket.setReconnectInterval(5000);

   // 가속도 센서 초기화
  if (!IMU.begin()) { // LSM6DS3 센서 시작
    Serial.println("LSM6DS3센서 오류!");
    while (1);
  }
}

void loop() {
  unsigned long currentTime = millis();

  // 가속도 센서 데이터 읽기 및 각도 계산
  if (IMU.accelerationAvailable()) {
    IMU.readAcceleration(acc_x, acc_y, acc_z);

    // 각도 계산
    acc_pit = RAD_TO_DEG * atan(acc_x / sqrt(acc_y * acc_y + acc_z * acc_z));
    acc_roll = RAD_TO_DEG * atan(acc_y / sqrt(acc_x * acc_x + acc_z * acc_z));
    lenght = a * sin(DEG_TO_RAD * acc_pit) + 4;
    percent = (lenght / b) * (-100);
    num = 2;

    if (percent < 0){
      percent = 0;
    }

    Serial.print("lenght = ");
    Serial.print(lenght);
    Serial.print(", percent = ");
    Serial.println(percent);
  }


  // 10초마다 센서 데이터 서버로 전송
  if (currentTime - lastStatusSendTime >= 10000) {
    lastStatusSendTime = currentTime;


    now_percent = percent;

    if (last_percent - now_percent < 0){
      count += 1;
      last_percent = now_percent;
    }
    else{
      last_percent = now_percent;
    }

    // 고유 장치 ID 포함한 센서 데이터 전송
    String message = String("{\"device_id\": \"") + DEVICE_ID + 
                 String("\", \"lenght\": ") + String(lenght, 2) +
                 String(", \"percent\": ") + String(percent, 2) +
                 String(", \"count\": ") + String(count) +
                 String(", \"floor\": ") + String(num) +
                 String(", \"port\": \"") + WiFi.localIP().toString() + ":8083\"}";
    webSocket.sendTXT(message);
  }

  // WebSocket 이벤트 루프
  webSocket.loop();
}