package com.simonediberardino.stradesicure.entity

import com.simonediberardino.stradesicure.utils.Utility.capitalizeWords

open class User{
    var fullName: String = String()
        get() {
            return field.capitalizeWords()
        }

    lateinit var uniqueId: String
    var role = Roles.UTENTE

    constructor()

    constructor(fullName: String, uniqueId: String) {
        this.fullName = fullName.trim().lowercase()
        this.uniqueId = uniqueId.trim().lowercase()
    }

    override fun equals(other: Any?): Boolean {
        return this.uniqueId == (other as User).uniqueId
    }
}
