package com.simonediberardino.stradesicure.activities

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.misc.NetworkStatusListener
import com.simonediberardino.stradesicure.utils.Utility

open class AdaptedActivity : AppCompatActivity() {
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
    }

    fun onPageLoaded(){
        Utility.ridimensionamento(this, findViewById(R.id.parent))
    }

}