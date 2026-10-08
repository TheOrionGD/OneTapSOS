package com.onetapsos.app

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper

class IncidentReportActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident_report)

        dbHelper = AppDatabaseHelper(this)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val etCategory = findViewById<EditText>(R.id.etIncidentCategory)
        val etDesc = findViewById<EditText>(R.id.etIncidentDesc)
        val rbHigh = findViewById<RadioButton>(R.id.rbHigh)
        val rbLow = findViewById<RadioButton>(R.id.rbLow)

        val preselect = intent.getStringExtra("PRESELECT_CATEGORY")
        if (!preselect.isNullOrEmpty()) {
            etCategory.setText(preselect)
        }

        findViewById<CardView>(R.id.btnSaveIncident).setOnClickListener {
            val category = etCategory.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val severity = when {
                rbHigh.isChecked -> "Critical"
                rbLow.isChecked -> "Low"
                else -> "Medium"
            }

            if (category.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Please enter category and description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.addIncidentReport(category, desc, 20.5937, 78.9629, severity)
            Toast.makeText(this, "✅ Incident report saved locally", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
