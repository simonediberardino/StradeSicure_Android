package com.simonediberardino.stradesicure.entity

class EmailUser : User {
    lateinit var password: String

    constructor() {}

    constructor(
        fullName: String,
        uniqueId: String,
        password: String,
    ) : super(fullName, uniqueId) {
        this.password = password
    }
}

