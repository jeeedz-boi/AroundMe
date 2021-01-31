package com.egco428.aroundme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_userprofile.*
import kotlinx.android.synthetic.main.row_userprofilelistview.view.*


class UserProfileActivity : AppCompatActivity() {

    private val ListViewStr: ArrayList<String> = ArrayList<String>()

 //  define variable needed for firebase auth
    private var mAuth: FirebaseAuth? = null
    private var email:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userprofile)
        val actionBar = supportActionBar!!
        actionBar.hide()

        email = intent.getStringExtra("email").toString()
        mAuth = FirebaseAuth.getInstance()
        initListViewStr()
        initUserProfileListView()
        userprofileListView.setOnItemClickListener { _, view, position, _ ->
            if(position != 0){
                itemClickUserProfileListView(position)
            }
        }
    }
//  OnItemclick start new intent
    private fun itemClickUserProfileListView(position: Int){
        lateinit var intent: Intent
        when(position){
            0 -> { }
            1 -> {
                intent = Intent(this@UserProfileActivity, UsernameEditActivity::class.java)
            }
            2 -> {
                intent = Intent(this@UserProfileActivity, PhoneEditActivity::class.java)
            }
            3 -> {
                intent = Intent(this@UserProfileActivity, PasswordEditActivity::class.java)
            }
        }
        try {
            startActivity(intent)
        }catch (e: Exception){}

    }
//  Set List View adapter
    private fun initUserProfileListView(){
        userprofileListView.adapter = userProfileAdapter(this, ListViewStr, email)
    }
//  Add item to List View
    private fun initListViewStr(){
        ListViewStr.add("\tEmail")
        ListViewStr.add("\tUsername")
        ListViewStr.add("\tMobile Number")
        ListViewStr.add("\tReset password")
    }

//  Create List View Adapter class
    private class userProfileAdapter(context: Context, arr: ArrayList<String>, email: String): BaseAdapter(){
        private val mContext: Context = context
        val data: ArrayList<String> = arr
        val email: String = email
        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val rowMain: View
            if(convertView == null){
                val layoutInflater = LayoutInflater.from(mContext)
                rowMain = layoutInflater.inflate(R.layout.row_userprofilelistview, viewGroup, false)
            }else {
                rowMain = convertView
            }
            rowMain.row_userprofileTextView.text = data[position].toString()
            if (position == 0){
                rowMain.isEnabled = false
                rowMain.imageView.setImageDrawable(null)
                rowMain.emailTextView.text = email
            }
            return rowMain
        }
        override fun getItem(position: Int): Any { return position }
        override fun getItemId(position: Int): Long { return position.toLong() }
        override fun getCount(): Int { return data.count() }
    }

//  define function that perform Signout and go to MainActivity
    fun onClickSignOut(view: View){
        mAuth!!.signOut()
        val intent = Intent(this@UserProfileActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    fun back(view: View){
        finish()
    }

}