package com.sosence.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class FirstAidGuide(val title: String, val icon: String, val steps: List<String>)

class FirstAidActivity : BaseActivity() {

    private val guides = listOf(
        FirstAidGuide("CPR (Cardiopulmonary Resuscitation)", "❤️", listOf(
            "Call emergency services (112 / 911) immediately.",
            "Place the person on their back on a firm, flat surface.",
            "Kneel beside them and place the heel of your hand on the center of their chest.",
            "Push down hard and fast — at least 2 inches deep, 100–120 times per minute.",
            "If trained, give 2 rescue breaths after every 30 compressions.",
            "Continue until emergency services arrive or the person recovers."
        )),
        FirstAidGuide("Choking — Heimlich Maneuver", "🤧", listOf(
            "Ask the person if they are choking. If they cannot speak, act immediately.",
            "Stand behind them and wrap your arms around their waist.",
            "Make a fist with one hand and place it thumb-side in just above the navel.",
            "Grab your fist with your other hand and give firm upward thrusts.",
            "Repeat until the object is dislodged or emergency services arrive."
        )),
        FirstAidGuide("Severe Bleeding", "🩸", listOf(
            "Call emergency services immediately for serious wounds.",
            "Apply firm direct pressure to the wound with a clean cloth or bandage.",
            "Do not remove the cloth — add more layers if blood soaks through.",
            "Keep pressure for at least 15 minutes continuously.",
            "Elevate the injured limb above heart level if possible.",
            "Monitor for signs of shock: pale skin, rapid breathing, confusion."
        )),
        FirstAidGuide("Burns", "🔥", listOf(
            "Move the person away from the heat source immediately.",
            "Cool the burn under cool (not cold) running water for 20 minutes.",
            "Do NOT use ice, butter, or toothpaste on burns.",
            "Cover loosely with a clean, non-fluffy material.",
            "Seek medical attention for burns larger than a palm or on face/hands."
        )),
        FirstAidGuide("Seizures", "⚡", listOf(
            "Stay calm and time the seizure.",
            "Clear the area of hard/sharp objects.",
            "Gently place something soft under the head.",
            "Do NOT restrain the person or put anything in their mouth.",
            "Turn them on their side when convulsions stop (recovery position).",
            "Call emergency services if seizure lasts more than 5 minutes."
        )),
        FirstAidGuide("Stroke — FAST Recognition", "🧠", listOf(
            "Face: Ask them to smile. Is one side drooping?",
            "Arms: Ask them to raise both arms. Does one drift down?",
            "Speech: Ask them to repeat a simple phrase. Is speech slurred?",
            "Time: If ANY sign is present, call emergency services IMMEDIATELY.",
            "Do NOT give them food or water — swallowing may be impaired."
        )),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_aid)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivFirstAidBack)
        val rvGuides = findViewById<RecyclerView>(R.id.rvFirstAid)

        btnBack.setOnClickListener { finish() }
        rvGuides.layoutManager = LinearLayoutManager(this)
        rvGuides.adapter = FirstAidAdapter(guides)
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class FirstAidAdapter(private val guides: List<FirstAidGuide>) :
    RecyclerView.Adapter<FirstAidAdapter.VH>() {

    private val expanded = mutableSetOf<Int>()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvFirstAidIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvFirstAidTitle)
        val tvSteps: TextView = view.findViewById(R.id.tvFirstAidSteps)
        val card: androidx.cardview.widget.CardView = view.findViewById(R.id.cardFirstAid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_first_aid, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val guide = guides[position]
        holder.tvIcon.text = guide.icon
        holder.tvTitle.text = guide.title

        if (expanded.contains(position)) {
            holder.tvSteps.visibility = View.VISIBLE
            holder.tvSteps.text = guide.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n\n")
        } else {
            holder.tvSteps.visibility = View.GONE
        }

        holder.card.setOnClickListener {
            if (expanded.contains(position)) expanded.remove(position) else expanded.add(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = guides.size
}
