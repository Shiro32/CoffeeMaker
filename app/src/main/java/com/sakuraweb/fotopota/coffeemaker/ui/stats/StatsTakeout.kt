package com.sakuraweb.fotopota.coffeemaker.ui.stats

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.home.calcCupsDrunkOfPeriod
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_stats_takeout.*
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class StatsTakeout : Fragment() {

    // 回転　： onStart
    // 切替　： onStart, onResume
    // なので、onResumeでやることにした
    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    override fun onResume() {
        super.onResume()
        Log.d("SHIRO", "STATS-TAKEOUT / onResume")
//        blackToast( context as Context, "外飲みResume" )

        (activity as MainActivity).sortSpn.onItemSelectedListener = TakeoutSpinnerChangeListener()
        drawTakeoutStats( prepareToStats(selectedPage, spinPosition, spinSelectedItem) )
    }

    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    // 月別Spinnerを変更した際のグラフ再描画処理
    private inner class TakeoutSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            Log.d("SHIRO", "STATS-TAKEOUT / Spinner")
//            blackToast(context as Context, "外飲みSpinner")

            // Spinnerの情報をグローバル変数に保管しておく
            if( activity!=null ) {
                (activity as MainActivity).sortSpn.apply {
                    spinPosition = selectedItemPosition
                    spinSelectedItem = selectedItem.toString()
                }
                drawTakeoutStats(prepareToStats(selectedPage, spinPosition, spinSelectedItem))
            }
        }

        // OnItemSelecctedListenerの実装にはこれを入れないといけない（インターフェースなので）
        // だけど無選択時にやることは無いので、何も書かずさようなら
        override fun onNothingSelected(parent: AdapterView<*>?) { }
    }

    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    // ここから統計データ表示のメイン処理
    // メイン画面から、統計基本情報を含んだStatsPackを受け取る
    // 期間（begin～last）、そこに含まれるbeansID、takeoutID、表示用ヒントなどを含む
    fun drawTakeoutStats( spin:StatsPack ) {

        // 期間のラベル
        statsTakeoutTotalHint.text = spin.msg

        // 全体のカップ数
        statsTakeoutTotalCupsText.text = calcCupsDrunkOfPeriod(BREW_IN_SHOP, spin.begin, spin.last).toString()

        // テイクアウト関係ランキングの前処理（とても長いけど）
        // 範囲限定版のTAKEOUT
        val realm = Realm.getInstance(takeoutRealmConfig)
        var takeouts = realm.where<TakeoutData>()
            .`in`("id", spin.takeoutIDList)
            .findAll()

        // 面倒くさいので、BEANSの範囲限定版をコピーして作っちゃう
        // たかがDBを複製するだけなんだから、１行でできないかね・・・？
        val tempRealmConfig = RealmConfiguration.Builder()
            .name("temp")
            .deleteRealmIfMigrationNeeded()
            .build()
        val tempTakeoutRealm = Realm.getInstance(tempRealmConfig)

        val tempTakeouts = tempTakeoutRealm.where<TakeoutData>().findAll()
        tempTakeoutRealm.executeTransaction { tempTakeouts.deleteAllFromRealm() }

        tempTakeoutRealm.beginTransaction()
        for( org in takeouts) {
            var dst = tempTakeoutRealm.createObject<TakeoutData>(org.id)
            dst.first   = org.first
            dst.name    = org.name
            dst.chain   = org.chain
            dst.shop    = org.shop
            dst.price   = org.price
            dst.size    = org.size
            dst.memo    = org.memo

            // ここから下のプロパティは範囲限定の際は意味なし
            dst.recent  = org.recent
            dst.rating  = org.rating
            dst.count   = org.count
        }
        tempTakeoutRealm.commitTransaction()

        // これでオリジナルは用無しなので閉める
        realm.close()


        // BREWからの参照を全部調べ上げて、BEANSの各種参照情報を更新する
        // 評価、最終利用日、利用回数
        val brewRealm = Realm.getInstance(brewRealmConfig)
        takeouts = tempTakeoutRealm.where<TakeoutData>().findAll()

        for( take in takeouts ) {
            // BREWの中で自分を参照しているデータを日付ソートで全部拾う
            val brews= brewRealm
                .where<BrewData>()
                .equalTo("takeoutID", take.id)
                .between( "date", beginPeriod, endPeriod)
                .findAll()
                .sort("date", Sort.DESCENDING)
            if (brews.size > 0) {
                // 最新利用日のセット
                val recent = brews[0]?.date
                // 利用側（BREW）での評価の算出
                var rate: Float = 0.0F
                for (b in brews) rate += b.rating

                tempTakeoutRealm.executeTransaction {
                    if (recent != null) take.recent = recent
                    take.rating = rate / brews.size
                    take.count = brews.size
                }
            } else {
                // １回も利用が無かった場合・・・（涙）
                tempTakeoutRealm.executeTransaction {
                    // 利用無し、をどう表現したらいいやら・・・。Non-nullだし。
                }
            }
        }
        brewRealm.close()

        // ここまでの前処理で、BREWの日付範囲で限定されたBEANSの各要素で、
        // 再度、カウント、評価、最新利用日を設定したtempデータベースが完成

        // 好評価ランキング
        // もちろん、範囲限定できているので簡単
        takeouts = tempTakeoutRealm.where<TakeoutData>().findAll()
            .sort("rating", Sort.DESCENDING)

        if( takeouts.size>0 ) {
            statsTakeoutFavRank1Name.text = takeouts[0]?.name
            favTakeoutRank1Count.text = "%1.1f".format(takeouts[0]?.rating)
        } else {
            statsTakeoutFavRank1Name.text = ""
            favTakeoutRank1Count.text = ""
        }
        if( takeouts.size>1 ) {
            favTakeoutRank2Name.text = takeouts[1]?.name
            favTakeoutRank2Count.text = "%1.1f".format(takeouts[1]?.rating)
        } else {
            favTakeoutRank2Name.text = ""
            favTakeoutRank2Count.text = ""
        }
        if( takeouts.size>2 ) {
            favTakeoutRank3Name.text = takeouts[2]?.name
            favTakeoutRank3Count.text = "%1.1f".format(takeouts[2]?.rating)
        } else {
            favTakeoutRank3Name.text = ""
            favTakeoutRank3Count.text = ""
        }

        // 多頻度豆ランキング
        takeouts = tempTakeoutRealm.where<TakeoutData>().findAll()
            .sort("count", Sort.DESCENDING)

        if( takeouts.size>0 ) {
            countTakeoutRank1Name.text = takeouts[0]?.name
            countTakeoutRank1Count.text = "%d".format(takeouts[0]?.count)
        } else {
            countTakeoutRank1Name.text = ""
            countTakeoutRank1Count.text = ""
        }
        if( takeouts.size>1 ) {
            countTakeoutRank2Name.text = takeouts[1]?.name
            countTakeoutRank2Count.text = "%d".format(takeouts[1]?.count)
        } else {
            countTakeoutRank2Name.text = ""
            countTakeoutRank2Count.text = ""
        }
        if( takeouts.size>2 ) {
            countTakeoutRank3Name.text = takeouts[2]?.name
            countTakeoutRank3Count.text = "%d".format(takeouts[2]?.count)
        } else {
            countTakeoutRank3Name.text = ""
            countTakeoutRank3Count.text = ""
        }
        tempTakeoutRealm.close()


        // TODO: １日で一番飲んだ日も見たい？

    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stats_takeout, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment StatsTakeout.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StatsTakeout().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}