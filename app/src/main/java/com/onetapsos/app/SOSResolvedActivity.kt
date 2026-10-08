package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSResolvedActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_resolved)

        val resTime = intent.getLongExtra("RESOLUTION_TIME", System.currentTimeMillis())
        val recipientsCount = intent.getIntExtra("RECIPIENTS_COUNT", 0)

        val tvResolutionTime = findViewById<TextView>(R.id.tvResolutionTime)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvResolvedRecipients = findViewById<TextView>(R.id.tvResolvedRecipients)

        tvResolutionTime.text = SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date(resTime))
        tvResolvedRecipients.text = "$recipientsCount Contact(s)"

        val startTime = appSettings.lastSosTimestamp
        if (startTime > 0 && resTime > startTime) {
            val seconds = (resTime - startTime) / 1000
            val mins = seconds / 60
            val secs = seconds % 60
            tvDuration.text = "${mins}m ${secs}s"
        } else {
            tvDuration.text = "Under 1 min"
        }

        findViewById<CardView>(R.id.btnDone).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
