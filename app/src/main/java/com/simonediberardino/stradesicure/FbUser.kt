package com.simonediberardino.stradesicure

class FbUser : User {
    lateinit var userId: String
    constructor() {}
    constructor(nome: String, cognome: String, userId: String) : super(nome, cognome) {
        this.userId = userId
    }
}


