package com.simonediberardino.stradesicure

import android.location.Location
import android.os.Bundle
import android.os.IBinder
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.internal.maps.*
import com.google.android.gms.internal.maps.zzaa
import com.google.android.gms.internal.maps.zzag
import com.google.android.gms.internal.maps.zzl
import com.google.android.gms.internal.maps.zzo
import com.google.android.gms.internal.maps.zzr
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.internal.*
import com.google.android.gms.maps.internal.zzab
import com.google.android.gms.maps.internal.zzad
import com.google.android.gms.maps.internal.zzaf
import com.google.android.gms.maps.internal.zzah
import com.google.android.gms.maps.internal.zzd
import com.google.android.gms.maps.internal.zzi
import com.google.android.gms.maps.internal.zzn
import com.google.android.gms.maps.internal.zzp
import com.google.android.gms.maps.internal.zzt
import com.google.android.gms.maps.internal.zzv
import com.google.android.gms.maps.internal.zzx
import com.google.android.gms.maps.internal.zzz
import com.google.android.gms.maps.model.*
import java.lang.reflect.Field

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
        val ms = markers.filter {
            it.position.latitude == location.latitude && it.position.longitude == location.longitude
        }

        removeMarker(ms[0])
/*        ms.forEach {
            removeMarker(it)
        }*/
    }
}