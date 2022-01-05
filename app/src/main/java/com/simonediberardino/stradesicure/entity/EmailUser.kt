package com.simonediberardino.stradesicure.entity

class EmailUser : User {
    lateinit var email: String
    lateinit var password: String
    constructor() {}

    constructor(
        nome: String,
        cognome: String,
        email: String,
        password: String,
    ) : super(nome, cognome) {
        this.email = email
        this.password = password
    }
}

