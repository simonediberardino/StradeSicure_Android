package com.simonediberardino.stradesicure.entity

enum class Roles {
    UTENTE,
    VIP,
    MODERATORE,
    AMMINISTRATORE,
    PROPRIETARIO;

    fun isGreaterOr(role: Roles?): Boolean {
        if(role == null) return true
        return this >= role
    }

    fun isLowerOr(role: Roles?): Boolean{
        if(role == null) return true
        return this <= role
    }

}