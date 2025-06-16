package com.example.canstone2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat

class DrivingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_driving, container, false)

        // 캘린더 선택 날짜 처리
        val calendarView = view.findViewById<CustomCalendarView>(R.id.calendarView)
        val timeText = view.findViewById<TextView>(R.id.time)
        calendarView.setDrivingTable(view.findViewById(R.id.gridContainer))


        // 그래프 설정
        val customGraphView = view.findViewById<CustomGraphView>(R.id.graphView)
        calendarView.setOnDateSelectedListener(object : CustomCalendarView.OnDateSelectedListener {
            override fun onDateSelected(date: Calendar, formttedDate:String) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = sdf.format(date.time)
                fetchDataForDate(formattedDate, customGraphView)
            }
        })
        val db = FirebaseFirestore.getInstance()
        db.collection("sensor_data")
            .get()
            .addOnSuccessListener { result ->
                Log.d("Firestore", "문서 수: ${result.size()}") // 몇 개 있는지
                for (doc in result.documents) {
                    Log.d("Firestore", "문서 ID: ${doc.id}")
                }

                val documentIds = result.documents.map { it.id }
                Log.d("Firestore", "날짜 목록: $documentIds")

                val blueDates = /*documentIds.drop(1)*/listOf("2025-05-28", "2025-05-29", "2025-05-30")
                val redDates = /*documentIds.take(1)*/listOf("2025-06-04")
                calendarView.setAvailableDates(blueDates, redDates)
            }
            .addOnFailureListener {
                Log.e("Firestore", "문서 목록 가져오기 실패: ${it.message}")
            }

//        val db = FirebaseFirestore.getInstance()
//
//        db.collection("sensor_data")
//            .get()
//            .addOnSuccessListener { result ->
//                val documentIds = result.documents.map { it.id }
//                Log.d("Firestore", "날짜 목록: $documentIds")
//                val blueDates = documentIds.drop(1)
//                val redDates = documentIds.take(1)
//                calendarView.setAvailableDates(blueDates, redDates)
//            }
//            .addOnFailureListener {
//                Log.e("Firestore", "문서 목록 가져오기 실패: ${it.message}")
//            }
        val speedList = listOf(
            Entry(0f, 0f), Entry(1f, 0f), Entry(2f, 0f), Entry(3f, 5f),
            Entry(4f, 20f), Entry(4.5f, 45f), // 급발진 조건 성립
            Entry(5f, 60f), Entry(6f, 70f), Entry(7f, 75f), Entry(8f, 78f),
            Entry(9f, 80f), Entry(10f, 80f), Entry(11f, 75f), Entry(12f, 60f),
            Entry(13f, 30f), Entry(14f, 5f), Entry(15f, 0f)
        )

        val rpmList = listOf(
            Entry(0f, 800f), Entry(1f, 800f), Entry(2f, 800f), Entry(3f, 1000f),
            Entry(4f, 1500f), Entry(4.5f, 3200f), // 급발진 조건 성립
            Entry(5f, 3500f), Entry(6f, 3600f), Entry(7f, 3700f), Entry(8f, 3600f),
            Entry(9f, 3400f), Entry(10f, 3000f), Entry(11f, 2000f), Entry(12f, 1500f),
            Entry(13f, 1000f), Entry(14f, 900f), Entry(15f, 800f)
        )

        val accelList = listOf(
            Entry(0f, 0f), Entry(1f, 0f), Entry(2f, 0f), Entry(3f, 2f),
            Entry(4f, 1f), Entry(4.5f, 0f), // 급발진 조건 성립 (엑셀 안 밟음)
            Entry(5f, 30f), Entry(6f, 50f), Entry(7f, 70f), Entry(8f, 80f),
            Entry(9f, 75f), Entry(10f, 70f), Entry(11f, 40f), Entry(12f, 20f),
            Entry(13f, 5f), Entry(14f, 0f), Entry(15f, 0f)
        )

        val brakeList = listOf(
            Entry(0f, 5f), Entry(1f, 10f), Entry(2f, 0f), Entry(3f, 20f),
            Entry(4f, 1f), Entry(4.5f, 0f), // 급발진 조건 성립 (엑셀 안 밟음)
            Entry(5f, 30f), Entry(6f, 0f), Entry(7f, 70f), Entry(8f, 80f),
            Entry(9f, 45f), Entry(10f, 0f), Entry(11f, 30f), Entry(12f, 20f),
            Entry(13f, 5f), Entry(14f, 0f), Entry(15f, 0f)
        )// 전체 무브레이크 상황


        val highlightXs = listOf(4.5f)
        customGraphView.setDataLists(
            speedList = speedList,
            brakeList = brakeList,
            accelList = accelList,
            rpmList = rpmList,
            highlightXs = highlightXs
        )

        return view
    }
    private fun fetchDataForDate(dateDoc: String, customGraphView: CustomGraphView) {
        val db = FirebaseFirestore.getInstance()
        db.collection("sensor_data")
            .document(dateDoc)
            .collection("entries")
            .get()
            .addOnSuccessListener { result ->
                val speedList = mutableListOf<Entry>()
                val rpmList = mutableListOf<Entry>()
                val accelList = mutableListOf<Entry>()
                val brakeList = mutableListOf<Entry>()

                for (doc in result) {
                    val x = doc.getDouble("time")?.toFloat() ?: continue
                    speedList.add(Entry(x, doc.getDouble("speed")?.toFloat() ?: 0f))
                    rpmList.add(Entry(x, doc.getDouble("rpm")?.toFloat() ?: 0f))
                    accelList.add(Entry(x, doc.getDouble("accelerator")?.toFloat() ?: 0f))
                    brakeList.add(Entry(x, doc.getDouble("brake")?.toFloat() ?: 0f))
                }

                setupDrivingRecordGraph(customGraphView, speedList, rpmList, accelList, brakeList)
            }
            .addOnFailureListener {
                Log.e("Firestore", "선택된 날짜 데이터 불러오기 실패: ${it.message}")
            }
    }
    private fun setupDrivingRecordGraph(customGraphView: CustomGraphView,
                                        speedList: List<Entry>,
                                        rpmList: List<Entry>,
                                        accelList: List<Entry>,
                                        brakeList: List<Entry>) {

        val highlightXs = listOf(4.5f)
        customGraphView.setDataLists(
            speedList = speedList.toMutableList(),
            brakeList = brakeList.toMutableList(),
            accelList = accelList.toMutableList(),
            rpmList = rpmList.toMutableList(),
            highlightXs = highlightXs
        )
    }
}
