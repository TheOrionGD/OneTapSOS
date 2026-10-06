package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class UnsafeSituationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unsafe_situation)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val setupCard = { cardId: Int, category: String ->
            findViewById<CardView>(cardId).setOnClickListener {
                val intent = Intent(this, IncidentReportActivity::class.java).apply {
                    putExtra("PRESELECT_CATEGORY", category)
                }
                startActivity(intent)
            }
        }

        setupCard(R.id.cardFollowing, "Being Followed")
        setupCard(R.id.cardHarassment, "Harassment")
        setupCard(R.id.cardMedical, "Medical Emergency")
        setupCard(R.id.cardAccident, "Road Accident")
        setupCard(R.id.cardThreat, "Physical Threat")
        setupCard(R.id.cardLost, "Lost / Stranded")
    }
}
