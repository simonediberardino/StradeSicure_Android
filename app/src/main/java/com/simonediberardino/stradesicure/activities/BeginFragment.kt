package com.simonediberardino.stradesicure.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment


class BeginFragment : Fragment() {
    var contentViewId: Int? = null

    fun NumberFragment(){}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(contentViewId!!, container, false)
    }
}