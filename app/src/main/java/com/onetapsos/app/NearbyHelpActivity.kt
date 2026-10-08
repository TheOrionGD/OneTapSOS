package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.cardview.widget.CardView

class NearbyHelpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = intent.getStringExtra("FILTER_TYPE") ?: intent.getStringExtra("FILTER_CATEGORY")
        val safeMapIntent = Intent(this, SafeMapActivity::class.java).apply {
            putExtra("FILTER_CATEGORY", filter)
        }
        startActivity(safeMapIntent)
        finish()
    }
}
