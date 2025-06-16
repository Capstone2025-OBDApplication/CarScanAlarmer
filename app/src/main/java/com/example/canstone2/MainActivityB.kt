package com.example.canstone2
// file
import com.example.canstone2.localDB.DatabaseClass
import com.example.canstone2.localDB.DataTableSensor
import com.example.canstone2.onnx.OnnxModelRunner
import com.example.canstone2.viewmodel.ViewModelMain
import com.example.canstone2.ManagerArduino

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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
// fragment
import com.example.canstone2.ui.AlertFragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

import android.view.View



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

    // 코루틴
    private val ioScope = CoroutineScope(Dispatchers.IO + Job())

    // ONNX 모델
    private lateinit var modelRunner: OnnxModelRunner

    // ViewModel 연결
    private val viewModelMain: ViewModelMain by viewModels() // ONNX

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainb)
        supportActionBar?.hide()

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
//                supportFragmentManager.commit { // fragment 보이기
//                    replace(R.id.fragmentContainer, AlertFragment())
//                    addToBackStack(null)
//                }
                val intent = Intent(this, SuddenActivity::class.java)
                startActivity(intent)
            }
        })
        val rootLayout = findViewById<View>(R.id.activityMainB)
        val fragmentContainer = findViewById<View>(R.id.fragmentContainer)
        // 화면전환
        val gotoButton = findViewById<Button>(R.id.gotoMainButton)
        gotoButton.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
            rootLayout.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MainFragment())
                .addToBackStack(null) // 뒤로 가기 버튼 누르면 원래 화면으로 돌아감
                .commit()
        }

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

    override fun onResume() {
        super.onResume()
        Log.d("AAAAAAA MainActivityB", "onResume: 살아있음")
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

                // arduino socket
//                val adapter = BluetoothAdapter.getDefaultAdapter()
//                val arduinoDevice = adapter?.bondedDevices?.firstOrNull { it.name == "HC-06" }
//                val arduinoSocket: BluetoothSocket? = arduinoDevice?.createRfcommSocketToServiceRecord(uuid)

                // OBD 수신 시작
                // startObdCommunication() -> lifecycleScope.launch로 변경
                // 블루투스 소켓 통신 시작 (전송 및 응답 받기)
                bluetoothSocket?.let { socket -> // 블루투스 소켓이 null이 아닐 때만 실행
                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            viewModelMain.startSensorCollection(socket).collect{
                                Log.d(TAG, "데이터 수신 완료")
                                // UI 업데이트 처리
                                tvRpmValue.text = getString(R.string.text_rpm, it.rpm)
                                tvSpeedValue.text = getString(R.string.text_speed, it.speed)
                            }
                        }
                    }
                }
//                if (bluetoothSocket != null && arduinoSocket != null) { // 블루투스 소켓이 null이 아닐 때만 실행
//                    arduinoSocket.connect()
//                    Log.d(TAG, "connectToDevice: 장치 연결에 성공 했습니다 ${arduinoDevice.name}")
//
//                    lifecycleScope.launch {
//                        repeatOnLifecycle(Lifecycle.State.STARTED) {
//                            viewModelMain.startSensorCollection(bluetoothSocket!!, arduinoSocket).collect{
//                                Log.d(TAG, "데이터 수신 완료")
//                                // UI 업데이트 처리
//                                tvRpmValue.text = getString(R.string.text_rpm, it.rpm)
//                                tvSpeedValue.text = getString(R.string.text_speed, it.speed)
//                            }
//                        }
//                    }
//                } else {
//                    Log.e(TAG, "블루투스 장치를 찾을 수 없습니다.")
//                }
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
}