package com.onetapsos.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.onetapsos.app.data.AppDatabaseHelper

class HomeFragment : Fragment() {

    private lateinit var appSettings: AppSettings
    private lateinit var dbHelper: AppDatabaseHelper
    private var vibrator: Vibrator? = null

    private var holdTimer: CountDownTimer? = null
    private val holdDurationMs = 3000L

    private var glowOuterAnim: ObjectAnimator? = null
    private var glowInnerAnim: ObjectAnimator? = null

    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var cardActiveBanner: CardView
    private lateinit var tvActiveRecipients: TextView
    private lateinit var tvActiveCoords: TextView
    private lateinit var btnBannerImSafe: CardView
    private lateinit var btnBannerLiveTrack: CardView
    private lateinit var frameHeroSOS: FrameLayout
    private lateinit var tvSosSubLabel: TextView
    private lateinit var tvHudContacts: TextView
    private lateinit var tvHudLocation: TextView
    private lateinit var tvHudBattery: TextView
    private lateinit var tvHudFall: TextView
    private lateinit var tvRecentSummary: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        appSettings = AppSettings(ctx)
        dbHelper = AppDatabaseHelper(ctx)
        initVibrator(ctx)

        bindViews(view)
        setupPulsingGlow(view)
        setupSosButtonTouch()
        setupHudClickListeners()
        setupQuickActionListeners()

        view.findViewById<View>(R.id.btnHomeAiChat)?.setOnClickListener {
            startActivity(Intent(activity, ChatActivity::class.java))
        }

        view.findViewById<View>(R.id.btnHomeNotifs)?.setOnClickListener {
            startActivity(Intent(activity, NotificationSettingsActivity::class.java))
        }

