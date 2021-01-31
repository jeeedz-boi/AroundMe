package com.egco428.aroundme

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.egco428.aroundme.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_aroundme.*
import okhttp3.*
import java.io.IOException

class FavoriteListActivity : AppCompatActivity() {
//  define variable needed for Places API
    private val apiKey = "API-KEY-HERE"
    private lateinit var user: FirebaseUser
    private var mAuth: FirebaseAuth? = null
//  define variable needed for expandable list
    private val expandableListDetail = HashMap<String, List<String>>()

    private var latitude:String? = null
    private var longitude:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_list)

        val actionBar = supportActionBar!!
        actionBar.hide()

//      make sure info array and FavoriteList are empty every time this page has been called
        PlaceInfos.infoArray.clear()
        FavoriteListDatabase.FavoriteList.clear()

//      get value from intent extras (sent by HomeActivity)
        latitude = intent.getStringExtra("currentLatitude")
        longitude = intent.getStringExtra("currentLongitude")

        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser!!
    }

// read favorite list to firestore when on resume (happened on create too)
    override fun onResume() {
        super.onResume()
        readFromFirestore()
    }
// write favorite list to firestore when on pause (happen when hit back button too)
    override fun onPause() {
        super.onPause()
        writeToFirestore()
    }

//  define function that write/update favorite list to firestore
    private fun writeToFirestore(){
        var dataReference = FirebaseFirestore.getInstance()
        user = mAuth!!.currentUser!!
        var  db =  dataReference.collection("UserFavoriteList").document(user.uid)
        val fav = FavtoDB(FavoriteListDatabase.FavoriteList)
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
                    GsonFetching()
                }
            }
    }

//  define function that send request and fetching respond body to gson object
    private fun GsonFetching() {
        val allPlaceSearchResults = ArrayList<QueryResult>()
        var count = 1
        for(place_id in FavoriteListDatabase.FavoriteList)
        {
            val searchByPlaceId = "https://maps.googleapis.com/maps/api/place/details/json?key=${apiKey}&place_id=${place_id}"
            val request = Request.Builder().url(searchByPlaceId).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Toast.makeText(applicationContext, "Check Your Internet Connection", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val gson = GsonBuilder().create()
                    val result = gson.fromJson(body, PlaceSearchResult::class.java)

                    allPlaceSearchResults.add(result.result)

                    if(count >= FavoriteListDatabase.FavoriteList.size){
                        runOnUiThread {
                            println("kuy")
                            GsonToExpandableListView(allPlaceSearchResults)
                        }
                    }
                    count += 1
                }
            })

        }
    }

    //          GSON Object -> Expandable ListView
    private fun GsonToExpandableListView(allPlaceSearchResults: ArrayList<QueryResult>) {
        for(result in allPlaceSearchResults) {
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

//          calculate distance between current location and destination location
            val startPoint = Location("locationA")
            startPoint.latitude = latitude!!.toDouble()
            startPoint.longitude= longitude!!.toDouble()
            val endPoint = Location("locationB")
            endPoint.latitude = result.geometry.location.lat
            endPoint.longitude = result.geometry.location.lng

//          add object PlaceInfos in using afterward
            val newPlaceInfo = PlaceInfo(result.name, latitude!!.toDouble(), longitude!!.toDouble(), result.place_id)
            PlaceInfos.infoArray.put(result.name, newPlaceInfo)

            val distance= startPoint.distanceTo(endPoint)
            tempArray.add(java.lang.String.format("%.3f", (distance / 1000)).toString()+" Metre Away")
            tempArray.add("Open in Google Maps")

            expandableListDetail.put(result.name, tempArray)
        }
        //  set adapter of aroundMeListView as CustomExpandableListAdapter
        aroundMeListView.setAdapter(
            AroundMeActivity.CustomExpandableListAdapter(
                applicationContext,
                ArrayList<String>(expandableListDetail.keys),
                expandableListDetail
            )
        )
        //  set on click listener on child and check if click Open in Google Maps fi true open google maps and mark designed location
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

                Log.d("place detail", "${placeID} ${PlaceInfos.infoArray[placeName]!!.name} ${placeName} ")
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
//  Onclick back button
    fun back(view: View){
        finish()
    }
}