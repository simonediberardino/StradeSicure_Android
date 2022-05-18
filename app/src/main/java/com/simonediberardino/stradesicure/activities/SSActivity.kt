package com.simonediberardino.stradesicure.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.matthewtamlin.sliding_intro_screen_library.buttons.IntroButton
import com.matthewtamlin.sliding_intro_screen_library.core.IntroActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.LoadingDialog
import com.simonediberardino.stradesicure.admob.Ads
import com.simonediberardino.stradesicure.misc.NetworkStatusListener
import com.simonediberardino.stradesicure.utils.Utility

abstract class SSActivity : IntroActivity() {
    lateinit var loadingDialog: LoadingDialog
    var uploadedImage: Uri? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var currentContext: AppCompatActivity
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        this.registerReceiver(NetworkStatusListener(), intentFilter)

        currentContext = this
        loadingDialog = LoadingDialog(this)

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
    open fun onPageLoaded(){
        val parent = findViewById<ViewGroup>(R.id.parent)
        if(parent != null) {
            this.setupTopbar()
            Utility.ridimensionamento(this, parent)
        }

        Ads.showBanner(this)

        if(this is MapsActivity || this is BeginActivity){
            return
        }

        Utility.runnablePercentage(10){
            Ads.showInterstitial(this)
        }
    }

    abstract fun initializeLayout()
    open fun setContentView(){}

    fun startGalleryActivity(){
        val PICK_IMAGE = 100
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    override fun generatePages(savedInstanceState: Bundle?): Collection<Fragment> {
        return pages
    }

    override fun generateFinalButtonBehaviour(): IntroButton.Behaviour? {
        return object : IntroButton.Behaviour{
            override fun run() {}
            override fun setActivity(activity: IntroActivity?) {}

            override fun getActivity(): IntroActivity {
                return this@SSActivity
            }
        }
    }
}