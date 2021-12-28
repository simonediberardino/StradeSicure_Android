package com.simonediberardino.stradesicure

open class User{
    lateinit var nome: String
    lateinit var cognome: String

    constructor()

    constructor(nome: String, cognome: String) {
        this.nome = nome.trim { it <= ' ' }
        this.cognome = cognome.trim { it <= ' ' }
    }
}
