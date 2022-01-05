package com.simonediberardino.stradesicure.entity

import com.simonediberardino.stradesicure.misc.LocationExtended

class Anomaly{
    lateinit var location: LocationExtended
    lateinit var spotterId: String
    lateinit var description: String
    var stato: Int = 0
    constructor()
    constructor(location: LocationExtended, spotterId: String, description: String, stato: Int){
        this.location = location
        this.spotterId = spotterId
        this.description = description
        this.stato = stato
    }
}