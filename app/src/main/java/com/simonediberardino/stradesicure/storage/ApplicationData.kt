package com.simonediberardino.stradesicure.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.maps.GoogleMap
import com.google.gson.Gson
import com.simonediberardino.stradesicure.activities.SSActivity

object ApplicationData {
    const val DATA_ID = "data"
    const val ACCOUNT_ID = "ACCOUNT_ID"
    private const val MAP_STYLE_ID = "MAP_STYLE_ID"
    private const val MAP_STYLE_DEFAULT = GoogleMap.MAP_TYPE_NORMAL.toString()
    private const val MAP_UPDATE_ID = "MAP_UPDATE"
    private const val MAP_UPDATE_DEFAULT = "true"
    private const val ANOMALIE_CITTA_ID = "MAP_UPDATE"
    private const val ANOMALIE_CITTA_DEFAULT = "false"
    
    val ACCOUNT_DEFAULT = null

    fun getApplicationData(): SharedPreferences {
        return SSActivity.currentContext?.getSharedPreferences(DATA_ID, Context.MODE_PRIVATE)!!
    }

    fun isRealtimeUpdated(): Boolean {
        val savedJson: String? = getApplicationData().getString(MAP_UPDATE_ID, MAP_UPDATE_DEFAULT)
        return Gson().fromJson(savedJson, String::class.java).toBoolean()
    }

    fun isRealtimeUpdated(flag: Boolean) {
        val json = Gson().toJson(flag)
        val dataEditor = getApplicationData().edit()
        dataEditor.putString(MAP_UPDATE_ID, json)
        dataEditor.apply()
    }

    fun anomaliesInCity(): Boolean {
        val savedJson: String? = getApplicationData().getString(ANOMALIE_CITTA_ID, ANOMALIE_CITTA_DEFAULT)
        return Gson().fromJson(savedJson, String::class.java).toBoolean()
    }

    fun anomaliesInCity(flag: Boolean) {
        val json = Gson().toJson(flag)
        val dataEditor = getApplicationData().edit()
        dataEditor.putString(ANOMALIE_CITTA_ID, json)
        dataEditor.apply()
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

    fun getSavedMapStyle(): Int {
        val savedJson: String? = getApplicationData().getString(MAP_STYLE_ID, MAP_STYLE_DEFAULT)
        return Gson().fromJson(savedJson, String::class.java).toInt()
    }

    fun setSavedMapStyle(mapStyle: Int) {
        val json = Gson().toJson(mapStyle)
        val dataEditor = getApplicationData().edit()
        dataEditor.putString(MAP_STYLE_ID, json)
        dataEditor.apply()
    }
}