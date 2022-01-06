package com.simonediberardino.stradesicure.activities

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import com.simonediberardino.stradesicure.R

class AccountActivity : AdaptedActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.initializeLayout()

        super.onPageLoaded()
    }

    override fun initializeLayout() {
        setContentView(R.layout.activity_login)
        findViewById<View>(R.id.login_login_button).setOnClickListener {
            println("a")
        }
    }
}