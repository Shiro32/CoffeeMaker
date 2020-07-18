package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

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
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.SetTakeoutListener
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutEditActivity
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutRecyclerViewAdapter
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_takeout_list.*
import kotlinx.android.synthetic.main.fragment_takeout_list.view.*
import java.text.SimpleDateFormat

var isCalledFromBrewEditToTakeout: Boolean = false

class TakeoutFragment : Fragment(), SetTakeoutListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: TakeoutRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス
    private lateinit var sortList: Array<String>

    // リスト表示の準備
    // データを読み込んだり、メニューを作ったり、Viewをインフレートしたり
    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        val root = inflater.inflate(R.layout.fragment_takeout_list, container, false)

        // Brewの編集画面から呼ばれたかどうかを覚えておく
        isCalledFromBrewEditToTakeout = activity?.intent?.getStringExtra("from") == "Edit"

        // BREWからの参照を全部調べ上げて、TAKEOUTの各種参照情報を更新する
        // 評価、最終利用日、利用回数
        val takeRealm = Realm.getInstance(takeoutRealmConfig)
        val brewRealm = Realm.getInstance(brewRealmConfig)
        val takeouts = takeRealm.where<TakeoutData>().findAll()

        for( take in takeouts) {
            // 自分を参照しているBREWを全部拾う
            val brews = brewRealm.where<BrewData>().equalTo("takeoutID", take.id).findAll().sort("date", Sort.DESCENDING)
            if( brews.size>0 ) {
                // 最新利用日のセット
                val recent = brews[0]?.date
                // 利用側（BREW）での評価の算出
                var rate:Float = 0.0F
                for( b in brews)  rate += b.rating

                takeRealm.executeTransaction {
                    take.recent = recent
                    take.rating = rate / brews.size
                    take.count = brews.size
                }
            } else {
                // １回も利用が無かった場合・・・（涙）
                takeRealm.executeTransaction { take.recent = null }
            }
        }
        takeRealm.close()
        brewRealm.close()

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
//        setHasOptionsMenu(true)

        // ーーーーーーーーーー　ツールバー上のソートスピナーを作る　ーーーーーーーーーー
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        if( !isCalledFromBrewEditToTakeout ) {
            sortList = resources.getStringArray(R.array.sort_mode_takeout)
            val adapter =
                ArrayAdapter<String>(ac, android.R.layout.simple_spinner_dropdown_item, sortList)
            ac.sortSpn.adapter = adapter
            ac.sortSpn.onItemSelectedListener = SortSpinnerChangeListener()
        }

        Log.d("SHIRO", "takeout / onCreateView")
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

//        if (!isCalledFromBrewEditToTakeout) inflater?.inflate(R.menu.menu_opt_menu_1, menu)
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

        // 全部の外飲みデータをrealmResults配列に読み込む
        // 並び順ルールは、１：購入日（ＤＢ登録日）、２：最近の利用日（BREWからの参照）の順で行う
        // こうすることで、最近利用する商品や最近登録した商品が上にくるようになる（気が付くねぇ・・・）
        val realmResults: RealmResults<TakeoutData>

//        val realmResults = realm.where<TakeoutData>().findAll().sort("recent", Sort.DESCENDING)//.sort("first", Sort.DESCENDING)

        if( isCalledFromBrewEditToTakeout ) {
            realmResults = realm.where<TakeoutData>().findAll().sort("recent", Sort.DESCENDING)
        } else {
            val ma = activity as MainActivity
            when (ma.sortSpn.selectedItem.toString()) {
                sortList[0] -> {    // 使用日順（イマイチ悩ましい）
                    realmResults =
                        realm.where<TakeoutData>().findAll().sort("recent", Sort.DESCENDING)
                }
                sortList[1] -> {    // 評価順
                    realmResults = realm.where<TakeoutData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("rating", Sort.DESCENDING)
                }
                sortList[2] -> {     // 使用回数
                    realmResults = realm.where<TakeoutData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("count", Sort.DESCENDING)
                }
                sortList[3] -> {    // 購入店
                    realmResults = realm.where<TakeoutData>().findAll()
                        .sort("recent", Sort.DESCENDING)
                        .sort("chain", Sort.ASCENDING)
                }
                // TODO: 回数×金額でソートもしたいけど、ものすごく面倒？ いや簡単？
                else -> {
                    realmResults =
                        realm.where<TakeoutData>().findAll().sort("recent", Sort.DESCENDING)
                }
            }
        }

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

// 個別のテイクアウト商品ごとに、何回飲んだかをカウントする
// BREWデータベースを開いて、そこで、何度自分をリンクしているか数える
fun findTakeoutUseCount( takeout: TakeoutData ) : Int {
    val takeID = takeout.id

    // 摂取DBを開いて、外飲みIDの数を数える
    val brewRealm = Realm.getInstance(brewRealmConfig)
    val brews = brewRealm.where<BrewData>().equalTo("takeoutID", takeID).findAll()
    val counts = brews.size
    brewRealm.close()

    return counts
}


// テイクアウトの系列店名（TakeoutData.chain）を返す
fun findTakeoutChainNameByID( id: Long ): String {
    val realm = Realm.getInstance(takeoutRealmConfig)
    val bean = realm.where<TakeoutData>().equalTo("id",id).findFirst()
    var name = bean?.chain.toString()
    realm.close()

    if( name=="null" ) name=""
    return name
}

// テイクアウトの利用日をセットする（無茶苦茶やな・・・）
// テイクアウト（外飲み）ＤＢの全部のアイテムについて、実際に飲んだ日をBREWから探し出して最新の利用日をセットする
// ★★この関数、使ってないんだって・・・（onStartの中に書いちゃってる）
fun setTakeoutTakeDay() {
    val takeRealm = Realm.getInstance(takeoutRealmConfig)
    val brewRealm = Realm.getInstance(brewRealmConfig)

    val past = SimpleDateFormat("yyyy/MM/dd HH:mm:ss" ).parse("2000/01/01 00:00:00")

    // 全店飲みを読み込む
    val takeouts = takeRealm.where(TakeoutData::class.java).findAll()

    // 全店飲みについて処理する
    for( take in takeouts) {

        // BREWの中で自分を参照しているデータを日付ソートで全部拾う
        val brews = brewRealm.where<BrewData>().equalTo("takeoutID", take.id).findAll().sort("date", Sort.DESCENDING)

        if( brews.size>0 ) {
            // １回でも利用があった場合
            // 最新利用日のセット
            val recent = brews[0]?.date

            var rate:Float = 0.0F
            for( b in brews)  rate += b.rating

            takeRealm.executeTransaction {
                take.recent = recent
                take.rating = rate / brews.size
            }
        } else {
            // １回も利用していない場合（なんで登録してあるんだ？）
            takeRealm.executeTransaction { take.recent = null }
        }
    }

    takeRealm.close()
    brewRealm.close()
}






















