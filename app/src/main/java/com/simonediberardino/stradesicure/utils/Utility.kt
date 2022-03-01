package com.simonediberardino.stradesicure.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CDialog
import com.simonediberardino.stradesicure.activities.MapsActivity
import com.simonediberardino.stradesicure.activities.SSActivity
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import android.R as R1


object Utility {
    fun navigateTo(c: AppCompatActivity, cl: Class<*>?) {
        val intent = Intent(c, cl)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val bundle = ActivityOptionsCompat.makeCustomAnimation(c, R1.anim.fade_in, R1.anim.fade_out).toBundle()
        c.startActivity(intent, bundle)
    }

    fun goToMainMenu(c: AppCompatActivity){
        navigateTo(c, MapsActivity::class.java)
    }

    fun oneLineDialog(c: AppCompatActivity, title: String){
        CDialog(c, title, null).show()
    }

    fun oneLineDialog(c: AppCompatActivity, title: String, callback: Runnable){
        CDialog(c, title, callback).show()
    }

    fun oneLineDialog(c: AppCompatActivity, title: String, description: String?, callbackConfirm: Runnable?) {
        CDialog(c, title, description, c.resources.getStringArray(R.array.yesno)[0], c.resources.getStringArray(R.array.yesno)[1], callbackConfirm, null, null).show()
    }

    fun oneLineDialog(c: AppCompatActivity, title: String, description: String?, options: Array<String>, callbackConfirm: Runnable, callbackDeny: Runnable?) {
        CDialog(c, title, description, options[0], options[1], callbackConfirm, callbackDeny, null).show()
    }

    fun oneLineDialog(c: AppCompatActivity, title: String, description: String?, options: Array<String>, callbackConfirm: Runnable, callbackDeny: Runnable, callbackDismiss: Runnable) {
        CDialog(c, title, description, options[0], options[1], callbackConfirm, callbackDeny, callbackDismiss).show()
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

    fun isInternetAvailable(): Boolean {
        return isInternetAvailable(SSActivity.currentContext)
    }

    fun isInternetAvailable(appCompatActivity: AppCompatActivity): Boolean {
        val cm = appCompatActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nInfo = cm.activeNetworkInfo
        return nInfo != null && nInfo.isAvailable && nInfo.isConnected
    }

    fun capitalizeFirstLetter(string: String): String {
        if(string.isEmpty())
            return string

        val firstChar = string[0]
        val restOfString = if(string.length > 1) string.drop(1) else String()
        return "${firstChar.uppercase()}${restOfString.lowercase()}"
    }

    fun String.capitalizeWords(): String =
        lowercase().split(" ").joinToString(" ") { it.capitalize() }

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