package com.simonediberardino.stradesicure.login

import com.facebook.AccessToken
import com.facebook.Profile
import com.facebook.login.LoginManager
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.activities.SSActivity
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData
import com.simonediberardino.stradesicure.utils.Utility

object LoginHandler {
    var deviceUser: User? = null
    var accessToken: AccessToken? = null

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

    inline fun <reified T> doLogin(loggedUser: T?){
        if(loggedUser is FbUser)
            accessToken =  AccessToken.getCurrentAccessToken()

        deviceUser = loggedUser as User?
        ApplicationData.setSavedAccount<T>(loggedUser)
    }

    fun doLogout(){
        if(isFacebookLoggedIn())
            LoginManager.getInstance().logOut()

        deviceUser = null
        ApplicationData.setSavedAccount<User>(null)
    }

    fun logoutByError(){
        Utility.showToast(SSActivity.currentContext, SSActivity.currentContext.getString(R.string.erroreprofilocredenziali))
        doLogout()
    }
}