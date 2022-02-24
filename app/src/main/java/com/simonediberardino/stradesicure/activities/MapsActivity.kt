package com.simonediberardino.stradesicure.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.Profile
import com.github.techisfun.android.topsheet.TopSheetBehavior
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.CBottomSheetDialog
import com.simonediberardino.stradesicure.databinding.ActivityMapsBinding
import com.simonediberardino.stradesicure.entity.*
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.GoogleMapExtended
import com.simonediberardino.stradesicure.misc.LocationExtended
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData
import com.simonediberardino.stradesicure.utils.Utility
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate
import kotlin.collections.ArrayList
import kotlin.concurrent.withLock

class MapsActivity : SSActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener{
    /** Google map object provided by Google; */
    internal var googleMap: GoogleMapExtended? = null
    internal lateinit var mapBinding: ActivityMapsBinding

    /** Wheter markers are shown or not; */
    internal var areMarkerShown = true
    /** Exact user location; */
    internal var userLocation: Location? = null
    /** User location updated every 100 meters; */
    internal var intervalUserLocation: Location? = null
    /** Anomaly marker placed by the user; */
    internal var anomalyMarker: Marker? = null
    /** Location icon placed on the user location updated every time the user location changes; */
    internal var userLocMarker: Marker? = null
    /** Circle placed on the user location updated every time the user location changes; */
    internal var userLocCircle: Circle? = null
    /** Locks the FireBase query thread until the map is fully initializated; */
    internal var threadLocker = ReentrantLock()
    internal var threadLockerCond = threadLocker.newCondition()
    internal var hasListedAnomalies = false
    /** GPS Manager; */
    internal lateinit var locationManager: FusedLocationProviderClient
    /** User Interface objects; */
    internal lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    internal lateinit var topSheetBehavior: TopSheetBehavior<View>
    internal lateinit var refreshLayout: SwipeRefreshLayout
    internal lateinit var drawerLayout: DrawerLayout
    /** ArrayList containing all the anomalies reported by the users; */
    internal lateinit var anomalies: ArrayList<Anomaly>
    /** ArrayList containing all the anomalies not reported by the voice assistant; */
    internal lateinit var notEncounteredAnomalies: ArrayList<Anomaly>
    /** Voice assistant; */
    internal lateinit var TTS: TTS

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        mapsActivity = this
        super.onCreate(savedInstanceState)
        this.setupTTS()
        this.fetchUserData()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun initializeLayout(){
        this.setContentView()
        this.setupSideMenu()
        this.setupBottomSheet()
        this.setupReportSheet()
        this.setupButtons()
    }

    private fun setupTTS(){
        TTS = TTS(this){}
    }

    private fun fetchUserData() {
        if(LoginHandler.deviceUser != null)
            return

        if(LoginHandler.isFacebookLoggedIn()){
            LoginHandler.waittilFBProfileIsReady(object : RunnablePar{
                override fun run(p: Any?) {
                    val userId = (p as Profile).id
                    FirebaseClass.getUserObjectById<FbUser>(
                        userId,
                        object : RunnablePar {
                            override fun run(p: Any?) {
                                val fbUser = p as FbUser?
                                if(fbUser != null)
                                    LoginHandler.doLogin(fbUser)
                            }
                        }
                    )
                }

            })
        }else{
            val storedAccount = ApplicationData.getSavedAccount<EmailUser>() ?: return
            FirebaseClass.getUserObjectById<EmailUser>(
                storedAccount.uniqueId,
                object : RunnablePar{
                    override fun run(p: Any?) {
                        val retrievedUser = p as EmailUser?
                        val passwordOnDatabase = retrievedUser?.password
                        val passwordOnDevice = storedAccount.password

                        if(passwordOnDatabase == passwordOnDevice){
                            LoginHandler.doLogin(retrievedUser)
                        }else{
                            Utility.showToast(this@MapsActivity, getString(R.string.erroreprofilo))
                            LoginHandler.doLogout()
                        }
                    }
            })
        }
    }

