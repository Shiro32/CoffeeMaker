package com.sakuraweb.fotopota.coffeemaker.ui.brews

// 他のフラグメントへ移動するサンプル。ここから直接豆リストはないか？
//        root.button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.navigation_settings, null))
// これよりも、onClickイベントを発生させた方が良いみたい（＾＾）

import android.content.Context
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

var brewListLayoutStyle: Int = CARD_STYLE
var configMillUnit: Int = GRIND_UNIT_FLOAT

// 各種表示設定（設定画面で設定したものの保持用）
var configMillMax = 10F
var configMillMin = 0F
var configMillDispSw = true

var configSteamTimeSw = true
var configSteamTimeMax = 60F
var configSteamTimeMin = 0F
var configSteamTimeDispSw = true

var configBrewTimeSw = true
var configBrewTimeMax = 120F
var configBrewTimeMin = 0F
var configBrewTimeDispSw = true

var configWaterVolumeSw = true
var configWaterVolumeMax = 300F
var configWaterVolumeDispSw = true

var configTempSw = true
var configTempMax = 120F
var configTempMin = 0F
var configTempDispSw = true

var configCBRSw = true
var configCBRDispSw = true

var configBeansSw = true
var configBeansDispSw = true

var configMilkSw = true
var configMilkDispSw = false

var configSugarSw = true
var configSugarDispSw = false

var configCupsBrewedSw = true
var configCupsBrewedDispSw = false

var configCupsDrunkSw = true
var configCupsDrunkDispSw = false

var configWaterVolumeUnit = "cc"

lateinit var grind2Labels: Array<String>

// クラス内に書くとCreateのたびに初期化される。ここに書かないといけない
private var brewRecyclerPosition: Int = 0


// Sleep中にアクティビティがKillされることがあるので、リロード簡単にできるよう、関数化した
// メモリの少ないRakuten miniでは頻発する模様・・・。
fun readBrewConfig( context: Context ) {
    // ーーーーーーーーーー　表示項目のON/OFFをPreferenceから読んでおく　ーーーーーーーーーー
    PreferenceManager.getDefaultSharedPreferences(context).apply {
        getString("water_volume_max", "300")?.let { configWaterVolumeMax = it.toFloat() }
//        getString("water_volume_min", "0")?.let { configWaterVolumeMin = it.toFloat() }
        configWaterVolumeSw     = getBoolean("water_volume_sw", true)
        configWaterVolumeDispSw = getBoolean("water_volume_disp_sw", true)

        getString("mill_min", "00")?.let { configMillMin = it.toFloat() }
        getString("mill_max", "20")?.let { configMillMax = it.toFloat() }
        configMillUnit = if( getString("mill_unit_sw", "") == "int" ) GRIND_UNIT_INT else GRIND_UNIT_FLOAT
        configMillDispSw = getBoolean("mill_disp_sw", true)

        getString("steam_min", "00")?.let { configSteamTimeMin = it.toFloat() }
        getString("steam_max", "60")?.let { configSteamTimeMax = it.toFloat() }
        configSteamTimeSw       = getBoolean("steam_sw", true)
        configSteamTimeDispSw   = getBoolean("steam_disp_sw", true)

        getString("brew_min", "000")?.let { configBrewTimeMin = it.toFloat() }
        getString("brew_max", "120")?.let { configBrewTimeMax = it.toFloat() }
        configBrewTimeSw        = getBoolean("brew_sw", true)
        configBrewTimeDispSw    = getBoolean("brew_disp_sw",true)

        getString("temp_min", "0")?.let { configTempMin = it.toFloat() }
        getString("temp_max", "120")?.let { configTempMax = it.toFloat() }
        configTempSw            = getBoolean("temp_sw", true)
        configTempDispSw        = getBoolean("temp_disp_sw",true)

        configCBRSw             = getBoolean( "cbr_sw", true)
        configCBRDispSw         = getBoolean( "cbr_disp_sw", true)

        configBeansSw           = getBoolean("beans_sw", true)
        configBeansDispSw       = getBoolean("beans_disp_sw", true)

        configMilkSw            = getBoolean("milk_sw", true)
        configMilkDispSw        = getBoolean("milk_disp_sw", false)

        configSugarSw           = getBoolean("sugar_sw", true)
        configSugarDispSw       = getBoolean("sugar_disp_sw", false)

        configCupsBrewedSw      = getBoolean("cups_brewed_sw", true)
        configCupsBrewedDispSw  = getBoolean("cups_brewed_disp_sw", false)

        configCupsDrunkSw       = getBoolean("cups_drunk_sw", true)
        configCupsDrunkDispSw   = getBoolean("cups_drunk_disp_sw", false)

        brewListLayoutStyle = if( getString("list_sw", "") == "card" ) CARD_STYLE else FLAT_STYLE
        grind2Labels = arrayOf( "0", configMillMax.toInt().toString() )

        getString("water_volume_unit", "cc")?.let { configWaterVolumeUnit = it } // 水の使用量の単位（カップ、cc）
    }
}

