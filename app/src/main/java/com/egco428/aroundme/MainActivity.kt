package com.egco428.aroundme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
//  define variable needed for firebase auth
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actionBar = supportActionBar!!
        actionBar.hide()
        mAuth = FirebaseAuth.getInstance()
//      check if have current user if exist skip this step go to HomeActivity
        if(mAuth!!.currentUser != null){
            startHomeActivity()
            finish()
        }
//      define login button listener
        goToLoginBtn.setOnClickListener {
            LoginDialog.newInstance().show(supportFragmentManager, "Login")
        }
//      define register button listener
        goToRegisterBtn.setOnClickListener {
            RegisterDialog.newInstance().show(supportFragmentManager, "Register")
        }
    }

//  define function that start HomeActivity when called
    private fun startHomeActivity() {
        val homeIntent = Intent(this@MainActivity, HomeActivity::class.java)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(homeIntent)
    }

//  show founder list
    fun onClickLogo(view: View){
        Toast.makeText(applicationContext, "Prai And Pete Made This", Toast.LENGTH_LONG).show()
    }
}