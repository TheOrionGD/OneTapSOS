package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper

class TrustedContactsActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trusted_contacts)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llContactsList)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val openAdd = { startActivity(Intent(this, AddTrustedContactActivity::class.java)) }
        findViewById<TextView>(R.id.btnAddContactHeader).setOnClickListener { openAdd() }
        findViewById<CardView>(R.id.btnAddNewContact).setOnClickListener { openAdd() }

        loadContacts()
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
    }

    private fun loadContacts() {
        container.removeAllViews()
        val contacts = dbHelper.getAllContacts()

        if (contacts.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_state, container, false)
            emptyView.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Trusted Contacts"
            emptyView.findViewById<TextView>(R.id.tvEmptyDesc).text = "Add family members or close friends who should receive emergency SOS alerts."
            container.addView(emptyView)
            return
        }

        for (c in contacts) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_trusted_contact_row, container, false)
            val tvName = view.findViewById<TextView>(R.id.tvContactRowName)
            val tvPhone = view.findViewById<TextView>(R.id.tvContactRowPhone)
            val tvPriority = view.findViewById<TextView>(R.id.tvContactPriority)
            val tvCustomMsgBadge = view.findViewById<TextView>(R.id.tvCustomMsgBadge)
            val btnEdit = view.findViewById<TextView>(R.id.btnEditRow)
            val btnDelete = view.findViewById<TextView>(R.id.btnDeleteRow)

            tvName.text = c.name
            tvPhone.text = c.phone
            tvPriority.text = "Priority ${c.priority}"
            if (c.customMessage.isNotBlank()) {
                tvCustomMsgBadge.visibility = android.view.View.VISIBLE
            } else {
                tvCustomMsgBadge.visibility = android.view.View.GONE
            }

            btnEdit.setOnClickListener {
                val intent = Intent(this, EditTrustedContactActivity::class.java).apply {
                    putExtra("CONTACT_ID", c.id)
                    putExtra("CONTACT_NAME", c.name)
                    putExtra("CONTACT_PHONE", c.phone)
                    putExtra("CONTACT_PRIORITY", c.priority)
                    putExtra("CONTACT_CUSTOM_MESSAGE", c.customMessage)
                }
                startActivity(intent)
            }


            btnDelete.setOnClickListener {
                dbHelper.deleteContact(c.id)
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
                loadContacts()
            }

            view.setOnClickListener {
                val intent = Intent(this, ContactDetailActivity::class.java).apply {
                    putExtra("CONTACT_NAME", c.name)
                    putExtra("CONTACT_PHONE", c.phone)
                }
                startActivity(intent)
            }

            container.addView(view)
        }
    }
}
