package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

data class HelpCategory(
    val title: String,
    val icon: String,
    val items: List<Pair<String, Class<*>>>
)

class HelpAndSupportActivity : BaseActivity() {

    private val categories = listOf(
        HelpCategory(
            "1. EMERGENCY SOS & CRISIS RESPONSE",
            "🚨",
            listOf(
                "SOS Activation Trigger" to SOSActivationActivity::class.java,
                "SOS Cancel Countdown Buffer" to SOSCountdownActivity::class.java,
                "SOS Active Crisis Monitoring" to SOSActiveActivity::class.java,
                "SOS Resolved & I'm Safe Summary" to SOSResolvedActivity::class.java,
                "SOS Incident Details Log" to SOSDetailsActivity::class.java,
                "SOS Full History Records" to SOSHistoryActivity::class.java,
                "SOS System & Sensor Diagnostics" to SOSDiagnosticsActivity::class.java
            )
        ),
        HelpCategory(
            "2. PREVENTION, TIMERS & MONITORING",
            "🛡️",
            listOf(
                "Journey Mode & Route Escort" to JourneyModeActivity::class.java,
                "Active Route Sharing Monitor" to LocationShareActiveActivity::class.java,
                "Safety Countdown Timer" to SafetyTimerActivity::class.java,
                "Active Safety Timer Session" to SafetyTimerActiveActivity::class.java,
                "Scheduled Safety Check-In" to CheckInActivity::class.java,
                "Safety Check-In Log" to SafetyCheckInHistoryActivity::class.java,
                "Report Unsafe Situation" to UnsafeSituationActivity::class.java,
                "Incident Documentation Report" to IncidentReportActivity::class.java,
                "Incident History Records" to IncidentHistoryActivity::class.java,
                "Emergency Kit Preparation Checklist" to EmergencyPreparationActivity::class.java
            )
        ),
        HelpCategory(
            "3. SAFE MAP & LOCATION DISCOVERY",
            "🗺️",
            listOf(
                "Full Screen Interactive Safe Map" to MapActivity::class.java,
                "Safe Havens & Zones" to SafeMapActivity::class.java,
                "Nearby Police & Hospitals Directory" to NearbyHelpActivity::class.java,
                "Live Location Tracking & Breadcrumbs" to LiveTrackingActivity::class.java,
                "Location Timeline & History Log" to LocationHistoryActivity::class.java,
                "Location Sharing Settings" to LocationSharingSettingsActivity::class.java,
                "Map Display & Offline Settings" to MapSettingsActivity::class.java,
                "Location Permissions Guide" to LocationPermissionActivity::class.java
            )
        ),
        HelpCategory(
            "4. TRUSTED CIRCLE & MESSAGING",
            "👥",
            listOf(
                "Trusted Contacts Manager" to TrustedContactsActivity::class.java,
                "Add New Emergency Contact" to AddTrustedContactActivity::class.java,
                "Edit Contact & Custom Alerts" to EditTrustedContactActivity::class.java,
                "Contact Detail Information" to ContactDetailActivity::class.java,
                "Emergency Groups & Circles" to GroupsActivity::class.java,
                "Emergency Message Hub" to EmergencyMessagesActivity::class.java,
                "Custom I'm Safe Message Editor" to ImSafeMessageActivity::class.java,
                "Emergency Message Templates" to MessageTemplateActivity::class.java,
                "Emergency SMS Broadcast Launcher" to EmergencyBroadcastActivity::class.java,
                "Conversation & Delivery Threads" to ConversationActivity::class.java,
                "SMS & Contact Permissions Guide" to ContactPermissionActivity::class.java
            )
        ),
        HelpCategory(
            "5. MEDICAL ID, GUIDES & SETTINGS",
            "⚙️",
            listOf(
                "Medical Emergency ID Card" to EmergencyCardActivity::class.java,
                "User Profile & Emergency Alias" to ProfileActivity::class.java,
                "Emergency Numbers Directory (112, 100, 108)" to EmergencyNumbersActivity::class.java,
                "Offline First Aid Survival Guide" to FirstAidActivity::class.java,
                "Disaster Readiness & Survival Guide" to DisasterGuideActivity::class.java,
                "Personal Safety Tips & Best Practices" to SafetyTipsActivity::class.java,
                "AI Safety Companion Chat" to ChatActivity::class.java,
                "SOS Settings & Hardware Triggers" to SOSSettingsActivity::class.java,
                "Notification Preferences & Test Lab" to NotificationSettingsActivity::class.java,
                "Battery Safety & 5% Alert Settings" to BatterySafetyActivity::class.java,
                "Data Management & Clear Cache" to DataManagementActivity::class.java,
                "Appearance & Dark Mode" to AppearanceSettingsActivity::class.java,
                "Permissions Dashboard" to PermissionsActivity::class.java,
                "Background Safety Engine & Diagnostics" to BackgroundDiagnosticsActivity::class.java,
                "Fake Call Generator" to FakeCallActivity::class.java,
                "Home Screen Panic Widget Guide" to PanicWidgetActivity::class.java
            )
        )
    )

    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_and_support)

        findViewById<ImageView>(R.id.ivHelpBack).setOnClickListener { finish() }
        container = findViewById(R.id.llHelpCategoriesContainer)
        etSearch = findViewById(R.id.etHelpSearch)

        renderCategories("")

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderCategories(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderCategories(query: String) {
        container.removeAllViews()

        val isSearching = query.isNotEmpty()

        for (cat in categories) {
            val filteredItems = if (isSearching) {
                cat.items.filter { it.first.contains(query, ignoreCase = true) }
            } else {
                cat.items
            }

            if (filteredItems.isEmpty()) continue

            // Category Header
            val headerTv = TextView(this).apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = dpToPx(16)
                params.bottomMargin = dpToPx(8)
                layoutParams = params
                text = "${cat.icon}  ${cat.title}"
                setTextColor(ContextCompat.getColor(this@HelpAndSupportActivity, R.color.accent_cyan))
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                letterSpacing = 0.06f
            }
            container.addView(headerTv)

            // Category Items Card Container
            val sectionCard = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setCardBackgroundColor(ContextCompat.getColor(this@HelpAndSupportActivity, R.color.card_bg))
                radius = dpToPx(14).toFloat()
                cardElevation = dpToPx(3).toFloat()
            }

            val itemsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            filteredItems.forEachIndexed { idx, (itemTitle, targetClass) ->
                val rowView = createItemRow(itemTitle, targetClass)
                itemsLayout.addView(rowView)

                if (idx < filteredItems.size - 1) {
                    val divider = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(1)
                        )
                        setBackgroundColor(0xFF2E334D.toInt())
                    }
                    itemsLayout.addView(divider)
                }
            }

            sectionCard.addView(itemsLayout)
            container.addView(sectionCard)
        }
    }

    private fun createItemRow(title: String, targetClass: Class<*>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            setBackgroundResource(android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                try {
                    startActivity(Intent(this@HelpAndSupportActivity, targetClass))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val tvTitle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = title
            setTextColor(ContextCompat.getColor(this@HelpAndSupportActivity, R.color.text_primary))
            textSize = 14f
        }

        val tvArrow = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "›"
            setTextColor(ContextCompat.getColor(this@HelpAndSupportActivity, R.color.text_muted))
            textSize = 18f
        }

        row.addView(tvTitle)
        row.addView(tvArrow)
        return row
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
