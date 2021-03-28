package com.sakuraweb.fotopota.coffeemaker.ui.brews

// 他のフラグメントへ移動するサンプル。ここから直接豆リストはないか？
//        root.button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.navigation_settings, null))
// これよりも、onClickイベントを発生させた方が良いみたい（＾＾）

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_brew_list.*
import kotlinx.android.synthetic.main.fragment_brew_list.view.*
import android.content.Intent as Intent

var brewListLayoutStyle: Int = 0

private lateinit var sortList: Array<String>

class BrewFragment : Fragment() {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BrewRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {

        // このfragment自身を指す。ボタンなどを指定するには、rootが必要
        val root = inflater.inflate(R.layout.fragment_brew_list, container, false)

        // ーーーーーーーーーー　表示項目のON/OFFをPreferenceから読んでおく　ーーーーーーーーーー
        PreferenceManager.getDefaultSharedPreferences(context).apply {
//            settingTermSw   = getBoolean("term_sw", true)
//            settingKmSw     = getBoolean("km_sw", true)

            brewListLayoutStyle = if( getString("list_sw", "") == "card" ) 0 else 1
        }

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        // realmのインスタンスを作る。Configはグローバル化してあるので、そのままインスタンスを作るだけ
        realm = Realm.getInstance(brewRealmConfig)

        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        root.brewFAB.setOnClickListener {
            val intent = Intent(activity, BrewEditActivity::class.java)
            intent.putExtra("mode", BREW_EDIT_MODE_NEW)
            startActivity(intent)
        }

        // ーーーーーーーーーー　ツールバーやメニューの装備　ーーーーーーーーーー
        // 「戻る」ボタン
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.title = getString(R.string.titleBrewList)
        ac.supportActionBar?.show()

        // メニュー構築（実装はonCreateOptionsMenu内で）
        // これを呼び出すことでfragmentがメニューを持つことを明示（https://developer.android.com/guide/components/fragments?hl=ja）
        // setHasOptionsMenu(true)

        // コンテキストメニューをセット
        // 長押しで編集メニューとか出せるけど、その操作方法はやめて、無難に編集画面からやるようにしているのでコメントアウト
//        registerForContextMenu(root)

        // ーーーーーーーーーー　ツールバー上のソートスピナーを作る　ーーーーーーーーーー
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        sortList = resources.getStringArray(R.array.sort_mode_brew)
        val adapter = ArrayAdapter<String>(ac, android.R.layout.simple_spinner_dropdown_item, sortList)
        ac.sortSpn.visibility = View.VISIBLE
        ac.sortSpn.adapter = adapter
        ac.sortSpn.onItemSelectedListener = SortSpinnerChangeListener()

        Log.d("SHIRO", "brew / onCreateView - DB OPEN")
        return root
    }

    // ソートSpinnerを変更した時のリスナ
    private inner class SortSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            // ここで何かしないと、Fragmentが更新できない
            // 何をしたらええねん？
            // TODO: とりあえずonStartを呼び出してるけど、多重で呼び出しているような・・・。
            onStart()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }

/*
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // コンテキストメニューをセット
        // どうやら、RecyclerViewには簡単にContextの利スタ設定ができないみたい・・・！
//        registerForContextMenu(brewRecycleView)
//        registerForContextMenu(sampleButton2)
    }


    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        // TODO: インフレーターが見当たらず、activityを使ったけど大丈夫か？
        activity?.menuInflater?.inflate(R.menu.menu_context_brew, menu)

    }
*/


    // オプションメニュー設置
    // Fragment内からActivity側のToolbarに無理矢理設置する
    // 本来は逆だけど、こうすればFragmentごとに違うメニューが作れる
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        //  inflater?.inflate(R.menu.menu_opt_menu_1, menu)

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

/*    // Details画面からの返事を処理
    // RESULT_TO_HOMEならホーム画面まで、
    // RESULT_TO_LISTならリスト画面まで（＝ここ）
    // だけどスルーされちゃうみたい・・・。
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        Toast.makeText(activity, "BREW-LIST-キャッチ！", Toast.LENGTH_SHORT).show()
        if( requestCode == REQUEST_CODE_SHOW_DETAILS) {
            when( resultCode ) {
                RESULT_TO_LIST -> {
                    Toast.makeText(activity, "TO_LIST", Toast.LENGTH_SHORT).show()
                }
                RESULT_TO_HOME -> {
                    Toast.makeText(activity, "TO_HOME", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }*/


    // ━━━━━━━━━　いよいよここでリスト表示　━━━━━━━━━
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        super.onStart()

        val ma = activity as MainActivity
        val realmResults: RealmResults<BrewData>

        when( ma.sortSpn.selectedItem.toString() ) {
            sortList[0] -> {    // 日付順
                realmResults = realm.where<BrewData>()
                    .findAll()
                    .sort("date", Sort.DESCENDING)
            }
            sortList[1] -> {    // 評価順
                realmResults = realm.where<BrewData>()
                    .findAll()
                    .sort("date", Sort.DESCENDING)
                    .sort("rating", Sort.DESCENDING)
            }
            sortList[2] -> {    // 使用豆
                realmResults = realm.where<BrewData>()
                    .equalTo("place", BREW_IN_HOME)
                    .findAll()
                    .sort("date", Sort.DESCENDING)
                    .sort("beansID", Sort.DESCENDING)
            }
            sortList[3] -> {    // メソッド順
                realmResults = realm.where<BrewData>()
                    .equalTo("place", BREW_IN_HOME)
                    .findAll()
                    .sort("date", Sort.DESCENDING)
                    .sort("methodID", Sort.DESCENDING)
            }
            sortList[4] -> {    // 外飲み除外
                realmResults = realm.where<BrewData>()
                    .equalTo("place", BREW_IN_HOME)
                    .findAll()
                    .sort("date", Sort.DESCENDING)
            }
            sortList[5] -> {    // 外飲みのみ
                realmResults = realm.where<BrewData>()
                    .equalTo("place", BREW_IN_SHOP)
                    .findAll()
                    .sort("date", Sort.DESCENDING)
                    .sort("beansID", Sort.DESCENDING)
            }
            else -> {
                realmResults = realm.where<BrewData>()
                    .findAll().sort("date", Sort.DESCENDING)
            }
        }

//        // 全部のコースデータをrealmResults配列に読み込む
//        val realmResults = realm.where<BrewData>()
//            .findAll().sort("date", Sort.DESCENDING)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        brewRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = BrewRecyclerViewAdapter( realmResults )
        brewRecycleView.adapter = this.adapter

//        // コンテキストメニューをセット
//        registerForContextMenu(brewRecycleView)

        Log.d("SHIRO", "brew / onStart - READ & Re-draw")
//        Toast.makeText(activity, "homeのonStart", Toast.LENGTH_SHORT).show()

    }

    // 終了処理
    // 特にきめがあって書いたわけではなく、HHCのコースセレクトダイアログに従って書いただけ見たい
    // そう考えると、書く位置を間違っているのかも
    // TODO: onStopの方がいいそうです（onDestroyは呼ばれないことがある）
    override fun onDestroy() {
        Log.d("SHIRO", "brew / onDestroy - DB CLOSE")

        super.onDestroy()
        realm.close()

//        Toast.makeText(activity, "homeのonDestroy", Toast.LENGTH_SHORT).show()
    }

}
