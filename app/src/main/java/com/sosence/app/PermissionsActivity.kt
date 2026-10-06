package com.sosence.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class PermissionStatus(val name: String, val description: String, val isGranted: Boolean, val icon: String)

class PermissionsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivPermissionsBack)
        val rvPerms = findViewById<RecyclerView>(R.id.rvPermissions)

        btnBack.setOnClickListener { finish() }

        val perms = listOf(
            PermissionStatus("Location", "Required for SOS with precise coordinates", isGranted(Manifest.permission.ACCESS_FINE_LOCATION), "📍"),
            PermissionStatus("Send SMS", "Required to alert trusted contacts", isGranted(Manifest.permission.SEND_SMS), "📱"),
            PermissionStatus("Camera", "Used for safety documentation", isGranted(Manifest.permission.CAMERA), "📷"),
            PermissionStatus("Microphone", "Used for AI safety features", isGranted(Manifest.permission.RECORD_AUDIO), "🎤"),
            PermissionStatus("Contacts", "Read contacts for quick add", isGranted(Manifest.permission.READ_CONTACTS), "👥"),
            PermissionStatus("Activity Recognition", "Powers fall detection", isGranted(Manifest.permission.ACTIVITY_RECOGNITION), "🏃"),
            PermissionStatus("Notifications", "Alerts for check-ins and battery", isGranted(Manifest.permission.POST_NOTIFICATIONS), "🔔"),
            PermissionStatus("Vibration", "Feedback for SOS alerts", true, "📳"),
        )

        rvPerms.layoutManager = LinearLayoutManager(this)
        rvPerms.adapter = PermissionsAdapter(perms)
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class PermissionsAdapter(private val items: List<PermissionStatus>) :
    RecyclerView.Adapter<PermissionsAdapter.VH>() {

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
            holder.tvStatus.text = "❌ Denied"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.danger_red))
        }
    }

    override fun getItemCount() = items.size
}
