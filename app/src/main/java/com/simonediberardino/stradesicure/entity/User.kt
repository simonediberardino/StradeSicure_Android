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
}
