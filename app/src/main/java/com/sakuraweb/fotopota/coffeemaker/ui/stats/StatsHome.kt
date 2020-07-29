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

class StatsHome : Fragment() {
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

    // ここからメイン処理
    // Fragmentが最初に呼ばれたとき、分析期間のSpinnerを変更した時、に呼ばれて分析画面を作り上げる

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
        statsHomeTotalHint.text = headerMsg

        // 全体のカップ数
        statsHomeTotalCupsText.text = calcCupsDrunkOfPeriod(BREW_IN_HOME, begin, last).toString()


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
            statsHomeFavRank1Name.text = beans[0]?.name
            statsHomeFavRank1Count.text = "%1.1f".format(beans[0]?.rating)
        } else {
            statsHomeFavRank1Name.text = ""
            statsHomeFavRank1Count.text = ""
        }
        if( beans.size>1 ) {
            statsHomeFavRank2Name.text = beans[1]?.name
            statsHomeFavRank2Count.text = "%1.1f".format(beans[1]?.rating)
        } else {
            statsHomeFavRank2Name.text = ""
            statsHomeFavRank2Count.text = ""
        }
        if( beans.size>2 ) {
            statsHomeFavRank3Name.text = beans[2]?.name
            statsHomeFavRank3Count.text = "%1.1f".format(beans[2]?.rating)
        } else {
            statsHomeFavRank3Name.text = ""
            statsHomeFavRank3Count.text = ""
        }

        // 多頻度豆ランキング
        beans = tempBeansRealm.where<BeansData>().findAll()
            .sort("count", Sort.DESCENDING)

        if( beans.size>0 ) {
            statsHomeCountRank1Name.text = beans[0]?.name
            statsHomeCountRank1Count.text = "%d".format(beans[0]?.count)
        } else {
            statsHomeCountRank1Name.text = ""
            statsHomeCountRank1Count.text = ""
        }
        if( beans.size>1 ) {
            statsHomeCountRank2Name.text = beans[1]?.name
            statsHomeCountRank2Count.text = "%d".format(beans[1]?.count)
        } else {
            statsHomeCountRank2Name.text = ""
            statsHomeCountRank2Count.text = ""
        }
        if( beans.size>2 ) {
            statsHomeCountRank3Name.text = beans[2]?.name
            statsHomeCountRank3Count.text = "%d".format(beans[2]?.count)
        } else {
            statsHomeCountRank3Name.text = ""
            statsHomeCountRank3Count.text = ""
        }
        tempBeansRealm.close()



        // TODO: １日で一番飲んだ日も見たい？

    }
}