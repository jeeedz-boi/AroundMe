package com.egco428.aroundme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_phoneedit.*

class PhoneEditActivity : AppCompatActivity() {
//  define variable needed for firebase auth
    private var mAuth: FirebaseAuth? = null
    private lateinit var user: FirebaseUser
//  define variable needed for firebase firestore
    private lateinit var dataReference: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phoneedit)
        val actionBar = supportActionBar!!
        actionBar.hide()

        mAuth = FirebaseAuth.getInstance()
        dataReference = FirebaseFirestore.getInstance()
        user =  mAuth!!.currentUser!!
        loadCurrentMobileNumber()
    }
// define function that read currently user's mobile number and display on oldPhoneTextView
    private fun loadCurrentMobileNumber() {
        val db =  dataReference.collection("UserInformation").document(user.uid)
        db.get()
            .addOnSuccessListener {documentSnapshot ->
                oldPhoneTextView.text = "Your Numbers : ${documentSnapshot.getString("mobileNo")}"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Load Mobile Number Failed Please Try Again!", Toast.LENGTH_LONG).show()
            }
    }
//  Onclick phone number submit
    fun phoneSubmit(view: View){
        if(newPhoneEdittext.text.isEmpty()){
            newPhoneEdittext.error = "Empty Not Allow"
        }
        else{
//          Update phone number
            val db =  dataReference.collection("UserInformation").document(user.uid)
            val newMoblieNo = newPhoneEdittext.text.toString()
            db.update("mobileNo", newMoblieNo)
                .addOnCompleteListener {
                Toast.makeText(this, "Mobile Numbers Update Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Mobile Number Update Failed Please Try Again!", Toast.LENGTH_LONG).show()
                }
            finish()
        }
    }
//  Onclick back button
    fun back(view: View){
        finish()
    }

}