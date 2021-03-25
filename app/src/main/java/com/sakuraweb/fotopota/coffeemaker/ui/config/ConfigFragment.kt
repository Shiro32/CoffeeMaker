package com.sakuraweb.fotopota.coffeemaker.ui.config

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sakuraweb.fotopota.coffeemaker.R

class ConfigFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.root_preferences)
    }
}
