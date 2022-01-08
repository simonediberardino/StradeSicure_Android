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
        return if(deviceUser == null){
            ApplicationData.getSavedAccount<EmailUser>() == null
        }else{
            true
        }
    }

    fun isLoggedIn(): Boolean {
        return deviceUser != null
    }

    fun getFullName(user: User): String{
        return "${user.nome} ${user.cognome}"
    }

    fun hasValidCredentials(): Boolean{
        // Not implemented yet;
        return false
    }
}