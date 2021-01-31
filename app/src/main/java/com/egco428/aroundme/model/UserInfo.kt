package com.egco428.aroundme.model

// user information for set to firestore
class UserInfo(
    var uid:String,
    var email: String,
    var username: String,
    var mobileNo: String){

        constructor(): this("","","","")
}