package com.simonediberardino.stradesicure.UI

import android.app.Dialog
import android.content.Context
import com.simonediberardino.stradesicure.R

class LoadingDialog(ctx: Context) : Dialog(ctx, R.style.Theme_StradeSicure) {
    init {
        setCancelable(false)
    }
    override fun show(){
        setContentView(R.layout.activity_loading)
        super.show()
    }
}