package com.sosence.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SafeZone(
    val name: String,
    val type: String,
    val icon: String,
    val distance: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "Address not available",
    val phone: String = "Not available"
)

class SafeZoneAdapter(
    private val zones: List<SafeZone>,
    private val onZoneClick: (SafeZone) -> Unit,
    private val onDetailsClick: (SafeZone) -> Unit
) : RecyclerView.Adapter<SafeZoneAdapter.SafeZoneViewHolder>() {

    class SafeZoneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvZoneIcon)
        val tvName: TextView = view.findViewById(R.id.tvZoneName)
        val tvType: TextView = view.findViewById(R.id.tvZoneType)
        val tvDistance: TextView = view.findViewById(R.id.tvZoneDistance)
        val btnDetails: TextView = view.findViewById(R.id.btnZoneDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafeZoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_safe_zone, parent, false)
        return SafeZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: SafeZoneViewHolder, position: Int) {
        val zone = zones[position]
        holder.tvIcon.text = zone.icon
        holder.tvName.text = zone.name
        holder.tvType.text = zone.type
        holder.tvDistance.text = zone.distance

        holder.itemView.setOnClickListener {
            onZoneClick(zone)
        }

        holder.btnDetails.setOnClickListener {
            onDetailsClick(zone)
        }
    }

    override fun getItemCount(): Int = zones.size
}
