package com.simonediberardino.stradesicure.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.UI.ToastSS
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.Roles
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.login.LoginHandler.deviceUser
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility
import de.hdodenhof.circleimageview.CircleImageView

class AccountActivity : SSActivity() {
    private lateinit var userToShow: User
    private lateinit var nameTW: TextView
    private lateinit var emailTW: TextView
    private lateinit var reportsTW: TextView
    private lateinit var reviewsTW: TextView
    private lateinit var userIDTW: TextView
    private lateinit var roleTW: TextView
    private lateinit var roleEditBtn: View
    private lateinit var profileImageIV: CircleImageView
    private lateinit var logoutBtn: RadioButton
    private lateinit var editIDBtn: ImageView
    private lateinit var editImageBtn: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var segnalazioniBtn: View

    override fun initializeLayout() {
        this.setContentView(R.layout.activity_account)
        this.setupDialog()

        FirebaseClass.getUserObjectById<User>(
            intent.extras!!.getString("userToShow")!!,
            object : RunnablePar{
                override fun run(p: Any?) {
                    userToShow = p as User? ?: return
                    setProfileName()
                    setProfileEmail()
                    showRole()
                    setAnomaliesNumber()
                    setReportsNumber()
                    setupReportsBtn()
                    setUserId()
                    setProfileImage()
                    setupLogout()
                    setupRoles()
                    setupCamera()
                }
            })
    }

    private fun setupDialog(){
        nameTW = findViewById(R.id.account_name)
        emailTW = findViewById(R.id.account_emailid_text)
        reportsTW = findViewById(R.id.account_segnalazioni_number)
        reviewsTW = findViewById(R.id.account_recensioni_number)
        userIDTW = findViewById(R.id.account_id_text)
        roleTW = findViewById(R.id.account_role_text)
        roleEditBtn = findViewById(R.id.account_role_edit)
        profileImageIV = findViewById(R.id.account_icon)
        editIDBtn = findViewById(R.id.account_emailid_edit)
        editImageBtn = findViewById(R.id.account_camera)
        progressBar = findViewById(R.id.account_progressBar)
        segnalazioniBtn = findViewById(R.id.account_segnalazioni_btn)
    }

    private fun setupRoles(){
        if(deviceUser?.role?.isGreaterOr(Roles.AMMINISTRATORE) == true && deviceUser != userToShow){
            roleEditBtn.visibility = View.VISIBLE
            roleEditBtn.setOnClickListener {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(title)

                val availableRoles: Array<Roles> = Roles.values()

                builder.setItems(availableRoles.map { Utility.capitalizeFirstLetter(it.toString()) }.toTypedArray()) { _, which ->
                    FirebaseClass.updateUserRole(userToShow, Roles.values()[which]){
                        userToShow.role = Roles.values()[which]
                        showRole()
                    }
                }

                builder.create().show()
            }
        }
    }

    private fun setupLogout(){
        logoutBtn = findViewById(R.id.account_logout_btn)
        if(userToShow != deviceUser){
            logoutBtn.visibility = View.INVISIBLE
        }else{
            logoutBtn.setOnClickListener{
                Utility.oneLineDialog(this,
                    getString(R.string.logout_confirm),
                    getString(R.string.logout_description))
                { doLogout() }
            }
        }
    }

    private fun setupCamera(){
        editImageBtn.visibility =
            if(userToShow is FbUser) {
                View.INVISIBLE
            }else{
                if(userToShow != deviceUser && deviceUser?.role?.isGreaterOr(Roles.AMMINISTRATORE) == false || deviceUser?.role?.isGreaterOr(userToShow.role) == false)
                    View.INVISIBLE
                else
                    View.VISIBLE
            }

        editImageBtn.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(title)

            val options = resources.getStringArray(R.array.camera_options)

            builder.setTitle(getString(R.string.modifica_immagine))
            builder.setItems(options) { _, which ->
                when(which){
                    0 -> super.startGalleryActivity()
                    1 -> {
                        Utility.oneLineDialog(this,
                            getString(R.string.ripristina_immagine_conferma),
                            getString(R.string.azione_non_reversibile)
                        ){
                            FirebaseClass.getSnapshotFromUser(userToShow, object : RunnablePar {
                                @SuppressLint("UseCompatLoadingForDrawables")
                                override fun run(p: Any?) {
                                    val dataSnapshot = p!! as DataSnapshot
                                    FirebaseClass.deleteImageFromFirebase(this@AccountActivity, dataSnapshot){
                                        profileImageIV.setImageDrawable(getDrawable(R.drawable.com_facebook_profile_picture_blank_square))
                                    }
                                }
                            })
                        }
                    }
                }
            }

            builder.create().show()
        }
    }

    private fun doLogout(){
        LoginHandler.doLogout()
        ToastSS.show(this, getString(R.string.logout_success))
        Utility.goToMainMenu(this)
    }

    private fun setProfileName() {
        nameTW.text = userToShow.fullName
    }

    private fun setProfileEmail() {
        emailTW.text =
            if(deviceUser == userToShow || deviceUser?.role?.isGreaterOr(Roles.AMMINISTRATORE) == true)
                userToShow.uniqueId
            else
                getString(R.string.sconosciuto)
    }

    private fun showRole(){
        roleTW.text = Utility.capitalizeFirstLetter(userToShow.role.toString())
    }

    private fun setAnomaliesNumber() {
        FirebaseClass.getAnomaliesByUser(
            userToShow,
            object : RunnablePar{
                override fun run(p: Any?) {
                    reportsTW.text = (p as Int).toString()
                }
            }
        )
    }

    private fun setReportsNumber() {
        FirebaseClass.getReportsByUser(
            userToShow,
            object : RunnablePar{
                override fun run(p: Any?) {
                    reviewsTW.text = (p as Int).toString()
                }
            }
        )
    }

    private fun setupReportsBtn(){
        segnalazioniBtn.setOnClickListener {
            val intent = Intent(this, AnomaliesActivity::class.java)
            intent.putExtra("reporterId", userToShow.uniqueId)
            startActivity(intent)
        }
    }

    private fun setUserId() {
        FirebaseClass.getUserSnapshotId<User>(
            userToShow.uniqueId,
            object : RunnablePar{
                override fun run(p: Any?) {
                    if(p == null) return

                    val userId = p as String
                    userIDTW.text = userId
                }
            })
    }

    private fun setProfileImage() {
        FirebaseClass.getProfileImage(
            userToShow,
            object : RunnablePar{
                override fun run(p: Any?) {
                    if(p != null)
                        profileImageIV.setImageBitmap(p as Bitmap)
                    progressBar.visibility = View.INVISIBLE
                }
            }
        )
    }

    private fun updateProfileImage(uploadedImage: Uri){
        FirebaseClass.getSnapshotFromUser(userToShow, object : RunnablePar{
            override fun run(p: Any?) {
                val dataSnapshot = p as DataSnapshot?
                if(dataSnapshot != null){
                    RegisterActivity.uploadProfilePicToFirebase(
                        this@AccountActivity,
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