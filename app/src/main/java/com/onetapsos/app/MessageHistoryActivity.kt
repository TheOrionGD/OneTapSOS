package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.onetapsos.app.data.AppDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageHistoryActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_history)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llMessagesList)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        loadMessageHistory()
    }

    private fun loadMessageHistory() {
        container.removeAllViews()
        val messages = dbHelper.getAllMessages()

        if (messages.isEmpty()) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_state, container, false)
            emptyView.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Sent Messages Yet"
            emptyView.findViewById<TextView>(R.id.tvEmptyDesc).text = "Sent emergency broadcasts and SOS alerts will be logged here."
            container.addView(emptyView)
            return
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        for (msg in messages) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_message_row, container, false)
            val tvRecipient = view.findViewById<TextView>(R.id.tvMsgRecipient)
            val tvTime = view.findViewById<TextView>(R.id.tvMsgTime)
            val tvText = view.findViewById<TextView>(R.id.tvMsgText)

            tvRecipient.text = "To: ${msg.receiverId}"
            tvTime.text = sdf.format(Date(msg.timestamp))
            tvText.text = msg.message

            view.setOnClickListener {
                val intent = Intent(this, ConversationActivity::class.java).apply {
                    putExtra("CONVERSATION_ID", msg.conversationId)
                    putExtra("CONTACT_PHONE", msg.receiverId)
                }
                startActivity(intent)
            }

            container.addView(view)
        }
    }
}
