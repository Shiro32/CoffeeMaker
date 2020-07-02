package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.takeoutRealmConfig
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.SetTakeoutListener
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutEditActivity
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutRecyclerViewAdapter
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_takeout_list.*
import kotlinx.android.synthetic.main.fragment_takeout_list.view.*

var isCalledFromBrewEditToTakeout: Boolean = false

class TakeoutFragment : Fragment(), SetTakeoutListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: TakeoutRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        val root = inflater.inflate(R.layout.fragment_takeout_list, container, false)

//      呼び出し元の検知方法。どこかで出番があるかも
//        if( activity?.intent?.getStringExtra("from") == "Edit" ) {
//            root.callFromText.text = "Edit画面から呼ばれました"
//        } else {
//            root.callFromText.text = "ナビゲーションからだと思います・・・。"
//        }

        // Brewの編集画面から呼ばれたかどうかを覚えておく
        isCalledFromBrewEditToTakeout = activity?.intent?.getStringExtra("from") == "Edit"

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        // realmのインスタンスを作る。ConfigはStartupで設定済み
        realm = Realm.getInstance(takeoutRealmConfig)


        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        root.takeoutFAB.setOnClickListener {
            val intent = Intent(activity, TakeoutEditActivity::class.java)
            startActivity(intent)
        }

        // ーーーーーーーーーー　ツールバーやメニューの装備　ーーーーーーーーーー
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.show()

        // Edit経由のTitleはうまくできない・・・。
        // なのでデフォでEdit経由をセットしておき、Navi経由はここで書き換える
        if( !isCalledFromBrewEditToTakeout ) {
//            ac.supportActionBar?.title =ac.getString(R.string.takeoutListFromBtnvTitle)
            ac.supportActionBar?.title = getString(R.string.titleTakeoutListFromBtnv)
        }

        // メニュー構築（実装はonCreateOptionsMenu内で）
        setHasOptionsMenu(true)

        Log.d("SHIRO", "takeout / onCreateView")
        return root
    }

    // オプションメニュー設置
    // Fragment内からActivity側のToolbarに無理矢理設置する
    // 本来は逆だけど、こうすればFragmentごとに違うメニューが作れる
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (!isCalledFromBrewEditToTakeout) inflater?.inflate(R.menu.menu_opt_menu_1, menu)
    }

    // オプションメニュー対応
    // あまり項目がないけど（ホームのみ？）
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId ) {
            R.id.optMenu1ItemHome -> {
                activity?.nav_view?.selectedItemId = R.id.navigation_home
            }
        }

        return super.onOptionsItemSelected(item)
    }

    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        super.onStart()

        // 全部の豆データをrealmResults配列に読み込む
        val realmResults = realm.where(TakeoutData::class.java).findAll().sort("id", Sort.DESCENDING)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        takeoutRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = TakeoutRecyclerViewAdapter(realmResults, this)
        takeoutRecycleView.adapter = this.adapter

        Log.d("SHIRO", "takeout / onStart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "takeout / onDestroy")
    }

    override fun okBtnTapped(ret: TakeoutData?) {
        val intent = Intent()
        intent.putExtra("id", ret?.id )
        intent.putExtra( "name", ret?.name )

        activity?.setResult(RESULT_OK, intent)
        activity?.finish()
    }

}



fun findTakeoutNameByID( id: Long ): String {
    val realm = Realm.getInstance(takeoutRealmConfig)
    val bean = realm.where<TakeoutData>().equalTo("id",id).findFirst()
    var name = bean?.name.toString()
    realm.close()

    if( name=="null" ) name="データなし"
    return name
}

