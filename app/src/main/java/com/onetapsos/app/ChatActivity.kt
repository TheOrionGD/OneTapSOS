package com.onetapsos.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : BaseActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etChatInput: EditText

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-3.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content {
                text("You are OneTapSOS AI, an intelligent, calm, empathetic, and rapid crisis safety companion. You provide direct, actionable personal safety advice, disaster survival protocols, emergency first aid steps, and situational risk assessments. Keep responses concise, structured, and prioritized by life-saving importance.")
            }
        )
    }

    private val chat by lazy { generativeModel.startChat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        animateEntrance()

        // 1. Header Navigation
        findViewById<ImageView>(R.id.ivChatBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.ivClearChat).setOnClickListener {
            showClearChatDialog()
        }

        // 2. Chat RecyclerView
        val messages = mutableListOf<ChatMessage>()
        chatAdapter = ChatAdapter(messages)

        rvChatMessages = findViewById(R.id.rvChatMessages)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChatMessages.layoutManager = layoutManager
        rvChatMessages.adapter = chatAdapter

        // 3. Input & Send
        etChatInput = findViewById(R.id.etChatInput)
        val btnSend = findViewById<CardView>(R.id.btnSendChatContainer)

        btnSend.setOnClickListener {
            val text = etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                submitUserPrompt(text)
                etChatInput.text.clear()
            }
        }

        // 4. Suggestion Chips
        setupSuggestionChips()

        // 5. Initial AI Greeting
        addBotMessage("👋 Hello! I am OneTapSOS AI, your personal safety companion.\n\nAsk me for emergency first aid protocols, disaster survival guides, or situational risk advice.")
    }

    private fun setupSuggestionChips() {
        findViewById<View>(R.id.chipEarthquake)?.setOnClickListener {
            submitUserPrompt("What are the immediate survival steps during an earthquake?")
        }

        findViewById<View>(R.id.chipBleeding)?.setOnClickListener {
            submitUserPrompt("How do I perform emergency first aid to stop severe bleeding?")
        }

        findViewById<View>(R.id.chipCommute)?.setOnClickListener {
            submitUserPrompt("Give me a safety checklist for late night solo commutes.")
        }

        findViewById<View>(R.id.chipSuspicious)?.setOnClickListener {
            submitUserPrompt("What should I do if I notice someone following me or feel unsafe?")
        }
    }

    private fun submitUserPrompt(prompt: String) {
        addUserMessage(prompt)
        handleUserInput(prompt)
    }

    private fun addUserMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, true))
        scrollToBottom()
    }

    private fun addBotMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, false))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        rvChatMessages.post {
            if (chatAdapter.itemCount > 0) {
                rvChatMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun handleUserInput(prompt: String) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "\"\"" || apiKey == "YOUR_GEMINI_API_KEY") {
            window.decorView.postDelayed({
                addBotMessage(
                    "⚠️ Gemini AI Offline Mode:\n\n" +
                    "To enable real-time generative safety intelligence, configure your GEMINI_API_KEY in local.properties.\n\n" +
                    "🚨 For immediate emergencies, use the Emergency SOS button or dial 112 / 100 / 108 directly from the Hotlines tab."
                )
            }, 500)
            return
        }

        addBotMessage("Analyzing safety protocols...")
        val typingIndex = chatAdapter.itemCount - 1

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = chat.sendMessage(prompt)
                withContext(Dispatchers.Main) {
                    val reply = response.text ?: "I am here to assist with safety protocols. Please provide more details."
                    chatAdapter.updateMessage(typingIndex, reply)
                    scrollToBottom()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatAdapter.updateMessage(
                        typingIndex,
                        "⚠️ Unable to reach AI safety service (${e.localizedMessage ?: "Network error"}). For active crises, trigger SOS or dial 112 immediately."
                    )
                    scrollToBottom()
                }
            }
        }
    }

    private fun showClearChatDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Conversation")
            .setMessage("Are you sure you want to clear the AI chat history?")
            .setPositiveButton("Clear") { _, _ ->
                chatAdapter.clearMessages()
                addBotMessage("Chat history cleared. How can I assist with your safety today?")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(240)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}
