package com.egco428.aroundme

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.egco428.aroundme.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_aroundme.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.lang.String.format


class AroundMeActivity: AppCompatActivity(), SensorEventListener {
//  define variable needed for Places API
    private val apiKey = "API-KEY-HERE"
    private var latitude:String? = null
    private var longitude:String? = null
    private var radius:String? = null
    private var type:String? =  null
    private var nearbySearchAPI_JSON = ""
    private lateinit var thisNearbySearchResults: List<QueryResult>
//  define variable needed for expandable list
    private val expandableListDetail = HashMap<String, List<String>>()
//  define variable needed for sensor handler
    private var sensorManager: SensorManager? = null
    private var lastUpdate: Long = 0
    private var randPos: Int = 0
    private var expanded: Int = 0

//  define variable needed for firebase auth and firebase firestore
    private var mAuth: FirebaseAuth? = null
    private lateinit var user: FirebaseUser
    private var dataReference = FirebaseFirestore.getInstance()
    private var  db =  dataReference.collection("UserFavoriteList")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aroundme)
        val actionBar = supportActionBar!!
        actionBar.hide()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lastUpdate = System.currentTimeMillis()

//      make sure info array and FavoriteList are empty every time this page has been called
        PlaceInfos.infoArray.clear()
        FavoriteListDatabase.FavoriteList.clear()

//      get currently user
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser!!

//      Prepare Place Search API get value from intent extras (sent by HomeActivity)
        latitude = intent.getStringExtra("currentLatitude")
        longitude = intent.getStringExtra("currentLongitude")
        type = intent.getStringExtra("type")
        radius = intent.getStringExtra("radius")
        nearbySearchAPI_JSON = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=${apiKey}&location=${latitude}%2C${longitude}&radius=${radius}&keyword=${type}"
        println(nearbySearchAPI_JSON)
        keywordTextView.text = "\t\t"+type!!.capitalize()
        
//      Fetching Result to GSON Object
        GsonFetching()
//      Disable accelerometer when group expanded (prevent crash)
        aroundMeListView.setOnGroupExpandListener {
            if(expanded == 0){
                aroundMeListView.get(randPos).findViewById<View>(R.id.groupConst).setBackgroundColor(Color.TRANSPARENT)
                sensorManager!!.unregisterListener(this)
            }
            expanded++
        }
//      Enable accelerometer when group Collapsed
        aroundMeListView.setOnGroupCollapseListener {
            if(expanded == 1){
                sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL)
            }
            expanded--
        }
        aroundMeListView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            try{
                for (index in 0 until ArrayList<String>(expandableListDetail.keys).size){
                    aroundMeListView.get(index).findViewById<View>(R.id.groupConst).setBackgroundColor(Color.TRANSPARENT)
                }
            }catch (e :Exception){ }
        }
    }

//  enable sensor when app resume
    override fun onResume() {
        super.onResume()
        readFromFirestore()
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
    }

//  disable sensor when app pause
    override fun onPause() {
        super.onPause()
        writeToFirestore()
        sensorManager!!.unregisterListener(this)
    }
//  Accelerometer changed detect
    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER){
            getAccelerometer(event)
        }
    }
//  Check Accelerometer movement and random place
    private fun getAccelerometer(event: SensorEvent) {
        val values = event.values
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val accel = (x*x+y*y+z*z) / (SensorManager.GRAVITY_EARTH*SensorManager.GRAVITY_EARTH)
        val actualTime = System.currentTimeMillis()
        if(accel>=1.50){
            if(actualTime-lastUpdate < 500){
                return
            }
            lastUpdate = actualTime
//          Random and highlight item
            try{
                aroundMeListView.get(randPos).findViewById<View>(R.id.groupConst).setBackgroundColor(Color.TRANSPARENT)
                var randRange = ArrayList<String>(expandableListDetail.keys).size
                if (randRange > 15){
                    randRange = 15
                }
                randPos = (0 until randRange).random()
                aroundMeListView.get(randPos).findViewById<View>(R.id.groupConst).setBackgroundColor(Color.parseColor("#FFC875"))
            }catch (e :Exception){ }
        }
    }
//  Abstract function defined (Keep compiler happy)
    override fun onAccuracyChanged(p0: Sensor, p1: Int) { }

