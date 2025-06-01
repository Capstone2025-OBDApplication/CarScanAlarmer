package com.example.canstone2

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.InputStream
import java.io.OutputStream

object ManagerOBD {
    private const val TAG = "ManagerOBD"

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

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