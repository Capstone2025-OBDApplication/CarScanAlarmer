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
                val documentIds = result.documents.map { it.id }
                Log.d("Firestore", "날짜 목록: $documentIds")
                val blueDates = documentIds.drop(1)
                val redDates = documentIds.take(1)
                calendarView.setAvailableDates(blueDates, redDates)
            }
            .addOnFailureListener {
                Log.e("Firestore", "문서 목록 가져오기 실패: ${it.message}")
            }

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
            speedList = speedList,
            brakeList = brakeList,
            accelList = accelList,
            rpmList = rpmList,
            highlightXs = highlightXs
        )
    }
}
