package com.simonediberardino.stradesicure.entity

class EmailUser : User {
    lateinit var password: String

    constructor() {}

    constructor(
        nome: String,
        cognome: String,
        uniqueId: String,
        password: String,
    ) : super(nome, cognome, uniqueId) {
        this.password = password
    }
}

