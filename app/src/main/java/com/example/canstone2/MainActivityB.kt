package com.example.canstone2
// file
import com.example.canstone2.localDB.DatabaseClass
import com.example.canstone2.localDB.DataTableSensor
import com.example.canstone2.onnx.OnnxModelRunner
import com.example.canstone2.viewmodel.ViewModelMain

import android.annotation.SuppressLint
import android.Manifest
import android.util.Log
// os
import android.os.Bundle
import android.os.Build
// bluetooth
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
// content
import android.content.Intent
import android.content.pm.PackageManager
// widget
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import android.widget.TextView
// activity, content
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
// network
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
// coroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
// java
import java.io.IOException
import java.util.*
// dialog
import android.app.AlertDialog
import android.provider.Settings
// lifecycle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
// fragment
import com.example.canstone2.ui.AlertFragment
import androidx.fragment.app.commit



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

    // local DB
    private lateinit var db: DatabaseClass

    // Manager
    private val managerObd = ManagerOBD()

    // 코루틴
    private val ioScope = CoroutineScope(Dispatchers.IO + Job())

    // ONNX 모델
    private lateinit var modelRunner: OnnxModelRunner

    // ViewModel 연결
    private val viewModelMain: ViewModelMain by viewModels() // ONNX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainb)

        // db 선언
        db = DatabaseClass.getInstance(this)!!

        // UI 초기화
        deviceListView = findViewById(R.id.deviceListView)
        scanButton = findViewById(R.id.scanButton)
        tvRpmValue = findViewById(R.id.tvRpmValue)
        tvSpeedValue = findViewById(R.id.tvSpeedValue)

        // 블루투스 권한 확인
        checkPermissions()

        // 블루투스
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

