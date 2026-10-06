package com.sosence.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class EmergencyNumber(val country: String, val police: String, val ambulance: String, val fire: String, val flag: String)

class EmergencyNumbersActivity : BaseActivity() {

    private val numbers = listOf(
        EmergencyNumber("India", "100", "108", "101", "🇮🇳"),
        EmergencyNumber("Universal Emergency", "112", "112", "112", "🌍"),
        EmergencyNumber("United States", "911", "911", "911", "🇺🇸"),
        EmergencyNumber("United Kingdom", "999", "999", "999", "🇬🇧"),
        EmergencyNumber("Australia", "000", "000", "000", "🇦🇺"),
        EmergencyNumber("Canada", "911", "911", "911", "🇨🇦"),
        EmergencyNumber("Germany", "110", "112", "112", "🇩🇪"),
        EmergencyNumber("France", "17", "15", "18", "🇫🇷"),
        EmergencyNumber("Japan", "110", "119", "119", "🇯🇵"),
        EmergencyNumber("China", "110", "120", "119", "🇨🇳"),
        EmergencyNumber("Brazil", "190", "192", "193", "🇧🇷"),
        EmergencyNumber("South Africa", "10111", "10177", "10177", "🇿🇦"),
        EmergencyNumber("UAE", "999", "998", "997", "🇦🇪"),
        EmergencyNumber("Singapore", "999", "995", "995", "🇸🇬"),
        EmergencyNumber("New Zealand", "111", "111", "111", "🇳🇿"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_numbers)
        animateEntrance()

        val btnBack = findViewById<ImageView>(R.id.ivEmergNumBack)
        val rvNumbers = findViewById<RecyclerView>(R.id.rvEmergencyNumbers)

        btnBack.setOnClickListener { finish() }
        rvNumbers.layoutManager = LinearLayoutManager(this)
        rvNumbers.adapter = EmergencyNumbersAdapter(numbers) { number ->
            try {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Unable to open dialer", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateEntrance() {
        window.decorView.alpha = 0f
        window.decorView.animate().alpha(1f).setDuration(280)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }
}

class EmergencyNumbersAdapter(
    private val items: List<EmergencyNumber>,
    private val onCall: (String) -> Unit
) : RecyclerView.Adapter<EmergencyNumbersAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvFlag: TextView = view.findViewById(R.id.tvEmergFlag)
        val tvCountry: TextView = view.findViewById(R.id.tvEmergCountry)
        val tvPolice: TextView = view.findViewById(R.id.tvEmergPolice)
        val tvAmbulance: TextView = view.findViewById(R.id.tvEmergAmbulance)
        val tvFire: TextView = view.findViewById(R.id.tvEmergFire)
        val btnCallPolice: CardView = view.findViewById(R.id.btnCallPolice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_emergency_number, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvFlag.text = item.flag
        holder.tvCountry.text = item.country
        holder.tvPolice.text = "Police: ${item.police}"
        holder.tvAmbulance.text = "Ambulance: ${item.ambulance}"
        holder.tvFire.text = "Fire: ${item.fire}"
        holder.btnCallPolice.setOnClickListener { onCall(item.police) }
    }

    override fun getItemCount() = items.size
}
