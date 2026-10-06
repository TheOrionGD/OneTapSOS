package com.sosence.app

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView

class ImSafeMessageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_im_safe_message)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val etImSafeText = findViewById<EditText>(R.id.etImSafeText)
        etImSafeText.setText(appSettings.customSafeMessage)

        findViewById<CardView>(R.id.btnResetDefault).setOnClickListener {
            etImSafeText.setText(AppSettings.DEFAULT_SAFE_MESSAGE)
            Toast.makeText(this, "Reset to default message", Toast.LENGTH_SHORT).show()
        }

        findViewById<CardView>(R.id.btnSaveImSafe).setOnClickListener {
            val text = etImSafeText.text.toString().trim()
            if (text.isNotEmpty()) {
                appSettings.customSafeMessage = text
                Toast.makeText(this, "I'm Safe message saved locally", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