//        // 블루투스 BLE (아두이노) 권한 확인
//        val bleManager = ManagerArduinoBLE(
//            context = this,
//            onConnected = { gatt ->
//                runOnUiThread {
//                    Toast.makeText(this, "Bluno 연결됨", Toast.LENGTH_SHORT).show()
//                }
//            },
//            onError = { msg ->
//                runOnUiThread {
//                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//                }
//            }
//        )
//        // 블루투스 BLE (아두이노) 스캔 및 연결
//        bleManager.startScan()

        // 인터넷 연결 확인
        checkInternetAndPrompt(this)

        // ONNX 모델 선언
        modelRunner = OnnxModelRunner(applicationContext)

        // 블루투스 장치 스캔 버튼
        scanButton.setOnClickListener {
            scanDevices()
        }

        // 블루투스 장치 리스트
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            connectToDevice(device)
        }

        // ONNX 모델 LiveData로 예측 결과 실행
        viewModelMain.prediction.observe(this, Observer { result ->
            Log.d(TAG, "viewModelMain: 예측 결과는 $result 입니다")
            if (result == 1) { // 예측 결과가 1이면
                supportFragmentManager.commit { // fragment 보이기
                    replace(R.id.fragmentContainer, AlertFragment())
                    addToBackStack(null)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel()  // 생명 주기 종료 시 코루틴 정리
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

        // Android 12(API 31) 이상 -> BLUETOOTH_CONNECT 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS
            && grantResults.all{ it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // 인터넷 연결 확인
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    // 인터넷 연결 확인 및 다이얼로그 표시
    fun checkInternetAndPrompt(context: Context) {
        if (!isNetworkAvailable(context)) {
            // 인터넷 연결 안됨 → 다이얼로그 표시
            AlertDialog.Builder(context)
                .setTitle("인터넷 연결 안됨")
                .setMessage("인터넷에 연결되어 있지 않습니다.\n설정으로 이동하시겠습니까?")
                .setPositiveButton("예") { dialog, _ ->
                    // 네트워크 설정 화면으로 이동
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    dialog.dismiss()
                }
                .setNegativeButton("아니오") { dialog, _ ->
                    Toast.makeText(context, "인터넷 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setCancelable(false) // 다이얼로그 밖을 클릭해도 안 닫힘
                .show()
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

                // OBD 수신 시작
                // startObdCommunication() -> onCreate에 lifecycleScope.launch로
                // 블루투스 소켓 통신 시작
                bluetoothSocket?.let { socket -> // 블루투스 소켓이 null이 아닐 때만 실행
                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            viewModelMain.startSensorCollection(socket).collect{
                                Log.d(TAG, "데이터 수신 완료")
                            }
                        }
                    }
                }

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

    // 전송 및 응답 받기
    private fun startObdCommunication() {
        Log.d(TAG, "startObdCommunication: 통신을 시도합니다")
        val managerOBD = ManagerOBD()
        try {
            bluetoothSocket?.let{
                // OBD2 초기화 명령 보내기
                managerOBD.sendObdInit(this, bluetoothSocket!!)
            }

            // RPM, 속도 데이터 지속적으로 받기
            while(true) {
                // break 여기 넣거나 마지막에 if 넣거나
                // RPM 데이터 수신
                var RPM_response = managerOBD.sendObdCommand("010C")
                var real_RPM_response = managerOBD.calculateRPM(RPM_response)
                // Thread sleep() 유무 차이는 없을 수도 있음 10~250 사이 값으로 테스트 해보기 rpm, speed 사이의 텀이 없어서 오류 뜰수도
                Thread.sleep(250) // 0.25초
                // 속도 데이터 수신
                var Speed_response = managerOBD.sendObdCommand("010D")
                var real_Speed_response = managerOBD.calculateSpeed(Speed_response)

                runOnUiThread {
                    if(real_RPM_response != 0f) {
                        tvRpmValue.text = getString(R.string.text_rpm, real_RPM_response)
                    }
                    Thread.sleep(250)
                    if(real_Speed_response != 0) {
                        tvSpeedValue.text = getString(R.string.text_speed, real_Speed_response)
                    }
                }
                // ui 스레드 끝남
                Log.d(TAG, "startObdCommunication: 차량 지원 RPM는 $real_RPM_response 입니다")
                Log.d(TAG, "startObdCommunication: 차량 지원 Speed는 $real_Speed_response 입니다")

                var real_accel = 0
                val real_brake = 1

                // 비동기 실행
                ioScope.launch{
                    // ONNX 모델 실행 (예측값 local DB 저장 포함)
                    viewModelMain.runPrediction(real_Speed_response, real_RPM_response, real_accel, real_brake)

                    // local DB (Room) 데이터 생성
                    val data = DataTableSensor( // id는 자동 생성
                        date = System.currentTimeMillis(),
                        accel = real_accel,
                        rpm = real_RPM_response,
                        speed = real_Speed_response,
                        brake = real_brake.toString()
                    )

                    // local DB (Room) 데이터 저장
                    viewModelMain.saveToLocal(data)

                    // firebase DB 데이터 추가
                    ManagerFirebase.addSensorDataBuffer( // date 자동 생성
                        speed = real_Speed_response,
                        rpm = real_RPM_response,
                        accel = real_accel,
                        brake = real_brake.toString()
                    )
                }
            }
            Thread.sleep(500) // 0.5초 // 항상 넣기 쓰레드 안에
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 아두이노 데이터 처리
    private fun handleReceivedBLEData(data: String) {
        try {
            val parts = data.split(",")  // ["BREAK:12.34", "ACCEL:5.67"]
            var breakForce: Float? = null
            var accelForce: Float? = null

            for (part in parts) {
                val keyValue = part.split(":")
                if (keyValue.size == 2) {
                    when (keyValue[0].trim().uppercase()) {
                        "BREAK" -> breakForce = keyValue[1].toFloatOrNull()
                        "ACCEL" -> accelForce = keyValue[1].toFloatOrNull()
                    }
                }
            }

            // UI에 표시하거나 처리
            runOnUiThread {
                val result = buildString {
                    append("브레이크: ${breakForce ?: "N/A"} N\n")
                    append("엑셀: ${accelForce ?: "N/A"} N")
                }
                //findViewById<TextView>(R.id.textView).text = result
            }

        } catch (e: Exception) {
            Log.e("BLE_PARSE", "데이터 파싱 오류: ${e.message}")
        }
    }
}