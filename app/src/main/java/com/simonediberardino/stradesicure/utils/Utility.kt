package com.simonediberardino.stradesicure.utils

import android.content.Intent
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simonediberardino.stradesicure.UI.CDialog
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object Utility {
    fun navigateTo(c: AppCompatActivity, cl: Class<*>?) {
        val i = Intent(c, cl)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        c.startActivity(i)
    }

    fun oneLineDialog(c: AppCompatActivity?, title: String?, callback: Runnable?) {
        CDialog(c!!, title, callback).show()
    }

    fun oneLineDialog(
        c: AppCompatActivity?,
        title: String?,
        option1: String?,
        option2: String?,
        firstCallback: Runnable?,
        secondCallback: Runnable?,
        dismissCallback: Runnable?,
    ) {
        CDialog(
            c!!, title, option1!!, option2!!, firstCallback, secondCallback, dismissCallback
        ).show()
    }

    fun getMD5(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val messageDigest = md.digest(input.toByteArray())

            val no = BigInteger(1, messageDigest)

            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            hashtext
        }
        catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
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