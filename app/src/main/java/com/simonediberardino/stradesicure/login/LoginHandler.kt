package com.simonediberardino.stradesicure.login

import com.facebook.AccessToken
import com.facebook.Profile
import com.facebook.login.LoginManager
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.activities.SSActivity
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData

object LoginHandler {
    var deviceUser: User? = null

    fun isFacebookLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null || accessToken?.isExpired == true
    }

    fun waittilFBProfileIsReady(callback: RunnablePar){
        Thread{
            var currProfile: Profile?

            while(Profile.getCurrentProfile().also { currProfile = it } == null)
                Thread.sleep(1)

            callback.run(currProfile)
        }.start()
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

    inline fun <reified T> doLogin(loggedUser: T?){
        deviceUser = loggedUser as User?
        ApplicationData.setSavedAccount<T>(loggedUser)
    }

    fun doLogout(){
        if(isFacebookLoggedIn())
            LoginManager.getInstance().logOut()

        deviceUser = null
        ApplicationData.setSavedAccount<User>(null)
    }
}