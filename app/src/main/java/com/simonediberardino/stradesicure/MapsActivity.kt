package com.simonediberardino.stradesicure

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import kotlin.collections.ArrayList


class MapsActivity : AdaptedActivity(), OnMapReadyCallback, LocationListener {
    companion object {
        private val GEOLOCATION_PERMISSION_CODE = 1
    }

    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var userLocation: Location
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var topSheetBehavior: TopSheetBehavior<View>
    private lateinit var refreshMapTimer: Countdown
    private var lastMarker: Marker? = null
    private var anomalies: ArrayList<Anomaly> = ArrayList()

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
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false

        val bottomSheet = findViewById<View>(R.id.top_sheet_persistent)
        topSheetBehavior = TopSheetBehavior.from(bottomSheet)
        topSheetBehavior.state = TopSheetBehavior.STATE_HIDDEN

        val reportBTN = findViewById<View>(R.id.dialog_report_btn)

        reportBTN.setOnClickListener{
            topSheetBehavior.state =
                if(topSheetBehavior.state == TopSheetBehavior.STATE_EXPANDED)
                    TopSheetBehavior.STATE_COLLAPSED
                else
                    TopSheetBehavior.STATE_EXPANDED

        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        //this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_night));

        this.googleMap.setOnMapClickListener {
            topSheetBehavior.state = TopSheetBehavior.STATE_COLLAPSED
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
                Companion.GEOLOCATION_PERMISSION_CODE
            )

            return
        }else{
            this.setupMap()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            Companion.GEOLOCATION_PERMISSION_CODE -> {
                if( grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    this.setupMap()
                }else{
                    Utility.showDialog(this, getString(R.string.request_permissions))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun setupMap(){
        this.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)!!)

        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(userLocation.latitude, userLocation.longitude),
                12.0f
            )
        )

        this.fetchAnomalies(object : RunnablePar {
            override fun run(any: Any) {
                showAnomaly(any as Anomaly)
                storeAnomaly(any)
            }
        }) {
            listAnomalies()
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 2f, this)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onLocationChanged(location: Location) {
        val locationLatLng = LatLng(location.latitude, location.longitude)

        val drawable = getDrawable(R.drawable.car_icon)
        val bitmap = drawable?.toBitmap(128, 128)

        val markerOptions = MarkerOptions().position(locationLatLng).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        userLocation = location
        lastMarker?.remove()
        lastMarker = googleMap.addMarker(markerOptions)!!

        this.findViewById<TextView>(R.id.dialog_city_tw).text = getCity(location)
    }

    fun fetchAnomalies(callback: RunnablePar, onCompleteCallback: Runnable){
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

    fun storeAnomaly(anomaly: Anomaly){
        anomalies.add(anomaly)
        anomalies.sortBy {anomaly.location.distanceTo(userLocation)}
    }

    fun listAnomalies(){
        var i = 0
        val toShow = 5
        while(i < toShow && i < anomalies.size) {
            listAnomaly(anomalies[i])
            i++
        }
    }

    fun listAnomaly(anomaly: Anomaly){
        if(anomaly.location.distanceTo(userLocation) > 15 * 1000)
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

        addressTW.text = getCity(anomaly.location)
        reporterTW.text = reporterTW.text.toString().replace("{username}", anomaly.spotterId)
        distanceTW.text = distanceTW.text.toString().replace("{distance}", getDistanceString(userLocation, anomaly.location))

        Utility.ridimensionamento(this, parentView)
        gallery.addView(view)
    }

    fun refreshMap(){
        anomalies.clear()
        googleMap.clear()
        this.setupMap()
    }

    fun getCity(location: Location): String? {
        val gcd = Geocoder(this, Locale.getDefault())
        val addresses = gcd.getFromLocation(location.latitude, location.longitude, 1)
        return addresses[0].locality
    }

    fun getAnomaliesInCity(location: Location){
        val currentCity = getCity(location)
        // ...
    }

    fun reportDialog(){
        val dialog = TopSheetDialog(this)
        dialog.setContentView(R.layout.report_dialog)
        dialog.show()
    }

    fun getDistanceString(location1: Location, location2: Location): String {
        val distanceValue = location1.distanceTo(location2).toInt()
        return if(distanceValue >= 1000)
                "${distanceValue/1000} km"
            else
                "$distanceValue m"
    }

   fun setupTimer(){
       val refreshTimeout = 5 * 60
       val timerTW = findViewById<TextView>(R.id.main_refresh_timer)

       refreshMapTimer = Countdown(refreshTimeout) {
           this.runOnUiThread {
                this.refreshMap()
           }
       }

       refreshMapTimer.onSecondCallback = Runnable {
           this.runOnUiThread {
               timerTW.text = refreshMapTimer.getElapsedTimeString()
           }
       }

       refreshMapTimer.start()
   }
}