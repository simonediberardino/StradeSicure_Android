package com.simonediberardino.stradesicure.UI

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.misc.RunnablePar

open class CSpinner(val context: AppCompatActivity, val parent: ViewGroup) : View(context) {
    private var titleTV: TextView
    private var valueTV: TextView
    private var content: View
    private var onChangeListener: RunnablePar? = null

    var options: Array<String>? = null

    var title: String = String()
        get() {
            return String()
        }
        set(value){
            field = value
            titleTV.text = value
        }

    fun setOnChangeListener(onChangeListener: RunnablePar){
        this.onChangeListener = onChangeListener
    }

    open fun onSelectedItem(){}

    private fun selectItem(index: Int){
        if(options == null)
            return

        valueTV.text = options!![index]
        onSelectedItem()
    }

    private fun showConfirmDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(title)

        builder.setItems(options) { _, which ->
            selectItem(which)
            this.onChangeListener?.run(which)
        }

        builder.create().show()
    }

    fun apply() {
        selectItem(0)
        parent.addView(content)
    }

    init {
        val inflater = LayoutInflater.from(context)
        val layoutToAdd: Int = R.layout.long_spinner

        content = inflater.inflate(layoutToAdd, parent, false)
        titleTV = content.findViewById(R.id.spinner_title)
        valueTV = content.findViewById(R.id.spinner_value)
        content.setOnClickListener { if(options != null) showConfirmDialog() }
    }
}