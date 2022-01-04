package com.simonediberardino.stradesicure

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity


object Utility {
    fun navigateTo(c: AppCompatActivity, cl: Class<*>?) {
        val i = Intent(c, cl)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        c.startActivity(i)
    }

    fun oneLineDialog(c: Context?, title: String?, callback: Runnable?) {
        CDialog((c as Activity?)!!, title, callback).show()
    }

    fun oneLineDialog(
        c: Context?,
        title: String?,
        option1: String?,
        option2: String?,
        firstCallback: Runnable?,
        secondCallback: Runnable?,
        dismissCallback: Runnable?
    ) {
        CDialog(
            (c as AppCompatActivity?)!!, title, option1!!, option2!!, firstCallback, secondCallback, dismissCallback
        ).show()
    }


    fun showToast(c: AppCompatActivity, message: String){
        Toast.makeText(c, message, Toast.LENGTH_LONG).show()
    }

    fun ridimensionamento(activity: AppCompatActivity, v: ViewGroup) {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val baseHeight = 1920.0
        val height = displayMetrics.heightPixels.toDouble()

        for (i in 0 until v.childCount) {
            val vAtI = v.getChildAt(i)
            val curHeight = vAtI.layoutParams.height
            val curWidth = vAtI.layoutParams.width
            val ratio = height / baseHeight

            if (curHeight > ViewGroup.LayoutParams.MATCH_PARENT)
                vAtI.layoutParams.height = (curHeight * ratio).toInt()

            if (curWidth > ViewGroup.LayoutParams.MATCH_PARENT)
                vAtI.layoutParams.width = (curWidth * ratio).toInt()

            if (vAtI is TextView) {
                val curSize = vAtI.textSize.toInt()
                val newSize = (curSize * ratio).toInt()
                vAtI.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize.toFloat())
            }

            vAtI.requestLayout()

            if (vAtI is ViewGroup) {
                ridimensionamento(activity, vAtI)
            }
        }
    }
}