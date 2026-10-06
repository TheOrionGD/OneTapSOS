package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class EmergencyMessagesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_messages)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val tvPreview = findViewById<TextView>(R.id.tvImSafePreview)
        tvPreview.text = appSettings.customSafeMessage

        findViewById<CardView>(R.id.cardImSafeEditor).setOnClickListener {
            startActivity(Intent(this, ImSafeMessageActivity::class.java))
        }

        findViewById<CardView>(R.id.cardTemplates).setOnClickListener {
            startActivity(Intent(this, MessageTemplateActivity::class.java))
        }

        findViewById<CardView>(R.id.cardBroadcast).setOnClickListener {
            startActivity(Intent(this, EmergencyBroadcastActivity::class.java))
        }

        findViewById<CardView>(R.id.cardMessageHistory).setOnClickListener {
            startActivity(Intent(this, MessageHistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.tvImSafePreview).text = appSettings.customSafeMessage
    }
}
