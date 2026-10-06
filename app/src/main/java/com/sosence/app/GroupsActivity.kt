package com.sosence.app

import android.os.Bundle
import android.widget.TextView

class GroupsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
