package com.simonediberardino.stradesicure.activities

import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CButton
import com.simonediberardino.stradesicure.UI.CSpinner
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData
import com.simonediberardino.stradesicure.utils.Utility

class SettingsActivity : AdaptedActivity() {
    override fun initializeLayout() {
        setContentView(R.layout.activity_settings)
        this.setupMapThemeSpinner()
        this.setupLogoutBtn()
    }

    private fun setupMapThemeSpinner(){
        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_1)
        val spinner = CSpinner(this, mainLayout)

        val themeNames = resources.getStringArray(R.array.maptheme)
        val themeObjs = arrayOf(
            MapTheme(GoogleMap.MAP_TYPE_NORMAL, themeNames[0]),
            MapTheme(GoogleMap.MAP_TYPE_SATELLITE, themeNames[1])
        )

        spinner.title = getString(R.string.mapstyle)
        spinner.options = themeNames
        spinner.setOnChangeListener(object : RunnablePar {
            override fun run(p: Any?) {
                ApplicationData.setSavedMapStyle(themeObjs[p as Int].themeId)
            }
        })

        spinner.apply()
        spinner.selectItem(themeObjs.map { it.themeId }.indexOf(ApplicationData.getSavedMapStyle()))
    }

    private fun setupLogoutBtn(){
        if(!LoginHandler.isLoggedIn())
            return

        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_2)
        val button = CButton(this, mainLayout)

        button.title = getString(R.string.logout)
        button.setOnConfirmListener {
            LoginHandler.doLogout()
            Utility.showToast(this, getString(R.string.logout_success))
        }

        button.apply()
    }

    class MapTheme(val themeId: Int, val themeName: String)
}