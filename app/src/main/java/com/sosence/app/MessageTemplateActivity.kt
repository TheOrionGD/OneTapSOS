package com.sosence.app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.sosence.app.data.AppDatabaseHelper

class MessageTemplateActivity : BaseActivity() {

    private lateinit var dbHelper: AppDatabaseHelper
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_template)

        dbHelper = AppDatabaseHelper(this)
        container = findViewById(R.id.llTemplatesList)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val etTitle = findViewById<EditText>(R.id.etTemplateTitle)
        val etContent = findViewById<EditText>(R.id.etTemplateContent)

        findViewById<CardView>(R.id.btnAddTemplate).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                dbHelper.addTemplate(title, content)
                etTitle.text.clear()
                etContent.text.clear()
                Toast.makeText(this, "Template added successfully", Toast.LENGTH_SHORT).show()
                loadTemplates()
            } else {
                Toast.makeText(this, "Please enter title and content", Toast.LENGTH_SHORT).show()
            }
        }

        loadTemplates()
    }

    private fun loadTemplates() {
        container.removeAllViews()
        val templates = dbHelper.getAllTemplates()

        for (t in templates) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_template_row, container, false)
            val tvTitle = view.findViewById<TextView>(R.id.tvTemplateTitle)
            val tvContent = view.findViewById<TextView>(R.id.tvTemplateContent)
            val tvCategory = view.findViewById<TextView>(R.id.tvTemplateCategory)

            tvTitle.text = t.title
            tvContent.text = t.content
            tvCategory.text = t.category

            container.addView(view)
        }
    }
}
