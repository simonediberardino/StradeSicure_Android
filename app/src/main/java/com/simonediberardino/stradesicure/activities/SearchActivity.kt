package com.simonediberardino.stradesicure.activities

import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.simonediberardino.stradesicure.R
import com.simonediberardino.stradesicure.entity.User
import com.simonediberardino.stradesicure.firebase.FirebaseClass
import com.simonediberardino.stradesicure.misc.RunnablePar
import com.simonediberardino.stradesicure.utils.Utility

class SearchActivity : SSActivity() {
    private lateinit var searchButton: View
    private lateinit var results: LinearLayout
    private lateinit var editText: EditText

    /**
     * @todo migliorare la ricerca
     */
    override fun initializeLayout() {
        setContentView(R.layout.activity_search)
        editText = findViewById(R.id.search_edit_text)
        results = findViewById(R.id.search_results)
        searchButton = findViewById(R.id.search_button)

        searchButton.setOnClickListener {
            val enteredText = editText.text.toString()
            results.removeAllViews()

            for(userReference: DatabaseReference in FirebaseClass.userReferences){
                Thread{
                    userReference
                        .orderByChild("fullName")
                        .startAt(enteredText)
                        .endAt(enteredText+"\uf8ff")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for(dataSnapshot: DataSnapshot in snapshot.children){
                                    runOnUiThread {
                                        showSingleUser(dataSnapshot.getValue(User::class.java)!!)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError){}
                        })
                }.start()
            }
        }
    }

    private fun showSingleUser(user: User){
        val parentLayoutId = R.id.search_results
        val parentViewId = R.id.parent
        val layoutToAddId = R.layout.single_user

        val inflater = LayoutInflater.from(this)
        val gallery: LinearLayout = findViewById(parentLayoutId)
        val view = inflater.inflate(layoutToAddId, gallery, false)
        val parentView = view.findViewById<ViewGroup>(parentViewId)

        val userNameTW = view.findViewById<TextView>(R.id.single_user_name)
        val userIconIW = view.findViewById<ImageView>(R.id.single_user_icon)
        val userRoleTW = view.findViewById<TextView>(R.id.single_user_role)
        val progressBar = view.findViewById<ProgressBar>(R.id.single_user_progressbar)

        userNameTW.text = user.fullName
        userRoleTW.text = Utility.capitalizeFirstLetter(user.role.toString())

        FirebaseClass.getProfileImage(user, object : RunnablePar{
            override fun run(p: Any?) {
                if(p != null)
                    userIconIW.setImageBitmap(p as Bitmap)
                progressBar.visibility = View.INVISIBLE
            }
        })

        view.setOnClickListener {
            val intent = Intent(this, AccountActivity::class.java)
            intent.putExtra("userToShow", user.uniqueId)
            startActivity(intent)
        }

        Utility.ridimensionamento(this, parentView)
        gallery.addView(view)
    }
}