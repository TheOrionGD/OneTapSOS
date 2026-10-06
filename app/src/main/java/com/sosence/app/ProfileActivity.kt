package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        animateEntrance()

        val prefs = getSharedPreferences("sosense_prefs", MODE_PRIVATE)
        val etName = findViewById<EditText>(R.id.etProfileName)
        val etBloodGroup = findViewById<EditText>(R.id.etProfileBloodGroup)
        val etMedical = findViewById<EditText>(R.id.etProfileMedical)
        val etEmergContact = findViewById<EditText>(R.id.etProfileEmergContact)
        val btnSave = findViewById<CardView>(R.id.btnSaveProfile)
        val btnBack = findViewById<ImageView>(R.id.ivProfileBack)
        val btnEmergCard = findViewById<CardView>(R.id.btnViewEmergCard)

        etName.setText(prefs.getString("user_name", ""))
        etBloodGroup.setText(prefs.getString("blood_group", ""))
        etMedical.setText(prefs.getString("medical_info", ""))
        etEmergContact.setText(prefs.getString("emerg_contact", ""))

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("user_name", etName.text.toString().trim())
                .putString("blood_group", etBloodGroup.text.toString().trim())
                .putString("medical_info", etMedical.text.toString().trim())
                .putString("emerg_contact", etEmergContact.text.toString().trim())
                .apply()
            Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener { finish() }

        btnEmergCard.setOnClickListener {
            startActivity(Intent(this, EmergencyCardActivity::class.java))
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
