package com.sosence.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.widget.Toast

class EmergencyCardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_card)
        animateEntrance()

        val prefs = getSharedPreferences("sosense_prefs", MODE_PRIVATE)

        val tvName = findViewById<TextView>(R.id.tvCardName)
        val tvBlood = findViewById<TextView>(R.id.tvCardBloodGroup)
        val tvMedical = findViewById<TextView>(R.id.tvCardMedical)
        val tvEmerg = findViewById<TextView>(R.id.tvCardEmergContact)
        val btnBack = findViewById<ImageView>(R.id.ivEmergCardBack)
        val btnShare = findViewById<CardView>(R.id.btnShareEmergCard)

        tvName.text = prefs.getString("user_name", "Not set")
        tvBlood.text = prefs.getString("blood_group", "Not set")
        tvMedical.text = prefs.getString("medical_info", "None")
        tvEmerg.text = prefs.getString("emerg_contact", "Not set")

        btnBack.setOnClickListener { finish() }
        btnShare.setOnClickListener {
            val info = """
                🆘 EMERGENCY CARD - SOSense
                Name: ${tvName.text}
                Blood Group: ${tvBlood.text}
                Medical Info: ${tvMedical.text}
                Emergency Contact: ${tvEmerg.text}
            """.trimIndent()
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, info)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Emergency Card"))
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