//  define function that write/update favorite list to firestore
    private fun writeToFirestore(){
        var dataReference = FirebaseFirestore.getInstance()
        user = mAuth!!.currentUser!!
        var  db =  dataReference.collection("UserFavoriteList").document(user.uid)
        val fav = FavtoDB(FavoriteListDatabase.FavoriteList)
        println("write"+FavoriteListDatabase.FavoriteList.size)
        db.get()
            .addOnSuccessListener {snapshot ->
                if(snapshot.exists()) db.update("faveList", FavoriteListDatabase.FavoriteList)
                else db.set(fav)
            }
    }
//  define function that read favorite list from firestore
    private fun readFromFirestore(){
        var dataReference = FirebaseFirestore.getInstance()
        user = mAuth!!.currentUser!!
        var  db =  dataReference.collection("UserFavoriteList").document(user.uid)
        val fav = FavtoDB(FavoriteListDatabase.FavoriteList)
        db.get()
            .addOnSuccessListener {documentSnapshot ->
                if (documentSnapshot.exists()){
                    val tempArray: FavtoDB? = documentSnapshot.toObject(FavtoDB::class.java)
                    for(temp in tempArray!!.faveList){
                        FavoriteListDatabase.FavoriteList.add(temp)
                    }
                    println("read"+FavoriteListDatabase.FavoriteList.size)
                }
            }
    }

//  define function that send request and fetching respond body to gson object
    private fun GsonFetching() {
        val request = Request.Builder().url(nearbySearchAPI_JSON).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                Toast.makeText(applicationContext, "Check Your Internet Connnection", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val gson = GsonBuilder().create()
                val results = gson.fromJson(body, NearbySearchResult::class.java)

//              return results json array
                thisNearbySearchResults = results.results
//              GSON Object -> Expandable ListView
//              Since it seem to be async task for some reason, which mean if all of work do in onRespond we can confirm that thisNearbySearchResults won't be NULL
                runOnUiThread {
                    GsonToExpandableListView(thisNearbySearchResults)
                }
            }

//          GSON Object -> Expandable ListView
            private fun GsonToExpandableListView(thisNearbySearchResults: List<QueryResult>) {
                for(result in thisNearbySearchResults) {
                    val tempArray: MutableList<String> = ArrayList()
                    val types = result.types
                    if(result.opening_hours != null)
                    {
                        var isOpen = "NOW OPEN"
                        if(!result.opening_hours.open_now){
                            isOpen = "NOW CLOSE"
                        }
                        tempArray.add(isOpen)
                    }
                    if(result.rating > 0f){
                        tempArray.add("RATING: "+result.rating.toString())
                    }
                    else{
                        tempArray.add("RATING: NO DATA")
                    }
                    if (types.isNotEmpty())
                    {
                        var tempString = ""
                        var sizeCount = 1
                        val limit = 2

                        for (type in types) {
                            if(limit > sizeCount){
                                tempString += "${type.capitalize()}, "
                                println(types)
                                sizeCount += 1
                            }
                            else{
                                tempString += "${type.capitalize()}"
                            }
                        }
                        tempArray.add(tempString)
                    }
                    else{
                        tempArray.add("No type founded")
                    }
                    tempArray.add(result.vicinity)

//                  calculate distance between current location and destination location
                    val startPoint = Location("locationA")
                    startPoint.latitude = latitude!!.toDouble()
                    startPoint.longitude= longitude!!.toDouble()
                    val endPoint = Location("locationB")
                    endPoint.latitude = result.geometry.location.lat
                    endPoint.longitude = result.geometry.location.lng

//                  add object PlaceInfos in using afterward
                    val newPlaceInfo = PlaceInfo(result.name, latitude!!.toDouble(), longitude!!.toDouble(), result.place_id)
                    PlaceInfos.infoArray.put(result.name, newPlaceInfo)

                    val distance= startPoint.distanceTo(endPoint)
                    tempArray.add(format("%.3f",(distance/1000)).toString()+" Metre Away")
                    tempArray.add("Open in Google Maps")

                    expandableListDetail.put(result.name, tempArray)
                }
//          set adapter of aroundMeListView as CustomExpandableListAdapter
            aroundMeListView.setAdapter(CustomExpandableListAdapter(applicationContext, ArrayList<String>(expandableListDetail.keys), expandableListDetail))
//          set on click listener on child and check if click Open in Google Maps fi true open google maps and mark designed location
            aroundMeListView.setOnChildClickListener { expandableListView, view, groupPosition, childPosition, l ->
                val placeName = ArrayList<String>(expandableListDetail.keys)[groupPosition]
                val conf = expandableListDetail[ArrayList<String>(expandableListDetail.keys)[groupPosition]]!![childPosition]
                if(conf == "Open in Google Maps")
                {
                    val lat = PlaceInfos.infoArray[placeName]!!.latitude
                    val lon = PlaceInfos.infoArray[placeName]!!.longitude
                    val name = PlaceInfos.infoArray[placeName]!!.name

                    val gmmIntentUri = Uri.parse("geo:$lat,$lon?q=($name)")
                    val aroundIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    aroundIntent.setPackage("com.google.android.apps.maps")
                    startActivity(aroundIntent)
                }
                true
            }

            }
        })
    }

