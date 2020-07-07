package com.sakuraweb.fotopota.coffeemaker.ui.home

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
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
import com.sakuraweb.fotopota.coffeemaker.toString
import com.sakuraweb.fotopota.coffeemaker.ui.brews.*
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_brew_list.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_home.view.copyRightText
import kotlinx.android.synthetic.main.fragment_home.view.ppText


// Home画面で表示する、カップ数、使い始めの日を算出する
var theFirstBrew: String = ""
var cupsFromTheFirstDay: Int = 0

fun calcCupsOfLife() {
    val realm = Realm.getInstance(brewRealmConfig)
    val brews = realm.where<BrewData>().findAll().sort("date", Sort.ASCENDING)

    if( brews.size>0 ) {
        val firstBrew = brews[0]?.date
        theFirstBrew = firstBrew?.toString("yyyy/MM/dd") as String

        cupsFromTheFirstDay = 0
        for (b in brews) {
            cupsFromTheFirstDay += b.cups.toInt()
        }
    } else {
        theFirstBrew = ""
        cupsFromTheFirstDay = 0
    }

    realm.close()
}


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
        root.cupsText.text = "%s cups".format(cupsFromTheFirstDay.toString())
        root.sinceText.text = theFirstBrew

        // copyrightメッセージにURLを埋め込む
        root.copyRightText.setText(Html.fromHtml("v1.1 Copyright ©2020 Shiro, <a href=\"http://fotopota.sakuraweb.com\">フォトポタ日記2.0</a>"))
        root.copyRightText.movementMethod = LinkMovementMethod.getInstance()

        // privacy policyにURLを埋め込む
        root.ppText.setText(Html.fromHtml("<a href=\"http://fotopota.sakuraweb.com/privacy-coffee.html\">プライバシーポリシー</a>"))
        root.ppText.movementMethod = LinkMovementMethod.getInstance()


        // 逆にツールバーを消したい・・・！ fragmentから
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.hide()

        // ツールバーやメニューの装備（ホームなのでメニュー無いけど）
//        val ac = activity as AppCompatActivity
//        ac.supportActionBar?.title = getString(R.string.app_name)

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
