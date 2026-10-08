package com.onetapsos.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onetapsos.app.utils.AnimationExtensions.applyTouchBounce

data class OnboardingPermItem(
    val id: String,
    val permissions: List<String>,
    val emoji: String,
    val title: String,
    val description: String,
    var isGranted: Boolean = false
)

class OnboardingPermissionsActivity : BaseActivity() {

    private lateinit var rvPermissions: RecyclerView
    private lateinit var pbPermissions: ProgressBar
    private lateinit var tvProgressBadge: TextView
    private lateinit var btnGrantAll: FrameLayout
    private lateinit var btnContinue: FrameLayout
    private lateinit var tvSkip: TextView
    private lateinit var adapter: OnboardingPermissionsAdapter

    private val permissionItems = mutableListOf<OnboardingPermItem>()

    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionStates()
    }

    private val singlePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionStates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_permissions)

        rvPermissions = findViewById(R.id.rvOnboardingPermissions)
        pbPermissions = findViewById(R.id.pbPermissions)
        tvProgressBadge = findViewById(R.id.tvPermissionsProgressBadge)
        btnGrantAll = findViewById(R.id.btnGrantAllPermissions)
        btnContinue = findViewById(R.id.btnContinueToApp)
        tvSkip = findViewById(R.id.tvSkipPermissions)

        btnGrantAll.applyTouchBounce()
        btnContinue.applyTouchBounce()

        setupPermissionsList()

        adapter = OnboardingPermissionsAdapter(permissionItems) { item ->
            val ungranted = item.permissions.filter { !isPermissionGranted(it) }
            if (ungranted.isNotEmpty()) {
                singlePermissionLauncher.launch(ungranted.toTypedArray())
            }
        }

        rvPermissions.layoutManager = LinearLayoutManager(this)
        rvPermissions.adapter = adapter

        btnGrantAll.setOnClickListener {
            requestAllUngranted()
        }

        btnContinue.setOnClickListener {
            finishAndOpenMain()
        }

        tvSkip.setOnClickListener {
            finishAndOpenMain()
        }

        refreshPermissionStates()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun setupPermissionsList() {
        permissionItems.clear()

        // 1. Location
        permissionItems.add(
            OnboardingPermItem(
                id = "location",
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                emoji = "📍",
                title = "Precise GPS Location",
                description = "Coordinates for live tracking and rapid emergency dispatch"
            )
        )

        // 2. SMS Broadcast
        permissionItems.add(
            OnboardingPermItem(
                id = "sms",
                permissions = listOf(Manifest.permission.SEND_SMS),
                emoji = "📱",
                title = "Emergency SMS Broadcast",
                description = "Instant offline SMS alerts sent to your trusted contacts"
            )
        )

        // 3. Contacts
        permissionItems.add(
            OnboardingPermItem(
                id = "contacts",
                permissions = listOf(Manifest.permission.READ_CONTACTS),
                emoji = "👥",
                title = "Trusted Contacts",
                description = "Import and assign emergency circle members with custom alerts"
            )
        )

        // 4. Notifications (Android 13+)
        val notifPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
        if (notifPerms.isNotEmpty()) {
            permissionItems.add(
                OnboardingPermItem(
                    id = "notifications",
                    permissions = notifPerms,
                    emoji = "🔔",
                    title = "Safety Notifications",
                    description = "Real-time check-in prompts, fall warnings, and battery alerts"
                )
            )
        }

        // 5. Activity Recognition (Fall Detection)
        val activityPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            emptyList()
        }
        if (activityPerms.isNotEmpty()) {
            permissionItems.add(
                OnboardingPermItem(
                    id = "activity",
                    permissions = activityPerms,
                    emoji = "🏃",
                    title = "Motion & Fall Detection",
                    description = "Detects severe free-fall impacts and activates protection"
                )
            )
        }

        // 6. Microphone (AI Safety Voice)
        permissionItems.add(
            OnboardingPermItem(
                id = "audio",
                permissions = listOf(Manifest.permission.RECORD_AUDIO),
                emoji = "🎤",
                title = "Safety Voice & Audio",
                description = "Hands-free voice SOS trigger and AI safety assistance"
            )
        )

        // 7. Camera (Documentation)
        permissionItems.add(
            OnboardingPermItem(
                id = "camera",
                permissions = listOf(Manifest.permission.CAMERA),
                emoji = "📷",
                title = "Emergency Camera",
                description = "Incident photo evidence and situation documentation"
            )
        )
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun refreshPermissionStates() {
        var grantedCount = 0
        for (item in permissionItems) {
            item.isGranted = item.permissions.isEmpty() || item.permissions.all { isPermissionGranted(it) }
            if (item.isGranted) grantedCount++
        }

        val total = permissionItems.size
        pbPermissions.max = total
        pbPermissions.progress = grantedCount

        tvProgressBadge.text = "$grantedCount of $total Enabled"
        if (grantedCount == total) {
            tvProgressBadge.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_granted)
            findViewById<TextView>(R.id.tvGrantAllLabel)?.text = "✅ All Permissions Granted"
        } else {
            tvProgressBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_coral))
            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_pill)
            findViewById<TextView>(R.id.tvGrantAllLabel)?.text = "⚡ Grant All Permissions (${total - grantedCount} remaining)"
        }

        adapter.notifyDataSetChanged()
    }

    private fun requestAllUngranted() {
        val needed = mutableListOf<String>()
        for (item in permissionItems) {
            for (p in item.permissions) {
                if (!isPermissionGranted(p)) {
                    needed.add(p)
                }
            }
        }

        if (needed.isNotEmpty()) {
            multiplePermissionsLauncher.launch(needed.distinct().toTypedArray())
        } else {
            finishAndOpenMain()
        }
    }

    private fun finishAndOpenMain() {
        try {
            getSharedPreferences("sosense_prefs", MODE_PRIVATE).edit()
                .putBoolean("onboarding_done", true)
                .apply()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class OnboardingPermissionsAdapter(
    private val items: List<OnboardingPermItem>,
    private val onGrantClicked: (OnboardingPermItem) -> Unit
) : RecyclerView.Adapter<OnboardingPermissionsAdapter.PermViewHolder>() {

    class PermViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvPermItemEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvPermItemTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvPermItemDesc)
        val btnAction: FrameLayout = view.findViewById(R.id.btnPermAction)
        val tvActionLabel: TextView = view.findViewById(R.id.tvPermActionLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_permission, parent, false)
        return PermViewHolder(view)
    }

    override fun onBindViewHolder(holder: PermViewHolder, position: Int) {
        val item = items[position]
        holder.tvEmoji.text = item.emoji
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.description

        if (item.isGranted) {
            holder.tvActionLabel.text = "Granted ✅"
            holder.tvActionLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.success_green))
            holder.btnAction.setBackgroundResource(R.drawable.bg_badge_granted)
            holder.btnAction.setOnClickListener(null)
        } else {
            holder.tvActionLabel.text = "Grant Access"
            holder.tvActionLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.accent_teal))
            holder.btnAction.setBackgroundResource(R.drawable.bg_badge_enable)
            holder.btnAction.setOnClickListener {
                onGrantClicked(item)
            }
        }
    }

    override fun getItemCount() = items.size
}
