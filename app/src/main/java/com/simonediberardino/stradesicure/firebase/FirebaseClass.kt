package com.simonediberardino.stradesicure.firebase

import android.graphics.BitmapFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.simonediberardino.stradesicure.activities.AdaptedActivity
import com.simonediberardino.stradesicure.entity.Anomaly
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.misc.RunnablePar
import java.net.URL


object FirebaseClass {
    private var DB_REF = "https://strade-sicure-default-rtdb.europe-west1.firebasedatabase.app"
    private var STOR_REF = "gs://strade-sicure.appspot.com"
    fun isFirebaseStringValid(string: String?): Boolean {
        val tempString = string?.trim { it <= ' ' }
        return !((tempString == "null"
                || tempString?.isEmpty()!!
                || tempString.contains("."))
                || tempString.contains("#")
                || tempString.contains("$")
                || tempString.contains("[")
                || tempString.contains("]"))
    }

    fun getStorageRef(): StorageReference {
        return FirebaseStorage.getInstance(STOR_REF).reference

    }
    fun getDBRef(): DatabaseReference {
        return FirebaseDatabase.getInstance(DB_REF).reference
    }

    fun getEmailUsersRef() : DatabaseReference{
        return FirebaseDatabase.getInstance(DB_REF).reference.child("EmailUsers")
    }

    fun getFbUsersRef() : DatabaseReference{
        return FirebaseDatabase.getInstance(DB_REF).reference.child("FbUsers")
    }

    fun getAnomaliesRef() : DatabaseReference{
        return FirebaseDatabase.getInstance(DB_REF).reference.child("Anomalies")
    }

    fun getSpecificField(referString: String, path: String): DatabaseReference {
        return FirebaseDatabase.getInstance(referString).getReference(path)
    }

    fun addAnomalyToFirebase(anomaly: Anomaly){
        getAnomaliesRef().push().setValue(anomaly)
    }

    fun addEmailUserToFirebase(utente: EmailUser){
        getEmailUsersRef().push().setValue(utente)
    }

    fun addFbUserToFirebase(utente: FbUser){
        getFbUsersRef().push().setValue(utente)
    }

    fun getReferenceByUser(user: User): DatabaseReference {
        return if(user is EmailUser) getEmailUsersRef() else getFbUsersRef()
    }

    inline fun <reified T: User> getReferenceByUser(): DatabaseReference {
        return if(T::class.java == EmailUser::class.java) getEmailUsersRef() else getFbUsersRef()
    }

    fun getAnomaliesByUser(user: User, callback: RunnablePar) {
        getAnomaliesRef().get().addOnCompleteListener { dataSnapshot ->
            val count = dataSnapshot.result.children.count {
                it.child("spotterId").value.toString().equals(user.uniqueId, ignoreCase = true)
            }

            callback.run(count)
        }
    }

    fun getReportsByUser(user: User, callback: RunnablePar) {
        getAnomaliesRef().get().addOnCompleteListener { dataSnapshot ->
            val count = dataSnapshot.result.children.count {
                it.child("authorId").value.toString().equals(user.uniqueId, ignoreCase = true)
            }
            callback.run(count)
        }
    }

    fun getGenericUserSnapshotById(userId: String, callback: RunnablePar){
        getUserSnapshotById<EmailUser>(userId, callback)
        getUserSnapshotById<FbUser>(userId, callback)
    }

    inline fun <reified T: User> getUserSnapshotById(userId: String, callback: RunnablePar) {
        if(T::class.java == User::class.java) {
            getGenericUserSnapshotById(userId, callback)
            return
        }

        getReferenceByUser<T>().get().addOnCompleteListener { dataSnapshot ->
            val userFound = dataSnapshot.result.children.find {
                it.child("uniqueId").value.toString().equals(userId, ignoreCase = true)
            }

            callback.run(userFound)
        }
    }

    inline fun <reified T: User> getUserObjectById(userId: String, callback: RunnablePar) {
        getUserSnapshotById<T>(userId, object : RunnablePar {
            override fun run(p: Any?) {
                val snapshot = p as DataSnapshot?
                callback.run(snapshot?.getValue(T::class.java))
            }
        })
    }

    fun getSnapshotFromUser(user: User, callback: RunnablePar) {
        if(user is EmailUser)
            getUserSnapshotById<EmailUser>(user.uniqueId, callback)
        else
            getUserSnapshotById<FbUser>(user.uniqueId, callback)
    }

    fun getProfileImage(user: User, callback: RunnablePar){
        getStorageRef().child("images/${user.uniqueId}")
            .downloadUrl
            .addOnSuccessListener {
                getImageFromUrl(it.toString(), callback)
            }
    }

    fun getImageFromUrl(imageUrl: String, callback: RunnablePar){
        Thread{
            val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openConnection().getInputStream())
            AdaptedActivity.lastContext?.runOnUiThread {
                callback.run(bitmap)
            }
        }.start()
    }

    fun <T> editFieldFirebase(referString: String, field: String, child: String, value: T) {
        val update = getSpecificField(referString, field)
        update.child(child).setValue(value)
    }

    fun <T> deleteFieldFirebase(referString: String, field: String, child: String) {
        getSpecificField(referString, field).child(child).removeValue()
    }
}