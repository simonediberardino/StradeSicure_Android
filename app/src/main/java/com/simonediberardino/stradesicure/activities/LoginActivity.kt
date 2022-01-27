package com.simonediberardino.stradesicure.activities

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
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.storage.ApplicationData
import com.simonediberardino.stradesicure.utils.Utility


class LoginActivity : AdaptedActivity() {
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

        FirebaseClass.getEmailUsersRef().get().addOnCompleteListener { users ->
            val matchedUser: EmailUser? =
                users.result.children.find {
                it.child("uniqueId").value.toString().equals(enteredEmail, ignoreCase = true)
            }?.getValue(EmailUser::class.java)

            if(matchedUser == null || matchedUser.password != encryptedPassword){
                Utility.oneLineDialog(this, this.getString(R.string.credenzialierrate), null)
            }else{
                LoginHandler.doLogin(matchedUser)
                Utility.showToast(this, this.getString(R.string.loginsuccess))
                Utility.goToMainMenu(this)
            }
        }
    }

    // TODO: Update data;
    fun handleLoginFB(){
        val loginButton = findViewById<LoginButton>(R.id.login_facebook_button)

        callbackManager = CallbackManager.Factory.create()

        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun onSuccess(loginResult: LoginResult) {
                val token = loginResult.accessToken
                val userId = token.userId

                FirebaseClass.getUserObjectById<FbUser>(userId, object : RunnablePar{
                    override fun run(p: Any?) {
                        var loggedUser = p as FbUser?

                        if(loggedUser == null){
                            loggedUser = RegisterActivity.registerFbUser(userId)
                        }

                        LoginHandler.doLogin(loggedUser)
                    }

                })
            }

            override fun onCancel() {
            }

            override fun onError(e: FacebookException) {
            }
        })
    }
}