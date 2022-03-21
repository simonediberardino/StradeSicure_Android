package com.simonediberardino.stradesicure.UI

import android.content.Context
import android.widget.Toast

object ToastSS {
    private var toast: Toast? = null

    fun show(context: Context?, text: String) {
        if (toast == null) {
            toast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
            toast!!.show()
            Thread{
                Thread.sleep(toast!!.duration.toLong())
                toast = null
            }.start()
        }
    }
}