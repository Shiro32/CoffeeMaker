package com.sakuraweb.fotopota.coffeemaker. ui.beans

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_beans_list.*
import kotlinx.android.synthetic.main.fragment_beans_list.view.*
import java.util.*

var isCalledFromBrewEditToBeans: Boolean = false


class BeansFragment : Fragment(), SetBeansListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BeansRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス
    private lateinit var sortList: Array<String>

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        val root = inflater.inflate(R.layout.fragment_beans_list, container, false)

//      呼び出し元の検知方法。どこかで出番があるかも
//        if( activity?.intent?.getStringExtra("from") == "Edit" ) {
//            root.callFromText.text = "Edit画面から呼ばれました"
//        } else {
//            root.callFromText.text = "ナビゲーションからだと思います・・・。"
//        }

        // Brewの編集画面から呼ばれたかどうかを覚えておく
        isCalledFromBrewEditToBeans = activity?.intent?.getStringExtra("from") == "Edit"

        // BREWからの参照を全部調べ上げて、TAKEOUTの各種参照情報を更新する
        // 評価、最終利用日、利用回数
        val beansRealm = Realm.getInstance(beansRealmConfig)
        val brewRealm = Realm.getInstance(brewRealmConfig)
        val beans = beansRealm.where(BeansData::class.java).findAll()

        for( bean in beans) {
            // BREWの中で自分を参照しているデータを日付ソートで全部拾う
            val brews = brewRealm.where<BrewData>().equalTo("beansID", bean.id).findAll().sort("date", Sort.DESCENDING)
            if( brews.size>0 ) {
                // 最新利用日のセット
                val recent = brews[0]?.date
                // 利用側（BREW）での評価の算出
                var rate:Float = 0.0F
                for( b in brews)  rate += b.rating

                beansRealm.executeTransaction {
                    if( recent!=null ) bean.recent = recent
                    bean.rating = rate / brews.size
                    bean.count = brews.size
                }
            } else {
                // １回も利用が無かった場合・・・（涙）
                beansRealm.executeTransaction {
                    // 利用無し、をどう表現したらいいやら・・・。Non-nullだし。
                }
            }
        }
        beansRealm.close()
        brewRealm.close()

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
        ac.supportActionBar?.show()

        // Edit経由のTitleはうまくできない・・・。
        // なのでデフォでEdit経由をセットしておき、Navi経由はここで書き換える
        if( !isCalledFromBrewEditToBeans ) {
//            ac.supportActionBar?.title =ac.getString(R.string.beansListFromBtnvTitle)
            ac.supportActionBar?.title = getString(R.string.titleBeansListFromBtnv)
        }

        // メニュー構築（実装はonCreateOptionsMenu内で）
//        setHasOptionsMenu(true)

        // ーーーーーーーーーー　ツールバー上のソートスピナーを作る　ーーーーーーーーーー
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        if( !isCalledFromBrewEditToBeans ) {
            sortList = resources.getStringArray(R.array.sort_mode_beans)
            val adapter =
                ArrayAdapter<String>(ac, android.R.layout.simple_spinner_dropdown_item, sortList)
            ac.sortSpn.adapter = adapter
            ac.sortSpn.onItemSelectedListener = SortSpinnerChangeListener()
        }

        Log.d("SHIRO", "beans / onCreateView")
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

    // オプションメニュー設置
    // Fragment内からActivity側のToolbarに無理矢理設置する
    // 本来は逆だけど、こうすればFragmentごとに違うメニューが作れる
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

//        if (!isCalledFromBrewEditToBeans) inflater?.inflate(R.menu.menu_opt_menu_1, menu)
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
        val realmResults: RealmResults<BeansData>

        if( isCalledFromBrewEditToBeans ) {
            realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING)
        } else {
            val ma = activity as MainActivity
            when (ma.sortSpn.selectedItem.toString()) {
                sortList[0] -> {    // 使用日順
                    realmResults =
                        realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING)
                }
                sortList[1] -> {    // 購入日順
                    realmResults = realm.where<BeansData>().findAll().sort("date", Sort.DESCENDING)
                }
                sortList[2] -> {     // 評価順
                    realmResults = realm.where<BeansData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("rating", Sort.DESCENDING)
                }
                sortList[3] -> {    // 回数順
                    realmResults = realm.where<BeansData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("count", Sort.DESCENDING)
                }
                sortList[4] -> {    // 購入店
                    realmResults = realm.where<BeansData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("shop", Sort.DESCENDING)
                }
                sortList[5] -> {    // 金額
                    realmResults = realm.where<BeansData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("price", Sort.DESCENDING)
                }
                else -> {
                    realmResults =
                        realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING)
                }
            }
        }

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        beansRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = BeansRecyclerViewAdapter(
            realmResults,
            this
        )
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



fun findBeansNameByID( place:Int, beansID: Long, takeoutID: Long ): String {
    if (place == BREW_IN_HOME) {
        val realm = Realm.getInstance(beansRealmConfig)
        val bean = realm.where<BeansData>().equalTo("id", beansID).findFirst()
        var name = bean?.name.toString()
        realm.close()
        if (name != "null") return name else return "データなし"
    } else {
        val realm = Realm.getInstance(takeoutRealmConfig)
        val bean = realm.where<TakeoutData>().equalTo("id", takeoutID).findFirst()
        var name = bean?.name.toString()
        realm.close()
        if (name != "null") return name else return "データなし"
    }
}

fun findBeansDateByID( id:Long): Date? {
    val realm = Realm.getInstance(beansRealmConfig)
    val bean = realm.where<BeansData>().equalTo("id",id).findFirst()
    var d1 = bean?.date

    realm.close()

    return d1
}
