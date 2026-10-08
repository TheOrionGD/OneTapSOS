package com.onetapsos.app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.onetapsos.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IncidentHistoryActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incident_history)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llIncidentsList)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        loadIncidents()
    }

    private fun loadIncidents() {
        container.removeAllViews()
        val incidents = dbHelper.getAllIncidents()

        if (incidents.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_state, container, false)
            emptyView.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Reported Incidents"
            emptyView.findViewById<TextView>(R.id.tvEmptyDesc).text = "Structured incident logs will appear here."
            container.addView(emptyView)
            return
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        for (inc in incidents) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_location_row, container, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvLocCoords)
            val tvTime = view.findViewById<TextView>(R.id.tvLocTime)
            val tvDesc = view.findViewById<TextView>(R.id.tvLocAddress)

            tvTitle.text = "${inc.category} (${inc.severity})"
            tvTime.text = sdf.format(Date(inc.timestamp))
            tvDesc.text = inc.description

            container.addView(view)
        }
    }
}
