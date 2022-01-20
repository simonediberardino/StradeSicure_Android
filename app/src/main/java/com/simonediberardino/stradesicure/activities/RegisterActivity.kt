package com.simonediberardino.stradesicure.activities

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.facebook.Profile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ProgressDialog
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility


class RegisterActivity : AdaptedActivity() {
    private var uploadedImage: Uri? = null
    
    override fun initializeLayout() {
        setContentView(R.layout.activity_register)

        val registerBtn = this.findViewById<View>(R.id.register_register_button)
        registerBtn.setOnClickListener { registerEmailUser() }

        val loginBtn = this.findViewById<View>(R.id.register_login_text)
        loginBtn.setOnClickListener { onBackPressed() }

        val profileImage = this.findViewById<ImageView>(R.id.register_profile_image)
        profileImage.setOnClickListener { startGalleryActivity() }
    }

    private fun registerEmailUser() {
        val firstName = this.findViewById<EditText>(R.id.register_name_et).text.toString().trim()
        val lastName = this.findViewById<EditText>(R.id.register_surname_et).text.toString().trim()
        val email = this.findViewById<EditText>(R.id.register_email_et).text.toString().trim()
        val passwordRaw = this.findViewById<EditText>(R.id.register_password_et).text.toString().trim()
        
        val isValidEmail = !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        if (!isValidEmail) {
            val message = this.getString(R.string.emailnotvalid)
            Utility.oneLineDialog(this, message, null)
            return
        }

        val minLength = 3
        if(firstName.isEmpty()){
            val message = this.getString(R.string.nameinvalidlength).replace("{length}", minLength.toString())
            Utility.oneLineDialog(this, message, null)
            return
        }

        if(lastName.isEmpty()){
            val message = this.getString(R.string.surnameinvalidlength).replace("{length}", minLength.toString())
            Utility.oneLineDialog(this, message, null)
            return
        }

        val passwordEncrypted = Utility.getMD5(passwordRaw)

        FirebaseClass.getEmailUsersRef().get().addOnCompleteListener { snap ->
            val isEmailRegistered: Boolean = snap.result.children.any{
                it.child("email").value.toString().equals(email, ignoreCase = true)
            }

            if(isEmailRegistered) {
                val message = this.getString(R.string.emailgiaregistrata)
                Utility.oneLineDialog(this, message, null)
                return@addOnCompleteListener
            }

            val emailUser = EmailUser(firstName, lastName, email, passwordEncrypted!!)

            val finalCallback = Runnable {
                FirebaseClass.addEmailUserToFirebase(
                    emailUser,
                    object : RunnablePar {
                        override fun run(p: Any?) {
                            val dataSnapshot = p as DataSnapshot?
                            uploadProfilePicToFirebase(dataSnapshot!!) {
                                LoginActivity.onLogin(emailUser)
                                Utility.showToast(this@RegisterActivity, this@RegisterActivity.getString(R.string.register_success))
                                Utility.goToMainMenu(this@RegisterActivity)
                            }
                        }
                    })
            }

            if(uploadedImage == null)
                Utility.oneLineDialog(this, getString(R.string.noimagewarn)){
                    finalCallback.run()
                }
            else finalCallback.run()
        }
    }

    private fun startGalleryActivity(){
        val PICK_IMAGE = 100
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    private fun uploadProfilePicToFirebase(dataSnapshot: DataSnapshot, callback: Runnable){
        val storageReference = FirebaseStorage.getInstance().reference

        if(uploadedImage == null){
            callback.run()
            return
        }

        val progressDialog = ProgressDialog(this)
        val reference: StorageReference = storageReference.child("images/" + dataSnapshot.ref.key)

        reference.putFile(uploadedImage!!)
            .addOnSuccessListener {
                println("FINISHHHH!")
                progressDialog.dismiss()
                callback.run()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Utility.showToast(this, e.message.toString())
            }
            .addOnProgressListener { taskSnapshot ->
                val progress: Double = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                progressDialog.progress = progress.toInt()
            }
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        val profileImage = this.findViewById<ImageView>(R.id.register_profile_image)
        if (resultCode == RESULT_OK && reqCode == 100) {
            uploadedImage = data?.data
            profileImage.setImageURI(uploadedImage)
        }
    }

    companion object{
        fun registerFbUser(userId: String): FbUser {
            val createdUser = Profile.getCurrentProfile()
            val firstName = createdUser.firstName
            val lastName = createdUser.lastName
            val loggedUser = FbUser(firstName, lastName, userId)
            FirebaseClass.addFbUserToFirebase(loggedUser)
            return loggedUser
        }
    }
}