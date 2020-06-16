package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.beansRealmConfig
import io.realm.Realm
import io.realm.kotlin.where

class BeansFragment : Fragment() {
//    private lateinit var dashboardViewModel: BeansViewModel

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
/*
        dashboardViewModel = ViewModelProviders.of(this).get(BeansViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_beans, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        dashboardViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })
*/

        val root = inflater.inflate(R.layout.fragment_beans, container, false)
//        root.button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.navigation_dashboard, null))

        Log.d("SHIRO", "beans / onCreateView")
        return root
    }

    override fun onStart() {
        super.onStart()
        Log.d("SHIRO", "beans / onStart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "beans / onDestroy")
    }
}



fun findBeansNameByID( id: Long ): String {
    val realm = Realm.getInstance(beansRealmConfig)
    val bean = realm.where<BeansData>().equalTo("id",id).findFirst()
    val name = bean?.name.toString()
    realm.close()

    return name
}

class BeansDataInit(
    var date: String,
    var name: String,
    var gram: Int,
    var roast: Int,
    var shop: String,
    var price: Int,
    var memo: String) {
}