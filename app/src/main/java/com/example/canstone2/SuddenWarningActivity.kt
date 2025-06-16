package com.example.canstone2

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.example.canstone2.databinding.ActivitySuddenWarningBinding
import java.util.*
import android.util.Log
import android.content.Intent



class SuddenWarningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech  // TTS 선언
    private lateinit var binding: ActivitySuddenWarningBinding // Databinding 변수 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuddenWarningBinding.inflate(layoutInflater) // Binding 초기화
        setContentView(binding.root)
        supportActionBar?.hide()

        tts = TextToSpeech(this, this)
        binding.backToMainButton.setOnClickListener {
            finish()
            finish()

        }
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.KOREAN)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                val message = """
                1번. 브레이크를 한 번에 강하게 밟으십시오.
                2번. 기어를 중립, N으로 전환하십시오.
                3번. 사이드 브레이크를 사용해 속도를 감소시키십시오.
                모든 상황이 끝났다면 시동을 꺼주시기 바랍니다.
            """.trimIndent()

                // 3번 반복
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "utterance1")
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, "utterance2")
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, "utterance3")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()

    }
}