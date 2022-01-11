package com.simonediberardino.stradesicure.activities

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.misc.NetworkStatusListener
import com.simonediberardino.stradesicure.utils.Utility


abstract class AdaptedActivity : AppCompatActivity() {
    companion object {
        var lastContext: AdaptedActivity? = null
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        this.registerReceiver(NetworkStatusListener(), intentFilter)

        lastContext = this

        this.initializeLayout()
        this.onPageLoaded()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupTopbar() {
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.topbar))
        supportActionBar?.setCustomView(R.layout.topbar)

        val backBtn = findViewById<View>(R.id.topbar_lefticon)
        backBtn?.setOnClickListener { onBackPressed() }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    protected fun onPageLoaded(){
        this.setupTopbar()
        Utility.ridimensionamento(this, findViewById(R.id.parent))
    }

    abstract fun initializeLayout()
    open fun setContentView(){}
}