package com.sakuraweb.fotopota.coffeemaker.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_ana.*
import java.util.*

// TODO:  表を飛び出してしまう処理（ダークモカチップとか）

// この解析フラグメント全体で使う、解析期間の開始・終了
lateinit var beginPeriod: Date
lateinit var endPeriod: Date

fun getFirstBrewDate() : Date {
    var begin: Date

    val realm = Realm.getInstance(brewRealmConfig)
    val brews = realm.where<BrewData>().findAll().sort("date",Sort.ASCENDING)

    if( brews.size>0 ) {
        begin = brews[0]?.date as Date
    } else {
        begin = Date()
    }
    realm.close()

    return begin
}

class AnaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_ana, container, false)

        // ツールバーやメニューの装備（ホームなのでメニュー無いけど）
        // この強引なASキャストが正解の模様
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.title = getString(R.string.title_analyze)
        ac.supportActionBar?.show()

        val ma: MainActivity = activity as MainActivity
//        ma.mainToolbar.title = getString(R.string.title_analyze)

        // メニュー構築（実装はonCreateOptionsMenu内で）
//        setHasOptionsMenu(true)

        // 計測期間のデフォルト（＝全期間） →　Spinnerのリスナでやってもらえるので不要っぽい
        beginPeriod = getFirstBrewDate()
        endPeriod = Date()

        // ＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃＃
        // ツールバー上の日付スピナーを作る
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        var mList = mutableListOf<String>()

        var month = Calendar.getInstance()
        var last = Calendar.getInstance()
        last.set(Calendar.DATE, last.getActualMaximum(Calendar.DATE))

        val realm = Realm.getInstance(brewRealmConfig)
        var brews = realm.where<BrewData>().sort("date", Sort.ASCENDING).findAll()
        if( brews.size> 0 ) month.time = brews[0]?.date
        realm.close()

        do {
            mList.add(0, "%d年%02d月".format(month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1))
            month.add(Calendar.MONTH, 1)
        } while(month <= last)
        mList.add(0, getString(R.string.anaAllPeriod))

        // やっとこアダプタ・リスナをセットして終わり
        val adapter = ArrayAdapter<String>(ma, android.R.layout.simple_spinner_dropdown_item, mList)
        ma.sortSpn.adapter = adapter
        ma.sortSpn.onItemSelectedListener = MonthSpinnerChangeListener()

        return root
    }



    // 分析期間Spinnerを変更した時のリスナ
    private inner class MonthSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

            val ma: MainActivity = activity as MainActivity

            // 選択肢から期間を求める
            var begin = Calendar.getInstance()
            var last = Calendar.getInstance()

            // 「全期間」を選んだときは全範囲を設定しよう
            var headerMsg: String
            if( ma.sortSpn.selectedItemPosition == 0 ) {
                val realm = Realm.getInstance(brewRealmConfig)
                val brews = realm.where<BrewData>().findAll().sort("date",Sort.ASCENDING)
                if( brews.size>0 ) begin.time = brews[0]?.date
                realm.close()
                last.time = Date()
                headerMsg = "アプリの使用開始（%d年%d月）から今日までに飲んだコーヒー".format(begin.get(Calendar.YEAR), begin.get(Calendar.MONTH)+1)
            } else {
                // 特定の月の時はその月をセット
                val m = ma.sortSpn.selectedItem.toString()
                val a = m.split("年","月")
                begin.set(a[0].toInt(), a[1].toInt()-1, 1, 0, 0, 0)
                last.set(a[0].toInt(), a[1].toInt()-1, 1, 23,59,59)
                last.set(Calendar.DATE, last.getActualMaximum(Calendar.DATE))
                headerMsg = "%d年%d月に飲んだコーヒー".format(begin.get(Calendar.YEAR), begin.get(Calendar.MONTH)+1, last.get(Calendar.YEAR), last.get(Calendar.MONTH)+1)
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
                if( brew.place == BREW_IN_HOME ) {
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

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }
    // オプションメニュー設置
    // Fragment内からActivity側のToolbarに無理矢理設置する
    // 本来は逆だけど、こうすればFragmentごとに違うメニューが作れる
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

//        inflater?.inflate(R.menu.menu_opt_menu_1, menu)
//     ホームに戻るメニューはくどいのでやめた

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
    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "settings / onDestroy")
    }

    // ここから各種描画開始
    override fun onStart() {
        super.onStart()

        // Activity側の画面もいじるので無理矢理
//        val ma: MainActivity = activity as MainActivity

        // 飲んだ回数
//        anaCupsText.text = calcCupsOfMonth(2020,5).toString()

    }
}

