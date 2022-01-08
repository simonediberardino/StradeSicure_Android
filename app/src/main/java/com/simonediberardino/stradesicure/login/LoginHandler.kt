package com.simonediberardino.stradesicure.login

import com.facebook.AccessToken
import com.simonediberardino.stradesicure.entity.User

object LoginHandler {
    var deviceUser: User? = null

    fun isFacebookLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null || accessToken?.isExpired == true
    }

    fun isEmailLoggedIn(): Boolean{
        return deviceUser != null
    }

    fun isLoggedIn(): Boolean {
        return isEmailLoggedIn() || isFacebookLoggedIn()
    }

    fun getFullName(user: User): String{
        return "${user.nome} ${user.cognome}"
    }

    fun hasValidCredentials(): Boolean{
        // Not implemented yet;
        return false
    }
}