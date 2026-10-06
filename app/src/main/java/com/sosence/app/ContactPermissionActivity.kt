package com.sosence.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class ContactPermissionActivity : BaseActivity() {

    private val requestPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { updateStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_permission)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        updateStatus()

        findViewById<CardView>(R.id.btnGrantPermissions).setOnClickListener {
            requestPermLauncher.launch(arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS
            ))
        }
    }

    private fun updateStatus() {
        val hasSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

        val tvSms = findViewById<TextView>(R.id.tvSmsStatus)
        val tvContacts = findViewById<TextView>(R.id.tvContactsStatus)

        tvSms.text = "SMS Permission Status: ${if (hasSms) "GRANTED ✅" else "NOT GRANTED ❌"}"
        tvSms.setTextColor(ContextCompat.getColor(this, if (hasSms) R.color.success_green else R.color.danger_red))

        tvContacts.text = "Contacts Permission Status: ${if (hasContacts) "GRANTED ✅" else "NOT GRANTED ❌"}"
        tvContacts.setTextColor(ContextCompat.getColor(this, if (hasContacts) R.color.success_green else R.color.danger_red))
    }
}
