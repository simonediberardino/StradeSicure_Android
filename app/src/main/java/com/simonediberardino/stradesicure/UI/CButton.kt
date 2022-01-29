package com.simonediberardino.stradesicure.UI

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.R




@SuppressLint("ViewConstructor")
open class CButton(val context: AppCompatActivity, val parent: ViewGroup) : View(context) {
    private var titleTV: TextView
    private var descriptionTV: TextView
    private var content: View
    private var onConfirmListener: Runnable
    private var onDenyListener: Runnable

    var question: String? = null

    var options: Array<String>? = null

    var title: String = String()
        get() {
            return String()
        }
        set(value){
            field = value
            titleTV.text = value
        }

    var description: String = String()
        get() {
            return String()
        }
        set(value){
            field = value
            descriptionTV.text = value
        }


    fun setOnConfirmListener(onConfirmListener: Runnable){
        this.onConfirmListener = onConfirmListener
    }

    fun setOnDenyListener(onDenyListener: Runnable){
        this.onDenyListener = onDenyListener
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showConfirmDialog(){
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(
                context.getString(R.string.logout_confirm)
            )
            .setPositiveButton(
                context.getString(R.string.conferma)
            ) { _, _ ->
                this.onConfirmListener.run()
            }
            .setNegativeButton(
                resources.getString(R.string.annulla)
            ) { _, _ ->

            }.show()
    }

    fun apply() {
        parent.addView(content)
    }

    init {
        val inflater = LayoutInflater.from(context)
        val layoutToAdd: Int = R.layout.long_button

        content = inflater.inflate(layoutToAdd, parent, false)
        titleTV = content.findViewById(R.id.button_title)
        descriptionTV = content.findViewById(R.id.button_description)
        options = arrayOf(context.getString(R.string.conferma), context.getString(R.string.annulla))
        onConfirmListener = Runnable {}
        onDenyListener = Runnable {}
        content.setOnClickListener {
            if(question == null)
                this.onConfirmListener.run()
            else showConfirmDialog()
        }
    }
}