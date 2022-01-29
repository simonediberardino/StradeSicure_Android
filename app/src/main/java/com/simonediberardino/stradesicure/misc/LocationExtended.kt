package com.simonediberardino.stradesicure.misc

import android.location.Location
class LocationExtended : Location(String()){
    override fun equals(other: Any?): Boolean {
        val startLat = this.latitude
        val startLong = this.longitude
        val endLat = (other as Location).latitude
        val endLong = (other as Location).longitude

        return startLat == endLat && startLong == endLong
    }

}