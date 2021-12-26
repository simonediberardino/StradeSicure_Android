package com.simonediberardino.stradesicure

import android.content.Intent
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity


object Utility {
    fun navigateTo(c: AppCompatActivity, cl: Class<*>?) {
        val i = Intent(c, cl)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        c.startActivity(i)
    }

    // Ridimensiona i componenti in base alla dimensione dello schermo;
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