class BrewFragment : Fragment() {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BrewRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス
    private lateinit var realmResults: RealmResults<BrewData>

    private lateinit var sortList: Array<String>
    private var brewSpinPosition:Int = 0
    private lateinit var brewSpinSelectedItem: String

    private var brewFirstSortSpin: Boolean = true

/*
    // 回転時（＝Activity再構築）に備えて保存する
    // たいていのViewは保存されるけど、画像とかは自分でやらないとあかん
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d( "SHIRO", "BREW / onSaveInstanceState")

        brewRecyclerPosition = (brewRecycleView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
    }
*/

    // 他のアクテビティから戻ってきたときに、RecyclerViewの位置を復元する
    override fun onResume() {
        super.onResume()
        (brewRecycleView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(brewRecyclerPosition, 0)
        Log.d("SHIRO", "BREW / onResume (POS:$brewRecyclerPosition)")
    }

    // 他のアクティビティに隠される前に、RecyclerViewの位置を保存する
    override fun onPause() {
        super.onPause()
        brewRecyclerPosition = (brewRecycleView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        Log.d("SHIRO", "BREW / onPause (POS:$brewRecyclerPosition)")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {
        Log.d("SHIRO", "BREW / onCreateView - DB OPEN")

        // このfragment自身を指す。ボタンなどを指定するには、rootが必要
        val root = inflater.inflate(R.layout.fragment_brew_list, container, false)

        // ーーーーーーーーーー　表示項目のON/OFFをPreferenceから読んでおく　ーーーーーーーーーー
        readBrewConfig( context as Context )

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        // realmのインスタンスを作る。Configはグローバル化してあるので、そのままインスタンスを作るだけ
        realm = Realm.getInstance(brewRealmConfig)

        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        root.brewFAB.setOnClickListener {
            val intent = Intent(activity, BrewEditActivity::class.java)
            intent.putExtra("mode", BREW_EDIT_MODE_NEW)
            startActivity(intent)
        }

        // スピナーの設定
        // スピナーは親Activityにあり、このタイミングではまだ存在していない（！）
        // なので定数だけ準備しておくにとどめる
        sortList = resources.getStringArray(R.array.sort_mode_brew)
        brewSpinPosition = 0
        brewSpinSelectedItem = sortList[brewSpinPosition]

        return root
    }

    // ソートSpinnerを変更した時のリスナ
    private inner class SortSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            // TODO: 2023/5/29 ヌルぽ対策カット（この対策が必要なのはSTAT Fragmentだけの模様）
//            if( activity==null ) return


            // onCreateView, onViewCreatedではSpinにアクセルできないので、グローバル保管
            // 2023/6/1 どうもMainActivityではなく、AppCompatActivityが正しい模様（これが原因で落ちている）
            (activity as AppCompatActivity).sortSpn.apply {
                brewSpinPosition = selectedItemPosition
                brewSpinSelectedItem = selectedItem.toString()

                if( brewFirstSortSpin ) {
                    Log.d("SHIRO", "BREW / 偽Spinner")
                    brewFirstSortSpin = false
                    return
                }
            }
            Log.d("SHIRO", "BREW / Spinner!")

            // ここで何かしないと、Fragmentが更新できない
            loadBrewData()
            adapter = BrewRecyclerViewAdapter( realmResults )
            brewRecycleView.adapter = adapter
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

    private fun loadBrewData() {

        // V3.7から選択肢を倍増（UP/DOWN）
        when( brewSpinSelectedItem ) {
            // 日付順
            sortList[0] -> realmResults = realm.where<BrewData>().findAll().sort("date", Sort.DESCENDING) // 日付順UP
            sortList[1] -> realmResults = realm.where<BrewData>().findAll().sort("date", Sort.ASCENDING) // 日付順DOWN
            // 評価準
            sortList[2] -> realmResults = realm.where<BrewData>().findAll().sort("date", Sort.DESCENDING).sort("rating", Sort.DESCENDING)
            sortList[3] -> realmResults = realm.where<BrewData>().findAll().sort("date", Sort.ASCENDING).sort("rating", Sort.DESCENDING)
            // 使用豆
            sortList[4] -> realmResults = realm.where<BrewData>().equalTo("place", BREW_IN_HOME).findAll().sort("date", Sort.DESCENDING).sort("beansID", Sort.DESCENDING)
            sortList[5] -> realmResults = realm.where<BrewData>().equalTo("place", BREW_IN_HOME).findAll().sort("date", Sort.ASCENDING).sort("beansID", Sort.DESCENDING)
            // メソッド順
            sortList[6] -> realmResults = realm.where<BrewData>().equalTo("place", BREW_IN_HOME).findAll().sort("date", Sort.DESCENDING).sort("methodID", Sort.DESCENDING)
            sortList[7] -> realmResults = realm.where<BrewData>().equalTo("place", BREW_IN_HOME).findAll().sort("date", Sort.ASCENDING).sort("methodID", Sort.DESCENDING)
            // 家のみ
            sortList[8] -> realmResults = realm.where<BrewData>().equalTo("place", BREW_IN_HOME).findAll().sort("date", Sort.DESCENDING)
            // 外飲み
            sortList[9] -> realmResults = realm.where<BrewData>().equalTo("place", BREW_IN_SHOP).findAll().sort("date", Sort.DESCENDING).sort("beansID", Sort.DESCENDING)
            else ->  realmResults = realm.where<BrewData>().findAll().sort("date", Sort.DESCENDING)
        }

    }


    // 画面精製の都度に呼ばれないように、onViewCreatedに記載。onCreateでもいい！？
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SHIRO", "BREW / onViewCreated")

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        brewRecycleView.layoutManager = layoutManager

        // とりあえずのデータ
        loadBrewData()
//        realmResults = realm.where<BrewData>().findAll()

        // アダプターを設定する
        // まだrealmResultsには何も入ってないけど大丈夫か？
        adapter = BrewRecyclerViewAdapter( realmResults )
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        brewRecycleView.adapter = this.adapter

    }


    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    // ここまでくれば、親Activityの準備ができている（メニューバー、スピナー他）
    override fun onStart() {
        Log.d("SHIRO", "BREW / onStart")
        super.onStart()

        // タイトルバーはonStart以降しか作れない
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.title = getString(R.string.titleBrewList)
        ac.supportActionBar?.show()

        // 親Activityのスピナー（Sort）をここでセットする
        // スピナをセットすると自動的に１回呼ばれてしまう（＝Recyclerが初期化されちゃう）ので、フラグで回避
        brewFirstSortSpin = true
        val adapter = ArrayAdapter<String>(ac, android.R.layout.simple_spinner_dropdown_item, sortList)
        ac.sortSpn.apply {
            visibility = View.VISIBLE
            this.adapter = adapter
            setSelection(brewSpinPosition)
            onItemSelectedListener = SortSpinnerChangeListener()
        }

        // RecyclerView更新
        brewRecycleView.adapter?.notifyDataSetChanged()
    }

    // realmの閉め忘れに注意！
    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "BREW / onDestroy - DB close")
        realm.close()
    }

    override fun onStop() {
        super.onStop()
        Log.d( "SHIRO", "BREW / onStop")
    }
}
