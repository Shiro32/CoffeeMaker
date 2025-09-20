package com.sakuraweb.fotopota.coffeemaker.ui.home

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.*
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_brew_list.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_home.view.copyRightText
import kotlinx.android.synthetic.main.fragment_home.view.ppText
import java.util.*


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
            cupsFromTheFirstDay += b.cupsDrunk.toInt()
        }
    } else {
        theFirstBrew = ""
        cupsFromTheFirstDay = 0
    }

    realm.close()
}

fun calcCupsDrunkOfPeriod(place: Int, begin: Calendar, end:Calendar) : Int {

    // 期間の0時から23時まで全部カウント
    begin.set( begin.get(Calendar.YEAR), begin.get(Calendar.MONTH), 1, 0, 0, 0)
    // 日を月末指定したことで、グラフ表示で不具合が出ていたので、いじらないことにした
//    end.set( end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.getActualMaximum(Calendar.DATE), 23, 59, 59)
    end.set( end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DATE), 23, 59, 59)

    lateinit var brews: RealmResults<BrewData>

    val realm = Realm.getInstance(brewRealmConfig)

    when( place ) {
        BREW_IN_HOME -> {
            brews = realm.where<BrewData>()
                .between("date", begin.time, end.time)
                .equalTo("place", BREW_IN_HOME)
                .findAll()
        }
        BREW_IN_SHOP -> {
            brews = realm.where<BrewData>()
                .between("date", begin.time, end.time)
                .equalTo("place", BREW_IN_SHOP)
                .findAll()
        }
        BREW_IN_BOTH -> {
            brews = realm.where<BrewData>()
                .between("date", begin.time, end.time)
                .findAll()
        }
    }

    var cups:Int = 0
    for( b in brews ) cups += b.cupsDrunk.toInt()
    realm.close()

    return cups
}

// トップページで全期間でのコーヒーカップ数を表示するためだけの関数
// 全く無駄に、全部のBrewsデータベースをチェックしている
fun calcCupsOfMonth(y:Int, m:Int): Int {
    var begin: Date
    var end: Date

    var c = Calendar.getInstance()
    c.set(y,m,1)
    begin = c.time

    c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE))
    end = c.time

    val realm = Realm.getInstance(brewRealmConfig)
    val brews = realm.where<BrewData>().between("date", begin, end).findAll()
    var cups = 0
    for( b in brews ) cups += b.cupsDrunk.toInt()
    realm.close()

    return cups
}



class HomeFragment : Fragment() {
//    private lateinit var dashboardViewModel: BeansViewModel

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        Log.d("SHIRO", "HOME / onCreateView")

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
        root.copyRightText.setText(Html.fromHtml("v4.00 ©2024 Shiro, <a href=\"http://fotopota.sakuraweb.com\">フォトポタ日記2.0</a>"))
        root.copyRightText.movementMethod = LinkMovementMethod.getInstance()

        // privacy policyにURLを埋め込む
        root.ppText.setText(Html.fromHtml("<a href=\"http://fotopota.sakuraweb.com/privacy-coffee.html\">プライバシー\n\nポリシー</a>"))
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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("SHIRO", "HOME / onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        homeHistoryText.movementMethod = ScrollingMovementMethod()
    }

    override fun onStart() {
        Log.d("SHIRO", "HOME / onStart")
        super.onStart()

        // 逆にツールバーを消したい・・・！ fragmentから
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.hide()
    }

    override fun onDestroy() {
        Log.d("SHIRO", "HOME / onDestroy")
        super.onDestroy()
    }
}
