package com.simonediberardino.stradesicure.storage

import android.content.Context
import android.content.SharedPreferences
import com.simonediberardino.stradesicure.activities.AdaptedActivity

object ApplicationData {
    private const val DATA_ID = "data"

    private const val ACCOUNT_ID = "ACCOUNT_ID"
    private val ACCOUNT_DEFAULT = null

    private const val PASSWORD_ID = "PASSWORD"
    private val PASSWORD_DEFAULT = null

    private fun getApplicationData(): SharedPreferences {
        return AdaptedActivity.lastContext?.getSharedPreferences(DATA_ID, Context.MODE_PRIVATE)!!
    }

    fun getAccountID(): String? {
        return getApplicationData().getString(ACCOUNT_ID, ACCOUNT_DEFAULT)
    }

    fun setAccountID(accountID: String) {
        getApplicationData().edit().putString(ACCOUNT_ID, accountID).apply()
    }

    fun getPassword(): String? {
        return getApplicationData().getString(PASSWORD_ID, PASSWORD_DEFAULT)
    }

    fun setPassword(password: String) {
        getApplicationData().edit().putString(PASSWORD_ID, PASSWORD_DEFAULT).apply()
    }
}