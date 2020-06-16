package com.sakuraweb.fotopota.coffeemaker.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.sakuraweb.fotopota.coffeemaker.R

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        Log.d("SHIRO", "settings / onCreateView")
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "settings / onDestroy")
    }
}