import websockets
import asyncio
import json

# 안드로이드 WebSocket 연결을 저장할 변수
android_websocket = None
# 아두이노와의 WebSocket 연결을 저장할 변수
arduino1_websocket = None
arduino2_websocket = None
arduino3_websocket = None


async def handle_android_message(websocket, path):
    global android_websocket
    android_websocket = websocket
    print("Android connected!")

    try:
        # 안드로이드는 수신만 함
        async for message in websocket:
            print(f"Received message from Android: {message}")
    except websockets.exceptions.ConnectionClosed as e:
        print(f"Android connection closed: {e}")
        android_websocket = None

async def handle_arduino1_connection(websocket, path):
    global arduino1_websocket
    arduino1_websocket = websocket
    print("Arduino1 connected!")

    try:
        # 아두이노와의 연결 유지
        async for message in websocket:
            print(f"Received message from Arduino1: {message}")
            # 여기서 메시지를 처리할 수 있습니다.
            # 예를 들어, 메시지가 "turnOn"이면 특정 동작을 할 수 있습니다.
            # 아두이노로부터 받은 메시지를 안드로이드로 전송
            if android_websocket is not None:
                await android_websocket.send(message)
                print(f"Message forwarded to Android1: {message}")


    except websockets.exceptions.ConnectionClosed as e:
        print(f"Arduino1 connection closed: {e}")
        arduino1_websocket = None

async def handle_arduino2_connection(websocket, path):
    global arduino2_websocket
    arduino2_websocket = websocket
    print("Arduino2 connected!")

    try:
        # 아두이노와의 연결 유지
        async for message in websocket:
            print(f"Received message from Arduino2: {message}")
            # 여기서 메시지를 처리할 수 있습니다.
            # 예를 들어, 메시지가 "turnOn"이면 특정 동작을 할 수 있습니다.
            # 아두이노로부터 받은 메시지를 안드로이드로 전송
            if android_websocket is not None:
                await android_websocket.send(message)
                print(f"Message forwarded to Android2: {message}")

    except websockets.exceptions.ConnectionClosed as e:
        print(f"Arduino2 connection closed: {e}")
        arduino2_websocket = None


async def handle_arduino3_connection(websocket, path):
    global arduino3_websocket
    arduino3_websocket = websocket
    print("Arduino3 connected!")

    try:
        # 아두이노와의 연결 유지
        async for message in websocket:
            print(f"Received message from Arduino3: {message}")
            # 여기서 메시지를 처리할 수 있습니다.
            # 예를 들어, 메시지가 "turnOn"이면 특정 동작을 할 수 있습니다.
            # 아두이노로부터 받은 메시지를 안드로이드로 전송
            if android_websocket is not None:
                await android_websocket.send(message)
                print(f"Message forwarded to Android3: {message}")

    except websockets.exceptions.ConnectionClosed as e:
        print(f"Arduino3 connection closed: {e}")
        arduino3_websocket = None

async def main():
    # 두 개의 WebSocket 서버 생성: 하나는 아두이노용, 하나는 안드로이드용
    android_server = await websockets.serve(handle_android_message, "0.0.0.0", 8080)  # 안드로이드 연결 처리
    arduino_server1 = await websockets.serve(handle_arduino1_connection, "0.0.0.0", 8081)  # 아두이노 연결 처리
    arduino_server2 = await websockets.serve(handle_arduino2_connection, "0.0.0.0", 8082)
    arduino_server3 = await websockets.serve(handle_arduino3_connection, "0.0.0.0", 8083)

    print("WebSocket servers running...")

    # 서버가 종료되지 않도록 대기
    await asyncio.gather(android_server.wait_closed(), arduino_server1.wait_closed())
    await asyncio.gather(android_server.wait_closed(), arduino_server2.wait_closed())
    await asyncio.gather(android_server.wait_closed(), arduino_server3.wait_closed())


asyncio.run(main())
