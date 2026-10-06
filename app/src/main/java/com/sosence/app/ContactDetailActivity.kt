package com.sosence.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class ContactDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_detail)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val name = intent.getStringExtra("CONTACT_NAME") ?: "Trusted Contact"
        val phone = intent.getStringExtra("CONTACT_PHONE") ?: "+91 9876543210"

        findViewById<TextView>(R.id.tvDetailContactName).text = name
        findViewById<TextView>(R.id.tvDetailContactPhone).text = phone

        findViewById<CardView>(R.id.btnCallContact).setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(callIntent)
        }

        findViewById<CardView>(R.id.btnOpenThread).setOnClickListener {
            val intent = Intent(this, ConversationActivity::class.java).apply {
                putExtra("CONTACT_PHONE", phone)
                putExtra("CONVERSATION_ID", "conv_$phone")
            }
            startActivity(intent)
        }
    }
}
