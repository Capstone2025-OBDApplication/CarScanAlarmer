package com.example.canstone2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class MainFragment : Fragment() {
    private val notificationFragment = NotificationFragment()
    private val drivingFragment = DrivingFragment()  // 기본 페이지
    private val settingsFragment = SettingsFragment()
    private val monitoringFragment = MonitoringFragment()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃 연결 (activity_main.xml 그대로 사용 가능)
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase 초기화 (한 번만 하면 됨, App context 사용)
        FirebaseApp.initializeApp(requireContext())

        val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Firestore 연동
        val db = FirebaseFirestore.getInstance()
        db.collection("sensor_data")
            .get()
            .addOnSuccessListener { result ->
                Log.d("Firebase", "✔ 문서 수: ${result.size()}")
                if (result.isEmpty) {
                    Log.w("Firebase", "⚠️ 문서 없음 or 권한 없음")
                }
                for (doc in result) {
                    Log.d("Firebase", "📄 문서 ID: ${doc.id}")
                    Log.d("Firebase", "📋 내용: ${doc.data}")
                }
            }
            .addOnFailureListener {
                Log.e("Firebase", "🔥 실패: ${it.message}")
            }

        // 초기 Fragment 로딩
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, monitoringFragment)
            .commit()

        // 기본 페이지: 운전 기록 페이지 (DrivingFragment)
        bottomNavigationView.selectedItemId = R.id.navigation_monitoring
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.navigation_notifications -> notificationFragment
                R.id.navigation_monitoring -> monitoringFragment
                R.id.navigation_driving -> drivingFragment
                R.id.navigation_settings -> settingsFragment
                else -> null
            }

            selectedFragment?.let {
                childFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }
    }
}
