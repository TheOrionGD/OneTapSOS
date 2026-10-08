package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onetapsos.app.data.AppDatabaseHelper
import com.onetapsos.app.data.SosEventModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSHistoryActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SOSHistoryAdapter
    private val eventsList = mutableListOf<SosEventModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos_history)
        animateEntrance()

        dbHelper = AppDatabaseHelper(this)

        val btnBack = findViewById<ImageView>(R.id.ivHistoryBack)
        rvHistory = findViewById(R.id.rvSOSHistory)
        tvEmpty = findViewById(R.id.tvHistoryEmpty)

        btnBack.setOnClickListener { finish() }

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = SOSHistoryAdapter(eventsList) { event ->
            val intent = Intent(this, SOSDetailsActivity::class.java).apply {
                putExtra("EVENT_ID", event.id)
            }
            startActivity(intent)
        }
        rvHistory.adapter = adapter

        loadSosHistory()
    }

    override fun onResume() {
        super.onResume()
        loadSosHistory()
    }

    private fun loadSosHistory() {
        val records = dbHelper.getAllSosEvents()
        eventsList.clear()
        eventsList.addAll(records)
        adapter.notifyDataSetChanged()

        if (records.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class SOSHistoryAdapter(
    private val events: List<SosEventModel>,
    private val onItemClick: (SosEventModel) -> Unit
) : RecyclerView.Adapter<SOSHistoryAdapter.VH>() {

    private val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTriggerType: TextView = view.findViewById(R.id.tvHistoryTriggerType)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val tvMsg: TextView = view.findViewById(R.id.tvHistoryMsg)
        val tvLocation: TextView = view.findViewById(R.id.tvHistoryLocation)
        val tvRecipients: TextView = view.findViewById(R.id.tvHistoryRecipients)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_sos_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = events[position]
        val ctx = holder.itemView.context

        // Trigger Type
        val triggerLabel = when (e.triggerType) {
            "FALL_DETECTION" -> "⚠️ Fall Detection"
            "VOLUME_KEY" -> "🔊 Hardware Key"
            "WIDGET" -> "📱 Panic Widget"
            "TILE" -> "⚡ Quick Tile"
            else -> "🚨 Emergency SOS"
        }
        holder.tvTriggerType.text = triggerLabel

        // Status badge
        if (e.isResolved) {
            holder.tvStatus.text = if (e.durationSeconds > 0) "Resolved (${e.durationSeconds}s)" else "Resolved"
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_granted)
        } else {
            holder.tvStatus.text = "Active 🚨"
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.danger_red))
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pill)
        }

        // Time
        val timeFormatted = if (e.formattedTime.isNotBlank()) e.formattedTime else sdf.format(Date(e.timestamp))
        holder.tvTime.text = timeFormatted

        // Message
        holder.tvMsg.text = e.message.ifBlank { "Emergency SOS Alert dispatched" }

        // Location
        val locText = if (e.locationName.isNotBlank() && e.locationName != "Unknown") {
            "📍 ${e.locationName}"
        } else if (e.latitude != 0.0 || e.longitude != 0.0) {
            "📍 GPS: %.4f, %.4f".format(e.latitude, e.longitude)
        } else {
            "📍 Location unavailable"
        }
        holder.tvLocation.text = locText

        // Recipients
        holder.tvRecipients.text = "👥 ${e.recipientsCount} contact(s)"

        holder.itemView.setOnClickListener {
            onItemClick(e)
        }
    }

    override fun getItemCount() = events.size
}
