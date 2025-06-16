package com.example.canstone2.viewmodel
import com.example.canstone2.localDB.DatabaseClass
import com.example.canstone2.localDB.DataTableSensor
import com.example.canstone2.localDB.DataTableSensorClass
import com.example.canstone2.localDB.ObdData
import com.example.canstone2.repository.RepositorySensor
import com.example.canstone2.onnx.OnnxModelRunner
import com.example.canstone2.ManagerOBD


import android.app.Application
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.canstone2.ManagerArduino
import com.example.canstone2.ManagerFirebase
import com.example.canstone2.localDB.ArduinoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val managerArduino = ManagerArduino("HC-06")

    // ONNX 모델
    private val modelRunner = OnnxModelRunner(application.applicationContext)
    // 예측 결과
    private val _prediction = MutableLiveData<Int>()
    val prediction: LiveData<Int> get() = _prediction

    //
    private val _obdDataLive = MutableLiveData<ObdData>()
    val sensorData: LiveData<ObdData> get() = _obdDataLive



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
        viewModelScope.launch(Dispatchers.IO) {
            // firebase DB 데이터 추가
            ManagerFirebase.addSensorDataBuffer( // date 자동 생성
                speed = speed,
                rpm = rpm,
                accel = accel,
                brake = brake
            )
        }
    }

    fun startSensorCollection(socket: BluetoothSocket): Flow<ObdData> {
//        return managerOBD.startObdFlow(appContext, socket)
        return managerOBD.startObdFlow(socket)
            .onEach { data : ObdData ->
                runPrediction(data.speed, data.rpm, 0, 1)
                val sensor = DataTableSensor(
                    date = System.currentTimeMillis(),
                    speed = data.speed,
                    rpm = data.rpm,
                    accel = 0,
                    brake = 1
                )
                saveToLocal(sensor)
                _obdDataLive.postValue(data)
                // saveToFirebase(data.speed, data.rpm, 0, 1)
            }
            .flowOn(Dispatchers.IO)
    }

//    fun startSensorCollectionArduino(obdSocket: BluetoothSocket, arduinoSocket: BluetoothSocket): Flow<DataTableSensor> {
////        return managerOBD.startObdFlow(appContext, obdSocket)
//        return managerOBD.startObdFlow(obdSocket)
//            .combine(managerArduino.startArduinoFlow(arduinoSocket)) { obd, ard ->
//                DataTableSensor(
//                    date = System.currentTimeMillis(),
//                    speed = obd.speed,
//                    rpm = obd.rpm,
//                    accel = ard.accel,
//                    brake = ard.brake
//                )
//            }
//            .onEach { data : DataTableSensor ->
//                runCatching {
//                    runPrediction(data.speed, data.rpm, data.accel, data.brake)
//                    saveToLocal(data)
//                    // saveToFirebase(data.speed, data.rpm, 0, 1)
//                }.onFailure {
//                    Log.e("SensorCollector", "Error during processing", it)
//                }
//            }
//            .flowOn(Dispatchers.IO)
//    }
}