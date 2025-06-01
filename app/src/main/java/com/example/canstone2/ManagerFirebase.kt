package com.example.canstone2
import android.content.Context
import com.example.canstone2.localDB.DataTableSensor

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ManagerFirebase {
    private const val TAG = "ManageFirebase"

    private val dbFirebase: FirebaseFirestore
        get() = Firebase.firestore

    private val buffer = mutableListOf<DataTableSensor>()
    private var bufferStartTime: Long = 0L // 운행 시작 시간
    private var job: Job? = null

    // firebase 운행 기록 계산 변수
    private var currentStartTime = 0L
    private var lastReceiveTime = 0L
    private val GAP_TIME = 60000L // 60초 이상 데이터 없으면 새 문서로

    fun startBuffering() {
        buffer.clear()
        bufferStartTime = System.currentTimeMillis()

        // 30초 후 자동 업로드
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            delay(30_000L)
            uploadBufferToFirebase()
        }
    }

    fun addSensorDataBuffer(speed: Int, rpm: Float, accel: Int, brake: String) {
        if (buffer.isEmpty()) startBuffering()

        val data = DataTableSensor(

            date = System.currentTimeMillis(),
            speed = speed,
            rpm = rpm,
            accel = accel,
            brake = brake
        )
        buffer.add(data)
    }

    private suspend fun uploadBufferToFirebase() {
        if (buffer.isEmpty()) return
        Log.d("SensorBuffer", "Uploading ${buffer.size} entries to Firebase")

        // 1. 현재 날짜를 yyyy-MM-dd 형식으로 포맷
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1-2. 현재 시간을 HH-mm-ss 형식으로 포맷
        val timeFormat = SimpleDateFormat("HH-mm-ss", Locale.getDefault()) // 24시간 형식
        // val timeFormat = SimpleDateFormat("hh-mm-ss a", Locale.getDefault()) // 12시간 형식 (오전/오후 표시)
        val formattedTime = timeFormat.format(Date(bufferStartTime))

        // 2. entries 내부 문서 ID (시간)
        val docId = "start_${formattedTime}"

        // 3. 데이터 리스트
        val timestampList = buffer.map { it.date }
        val valueListSpeed = buffer.map { it.speed }
        val valueListRpm = buffer.map { it.rpm }
        val valueListAccel = buffer.map { it.accel }
        val valueListbrake = buffer.map { it.brake }

        val entryData = mapOf(
            "timestampList" to timestampList,
            "valueListSpeed" to valueListSpeed,
            "valueListRpm" to valueListRpm,
            "valueListAccel" to valueListAccel,
            "valueListbrake" to valueListbrake
        )

        try {
            dbFirebase.collection("sensor_data")
                .document(date)
                .collection("entries")
                .document(docId) // 문서 Id 지정
                .set(entryData, SetOptions.merge()) // set으로 저장
                .await()

            Log.d(TAG, "Uploaded ${buffer.size} entries to Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}")
        } finally {
            buffer.clear()
            bufferStartTime = 0L
        }
    }

    // JSON으로 내보내기
    fun exportSensorDataAsJson(
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val resultJson = JSONObject()

        db.collection("sensor_data")
            .get()
            .addOnSuccessListener { dateDocuments ->
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (dateDoc in dateDocuments) {
                    val dateKey = dateDoc.id
                    val dateJson = JSONObject()

                    val task = db.collection("sensor_data")
                        .document(dateKey)
                        .collection("entries")
                        .get()
                        .addOnSuccessListener { entryDocs ->
                            for (entryDoc in entryDocs) {
                                val entryKey = entryDoc.id
                                val data = entryDoc.data

                                val entryJson = JSONObject(data)
                                dateJson.put(entryKey, entryJson)
                            }
                            resultJson.put(dateKey, dateJson)
                        }

                    tasks.add(task)
                }

                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        onSuccess(resultJson.toString(2)) // pretty-print JSON
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }

            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // CSV로 내보내기
    /*  출력형태 (예시)
        date, entry_id, timestamp, value
        2025-05-27, start_1716800000000, 1716800000000, 1.1
        2025-05-27, start_1716800000000, 1716800001000, 1.3
     */
    fun exportSensorDataAsCSV(
        context: Context,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val csvBuilder = StringBuilder()
        csvBuilder.append("date,entry_id,timestamp,value\n")

        db.collection("sensor_data")
            .get()
            .addOnSuccessListener { dateDocuments ->
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (dateDoc in dateDocuments) {
                    val dateKey = dateDoc.id

                    val task = db.collection("sensor_data")
                        .document(dateKey)
                        .collection("entries")
                        .get()
                        .addOnSuccessListener { entryDocs ->
                            for (entryDoc in entryDocs) {
                                val entryId = entryDoc.id
                                val data = entryDoc.data
                                val timestamps = data["timestampList"] as? List<Long>
                                val values = data["valueList"] as? List<Number>

                                if (timestamps != null && values != null) {
                                    for (i in timestamps.indices) {
                                        val timestamp = timestamps[i]
                                        val value = values.getOrNull(i)
                                        if (value != null) {
                                            csvBuilder.append("$dateKey,$entryId,$timestamp,$value\n")
                                        }
                                    }
                                }
                            }
                        }

                    tasks.add(task)
                }

                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        try {
                            val file = File(context.filesDir, "exported_data.csv") // filesDir은 앱 내부 저장소
                            file.writeText(csvBuilder.toString())
                            onSuccess(file)
                        } catch (e: Exception) {
                            onFailure(e)
                        }
                    }
                    .addOnFailureListener {
                        onFailure(it)
                    }
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }



    // 알림 저장
    private suspend fun uploadAlarmToFirebase() {
        val now = System.currentTimeMillis()

        // 1. 현재 날짜를 yyyy-MM-dd 형식으로 포맷
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1-2. 현재 시간을 HH-mm-ss 형식으로 포맷
        val timeFormat = SimpleDateFormat("HH-mm-ss", Locale.getDefault()) // 24시간 형식
        // val timeFormat = SimpleDateFormat("hh-mm-ss a", Locale.getDefault()) // 12시간 형식 (오전/오후 표시)
        val formattedTime = timeFormat.format(Date())

        // 2. entries 내부 문서 ID (시간)
        val docId = "start_${formattedTime}"

        // 3. 데이터 리스트
        val timestampList = FieldValue.arrayUnion(now)
        val valueListCategory = FieldValue.arrayUnion("category")
        val valueListType = FieldValue.arrayUnion("type")

        val entryData = mapOf(
            "timestampList" to timestampList,
            "valueListCategory" to valueListCategory,
            "valueListType" to valueListType
        )

        try {
            dbFirebase.collection("alarm_data")
                .document(date)
                .collection("entries")
                .document(docId) // 문서 Id 지정
                .set(entryData, SetOptions.merge()) // set으로 저장
                .await()

            Log.d(TAG, "Uploaded Alarm entries to Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}")
        } finally {
            buffer.clear()
            bufferStartTime = 0L
        }
    }
}