package com.onetapsos.app

import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.data.ConversationMessageModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout
    private var conversationId: String = ""
    private var contactPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        dbHelper = AppDatabaseHelper(this)

        contactPhone = intent.getStringExtra("CONTACT_PHONE") ?: "+91 9876543210"
        conversationId = intent.getStringExtra("CONVERSATION_ID") ?: "conv_$contactPhone"

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        val tvName = findViewById<TextView>(R.id.tvContactName)
        val tvPhone = findViewById<TextView>(R.id.tvContactPhone)

        val contact = dbHelper.getAllContacts().find { it.phone == contactPhone }
        tvName.text = contact?.name ?: "Emergency Contact"
        tvPhone.text = contactPhone

        container = findViewById(R.id.llThreadMessages)
        val etInput = findViewById<EditText>(R.id.etInputMessage)

        findViewById<CardView>(R.id.btnSendMsg).setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendSms(contactPhone, text)
                dbHelper.addMessage(ConversationMessageModel(
                    conversationId = conversationId,
                    senderId = "user_me",
                    receiverId = contactPhone,
                    message = text,
                    isSosRelated = false
                ))
                etInput.text.clear()
                loadConversationThread()
            }
        }

        loadConversationThread()
    }

    private fun loadConversationThread() {
        container.removeAllViews()
        // Strict filtering by conversationId
        val messages = dbHelper.getMessagesForConversation(conversationId)

        if (messages.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_state, container, false)
            emptyView.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Messages in Thread"
            emptyView.findViewById<TextView>(R.id.tvEmptyDesc).text = "Start a direct emergency message exchange with this contact."
            container.addView(emptyView)
            return
        }

        val sdf = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
        for (m in messages) {
            val isMe = m.senderId == "user_me"
            val view = LayoutInflater.from(this).inflate(
                if (isMe) R.layout.item_chat_sent else R.layout.item_chat_received,
                container,
                false
            )
            view.findViewById<TextView>(R.id.tvChatMessage).text = m.message
            view.findViewById<TextView>(R.id.tvChatTime).text = sdf.format(Date(m.timestamp))
            container.addView(view)
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
            Toast.makeText(this, "Failed to send SMS to $phone", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
