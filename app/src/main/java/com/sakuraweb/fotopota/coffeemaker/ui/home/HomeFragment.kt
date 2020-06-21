package com.sakuraweb.fotopota.coffeemaker.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.ui.brews.*
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_brew_list.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {
//    private lateinit var dashboardViewModel: BeansViewModel

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
/*
        dashboardViewModel = ViewModelProviders.of(this).get(BeansViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_beans_list, container, false)
        val textView: TextView = root.findViewById(R.id.text_dashboard)
        dashboardViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })
*/

        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        root.button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.navigation_dashboard, null))

        calcCupsOfLife()
        root.cupsText.text = cupsFromTheFirstDay.toString() + "cups"
        root.sinceText.text = theFirstBrew

        // ツールバーやメニューの装備（ホームなのでメニュー無いけど）
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.title = getString(R.string.app_name)

//        ac.supportActionBar?.setDisplayShowHomeEnabled(true)
//        ac.supportActionBar?.setDisplayHomeAsUpEnabled(true)
//      置くことはできたけど、各フラグメントで動きの実装ができなくて断念

        Log.d("SHIRO", "home / onCreateView")
        return root
    }

    override fun onStart() {
        super.onStart()
        Log.d("SHIRO", "home / onStart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "home / onDestroy")
    }
}
