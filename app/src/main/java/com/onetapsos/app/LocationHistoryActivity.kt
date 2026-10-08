package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.onetapsos.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationHistoryActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_history)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llLocationsList)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        loadLocations()
    }

    private fun loadLocations() {
        container.removeAllViews()
        val locations = dbHelper.getLocationHistory()

        if (locations.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_state, container, false)
            emptyView.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Location Logs Recorded"
            emptyView.findViewById<TextView>(R.id.tvEmptyDesc).text = "Location fixes generated during SOS alerts and safety check-ins will appear here."
            container.addView(emptyView)
            return
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        for (loc in locations) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_location_row, container, false)
            val tvCoords = view.findViewById<TextView>(R.id.tvLocCoords)
            val tvTime = view.findViewById<TextView>(R.id.tvLocTime)
            val tvAddress = view.findViewById<TextView>(R.id.tvLocAddress)

            tvCoords.text = "📍 Lat: %.4f, Lng: %.4f".format(loc.latitude, loc.longitude)
            tvTime.text = sdf.format(Date(loc.timestamp))
            tvAddress.text = if (loc.address.isNotEmpty()) loc.address else "GPS Location Log"

            view.setOnClickListener {
                val intent = Intent(this, MapActivity::class.java).apply {
                    putExtra("MAP_LAT", loc.latitude)
                    putExtra("MAP_LNG", loc.longitude)
                }
                startActivity(intent)
            }

            container.addView(view)
        }
    }
}
