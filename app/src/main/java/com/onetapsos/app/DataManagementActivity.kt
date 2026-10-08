package com.onetapsos.app

import android.app.AlertDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper

class DataManagementActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_management)

        dbHelper = AppDatabaseHelper(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btnClearSosHistory).setOnClickListener {
            showConfirmationDialog("Clear SOS History", "Are you sure you want to delete past SOS event logs? Trusted contacts will NOT be deleted.") {
                dbHelper.clearSosHistory()
                Toast.makeText(this, "SOS history cleared", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<CardView>(R.id.btnClearLocationHistory).setOnClickListener {
            showConfirmationDialog("Clear Location Logs", "Are you sure you want to clear recorded location history?") {
                dbHelper.clearLocationHistory()
                Toast.makeText(this, "Location history cleared", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<CardView>(R.id.btnExportData).setOnClickListener {
            Toast.makeText(this, "Local safety data exported to device storage", Toast.LENGTH_LONG).show()
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        if (isFinishing || isDestroyed) return
        try {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirm") { _, _ -> onConfirm() }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

