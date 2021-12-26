package com.simonediberardino.stradesicure

import android.Manifest
import android.annotation.SuppressLint
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
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.collections.ArrayList


class MapsActivity : AdaptedActivity(), OnMapReadyCallback, LocationListener {
    private lateinit var googleMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private var lastMarker: Marker? = null
    private var anomaly: ArrayList<Anomaly> = ArrayList()

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
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        //this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_night));

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        this.setupMarker()
    }

    fun setupMarker() {
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
                1001
            )

            return
        }

        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lastLocation?.latitude!!, lastLocation.longitude),
                12.0f
            )
        )

        this.onLocationChanged(lastLocation)

        addAnomaly(lastLocation)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.01f, this)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onLocationChanged(location: Location) {
        val locationLatLng = LatLng(location.latitude, location.longitude)

        val drawable = getDrawable(R.drawable.car_icon)
        val bitmap = drawable?.toBitmap(128, 128)

        val markerOptions = MarkerOptions().position(locationLatLng).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        lastMarker?.remove()
        lastMarker = googleMap.addMarker(markerOptions)!!

        this.findViewById<TextView>(R.id.dialog_city_tw).text = getCity(location)
    }

    fun addAnomaly(location: Location){
        val height = 64; val width = 64

        val drawable = getDrawable(R.drawable.buca_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions().position(LatLng(location.latitude, location.longitude)).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))
        val anomalyObj = Anomaly(location, -1)

        googleMap.addMarker(markerOptions)!!
        anomaly.add(anomalyObj)

        val parentLayoutId = R.id.dialog_anomaly_layout
        val parentViewId = R.id.parent
        val layoutToAddId = R.layout.single_anomaly

        val inflater = LayoutInflater.from(this)
        val gallery: LinearLayout = findViewById(parentLayoutId)
        val view = inflater.inflate(layoutToAddId, gallery, false)
        val parentView = view.findViewById<ViewGroup>(parentViewId)

        val addressTW = view.findViewById<TextView>(R.id.single_anomaly_title)
        val reporterTW = view.findViewById<TextView>(R.id.single_anomaly_reporter)
        val dateTW = view.findViewById<TextView>(R.id.single_anomaly_date)
        val indicationsTW = view.findViewById<TextView>(R.id.single_anomaly_indications)

        addressTW.text = getCity(anomalyObj.location)
        reporterTW.text = reporterTW.text.toString().replace("{username}", anomalyObj.spotterId.toString())
        dateTW.text = Date().day.toString()

        Utility.ridimensionamento(this, parentView)
        gallery.addView(view)
    }

    fun refreshMap(){
        anomaly.clear()
        googleMap.clear()
        this.setupMarker()
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
}
