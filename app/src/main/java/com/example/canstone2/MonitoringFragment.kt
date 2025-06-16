package com.example.canstone2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import com.example.canstone2.localDB.ObdData
import com.example.canstone2.viewmodel.ViewModelMain

class MonitoringFragment : Fragment() {

    private lateinit var graphView: CustomGraphView
    private val handler = Handler(Looper.getMainLooper())
    private var index = 0
    private lateinit var rpmTextView: TextView
    private lateinit var speedTextView: TextView

    // 병합
    private lateinit var viewModelMain: ViewModelMain

    // 예시 급발진 데이터 (속도, 브레이크, 엑셀, RPM)
    private val speedList = listOf(
        0f, 0f, 0f, 5f, 20f, 45f, 60f, 70f, 75f, 78f,
        80f, 80f, 75f, 60f, 30f, 5f, 0f
    )
    private val rpmList = listOf(
        800f, 800f, 800f, 1000f, 1500f, 3200f, 3500f, 3600f, 3700f, 3600f,
        3400f, 3000f, 2000f, 1500f, 1000f, 900f, 800f
    )
    private val accelList = listOf(
        0f, 0f, 0f, 2f, 1f, 0f, 30f, 50f, 70f, 80f,
        75f, 70f, 40f, 20f, 5f, 0f, 0f
    )
    private val brakeList = List(speedList.size) { 0f }

    private val highlightXs = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monitoring, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rpmTextView = view.findViewById(R.id.rpmValueTextView)
        speedTextView = view.findViewById(R.id.speedValueTextView)
        graphView = requireView().findViewById(R.id.graphView)
        simulateRealTimeUpdate()
    }

    private fun simulateRealTimeUpdate() {
        //if (index >= speedList.size) return

        viewModelMain = ViewModelProvider(requireActivity())[ViewModelMain::class.java]

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelMain.sensorData.observe(viewLifecycleOwner) { data ->
                    data?.let {
                        rpmTextView.text = getString(R.string.text_rpm, it.rpm).let {
                            it.dropLast(4).trimEnd() + "\n" + it.takeLast(3)
                        }
                        speedTextView.text = getString(R.string.text_speed, it.speed).let {
                            it.dropLast(5).trimEnd() + "\n" + it.takeLast(4)
                        }
                        graphView.updateData(
                            xValue = index.toFloat(),
                            speed = it.speed.toFloat(),
                            rpm = it.rpm.toFloat(),
                            brake = 1f,
                            accel = 0f,
                        )
                        index++
                    }
                }
            }
        }

//        val x = index.toFloat()
//        val speed = speedList[index]
//        val brake = brakeList[index]
//        val accel = accelList[index]
//        val rpm = rpmList[index]
//        rpmTextView.text = rpm.toInt().toString()
//        speedTextView.text = speed.toInt().toString()
//        val isSuddenAccel = accel < 5f && speed > 20f && rpm > 2000f
//        graphView.updateData(
//            xValue = x,
//            speed = speed,
//            brake = brake,
//            accel = accel,
//            rpm = rpm,
//            highlight = isSuddenAccel
//        )
//        if (isAdded && isSuddenAccel) {
//            val intent = Intent(requireContext(), SuddenActivity::class.java)
//            startActivity(intent)
//        }
//        index++
//        handler.postDelayed({ simulateRealTimeUpdate() }, 500)
    }
}
