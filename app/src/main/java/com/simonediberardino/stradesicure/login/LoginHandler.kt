package com.simonediberardino.stradesicure.login

import com.facebook.AccessToken
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.storage.ApplicationData

object LoginHandler {
    var deviceUser: User? = null

    fun isFacebookLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null || accessToken?.isExpired == true
    }

    fun isEmailLoggedIn(): Boolean {
        val storedUser = ApplicationData.getSavedAccount<User>()
        return storedUser is EmailUser
    }

    fun isLoggedIn(): Boolean {
        return deviceUser != null
    }

    fun getFullName(user: User?): String{
        return if(user == null)
            "undefined"
        else "${user.nome} ${user.cognome}"
    }

    fun hasValidCredentials(): Boolean{
        // Not implemented yet;
        return false
    }
}