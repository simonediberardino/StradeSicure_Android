package com.simonediberardino.stradesicure.activities

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.utils.Utility


class LoginActivity : AdaptedActivity() {
    override fun initializeLayout() {
        setContentView(R.layout.activity_login)

        val registerBtn = this.findViewById<View>(R.id.login_register_button)
        registerBtn.setOnClickListener {
            Utility.navigateTo(this, RegisterActivity::class.java)
        }
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        val imageUri: Uri?
        val profileImage = this.findViewById<ImageView>(R.id.register_profile_image)
        if (resultCode == RESULT_OK && reqCode == 100) {
            imageUri = data?.data
            profileImage.setImageURI(imageUri)
        }
    }

    fun onLogin(loggedUser: User){
        LoginHandler.deviceUser = loggedUser
    }

    fun onLogout(){
        LoginHandler.deviceUser = null
    }

}