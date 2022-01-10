package com.simonediberardino.stradesicure.activities

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.misc.NetworkStatusListener
import com.simonediberardino.stradesicure.utils.Utility


abstract class AdaptedActivity(private val showTopBar: Boolean) : AppCompatActivity() {
    constructor() : this(false)

    companion object {
        var lastContext: AdaptedActivity? = null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        this.registerReceiver(NetworkStatusListener(), intentFilter)

        lastContext = this

        this.initializeLayout()
        this.onPageLoaded()
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
        layoutParams.leftMargin = 32
        layoutParams.topMargin = 32

        backButton.layoutParams = layoutParams;

        backButton.requestLayout()
        backButton.setOnClickListener {
            onBackPressed()
        }

        parent.addView(backButton)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    protected fun onPageLoaded(){
        this.setupTopbar()
        Utility.ridimensionamento(this, findViewById(R.id.parent))
    }

    abstract fun initializeLayout()
    open fun setContentView(){}
}