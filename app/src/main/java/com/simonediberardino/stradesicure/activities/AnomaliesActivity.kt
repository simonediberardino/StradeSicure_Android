package com.simonediberardino.stradesicure.activities

import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CSpinner
import com.simonediberardino.stradesicure.UI.ToastSS
import com.simonediberardino.stradesicure.activities.MapsActivity.Companion.mapsActivity
import com.simonediberardino.stradesicure.entity.Anomaly
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData

class AnomaliesActivity : SSActivity() {
    lateinit var refreshLayout: SwipeRefreshLayout
    var reporterId: String? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun initializeLayout() {
        setContentView(R.layout.activity_anomalies)

        reporterId = intent.extras?.getString("reporterId")

        refreshLayout = findViewById(R.id.anomalies_refresh_layout)
        refreshLayout.setOnRefreshListener {
            this.listAnomalies()
            refreshLayout.isRefreshing = false
        }

        setupFilterSpinner()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPageLoaded() {
        super.onPageLoaded()
        listAnomalies()
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private fun listAnomalies() {
        val parentLayoutId = R.id.anomalies_anomaly_layout

        val anomaliesContainer = findViewById<LinearLayout>(parentLayoutId)
        anomaliesContainer.removeAllViews()

        var anomaliesToShow = if(ApplicationData.anomaliesInCity()){
                mapsActivity.getAnomaliesInCity(mapsActivity.userLocation)
            }else{
                mapsActivity.anomalies.toTypedArray()
            }

        if(reporterId != null)
            anomaliesToShow = anomaliesToShow.filter { it.spotterId == reporterId }.toTypedArray()

        val nAnomaliesTW = findViewById<TextView>(R.id.anomalies_n)
        nAnomaliesTW.text = getString(R.string.anomalie_trovate)
            .replace("{number}", anomaliesToShow.size.toString())

        if(anomaliesToShow.isEmpty()) {
            ToastSS.show(this, getString(R.string.nessunaanomalia))
            return
        }

        mapsActivity.sortByDistance(anomaliesToShow)

        val parentViewId = R.id.parent
        val layoutToAddId = R.layout.single_anomaly

        for(anomaly: Anomaly in anomaliesToShow){
            MapsActivity.listAnomaly(this, parentLayoutId, parentViewId, layoutToAddId, anomaly)
        }
    }

    private fun setupFilterSpinner(){
        val mainLayout = findViewById<ViewGroup>(R.id.anomalies_viewgroup_2)
        val spinner = CSpinner(this, mainLayout)

        val options = resources.getStringArray(R.array.yesno)

        spinner.title = getString(R.string.anomalies_filter)
        spinner.options = options
        spinner.setOnChangeListener(object : RunnablePar() {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun run(p: Any?) {
                ApplicationData.anomaliesInCity((p as Int) == 0)
                listAnomalies()
            }
        })

        spinner.apply()
        spinner.selectItem(if(!ApplicationData.anomaliesInCity()) 1 else 0)
    }
}