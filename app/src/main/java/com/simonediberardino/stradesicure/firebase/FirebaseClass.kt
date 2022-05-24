package com.simonediberardino.stradesicure.firebase

import android.graphics.BitmapFactory
import androidx.core.text.isDigitsOnly
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.simonediberardino.stradesicure.activities.SSActivity
import com.simonediberardino.stradesicure.entity.*
import com.simonediberardino.stradesicure.login.LoginHandler
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility
import java.net.URL


object FirebaseClass {
    private var DB_REF = "https://strade-sicure-default-rtdb.europe-west1.firebasedatabase.app"
    private var STOR_REF = "gs://strade-sicure.appspot.com"

    val userReferences: Array<DatabaseReference>
        get() {
            return arrayOf(emailUsersRef, fbUsersRef)
        }

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
        if(!Utility.isInternetAvailable()) return

        anomaliesRef.push().setValue(anomaly)
    }

    fun addEmailUserToFirebase(utente: EmailUser){
        addEmailUserToFirebase(utente, null)
    }

    fun addEmailUserToFirebase(utente: EmailUser, callback: RunnablePar?){
        if(!Utility.isInternetAvailable()) return

        emailUsersRef.push().setValue(utente).addOnCompleteListener {
            getSnapshotFromUser(
                utente,
                object : RunnablePar(){
                    override fun run(p: Any?) {
                        callback?.run(p as DataSnapshot?)
                    }
                })
        }
    }

    fun addFbUserToFirebase(utente: FbUser){
        if(!Utility.isInternetAvailable()) return

        addFbUserToFirebase(utente, null)
    }

    fun addFbUserToFirebase(utente: FbUser, callback: RunnablePar?){
        if(!Utility.isInternetAvailable()) return

        fbUsersRef.push().setValue(utente).addOnCompleteListener { callback?.run(it) }
    }

    fun getReferenceByUser(user: User): DatabaseReference {
        return if(user is EmailUser) emailUsersRef else fbUsersRef
    }

    fun isFbUserById(userId: String): Boolean {
        return userId.isDigitsOnly()
    }

    fun isEmailUserById(userId: String): Boolean {
        return !isFbUserById(userId)
    }

    inline fun <reified T: User> getReferenceByUser(): DatabaseReference {
        return if(T::class.java == EmailUser::class.java) emailUsersRef else fbUsersRef
    }

    inline fun <reified T: User> updateUserRole(user: T, role: Roles, callback: Runnable?){
        if(!Utility.isInternetAvailable()) return

        getUserSnapshotId<T>(user.uniqueId, object : RunnablePar(){
            override fun run(p: Any?) {
                val key = p as String

                val updateRole = object : RunnablePar(){
                    override fun run(p: Any?) {
                        val databaseReference = p as DatabaseReference
                        databaseReference.child(key)
                            .child("role")
                            .setValue(role)
                            .addOnCompleteListener {
                                callback?.run()
                            }
                    }
                }

                executeIfIsChild(emailUsersRef, key, updateRole)
                executeIfIsChild(fbUsersRef, key, updateRole)
            }
        })

    }

    fun executeIfIsChild(databaseReference: DatabaseReference, child: String, callback: RunnablePar){
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.hasChild(child))
                    callback.run(databaseReference)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun getAnomaliesByUser(user: User, callback: RunnablePar) {
        if(!Utility.isInternetAvailable()) return

        anomaliesRef.get().addOnCompleteListener { dataSnapshot ->
            val count = dataSnapshot.result.children.count {
                it.child("spotterId").value.toString().equals(user.uniqueId, ignoreCase = true)
            }

            callback.run(count)
        }
    }

    fun getReportsByUser(user: User, callback: RunnablePar) {
        if(!Utility.isInternetAvailable()) return

        anomaliesRef.get().addOnCompleteListener { dataSnapshot ->
            val count = dataSnapshot.result.children.count {
                it.child("authorId").value.toString().equals(user.uniqueId, ignoreCase = true)
            }
            callback.run(count)
        }
    }

    fun getGenericUserSnapshotById(userId: String, callback: RunnablePar){
        if(!Utility.isInternetAvailable()) return

        if(isEmailUserById(userId))
            getUserSnapshotById<EmailUser>(userId, callback)
        else getUserSnapshotById<FbUser>(userId, callback)
    }

    inline fun <reified T: User> getUserSnapshotId(userId: String, callback: RunnablePar){
        if(!Utility.isInternetAvailable()) return

        getUserSnapshotById<T>(userId, object : RunnablePar() {
            override fun run(p: Any?) {
                callback.run((p as DataSnapshot?)?.ref?.key)
            }
        })
    }

    inline fun <reified T: User> getUserSnapshotById(userId: String, callback: RunnablePar) {
        if(!Utility.isInternetAvailable()) return

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
        if(!Utility.isInternetAvailable()) return

        anomaliesRef.get().addOnCompleteListener { dataSnapshot ->
            callback.run(dataSnapshot.result.children.find{
                it.getValue(Anomaly::class.java)!!.location == anomaly.location
            })
        }
    }

    fun getGenericUserObjectById(userId: String, callback: RunnablePar) {
        if(isEmailUserById(userId))
            getUserObjectById<EmailUser>(userId, callback)
        else getUserObjectById<FbUser>(userId, callback)
    }

    inline fun <reified T: User> getUserObjectById(userId: String, callback: RunnablePar) {
        if(!Utility.isInternetAvailable()) return

        getUserSnapshotById<T>(userId, object : RunnablePar() {
            override fun run(p: Any?) {
                val snapshot = p as DataSnapshot?
                callback.run(snapshot?.getValue(T::class.java))
            }
        })
    }

    fun getSnapshotFromUser(user: User, callback: RunnablePar) {
        when (user) {
            is EmailUser -> getUserSnapshotById<EmailUser>(user.uniqueId, callback)
            is FbUser -> getUserSnapshotById<FbUser>(user.uniqueId, callback)
            else -> getUserSnapshotById<User>(user.uniqueId, callback)
        }
    }

    fun getFBProfileImage(user: User, callback: RunnablePar){
        getImageFromUrl("https://graph.facebook.com/${user.uniqueId}/picture?access_token=${LoginHandler.accessToken?.token}", callback)
    }

    fun getProfileImage(user: User, callback: RunnablePar){
        if(!Utility.isInternetAvailable()) return

        if(user is FbUser){
            getFBProfileImage(user, callback)
            return
        }

        getUserSnapshotId<User>(
            user.uniqueId,
            object : RunnablePar(){
                override fun run(p: Any?) {
                    val userId = p as String?

                    if(userId != null){
                        storageRef.child("images/${userId}")
                            .downloadUrl
                            .addOnSuccessListener {
                                getImageFromUrl(it.toString(), callback)
                            }.addOnFailureListener {
                                getFBProfileImage(user, callback)
                            }
                    }
                }

            }
        )
    }

    fun getImageFromUrl(imageUrl: String, callback: RunnablePar){
        if(!Utility.isInternetAvailable()) return

        Thread{
            try{
                val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openConnection().getInputStream())
                SSActivity.currentContext.runOnUiThread {
                    callback.run(bitmap)
                }
            }catch (exception: Exception){
                SSActivity.currentContext.runOnUiThread {
                    callback.run(null)
                }
            }

        }.start()
    }

    fun <T> editFieldFirebase(referString: String, field: String, child: String, value: T) {
        if(!Utility.isInternetAvailable()) return

        val update = getSpecificField(referString, field)
        update.child(child).setValue(value)
    }

    fun deleteAnomalyFirebase(anomaly: Anomaly){
        if(!Utility.isInternetAvailable()) return

        getAnomalySnapshot(anomaly, object : RunnablePar(){
            override fun run(p: Any?) {
                deleteFieldFirebase(anomaliesRef, (p as DataSnapshot?)?.ref?.key)
            }
        } )
    }

    fun deleteAccountFirebase(account: User){
        if(!Utility.isInternetAvailable()) return

        getGenericUserSnapshotById(account.uniqueId, object : RunnablePar(){
            override fun run(p: Any?) {
                if(p == null) return

                val parent = (p as DataSnapshot).ref.parent!!.ref
                val child = p.ref.key

                deleteFieldFirebase(parent, child)
            }
        } )
    }

    fun deleteFieldFirebase(reference: DatabaseReference, child: String?) {
        if(child != null)
            reference.child(child).removeValue()
    }

    fun deleteImageFromFirebase(dataSnapshot: DataSnapshot, callback: Runnable){
        val storageReference = FirebaseStorage.getInstance().reference
        val reference: StorageReference = storageReference.child("images/" + dataSnapshot.ref.key)

        reference.delete()
            .addOnCompleteListener {
                callback.run()
            }
    }
}