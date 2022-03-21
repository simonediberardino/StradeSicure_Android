package com.simonediberardino.stradesicure.activities

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ToastSS
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility


class LoginActivity : SSActivity() {
    private lateinit var callbackManager: CallbackManager

    override fun initializeLayout() {
        setContentView(R.layout.activity_login)

        val registerButton = this.findViewById<View>(R.id.login_register_button)
        registerButton.setOnClickListener {
            Utility.navigateTo(this, RegisterActivity::class.java)
        }

        val loginButton = this.findViewById<View>(R.id.login_login_button)
        loginButton.setOnClickListener { handleLoginEmail() }

        this.handleLoginFB()
    }

    fun handleLoginEmail() {
        val enteredEmail = this.findViewById<EditText>(R.id.login_email_et).text.toString().lowercase()
        val enteredPassword = this.findViewById<EditText>(R.id.login_password_et).text.toString().lowercase()
        val encryptedPassword = Utility.getMD5(enteredPassword)

        FirebaseClass.emailUsersRef.get().addOnCompleteListener { users ->
            val matchedUser: EmailUser? =
                users.result.children.find {
                it.child("uniqueId").value.toString().equals(enteredEmail, ignoreCase = true)
            }?.getValue(EmailUser::class.java)

            if(matchedUser == null || matchedUser.password != encryptedPassword){
                Utility.oneLineDialog(this, this.getString(R.string.credenzialierrate))
            }else{
                loginSuccess(matchedUser)
            }
        }
    }

    fun handleLoginFB(){
        callbackManager = CallbackManager.Factory.create()

        val loginButton = findViewById<LoginButton>(R.id.login_facebook_button)

        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun onSuccess(loginResult: LoginResult) {
                val token = loginResult.accessToken
                val userId = token.userId

                FirebaseClass.getUserObjectById<FbUser>(userId, object : RunnablePar{
                    override fun run(p: Any?) {
                        Thread{
                            val loggedUser = p as FbUser?

                            val doLogin = {
                                loginSuccess(loggedUser)
                            }

                            if(loggedUser != null){
                                doLogin()
                            }else{
                                RegisterActivity.registerFbUser(userId){
                                    doLogin()
                                }
                            }
                        }.start()
                    }
                })
            }

            override fun onCancel() {
                loginError()
            }

            override fun onError(e: FacebookException) {
                loginError()
            }
        })
    }

    fun loginSuccess(loggedUser: User?){
        runOnUiThread {
            LoginHandler.doLogin(loggedUser)
            ToastSS.show(this, this.getString(R.string.login_success))
            Utility.goToMainMenu(this)
        }
    }

    fun loginError(){
        runOnUiThread {
            ToastSS.show(this, this.getString(R.string.login_error))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}