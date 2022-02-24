package com.simonediberardino.stradesicure.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.matthewtamlin.sliding_intro_screen_library.background.BackgroundManager
import com.matthewtamlin.sliding_intro_screen_library.background.ColorBlender
import com.matthewtamlin.sliding_intro_screen_library.buttons.IntroButton.Behaviour
import com.matthewtamlin.sliding_intro_screen_library.buttons.IntroButton.ProgressToNextActivity
import com.matthewtamlin.sliding_intro_screen_library.core.IntroActivity
import com.matthewtamlin.sliding_intro_screen_library.transformers.MultiViewParallaxTransformer
import com.simonediberardino.stradesicure.R





class BeginActivity : IntroActivity() {
    companion object{
        private val BACKGROUND_COLORS = intArrayOf(-0xcfb002, -0x33ff9a, -0x66ff01)

        val PAGE_IDS = arrayOf(
            R.layout.activity_intro_1,
            R.layout.activity_intro_2,
            R.layout.activity_intro_3
        )
    }

    val DISPLAY_ONCE_PREFS = "display_only_once_spfile"
    val DISPLAY_ONCE_KEY = "display_only_once_spkey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

/*        if (introductionCompletedPreviously()) {
            val nextActivity = Intent(this, MapsActivity::class.java)
            startActivity(nextActivity)
        }*/
        configureTransformer()
    }

    override fun generatePages(savedInstanceState: Bundle?): Collection<Fragment> {
        val pages: ArrayList<Fragment> = ArrayList()

        PAGE_IDS.forEach {
            val fragment = BeginFragment()
            fragment.contentViewId = it
            pages.add(fragment)
        }
        return pages
    }

    @SuppressLint("CommitPrefEdits")
    override fun generateFinalButtonBehaviour(): Behaviour {
        val sp = getSharedPreferences(DISPLAY_ONCE_PREFS, MODE_PRIVATE)
        val pendingEdits = sp.edit().putBoolean(DISPLAY_ONCE_KEY, true)

        val nextActivity = Intent(this, MapsActivity::class.java)
        return ProgressToNextActivity(nextActivity, pendingEdits)
    }

    private fun introductionCompletedPreviously(): Boolean {
        val sp = getSharedPreferences(DISPLAY_ONCE_PREFS, MODE_PRIVATE)
        return sp.getBoolean(DISPLAY_ONCE_KEY, false)
    }

    private fun configureTransformer() {
        val transformer = MultiViewParallaxTransformer()
        transformer.withParallaxView(R.id.activity_begin_fragment_text_holder, 1.2f)
        setPageTransformer(false, transformer)
    }

    private fun configureBackground() {
        val backgroundManager: BackgroundManager = ColorBlender(BACKGROUND_COLORS)
        setBackgroundManager(backgroundManager)
    }
}