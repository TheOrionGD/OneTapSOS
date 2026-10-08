package com.onetapsos.app

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper

class AddTrustedContactActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trusted_contact)

        dbHelper = AppDatabaseHelper(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etContactName)
        val etPhone = findViewById<EditText>(R.id.etContactPhone)
        val etPriority = findViewById<EditText>(R.id.etContactPriority)
        val etCustomSosMessage = findViewById<EditText>(R.id.etCustomSosMessage)

        findViewById<CardView>(R.id.btnSaveContact).setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val priority = etPriority.text.toString().toIntOrNull() ?: 1
            val customMessage = etCustomSosMessage.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter contact name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.length < 10) {
                Toast.makeText(this, "Please enter valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Duplicate check
            val existing = dbHelper.getAllContacts()
            if (existing.any { it.phone == phone }) {
                Toast.makeText(this, "Contact with phone $phone already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            dbHelper.addContact(name, phone, priority, customMessage)
            Toast.makeText(this, "✅ Trusted contact $name saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

