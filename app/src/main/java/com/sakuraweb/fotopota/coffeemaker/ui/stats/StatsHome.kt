package com.sakuraweb.fotopota.coffeemaker.ui.stats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.home.calcCupsDrunkOfPeriod
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_stats_home.*
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [StatsHome.newInstance] factory method to
 * create an instance of this fragment.
 */
class StatsHome : Fragment() {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stats_home, container, false)
    }

    companion object {
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment NumericalAnalyze.
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StatsHome().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

    }


    // 分析期間Spinnerを変更した時のリスナ
    open fun reload(spnPosition:Int, spnItem:String ) {
        val ma: MainActivity = activity as MainActivity

        // 選択肢から期間を求める
        var begin = Calendar.getInstance()
        var last = Calendar.getInstance()

        // 「全期間」を選んだときは全範囲を設定しよう
        var headerMsg: String
        if( spnPosition == 0 ) {
            val realm = Realm.getInstance(brewRealmConfig)
            val brews = realm.where<BrewData>().findAll().sort("date", Sort.ASCENDING)
            if( brews.size>0 ) begin.time = brews[0]?.date
            realm.close()
            last.time = Date()
            headerMsg = "アプリの使用開始（%d年%d月）から今日までに飲んだコーヒー".format(begin.get(Calendar.YEAR), begin.get(
                Calendar.MONTH)+1)
        } else {
            // 特定の月の時はその月をセット
            val m = spnItem
            val a = m.split("年","月")
            begin.set(a[0].toInt(), a[1].toInt()-1, 1, 0, 0, 0)
            last.set(a[0].toInt(), a[1].toInt()-1, 1, 23,59,59)
            last.set(Calendar.DATE, last.getActualMaximum(Calendar.DATE))
            headerMsg = "%d年%d月に飲んだコーヒー".format(begin.get(Calendar.YEAR), begin.get(Calendar.MONTH)+1, last.get(
                Calendar.YEAR), last.get(Calendar.MONTH)+1)
        }

        beginPeriod = begin.time
        endPeriod = last.time

        // ここまででBREWの範囲が決まったことになる
        // 範囲内のBREWが参照しているBEANSとTAKEOUTをリスト化する
        var brewRealm = Realm.getInstance(brewRealmConfig)
        var brews = brewRealm.where<BrewData>()
            .between("date", beginPeriod, endPeriod)
            .findAll()

        var beansList = arrayOf<Long>()
        var takeoutList = arrayOf<Long>()

        for( brew in brews) {
            if( brew.place == BREW_IN_HOME) {
                // 家飲みの場合はBEANSのリスト追加
                beansList += brew.beansID
            } else {
                takeoutList += brew.takeoutID
            }
        }
        brewRealm.close()


        // 期間のラベル
        anaHeaderText.text = headerMsg

        // 全体のカップ数
        anaCupsText.text = calcCupsDrunkOfPeriod(begin, last).toString()


// ------------------------------- 猛烈に長いけど、豆ランキング　-------------------------------

        // 豆関係ランキングの前処理（とても長いけど）
        // 範囲限定版のBEANS
        var realm = Realm.getInstance(beansRealmConfig)
        var beans = realm.where<BeansData>()
            .`in`("id", beansList)
            .findAll()


        // 面倒くさいので、BEANSの範囲限定版をコピーして作っちゃう
        // たかがDBを複製するだけなんだから、１行でできないかね・・・？
        val tempRealmConfig = RealmConfiguration.Builder()
            .name("temp")
            .deleteRealmIfMigrationNeeded()
            .build()
        val tempBeansRealm = Realm.getInstance(tempRealmConfig)

        var temps = tempBeansRealm.where<BeansData>().findAll()
        tempBeansRealm.executeTransaction { temps.deleteAllFromRealm() }

        tempBeansRealm.beginTransaction()
        for( org in beans) {
            var dst = tempBeansRealm.createObject<BeansData>(org.id)
            dst.date    = org.date
            dst.name    = org.name
            dst.gram    = org.gram
            dst.roast   = org.roast
            dst.shop    = org.shop
            dst.price   = org.price
            dst.memo    = org.memo

            // ここから下のプロパティは範囲限定の際は意味なし
            dst.recent  = org.recent
            dst.rating  = org.rating
            dst.count   = org.count
        }
        tempBeansRealm.commitTransaction()

        // これでオリジナルは用無しなので閉める
        realm.close()


        // BREWからの参照を全部調べ上げて、BEANSの各種参照情報を更新する
        // 評価、最終利用日、利用回数
        brewRealm = Realm.getInstance(brewRealmConfig)
        beans = tempBeansRealm.where<BeansData>().findAll()

        for( bean in beans) {
            // BREWの中で自分を参照しているデータを日付ソートで全部拾う
            val brews= brewRealm
                .where<BrewData>()
                .equalTo("beansID", bean.id)
                .between( "date", beginPeriod, endPeriod)
                .findAll()
                .sort("date", Sort.DESCENDING)
            if (brews.size > 0) {
                // 最新利用日のセット
                val recent = brews[0]?.date
                // 利用側（BREW）での評価の算出
                var rate: Float = 0.0F
                for (b in brews) rate += b.rating

                tempBeansRealm.executeTransaction {
                    if (recent != null) bean.recent = recent
                    bean.rating = rate / brews.size
                    bean.count = brews.size
                }
            } else {
                // １回も利用が無かった場合・・・（涙）
                tempBeansRealm.executeTransaction {
                    // 利用無し、をどう表現したらいいやら・・・。Non-nullだし。
                }
            }
        }
        brewRealm.close()

        // ここまでの前処理で、BREWの日付範囲で限定されたBEANSの各要素で、
        // 再度、カウント、評価、最新利用日を設定したtempデータベースが完成

        // 好評価ランキング
        // もちろん、範囲限定できているので簡単
        beans = tempBeansRealm.where<BeansData>().findAll()
            .sort("rating", Sort.DESCENDING)

        if( beans.size>0 ) {
            favBeanRank1Name.text = beans[0]?.name
            favBeanRank1Count.text = "%1.1f".format(beans[0]?.rating)
        } else {
            favBeanRank1Name.text = ""
            favBeanRank1Count.text = ""
        }
        if( beans.size>1 ) {
            favBeanRank2Name.text = beans[1]?.name
            favBeanRank2Count.text = "%1.1f".format(beans[1]?.rating)
        } else {
            favBeanRank2Name.text = ""
            favBeanRank2Count.text = ""
        }
        if( beans.size>2 ) {
            favBeanRank3Name.text = beans[2]?.name
            favBeanRank3Count.text = "%1.1f".format(beans[2]?.rating)
        } else {
            favBeanRank3Name.text = ""
            favBeanRank3Count.text = ""
        }

        // 多頻度豆ランキング
        beans = tempBeansRealm.where<BeansData>().findAll()
            .sort("count", Sort.DESCENDING)

        if( beans.size>0 ) {
            countBeanRank1Name.text = beans[0]?.name
            countBeanRank1Count.text = "%d".format(beans[0]?.count)
        } else {
            countBeanRank1Name.text = ""
            countBeanRank1Count.text = ""
        }
        if( beans.size>1 ) {
            countBeanRank2Name.text = beans[1]?.name
            countBeanRank2Count.text = "%d".format(beans[1]?.count)
        } else {
            countBeanRank2Name.text = ""
            countBeanRank2Count.text = ""
        }
        if( beans.size>2 ) {
            countBeanRank3Name.text = beans[2]?.name
            countBeanRank3Count.text = "%d".format(beans[2]?.count)
        } else {
            countBeanRank3Name.text = ""
            countBeanRank3Count.text = ""
        }
        tempBeansRealm.close()

// ------------------------------- 猛烈に長いけど、テイクアウトランキング　-------------------------------
        // テイクアウト関係ランキングの前処理（とても長いけど）
        // 範囲限定版のTAKEOUT
        realm = Realm.getInstance(takeoutRealmConfig)
        var takeouts = realm.where<TakeoutData>()
            .`in`("id", takeoutList)
            .findAll()

        // 面倒くさいので、BEANSの範囲限定版をコピーして作っちゃう
        // たかがDBを複製するだけなんだから、１行でできないかね・・・？
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
        brewRealm = Realm.getInstance(brewRealmConfig)
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
            favTakeoutRank1Name.text = takeouts[0]?.name
            favTakeoutRank1Count.text = "%1.1f".format(takeouts[0]?.rating)
        } else {
            favTakeoutRank1Name.text = ""
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
}