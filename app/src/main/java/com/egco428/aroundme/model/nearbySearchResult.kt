package com.egco428.aroundme.model

// class for handle json object we request from google place api
class NearbySearchResult(val html_attributions: List<String>, val next_page_token:String, val results: List<QueryResult>, val status:String)
class PlaceSearchResult(val html_attributions: List<String>, val next_page_token:String, val result: QueryResult, val status:String)

class QueryResult(val business_status:String, val geometry: Geometry, val icon:String, val name:String, val opening_hours:OpeningHours, val photos: List<QueryPhotos>, val place_id:String,val plus_code:PlusCode,val rating:Double, val reference:String, val scope:String, val types: List<String>,val user_ratings_total:Int, val vicinity:String)
class QueryPhotos(val height:Int, val html_attributions: List<String>, val HTMLAttributions:String, val width:Int)
class Geometry(val location: Location, viewport: Viewport)
class Location(val lat:Double, val lng: Double)
class Viewport(val northeast: Northeast, val southwest:Southwest)
class Northeast(val lat:Double, val lng: Double)
class Southwest(val lat:Double, val lng: Double)
class PlusCode(val compound_code:String, val global_code:String)
class OpeningHours(val open_now:Boolean)
