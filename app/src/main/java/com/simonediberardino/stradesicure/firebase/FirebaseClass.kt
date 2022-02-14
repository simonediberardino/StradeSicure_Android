package com.simonediberardino.stradesicure.firebase

import android.graphics.BitmapFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.simonediberardino.stradesicure.activities.SSActivity
import com.simonediberardino.stradesicure.entity.Anomaly
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.misc.RunnablePar
import java.net.URL





object FirebaseClass {
    private var DB_REF = "https://strade-sicure-default-rtdb.europe-west1.firebasedatabase.app"
    private var STOR_REF = "gs://strade-sicure.appspot.com"

    val storageRef: StorageReference
        get() {
            return FirebaseStorage.getInstance(STOR_REF).reference
        }

    val dbRef: DatabaseReference
        get() {
            return FirebaseDatabase.getInstance(DB_REF).reference
        }

    val emailUsersRef: DatabaseReference
        get() {
            return FirebaseDatabase.getInstance(DB_REF).reference.child("EmailUsers")
        }

    val fbUsersRef: DatabaseReference
        get() {
            return FirebaseDatabase.getInstance(DB_REF).reference.child("FbUsers")
        }

    val anomaliesRef: DatabaseReference
        get() {
            return FirebaseDatabase.getInstance(DB_REF).reference.child("Anomalies")
        }

    fun getSpecificField(referString: String, path: String): DatabaseReference {
        return FirebaseDatabase.getInstance(referString).getReference(path)
    }

    fun addAnomalyToFirebase(anomaly: Anomaly){
        anomaliesRef.push().setValue(anomaly)
    }

    fun addEmailUserToFirebase(utente: EmailUser){
        addEmailUserToFirebase(utente, null)
    }

    fun addEmailUserToFirebase(utente: EmailUser, callback: RunnablePar?){
        emailUsersRef.push().setValue(utente).addOnCompleteListener {
            getSnapshotFromUser(
                utente,
                object : RunnablePar{
                    override fun run(p: Any?) {
                        callback?.run(p as DataSnapshot?)
                    }
                })
        }
    }

    fun addFbUserToFirebase(utente: FbUser){
        addFbUserToFirebase(utente, null)
    }

    fun addFbUserToFirebase(utente: FbUser, callback: RunnablePar?){
        fbUsersRef.push().setValue(utente).addOnCompleteListener { callback?.run(it) }
    }

    fun getReferenceByUser(user: User): DatabaseReference {
        return if(user is EmailUser) emailUsersRef else fbUsersRef
    }

    inline fun <reified T: User> getReferenceByUser(): DatabaseReference {
        return if(T::class.java == EmailUser::class.java) emailUsersRef else fbUsersRef
    }

    fun getAnomaliesByUser(user: User, callback: RunnablePar) {
        anomaliesRef.get().addOnCompleteListener { dataSnapshot ->
            val count = dataSnapshot.result.children.count {
                it.child("spotterId").value.toString().equals(user.uniqueId, ignoreCase = true)
            }

            callback.run(count)
        }
    }

    fun getReportsByUser(user: User, callback: RunnablePar) {
        anomaliesRef.get().addOnCompleteListener { dataSnapshot ->
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

    inline fun <reified T: User> getUserSnapshotId(userId: String, callback: RunnablePar){
        getUserSnapshotById<T>(userId, object : RunnablePar{
            override fun run(p: Any?) {
                callback.run((p as DataSnapshot?)?.ref?.key)
            }
        })
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

    fun getAnomalySnapshot(anomaly: Anomaly, callback: RunnablePar){
        anomaliesRef.get().addOnCompleteListener { dataSnapshot ->
            callback.run(dataSnapshot.result.children.find{
                it.getValue(Anomaly::class.java)!!.location == anomaly.location
            })
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

    fun getFBProfileImage(user: FbUser, callback: RunnablePar){
        getImageFromUrl("https://graph.facebook.com/${user.uniqueId}/picture?type=large", callback)
    }

    fun getProfileImage(user: User, callback: RunnablePar){
        if(user is FbUser){
            getFBProfileImage(user, callback)
            return
        }

        getUserSnapshotId<User>(
            user.uniqueId,
            object : RunnablePar{
                override fun run(p: Any?) {
                    val userId = p as String?

                    if(userId != null){
                        storageRef.child("images/${userId}")
                            .downloadUrl
                            .addOnSuccessListener {
                                getImageFromUrl(it.toString(), callback)
                            }.addOnFailureListener {
                                callback.run(null)
                            }
                    }
                }

            }
        )
    }

    fun getImageFromUrl(imageUrl: String, callback: RunnablePar){
        Thread{
            val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openConnection().getInputStream())
            SSActivity.currentContext?.runOnUiThread {
                callback.run(bitmap)
            }
        }.start()
    }

    fun <T> editFieldFirebase(referString: String, field: String, child: String, value: T) {
        val update = getSpecificField(referString, field)
        update.child(child).setValue(value)
    }

    fun deleteAnomalyFirebase(anomaly: Anomaly){
        getAnomalySnapshot(anomaly, object : RunnablePar{
            override fun run(p: Any?) {
                deleteFieldFirebase(anomaliesRef, (p as DataSnapshot?)?.ref?.key)
            }
        } )
    }

    fun deleteFieldFirebase(reference: DatabaseReference, child: String?) {
        if(child != null)
            reference.child(child).removeValue()
    }
}