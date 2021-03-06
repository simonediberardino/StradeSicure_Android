package com.simonediberardino.stradesicure.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.matthewtamlin.sliding_intro_screen_library.buttons.IntroButton.Behaviour
import com.matthewtamlin.sliding_intro_screen_library.buttons.IntroButton.ProgressToNextActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.storage.ApplicationData
import com.simonediberardino.stradesicure.utils.Utility


class BeginActivity : SSActivity() {
    companion object{
        val PAGE_IDS = arrayOf(
            R.layout.activity_intro_1,
            R.layout.activity_intro_2,
            R.layout.activity_intro_3
        )
    }

    override fun initializeLayout() {}

    @RequiresApi(Build.VERSION_CODES.N)
    override fun generatePages(savedInstanceState: Bundle?): Collection<Fragment> {
        currentContext = this
        val pages: ArrayList<Fragment> = ArrayList()

        if (!ApplicationData.isFirstLaunch()) {
            Utility.goToMainMenu(this, false)
        }else{
            ApplicationData.isFirstLaunch(false)
            PAGE_IDS.forEach {
                val fragment = BeginFragment()
                fragment.contentViewId = it
                pages.add(fragment)
            }
        }

        return pages
    }

    @SuppressLint("CommitPrefEdits")
    override fun generateFinalButtonBehaviour(): Behaviour {
        val nextActivity = Intent(this, MapsActivity::class.java)
        return ProgressToNextActivity(nextActivity, null)
    }
}