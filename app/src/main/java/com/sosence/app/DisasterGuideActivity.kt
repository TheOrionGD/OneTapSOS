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

data class DisasterGuide(val type: String, val icon: String, val before: String, val during: String, val after: String)

class DisasterGuideActivity : BaseActivity() {

    private val guides = listOf(
        DisasterGuide("Earthquake", "🌍",
            "Secure heavy furniture. Identify safe spots (under sturdy tables, against inner walls). Keep emergency kit ready.",
            "DROP, COVER, HOLD ON. Stay away from windows and exterior walls. If outdoors, move away from buildings.",
            "Check for injuries. Expect aftershocks. Do not use elevators. Report gas leaks immediately."),
        DisasterGuide("Flood", "🌊",
            "Know your evacuation routes. Keep emergency documents waterproofed. Move valuables to upper floors.",
            "Move to higher ground immediately. Never walk through floodwater. Avoid bridges over fast-moving water.",
            "Do not return until authorities say it is safe. Document damage for insurance. Disinfect everything."),
        DisasterGuide("Fire", "🔥",
            "Install smoke detectors. Plan and practise escape routes. Keep fire extinguishers accessible.",
            "Alert everyone and activate alarms. Stay low under smoke. Close doors behind you. Use stairs, not elevators.",
            "Do not re-enter until cleared by fire service. Contact emergency services. Seek medical help for smoke inhalation."),
        DisasterGuide("Cyclone/Typhoon", "🌀",
            "Reinforce windows and doors. Stock 72-hour emergency supplies. Follow weather alerts closely.",
            "Stay indoors away from windows. If ordered to evacuate, go immediately to designated shelter.",
            "Watch out for flooding and damaged structures. Avoid downed power lines. Document damage."),
        DisasterGuide("Landslide", "⛰️",
            "Be alert during heavy rain in hilly areas. Note warning signs: cracks, unusual sounds, tilting trees.",
            "Move away immediately — do not try to outrun it. Alert authorities and neighbours.",
            "Stay away from affected area for risk of further slides. Help search for missing persons with rescuers."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disaster_guide)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivDisasterBack)
        val rvGuides = findViewById<RecyclerView>(R.id.rvDisasterGuides)

        btnBack.setOnClickListener { finish() }
        rvGuides.layoutManager = LinearLayoutManager(this)
        rvGuides.adapter = DisasterGuideAdapter(guides)
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class DisasterGuideAdapter(private val items: List<DisasterGuide>) :
    RecyclerView.Adapter<DisasterGuideAdapter.VH>() {

    private val expanded = mutableSetOf<Int>()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvDisasterIcon)
        val tvType: TextView = view.findViewById(R.id.tvDisasterType)
        val tvBefore: TextView = view.findViewById(R.id.tvDisasterBefore)
        val tvDuring: TextView = view.findViewById(R.id.tvDisasterDuring)
        val tvAfter: TextView = view.findViewById(R.id.tvDisasterAfter)
        val layoutDetails: View = view.findViewById(R.id.layoutDisasterDetails)
        val card: androidx.cardview.widget.CardView = view.findViewById(R.id.cardDisaster)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_disaster_guide, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvIcon.text = item.icon
        holder.tvType.text = item.type
        holder.tvBefore.text = "📋 Before: ${item.before}"
        holder.tvDuring.text = "⚡ During: ${item.during}"
        holder.tvAfter.text = "✅ After: ${item.after}"
        holder.layoutDetails.visibility = if (expanded.contains(position)) View.VISIBLE else View.GONE
        holder.card.setOnClickListener {
            if (expanded.contains(position)) expanded.remove(position) else expanded.add(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = items.size
}
