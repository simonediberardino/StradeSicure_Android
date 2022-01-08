package com.simonediberardino.stradesicure.activities

import android.view.View
import android.widget.EditText
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.utils.Utility


class LoginActivity : AdaptedActivity() {
    override fun initializeLayout() {
        setContentView(R.layout.activity_login)

        val registerBtn = this.findViewById<View>(R.id.login_register_button)
        registerBtn.setOnClickListener {
            Utility.navigateTo(this, RegisterActivity::class.java)
        }

        val loginButton = this.findViewById<View>(R.id.login_login_button)
        loginButton.setOnClickListener { handleLogin() }
    }

    fun handleLogin() {
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
                onLogin(matchedUser)
                Utility.showToast(this, this.getString(R.string.loginsuccess))
                Utility.goToMainMenu(this)
            }
        }
    }

    companion object {
        fun onLogin(loggedUser: User){
            LoginHandler.deviceUser = loggedUser
        }

        fun onLogout(){
            LoginHandler.deviceUser = null
        }
    }
}