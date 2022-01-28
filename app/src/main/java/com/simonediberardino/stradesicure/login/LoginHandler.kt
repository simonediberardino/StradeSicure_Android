package com.simonediberardino.stradesicure.login

import com.facebook.AccessToken
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.activities.SSActivity
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
            SSActivity.currentContext?.getString(R.string.account_eliminato)!!
        else "${user.nome} ${user.cognome}"
    }

    inline fun <reified T> doLogin(loggedUser: T){
        deviceUser = loggedUser as User
        ApplicationData.setSavedAccount<T>(loggedUser)
    }

    fun doLogout(){
        deviceUser = null
        ApplicationData.setSavedAccount<User>(null)
    }
}