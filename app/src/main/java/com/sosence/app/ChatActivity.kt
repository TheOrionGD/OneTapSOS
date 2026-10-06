package com.sosence.app

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
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

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text("You are SOSense AI, a helpful and empathetic safety assistant. You provide practical safety advice, help users assess risks, and can guide them on what to do in emergencies. Keep responses concise, supportive, and focused on personal safety.") }
        )
    }
    
    private val chat by lazy { generativeModel.startChat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Screen entrance animation
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        val messages = mutableListOf<ChatMessage>()
        chatAdapter = ChatAdapter(messages)

        rvChatMessages = findViewById(R.id.rvChatMessages)
        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = chatAdapter

        val etChatInput = findViewById<EditText>(R.id.etChatInput)
        val btnSend = findViewById<TextView>(R.id.btnSendChat)

        addBotMessage("Hello! I'm SOSense AI, your personal safety assistant. How can I help you today?")

        btnSend.setOnClickListener {
            val text = etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                etChatInput.text.clear()
                handleUserInput(text)
            }
        }
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
            rvChatMessages.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun handleUserInput(prompt: String) {
        if (BuildConfig.GEMINI_API_KEY == "YOUR_API_KEY_HERE" || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            addBotMessage("⚠️ Please set your GEMINI_API_KEY in local.properties to enable AI chat.")
            return
        }

        addBotMessage("...") // Typing indicator
        val typingIndex = chatAdapter.itemCount - 1

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = chat.sendMessage(prompt)
                withContext(Dispatchers.Main) {
                    // Remove typing indicator and add real response
                    chatAdapter.updateMessage(typingIndex, response.text ?: "I'm sorry, I couldn't generate a response.")
                    scrollToBottom()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatAdapter.updateMessage(typingIndex, "Error: ${e.localizedMessage}")
                    scrollToBottom()
                }
            }
        }
    }
}
