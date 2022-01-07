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
        return hasValidCredentials()
    }

    fun isLoggedIn(): Boolean {
        return isEmailLoggedIn() || isFacebookLoggedIn()
    }

    fun hasValidCredentials(): Boolean{
        // Not implemented yet;
        return true
    }
}