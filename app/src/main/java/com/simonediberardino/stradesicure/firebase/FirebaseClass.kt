package com.simonediberardino.stradesicure.firebase

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.simonediberardino.stradesicure.entity.Anomaly
import com.simonediberardino.stradesicure.entity.EmailUser
import com.simonediberardino.stradesicure.entity.FbUser
import com.simonediberardino.stradesicure.entity.User

object FirebaseClass {
    private var DB_REF = "https://strade-sicure-default-rtdb.europe-west1.firebasedatabase.app"

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

    fun addEmailUserToFirebase(email: String, utente: EmailUser){
        getEmailUsersRef().child(email).setValue(utente)
    }

    fun addFbUserToFirebase(userId: String, utente: FbUser){
        getFbUsersRef().child(userId).setValue(utente)
    }

    fun addUserToFirebase(email: String, utente: User?) {
        getDBRef().child(email).setValue(utente)
    }

    fun <T> editFieldFirebase(referString: String, field: String, child: String, value: T) {
        val update = getSpecificField(referString, field)
        update.child(child).setValue(value)
    }

    fun <T> deleteFieldFirebase(referString: String, field: String, child: String) {
        getSpecificField(referString, field).child(child).removeValue()
    }
}