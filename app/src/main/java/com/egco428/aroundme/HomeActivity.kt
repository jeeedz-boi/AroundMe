package com.egco428.aroundme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.egco428.aroundme.model.UserInfo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
//  define varible needed for google maps fragment
    private lateinit var mMap: GoogleMap
    private lateinit var locationManger: LocationManager
    private lateinit var locationListener: LocationListener
    private var prevLatLng: LatLng = LatLng(0.0, 0.0)
    private var currentLatLng: LatLng = LatLng(0.1, 0.0)
    private var mAuth: FirebaseAuth? = null
    private lateinit var dataReference: FirebaseFirestore
    private lateinit var user: FirebaseUser
    private var cameraMoved: Boolean = false
    private var lastUpdate: Long = 0
    private var sensorManager: SensorManager? = null

    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_main_float_button_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_main_float_button_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_float_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_float_bottom_anim) }

    private var mainFloatClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val actionBar = supportActionBar!!
        actionBar.hide()

//        get current user email and display
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser!!

        if(user != null){
            val uid = user.uid
            getUserInformation(uid)
        }
        else{
            usernameTextView.text = "DEV"
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationManger = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object: LocationListener{
            override fun onLocationChanged(location: Location) {
                //locationTextView.text = " Location: "+location.latitude.toString()+" , "+location.longitude.toString()
                currentLatLng = LatLng(location.latitude, location.longitude)
                if(prevLatLng != currentLatLng){
                    prevLatLng = currentLatLng

                    if(!cameraMoved){
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14.5f))
                        cameraMoved = true
                    }
//                  Remove old marker, circle and add new one
                    mMap.clear()
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Hi"))
                    mMap.addCircle(CircleOptions()
                            .center(currentLatLng)
                            .radius((radSeekBar.progress * 1000).toDouble())
                            .strokeColor(0x7766FFFF)
                            .fillColor(0x4499FFFF))
                }
            }
            override fun onProviderDisabled(provider: String) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }

//      set listener on seek bar when it change will change radiasTextView text too
        radSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                val progress = radSeekBar.progress
                radiasTextView.text = "${progress * 1000} Metre"
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("Hi"))
                mMap.addCircle(CircleOptions()
                        .center(currentLatLng)
                        .radius((progress * 1000).toDouble())
                        .strokeColor(0x7766FFFF)
                        .fillColor(0x4499FFFF))
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                val progress = radSeekBar.progress
                radiasTextView.text = "${progress * 1000} Metre"
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                val progress = radSeekBar.progress
                radiasTextView.text = "${progress * 1000} Metre"
            }

        })

        requestLocationButton()
//      add Short cut button listener
        mainFloatBtn.setOnClickListener {
            onMainFloatBtnClicked()
        }
        restaurFloatBtn.setOnClickListener {
            onClickShortcut("restaurant")
        }
        entertainFloatBtn.setOnClickListener {
            onClickShortcut("entertainment")
        }
        hospitalFloatBtn.setOnClickListener {
            onClickShortcut("hospital")
        }
        attracFloatBtn.setOnClickListener {
            onClickShortcut("attraction")
        }
        marketFloatBtn.setOnClickListener {
            onClickShortcut("market")
        }

    }
//  Do thing when main shortcut button Onclick
    private fun onMainFloatBtnClicked(){
        setVisibility(mainFloatClicked)
        setAnimation(mainFloatClicked)
        setClickable(mainFloatClicked)
        mainFloatClicked = !mainFloatClicked
    }
//  Show/Hide sub shortcut button
    private fun setVisibility(clicked: Boolean) {
        if (!clicked){
            restaurFloatBtn.visibility = View.VISIBLE
            entertainFloatBtn.visibility = View.VISIBLE
            hospitalFloatBtn.visibility = View.VISIBLE
            attracFloatBtn.visibility = View.VISIBLE
            marketFloatBtn.visibility = View.VISIBLE
            favFloatBtn.visibility = View.VISIBLE
        }else{
            restaurFloatBtn.visibility = View.INVISIBLE
            entertainFloatBtn.visibility = View.INVISIBLE
            hospitalFloatBtn.visibility = View.INVISIBLE
            attracFloatBtn.visibility = View.INVISIBLE
            marketFloatBtn.visibility = View.INVISIBLE
            favFloatBtn.visibility = View.INVISIBLE
        }
    }
//  Animation when Showing/Hiding sub shortcut button
    private fun setAnimation(clicked: Boolean) {
        if(!clicked){
            restaurFloatBtn.startAnimation(fromBottom)
            entertainFloatBtn.startAnimation(fromBottom)
            hospitalFloatBtn.startAnimation(fromBottom)
            attracFloatBtn.startAnimation(fromBottom)
            marketFloatBtn.startAnimation(fromBottom)
            favFloatBtn.startAnimation(fromBottom)
            mainFloatBtn.startAnimation(rotateOpen)
        }else{
            restaurFloatBtn.startAnimation(toBottom)
            entertainFloatBtn.startAnimation(toBottom)
            hospitalFloatBtn.startAnimation(toBottom)
            attracFloatBtn.startAnimation(toBottom)
            marketFloatBtn.startAnimation(toBottom)
            favFloatBtn.startAnimation(toBottom)
            mainFloatBtn.startAnimation(rotateClose)
        }
    }
