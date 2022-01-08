package com.simonediberardino.stradesicure.UI

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R

class ProgressDialog(appCompatActivity: AppCompatActivity) : Dialog(appCompatActivity) {
    var progress: Int = 0
        set(value){
            field = value
            this.findViewById<TextView>(R.id.load_dialog_descr).text =
                this.context.getString(R.string.progress).replace("{progress}", progress.toString())
            if(progress >= 100)
                this.dismiss()
        }

    init{
        this.setContentView(R.layout.loading_dialog)
        this.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        this.setCancelable(false)
        this.show()
    }
}