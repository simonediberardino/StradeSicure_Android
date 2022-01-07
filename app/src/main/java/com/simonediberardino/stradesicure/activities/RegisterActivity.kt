package com.simonediberardino.stradesicure.activities

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ProgressDialog
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.utils.Utility


class RegisterActivity : AdaptedActivity() {
    private var uploadedImage: Uri? = null
    
    override fun initializeLayout() {
        setContentView(R.layout.activity_register)

        val registerBtn = this.findViewById<View>(R.id.register_register_button)
        registerBtn.setOnClickListener { registerUser() }

        val loginBtn = this.findViewById<View>(R.id.register_login_text)
        loginBtn.setOnClickListener { onBackPressed() }

        val profileImage = this.findViewById<ImageView>(R.id.register_profile_image)

        profileImage.setOnClickListener { startGalleryActivity() }
    }

    private fun registerUser() {
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

            if(isEmailRegistered)
                Utility.showToast(this, "email registered")
        }

        val emailUser = EmailUser(firstName, lastName, email, passwordEncrypted!!)
        uploadProfilePicToFirebase(emailUser){
            FirebaseClass.addEmailUserToFirebase(emailUser)
            Utility.showToast(this, getString(R.string.register_success))
            Utility.navigateTo(this, MapsActivity::class.java)
        }
    }

    private fun startGalleryActivity(){
        val PICK_IMAGE = 100
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    private fun uploadProfilePicToFirebase(emailUser: EmailUser, callback: Runnable){
        val storageReference = FirebaseStorage.getInstance().reference

        if(uploadedImage == null){
            callback.run()
            return
        }

        val progressDialog = ProgressDialog(this)
        val reference: StorageReference = storageReference.child("images/" + emailUser.email)

        reference.putFile(uploadedImage!!)
            .addOnSuccessListener {
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
}