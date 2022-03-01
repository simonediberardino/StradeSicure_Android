package com.simonediberardino.stradesicure.entity

open class User{
    lateinit var fullName: String
    lateinit var uniqueId: String
    var role = Roles.UTENTE

    constructor()

    constructor(fullName: String, uniqueId: String) {
        this.fullName = fullName.trim()
        this.uniqueId = uniqueId.trim()
    }

    override fun equals(other: Any?): Boolean {
        return this.uniqueId == (other as User).uniqueId
    }
}
