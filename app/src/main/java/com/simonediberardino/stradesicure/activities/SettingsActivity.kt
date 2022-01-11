package com.simonediberardino.stradesicure.activities

import android.view.ViewGroup
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CSpinner

class SettingsActivity : AdaptedActivity() {
    override fun initializeLayout() {
        setContentView(R.layout.activity_settings)
        this.setupMapThemeSpinner()
    }

    private fun setupMapThemeSpinner(){
        val mainLayout = findViewById<ViewGroup>(R.id.settings_viewgroup_1)
        var spinner = CSpinner(this, mainLayout)

        spinner.options = resources.getStringArray(R.array.maptheme)
        spinner.title = getString(R.string.mapstyle)

        spinner.apply()
    }
}