package com.sakuraweb.fotopota.coffeemaker. ui.beans

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
import com.sakuraweb.fotopota.coffeemaker.beansRealmConfig
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_beans_list.*
import kotlinx.android.synthetic.main.fragment_beans_list.view.*

var isCalledFromBrewEdit: Boolean = false

class BeansFragment : Fragment(), SetBeansListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BeansRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        val root = inflater.inflate(R.layout.fragment_beans_list, container, false)

//      呼び出し元の検知方法。どこかで出番があるかも
//        if( activity?.intent?.getStringExtra("from") == "Edit" ) {
//            root.callFromText.text = "Edit画面から呼ばれました"
//        } else {
//            root.callFromText.text = "ナビゲーションからだと思います・・・。"
//        }

        // Brewの編集画面から呼ばれたかどうかを覚えておく
        isCalledFromBrewEdit = activity?.intent?.getStringExtra("from") == "Edit"

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        // realmのインスタンスを作る。ConfigはStartupで設定済み
        realm = Realm.getInstance(beansRealmConfig)


        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        root.beansFAB.setOnClickListener {
            val intent = Intent(activity, BeansEditActivity::class.java)
            startActivity(intent)
        }

        // ーーーーーーーーーー　ツールバーやメニューの装備　ーーーーーーーーーー
        val ac = activity as AppCompatActivity

        // Edit経由のTitleはうまくできない・・・。
        // なのでデフォでEdit経由をセットしておき、Navi経由はここで書き換える
        if( !isCalledFromBrewEdit ) {
//            ac.supportActionBar?.title =ac.getString(R.string.beansListFromBtnvTitle)
            ac.supportActionBar?.title ="最近の豆一覧"
        }

        // メニュー構築（実装はonCreateOptionsMenu内で）
        setHasOptionsMenu(true)

        Log.d("SHIRO", "beans / onCreateView")
        return root
    }

    // オプションメニュー設置
    // Fragment内からActivity側のToolbarに無理矢理設置する
    // 本来は逆だけど、こうすればFragmentごとに違うメニューが作れる
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (!isCalledFromBrewEdit) inflater?.inflate(R.menu.menu_opt_menu_1, menu)
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
        val realmResults = realm.where(BeansData::class.java).findAll().sort("date", Sort.DESCENDING)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        beansRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = BeansRecyclerViewAdapter(realmResults, this)
        beansRecycleView.adapter = this.adapter

        Log.d("SHIRO", "beans / onStart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "beans / onDestroy")
    }

    override fun okBtnTapped(ret: BeansData?) {
        val intent = Intent()
        intent.putExtra("id", ret?.id )
        intent.putExtra( "name", ret?.name )

        activity?.setResult(RESULT_OK, intent)
        activity?.finish()
    }

}



fun findBeansNameByID( id: Long ): String {
    val realm = Realm.getInstance(beansRealmConfig)
    val bean = realm.where<BeansData>().equalTo("id",id).findFirst()
    var name = bean?.name.toString()
    realm.close()

    if( name=="null" ) name="none"
    return name
}
