package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.equipRealmConfig
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.equip.EquipData
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_equip_list.*

var isCalledFromBrewEditToEquip: Boolean = false
var equipListLayout: Int = 0

// コーヒー器具一覧を出すためのActivity
// 呼ばれ方は以下の2ルートで、どちらもActivityから呼ばれる
// １．BrewList > BrewDetail > BrewEdit > EquipList
// ２．Config > EquipList
// 違いはタイトルくらいかな？

class EquipListActivity : AppCompatActivity(), SetEquipListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: EquipRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス
    private lateinit var sortList: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equip_list)

        // Configで設定したモードで、カード型かフラット型か決めてインフレート
        PreferenceManager.getDefaultSharedPreferences(applicationContext).apply {
            equipListLayout = if( getString("list_sw", "") == "card" ) R.layout.one_equip_card else R.layout.one_equip_card
        }

        // Brewの編集画面から呼ばれたかどうかを覚えておく （trueじゃなければConfigからの呼び出し）
        isCalledFromBrewEditToEquip = intent.getStringExtra("from") == "Edit"

        // BREWからの参照を全部調べ上げて、EQUIPの各種参照情報を更新する（要らない気もするが・・・）
        // 評価、最終利用日、利用回数
        val equipRealm = Realm.getInstance(equipRealmConfig)
        val brewRealm = Realm.getInstance(brewRealmConfig)
        val equips = equipRealm.where<EquipData>().findAll()

        for( equip in equips) {
            // 自分を参照しているBREWを全部拾う
            val brews = brewRealm.where<BrewData>().equalTo("methodID", equip.id).findAll().sort("date", Sort.DESCENDING)
            if( brews.size>0 ) {
                // 最新利用日のセット
                val recent = brews[0]?.date
                // 利用側（BREW）での評価の算出
                var rate:Float = 0.0F
                for( b in brews)  rate += b.rating

                equipRealm.executeTransaction {
                    equip.recent = recent
                    equip.rating = rate / brews.size
                    equip.count = brews.size
                }
            } else {
                // １回も利用が無かった場合・・・（涙）
                equipRealm.executeTransaction { equip.recent = null }
            }
        }
        equipRealm.close()
        brewRealm.close()

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        // realmのインスタンスを作る。ConfigはStartupで設定済み
        realm = Realm.getInstance(equipRealmConfig)

        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        equipFAB.setOnClickListener {
            val intent = Intent(applicationContext, EquipEditActivity::class.java)
            startActivity(intent)
        }

        // ーーーーーーーーーー　ツールバーやメニューの装備　ーーーーーーーーーー
        setSupportActionBar(equipListToolbar)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        supportActionBar?.title =
            getString( if(isCalledFromBrewEditToEquip) R.string.titleEquipListFromBrewEdit else R.string.titleEquipListFromBtnv )
    }

    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        super.onStart()

        // 全部の外飲みデータをrealmResults配列に読み込む
        // 並び順ルールは、１：購入日（ＤＢ登録日）、２：最近の利用日（BREWからの参照）の順で行う
        // こうすることで、最近利用する商品や最近登録した商品が上にくるようになる（気が付くねぇ・・・）
        val realmResults = realm.where<EquipData>().findAll().sort("recent", Sort.DESCENDING)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(applicationContext)
        equipRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = EquipRecyclerViewAdapter(realmResults, this)
        equipRecycleView.adapter = this.adapter
    }

    override fun okBtnTapped(ret: EquipData?) {
        val intent = Intent()
        intent.putExtra("id", ret?.id )
        intent.putExtra( "name", ret?.name )

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}