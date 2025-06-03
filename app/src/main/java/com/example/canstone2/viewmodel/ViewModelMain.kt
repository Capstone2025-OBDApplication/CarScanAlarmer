package com.example.canstone2.viewmodel
import com.example.canstone2.localDB.DatabaseClass
import com.example.canstone2.localDB.DataTableSensor
import com.example.canstone2.localDB.DataTableSensorClass
import com.example.canstone2.localDB.ObdData
import com.example.canstone2.repository.RepositorySensor
import com.example.canstone2.onnx.OnnxModelRunner
import com.example.canstone2.ManagerOBD
import com.example.canstone2.ManagerArduinoBLE

import android.app.Application
import android.bluetooth.BluetoothSocket
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.canstone2.ManagerFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ViewModelMain(application: Application) : AndroidViewModel(application) {
    val appContext = application.applicationContext

    // local DB 선언
    private val db = DatabaseClass.getInstance(application.applicationContext)!!
    // local DB Repository
    private val localRepository = RepositorySensor(db.sensorDAO())

    // manager OBD 선언
    private val managerOBD = ManagerOBD()
    // manager BLE
    private val managerBLE = null // TODO

    // ONNX 모델
    private val modelRunner = OnnxModelRunner(application.applicationContext)
    // 예측 결과
    private val _prediction = MutableLiveData<Int>()
    val prediction: LiveData<Int> get() = _prediction

    fun runPrediction(speed: Int, rpm: Float, accel: Int, brake: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = modelRunner.predict(speed, rpm, accel, brake)
            _prediction.postValue(result)

            if (result == 1) { // 예측 결과가 1이면 local DB의 DataTableSensorClass 저장
                val data_class = DataTableSensorClass(
                    timestamp = System.currentTimeMillis(),
                    clazz = "detected"
                )
                db.sensorClassDAO().insertClass(data_class)
            }
        }
    }

    fun saveToLocal(sensor: DataTableSensor) {
        viewModelScope.launch(Dispatchers.IO) {
            localRepository.insertSensor(sensor)
        }
    }

    fun saveToFirebase(speed: Int, rpm: Float, accel: Int, brake: Int) {
        // firebase DB 데이터 추가
        ManagerFirebase.addSensorDataBuffer( // date 자동 생성
            speed = speed,
            rpm = rpm,
            accel = accel,
            brake = brake.toString()
        )
    }

    fun startSensorCollection(socket: BluetoothSocket): Flow<ObdData> {
        return managerOBD.startObdFlow(appContext, socket)
//            .combine(managerBLE.bleFlow()) { obd, ble ->
//                DataTableSensor(
//                    date = System.currentTimeMillis(),
//                    speed = obd.speed,
//                    rpm = obd.rpm,
//                    accel = 0,
//                    brake = 0.toString()
//                )
//            }
            .onEach { data : ObdData ->
                runPrediction(data.speed, data.rpm, 0, 1)
                val sensor = DataTableSensor(
                    date = System.currentTimeMillis(),
                    speed = data.speed,
                    rpm = data.rpm,
                    accel = 0,
                    brake = 1.toString()
                )
                 saveToLocal(sensor)
                 // saveToFirebase(data.speed, data.rpm, 0, 1)
            }
            .flowOn(Dispatchers.IO)
    }
}