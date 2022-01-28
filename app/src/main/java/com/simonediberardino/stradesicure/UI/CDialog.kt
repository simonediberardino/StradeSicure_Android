package com.simonediberardino.stradesicure.UI


import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.utils.Utility


class CDialog(
    var c: AppCompatActivity,
    var title: String?,
    var option1: String,
    var option2: String,
    firstCallback: Runnable?,
    secondCallback: Runnable?,
    dismissCallback: Runnable?) : Dialog(c), View.OnClickListener {

    var firstCallback: Runnable
    var secondCallback: Runnable
    var dismissCallback: Runnable?

    constructor(c: AppCompatActivity, title: String?, firstCallback: Runnable?) : this(
        c,
        title,
        c.getString(R.string.ok),
        c.getString(R.string.annulla),
        firstCallback,
        null,
        null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.short_dialog)

        val parentView = findViewById<ViewGroup>(R.id.dialog_parent)
        val title = findViewById<TextView>(R.id.dialog_title)
        val btn1 = findViewById<Button>(R.id.dialog_btn1)
        val btn2 = findViewById<Button>(R.id.dialog_btn2)
        val btnClose = findViewById<View>(R.id.dialog_close)
        title.text = this.title
        btn1.setOnClickListener { firstCallback.run() }
        btn1.text = option1
        btn2.setOnClickListener { secondCallback.run() }
        btn2.text = option2
        setOnDismissListener { if (dismissCallback != null) dismissCallback!!.run() }
        btnClose.setOnClickListener { if (dismissCallback != null) dismissCallback!!.run() }
        Utility.ridimensionamento(c, parentView)
    }

    override fun onClick(v: View) {}

    init {
        this.dismissCallback = Runnable {
            dismissCallback?.run()
            dismiss()
        }
        this.firstCallback = Runnable {
            firstCallback?.run()
            this.dismissCallback = null
            dismiss()
        }
        this.secondCallback = Runnable {
            secondCallback?.run()
            this.dismissCallback = null
            dismiss()
        }
    }
}