//  create Expandable List Adapter class
    internal class CustomExpandableListAdapter(
        private val context: Context, private val expandableListTitle: List<String>,
        private val expandableListDetail: HashMap<String, List<String>>
    ) : BaseExpandableListAdapter(){
        override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
            return expandableListDetail[expandableListTitle[listPosition]]!![expandedListPosition]
        }

        override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
            return expandedListPosition.toLong()
        }

        override fun getChildView(
            listPosition: Int, expandedListPosition: Int,
            isLastChild: Boolean, convertView: View?, parent: ViewGroup
        ): View {
            var convertView = convertView
            val expandedListText = getChild(listPosition, expandedListPosition) as String
            if (convertView == null) {
                val layoutInflater = context
                    .getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = layoutInflater.inflate(R.layout.exlist_item, null)
            }
            val expandedListTextView = convertView!!
                .findViewById<View>(R.id.expandedListItem) as TextView
            expandedListTextView.text = expandedListText
            return convertView
        }

        override fun getChildrenCount(listPosition: Int): Int {
            return expandableListDetail[expandableListTitle[listPosition]]!!.size
        }

        override fun getGroup(listPosition: Int): Any {
            return expandableListTitle[listPosition]
        }

        override fun getGroupCount(): Int {
            return expandableListTitle.size
        }

        override fun getGroupId(listPosition: Int): Long {
            return getGroupCount() - listPosition.toLong()
        }

        override fun getGroupView(
            listPosition: Int, isExpanded: Boolean,
            convertView: View?, parent: ViewGroup
        ): View {
            var convertView = convertView
            val listTitle = getGroup(listPosition) as String
            if (convertView == null) {
                val layoutInflater =
                    context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = layoutInflater.inflate(R.layout.exlist_group, null)
            }
            val listTitleTextView = convertView!!
                .findViewById<View>(R.id.listTitle) as TextView
            val favImage = convertView!!
                    .findViewById<View>(R.id.favImageView) as ImageView

//          Get place name/id from focused list
            val placeName = ArrayList<String>(expandableListDetail.keys)[listPosition]
            val placeID = PlaceInfos.infoArray[placeName]!!.placeId

//          set favImage tag value and set favImage src
            if(FavoriteListDatabase.FavoriteList.contains(placeID)){
                favImage.tag = 1
                favImage.setImageResource(R.drawable.ic_baseline_star_24)
            }
            else{
                favImage.tag = 0
                favImage.setImageResource(R.drawable.ic_baseline_star_border_24)
            }

//          create favImage onClick function to save user favorite list
            favImage.setOnClickListener {
                val placeName = ArrayList<String>(expandableListDetail.keys)[listPosition]
                val placeID = PlaceInfos.infoArray[placeName]!!.placeId
                if (favImage.tag == 1)
                {
                    favImage.setImageResource(R.drawable.ic_baseline_star_border_24)
                    favImage.tag = 0
                    FavoriteListDatabase.FavoriteList.remove(placeID)
                    Toast.makeText(context, "$placeName\nremove to your favorite", Toast.LENGTH_SHORT).apply {
                        setGravity(Gravity.BOTTOM, 8, 8)
                        show()
                    }
                }
                else {
                    favImage.setImageResource(R.drawable.ic_baseline_star_24)
                    favImage.tag = 1
                    FavoriteListDatabase.FavoriteList.add(placeID)
                    Toast.makeText(context, "$placeName\nadded to your favorite", Toast.LENGTH_SHORT).apply {
                        setGravity(Gravity.BOTTOM, 8, 8)
                        show()
                    }
                }
            }

            listTitleTextView.setTypeface(null, Typeface.BOLD)
            listTitleTextView.text = listTitle
            return convertView
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
            return true
        }
    }
//  onClick back button
    fun back(view: View){
        finish()
    }
}