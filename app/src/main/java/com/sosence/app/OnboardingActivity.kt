package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : BaseActivity() {

    private val pages = listOf(
        OnboardingPage(
            step = "STEP 1 OF 4",
            emoji = "🛡️",
            title = "Instant Protection",
            description = "SOSense ensures your safety with rapid-response emergency alerts dispatched immediately to your circle.",
            highlight1 = "⚡ 1-Tap instant SOS trigger",
            highlight2 = "📶 Automated offline SMS backup delivery"
        ),
        OnboardingPage(
            step = "STEP 2 OF 4",
            emoji = "📍",
            title = "Live GPS Tracking",
            description = "Share real-time high precision GPS coordinates automatically so rescuers know your exact whereabouts.",
            highlight1 = "🗺️ OpenStreetMap live tracking link",
            highlight2 = "🔋 Low battery smart coordinate broadcast"
        ),
        OnboardingPage(
            step = "STEP 3 OF 4",
            emoji = "🤖",
            title = "AI Safety Companion",
            description = "Get proactive safety advice, risk assessment, and crisis guidance anywhere, anytime from your AI companion.",
            highlight1 = "💬 Instant safety answers & triage",
            highlight2 = "🔦 Smart situational preparedness check"
        ),
        OnboardingPage(
            step = "STEP 4 OF 4",
            emoji = "⚡",
            title = "One-Touch & Volume SOS",
            description = "Trigger urgent alerts silently by holding the SOS button or pressing Volume Down for 3 seconds.",
            highlight1 = "🔊 Silent volume key activation",
            highlight2 = "🛑 Anti-accidental countdown safeguard"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPagerOnboarding)
        val btnNext = findViewById<FrameLayout>(R.id.btnOnboardingNext)
        val btnSkip = findViewById<TextView>(R.id.tvOnboardingSkip)
        val tvBtnLabel = findViewById<TextView>(R.id.tvOnboardingNextLabel)
        val dotsContainer = findViewById<LinearLayout>(R.id.llOnboardingDots)

        viewPager.adapter = OnboardingAdapter(pages)
        setupDots(dotsContainer, 0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(dotsContainer, position)
                tvBtnLabel.text = if (position == pages.size - 1) "Get Started Now 🚀" else "Continue →"
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem++
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun setupDots(container: LinearLayout, activeIndex: Int) {
        container.removeAllViews()
        pages.indices.forEach { i ->
            val dot = View(this)
            val isSelected = i == activeIndex
            val width = if (isSelected) 28 else 8
            val params = LinearLayout.LayoutParams(dpToPx(width), dpToPx(8))
            params.marginEnd = dpToPx(6)
            dot.layoutParams = params
            dot.setBackgroundResource(if (isSelected) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            container.addView(dot)
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun finishOnboarding() {
        getSharedPreferences("sosense_prefs", MODE_PRIVATE).edit()
            .putBoolean("onboarding_done", true).apply()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}


data class OnboardingPage(
    val step: String,
    val emoji: String,
    val title: String,
    val description: String,
    val highlight1: String,
    val highlight2: String
)

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStepBadge: TextView = view.findViewById(R.id.tvOnboardStepBadge)
        val tvEmoji: TextView = view.findViewById(R.id.tvOnboardEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvOnboardTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvOnboardDesc)
        val tvHighlight1: TextView = view.findViewById(R.id.tvOnboardHighlight1)
        val tvHighlight2: TextView = view.findViewById(R.id.tvOnboardHighlight2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]
        holder.tvStepBadge.text = page.step
        holder.tvEmoji.text = page.emoji
        holder.tvTitle.text = page.title
        holder.tvDesc.text = page.description
        holder.tvHighlight1.text = page.highlight1
        holder.tvHighlight2.text = page.highlight2
    }

    override fun getItemCount() = pages.size
}