    /**
     * Sets the content of the activity;
     */
    override fun setContentView(){
        mapBinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(mapBinding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Initializes the left side menu;
     */
    private fun setupSideMenu(){
        drawerLayout = findViewById(R.id.main_drawerLayout)
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

            R.id.menu_segnalazioni -> {
                Utility.navigateTo(this, AnomaliesActivity::class.java)
            }

            R.id.menu_contatti -> {}

            R.id.menu_impostazioni -> {
                Utility.navigateTo(this, SettingsActivity::class.java)
            }
        }

        return true
    }

    /**
     * Initializes the bottom menu;
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupBottomSheet(){
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet_persistent))
        bottomSheetBehavior.peekHeight = 400
        bottomSheetBehavior.isHideable = false

        refreshLayout = findViewById(R.id.dialog_refresh_layout)
        refreshLayout.setOnRefreshListener {
            this.listAnomalies()
            refreshLayout.isRefreshing = false
        }
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
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onMapReady(googleMap: GoogleMap) {
        this.anomalies = ArrayList()
        this.notEncounteredAnomalies = ArrayList()
        this.googleMap = GoogleMapExtended(googleMap)
        this.googleMap!!.map.mapType = ApplicationData.getSavedMapStyle()

        this.setupGPS()
        this.setAnomaliesListener()
    }

    override fun onResume() {
        super.onResume()
        if(this.googleMap != null)
            this.googleMap!!.map.mapType = ApplicationData.getSavedMapStyle()
    }

    /**
     * Sets up the GPS, called when the map is ready;
     */
    @RequiresApi(Build.VERSION_CODES.N)
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
        }else{
            this.setupUserLocation()
            this.setupMap()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            GEOLOCATION_PERMISSION_CODE -> {
                if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    this.insufficientPermissions()
                }else{
                    this.setupUserLocation()
                }
                this.setupMap()
            }
        }
    }

    private fun howToReportAnomaly(){
        Utility.showToast(this, getString(R.string.aggiungi_marker_tut))
    }

    private fun getStatusDescByValue(value: Int): String {
        val statusArray = resources.getStringArray(R.array.stato)
        val maxIndex = statusArray.size - 1
        val index = (maxIndex * value) / 100
        return statusArray[index]
    }
    /**
     * Updates the textview attached to the anomaly status seekbar
     */
    private fun updateStatusDescription(value: Int){
        val statusValueTT = findViewById<TextView>(R.id.report_stato_value)
        statusValueTT.text = getStatusDescByValue(value)
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

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    private fun setupUserLocation(){
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                this@MapsActivity.onLocationChanged(locationResult?.lastLocation)
            }
        }

        locationManager = LocationServices.getFusedLocationProviderClient(this)
        locationManager.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        locationManager.lastLocation.addOnSuccessListener(this) {
                this.onLocationChanged(it)
                this.removeAnomalyMarker()
                this.zoomMapToUser()
            }
            .addOnFailureListener(this) {
                this.insufficientPermissions()
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupMap() {
        this.googleMap?.map?.setOnMarkerClickListener {
            when (it) {
                anomalyMarker -> this.removeAnomalyMarker()
                userLocMarker -> this.zoomMapToUser()
                /* Else it must be an anomaly */
                else -> {
                    val findAnomalyByMarker = getAnomalyByMarker(it)
                    if (findAnomalyByMarker != null) {
                        setupMoreAnomalyDialog(this, findAnomalyByMarker)
                    }
                }
            }

            return@setOnMarkerClickListener true
        }

        this.googleMap?.map?.setOnMapLongClickListener {
            addAnomalyMarker(it)
            updateAnomalyLocation()
        }

        this.googleMap?.map?.setOnCameraChangeListener {
            val minMarkerZoom = 11
            if(it.zoom < minMarkerZoom){
                if(areMarkerShown)
                    setMarkersVisibility(false)
            }else{
                if(!areMarkerShown)
                    setMarkersVisibility(true)
            }

            refreshLocationCircle()
        }

        fetchAnomalies(object : RunnablePar {
            override fun run(p: Any?) {
                storeAnomaly(p as Anomaly)
                showAnomaly(p)
            }
        }) {
            threadLocker.withLock {
                threadLockerCond.signalAll()
                this.firstAnomalyList()
            }
        }
    }

    private fun getMapZoom(): Float {
        return googleMap!!.map.cameraPosition.zoom
    }

    private fun zoomMapToUser(){
        if(userLocation == null)
            return

        googleMap?.map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(userLocation!!.latitude, userLocation!!.longitude),
                MAP_DEFAULT_ZOOM
            )
        )
    }

    private fun setMarkersVisibility(flag: Boolean){
        this.areMarkerShown = flag
        this.googleMap?.markers?.forEach { it.isVisible = flag }
    }

    private fun refreshLocationCircle(){
        userLocCircle?.isVisible = getMapZoom() >= MAP_DEFAULT_ZOOM
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onLocationChanged(newLocation: Location?){
        this.firstAnomalyList()

        val updateDistance = 100
        val currentCityTW = this.findViewById<TextView>(R.id.dialog_city_tw)

        if(newLocation == null){
            currentCityTW.text = getString(R.string.citynotfound)
            return
        }

        val locationLatLng = LatLng(newLocation.latitude, newLocation.longitude)

        val height = 33; val width = 33
        val drawable = getDrawable(R.drawable.location_icon)
        val bitmap = drawable?.toBitmap(width, height)

        val markerOptions = MarkerOptions()
            .position(locationLatLng)
            .icon(BitmapDescriptorFactory
                .fromBitmap(bitmap!!))

        val circleOptions = CircleOptions()
            .center(markerOptions.position)
            .radius(WARN_RADIUS)
            .strokeColor(0xf1E90FF)
            .fillColor(0x301E90FF)
            .strokeWidth(10f)

        if(userLocation != null){
            if(intervalUserLocation == null || userLocation!!.distanceTo(intervalUserLocation) >= updateDistance){
                intervalUserLocation = userLocation
                checkNearbyAnomalies()
            }
        }

        userLocation = newLocation
        userLocCircle?.remove()
        userLocMarker?.remove()
        userLocMarker = googleMap?.map?.addMarker(markerOptions)
        userLocCircle = googleMap?.map?.addCircle(circleOptions)

        refreshLocationCircle()

        currentCityTW.text = getCity(userLocation, this)
    }

    private fun checkNearbyAnomalies(){
        val nearestAnomaly = getNearestNotWarnedAnomaly() ?: return

        if(nearestAnomaly.location.distanceTo(userLocation) < WARN_RADIUS)
            warnAnomaly(nearestAnomaly)
    }

    private fun getNearestNotWarnedAnomaly(): Anomaly? {
        return if(notEncounteredAnomalies.size > 0)
            notEncounteredAnomalies.sortedBy { it.location.distanceTo(userLocation) }[0]
        else null
    }

    private fun warnAnomaly(anomaly: Anomaly){
        notEncounteredAnomalies.remove(anomaly)
        val distanceInMeters = anomaly.location.distanceTo(userLocation).toInt()
        val distanceInMetersApprox = ((distanceInMeters/10.0f).toInt())*10
        val messageToSay = getString(R.string.anomalia_in_range)
            .replace("{meters}", distanceInMetersApprox.toString())
                .replace("{value}", getStatusDescByValue(anomaly.stato).lowercase())

        TTS.speak(messageToSay)
        Utility.showToast(this, messageToSay)
    }

    private fun setAnomaliesListener() {
        /**
            Locks the thread until the first fetch is done, then starts listening for new anomalies
        */
        Thread{
            threadLocker.withLock {
                threadLockerCond.await()

                var index = 0
                FirebaseClass.anomaliesRef.addChildEventListener(
                    object : ChildEventListener {
                        @RequiresApi(Build.VERSION_CODES.N)
                        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                            index++
                            if(index <= anomalies.size)
                                return

                            val anomalyAdded = snapshot.getValue(Anomaly::class.java)

                            showAnomaly(anomalyAdded!!)
                            storeAnomaly(anomalyAdded)
                            if(isInSameCity(anomalyAdded.location))
                                listAnomaliesIfRealtimeUpdated()
                        }

                        @RequiresApi(Build.VERSION_CODES.N)
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
        FirebaseClass.anomaliesRef.get().addOnCompleteListener { task ->
            for(it : DataSnapshot in task.result.children){
                val anomaly = it.getValue(Anomaly::class.java)
                callback.run(anomaly!!)
            }

            onCompleteCallback.run()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun removeAnomaly(anomaly: Anomaly?){
        if(anomaly?.location == null)
            return

        val predicate = Predicate<Anomaly>{ it.location == anomaly.location }
        anomalies.removeIf(predicate)
        notEncounteredAnomalies.removeIf(predicate)
        googleMap?.removeMarker(anomaly.location)

        if(isInSameCity(anomaly.location))
            listAnomaliesIfRealtimeUpdated()
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

        markerOptions.visible(areMarkerShown)

        googleMap?.addMarker(markerOptions)
    }

    private fun storeAnomaly(anomaly: Anomaly) {
        anomalies.add(anomaly)
        notEncounteredAnomalies.add(anomaly)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun listAnomaliesIfRealtimeUpdated(){
        if(ApplicationData.isRealtimeUpdated())
            listAnomalies()
    }

    fun sortByDistance(array: Array<Anomaly>){
        array.sortBy { it.location.distanceTo(userLocation) }
    }

    fun sortByDistance(arrayList: ArrayList<Anomaly>){
        sortByDistance(arrayList.toTypedArray())
    }

    fun sortAnomalies(){
        sortByDistance(anomalies)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun listAnomalies() {
        val parentLayoutId = R.id.dialog_anomaly_layout

        val anomaliesContainer = findViewById<LinearLayout>(parentLayoutId)
        anomaliesContainer.removeAllViews()

        val nAnomaliesTW = findViewById<TextView>(R.id.dialog_anomaly_tw)
        val anomaliesInCity = getAnomaliesInCity(userLocation)

        sortAnomalies()

        if(anomaliesInCity.isEmpty()){
            nAnomaliesTW.text = getString(R.string.nessunaanomalia)
            return
        }

        val parentViewId = R.id.parent
        val layoutToAddId = R.layout.single_anomaly

        var i = 0
        val toShow = 5
        while(i < toShow && i < anomaliesInCity.size) {
            listAnomaly(this, parentLayoutId, parentViewId, layoutToAddId, anomaliesInCity[i])
            i++
        }

        nAnomaliesTW.text = getString(R.string.anomalie_citta).replace("{number}", anomaliesInCity.size.toString())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun firstAnomalyList(){
        if(!hasListedAnomalies && userLocation != null) {
            this.hasListedAnomalies = true
            this.listAnomalies()
        }
    }

    private fun addAnomalyMarker(latLng: LatLng){
        anomalyMarker?.remove()
        anomalyMarker = this.googleMap?.map?.addMarker(MarkerOptions().position(latLng))
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

    private fun getAnomalyByMarker(marker: Marker): Anomaly? {
        return try{
            val markerLocation = latLngToLocation(marker.position)
            this.anomalies.first { it.location == markerLocation }
        }catch(exception: NoSuchElementException){
            null
        }
    }

    internal fun getAnomaliesInCity(location: Location?) : Array<Anomaly> {
        if(location == null)
            return emptyArray()

        return anomalies.filter {
            isInSameCity(it.location)
        }.toTypedArray()
    }

    internal fun isInSameCity(location : Location) : Boolean{
        return getCity(userLocation, this) == getCity(location, this)
    }

    override fun onBackPressed() {
        if(!closeMenus())
            super.onBackPressed()
    }

    companion object {
        lateinit var mapsActivity: MapsActivity
        private const val MAP_DEFAULT_ZOOM = 15f
        private const val WARN_RADIUS = 200.0
        private const val GEOLOCATION_PERMISSION_CODE = 1

        @RequiresApi(Build.VERSION_CODES.N)
        internal fun listAnomaly(activity: AppCompatActivity, parentLayoudId: Int, parentViewId: Int, layoutToAddId: Int, anomaly: Anomaly) {
            val inflater = LayoutInflater.from(activity)
            val gallery: LinearLayout = activity.findViewById(parentLayoudId)
            val view = inflater.inflate(layoutToAddId, gallery, false)
            val parentView = view.findViewById<ViewGroup>(parentViewId)

            val addressTW = view.findViewById<TextView>(R.id.single_anomaly_title)
            val reporterTW = view.findViewById<TextView>(R.id.single_anomaly_reporter)
            val distanceTW = view.findViewById<TextView>(R.id.single_anomaly_distance)
            val moreBTN = view.findViewById<View>(R.id.single_anomaly_more)

            addressTW.text = getCity(anomaly.location, activity)

            val distanceTemplate = activity.getString(R.string.distance)
            distanceTW.text = distanceTemplate.replace("{distance}", getDistanceString(activity,
                mapsActivity.userLocation, anomaly.location))

            FirebaseClass.getUserObjectById<User>(
                anomaly.spotterId,
                object : RunnablePar {
                    override fun run(p: Any?) {
                        val user = p as User?
                        if(p == null) return
                        val reporterTemplate = activity.getString(R.string.segnalata_da)
                        reporterTW.text = reporterTemplate.replace("{username}", LoginHandler.getFullName(user))
                    }
                })

            moreBTN.setOnClickListener {
                setupMoreAnomalyDialog(activity, anomaly)
            }

            Utility.ridimensionamento(activity, parentView)
            gallery.addView(view)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun setupMoreAnomalyDialog(activity: AppCompatActivity, anomaly: Anomaly){
            val moreDialog = CBottomSheetDialog(activity)
            moreDialog.setContentView(R.layout.anomaly_more_dialog)

            val streetBtn = moreDialog.findViewById<View>(R.id.anomaly_more_map_btn)
            streetBtn?.setOnClickListener {
                val streetViewURI = Uri.parse("google.streetview:cbll=${anomaly.location.latitude},${anomaly.location.longitude}")
                val streetViewIntent = Intent (Intent.ACTION_VIEW, streetViewURI)
                streetViewIntent.setPackage ("com.google.android.apps.maps")
                activity.startActivity(streetViewIntent)
            }

            val removeBtn = moreDialog.findViewById<View>(R.id.anomaly_more_remove_btn)
            removeBtn?.setOnClickListener {
                if(mapsActivity.notEncounteredAnomalies.contains(anomaly)){
                    Utility.oneLineDialog(activity, activity.getString(R.string.anomalia_non_visitata), null)
                }else{
                    Utility.oneLineDialog(activity, activity.getString(R.string.dialog_conferma_eliminazione)) {
                        FirebaseClass.deleteAnomalyFirebase(anomaly)
                    }
                }
                moreDialog.dismiss()
            }

            moreDialog.show()
        }

        fun getCity(location: Location?, activity: AppCompatActivity): String? {
            if(location == null)
                return activity.getString(R.string.citynotfound)

            val city = Geocoder(activity, Locale.getDefault()).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )[0].locality

            return if(city == null || city.trim().isEmpty())
                return activity.getString(R.string.citynotfound)
            else city
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

        private fun getDistanceString(activity: AppCompatActivity, location1: Location?, location2: Location?): String {
            if(location1 == null || location2 == null)
                return activity.getString(R.string.posizionenontrovata)

            val distanceInMeters = location1.distanceTo(location2)
            val distanceInKm = getDistanceInKm(distanceInMeters.toInt())

            return if(distanceInMeters.toInt() == distanceInKm.toInt())
                "${distanceInMeters.toInt()} m"
            else "$distanceInKm km"
        }

        /**
         * @return the distance in km if it's greater than 1000m, else the distance in meters;
         */
        private fun getDistanceInKm(distanceValue: Int): Float {
            return if(distanceValue >= 1000){
                ((distanceValue/100).toFloat()/10)
            }else{
                distanceValue.toFloat()
            }
        }
    }
}