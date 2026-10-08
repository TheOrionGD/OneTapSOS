package com.onetapsos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.onetapsos.app.R
import com.onetapsos.app.data.SafetyEventRecord

class SafetyEventAdapter(
    private var events: List<SafetyEventRecord>
) : RecyclerView.Adapter<SafetyEventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvEventIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvPriorityBadge: TextView = view.findViewById(R.id.tvEventPriorityBadge)
        val tvDetails: TextView = view.findViewById(R.id.tvEventDetails)
        val tvTimestamp: TextView = view.findViewById(R.id.tvEventTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_safety_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        val context = holder.itemView.context

        holder.tvTimestamp.text = event.formattedTime
        holder.tvDetails.text = event.details
        holder.tvPriorityBadge.text = event.priority

        when (event.eventType) {
            "LOW_BATTERY" -> {
                holder.tvIcon.text = "🔋"
                holder.tvTitle.text = "Low Battery Alert (${event.batteryLevel}%)"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_alert)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow))
            }
            "CRITICAL_BATTERY" -> {
                holder.tvIcon.text = "🪫"
                holder.tvTitle.text = "Critical Battery Event"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_alert)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.danger_red))
            }
            "CHARGING_STARTED" -> {
                holder.tvIcon.text = "⚡"
                holder.tvTitle.text = "Power Connected"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_emerald)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.success_green))
            }
            "CHARGING_STOPPED" -> {
                holder.tvIcon.text = "🔌"
                holder.tvTitle.text = "Power Disconnected"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_neutral)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            "SAFETY_TIMER_EXPIRED" -> {
                holder.tvIcon.text = "⏱️"
                holder.tvTitle.text = "Safety Timer Expired"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_alert)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow))
            }
            "CHECKIN_EXPIRED" -> {
                holder.tvIcon.text = "📍"
                holder.tvTitle.text = "Check-In Missed"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_alert)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_yellow))
            }
            "BOOT_RESTORED" -> {
                holder.tvIcon.text = "🔄"
                holder.tvTitle.text = "System Boot Recovery"
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_accent)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_cyan))
            }
            else -> {
                holder.tvIcon.text = "🛡️"
                holder.tvTitle.text = event.eventType.replace("_", " ")
                holder.tvPriorityBadge.setBackgroundResource(R.drawable.bg_pill_neutral)
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<SafetyEventRecord>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