        view.findViewById<View>(R.id.cardRecentSosActivity)?.setOnClickListener {
            startActivity(Intent(activity, SOSHistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardState()
    }

    private fun bindViews(root: View) {
        tvGreeting = root.findViewById(R.id.tvHomeGreeting)
        tvStatusBadge = root.findViewById(R.id.tvHomeStatusBadge)
        cardActiveBanner = root.findViewById(R.id.cardEmergencyActiveBanner)
        tvActiveRecipients = root.findViewById(R.id.tvActiveRecipientsCount)
        tvActiveCoords = root.findViewById(R.id.tvActiveCoordinates)
        btnBannerImSafe = root.findViewById(R.id.btnBannerImSafe)
        btnBannerLiveTrack = root.findViewById(R.id.btnBannerLiveTrack)
        frameHeroSOS = root.findViewById(R.id.frameHeroSOS)
        tvSosSubLabel = root.findViewById(R.id.tvSosSubLabel)
        tvHudContacts = root.findViewById(R.id.tvHudContactsCount)
        tvHudLocation = root.findViewById(R.id.tvHudLocationStatus)
        tvHudBattery = root.findViewById(R.id.tvHudBatteryStatus)
        tvHudFall = root.findViewById(R.id.tvHudFallStatus)
        tvRecentSummary = root.findViewById(R.id.tvRecentSosSummary)

        btnBannerImSafe.setOnClickListener {
            (activity as? MainActivity)?.triggerImSafeResolution()
        }

        btnBannerLiveTrack.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val trackingUrl = com.onetapsos.app.utils.LiveLocationPublisher.buildLiveTrackingUrl(ctx, null)
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trackingUrl))
            try {
                startActivity(browserIntent)
            } catch (e: Exception) {
                (activity as? MainActivity)?.navigateToTab(R.id.nav_map)
            }
        }
    }

    private fun setupPulsingGlow(root: View) {
        val outer = root.findViewById<View>(R.id.viewHeroGlowOuter)
        val inner = root.findViewById<View>(R.id.viewHeroGlowInner)

        if (outer != null) {
            glowOuterAnim = ObjectAnimator.ofPropertyValuesHolder(
                outer,
                PropertyValuesHolder.ofFloat("scaleX", 0.92f, 1.22f),
                PropertyValuesHolder.ofFloat("scaleY", 0.92f, 1.22f),
                PropertyValuesHolder.ofFloat("alpha", 0.15f, 0.45f)
            ).apply {
                duration = 1300
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }

        if (inner != null) {
            glowInnerAnim = ObjectAnimator.ofPropertyValuesHolder(
                inner,
                PropertyValuesHolder.ofFloat("scaleX", 0.95f, 1.12f),
                PropertyValuesHolder.ofFloat("scaleY", 0.95f, 1.12f),
                PropertyValuesHolder.ofFloat("alpha", 0.25f, 0.55f)
            ).apply {
                duration = 900
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private var touchDownTimeMs = 0L

    private fun setupSosButtonTouch() {
        frameHeroSOS.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownTimeMs = System.currentTimeMillis()
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                    vibrateHaptic(80)
                    startHoldTimer()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)
                        .setInterpolator(OvershootInterpolator(1.4f))
                        .start()
                    val pressDuration = System.currentTimeMillis() - touchDownTimeMs
                    cancelHoldTimer()
                    if (pressDuration < 800) {
                        // Single tap activation: immediately launch SOS countdown workflow
                        vibrateHaptic(100)
                        (activity as? MainActivity)?.launchSosCountdownWorkflow()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150)
                        .setInterpolator(OvershootInterpolator(1.4f))
                        .start()
                    cancelHoldTimer()
                    true
                }
                else -> false
            }
        }
    }

    private fun startHoldTimer() {
        holdTimer?.cancel()
        holdTimer = object : CountDownTimer(holdDurationMs, 200) {
            override fun onTick(millisUntilFinished: Long) {
                vibrateHaptic(35)
            }
            override fun onFinish() {
                vibrateHaptic(300)
                (activity as? MainActivity)?.launchSosCountdownWorkflow()
            }
        }.start()
    }

    private fun cancelHoldTimer() {
        holdTimer?.cancel()
    }

    private fun setupHudClickListeners() {
        view?.findViewById<View>(R.id.cardHudContacts)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_contacts)
        }

        view?.findViewById<View>(R.id.cardHudLocation)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_map)
        }

        view?.findViewById<View>(R.id.cardHudBattery)?.setOnClickListener {
            startActivity(Intent(activity, BatterySafetyActivity::class.java))
        }

        view?.findViewById<View>(R.id.cardHudFall)?.setOnClickListener {
            (activity as? MainActivity)?.showFallDetectionDialog()
        }
    }

    private fun setupQuickActionListeners() {
        view?.findViewById<View>(R.id.cardActionBroadcast)?.setOnClickListener {
            startActivity(Intent(activity, EmergencyBroadcastActivity::class.java))
        }

        view?.findViewById<View>(R.id.cardActionFakeCall)?.setOnClickListener {
            startActivity(Intent(activity, FakeCallActivity::class.java))
        }

        view?.findViewById<View>(R.id.cardActionLiveTrack)?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val trackingUrl = com.onetapsos.app.utils.LiveLocationPublisher.buildLiveTrackingUrl(ctx, null)
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trackingUrl))
            try {
                startActivity(browserIntent)
            } catch (e: Exception) {
                (activity as? MainActivity)?.navigateToTab(R.id.nav_map)
            }
        }
    }

    fun refreshDashboardState() {
        val ctx = context ?: return

        // 1. HUD: Trusted Contacts
        val contacts = dbHelper.getAllContacts()
        val activeContacts = contacts.filter { it.isEnabled }
        tvHudContacts.text = "${activeContacts.size} Active"
        tvHudContacts.setTextColor(ContextCompat.getColor(ctx, if (activeContacts.isNotEmpty()) R.color.text_primary else R.color.warning_yellow))

        // 2. HUD: Live GPS Status
        val locManager = ctx.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val hasLocPerm = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val isGpsEnabled = locManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

        if (hasLocPerm && isGpsEnabled) {
            tvHudLocation.text = "Fix Active"
            tvHudLocation.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
        } else if (!hasLocPerm) {
            tvHudLocation.text = "Perm Needed"
            tvHudLocation.setTextColor(ContextCompat.getColor(ctx, R.color.warning_yellow))
        } else {
            tvHudLocation.text = "GPS Off"
            tvHudLocation.setTextColor(ContextCompat.getColor(ctx, R.color.danger_red))
        }

        // 3. HUD: 5% Safe SOS Battery Guard
        val isBatteryProtected = appSettings.isBatteryAlertEnabled && appSettings.isBackgroundSafetyEnabled
        if (isBatteryProtected) {
            tvHudBattery.text = "Protected (${appSettings.lowBatteryThreshold}%)"
            tvHudBattery.setTextColor(ContextCompat.getColor(ctx, R.color.accent_teal))
        } else {
            tvHudBattery.text = "Disabled"
            tvHudBattery.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }

        // 4. HUD: Fall Sense Status
        val isFallRunning = FallDetectionService.isServiceRunning || appSettings.isFallDetectionEnabled
        val isSensorAvailable = FallDetectionService.isAccelerometerAvailable
        if (isFallRunning) {
            tvHudFall.text = "Monitoring"
            tvHudFall.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
        } else if (!isSensorAvailable) {
            tvHudFall.text = "Sensor N/A"
            tvHudFall.setTextColor(ContextCompat.getColor(ctx, R.color.warning_yellow))
        } else {
            tvHudFall.text = "Disabled"
            tvHudFall.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }

        // 5. Hero & Emergency Banner State
        val isSosActive = appSettings.isSosActive
        if (isSosActive) {
            tvStatusBadge.text = "🚨 EMERGENCY SOS ACTIVE"
            tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.danger_red))
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill)
            cardActiveBanner.visibility = View.VISIBLE
            tvSosSubLabel.text = "EMERGENCY ACTIVE"
            val recipients = appSettings.sosRecipients
            tvActiveRecipients.text = "${recipients.size} Notified"
        } else {
            tvStatusBadge.text = "🟢 System Armed & Protected"
            tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_granted)
            cardActiveBanner.visibility = View.GONE
            tvSosSubLabel.text = "PRESS FOR HELP"
        }

        // 6. Emergency Log & History Card (Authoritative Room/SQLite Database)
        val history = dbHelper.getAllSosEvents()
        if (history.isNotEmpty()) {
            val last = history.first()
            val df = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
            val countLabel = if (history.size == 1) "1 recorded" else "${history.size} recorded"
            tvRecentSummary.text = "Last SOS: ${df.format(java.util.Date(last.timestamp))} • $countLabel"
        } else {
            tvRecentSummary.text = "No SOS alerts yet • All safe"
        }
    }

    private fun initVibrator(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {}
    }

    private fun vibrateHaptic(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        holdTimer?.cancel()
        glowOuterAnim?.cancel()
        glowInnerAnim?.cancel()
    }
}
