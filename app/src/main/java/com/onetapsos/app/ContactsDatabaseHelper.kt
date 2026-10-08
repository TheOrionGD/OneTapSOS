package com.onetapsos.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.onetapsos.app.data.AppDatabaseHelper

data class Contact(val name: String, val phone: String, val id: Long = -1, val customMessage: String = "")

class ContactsDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appDb = AppDatabaseHelper(context)

    companion object {
        private const val DATABASE_NAME = "sosence_contacts.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_CONTACTS = "contacts"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_CUSTOM_MESSAGE = "custom_message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE " + TABLE_CONTACTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_PHONE + " TEXT, "
                + COLUMN_CUSTOM_MESSAGE + " TEXT DEFAULT ''" + ")")
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_CONTACTS ADD COLUMN $COLUMN_CUSTOM_MESSAGE TEXT DEFAULT ''")
            } catch (e: Exception) {}
        }
    }

    fun getAllContacts(): List<Contact> {
        val list = mutableListOf<Contact>()
        // Combine contacts from legacy table and AppDatabaseHelper
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CONTACTS,
                null,
                null, null, null, null,
                "$COLUMN_NAME ASC"
            )
            with(cursor) {
                while (moveToNext()) {
                    val id = getLong(getColumnIndexOrThrow(COLUMN_ID))
                    val name = getString(getColumnIndexOrThrow(COLUMN_NAME))
                    val phone = getString(getColumnIndexOrThrow(COLUMN_PHONE))
                    val customMsgIdx = getColumnIndex(COLUMN_CUSTOM_MESSAGE)
                    val customMsg = if (customMsgIdx >= 0) getString(customMsgIdx) ?: "" else ""
                    list.add(Contact(name = name, phone = phone, id = id, customMessage = customMsg))
                }
                close()
            }
        } catch (e: Exception) {}

        val appContacts = appDb.getAllContacts()
        for (ac in appContacts) {
            if (list.none { it.phone == ac.phone }) {
                list.add(Contact(name = ac.name, phone = ac.phone, id = ac.id, customMessage = ac.customMessage))
            }
        }
        return list
    }

    fun addContact(name: String, phone: String, customMessage: String = ""): Long {
        appDb.addContact(name, phone, 1, customMessage)
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_NAME, name)
                put(COLUMN_PHONE, phone)
                put(COLUMN_CUSTOM_MESSAGE, customMessage)
            }
            db.insert(TABLE_CONTACTS, null, values)
        } catch (e: Exception) {
            -1L
        }
    }

    fun deleteContact(id: Long): Int {
        if (id != -1L) {
            appDb.deleteContact(id)
        }
        return try {
            val db = writableDatabase
            if (id != -1L) db.delete(TABLE_CONTACTS, "$COLUMN_ID = ?", arrayOf(id.toString())) else 0
        } catch (e: Exception) {
            0
        }
    }

    fun deleteContactByNameAndPhone(name: String, phone: String): Int {
        val appContacts = appDb.getAllContacts()
        val match = appContacts.find { it.name == name && it.phone == phone }
        if (match != null) {
            appDb.deleteContact(match.id)
        }
        return try {
            val db = writableDatabase
            db.delete(TABLE_CONTACTS, "$COLUMN_NAME = ? AND $COLUMN_PHONE = ?", arrayOf(name, phone))
        } catch (e: Exception) {
            0
        }
    }
}
