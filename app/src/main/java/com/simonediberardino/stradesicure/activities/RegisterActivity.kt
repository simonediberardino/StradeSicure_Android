package com.simonediberardino.stradesicure.activities

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.facebook.Profile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ToastSS
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility
import java.io.ByteArrayOutputStream


class RegisterActivity : SSActivity() {
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
            Utility.oneLineDialog(this, message)
            return
        }

        val minLength = 3
        if(firstName.isEmpty()){
            val message = this.getString(R.string.nameinvalidlength).replace("{length}", minLength.toString())
            Utility.oneLineDialog(this, message)
            return
        }

        if(lastName.isEmpty()){
            val message = this.getString(R.string.surnameinvalidlength).replace("{length}", minLength.toString())
            Utility.oneLineDialog(this, message)
            return
        }

        val passwordEncrypted = Utility.getMD5(passwordRaw)

        FirebaseClass.emailUsersRef.get().addOnCompleteListener { snap ->
            val isEmailRegistered: Boolean = snap.result.children.any{
                it.child("email").value.toString().equals(email, ignoreCase = true)
            }

            if(isEmailRegistered) {
                val message = this.getString(R.string.emailgiaregistrata)
                Utility.oneLineDialog(this, message)
                return@addOnCompleteListener
            }

            val emailUser = EmailUser("$firstName $lastName", email, passwordEncrypted!!)

            val finalCallback = Runnable {
                FirebaseClass.addEmailUserToFirebase(
                    emailUser,
                    object : RunnablePar() {
                        override fun run(p: Any?) {
                            val dataSnapshot = p as DataSnapshot?
                            uploadProfilePicToFirebase(this@RegisterActivity, uploadedImage, dataSnapshot!!) {
                                LoginHandler.doLogin(emailUser)
                                ToastSS.show(this@RegisterActivity, this@RegisterActivity.getString(R.string.register_success))
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

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        val profileImage = this.findViewById<ImageView>(R.id.register_profile_image)
        if (resultCode == RESULT_OK && reqCode == 100) {
            uploadedImage = data?.data
            profileImage.setImageURI(uploadedImage)
        }
    }

    companion object{
        fun registerFbUser(userId: String, callback: Runnable) {
            LoginHandler.waittilFBProfileIsReady(object : RunnablePar(){
                override fun run(p: Any?) {
                    val createdUser = Profile.getCurrentProfile()
                    val firstName = createdUser.firstName
                    val lastName = createdUser.lastName
                    val loggedUser = FbUser("$firstName $lastName", userId)
                    FirebaseClass.addFbUserToFirebase(loggedUser)
                    callback.run()
                }
            })
        }

        fun uploadProfilePicToFirebase(context: AppCompatActivity, uploadedImage: Uri?, dataSnapshot: DataSnapshot, callback: Runnable){
            val progressDialog = ProgressDialog(context)
            progressDialog.max = 100
            progressDialog.setTitle(context.getString(R.string.caricamento_immagine))
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog.show()
            progressDialog.setCancelable(false)

            val storageReference = FirebaseStorage.getInstance().reference
            val reference: StorageReference = storageReference.child("images/" + dataSnapshot.ref.key)

            if(uploadedImage == null){
                callback.run()
                return
            }

            val byteArrayOutputStream = ByteArrayOutputStream()

            MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                uploadedImage)
                .compress(
                    Bitmap.CompressFormat.JPEG,
                    25,
                    byteArrayOutputStream
                )

            val dataToByteArray = byteArrayOutputStream.toByteArray()
            val uploadTask: UploadTask = reference.putBytes(dataToByteArray)

            uploadTask.addOnSuccessListener {
                callback.run()
            }.addOnFailureListener{ e ->
                ToastSS.show(context, e.message.toString())
            }.addOnProgressListener { taskSnapshot ->
               val progress: Double = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                progressDialog.progress = progress.toInt()
            }.addOnCompleteListener{
                progressDialog.dismiss()
            }
        }
    }
}