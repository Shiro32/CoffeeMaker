package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.equip.EquipData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.REQUEST_CODE_SHOW_TAKEOUT_DETAILS
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_equip_list.*
import java.util.*

// TODO:金属フィルターなど

var isCalledFromBrewEditToEquip: Boolean = false
var equipListLayout: Int = 0

// コーヒー器具一覧を出すためのActivity
// 呼ばれ方は以下の2ルートで、どちらもActivityから呼ばれる
// １．BrewList > BrewDetail > BrewEdit > EquipList
// ２．Config > EquipList
// 違いはタイトルくらいかな？

class EquipListActivity : AppCompatActivity(), SetEquipListener {
    // onCreateで開いて、onStopで閉めるため、グローバルにしておく
    private lateinit var realm: Realm
    private lateinit var adapter: EquipRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equip_list)

        // Configで設定したモードで、カード型かフラット型か決めてインフレート
        PreferenceManager.getDefaultSharedPreferences(applicationContext).apply {
            equipListLayout = if( getString("list_sw", "") == "card" ) R.layout.one_equip_card else R.layout.one_equip_flat
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
            val brews = brewRealm.where<BrewData>().equalTo("equipID", equip.id).findAll().sort("date", Sort.DESCENDING)
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

    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when( requestCode ) {
            REQUEST_CODE_EQUIP_EDIT -> {
                when (resultCode) {
                    RESULT_TO_HOME -> {
                        val intent = Intent()
                        setResult(RESULT_TO_HOME, intent)
                        finish()
                    }
                }
            }
        }
    }

    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        super.onStart()

        // TODO: 外飲みだけ特別先頭に持っていきたい
        // 全部の外飲みデータをrealmResults配列に読み込む
        // 並び順ルールは、１：購入日（ＤＢ登録日）、２：最近の利用日（BREWからの参照）の順で行う
        // こうすることで、最近利用する商品や最近登録した商品が上にくるようになる（気が付くねぇ・・・）
        val realmResults = realm.where<EquipData>().findAll().sort("recent", Sort.ASCENDING ).sort("date", Sort.DESCENDING)

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

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}

fun findEquipIconByID( id:Long): Int {
    val realm = Realm.getInstance(equipRealmConfig)
    val equip = realm.where<EquipData>().equalTo("id",id).findFirst()

    val icon = equip?.icon ?: 0
    realm.close()
    return icon
}

fun findEquipNameByID( id:Long): String {
    val realm = Realm.getInstance(equipRealmConfig)
    val equip = realm.where<EquipData>().equalTo("id",id).findFirst()

    val name = equip?.name ?: "未設定"
    realm.close()
    return name
}

