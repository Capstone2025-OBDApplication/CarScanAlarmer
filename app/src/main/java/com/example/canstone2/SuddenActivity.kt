package com.example.canstone2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class SuddenActivity : AppCompatActivity() {

    private lateinit var countDownText: TextView
    private var countDownTimer: CountDownTimer? = null  // ✅ 타이머 변수 선언
    companion object {
        private const val REQUEST_CALL_PERMISSION = 1  // ✅ 여기 선언되어야 함
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sudden)
        supportActionBar?.hide()

        val cancelButton: Button = findViewById(R.id.cancelButton)
        countDownText = findViewById(R.id.countDownText)

        cancelButton.setOnClickListener {
            countDownTimer?.cancel()  // ✅ 버튼 누르면 타이머 멈춤
            val intent = Intent(this, SuddenWarningActivity::class.java)
            startActivity(intent)
            finish()
        }

        startCountDown(10)
    }

    private fun startCountDown(seconds: Int) {
        countDownTimer = object : CountDownTimer((seconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                countDownText.text = "신고(${secondsLeft}s)"
            }

            override fun onFinish() {
                if (ActivityCompat.checkSelfPermission(
                        this@SuddenActivity,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:112")
                    }
                    startActivity(callIntent)
                } else {
                    ActivityCompat.requestPermissions(
                        this@SuddenActivity,
                        arrayOf(Manifest.permission.CALL_PHONE),
                        REQUEST_CALL_PERMISSION
                    )
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()  // ✅ 액티비티 종료 시에도 타이머 정리
    }
}

