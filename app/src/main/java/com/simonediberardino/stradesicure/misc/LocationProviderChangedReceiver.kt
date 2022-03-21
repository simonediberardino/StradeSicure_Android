package com.simonediberardino.stradesicure.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.simonediberardino.stradesicure.activities.MapsActivity


class LocationProviderChangedReceiver : BroadcastReceiver() {
    var isGpsEnabled = false

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action!! == "android.location.PROVIDERS_CHANGED") {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            MapsActivity.mapsActivity.onGpsStatusChanged(isGpsEnabled)
        }
    }
}