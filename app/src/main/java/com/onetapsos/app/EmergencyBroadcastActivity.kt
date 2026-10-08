package com.onetapsos.app

import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.data.ConversationMessageModel

class EmergencyBroadcastActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_broadcast)

        dbHelper = AppDatabaseHelper(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val etText = findViewById<EditText>(R.id.etBroadcastText)
        val tvStatus = findViewById<TextView>(R.id.tvRecipientStatus)

        val contacts = dbHelper.getAllContacts()
        tvStatus.text = "Target: ${contacts.size} Trusted Contact(s)"

        findViewById<CardView>(R.id.btnSendBroadcast).setOnClickListener {
            val msg = etText.text.toString().trim()
            if (msg.isEmpty()) {
                Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contacts.isEmpty()) {
                Toast.makeText(this, "No contacts available to receive broadcast", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var count = 0
            for (c in contacts) {
                if (c.phone.isNotEmpty()) {
                    sendSms(c.phone, msg)
                    dbHelper.addMessage(ConversationMessageModel(
                        conversationId = "conv_${c.phone}",
                        senderId = "user_me",
                        receiverId = c.phone,
                        message = msg,
                        isSosRelated = true
                    ))
                    count++
                }
            }

            SOSNotificationManager.showBroadcastSentNotification(this, count)
            Toast.makeText(this, "📢 Broadcast sent to $count contacts!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun sendSms(phone: String, msg: String): Boolean {
        return try {
            val formatted = if (phone.startsWith("+")) phone else "+91$phone"
            val smsMgr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsMgr.divideMessage(msg)
            if (parts.size > 1) {
                smsMgr.sendMultipartTextMessage(formatted, null, parts, null, null)
            } else {
                smsMgr.sendTextMessage(formatted, null, msg, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
