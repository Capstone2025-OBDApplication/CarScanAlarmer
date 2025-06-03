package com.example.canstone2
import com.example.canstone2.localDB.ObdData

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.InputStream
import java.io.OutputStream

class ManagerOBD {
    private val TAG = "ManagerOBD"

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // 플로우 처리 -> RPM, 속도 데이터 지속적으로 받기 == startObdCommunication()
    fun startObdFlow(context: Context, socket: BluetoothSocket): Flow<ObdData> = callbackFlow {
        Log.d(TAG, "startObdCommunication: 통신을 시도합니다")
        try {
            // OBD2 초기화 명령 보내기
            sendObdInit(context, socket)

            // 센서 데이터 수신 루프
            val job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        val data = getSensorDataFromObd()
                        send(data)
                        delay(500L)
                    } catch (e: Exception) {
                        Log.e(TAG, "OBD 데이터 수신 중 오류 발생", e)
                        cancel("OBD 수신 오류", e)
                    }
                }
            }
            awaitClose {
                Log.d(TAG, "OBD Flow 종료됨. 리소스 정리 중...")
                job.cancel()
                socket.close()
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "OBD 초기화 또는 통신 오류", e)
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    // OBD 로부터 속도, rpm 데이터 받기
    fun getSensorDataFromObd(): ObdData{
        // break 여기 넣거나 마지막에 if 넣거나
        // RPM 데이터 수신
        var RPM_response = sendObdCommand("010C")
        var real_RPM_response = calculateRPM(RPM_response)
        // Thread sleep() 유무 차이는 없을 수도 있음 10~250 사이 값으로 테스트 해보기 rpm, speed 사이의 텀이 없어서 오류 뜰수도

        // 속도 데이터 수신
        var Speed_response = sendObdCommand("010D")
        var real_Speed_response = calculateSpeed(Speed_response)

        // 로그
        Log.d(TAG, "startObdCommunication: 차량 지원 RPM는 $real_RPM_response 입니다")
        Log.d(TAG, "startObdCommunication: 차량 지원 Speed는 $real_Speed_response 입니다")

        // local DB (Room) 데이터 생성
        val data = ObdData( // id는 자동 생성
            rpm = real_RPM_response,
            speed = real_Speed_response,
        )

        return data
    }

    // OBD2 초기화 명령 보내기
    fun sendObdInit(context: Context, socket: BluetoothSocket) {
        inputStream = socket.inputStream
        outputStream = socket.outputStream

        Log.d(TAG, "startObdCommunication: 리셋 명령을 내립니다")
        sendObdCommand("ATZ")  // Reset
        Thread.sleep(1000)                   // 잠시 대기
        Log.d(TAG, "startObdCommunication: Echo off 명령을 내립니다")
        sendObdCommand("ATE0")  // Echo off
        Log.d(TAG, "startObdCommunication: LineFeeds off 명령을 내립니다")
        sendObdCommand("ATL0")  // Linefeeds off
        Log.d(TAG, "startObdCommunication: Spaces off 명령을 내립니다")
        sendObdCommand("ATS0")  // Spaces off
        Log.d(TAG, "startObdCommunication: Headers on 명령을 내립니다")
        sendObdCommand("ATH0")  // Headers on
        Log.d(TAG, "startObdCommunication: protocal auto select")
        sendObdCommand("ATSP0")  // protocol auto select

        // 예시로 차량 지원 PID 읽기 (0100)
        val PID_response = sendObdCommand("0100")
        Log.d(TAG, "startObdCommunication: 차량 지원 PID를 읽었습니다")

        UtilNotifier.showMessageShort(context, "PID: $PID_response")

        Log.d(TAG, "startObdCommunication: 차량 지원 PID는 $PID_response 입니다")
        Log.d(TAG, "startObdCommunication: 차량 데이터를 수신합니다")
    }

    // 명령어 전송
    fun sendObdCommand(command: String): String {
        val cmdWithReturn = "$command\r"
        outputStream?.write(cmdWithReturn.toByteArray())
        outputStream?.flush()

        val response = StringBuilder()
        val buffer = ByteArray(1024)

        while (true) {
            val bytesRead = inputStream!!.read(buffer)
            if (bytesRead > 0) {
                val chunk = String(buffer, 0, bytesRead)
                response.append(chunk)
                if (chunk.contains(">")) { // OBD 통신 응답 구분자
                    break // 응답 끝남
                }
            } else {
                break
            }
        }

        return response.toString()
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")
            .trim() // 공백 제거
    }


    // OBD2 데이터 파싱 유틸리티 메서드

    // RPM 값 계산 (응답: 41 0C XX YY)
    fun calculateRPM(response: String): Float {
        try {
            val bytes = response.chunked(2)
            if (bytes.size >= 4 && bytes[0] == "41" && bytes[1] == "0C") {
                val a = Integer.parseInt(bytes[2], 16)
                val b = Integer.parseInt(bytes[3], 16)
                return (a * 256 + b) / 4f

            }
        } catch (e: Exception) {
            Log.e("OBD2Calc", "RPM 계산 오류", e)
        }
        return 0f
    }

    // 속도 계산 (응답: 41 0D XX)
    fun calculateSpeed(response: String): Int {
        try {
            val bytes = response.chunked(2)
            if (bytes.size >= 3 && bytes[0] == "41" && bytes[1] == "0D") {
                return bytes[2].toInt(16)
            }
        } catch (e: Exception) {
            Log.e("OBD2Calc", "속도 계산 오류", e)
        }
        return 0
    }

}