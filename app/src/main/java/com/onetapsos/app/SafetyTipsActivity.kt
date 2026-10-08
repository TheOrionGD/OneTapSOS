package com.onetapsos.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class SafetyTip(val category: String, val icon: String, val title: String, val body: String)

class SafetyTipsActivity : BaseActivity() {

    private val tips = listOf(
        SafetyTip("Personal Safety", "🚶", "Walking Alone at Night", "Stay in well-lit areas, avoid shortcuts through isolated paths, and keep your phone charged. Share your live location with a trusted contact."),
        SafetyTip("Personal Safety", "📱", "Keep Emergency Contacts Ready", "Store ICE (In Case of Emergency) contacts in your phone and ensure they are accessible even from a locked screen."),
        SafetyTip("Travel Safety", "✈️", "Travel Smart", "Research your destination, register with local embassy if abroad, and keep digital copies of all documents in secure cloud storage."),
        SafetyTip("Travel Safety", "🚗", "Road Safety", "Always wear a seatbelt, avoid distracted driving, and never drive under the influence. Share your route with someone before long trips."),
        SafetyTip("Home Safety", "🔒", "Secure Your Home", "Install proper locks, use a door chain, and never open your door to strangers without verification."),
        SafetyTip("Home Safety", "🔥", "Fire Safety", "Install smoke detectors, keep fire extinguishers accessible, and know your escape routes."),
        SafetyTip("Digital Safety", "🔐", "Protect Your Digital Identity", "Use strong unique passwords, enable 2FA, and be wary of phishing links in emails and messages."),
        SafetyTip("Natural Disasters", "🌊", "Flood Safety", "Move to higher ground immediately, avoid walking or driving through flood water, and listen to official evacuation orders."),
        SafetyTip("Natural Disasters", "🌍", "Earthquake Safety", "Drop, Cover, and Hold On. Stay away from windows and heavy furniture. After shaking stops, check for gas leaks."),
        SafetyTip("Medical", "🩺", "Know Your Allergies", "Always inform emergency responders about known allergies and carry necessary medications if you have a chronic condition."),
        SafetyTip("Medical", "🩸", "Know Your Blood Group", "Keep your blood group recorded in your emergency card and medical bracelet if possible."),
        SafetyTip("Public Safety", "🚇", "Public Transport Safety", "Stay alert in crowded spaces, keep valuables secured, and be aware of your surroundings at all times."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_tips)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivSafetyTipsBack)
        val rvTips = findViewById<RecyclerView>(R.id.rvSafetyTips)

        btnBack.setOnClickListener { finish() }
        rvTips.layoutManager = LinearLayoutManager(this)
        rvTips.adapter = SafetyTipsAdapter(tips)
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class SafetyTipsAdapter(private val tips: List<SafetyTip>) :
    RecyclerView.Adapter<SafetyTipsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvTipIcon)
        val tvCategory: TextView = view.findViewById(R.id.tvTipCategory)
        val tvTitle: TextView = view.findViewById(R.id.tvTipTitle)
        val tvBody: TextView = view.findViewById(R.id.tvTipBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_safety_tip, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tip = tips[position]
        holder.tvIcon.text = tip.icon
        holder.tvCategory.text = tip.category
        holder.tvTitle.text = tip.title
        holder.tvBody.text = tip.body
    }

    override fun getItemCount() = tips.size
}
