package com.sosence.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sosence.app.engine.BackgroundPermissionHelper

data class PermissionItem(
    val id: String,
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val icon: String,
    val permissionString: String? = null
)

class PermissionsActivity : BaseActivity() {

    private lateinit var rvPerms: RecyclerView
    private lateinit var adapter: PermissionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        animateEntrance()

        findViewById<ImageView>(R.id.ivPermissionsBack).setOnClickListener { finish() }
        rvPerms = findViewById(R.id.rvPermissions)
        rvPerms.layoutManager = LinearLayoutManager(this)

        refreshPermissionList()
    }

    private fun refreshPermissionList() {
        val perms = listOf(
            PermissionItem(
                id = "notif",
                name = "Notifications",
                description = "Required for high-priority low battery and emergency alerts",
                isGranted = BackgroundPermissionHelper.isNotificationPermissionGranted(this),
                icon = "🔔",
                permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            ),
            PermissionItem(
                id = "exact_alarm",
                name = "Exact Alarms (Doze-exempt)",
                description = "Powers Safety Timers & Check-In background timers while asleep",
                isGranted = BackgroundPermissionHelper.isExactAlarmPermissionGranted(this),
                icon = "⏱️"
            ),
            PermissionItem(
                id = "battery_opt",
                name = "Background Battery Exemption",
                description = "Prevents OS from suspending safety monitors during sleep",
                isGranted = BackgroundPermissionHelper.isBatteryOptimizationExempt(this),
                icon = "⚡"
            ),
            PermissionItem(
                id = "loc",
                name = "Precise GPS Location",
                description = "Provides coordinates in emergency SOS alerts",
                isGranted = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
                icon = "📍",
                permissionString = Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PermissionItem(
                id = "sms",
                name = "Send Emergency SMS",
                description = "Dispatches automated SOS messages to trusted contacts",
                isGranted = isGranted(Manifest.permission.SEND_SMS),
                icon = "📱",
                permissionString = Manifest.permission.SEND_SMS
            ),
            PermissionItem(
                id = "contacts",
                name = "Read Contacts",
                description = "Allows selecting emergency contacts from address book",
                isGranted = isGranted(Manifest.permission.READ_CONTACTS),
                icon = "👥",
                permissionString = Manifest.permission.READ_CONTACTS
            ),
            PermissionItem(
                id = "mic",
                name = "Microphone",
                description = "Enables voice commands and safety audio analysis",
                isGranted = isGranted(Manifest.permission.RECORD_AUDIO),
                icon = "🎤",
                permissionString = Manifest.permission.RECORD_AUDIO
            ),
            PermissionItem(
                id = "activity",
                name = "Physical Activity",
                description = "Powers autonomous fall detection engine",
                isGranted = isGranted(Manifest.permission.ACTIVITY_RECOGNITION),
                icon = "🏃",
                permissionString = Manifest.permission.ACTIVITY_RECOGNITION
            )
        )

        adapter = PermissionsAdapter(perms) { item ->
            handlePermissionClick(item)
        }
        rvPerms.adapter = adapter
    }

    private fun handlePermissionClick(item: PermissionItem) {
        when (item.id) {
            "notif" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    BackgroundPermissionHelper.requestNotificationPermission(this)
                } else {
                    Toast.makeText(this, "Notification permission is granted by default on this OS version.", Toast.LENGTH_SHORT).show()
                }
            }
            "exact_alarm" -> {
                BackgroundPermissionHelper.requestExactAlarmPermission(this)
            }
            "battery_opt" -> {
                BackgroundPermissionHelper.requestBatteryOptimizationExemption(this)
            }
            else -> {
                item.permissionString?.let { perm ->
                    ActivityCompat.requestPermissions(this, arrayOf(perm), 101)
                }
            }
        }
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissionList()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionList()
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class PermissionsAdapter(
    private val items: List<PermissionItem>,
    private val onClick: (PermissionItem) -> Unit
) : RecyclerView.Adapter<PermissionsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvPermIcon)
        val tvName: TextView = view.findViewById(R.id.tvPermName)
        val tvDesc: TextView = view.findViewById(R.id.tvPermDesc)
        val tvStatus: TextView = view.findViewById(R.id.tvPermStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_permission, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvIcon.text = item.icon
        holder.tvName.text = item.name
        holder.tvDesc.text = item.description

        if (item.isGranted) {
            holder.tvStatus.text = "✅ Granted"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.success_green))
        } else {
            holder.tvStatus.text = "⚠️ Tap to Grant"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.warning_yellow))
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = items.size
}
