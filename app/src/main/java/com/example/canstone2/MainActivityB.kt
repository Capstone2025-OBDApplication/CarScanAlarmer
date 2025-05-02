package com.example.canstone2

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import android.widget.TextView
import java.io.IOException
import java.util.*

class MainActivityB : AppCompatActivity() {

    private val TAG = "MainActivityB"

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceListView: ListView
    private lateinit var scanButton: Button

    private val deviceList = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()

    // UI 요소 선언
    private lateinit var tvRpmValue: TextView
    private lateinit var tvSpeedValue: TextView

    private var bluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainb)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // UI 초기화
        deviceListView = findViewById(R.id.deviceListView)
        scanButton = findViewById(R.id.scanButton)
        tvRpmValue = findViewById(R.id.tvRpmValue)
        tvSpeedValue = findViewById(R.id.tvSpeedValue)

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // !! 는 not Null을 의미
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } else {
            // 권한이 없을 경우 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                REQUEST_PERMISSIONS
            )
        }


        checkPermissions()

        scanButton.setOnClickListener {
            scanDevices()
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            connectToDevice(device)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // 권한 요청
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun scanDevices() {
        deviceList.clear()
        deviceNames.clear()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        pairedDevices?.forEach { device ->
            deviceList.add(device)
            deviceNames.add(device.name ?: "알 수 없는 장치 (${device.address})")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        deviceListView.adapter = adapter

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "페어링된 장치가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission") // 권한 체크 했음에도 API 버전 문제 때문에 코드 상으로만 경고 무시
    private fun connectToDevice(device: BluetoothDevice) {
        // 권한 체크 추가
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "connectToDevice: Bluetooth 권한을 확인 했습니다.")
        Thread {
            try {
                val uuid = device.uuids?.get(0)?.uuid
                    // Smart-OBD II UUID: RFCOMM (00000003-0000-1000-8000-00805f9b34fb)
//                    ?: UUID.fromString("00000003-0000-1000-8000-00805f9b34fb") // RFCOMM UUID
                    ?: UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // 기본 SPP UUID //Serial Port

                // cancelDiscovery 전에 권한 체크
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {  // S = API 31
                    bluetoothAdapter?.cancelDiscovery()
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                Log.d(TAG, "connectToDevice: 소켓을 생성 합니다")
                bluetoothSocket?.connect()
                Log.d(TAG, "connectToDevice:소켓에 연결합니다")

                runOnUiThread {
                    Toast.makeText(this, "${device.name ?: "알 수 없는 장치"} 연결 성공", Toast.LENGTH_SHORT).show()
                }
                Log.d(TAG, "connectToDevice: 장치 연결에 성공 했습니다 ${device.name}")

                startObdCommunication()

            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "연결 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
            }
        }.start()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // 전송 및 응답 받기
    private fun startObdCommunication() {
        Log.d(TAG, "startObdCommunication: 통신을 시도합니다")
        try {
            val inputStream = bluetoothSocket?.inputStream
            val outputStream = bluetoothSocket?.outputStream

            if (inputStream == null || outputStream == null) {
                runOnUiThread {
                    Toast.makeText(this, "통신 스트림을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "통신 스트림을 가져올 수 없습니다")
                }
                return
            }

            // OBD2 초기화 명령 보내기
            Log.d(TAG, "startObdCommunication: 리셋 명령을 내립니다")
            sendObdCommand(outputStream, "ATZ")  // Reset
            Thread.sleep(1000)                   // 잠시 대기
            Log.d(TAG, "startObdCommunication: Echo off 명령을 내립니다")
            sendObdCommand(outputStream, "ATE0")  // Echo off
            Log.d(TAG, "startObdCommunication: LineFeeds off 명령을 내립니다")
            sendObdCommand(outputStream, "ATL0")  // Linefeeds off
            Log.d(TAG, "startObdCommunication: Spaces off 명령을 내립니다")
            sendObdCommand(outputStream, "ATS0")  // Spaces off
            Log.d(TAG, "startObdCommunication: Headers on 명령을 내립니다")
            sendObdCommand(outputStream, "ATH0")  // Headers on (원하면 ATH0으로도 가능)
            Log.d(TAG, "startObdCommunication: protocal auto select")
            sendObdCommand(outputStream, "ATSP0")  // protocol auto select

            // 예시로 차량 지원 PID 읽기 (0100)
            val PID_response = sendObdCommand(outputStream, "0100")
            Log.d(TAG, "startObdCommunication: 차량 지원 PID를 읽었습니다")

            runOnUiThread {
                Toast.makeText(this, "응답: $PID_response", Toast.LENGTH_LONG).show()
            }
            Log.d(TAG, "startObdCommunication: 차량 지원 PID는 $PID_response 입니다")
            Log.d(TAG, "startObdCommunication: 차량 데이터를 수신합니다")
            while(true) {
                // RPM 데이터 수신
                var RPM_response = sendObdCommand(outputStream, "010C")
                var real_RPM_response = calculateRPM(RPM_response)

//                Log.d(TAG, "startObdCommunication: 차량 지원 RPM을 읽었습니다")

                runOnUiThread {
                    if(real_RPM_response != 0f) {
                        tvRpmValue.text = getString(R.string.text_rpm, real_RPM_response)
                        Toast.makeText(this, "응답: $real_RPM_response", Toast.LENGTH_LONG).show()
                    }
                    else{
                        Toast.makeText(this, "RPM 응답 error", Toast.LENGTH_SHORT).show()
                    }
                }
                Log.d(TAG, "startObdCommunication: 차량 지원 RPM는 $real_RPM_response 입니다")

                // 속도 데이터 수신
                var Speed_response = sendObdCommand(outputStream, "010D")
                var real_Speed_response = calculateSpeed(Speed_response)
//                Log.d(TAG, "startObdCommunication: 차량 지원 Speed를 읽었습니다")

                runOnUiThread {
                    if(real_Speed_response != 0) {
                        tvSpeedValue.text = getString(R.string.text_speed, real_Speed_response)
                        Toast.makeText(this, "응답: $real_Speed_response", Toast.LENGTH_LONG).show()
                    }
                    else{
                        Toast.makeText(this, " speed 응답 error", Toast.LENGTH_SHORT).show()
                    }
                }
                Log.d(TAG, "startObdCommunication: 차량 지원 Speed는 $real_Speed_response 입니다")
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // 명령어 전송
    private fun sendObdCommand(outputStream: java.io.OutputStream, command: String): String {
        val cmdWithReturn = "$command\r"
        outputStream.write(cmdWithReturn.toByteArray())
        outputStream.flush()

        val inputStream = bluetoothSocket?.inputStream ?: return ""

        val response = StringBuilder()
        val buffer = ByteArray(1024)

        while (true) {
            val bytesRead = inputStream.read(buffer)
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
    companion object {
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
}
