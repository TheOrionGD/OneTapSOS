package com.sosence.app

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactsActivity : BaseActivity() {

    private lateinit var dbHelper: ContactsDatabaseHelper
    private lateinit var adapter: ContactsAdapter
    private val contactsList = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        // Screen entrance animation
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        dbHelper = ContactsDatabaseHelper(this)

        val etName = findViewById<EditText>(R.id.etContactName)
        val etPhone = findViewById<EditText>(R.id.etContactPhone)
        val btnAdd = findViewById<CardView>(R.id.btnAddContact)
        val rvContacts = findViewById<RecyclerView>(R.id.rvContacts)

        adapter = ContactsAdapter(contactsList) { position ->
            deleteContact(position)
        }
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = adapter

        loadContacts()

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter name and phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.length < 10) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addContact(name, phone)
            etName.text.clear()
            etPhone.text.clear()
        }
    }

    private fun loadContacts() {
        contactsList.clear()
        contactsList.addAll(dbHelper.getAllContacts())
        adapter.notifyDataSetChanged()
    }

    private fun addContact(name: String, phone: String) {
        val insertedId = dbHelper.addContact(name, phone)
        if (insertedId != -1L) {
            Toast.makeText(this, "Contact added locally", Toast.LENGTH_SHORT).show()
            loadContacts()
        } else {
            Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteContact(position: Int) {
        if (position in 0 until contactsList.size) {
            val contact = contactsList[position]
            dbHelper.deleteContact(contact.id)
            dbHelper.deleteContactByNameAndPhone(contact.name, contact.phone)
            loadContacts()
            Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
        }
    }
}
