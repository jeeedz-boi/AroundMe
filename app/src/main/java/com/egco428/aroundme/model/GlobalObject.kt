package com.egco428.aroundme.model

// using object as global variable
class PlaceInfo(val name:String, val latitude:Double, val longitude:Double, val placeId: String)
object PlaceInfos {
    val infoArray =  HashMap<String, PlaceInfo>()
}

class FavtoDB(val faveList: ArrayList<String>){
    constructor(): this(ArrayList<String>())
}
object FavoriteListDatabase{
    val FavoriteList =  ArrayList<String>()
}