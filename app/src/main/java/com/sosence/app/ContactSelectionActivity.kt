package com.sosence.app

import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.sosence.app.data.AppDatabaseHelper

class ContactSelectionActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private val selectedPhones = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_selection)

        dbHelper = AppDatabaseHelper(this)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.llContactCheckboxes)
        val contacts = dbHelper.getAllContacts()

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts available", Toast.LENGTH_SHORT).show()
        }

        for (c in contacts) {
            val checkBox = CheckBox(this).apply {
                text = "${c.name} (${c.phone})"
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 15f
                isChecked = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!selectedPhones.contains(c.phone)) selectedPhones.add(c.phone)
                    } else {
                        selectedPhones.remove(c.phone)
                    }
                }
            }
            selectedPhones.add(c.phone)
            container.addView(checkBox)
        }

        findViewById<CardView>(R.id.btnConfirmSelection).setOnClickListener {
            appSettings.sosRecipients = selectedPhones
            Toast.makeText(this, "Selected ${selectedPhones.size} contacts for SOS", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
