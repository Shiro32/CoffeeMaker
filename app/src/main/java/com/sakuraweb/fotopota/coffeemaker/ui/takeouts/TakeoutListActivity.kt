package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_takeout_list.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_takeout_list.*
import java.text.SimpleDateFormat

var isCalledFromBrewEditToTakeout: Boolean = false
var takeoutListLayout:Int = 0

// 外飲み一覧表を出すためのActivity
// 呼ばれ方は以下の2ルートで、どちらもActivityから呼ばれる
// １．BrewList > BrewDetail > BrewEdit > TakeoutList
// ２．Config > TakeoutList
// 違いはタイトルくらいかな？（外飲み選んで / 外飲みDB編集）

class TakeoutListActivity : AppCompatActivity(), SetTakeoutListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: TakeoutRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス
    private lateinit var sortList: Array<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takeout_list)

        // Configで設定したモードで、カード型かフラット型か決めてインフレート
        PreferenceManager.getDefaultSharedPreferences(applicationContext).apply {
            takeoutListLayout = if( getString("list_sw", "") == "card" ) R.layout.one_takeout_card else R.layout.one_takeout_flat
        }

        // Brewの編集画面から呼ばれたかどうかを覚えておく （trueじゃなければConfigからの呼び出し）
        isCalledFromBrewEditToTakeout = intent.getStringExtra("from") == "Edit"

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
        takeoutFAB.setOnClickListener {
            val intent = Intent(applicationContext, TakeoutEditActivity::class.java)
            startActivity(intent)
        }

        // ーーーーーーーーーー　ツールバーやメニューの装備　ーーーーーーーーーー
        setSupportActionBar(takeoutListToolbar)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.title =
            getString( if(isCalledFromBrewEditToTakeout) R.string.titleTakeoutListFromBrewEdit else R.string.titleTakeoutListFromBtnv )

        // ーーーーーーーーーー　ツールバー上のソートスピナーを作る　ーーーーーーーーーー
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        if( !isCalledFromBrewEditToTakeout ) {
            sortList = resources.getStringArray(R.array.sort_mode_takeout)
            val adapter =
                ArrayAdapter<String>(applicationContext, android.R.layout.simple_spinner_dropdown_item, sortList)
            sortSpn2.adapter = adapter
            sortSpn2.onItemSelectedListener = SortSpinnerChangeListener()
        }
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
            when (sortSpn2.selectedItem.toString()) {
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
        layoutManager = LinearLayoutManager(applicationContext)
        takeoutRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = TakeoutRecyclerViewAdapter(realmResults, this)
        takeoutRecycleView.adapter = this.adapter
    }

    override fun okBtnTapped(ret: TakeoutData?) {
        val intent = Intent()
        intent.putExtra("id", ret?.id )
        intent.putExtra( "name", ret?.name )

        setResult(Activity.RESULT_OK, intent)
        finish()
    }




    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_opt_menu_2, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // オプションメニュー対応
    // あまり項目がないけど（ホームのみ？）
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId ) {
            R.id.optMenu2ItemHome -> {
                // 新機能！ いろいろウィンドウ閉めながらHomeまで帰る！
                val intent = Intent()
                setResult( RESULT_TO_HOME, intent)
                finish()
            }

            R.id.optMenu2ItemCancel -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
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




