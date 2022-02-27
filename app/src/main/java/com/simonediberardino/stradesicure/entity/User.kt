package com.simonediberardino.stradesicure.entity

open class User{
    lateinit var nome: String
    lateinit var cognome: String
    lateinit var uniqueId: String

    constructor()

    constructor(nome: String, cognome: String, uniqueId: String) {
        this.nome = nome.trim()
        this.cognome = cognome.trim()
        this.uniqueId = uniqueId.trim()
    }

    override fun equals(other: Any?): Boolean {
        println("ISTRUE ${this.uniqueId} ${(other as User).uniqueId}")
        return this.uniqueId == (other as User).uniqueId
    }
}
