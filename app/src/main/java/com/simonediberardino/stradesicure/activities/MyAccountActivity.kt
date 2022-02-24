package com.simonediberardino.stradesicure.activities

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ProgressDialog
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility
import de.hdodenhof.circleimageview.CircleImageView

class MyAccountActivity : SSActivity() {
    companion object {
        private const val totalSteps = 4
        private const val singleStepValue = 100/totalSteps
    }

    private lateinit var progressDialog: ProgressDialog
    private lateinit var nameTW: TextView
    private lateinit var emailTW: TextView
    private lateinit var reportsTW: TextView
    private lateinit var reviewsTW: TextView
    private lateinit var userIDTW: TextView
    private lateinit var profileImageIV: CircleImageView
    private lateinit var logoutBtn: RadioButton
    private lateinit var editIDBtn: ImageView
    private lateinit var editImageBtn: ImageView

    override fun initializeLayout() {
        this.setContentView(R.layout.activity_account)
        this.setupListeners()
        this.setupDialog()
        this.setProfileName()
        this.setProfileEmail()
        this.setAnomaliesNumber()
        this.setReportsNumber()
        this.setUserId()
        this.setProfileImage()
    }

    private fun setupDialog(){
        progressDialog = ProgressDialog(currentContext!!)

        nameTW = findViewById(R.id.account_name)
        emailTW = findViewById(R.id.account_emailid_text)
        reportsTW = findViewById(R.id.account_segnalazioni_number)
        reviewsTW = findViewById(R.id.account_recensioni_number)
        userIDTW = findViewById(R.id.account_id_text)
        profileImageIV = findViewById(R.id.account_icon)
        editIDBtn = findViewById(R.id.account_emailid_edit)
        editImageBtn = findViewById(R.id.account_camera)
    }

    private fun setupListeners(){
        logoutBtn = findViewById(R.id.account_logout_btn)
        logoutBtn.setOnClickListener{
            Utility.oneLineDialog(this, getString(R.string.logout_confirm)) { doLogout() }
        }
    }

    private fun doLogout(){
        LoginHandler.doLogout()
        Utility.showToast(this, getString(R.string.logout_success))
        Utility.goToMainMenu(this)
    }

    private fun setProfileName() {
        nameTW.text = LoginHandler.getFullName(LoginHandler.deviceUser!!)
    }

    private fun setProfileEmail() {
        emailTW.text = LoginHandler.deviceUser!!.uniqueId
        //editIDBtn.visibility = if(LoginHandler.isFacebookLoggedIn()) View.INVISIBLE else View.VISIBLE
    }

    private fun setAnomaliesNumber() {
        FirebaseClass.getAnomaliesByUser(
            LoginHandler.deviceUser!!,
            object : RunnablePar {
                override fun run(p: Any?) {
                    reportsTW.text = (p as Int).toString()
                    progressDialog.progress = progressDialog.progress + singleStepValue
                }
            }
        )
    }

    private fun setReportsNumber() {
        FirebaseClass.getReportsByUser(
            LoginHandler.deviceUser!!,
            object : RunnablePar {
                override fun run(p: Any?) {
                    reviewsTW.text = (p as Int).toString()
                    progressDialog.progress = progressDialog.progress + singleStepValue
                }
            }
        )
    }

    private fun setUserId() {
        FirebaseClass.getUserSnapshotId<User>(
            LoginHandler.deviceUser!!.uniqueId,
            object : RunnablePar {
                override fun run(p: Any?) {
                    if(p == null) return

                    val userId = p as String
                    userIDTW.text = userId
                    progressDialog.progress = progressDialog.progress + singleStepValue
                }
            })
    }

    private fun setProfileImage() {
        FirebaseClass.getProfileImage(
            LoginHandler.deviceUser!!,
            object : RunnablePar {
                override fun run(p: Any?) {
                    if(p != null)
                        profileImageIV.setImageBitmap(p as Bitmap)

                    progressDialog.progress = progressDialog.progress + singleStepValue
                }
            }
        )

        editImageBtn.visibility = if(LoginHandler.isFacebookLoggedIn()) View.INVISIBLE else View.VISIBLE
        editImageBtn.setOnClickListener {
            super.startGalleryActivity()
        }
    }

    private fun updateProfileImage(uploadedImage: Uri){
        FirebaseClass.getSnapshotFromUser(LoginHandler.deviceUser!!, object : RunnablePar{
            override fun run(p: Any?) {
                val dataSnapshot = p as DataSnapshot?
                if(dataSnapshot != null){
                    RegisterActivity.uploadProfilePicToFirebase(
                        this@MyAccountActivity,
                        uploadedImage,
                        dataSnapshot
                    ){
                        profileImageIV.setImageURI(uploadedImage)
                    }
                }

            }
        })
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        if (resultCode == RESULT_OK && reqCode == 100) {
            uploadedImage = data?.data
            updateProfileImage(uploadedImage!!)
        }
    }
}