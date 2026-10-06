package com.sosence.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Medical Card
        view.findViewById<View>(R.id.cardEmergencyMedicalCard)?.setOnClickListener {
            startActivity(Intent(activity, EmergencyCardActivity::class.java))
        }

        // 2. Profile
        view.findViewById<View>(R.id.cardUserProfile)?.setOnClickListener {
            startActivity(Intent(activity, ProfileActivity::class.java))
        }

        // 3. Hotlines
        view.findViewById<View>(R.id.cardMoreHotlines)?.setOnClickListener {
            startActivity(Intent(activity, EmergencyNumbersActivity::class.java))
        }

        // 4. First Aid
        view.findViewById<View>(R.id.cardMoreFirstAid)?.setOnClickListener {
            startActivity(Intent(activity, FirstAidActivity::class.java))
        }

        // 5. Disaster Survival
        view.findViewById<View>(R.id.cardMoreDisaster)?.setOnClickListener {
            startActivity(Intent(activity, DisasterGuideActivity::class.java))
        }

        // 6. Safety Tips
        view.findViewById<View>(R.id.cardMoreSafetyTips)?.setOnClickListener {
            startActivity(Intent(activity, SafetyTipsActivity::class.java))
        }

        // 7. SOS Settings
        view.findViewById<View>(R.id.cardMoreSosSettings)?.setOnClickListener {
            startActivity(Intent(activity, SOSSettingsActivity::class.java))
        }

        // 8. Notification Preferences & Test Lab
        view.findViewById<View>(R.id.cardMoreNotifSettings)?.setOnClickListener {
            startActivity(Intent(activity, NotificationSettingsActivity::class.java))
        }

        // 9. Language Selector
        view.findViewById<View>(R.id.cardMoreLanguage)?.setOnClickListener {
            startActivity(Intent(activity, LanguageSettingsActivity::class.java))
        }

        // 10. Privacy & App Lock
        view.findViewById<View>(R.id.cardMorePrivacy)?.setOnClickListener {
            startActivity(Intent(activity, AppLockActivity::class.java))
        }

        // 11. Permissions Dashboard
        view.findViewById<View>(R.id.cardMorePermissions)?.setOnClickListener {
            startActivity(Intent(activity, PermissionsActivity::class.java))
        }

        // 12. Help Center
        view.findViewById<View>(R.id.cardMoreHelp)?.setOnClickListener {
            startActivity(Intent(activity, HelpAndSupportActivity::class.java))
        }
    }
}
