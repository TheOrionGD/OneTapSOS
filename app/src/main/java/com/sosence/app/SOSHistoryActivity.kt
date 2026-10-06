package com.sosence.app

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SOSEvent(val timestamp: Long, val message: String, val recipientCount: Int)

class SOSHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_history)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivHistoryBack)
        val rvHistory = findViewById<RecyclerView>(R.id.rvSOSHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvHistoryEmpty)

        btnBack.setOnClickListener { finish() }

        val prefs = getSharedPreferences("sosense_prefs", MODE_PRIVATE)
        val historySet = prefs.getStringSet("sos_history", emptySet()) ?: emptySet()
        val events = historySet.mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 3) SOSEvent(parts[0].toLongOrNull() ?: 0L, parts[1], parts[2].toIntOrNull() ?: 0) else null
        }.sortedByDescending { it.timestamp }

        if (events.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            rvHistory.layoutManager = LinearLayoutManager(this)
            rvHistory.adapter = SOSHistoryAdapter(events)
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class SOSHistoryAdapter(private val events: List<SOSEvent>) :
    RecyclerView.Adapter<SOSHistoryAdapter.VH>() {

    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val tvMsg: TextView = view.findViewById(R.id.tvHistoryMsg)
        val tvRecipients: TextView = view.findViewById(R.id.tvHistoryRecipients)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sos_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = events[position]
        holder.tvTime.text = sdf.format(Date(e.timestamp))
        holder.tvMsg.text = e.message
        holder.tvRecipients.text = "Sent to ${e.recipientCount} contact(s)"
    }

    override fun getItemCount() = events.size
}
