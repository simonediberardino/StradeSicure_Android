package com.simonediberardino.stradesicure.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.drawerlayout.widget.DrawerLayout
import com.github.techisfun.android.topsheet.TopSheetBehavior
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.databinding.ActivityMapsBinding
import com.simonediberardino.stradesicure.entity.Anomaly
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.GoogleMapExtended
import com.simonediberardino.stradesicure.misc.LocationExtended
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock








class MapsActivity : AdaptedActivity(), OnMapReadyCallback, LocationListener, NavigationView.OnNavigationItemSelectedListener{
    private lateinit var googleMap: GoogleMapExtended
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var topSheetBehavior: TopSheetBehavior<View>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var anomalies: ArrayList<Anomaly>
    private var userLocation: Location? = null
    private var anomalyMarker: Marker? = null
    private var lastUserLocMarker: Marker? = null
    private var threadLocker = ReentrantLock()
    private var threadLockerCond = threadLocker.newCondition()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.checkIfLoggedIn()
    }

    override fun initializeLayout(){
        this.setContentView()
        this.setupSideMenu()
        this.setupBottomSheet()
        this.setupReportSheet()
        this.setupButtons()
    }

    private fun checkIfLoggedIn(){
        if(!LoginHandler.isLoggedIn()){
            Utility.navigateTo(this, LoginActivity::class.java)
        }
    }

    /**
     * Sets the content of the activity;
     */
    private fun setContentView(){
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Initializes the left side menu;
     */
    private fun setupSideMenu(){
        drawerLayout = findViewById(R.id.parent)
        val navigationView = findViewById<View>(R.id.main_navigation_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_mappa -> {
                setSideMenuVisibility(false)
            }

            R.id.menu_profilo -> {
                Utility.navigateTo(this,
                if(LoginHandler.isLoggedIn())
                    MyAccountActivity::class.java
                    else
                        LoginActivity::class.java)
            }

            R.id.menu_mie_segnalazioni -> {

            }

            R.id.menu_tutte_segnalazioni -> {

            }

            R.id.menu_contatti -> {

            }
        }

        return true
    }

    /**
     * Initializes the bottom menu;
     */
    private fun setupBottomSheet(){
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_persistent))
        bottomSheetBehavior.peekHeight = 350
        bottomSheetBehavior.isHideable = false
    }

    /**
     * Initializes the top menu and sets the listeners;
     */
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
        addressET.setOnClickListener{ howToReportAnomaly() }

        val statusBar = findViewById<SeekBar>(R.id.report_stato_bar)
        updateStatusDescription(statusBar.progress)

        statusBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(view: SeekBar?, value: Int, bool: Boolean) {
                updateStatusDescription(value)
            }

            override fun onStartTrackingTouch(var1: SeekBar?) {}
            override fun onStopTrackingTouch(var1: SeekBar?) {}
        })

        val confirmBtn = findViewById<View>(R.id.report_confirm)
        confirmBtn.setOnClickListener {
            if(LoginHandler.deviceUser == null){
                Utility.oneLineDialog(this, this.getString(R.string.nonloggato), null)
                return@setOnClickListener
            }

            if(userLocation == null && anomalyMarker == null){
                howToReportAnomaly()
                return@setOnClickListener
            }

            val anomalyLocation = if(anomalyMarker == null)
                    locationToLocationExtended(userLocation!!)
                else
                    latLngToLocation(anomalyMarker!!.position)

            val spotterId = LoginHandler.deviceUser!!.uniqueId
            val description = findViewById<TextInputEditText>(R.id.report_description).text.toString().trim()
            val stato = statusBar.progress

            setTopMenuVisibility(false)

            Utility.oneLineDialog(this as AppCompatActivity,
                this.getString(R.string.dialog_conferma_anomalia)
            ) {
                FirebaseClass.addAnomalyToFirebase(
                    Anomaly(
                        anomalyLocation,
                        spotterId,
                        description,
                        stato
                    )
                )

                removeAnomalyMarker()
            }
        }
    }

    private fun howToReportAnomaly(){
        Utility.showToast(this, getString(R.string.aggiungi_marker_tut))
    }

    /**
     * Updates the textview attached to the anomaly status seekbar
     */
    private fun updateStatusDescription(value: Int){
        val statusValueTT = findViewById<TextView>(R.id.report_stato_value)
        val statusArray = resources.getStringArray(R.array.stato)
        val maxIndex = statusArray.size - 1
        val index = (maxIndex * value) / 100
        statusValueTT.text = statusArray[index]
    }

    /**
     * Sets the listener of the buttons of the activity;
     */
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

    /**
     * Called when the map is ready: initializes the map, the GPS and the Firebase listener;
     */
    override fun onMapReady(googleMap: GoogleMap) {
        this.anomalies = ArrayList()
        this.googleMap = GoogleMapExtended(googleMap)

        this.googleMap.map.setOnMapLongClickListener {
            addAnomalyMarker(it)
            updateAnomalyLocation()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        this.setupMap()
        this.setupGPS()
        this.setAnomaliesListener()
    }

    /**
     * Sets up the GPS, called when the map is ready;
     */
    private fun setupGPS() {
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
                GEOLOCATION_PERMISSION_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            GEOLOCATION_PERMISSION_CODE -> {
                if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    this.insufficientPermissions()
                }
            }
        }
    }

    private fun insufficientPermissions(){
        Utility.showToast(this, getString(R.string.request_permissions))
    }

    private fun removeAnomalyMarker(){
        val addressET = findViewById<TextInputEditText>(R.id.report_address)

        addressET.setText(
            getString(
                if(userLocation != null)
                    R.string.tua_posizione
                else
                    R.string.posizionenontrovata
            )
        )

        anomalyMarker?.remove()
    }

    @SuppressLint("MissingPermission")
    private fun setupMap() {
        try {
            this.onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.5f, this)
        } catch (e: Exception) {
            this.insufficientPermissions()
        }finally {
            this.removeAnomalyMarker()

            this.googleMap.map.setOnMarkerClickListener {
                when (it) {
                    anomalyMarker -> this.removeAnomalyMarker()
                    lastUserLocMarker -> this.zoomMapToUser()
                }

                return@setOnMarkerClickListener false
            }

            fetchAnomalies(object : RunnablePar {
                override fun run(p: Any?) {
                    showAnomaly(p as Anomaly)
                    storeAnomaly(p)
                }
            }) {
                threadLocker.withLock {
                    threadLockerCond.signalAll()
                }

                listAnomalies()
            }

            this.zoomMapToUser()
        }
    }

    private fun zoomMapToUser(){
        if(userLocation == null)
            return

        val zoomValue = 14f

        googleMap.map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(userLocation!!.latitude, userLocation!!.longitude),
                zoomValue
            )
        )
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onLocationChanged(location: Location) {
        this.onLocationChanged(location as Location?)
    }

    @JvmName("onLocationChanged1")
    fun onLocationChanged(location: Location?){
        val currentCityTW = this.findViewById<TextView>(R.id.dialog_city_tw)
        if(location == null){
            currentCityTW.text = getString(R.string.citynotfound)
            return
        }

        val locationLatLng = LatLng(location.latitude, location.longitude)

        val height = 128; val width = 128
        val drawable = getDrawable(R.drawable.car_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions().position(locationLatLng).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        userLocation = location
        lastUserLocMarker?.remove()
        lastUserLocMarker = googleMap.map.addMarker(markerOptions)

        currentCityTW.text = getCity(location, this)
    }


    private fun setAnomaliesListener() {
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
                            if(index <= anomalies.size)
                                return

                            val anomalyAdded = snapshot.getValue(Anomaly::class.java)

                            showAnomaly(anomalyAdded!!)
                            storeAnomaly(anomalyAdded)
                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                            val anomalyRemoved = snapshot.getValue(Anomaly::class.java)
                            removeAnomaly(anomalyRemoved!!)
                        }

                        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                        override fun onCancelled(error: DatabaseError) {}
                    }
                )
            }
        }.start()
    }


    private fun fetchAnomalies(callback: RunnablePar, onCompleteCallback: Runnable) {
        FirebaseClass.getAnomaliesRef().get().addOnCompleteListener { task ->
            for(it : DataSnapshot in task.result.children){
                val anomaly = it.getValue(Anomaly::class.java)
                callback.run(anomaly!!)
            }

            onCompleteCallback.run()
        }
    }

    private fun removeAnomaly(anomaly: Anomaly){
        googleMap.removeMarker(anomaly.location)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showAnomaly(anomaly: Anomaly) {
        val height = 80; val width = 80
        val drawable = getDrawable(R.drawable.buca_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions().position(LatLng(
            anomaly.location.latitude,
            anomaly.location.longitude
        )).icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))

        googleMap.addMarker(markerOptions)
    }

    private fun storeAnomaly(anomaly: Anomaly) {
        anomalies.add(anomaly)

        if(userLocation != null)
            anomalies.sortBy{ anomaly.location.distanceTo(userLocation) }
    }

    private fun listAnomalies() {
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

    private fun listAnomaly(anomaly: Anomaly) {
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

    private fun addAnomalyMarker(latLng: LatLng){
        anomalyMarker?.remove()
        anomalyMarker = this.googleMap.map.addMarker(MarkerOptions().position(latLng))
    }

    private fun updateAnomalyLocation(){
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

    private fun closeMenus() : Boolean {
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

    private fun refreshMap() {}

    private fun getAnomaliesInCity(location: Location?) : Array<Anomaly> {
        if(location == null)
            return emptyArray()

        return anomalies.filter {
            getCity(it.location, this) == getCity(location, this)
        }.toTypedArray()
    }

    private fun isInSameCity(location : Location) : Boolean{
        return getCity(userLocation, this) == getCity(location, this)
    }

    private fun getDistanceString(location1: Location?, location2: Location?): String {
        if(location1 == null || location2 == null)
            return getString(R.string.posizionenontrovata)

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

    companion object {
        private const val GEOLOCATION_PERMISSION_CODE = 1

        fun getCity(location: Location?, activity: AppCompatActivity): String? {
            if(location == null)
                return activity.getString(R.string.citynotfound)

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

        fun locationToLocationExtended(location: Location): LocationExtended {
            val locationExtended = LocationExtended()
            locationExtended.latitude = location.latitude
            locationExtended.longitude = location.longitude
            return locationExtended
        }
    }
}