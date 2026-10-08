package com.onetapsos.app

import com.onetapsos.app.data.ContactModel
import com.onetapsos.app.data.SosEventModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SosLogicTest {

    @Test
    fun testContactModelCreation() {
        val contact = ContactModel(
            id = 1,
            name = "Emergency Contact",
            phone = "9876543210",
            priority = 1,
            isEnabled = true,
            isSosRecipient = true,
            customMessage = "Please help me immediately"
        )

        assertEquals("Emergency Contact", contact.name)
        assertEquals("9876543210", contact.phone)
        assertTrue(contact.isEnabled)
        assertTrue(contact.isSosRecipient)
        assertEquals("Please help me immediately", contact.customMessage)
    }

    @Test
    fun testSosEventModelCreation() {
        val now = System.currentTimeMillis()
        val event = SosEventModel(
            id = 10,
            timestamp = now,
            formattedTime = "06 Oct 2026, 22:50",
            latitude = 12.9716,
            longitude = 77.5946,
            locationName = "Bangalore, KA",
            message = "🚨 SOS ALERT",
            recipientsCount = 3,
            isResolved = false
        )

        assertEquals(10L, event.id)
        assertEquals(12.9716, event.latitude, 0.0001)
        assertEquals(77.5946, event.longitude, 0.0001)
        assertEquals(3, event.recipientsCount)
        assertFalse(event.isResolved)
    }

    @Test
    fun testPhoneNumberFormatting() {
        fun formatNumber(number: String): String {
            return if (number.startsWith("+")) number else "+91$number"
        }

        assertEquals("+919876543210", formatNumber("9876543210"))
        assertEquals("+11234567890", formatNumber("+11234567890"))
        assertEquals("+911234567890", formatNumber("+911234567890"))
    }

    @Test
    fun testSosMessageFormat() {
        fun buildSosMessage(lat: Double?, lng: Double?): String {
            val mapsLink = if (lat != null && lng != null) "https://maps.google.com/?q=$lat,$lng" else "Location unavailable"
            return buildString {
                appendLine("🚨 SOS ALERT")
                appendLine()
                appendLine("I need immediate assistance.")
                appendLine()
                appendLine("Location:")
                appendLine(mapsLink)
                appendLine()
                append("Please respond as soon as possible.")
            }
        }

        val msgWithLoc = buildSosMessage(13.0827, 80.2707)
        assertTrue(msgWithLoc.contains("🚨 SOS ALERT"))
        assertTrue(msgWithLoc.contains("https://maps.google.com/?q=13.0827,80.2707"))

        val msgNoLoc = buildSosMessage(null, null)
        assertTrue(msgNoLoc.contains("Location unavailable"))
    }
}
