package com.simonediberardino.stradesicure

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.simonediberardino.stradesicure.databinding.ActivityMapsBinding
import android.content.pm.PackageManager
import android.location.*

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*
import android.location.Geocoder
import android.os.PersistableBundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.github.techisfun.android.topsheet.TopSheetBehavior
import com.github.techisfun.android.topsheet.TopSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import kotlin.collections.ArrayList
import androidx.annotation.NonNull

import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception


class MapsActivity : AdaptedActivity(), OnMapReadyCallback, LocationListener {
    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var userLocation: Location
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var topSheetBehavior: TopSheetBehavior<View>
    private lateinit var refreshMapTimer: Countdown
    private lateinit var anomalies: ArrayList<Anomaly>
    private var anomalyMarker: Marker? = null
    private var lastUserLocMarker: Marker? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        this.initializeLayout()
        super.onPageLoaded()
    }

    fun initializeLayout(){
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_persistent))
        bottomSheetBehavior.peekHeight = 350
        bottomSheetBehavior.isHideable = false

        val topSheet = findViewById<View>(R.id.top_sheet_persistent)

        topSheetBehavior = TopSheetBehavior.from(topSheet)
        topSheetBehavior.state = TopSheetBehavior.STATE_COLLAPSED
        topSheetBehavior.peekHeight = 90
        topSheetBehavior.setTopSheetCallback(object : TopSheetBehavior.TopSheetCallback() {
            override fun onStateChanged(topSheet: View, newState: Int) {
                if(newState == TopSheetBehavior.STATE_HIDDEN)
                    topSheetBehavior.state = TopSheetBehavior.STATE_COLLAPSED
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float){}
        })

        val reportBTN = findViewById<View>(R.id.dialog_report_btn)

        reportBTN.setOnClickListener {
            topSheetBehavior.state =
                if(topSheetBehavior.state == TopSheetBehavior.STATE_EXPANDED)
                    TopSheetBehavior.STATE_COLLAPSED
                else
                    TopSheetBehavior.STATE_EXPANDED
        }

        val resetLocationBTN = findViewById<View>(R.id.main_my_location)
        resetLocationBTN.setOnClickListener{
            if(topSheetBehavior.state == TopSheetBehavior.STATE_COLLAPSED)
                this.zoomMapToUser()
        }

        val backBTN = findViewById<View>(R.id.report_back)
        backBTN.setOnClickListener{
            topSheetBehavior.state = TopSheetBehavior.STATE_COLLAPSED
        }

        val addressET = findViewById<TextInputEditText>(R.id.report_address)
        addressET.setOnClickListener{
            Toast.makeText(this, getString(R.string.aggiungi_marker_tut), Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.anomalies = ArrayList()
        this.googleMap = googleMap

        this.googleMap.setOnMapLongClickListener {
            addAnomalyMarker(it)
            updateAnomalyLocation()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        this.setupGPS()
        this.setupTimer()
    }

    fun setupGPS() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                GEOLOCATION_PERMISSION_CODE)
        }else{
            this.setupMap()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            GEOLOCATION_PERMISSION_CODE -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.setupMap()
                }else{
                    this.insufficientPermissions()
                }
            }
        }
    }

    fun insufficientPermissions(){
        Utility.showDialog(this, getString(R.string.request_permissions))
    }

    fun removeAnomalyMarker(){
        val addressET = findViewById<TextInputEditText>(R.id.report_address)
        addressET.setText(getString(R.string.tua_posizione))
        anomalyMarker?.remove()
    }

    @SuppressLint("MissingPermission")
    fun setupMap() {
        try {
            this.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)!!)
            this.removeAnomalyMarker()

            this.googleMap.setOnMarkerClickListener {
                when (it) {
                    anomalyMarker -> this.removeAnomalyMarker()
                    lastUserLocMarker -> this.zoomMapToUser()
                }

                return@setOnMarkerClickListener false
            }

            this.zoomMapToUser()

            this.fetchAnomalies(object : RunnablePar {
                override fun run(any: Any) {
                    showAnomaly(any as Anomaly)
                    storeAnomaly(any)
                }
            }) {
                listAnomalies()
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 2f, this)
        } catch (e: Exception) {
            this.insufficientPermissions()
        }
    }

    fun zoomMapToUser(){
        val zoomValue = 14f
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(userLocation.latitude, userLocation.longitude),
                zoomValue
            )
        )
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onLocationChanged(location: Location) {
        val locationLatLng = LatLng(location.latitude, location.longitude)

        val height = 128; val width = 128
        val drawable = getDrawable(R.drawable.car_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions().position(locationLatLng).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        userLocation = location
        lastUserLocMarker?.remove()
        lastUserLocMarker = googleMap.addMarker(markerOptions)!!

        this.findViewById<TextView>(R.id.dialog_city_tw).text = getCity(location, this)
    }

    fun fetchAnomalies(callback: RunnablePar, onCompleteCallback: Runnable) {
        FirebaseClass.getAnomaliesRef().get().addOnCompleteListener { task ->
            for(it : DataSnapshot in task.result.children){
                val anomaly = it.getValue(Anomaly::class.java)
                callback.run(anomaly!!)
            }

            onCompleteCallback.run()
        }
    }

    fun showAnomaly(anomaly: Anomaly) {
        val height = 64; val width = 64
        val drawable = getDrawable(R.drawable.buca_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions().position(LatLng(
            anomaly.location.latitude,
            anomaly.location.longitude
        )).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        googleMap.addMarker(markerOptions)!!
    }

    fun storeAnomaly(anomaly: Anomaly) {
        anomalies.add(anomaly)
        anomalies.sortBy{ anomaly.location.distanceTo(userLocation) }
    }

    fun listAnomalies() {
        var i = 0
        val toShow = 5
        while(i < toShow && i < anomalies.size) {
            listAnomaly(anomalies[i])
            i++
        }

        val nAnomaliesTW = findViewById<TextView>(R.id.dialog_anomaly_tw)
        val anomaliesInCity = getAnomaliesInCity(userLocation).size
        nAnomaliesTW.text = getString(R.string.buche_attive).replace("{number}", anomaliesInCity.toString())
    }

    fun listAnomaly(anomaly: Anomaly) {
        if(isInSameCity(anomaly.location))
            return

        val parentLayoutId = R.id.dialog_anomaly_layout
        val parentViewId = R.id.parent
        val layoutToAddId = R.layout.single_anomaly

        val inflater = LayoutInflater.from(this)
        val gallery: LinearLayout = findViewById(parentLayoutId)
        val view = inflater.inflate(layoutToAddId, gallery, false)
        val parentView = view.findViewById<ViewGroup>(parentViewId)

        val addressTW = view.findViewById<TextView>(R.id.single_anomaly_title)
        val reporterTW = view.findViewById<TextView>(R.id.single_anomaly_reporter)
        val distanceTW = view.findViewById<TextView>(R.id.single_anomaly_distance)

        addressTW.text = getCity(anomaly.location, this)
        reporterTW.text = reporterTW.text.toString().replace("{username}", anomaly.spotterId)
        distanceTW.text = distanceTW.text.toString().replace("{distance}", getDistanceString(userLocation, anomaly.location))

        Utility.ridimensionamento(this, parentView)
        gallery.addView(view)
    }

    fun addAnomalyMarker(latLng: LatLng){
        anomalyMarker?.remove()
        anomalyMarker = this.googleMap.addMarker(MarkerOptions().position(latLng))
    }

    fun updateAnomalyLocation(){
        val foundAddress = getAddress(anomalyMarker!!.position, this)
        val addressET = findViewById<TextInputEditText>(R.id.report_address)
        addressET.setText(foundAddress)
    }

    fun refreshMap() {
        anomalies.clear()
        googleMap.clear()
        this.setupMap()
    }

    private fun getAnomaliesInCity(location: Location) : Array<Anomaly> {
        return anomalies.filter {
            getCity(it.location, this) == getCity(location, this)
        }.toTypedArray()
    }

    private fun isInSameCity(location : Location) : Boolean{
        return getCity(userLocation, this) == getCity(location, this)
    }

    private fun getDistanceString(location1: Location, location2: Location): String {
        val distanceValue = location1.distanceTo(location2).toInt()
        return if(distanceValue >= 1000)
                "${distanceValue/1000} km"
            else
                "$distanceValue m"
    }

   private fun setupTimer() {
       val refreshTimeout = 10
       val timerTW = findViewById<TextView>(R.id.main_refresh_timer)

       refreshMapTimer = Countdown(refreshTimeout) {
           this.runOnUiThread {
                refreshMapTimer.start()
           }
       }

       refreshMapTimer.onSecondCallback = Runnable {
           this.runOnUiThread {
               timerTW.text = refreshMapTimer.getElapsedTimeString()
           }
       }

       refreshMapTimer.start()
   }

    companion object {
        private const val GEOLOCATION_PERMISSION_CODE = 1

        fun getCity(location: Location, activity: AppCompatActivity): String? {
            return Geocoder(activity, Locale.getDefault()).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )[0].locality
        }

        fun getAddress(latLng: LatLng, activity: AppCompatActivity) : String{
            val location = Location(String())
            location.latitude = latLng.latitude
            location.longitude = latLng.longitude
            return getAddress(location, activity)
        }

        fun getAddress(location: Location, activity: AppCompatActivity, ) : String{
            return Geocoder(activity, Locale.getDefault()).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )[0].getAddressLine(0)
        }
    }
}