//  Set clickable when sub shortcut button is Show/Hide
    private fun setClickable(clicked: Boolean){
        if(!clicked){
            restaurFloatBtn.isClickable = true
            entertainFloatBtn.isClickable = true
            hospitalFloatBtn.isClickable = true
            attracFloatBtn.isClickable = true
            marketFloatBtn.isClickable = true
            favFloatBtn.isClickable = true
        }else{
            restaurFloatBtn.isClickable = false
            entertainFloatBtn.isClickable = false
            hospitalFloatBtn.isClickable = false
            attracFloatBtn.isClickable = false
            marketFloatBtn.isClickable = false
            favFloatBtn.isClickable = false
        }
    }
//  Start AroundMe intent when shortcut clicked
    private fun onClickShortcut(shortcutType: String){
        val currentLatitude = currentLatLng.latitude.toString()
        val currentLongitude = currentLatLng.longitude.toString()
        val type = shortcutType.toLowerCase()
        val radius = (radSeekBar.progress*1000).toString()
        val intent = Intent(this@HomeActivity, AroundMeActivity::class.java)
        intent.putExtra("currentLatitude", currentLatitude)
        intent.putExtra("currentLongitude", currentLongitude)
        intent.putExtra("type", type)
        intent.putExtra("radius", radius)
        startActivity(intent)
    }

//  enable sensor when on resume
    override fun onResume() {
        super.onResume()
        getUserInformation(user.uid)
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
    }
//  disable sensor when on pause
    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }
//  read user information and display username on usernameTextView
    private fun getUserInformation(uid: String){
        dataReference = FirebaseFirestore.getInstance()
        var db =  dataReference.collection("UserInformation")
        var userInfos: MutableList<UserInfo>
        lateinit var  userInfo: UserInfo
        db.whereEqualTo("uid", uid).limit(1).get()
            .addOnSuccessListener { snapshot ->
                userInfos = snapshot.toObjects(UserInfo::class.java)
                for(user in userInfos){
                    usernameTextView.text = user.username
                }
            }
    }

//  define onClick function that when click go to UserProfileActivity
    fun onClickUserProfile(view: View){
        var email = ""
        if(user != null){
            email = user.email.toString()
        }
        val userprofileIntent = Intent(this@HomeActivity, UserProfileActivity::class.java)
        userprofileIntent.putExtra("email", email)
        startActivity(userprofileIntent)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when(requestCode){
            10 -> requestLocationButton()
            else -> { }
        }
    }

    private fun requestLocationButton() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                requestPermissions(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET), 10
                )
            }
            return
        }
        locationManger.requestLocationUpdates("gps", 5000, 0f, locationListener)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }


//  Start favorite intent when favorite button clicked
    fun onClickFavBtn(view: View){
        val currentLatitude = currentLatLng.latitude.toString()
        val currentLongitude = currentLatLng.longitude.toString()
        val intent = Intent(this@HomeActivity, FavoriteListActivity::class.java)
        intent.putExtra("currentLatitude", currentLatitude)
        intent.putExtra("currentLongitude", currentLongitude)
        startActivity(intent)
    }

    fun onClickSignOut(view: View){
        mAuth!!.signOut()
        val intent = Intent(this@HomeActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
//  Onclick Search button
    fun onClickToAroundMe(view: View){
        val currentLatitude = currentLatLng.latitude.toString()
        val currentLongitude = currentLatLng.longitude.toString()
        val type = searchTextView.text.toString().toLowerCase()
        val radius = (radSeekBar.progress*1000).toString()
        val intent = Intent(this@HomeActivity, AroundMeActivity::class.java)
        intent.putExtra("currentLatitude", currentLatitude)
        intent.putExtra("currentLongitude", currentLongitude)
        intent.putExtra("type", type)
        intent.putExtra("radius", radius)
        startActivity(intent)
    }
//  Accelerometer changed detect
    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            getAccelerometer(event)
        }
    }
//  Abstract function defined
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
//  Check Accelerometer movement and random radius
    private fun getAccelerometer(event: SensorEvent) {
        val values = event.values
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val accel = (x*x+y*y+z*z) / (SensorManager.GRAVITY_EARTH* SensorManager.GRAVITY_EARTH)
        val actualTime = System.currentTimeMillis()
        val timed = (actualTime-lastUpdate > 500 )
        val switched = accelSwitch.isChecked
        if(timed && switched) {
            if (accel >= 1.50) {
                lastUpdate = actualTime
                radSeekBar.progress = (1..10).random()
            }
        }
        else {
            return
        }
    }

}