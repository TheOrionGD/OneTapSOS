package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SafetyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safety, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Diagnostics shortcut
        view.findViewById<View>(R.id.btnSafetyDiagnostics)?.setOnClickListener {
            startActivity(Intent(activity, SOSDiagnosticsActivity::class.java))
        }

        // 2. Journey Mode
        view.findViewById<View>(R.id.cardJourneyMode)?.setOnClickListener {
            startActivity(Intent(activity, JourneyModeActivity::class.java))
        }

        // 3. Safety Timer
        view.findViewById<View>(R.id.cardSafetyTimer)?.setOnClickListener {
            startActivity(Intent(activity, SafetyTimerActivity::class.java))
        }

        // 4. Scheduled Check-In
        view.findViewById<View>(R.id.cardScheduledCheckIn)?.setOnClickListener {
            startActivity(Intent(activity, CheckInActivity::class.java))
        }

        // 5. Unsafe Situation
        view.findViewById<View>(R.id.cardUnsafeSituation)?.setOnClickListener {
            startActivity(Intent(activity, UnsafeSituationActivity::class.java))
        }

        // 6. Incident Reports & Log
        view.findViewById<View>(R.id.cardIncidentReportHistory)?.setOnClickListener {
            startActivity(Intent(activity, IncidentHistoryActivity::class.java))
        }

        // 7. Emergency Kit Checklist
        view.findViewById<View>(R.id.cardPrepKit)?.setOnClickListener {
            startActivity(Intent(activity, EmergencyPreparationActivity::class.java))
        }

        // 8. Battery Safety Hub & 5% Alert
        view.findViewById<View>(R.id.cardPrepBattery)?.setOnClickListener {
            startActivity(Intent(activity, BatterySafetyActivity::class.java))
        }
    }
}
