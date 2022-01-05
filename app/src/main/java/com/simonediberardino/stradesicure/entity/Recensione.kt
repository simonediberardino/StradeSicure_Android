package com.simonediberardino.stradesicure.entity

class Recensione{
    lateinit var authorId: String
    lateinit var anomalyId: String
    lateinit var content: String

    constructor()

    constructor(authorId: String, anomalyId: String, content: String){
        this.authorId = authorId
        this.anomalyId = anomalyId
        this.content = content
    }
}
