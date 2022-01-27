package com.simonediberardino.stradesicure.activities

import android.graphics.Bitmap
import android.widget.RadioButton
import android.widget.TextView
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ProgressDialog
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility
import de.hdodenhof.circleimageview.CircleImageView

class MyAccountActivity : AdaptedActivity() {
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
        progressDialog = ProgressDialog(lastContext!!)

        nameTW = findViewById(R.id.account_name)
        emailTW = findViewById(R.id.account_email_text)
        reportsTW = findViewById(R.id.account_segnalazioni_number)
        reviewsTW = findViewById(R.id.account_recensioni_number)
        userIDTW = findViewById(R.id.account_id_text)
        profileImageIV = findViewById(R.id.account_icon)
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
        FirebaseClass.getUserSnapshotId<EmailUser>(
            LoginHandler.deviceUser!!.uniqueId,
            object : RunnablePar {
                override fun run(p: Any?) {
                    val userId = p as String?
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
    }
}