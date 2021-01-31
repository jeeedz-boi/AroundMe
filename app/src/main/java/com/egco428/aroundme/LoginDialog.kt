package com.egco428.aroundme

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.login_dialog_fragment.emailField
import kotlinx.android.synthetic.main.login_dialog_fragment.passwordField
import kotlinx.android.synthetic.main.login_dialog_fragment.view.*

class LoginDialog : DialogFragment() {
    companion object {
        fun newInstance(): LoginDialog {
            val args = Bundle()
            val fragment = LoginDialog()
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_dialog_fragment, container, false)
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

    private var mAuth: FirebaseAuth? = null
    private fun setupClickListeners(view: View) {
        val homeIntent = Intent(activity, HomeActivity::class.java)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//      check if it already exist user login if true skip login step
        mAuth = FirebaseAuth.getInstance()
        if(mAuth!!.currentUser != null){
            startActivity(homeIntent)
        }

        view.submitBtn.setOnClickListener {
            onPerformLogin()
        }
        view.passwordField.setOnKeyListener { view, keyCode, event ->
            when {

                //Check if it is the Enter-Key,      Check if the Enter Key was pressed down
                ((keyCode == KeyEvent.KEYCODE_ENTER) && (event.action == KeyEvent.ACTION_DOWN)) -> {
                    //perform an action
                    onPerformLogin()
                    //return true
                    return@setOnKeyListener true
                }
                else -> false
            }
        }
    }

//  define function that perform when click
    private fun onPerformLogin(){
        val homeIntent = Intent(activity, HomeActivity::class.java)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val email = emailField.text.toString()
        val password = passwordField.text.toString()
        val emptyMsg = "Empty Not Allow!"
        var validator = true

//      check if every text field in valid pattern (no empty allow/email pattern matched)
        if(email.isEmpty()){
            emailField.error = emptyMsg
            validator = false
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailField.error = "Please Enter Valid Email"
            validator = false
        }
        else validator = true

        when {
            password.isEmpty() -> {
                passwordField.error = emptyMsg
                validator = false
            }
            password.length < 8 -> {
                passwordField.error = "Password require At least 8 Characters"
                validator = false
            }
            else -> validator = true
        }

//      check if every text field in valid pattern (no empty allow/email pattern matched)
        if(validator) {
//          check if user exist in firebase auth
            mAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener{ task ->
                    if(task.isSuccessful){
                        startActivity(homeIntent)
                        dismiss()
                    }
                    else{
                        emailField.error = "Email or Password Incorrect"
                    }
                }
                .addOnFailureListener {
                    emailField.error = "Email or Password Incorrect"
                }
        }
    }

}
