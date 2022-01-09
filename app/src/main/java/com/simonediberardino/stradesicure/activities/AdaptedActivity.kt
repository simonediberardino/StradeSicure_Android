package com.simonediberardino.stradesicure.activities

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.misc.NetworkStatusListener
import com.simonediberardino.stradesicure.utils.Utility
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*


abstract class AdaptedActivity(val showTopBar: Boolean) : AppCompatActivity() {
    constructor() : this(false)

    companion object {
        var lastContext: AdaptedActivity? = null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

/*
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
*/

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        this.registerReceiver(NetworkStatusListener(), intentFilter)

        lastContext = this

        this.initializeLayout()
        this.onPageLoaded()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
    }

    abstract fun initializeLayout()

    @RequiresApi(Build.VERSION_CODES.N)
    private fun onPageLoaded(){
        this.setupTopbar()
        Utility.ridimensionamento(this, findViewById(R.id.parent))
    }

    private fun setupTopbar(){
        if(!showTopBar)
            return

        val parent = findViewById<ViewGroup>(R.id.parent)

        val backButtonSize = 100
        val backButton = ImageView(this)
        backButton.setImageResource(R.drawable.ic_baseline_arrow_back_25)

        val layoutParams = ConstraintLayout.LayoutParams(backButtonSize, backButtonSize)
        layoutParams.leftToLeft = parent.id
        layoutParams.topToTop = parent.id
        layoutParams.leftMargin = 16
        layoutParams.topMargin = 16

        backButton.layoutParams = layoutParams;

        backButton.requestLayout()
        backButton.setOnClickListener {
            onBackPressed()
        }

        parent.addView(backButton)
    }
}