package com.onetapsos.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.onetapsos.app.data.AppDatabaseHelper

class ContactsFragment : Fragment() {

    private lateinit var dbHelper: AppDatabaseHelper
    private var tvCircleTitle: TextView? = null
    private var tvSummarySub: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = AppDatabaseHelper(requireContext())

        tvCircleTitle = view.findViewById(R.id.tvCircleCountTitle)
        tvSummarySub = view.findViewById(R.id.tvContactsSummarySub)

        // 1. + Add Contact in Header
        view.findViewById<View>(R.id.btnHeaderAddContact)?.setOnClickListener {
            startActivity(Intent(activity, AddTrustedContactActivity::class.java))
        }

        // 2. Manage Contacts
        view.findViewById<View>(R.id.cardManageCircle)?.setOnClickListener {
            startActivity(Intent(activity, TrustedContactsActivity::class.java))
        }

        // 3. Custom I'm Safe Editor ONLY
        view.findViewById<View>(R.id.cardImSafeEditor)?.setOnClickListener {
            startActivity(Intent(activity, ImSafeMessageActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshContactCounts()
    }

    private fun refreshContactCounts() {
        val contacts = dbHelper.getAllContacts()
        tvCircleTitle?.text = "${contacts.size} Trusted Contact(s) Configured"
        tvSummarySub?.text = if (contacts.isEmpty()) "No contacts added yet. Tap + to add." else "${contacts.size} contact(s) ready for instant distress alert"
    }
}
