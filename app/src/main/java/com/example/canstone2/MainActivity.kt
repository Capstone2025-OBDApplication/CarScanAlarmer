package com.example.canstone2

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {

    private val notificationFragment = NotificationFragment()
    private val drivingFragment = DrivingFragment()  // 기본 페이지
    private val settingsFragment = SettingsFragment()
    private val monitoringFragment = MonitoringFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        supportActionBar?.hide()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val db = FirebaseFirestore.getInstance()
        db.collection("sensor_data")
            .get()
            .addOnSuccessListener { result ->
                Log.d("Firebase", "✔ 문서 수: ${result.size()}") // ★ 문서 수 출력

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


        // 기본 페이지: 운전 기록 페이지 (DrivingFragment)
        loadFragment(monitoringFragment)
        bottomNavigationView.selectedItemId = R.id.navigation_monitoring
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_notifications -> {
                    loadFragment(notificationFragment)
                    true
                }
                R.id.navigation_monitoring -> {
                    loadFragment(monitoringFragment)
                    true
                }
                R.id.navigation_driving -> {
                    loadFragment(drivingFragment)
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
