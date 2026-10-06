package com.sosence.app

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.sosence.app.data.AppDatabaseHelper
import com.sosence.app.data.ContactModel

class EditTrustedContactActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private var contactId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_trusted_contact)

        dbHelper = AppDatabaseHelper(this)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        contactId = intent.getLongExtra("CONTACT_ID", -1L)
        val name = intent.getStringExtra("CONTACT_NAME") ?: ""
        val phone = intent.getStringExtra("CONTACT_PHONE") ?: ""
        val priority = intent.getIntExtra("CONTACT_PRIORITY", 1)
        val customMessage = intent.getStringExtra("CONTACT_CUSTOM_MESSAGE") ?: ""

        val etName = findViewById<EditText>(R.id.etEditName)
        val etPhone = findViewById<EditText>(R.id.etEditPhone)
        val etPriority = findViewById<EditText>(R.id.etEditPriority)
        val etCustomMessage = findViewById<EditText>(R.id.etEditCustomSosMessage)

        etName.setText(name)
        etPhone.setText(phone)
        etPriority.setText(priority.toString())
        etCustomMessage.setText(customMessage)

        findViewById<CardView>(R.id.btnUpdateContact).setOnClickListener {
            val updatedName = etName.text.toString().trim()
            val updatedPhone = etPhone.text.toString().trim()
            val updatedPriority = etPriority.text.toString().toIntOrNull() ?: 1
            val updatedCustomMessage = etCustomMessage.text.toString().trim()

            if (updatedName.isEmpty() || updatedPhone.length < 10) {
                Toast.makeText(this, "Please enter valid name and 10-digit phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbHelper.updateContact(ContactModel(
                id = contactId,
                name = updatedName,
                phone = updatedPhone,
                priority = updatedPriority,
                customMessage = updatedCustomMessage
            ))

            Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

