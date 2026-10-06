package com.sosence.app

import android.os.Bundle
import android.widget.TextView

class EmergencyPreparationActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_preparation)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
