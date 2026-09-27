package br.com.alongamento.tela

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.alongamento.tela.display.DisplayController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SafetyCountdownActivity : AppCompatActivity() {
    private var timer: CountDownTimer? = null
    private var kept = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety)
        val seconds = intent.getIntExtra("seconds", 12).coerceIn(5, 30)
        val label = findViewById<TextView>(R.id.txtCountdown)
        findViewById<MaterialButton>(R.id.btnKeep).setOnClickListener {
            kept = true
            timer?.cancel()
            finish()
        }
        findViewById<MaterialButton>(R.id.btnUndo).setOnClickListener {
            undo()
        }
        timer = object : CountDownTimer(seconds * 1000L, 200) {
            override fun onTick(millisUntilFinished: Long) {
                label.text = getString(R.string.safety_tick, ((millisUntilFinished + 999) / 1000).toInt())
            }
            override fun onFinish() {
                if (!kept) undo()
            }
        }.start()
    }

    private fun undo() {
        timer?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { DisplayController.restore() }
            runOnUiThread { finish() }
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
