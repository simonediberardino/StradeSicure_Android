package com.simonediberardino.stradesicure.UI


import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.utils.Utility


class CDialog(
    var c: AppCompatActivity,
    var title: String,
    var description: String?,
    private var option1: String,
    private var option2: String,
    firstCallback: Runnable?,
    secondCallback: Runnable?,
    dismissCallback: Runnable?) : Dialog(c), View.OnClickListener {

    var firstCallback: Runnable
    var secondCallback: Runnable
    var dismissCallback: Runnable?

    constructor(c: AppCompatActivity, title: String, firstCallback: Runnable?) : this(
        c,
        title,
        String(),
        c.resources.getStringArray(R.array.yesno)[0],
        c.resources.getStringArray(R.array.yesno)[1],
        firstCallback,
        null,
        null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.short_dialog)

        val parentView = findViewById<ViewGroup>(R.id.parent)
        val title = findViewById<TextView>(R.id.short_dialog_title)
        val description = findViewById<TextView>(R.id.short_dialog_description)

        val btn_text_1 = findViewById<TextView>(R.id.short_dialog_text_1)
        val btn_text_2 = findViewById<TextView>(R.id.short_dialog_text_2)
        val btn_1 = findViewById<View>(R.id.short_dialog_button_1)
        val btn_2 = findViewById<View>(R.id.short_dialog_button_2)
        title.text = this.title
        description.text = this.description

        btn_1.setOnClickListener { firstCallback.run() }
        btn_text_1.text = option1
        btn_2.setOnClickListener { secondCallback.run() }
        btn_text_2.text = option2
        setOnDismissListener { if (dismissCallback != null) dismissCallback!!.run() }
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