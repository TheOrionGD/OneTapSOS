package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.sosence.app.data.AppDatabaseHelper

class SOSDetailsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_details)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val eventId = intent.getLongExtra("EVENT_ID", -1L)
        val dbHelper = AppDatabaseHelper(this)
        val event = dbHelper.getSosEventById(eventId) ?: dbHelper.getAllSosEvents().firstOrNull()

        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvMessage = findViewById<TextView>(R.id.tvDetailMessage)
        val tvLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val tvRecipients = findViewById<TextView>(R.id.tvDetailRecipients)
        val tvStatus = findViewById<TextView>(R.id.tvDetailStatus)

        if (event != null) {
            tvTitle.text = "🚨 SOS Event #${event.id}"
            tvTime.text = event.formattedTime
            tvMessage.text = event.message
            tvLocation.text = "Location: ${event.locationName}"
            tvRecipients.text = "Recipients: ${event.recipientsCount} Contact(s)"
            tvStatus.text = if (event.isResolved) "Status: RESOLVED ✅ (Duration: ${event.durationSeconds}s)" else "Status: ACTIVE 🚨"
        } else {
            tvTitle.text = "🚨 SOS Emergency Log"
            tvTime.text = "No recorded event details"
            tvMessage.text = "Standard Emergency Message"
            tvLocation.text = "Location: GPS High Accuracy"
            tvRecipients.text = "Recipients: Trusted Contacts"
            tvStatus.text = "Status: Completed"
        }

        findViewById<CardView>(R.id.btnViewOnMap).setOnClickListener {
            val intent = Intent(this, MapActivity::class.java).apply {
                if (event != null) {
                    putExtra("MAP_LAT", event.latitude)
                    putExtra("MAP_LNG", event.longitude)
                }
            }
            startActivity(intent)
        }
    }
}
