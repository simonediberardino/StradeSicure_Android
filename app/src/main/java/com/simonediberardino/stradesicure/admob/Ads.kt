package com.simonediberardino.stradesicure.admob

import android.app.Activity
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.simonediberardino.stradesicure.R

object Ads {
    const val BANNER = "ca-app-pub-5725383971112097/7660393163"
    const val INTERSTITIAL = "ca-app-pub-5725383971112097/4706340037"

    fun showBanner(ctx: Activity){
        val layout = ctx.findViewById<ViewGroup>(R.id.footer) ?: return

        val adView = AdView(ctx)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = BANNER

        val adRequest = AdRequest.Builder().build()

        layout.addView(adView)
        adView.loadAd(adRequest)
    }

    fun showInterstitial(ctx: Activity){
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(ctx,INTERSTITIAL, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {}

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAd.show(ctx)
            }
        })
    }

}