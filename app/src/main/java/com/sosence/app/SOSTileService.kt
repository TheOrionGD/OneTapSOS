package com.sosence.app

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class SOSTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val sosIntent = Intent("com.sosence.app.SEND_SOS").apply {
            setPackage(packageName)
        }
        sendBroadcast(sosIntent)

        Toast.makeText(this, "🚨 SOS Triggered via Quick Settings!", Toast.LENGTH_LONG).show()
    }

    private fun updateTile() {
        val tile: Tile = qsTile ?: return
        tile.label = "Send SOS"
        tile.icon = Icon.createWithResource(this, android.R.drawable.ic_dialog_alert)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
