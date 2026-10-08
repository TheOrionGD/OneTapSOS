package com.onetapsos.app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.onetapsos.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyCheckInHistoryActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_check_in_history)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llCheckInsList)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        loadCheckIns()
    }

    private fun loadCheckIns() {
        container.removeAllViews()
        val checkIns = dbHelper.getAllCheckIns()

        if (checkIns.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_state, container, false)
            emptyView.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Safety Check-Ins"
            emptyView.findViewById<TextView>(R.id.tvEmptyDesc).text = "Completed and missed scheduled safety check-ins will be logged here."
            container.addView(emptyView)
            return
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        for (ci in checkIns) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_location_row, container, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvLocCoords)
            val tvTime = view.findViewById<TextView>(R.id.tvLocTime)
            val tvNote = view.findViewById<TextView>(R.id.tvLocAddress)

            tvTitle.text = "Status: ${ci.status} ✅"
            tvTime.text = sdf.format(Date(ci.timestamp))
            tvNote.text = if (ci.note.isNotEmpty()) ci.note else "Scheduled check-in completed"

            container.addView(view)
        }
    }
}
