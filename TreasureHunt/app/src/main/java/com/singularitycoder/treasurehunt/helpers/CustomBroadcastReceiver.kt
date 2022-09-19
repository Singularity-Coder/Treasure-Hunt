package com.singularitycoder.treasurehunt.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager

class CustomBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PROVIDER_CHANGED -> {
                sendLocationToggleStatus(context)
            }
        }
    }

    // https://stackoverflow.com/questions/15778807/how-to-detect-when-user-turn-on-off-gps-state
    private fun sendLocationToggleStatus(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val locationToggleStatusIntent = Intent(BroadcastKey.LOCATION_TOGGLE_STATUS).apply {
            putExtra(IntentKey.LOCATION_TOGGLE_STATUS, isGpsEnabled || isNetworkEnabled)
        }
        context.sendBroadcast(locationToggleStatusIntent)
    }
}