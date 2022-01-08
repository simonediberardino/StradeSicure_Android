package com.simonediberardino.stradesicure.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.simonediberardino.stradesicure.activities.AdaptedActivity

object ApplicationData {
    const val DATA_ID = "data"
    const val ACCOUNT_ID = "ACCOUNT_ID"
    val ACCOUNT_DEFAULT = null

    fun getApplicationData(): SharedPreferences {
        return AdaptedActivity.lastContext?.getSharedPreferences(DATA_ID, Context.MODE_PRIVATE)!!
    }

    inline fun <reified T> getSavedAccount(): T? {
        val savedJson: String? = getApplicationData().getString(ACCOUNT_ID, ACCOUNT_DEFAULT)
        return Gson().fromJson(savedJson, T::class.java)
    }

    inline fun <reified T> setSavedAccount(account: T?) {
        val json = Gson().toJson(account)
        val dataEditor = getApplicationData().edit()
        dataEditor.putString(ACCOUNT_ID, json);
        dataEditor.apply()
    }
}