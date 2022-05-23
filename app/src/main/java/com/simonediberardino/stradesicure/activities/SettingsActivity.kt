package com.simonediberardino.stradesicure.activities

import android.view.ViewGroup
import com.facebook.login.Login
import com.google.android.gms.maps.GoogleMap
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CButton
import com.simonediberardino.stradesicure.UI.CSpinner
import com.simonediberardino.stradesicure.UI.ToastSS
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData
import com.simonediberardino.stradesicure.utils.Utility

class SettingsActivity : SSActivity() {
    override fun initializeLayout() {
        setContentView(R.layout.activity_settings)
        this.setupMapThemeSpinner()
        this.setupUpdateAnomaliesSpinner()
        this.setupLogoutBtn()
        this.setupDeleteAccountButton()
        this.setupLoginButton()
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

    private fun setupUpdateAnomaliesSpinner(){
        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_1)
        val spinner = CSpinner(this, mainLayout)

        val options = resources.getStringArray(R.array.yesno)

        spinner.title = getString(R.string.realtimeupdate)
        spinner.options = options
        spinner.setOnChangeListener(object : RunnablePar {
            override fun run(p: Any?) {
                ApplicationData.isRealtimeUpdated((p as Int) == 0)
            }
        })

        spinner.apply()
        spinner.selectItem(if(!ApplicationData.isRealtimeUpdated()) 1 else 0)
    }

    private fun setupLogoutBtn(){
        if(!LoginHandler.isLoggedIn())
            return

        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_2)
        val button = CButton(this, mainLayout)

        button.title = getString(R.string.logout)
        button.description = getString(R.string.logout_description)
        button.question = getString(R.string.logout_confirm)

        button.setOnConfirmListener {
            LoginHandler.doLogout()
            Utility.goToMainMenu(this)
            ToastSS.show(this, getString(R.string.logout_success))
        }

        button.apply()
    }

    private fun setupDeleteAccountButton(){
        if(!LoginHandler.isLoggedIn())
            return

        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_2)
        val button = CButton(this, mainLayout)

        button.title = getString(R.string.cancella_account)
        button.description = getString(R.string.cancella_account_description)

        button.setOnConfirmListener {
            FirebaseClass.deleteAccountFirebase(LoginHandler.deviceUser!!)
            LoginHandler.doLogout()
            ToastSS.show(this, getString(R.string.cancella_account_successo))
        }

        button.apply()
    }

    private fun setupLoginButton(){
        if(LoginHandler.isLoggedIn())
            return

        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_2)
        val button = CButton(this, mainLayout)

        button.title = getString(R.string.login)
        button.description = getString(R.string.login_description)

        button.setOnConfirmListener {
            Utility.navigateTo(this, LoginActivity::class.java)
        }

        button.apply()
    }

    class MapTheme(val themeId: Int, val themeName: String)
}