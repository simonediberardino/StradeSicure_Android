package com.simonediberardino.stradesicure.misc

import android.net.ConnectivityManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.widget.Toast
import com.simonediberardino.stradesicure.R
import java.lang.Boolean


class NetworkStatusListener : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.extras!!.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
            Toast.makeText(context, context?.getString(R.string.connessione_persa), Toast.LENGTH_LONG).show()
        }
    }
}