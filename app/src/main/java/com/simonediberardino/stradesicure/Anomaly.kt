package com.simonediberardino.stradesicure

class Anomaly{
    lateinit var location: LocationExtended
    lateinit var spotterId: String
    lateinit var description: String
    var stars: Int = 0
    constructor()
    constructor(location: LocationExtended, spotterId: String, description: String, stars: Int, )
}