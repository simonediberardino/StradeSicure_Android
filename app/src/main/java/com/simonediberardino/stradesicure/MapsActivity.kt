package com.simonediberardino.stradesicure

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle

import com.google.android.gms.maps.GoogleMap
import com.simonediberardino.stradesicure.databinding.ActivityMapsBinding
import android.location.*

import android.os.Build
import android.view.*
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.*
import androidx.appcompat.app.ActionBarDrawerToggle
import com.github.techisfun.android.topsheet.TopSheetBehavior
import kotlin.collections.ArrayList

import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import android.content.IntentFilter





class MapsActivity : AdaptedActivity(), OnMapReadyCallback, LocationListener {
    private lateinit var googleMap: GoogleMapExtended
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var userLocation: Location
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var topSheetBehavior: TopSheetBehavior<View>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var anomalies: ArrayList<Anomaly>
    private var anomalyMarker: Marker? = null
    private var lastUserLocMarker: Marker? = null
    private var threadLocker = ReentrantLock()
    private var threadLockerCond = threadLocker.newCondition()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        this.registerReceiver(NetworkStatusListener(), intentFilter)

        this.initializeLayout()
        super.onPageLoaded()
    }

    fun initializeLayout(){
        this.setupSideMenu()
        this.setupBottomSheet()
        this.setupReportSheet()
        this.setupButtons()
    }

    private fun setupSideMenu(){
        drawerLayout = findViewById(R.id.parent)
    }

    private fun setupBottomSheet(){
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_persistent))
        bottomSheetBehavior.peekHeight = 350
        bottomSheetBehavior.isHideable = false
    }

    private fun setupReportSheet(){
        val topSheet = findViewById<View>(R.id.top_sheet_persistent)

        topSheetBehavior = TopSheetBehavior.from(topSheet)
        topSheetBehavior.peekHeight = 90
        topSheetBehavior.isHideable = false

        setTopMenuVisibility(false)

        val backBTN = findViewById<View>(R.id.report_back)
        backBTN.setOnClickListener{
            setTopMenuVisibility(false)
        }

        val addressET = findViewById<TextInputEditText>(R.id.report_address)
        addressET.setOnClickListener{
            Toast.makeText(
                this,
                getString(R.string.aggiungi_marker_tut),
                Toast.LENGTH_LONG
            ).show()
        }

        val statoBar = findViewById<SeekBar>(R.id.report_stato_bar)
        updateStatoDescription(statoBar.progress)

        statoBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(view: SeekBar?, value: Int, bool: Boolean) {
                updateStatoDescription(value)
            }

            override fun onStartTrackingTouch(var1: SeekBar?) {}
            override fun onStopTrackingTouch(var1: SeekBar?) {}
        })

        val confirmBtn = findViewById<View>(R.id.report_confirm)
        confirmBtn.setOnClickListener {
            val anomalyLocation = latLngToLocation(
                    (if(anomalyMarker == null)
                        lastUserLocMarker?.position
                    else
                        anomalyMarker!!.position)!!
                )

            val spotterId = "-1"
            val description = findViewById<TextInputEditText>(R.id.report_description).text.toString().trim()
            val stato = statoBar.progress

            FirebaseClass.addAnomalyToFirebase(Anomaly(
                anomalyLocation,
                spotterId,
                description,
                stato
            ))

            removeAnomalyMarker()
        }
    }

    fun updateStatoDescription(value: Int){
        val statoValueTT = findViewById<TextView>(R.id.report_stato_value)
        val statoArray = resources.getStringArray(R.array.stato)
        val maxIndex = statoArray.size - 1
        val index = (maxIndex * value) / 100
        statoValueTT.text = statoArray[index]
    }

    @SuppressLint("RtlHardcoded")
    private fun setupButtons(){
        val reportBTN = findViewById<View>(R.id.dialog_report_btn)

        reportBTN.setOnClickListener {
            setTopMenuVisibility(
                !isTopMenuShown()
            )
        }

        val resetLocationBTN = findViewById<View>(R.id.main_my_location)
        resetLocationBTN.setOnClickListener{
            if(!isTopMenuShown())
                this.zoomMapToUser()
        }

        val showSideMenuBTN = findViewById<View>(R.id.main_toggle_menu)
        showSideMenuBTN.setOnClickListener {
            if(!isTopMenuShown()){
                setSideMenuVisibility(true)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.anomalies = ArrayList()
        this.googleMap = GoogleMapExtended(googleMap)

        this.googleMap.map.setOnMapLongClickListener {
            addAnomalyMarker(it)
            updateAnomalyLocation()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        this.setupGPS()
        this.setAnomaliesListener()
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
        Utility.showToast(this, getString(R.string.request_permissions))
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

            this.googleMap.map.setOnMarkerClickListener {
                when (it) {
                    anomalyMarker -> this.removeAnomalyMarker()
                    lastUserLocMarker -> this.zoomMapToUser()
                }

                return@setOnMarkerClickListener false
            }

            this.fetchAnomalies(object : RunnablePar {
                override fun run(any: Any) {
                    showAnomaly(any as Anomaly)
                    storeAnomaly(any)
                }
            }) {
                threadLocker.withLock {
                    threadLockerCond.signalAll()
                }

                listAnomalies()
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 2f, this)
        } catch (e: Exception) {
            this.insufficientPermissions()
        }finally {
            this.zoomMapToUser()
        }
    }

    fun zoomMapToUser(){
        val zoomValue = 14f

        googleMap.map.animateCamera(
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
        lastUserLocMarker = googleMap.map.addMarker(markerOptions)

        this.findViewById<TextView>(R.id.dialog_city_tw).text = getCity(location, this)
    }

    fun setAnomaliesListener() {
        /**
            Locks the thread until the first fetch is done, then starts listening for new anomalies
        */
        Thread{
            threadLocker.withLock {
                threadLockerCond.await()

                var index = 0
                FirebaseClass.getAnomaliesRef().addChildEventListener(
                    object : ChildEventListener {
                        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                            index++
                            if(index <= anomalies.size) {
                                return
                            }

                            val anomalyAdded = snapshot.getValue(Anomaly::class.java)

                            showAnomaly(anomalyAdded!!)
                            storeAnomaly(anomalyAdded)
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                            val anomalyRemoved = snapshot.getValue(Anomaly::class.java)
                            removeAnomaly(anomalyRemoved!!)
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                        }

                        override fun onCancelled(error: DatabaseError) {}

                    }
                )
            }
        }.start()
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

    fun removeAnomaly(anomaly: Anomaly){
        googleMap.removeMarker(anomaly.location)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun showAnomaly(anomaly: Anomaly) {
        val height = 80; val width = 80
        val drawable = getDrawable(R.drawable.buca_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions().position(LatLng(
            anomaly.location.latitude,
            anomaly.location.longitude
        )).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        googleMap.addMarker(markerOptions)
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
        anomalyMarker = this.googleMap.map.addMarker(MarkerOptions().position(latLng))
    }

    fun updateAnomalyLocation(){
        val foundAddress = getAddress(anomalyMarker!!.position, this)
        val addressET = findViewById<TextInputEditText>(R.id.report_address)
        addressET.setText(foundAddress)
    }

    private fun setSideMenuVisibility(flag: Boolean) {
        if(flag){
            drawerLayout.openDrawer(Gravity.LEFT)
        }else{
            drawerLayout.closeDrawer(Gravity.LEFT)
        }
    }

    private fun isSideMenuShown(): Boolean {
        return drawerLayout.isDrawerOpen(Gravity.LEFT)
    }

    private fun setTopMenuVisibility(flag: Boolean){
        topSheetBehavior.state = if(flag){
            TopSheetBehavior.STATE_EXPANDED
        }else{
            TopSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun isTopMenuShown(): Boolean {
        return topSheetBehavior.state == TopSheetBehavior.STATE_EXPANDED
    }

    private fun setBottomMenuVisibility(flag: Boolean){
        bottomSheetBehavior.state = if(flag){
            BottomSheetBehavior.STATE_EXPANDED
        }else{
            BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun isBottomMenuShown(): Boolean{
        return bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
    }

    fun closeMenus() : Boolean {
        return when {
            isSideMenuShown() -> {
                setSideMenuVisibility(false)
                true
            }
            isTopMenuShown() -> {
                setTopMenuVisibility(false)
                true
            }
            isBottomMenuShown() -> {
                setBottomMenuVisibility(false)
                true
            }
            else -> {
                false
            }
        }
    }

    fun refreshMap() {
        anomalies.clear()
        googleMap.map.clear()
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

    override fun onBackPressed() {
        if(!closeMenus())
            super.onBackPressed()
    }

   @SuppressLint("UseCompatLoadingForDrawables")
   private fun setupTimer() {
/*       val refreshTimeout = 10
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

       refreshMapTimer.start()*/
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

        fun getAddress(location: Location, activity: AppCompatActivity) : String{
            return Geocoder(activity, Locale.getDefault()).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )[0].getAddressLine(0)
        }

        fun latLngToLocation(latLng: LatLng): LocationExtended {
            val location = LocationExtended()
            location.latitude = latLng.latitude
            location.longitude = latLng.longitude
            return location
        }
    }
}