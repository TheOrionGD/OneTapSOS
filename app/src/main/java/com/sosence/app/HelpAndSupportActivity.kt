package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView

class HelpAndSupportActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_and_support)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.llDirectoryButtons)
        val screens = listOf(
            "1. Splash Launch Screen" to SplashActivity::class.java,
            "2. Onboarding Intro" to OnboardingActivity::class.java,
            "3. Safety Dashboard (Main Hub)" to SafetyDashboardActivity::class.java,
            "4. SOS Activation Interface" to SOSActivationActivity::class.java,
            "5. SOS Countdown Screen" to SOSCountdownActivity::class.java,
            "6. SOS Active Monitoring" to SOSActiveActivity::class.java,
            "7. SOS Resolved Screen" to SOSResolvedActivity::class.java,
            "8. SOS Details Viewer" to SOSDetailsActivity::class.java,
            "9. SOS History Log" to SOSHistoryActivity::class.java,
            "10. SOS System Diagnostics" to SOSDiagnosticsActivity::class.java,
            "11. Custom I'm Safe Message Editor" to ImSafeMessageActivity::class.java,
            "12. Emergency Message Hub" to EmergencyMessagesActivity::class.java,
            "13. Message Templates Manager" to MessageTemplateActivity::class.java,
            "14. Emergency Broadcast Launcher" to EmergencyBroadcastActivity::class.java,
            "15. Message History Log" to MessageHistoryActivity::class.java,
            "16. Contact Conversation Thread" to ConversationActivity::class.java,
            "17. AI Safety Companion Chat" to ChatActivity::class.java,
            "18. Trusted Contacts Manager" to TrustedContactsActivity::class.java,
            "19. Add Trusted Contact Form" to AddTrustedContactActivity::class.java,
            "20. Edit Trusted Contact Form" to EditTrustedContactActivity::class.java,
            "21. Contact Detail Info" to ContactDetailActivity::class.java,
            "22. Contact Groups Manager" to GroupsActivity::class.java,
            "23. Emergency Contact Selector" to ContactSelectionActivity::class.java,
            "24. SMS & Contact Permissions" to ContactPermissionActivity::class.java,
            "25. OpenStreetMap Full View" to MapActivity::class.java,
            "26. Safe Map & Zones" to SafeMapActivity::class.java,
            "27. Safe Navigation Routes" to SafeRouteActivity::class.java,
            "28. Nearby Police & Hospitals" to NearbyHelpActivity::class.java,
            "29. Location Status Dashboard" to LocationDashboardActivity::class.java,
            "30. Live Location Tracking" to LiveTrackingActivity::class.java,
            "31. Location Timeline History" to LocationHistoryActivity::class.java,
            "32. Location Sharing Settings" to LocationSharingSettingsActivity::class.java,
            "33. Active Location Share Monitor" to LocationShareActiveActivity::class.java,
            "34. Location Permission Guide" to LocationPermissionActivity::class.java,
            "35. Map Display Settings" to MapSettingsActivity::class.java,
            "36. Scheduled Safety Check-In" to CheckInActivity::class.java,
            "37. Safety Check-In History" to SafetyCheckInHistoryActivity::class.java,
            "38. Safety Timer Setup" to SafetyTimerActivity::class.java,
            "39. Active Safety Timer" to SafetyTimerActiveActivity::class.java,
            "40. Journey Monitoring Mode" to JourneyModeActivity::class.java,
            "41. Report Unsafe Situation" to UnsafeSituationActivity::class.java,
            "42. Create Incident Report" to IncidentReportActivity::class.java,
            "43. Incident History Log" to IncidentHistoryActivity::class.java,
            "44. Personal Safety Tips" to SafetyTipsActivity::class.java,
            "45. Emergency Hotlines" to EmergencyNumbersActivity::class.java,
            "46. Disaster Readiness Guide" to DisasterGuideActivity::class.java,
            "47. First Aid Guide" to FirstAidActivity::class.java,
            "48. Emergency Kit Preparation" to EmergencyPreparationActivity::class.java,
            "49. Battery Safety & Health" to BatterySafetyActivity::class.java,
            "50. 5% Battery Alert Settings" to BatteryAlertSettingsActivity::class.java,
            "51. User Profile" to ProfileActivity::class.java,
            "52. Medical Emergency Card" to EmergencyCardActivity::class.java,
            "53. SOS Settings & Timer" to SOSSettingsActivity::class.java,
            "54. Notification Settings" to NotificationSettingsActivity::class.java,
            "55. Language Selector (EN/TA/HI)" to LanguageSettingsActivity::class.java,
            "56. Privacy & Data Controls" to PrivacySettingsActivity::class.java,
            "57. Permissions Dashboard" to PermissionsActivity::class.java,
            "58. Data & Storage Management" to DataManagementActivity::class.java,
            "59. Appearance & Theme" to AppearanceSettingsActivity::class.java,
            "60. App Security Lock" to AppLockActivity::class.java,
            "61. Fake Call Simulator" to FakeCallActivity::class.java,
            "62. Home Screen Panic Widget" to PanicWidgetActivity::class.java
        )

        for ((title, targetClass) in screens) {
            val card = CardView(this).apply {
                val cardParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(48)
                )
                cardParams.bottomMargin = dpToPx(8)
                layoutParams = cardParams
                setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this@HelpAndSupportActivity, R.color.card_bg))
                radius = dpToPx(10).toFloat()
                setOnClickListener {
                    try {
                        startActivity(Intent(this@HelpAndSupportActivity, targetClass))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val tv = TextView(this).apply {
                val tvParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                tvParams.gravity = Gravity.CENTER_VERTICAL
                tvParams.leftMargin = dpToPx(16)
                layoutParams = tvParams
                text = title
                setTextColor(androidx.core.content.ContextCompat.getColor(this@HelpAndSupportActivity, R.color.text_primary))
                textSize = 14f
            }
            card.addView(tv)
            container.addView(card)
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
