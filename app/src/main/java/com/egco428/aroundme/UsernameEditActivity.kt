package com.egco428.aroundme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_usernameedit.*

class UsernameEditActivity : AppCompatActivity() {
 //  define variable needed for firebase auth
    private var mAuth: FirebaseAuth? = null
    private lateinit var user: FirebaseUser
//  define variable needed for firebase firestore
    private lateinit var dataReference: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usernameedit)
        val actionBar = supportActionBar!!
        actionBar.hide()

        mAuth = FirebaseAuth.getInstance()
        dataReference = FirebaseFirestore.getInstance()
        user =  mAuth!!.currentUser!!
        loadCurrentUsername()
    }

// define function that read currently user's username and display on oldPhoneTextView
    private fun loadCurrentUsername() {
        val db =  dataReference.collection("UserInformation").document(user.uid)
        db.get()
            .addOnSuccessListener {documentSnapshot ->
                oldUsernameTextView.text = "Your Username : ${documentSnapshot.getString("username")}"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Load Username Failed Please Try Again!", Toast.LENGTH_LONG).show()
            }
    }
//  Onclick new Username submit
    fun emailSubmit(view: View) {
        if (newUsernameEdittext.text.isEmpty()) {
            newUsernameEdittext.error = "Please Enter new E-mail"
        } else {
//          Update new Username
            val db =  dataReference.collection("UserInformation").document(user.uid)
            val newUsername = newUsernameEdittext.text.toString()
            db.update("username", newUsername)
                .addOnCompleteListener {
                    Toast.makeText(this, "Username Update Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Username Update Failed Please Try Again!", Toast.LENGTH_LONG).show()
                }
            finish()
        }
    }
//  Onclick back button
    fun back(view: View){
        finish()
    }

}