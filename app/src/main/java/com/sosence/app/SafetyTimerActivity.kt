package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView

class SafetyTimerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_timer)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val rb15 = findViewById<RadioButton>(R.id.rb15Mins)
        val rb60 = findViewById<RadioButton>(R.id.rb60Mins)
        val etReason = findViewById<EditText>(R.id.etTimerReason)

        findViewById<CardView>(R.id.btnStartSafetyTimer).setOnClickListener {
            val minutes = when {
                rb15.isChecked -> 15
                rb60.isChecked -> 60
                else -> 30
            }
            val reason = etReason.text.toString().trim()

            com.sosence.app.engine.BackgroundPermissionHelper.checkExactAlarmWithRationale(this) {
                val intent = Intent(this, SafetyTimerActiveActivity::class.java).apply {
                    putExtra("TIMER_MINUTES", minutes)
                    putExtra("TIMER_REASON", if (reason.isNotEmpty()) reason else "Safety Monitoring")
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
