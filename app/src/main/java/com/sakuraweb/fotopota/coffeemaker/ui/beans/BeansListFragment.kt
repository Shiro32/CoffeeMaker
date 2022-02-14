package com.sakuraweb.fotopota.coffeemaker. ui.beans

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
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
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansEditActivity
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansRecyclerViewAdapter
import com.sakuraweb.fotopota.coffeemaker.ui.beans.SetBeansListener
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_beans_list.*
import kotlinx.android.synthetic.main.fragment_beans_list.view.*
import kotlinx.android.synthetic.main.fragment_brew_list.*
import java.util.*

var isCalledFromBrewEditToBeans: Boolean = false
var beansListStyle: Int = 0

// クラス内に書くとCreateのたびに初期化される。ここに書かないといけない
private var beansRecyclerPosition: Int = 0

class BeansFragment : Fragment(), SetBeansListener {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BeansRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス
    private lateinit var realmResults: RealmResults<BeansData>

    private lateinit var sortList: Array<String>
    private var beansSpinPosition:Int = 0
    private lateinit var beansSpinSelectedItem: String

    private var beansFirstSortSpin: Boolean = true

    // 他のアクティビティに隠される前に、RecyclerViewの位置を保存する
    override fun onPause() {
        super.onPause()
        Log.d("SHIRO", "BEANS / onPause")
        beansRecyclerPosition = (beansRecycleView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    }

    override fun onStop() {
        super.onStop()
        Log.d("SHIRO", "BEANS / onStop")
    }

    // 他のアクテビティから戻ってきたときに、RecyclerViewの位置を復元する
    override fun onResume() {
        super.onResume()
        Log.d("SHIRO", "BEANS / onResume")
        (beansRecycleView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(beansRecyclerPosition, 0)
    }


    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        Log.d("SHIRO", "BEANS / onCreateView - DB open")

        val root = inflater.inflate(R.layout.fragment_beans_list, container, false)

        // ーーーーーーーーーー　表示項目のON/OFFをPreferenceから読んでおく　ーーーーーーーーーー
        PreferenceManager.getDefaultSharedPreferences(context as Context).apply {
//            settingTermSw   = getBoolean("term_sw", true)
//            settingKmSw     = getBoolean("km_sw", true)
//            settingKcalSw   = getBoolean("kcal_sw", true)
//            settingMemoSw   = getBoolean("memo_sw", true)
//            settingMenuSw   = getBoolean("menu_sw", true)
//            settingPlaceSw  = getBoolean("place_sw", true)
            beansListStyle = if( getString("list_sw", "") == "card" ) R.layout.one_beans_card else R.layout.one_beans_flat
        }

        // Brewの編集画面から呼ばれたかどうかを覚えておく
        isCalledFromBrewEditToBeans = activity?.intent?.getStringExtra("from") == "Edit"

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        realm = Realm.getInstance(beansRealmConfig)

        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        root.beansFAB.setOnClickListener {
            val intent = Intent(activity, BeansEditActivity::class.java)
            startActivity(intent)
        }

        // スピナーの設定
        // スピナーは親Activityにあり、このタイミングではまだ存在していない（！）
        // なので定数だけ準備しておくにとどめる
        sortList = resources.getStringArray(R.array.sort_mode_beans)
        beansSpinPosition = 0
        beansSpinSelectedItem = sortList[beansSpinPosition]

        return root
    }

    // 豆の被使用状況（回数・評価）を更新
    private fun updateBeansUsage() {
        // BREWからの参照を全部調べ上げて、BEANSの各種被参照情報を更新する
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

                // 当該豆情報を更新
                beansRealm.executeTransaction {
                    if( recent!=null ) bean.recent = recent
                    bean.rating = rate / brews.size
                    bean.count = brews.size
                }
            } else {
                // １回も利用が無かった場合・・・（涙）
                beansRealm.executeTransaction {
                    bean.rating = 0.0F
                    bean.count = 0
                }
            }
        }
        beansRealm.close()
        brewRealm.close()
    }

    // ソートSpinnerを変更した時のリスナ
    private inner class SortSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            (activity as MainActivity).sortSpn.apply {
                beansSpinPosition = selectedItemPosition
                beansSpinSelectedItem = selectedItem.toString()

                if( beansFirstSortSpin ) {
                    Log.d("SHIRO", "BEANS / 偽Spinner")
                    beansFirstSortSpin = false
                    return
                }
            }
            Log.d("SHIRO", "BEANS / Spinner!")

            loadBeansData()
            adapter = BeansRecyclerViewAdapter( realmResults, this@BeansFragment )
            beansRecycleView.adapter = adapter
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

    // Realmから豆データを読み込む
    // それだけなんだけど、ソートが面倒なので関数
    private fun loadBeansData() {
        if( isCalledFromBrewEditToBeans ) {
            realmResults = realm.where<BeansData>().findAll().sort("repeatDate", Sort.DESCENDING)
        } else {
            when ( beansSpinSelectedItem ) {
                // 最新購入日順
                sortList[0] -> realmResults = realm.where<BeansData>().findAll().sort("repeatDate", Sort.DESCENDING)
                // 仕様日順
                sortList[1] ->  realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING)
                // 評価順
                sortList[2] -> realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING).sort("rating", Sort.DESCENDING)
                // 回数順
                sortList[3] -> realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING).sort("count", Sort.DESCENDING)
                // 購入店
                sortList[4] -> realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING).sort("shop", Sort.DESCENDING)
                // 金額
                sortList[5] -> realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING).sort("price", Sort.DESCENDING)
                // etc
                else -> realmResults = realm.where<BeansData>().findAll().sort("recent", Sort.DESCENDING)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("SHIRO", "BEANS / onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        beansRecycleView.layoutManager = layoutManager

        loadBeansData()
//        realmResults = realm.where<BeansData>().findAll()

        // アダプターを設定する
        adapter = BeansRecyclerViewAdapter(realmResults, this)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        beansRecycleView.adapter = this.adapter
    }




    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        Log.d("SHIRO", "BEANS / onStart")
        super.onStart()

        val ac = activity as AppCompatActivity
        ac.supportActionBar?.show()

        // Edit経由のTitleはうまくできない・・・。
        // なのでデフォでEdit経由をセットしておき、Navi経由はここで書き換える
        if( !isCalledFromBrewEditToBeans ) {
//            ac.supportActionBar?.title =ac.getString(R.string.beansListFromBtnvTitle)
            ac.supportActionBar?.title = getString(R.string.titleBeansListFromBtnv)
        }

        // 親Activityのスピナー（Sort）をここでセットする
        // スピナをセットすると自動的に１回呼ばれてしまう（＝Recyclerが初期化されちゃう）ので、フラグで回避
        if( !isCalledFromBrewEditToBeans ) {
            beansFirstSortSpin = true
            val adapter = ArrayAdapter<String>(ac, android.R.layout.simple_spinner_dropdown_item, sortList)
            ac.sortSpn.apply {
                visibility = View.VISIBLE
                this.adapter = adapter
                setSelection(beansSpinPosition)
                onItemSelectedListener = SortSpinnerChangeListener()
            }
        }

        // 豆の被使用状況を更新
        updateBeansUsage()

        beansRecycleView.adapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        Log.d( "SHIRO", "BEANS / onDestroy -- DB close")
        super.onDestroy()
        realm.close()
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

// beansIDから当該豆の最新購入日を返す
// 以前は最初の購入日だったのをv4から訂正
fun findBeansDateByID( id:Long): Date? {
    val realm = Realm.getInstance(beansRealmConfig)
    val bean = realm.where<BeansData>().equalTo("id",id).findFirst()
    var d1 = bean?.repeatDate

    realm.close()

    return d1
}
