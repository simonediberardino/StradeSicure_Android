package com.simonediberardino.stradesicure.misc

import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

class GoogleMapExtended(val map: GoogleMap) {
    val markers: ArrayList<Marker> = ArrayList()

    fun addMarker(markerOptions: MarkerOptions): Marker {
        val marker = map.addMarker(markerOptions)
        markers.add(marker!!)
        return marker
    }

    fun removeMarker(marker: Marker){
        marker.remove()
        markers.remove(marker)
    }

    fun removeMarker(location: Location){
        removeMarker(markers.filter {
            it.position.latitude == location.latitude && it.position.longitude == location.longitude
        }[0])
    }
}