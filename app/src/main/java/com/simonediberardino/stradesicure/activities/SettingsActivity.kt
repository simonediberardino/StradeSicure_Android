package com.simonediberardino.stradesicure.activities

import android.view.ViewGroup
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CButton
import com.simonediberardino.stradesicure.UI.CSpinner
import com.simonediberardino.stradesicure.login.LoginHandler
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

        spinner.options = resources.getStringArray(R.array.maptheme)
        spinner.title = getString(R.string.mapstyle)

        spinner.apply()
    }

    private fun setupLogoutBtn(){
        if(!LoginHandler.isLoggedIn())
            return

        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_2)
        val button = CButton(this, mainLayout)

        button.title = getString(R.string.logout)
        button.setOnConfirmListener {
            LoginActivity.onLogout()
            Utility.showToast(this, getString(R.string.logout_success))
        }

        button.apply()
    }
}