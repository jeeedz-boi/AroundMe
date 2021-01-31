package com.egco428.aroundme

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils.isDigitsOnly
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.egco428.aroundme.model.UserInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.register_dialog_fragment.*
import kotlinx.android.synthetic.main.register_dialog_fragment.view.*

class RegisterDialog : DialogFragment() {

    companion object {
        fun newInstance(): RegisterDialog {
            val args = Bundle()
            val fragment = RegisterDialog()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.register_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    lateinit var dataReference: FirebaseFirestore
//  define function that save all user information to firestore and using user uid as document name
    private fun submitUserData(uid:String, email:String, username:String, mobileNo:String) {
        dataReference = FirebaseFirestore.getInstance()
        var db =  dataReference.collection("UserInformation").document(uid)
        val userData = UserInfo(
                uid,
                email,
                username,
                mobileNo
            )
            db.set(userData)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Register Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Register Failed", Toast.LENGTH_SHORT).show()
                }
        }

    private var mAuth: FirebaseAuth? = null
    private fun setupClickListeners(view: View) {
        val homeIntent = Intent(activity, HomeActivity::class.java)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        view.submitBtn.setOnClickListener {
            val username = usernameField.text.toString()
            val mobileNo = mobileNoField.text.toString()
            val password = passwordField.text.toString()
            val pswConfirm = confirmPswField.text.toString()
            val email = emailField.text.toString()
            var validator = true
            val emptyMsg = "Empty Not Allow!"

//          check if every text field in valid pattern (no empty allow/email pattern matched/password matched)
            if(mobileNo.isEmpty()){
                mobileNoField.error = emptyMsg
                validator = false
            }
            else if(!isDigitsOnly(mobileNo)){
                mobileNoField.error = "Number Only"
                validator = false
            }
            else{
                validator = true
            }

            if(username.isEmpty()){
                usernameField.error = emptyMsg
                validator = false
            }
            else{
                validator = true
            }

            if(password.isEmpty()){
                passwordField.error = emptyMsg
                validator = false
            }
            else if(password.length < 8){
                passwordField.error = "Password require At least 8 Characters"
                validator = false
            }
            else{
                validator = true
            }

            if(pswConfirm.isEmpty()){
                validator = false
                confirmPswField.error = emptyMsg
            }
            else if(password != pswConfirm){
                validator = false
                confirmPswField.error = "Password not Matched"
            }
            else{
                validator = true
            }

            if(email.isEmpty()){
                validator = false
                emailField.error = emptyMsg

            }
            else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                emailField.error = "Please Enter Valid Email"
                validator = false
            }
            else{
                validator = true
            }

//          if every field valid allow to leave this dialog
            if(validator){
                mAuth = FirebaseAuth.getInstance()
//              create user and check if email already exist in firebase auth
                mAuth!!.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            val uid = mAuth!!.currentUser!!.uid
                            submitUserData(uid,  email, username, mobileNo)
                            startActivity(homeIntent)
                        }
                    }
                    .addOnFailureListener {exception ->
                        emailField.error = "This e-mail address already used by another account"
                    }
            }

        }


    }

}