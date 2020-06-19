package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.toString
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewRecyclerViewAdapter
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_brews.*
import kotlinx.android.synthetic.main.fragment_brews.view.*
import android.content.Intent as Intent

class BrewFragment : Fragment() {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BrewRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        // 当初は色々あったけど、全部削除した

        // このfragment自身を指す。ボタンなどを指定するには、rootが必要
        val root = inflater.inflate(R.layout.fragment_brews, container, false)

        // 他のフラグメントへ移動するサンプル。ここから直接豆リストはないか？
//        root.button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.navigation_dashboard, null))

        // ここまでが、よくわからん下部関係の動き？ AppBarも？
        // ここから下がリスト表示（RecyclerView）

        // realmのインスタンスを作る。Configはグローバル化してあるので、そのままインスタンスを作るだけ
        realm = Realm.getInstance(brewRealmConfig)

        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        root.brewFAB.setOnClickListener {
            val intent = Intent(activity, BrewEditActivity::class.java)
            startActivity(intent)
        }


        Log.d("SHIRO", "brew / onCreateView - DB OPEN")
        return root
    }



    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        super.onStart()

        // 全部のコースデータをrealmResults配列に読み込む
        val realmResults = realm.where(BrewData::class.java).findAll().sort("date", Sort.DESCENDING)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        brewRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = BrewRecyclerViewAdapter( realmResults )
        brewRecycleView.adapter = this.adapter

        Log.d("SHIRO", "brew / onStart - READ & Re-draw")
//        Toast.makeText(activity, "homeのonStart", Toast.LENGTH_SHORT).show()
    }

    // 終了処理
    // 特にきめがあって書いたわけではなく、HHCのコースセレクトダイアログに従って書いただけ見たい
    // そう考えると、書く位置を間違っているのかも
    override fun onDestroy() {
        Log.d("SHIRO", "brew / onDestroy - DB CLOSE")

        super.onDestroy()
        realm.close()

//        Toast.makeText(activity, "homeのonDestroy", Toast.LENGTH_SHORT).show()
    }

}

fun calcCupsOfLife() {
    val realm = Realm.getInstance(brewRealmConfig)
    val brews = realm.where<BrewData>().findAll().sort("date", Sort.ASCENDING)

    val firstBrew = brews[0]?.date
    theFirstBrew = firstBrew?.toString("yyyy/MM/dd") as String

    cupsFromTheFirstDay = 0
    for(b in brews) {
        cupsFromTheFirstDay += b.cups.toInt()
    }

    realm.close()
}

var theFirstBrew: String = ""
var cupsFromTheFirstDay: Int = 0
