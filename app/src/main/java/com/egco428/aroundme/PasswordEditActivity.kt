package com.egco428.aroundme

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_passwordedit.*


class PasswordEditActivity : AppCompatActivity() {
//  define variable needed for firebase auth
    private lateinit var mAuth: FirebaseAuth
    private lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passwordedit)
        val actionBar = supportActionBar!!
        actionBar.hide()
        mAuth = FirebaseAuth.getInstance()
        user = mAuth.currentUser!!
    }
//  Show error when false password input
    fun passwordSubmit(view: View){
        var check = true
        if(oldPasswordEditText.text.isEmpty()){
            oldPasswordEditText.error = "Please Enter old password"
            check = false
        }
        if(newPasswordEditText.text.isEmpty()){
            newPasswordEditText.error = "Please Enter new password"
            check = false
        }
        if(newPasswordEditText.length() < 8){
            newPasswordEditText.error = "Password require At least 8 Characters"
            check = false
        }
        if (newPasswordEditText.text.toString() != newPasswordEditText2.text.toString()){
            newPasswordEditText2.error = "Password not matched"
            check = false
        }
        if (check) {
//          Update new password
            user.updatePassword(newPasswordEditText.text.toString())
                .addOnSuccessListener {
                    Toast.makeText(this, "Password Changes Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Password Changes Failed Please Try Again!", Toast.LENGTH_SHORT).show()
                }
            finish()
        }
    }
//  Onclick back button
    fun back(view: View){
        finish()
    }

}