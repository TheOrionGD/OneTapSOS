package com.sosence.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ContactModel(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val priority: Int = 1,
    val isEnabled: Boolean = true,
    val isSosRecipient: Boolean = true,
    val customMessage: String = ""
)

data class SosEventModel(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val message: String = "",
    val recipientsCount: Int = 0,
    val isResolved: Boolean = false,
    val resolutionTime: Long = 0L,
    val durationSeconds: Long = 0L
)

data class MessageTemplateModel(
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Emergency",
    val isDefault: Boolean = false
)

data class IncidentReportModel(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val description: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val severity: String = "Medium",
    val status: String = "Reported"
)

data class SafetyCheckInModel(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Completed",
    val note: String = "",
    val scheduledTime: Long = System.currentTimeMillis()
)

data class RecordedLocationModel(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)

data class ConversationMessageModel(
    val id: Long = 0,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Sent",
    val isSosRelated: Boolean = false
)

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sosence_full.db"
        private const val DATABASE_VERSION = 3

        // Tables
        private const val TABLE_CONTACTS = "contacts"
        private const val TABLE_SOS_EVENTS = "sos_events"
        private const val TABLE_TEMPLATES = "message_templates"
        private const val TABLE_INCIDENTS = "incident_reports"
        private const val TABLE_CHECKINS = "check_ins"
        private const val TABLE_LOCATIONS = "location_logs"
        private const val TABLE_MESSAGES = "conversation_messages"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CONTACTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                phone TEXT UNIQUE,
                priority INTEGER DEFAULT 1,
                is_enabled INTEGER DEFAULT 1,
                is_sos_recipient INTEGER DEFAULT 1,
                custom_message TEXT DEFAULT ''
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_SOS_EVENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER,
                formatted_time TEXT,
                latitude REAL,
                longitude REAL,
                location_name TEXT,
                message TEXT,
                recipients_count INTEGER,
                is_resolved INTEGER DEFAULT 0,
                resolution_time INTEGER DEFAULT 0,
                duration_seconds INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_TEMPLATES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                content TEXT,
                category TEXT,
                is_default INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_INCIDENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER,
                category TEXT,
                description TEXT,
                latitude REAL,
                longitude REAL,
                severity TEXT,
                status TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_CHECKINS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER,
                status TEXT,
                note TEXT,
                scheduled_time INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_LOCATIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER,
                latitude REAL,
                longitude REAL,
                address TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversation_id TEXT,
                sender_id TEXT,
                receiver_id TEXT,
                message TEXT,
                timestamp INTEGER,
                status TEXT,
                is_sos_related INTEGER DEFAULT 0
            )
        """.trimIndent())

        // Seed initial templates
        seedInitialTemplates(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_CONTACTS ADD COLUMN custom_message TEXT DEFAULT ''")
            } catch (e: Exception) {
                // Column may already exist
            }
        }
    }

    private fun seedInitialTemplates(db: SQLiteDatabase) {
        val templates = listOf(
            MessageTemplateModel(title = "Emergency SOS", content = "🚨 EMERGENCY SOS! I need immediate help. My location:", category = "Emergency", isDefault = true),
            MessageTemplateModel(title = "I'm Safe", content = "✅ I'm Safe now. The emergency has been resolved.", category = "Status", isDefault = true),
            MessageTemplateModel(title = "Check In", content = "📍 Safety Check-In: I have arrived safely at my destination.", category = "Check-in", isDefault = false),
            MessageTemplateModel(title = "Medical Alert", content = "🏥 Medical Emergency! I need urgent medical assistance.", category = "Medical", isDefault = false)
        )
        for (t in templates) {
            val cv = ContentValues().apply {
                put("title", t.title)
                put("content", t.content)
                put("category", t.category)
                put("is_default", if (t.isDefault) 1 else 0)
            }
            db.insert(TABLE_TEMPLATES, null, cv)
        }
    }

    // --- CONTACTS CRUD ---
    fun getAllContacts(): List<ContactModel> {
        val list = mutableListOf<ContactModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_CONTACTS, null, null, null, null, null, "priority ASC, name ASC")
            with(cursor) {
                while (moveToNext()) {
                    val customMsgIdx = getColumnIndex("custom_message")
                    val customMsg = if (customMsgIdx >= 0) getString(customMsgIdx) ?: "" else ""
                    list.add(ContactModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        name = getString(getColumnIndexOrThrow("name")),
                        phone = getString(getColumnIndexOrThrow("phone")),
                        priority = getInt(getColumnIndexOrThrow("priority")),
                        isEnabled = getInt(getColumnIndexOrThrow("is_enabled")) == 1,
                        isSosRecipient = getInt(getColumnIndexOrThrow("is_sos_recipient")) == 1,
                        customMessage = customMsg
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addContact(name: String, phone: String, priority: Int = 1, customMessage: String = ""): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("name", name)
                put("phone", phone)
                put("priority", priority)
                put("is_enabled", 1)
                put("is_sos_recipient", 1)
                put("custom_message", customMessage)
            }
            db.insertWithOnConflict(TABLE_CONTACTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            -1L
        }
    }

    fun updateContact(contact: ContactModel): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("name", contact.name)
                put("phone", contact.phone)
                put("priority", contact.priority)
                put("is_enabled", if (contact.isEnabled) 1 else 0)
                put("is_sos_recipient", if (contact.isSosRecipient) 1 else 0)
                put("custom_message", contact.customMessage)
            }
            db.update(TABLE_CONTACTS, cv, "id = ?", arrayOf(contact.id.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun deleteContact(id: Long): Boolean {
        return try {
            val db = writableDatabase
            db.delete(TABLE_CONTACTS, "id = ?", arrayOf(id.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }


    // --- SOS EVENTS CRUD ---
    fun recordSosEvent(event: SosEventModel): Long {
        return try {
            val db = writableDatabase
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val cv = ContentValues().apply {
                put("timestamp", event.timestamp)
                put("formatted_time", if (event.formattedTime.isEmpty()) sdf.format(Date(event.timestamp)) else event.formattedTime)
                put("latitude", event.latitude)
                put("longitude", event.longitude)
                put("location_name", event.locationName)
                put("message", event.message)
                put("recipients_count", event.recipientsCount)
                put("is_resolved", if (event.isResolved) 1 else 0)
                put("resolution_time", event.resolutionTime)
                put("duration_seconds", event.durationSeconds)
            }
            db.insert(TABLE_SOS_EVENTS, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    fun markSosResolved(id: Long, resolutionTime: Long = System.currentTimeMillis()): Boolean {
        return try {
            val db = writableDatabase
            val event = getSosEventById(id)
            val duration = if (event != null) (resolutionTime - event.timestamp) / 1000 else 0L
            val cv = ContentValues().apply {
                put("is_resolved", 1)
                put("resolution_time", resolutionTime)
                put("duration_seconds", duration)
            }
            db.update(TABLE_SOS_EVENTS, cv, "id = ?", arrayOf(id.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun getSosEventById(id: Long): SosEventModel? {
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_SOS_EVENTS, null, "id = ?", arrayOf(id.toString()), null, null, null)
            with(cursor) {
                if (moveToFirst()) {
                    val model = SosEventModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        formattedTime = getString(getColumnIndexOrThrow("formatted_time")),
                        latitude = getDouble(getColumnIndexOrThrow("latitude")),
                        longitude = getDouble(getColumnIndexOrThrow("longitude")),
                        locationName = getString(getColumnIndexOrThrow("location_name")),
                        message = getString(getColumnIndexOrThrow("message")),
                        recipientsCount = getInt(getColumnIndexOrThrow("recipients_count")),
                        isResolved = getInt(getColumnIndexOrThrow("is_resolved")) == 1,
                        resolutionTime = getLong(getColumnIndexOrThrow("resolution_time")),
                        durationSeconds = getLong(getColumnIndexOrThrow("duration_seconds"))
                    )
                    close()
                    return model
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getAllSosEvents(): List<SosEventModel> {
        val list = mutableListOf<SosEventModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_SOS_EVENTS, null, null, null, null, null, "timestamp DESC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(SosEventModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        formattedTime = getString(getColumnIndexOrThrow("formatted_time")),
                        latitude = getDouble(getColumnIndexOrThrow("latitude")),
                        longitude = getDouble(getColumnIndexOrThrow("longitude")),
                        locationName = getString(getColumnIndexOrThrow("location_name")),
                        message = getString(getColumnIndexOrThrow("message")),
                        recipientsCount = getInt(getColumnIndexOrThrow("recipients_count")),
                        isResolved = getInt(getColumnIndexOrThrow("is_resolved")) == 1,
                        resolutionTime = getLong(getColumnIndexOrThrow("resolution_time")),
                        durationSeconds = getLong(getColumnIndexOrThrow("duration_seconds"))
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // --- TEMPLATES ---
    fun getAllTemplates(): List<MessageTemplateModel> {
        val list = mutableListOf<MessageTemplateModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_TEMPLATES, null, null, null, null, null, "id ASC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(MessageTemplateModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        title = getString(getColumnIndexOrThrow("title")),
                        content = getString(getColumnIndexOrThrow("content")),
                        category = getString(getColumnIndexOrThrow("category")),
                        isDefault = getInt(getColumnIndexOrThrow("is_default")) == 1
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addTemplate(title: String, content: String, category: String = "Custom"): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("title", title)
                put("content", content)
                put("category", category)
                put("is_default", 0)
            }
            db.insert(TABLE_TEMPLATES, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    // --- INCIDENTS ---
    fun addIncidentReport(category: String, description: String, lat: Double, lng: Double, severity: String): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("timestamp", System.currentTimeMillis())
                put("category", category)
                put("description", description)
                put("latitude", lat)
                put("longitude", lng)
                put("severity", severity)
                put("status", "Reported")
            }
            db.insert(TABLE_INCIDENTS, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    fun getAllIncidents(): List<IncidentReportModel> {
        val list = mutableListOf<IncidentReportModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_INCIDENTS, null, null, null, null, null, "timestamp DESC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(IncidentReportModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        category = getString(getColumnIndexOrThrow("category")),
                        description = getString(getColumnIndexOrThrow("description")),
                        latitude = getDouble(getColumnIndexOrThrow("latitude")),
                        longitude = getDouble(getColumnIndexOrThrow("longitude")),
                        severity = getString(getColumnIndexOrThrow("severity")),
                        status = getString(getColumnIndexOrThrow("status"))
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // --- CHECKINS ---
    fun recordCheckIn(status: String, note: String): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("timestamp", System.currentTimeMillis())
                put("status", status)
                put("note", note)
                put("scheduled_time", System.currentTimeMillis())
            }
            db.insert(TABLE_CHECKINS, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    fun getAllCheckIns(): List<SafetyCheckInModel> {
        val list = mutableListOf<SafetyCheckInModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_CHECKINS, null, null, null, null, null, "timestamp DESC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(SafetyCheckInModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        status = getString(getColumnIndexOrThrow("status")),
                        note = getString(getColumnIndexOrThrow("note")),
                        scheduledTime = getLong(getColumnIndexOrThrow("scheduled_time"))
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // --- LOCATION LOGS ---
    fun recordLocation(lat: Double, lng: Double, address: String = ""): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("timestamp", System.currentTimeMillis())
                put("latitude", lat)
                put("longitude", lng)
                put("address", address)
            }
            db.insert(TABLE_LOCATIONS, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    fun getLocationHistory(): List<RecordedLocationModel> {
        val list = mutableListOf<RecordedLocationModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_LOCATIONS, null, null, null, null, null, "timestamp DESC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(RecordedLocationModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        latitude = getDouble(getColumnIndexOrThrow("latitude")),
                        longitude = getDouble(getColumnIndexOrThrow("longitude")),
                        address = getString(getColumnIndexOrThrow("address"))
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // --- CONVERSATIONS & MESSAGES ---
    fun addMessage(msg: ConversationMessageModel): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("conversation_id", msg.conversationId)
                put("sender_id", msg.senderId)
                put("receiver_id", msg.receiverId)
                put("message", msg.message)
                put("timestamp", msg.timestamp)
                put("status", msg.status)
                put("is_sos_related", if (msg.isSosRelated) 1 else 0)
            }
            db.insert(TABLE_MESSAGES, null, cv)
        } catch (e: Exception) {
            -1L
        }
    }

    fun getMessagesForConversation(conversationId: String): List<ConversationMessageModel> {
        val list = mutableListOf<ConversationMessageModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_MESSAGES, null, "conversation_id = ?", arrayOf(conversationId), null, null, "timestamp ASC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(ConversationMessageModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        conversationId = getString(getColumnIndexOrThrow("conversation_id")),
                        senderId = getString(getColumnIndexOrThrow("sender_id")),
                        receiverId = getString(getColumnIndexOrThrow("receiver_id")),
                        message = getString(getColumnIndexOrThrow("message")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        status = getString(getColumnIndexOrThrow("status")),
                        isSosRelated = getInt(getColumnIndexOrThrow("is_sos_related")) == 1
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getAllMessages(): List<ConversationMessageModel> {
        val list = mutableListOf<ConversationMessageModel>()
        try {
            val db = readableDatabase
            val cursor = db.query(TABLE_MESSAGES, null, null, null, null, null, "timestamp DESC")
            with(cursor) {
                while (moveToNext()) {
                    list.add(ConversationMessageModel(
                        id = getLong(getColumnIndexOrThrow("id")),
                        conversationId = getString(getColumnIndexOrThrow("conversation_id")),
                        senderId = getString(getColumnIndexOrThrow("sender_id")),
                        receiverId = getString(getColumnIndexOrThrow("receiver_id")),
                        message = getString(getColumnIndexOrThrow("message")),
                        timestamp = getLong(getColumnIndexOrThrow("timestamp")),
                        status = getString(getColumnIndexOrThrow("status")),
                        isSosRelated = getInt(getColumnIndexOrThrow("is_sos_related")) == 1
                    ))
                }
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // --- DATA MANAGEMENT CLEAR DATA ---
    fun clearSosHistory() {
        try { writableDatabase.delete(TABLE_SOS_EVENTS, null, null) } catch (e: Exception) {}
    }

    fun clearLocationHistory() {
        try { writableDatabase.delete(TABLE_LOCATIONS, null, null) } catch (e: Exception) {}
    }

    fun clearMessageHistory() {
        try { writableDatabase.delete(TABLE_MESSAGES, null, null) } catch (e: Exception) {}
    }

    fun clearIncidents() {
        try { writableDatabase.delete(TABLE_INCIDENTS, null, null) } catch (e: Exception) {}
    }
}
