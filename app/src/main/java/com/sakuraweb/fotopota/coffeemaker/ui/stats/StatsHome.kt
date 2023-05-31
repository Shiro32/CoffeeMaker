package com.sakuraweb.fotopota.coffeemaker.ui.stats

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.home.calcCupsDrunkOfPeriod
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_stats_home.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class StatsHome : Fragment() {

    // 回転　： onStart
    // 切替　： onStart, onResume
    // なので、onResumeでやることにした
    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    override fun onResume() {
        super.onResume()
        Log.d("SHIRO", "STATS-HOME / onResume")
        drawHomeStats( prepareToStats(selectedPage, spinPosition, spinSelectedItem) )
        (activity as MainActivity).sortSpn.onItemSelectedListener = HomeSpinnerChangeListener()
    }

    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    // 月別Spinnerを変更した際のグラフ再描画処理
    private inner class HomeSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            Log.d("SHIRO", "STATS-HOME / Spinner")

            // 以前はヌルポチェックをやっていたが、3.70から廃止
            // おおもとのStatFragmentでonStart→onResume化で回避できたか？
//            if( activity==null ) return

            // Spinnerの情報をグローバル変数に保管しておく
            (activity as AppCompatActivity).sortSpn.apply {
                spinPosition = selectedItemPosition
                spinSelectedItem = selectedItem.toString()
            }
            drawHomeStats(prepareToStats(selectedPage, spinPosition, spinSelectedItem))
        }

        // OnItemSelecctedListenerの実装にはこれを入れないといけない（インターフェースなので）
        // だけど無選択時にやることは無いので、何も書かずさようなら
        override fun onNothingSelected(parent: AdapterView<*>?) { }
    }

    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    // ここから統計データ表示のメイン処理
    // メイン画面から、統計基本情報を含んだStatsPackを受け取る
    // 期間（begin～last）、そこに含まれるbeansID、takeoutID、表示用ヒントなどを含む
    fun drawHomeStats(spin:StatsPack ) {
        // v3.70以前はここでヌルぽチェックをしていたが、削除

        // 期間のラベル
        statsHomeTotalHint.text = spin.msg

        // 全体のカップ数
        statsHomeTotalCupsText.text = calcCupsDrunkOfPeriod(BREW_IN_HOME, spin.begin, spin.last).toString()

        // 豆関係ランキングの前処理（とても長いけど）
        // 範囲限定版のBEANS
        val realm = Realm.getInstance(beansRealmConfig)
        var beans = realm.where<BeansData>()
            .`in`("id", spin.beansIDList)
            .findAll()


        // 面倒くさいので、BEANSの範囲限定版をコピーして作っちゃう
        // たかがDBを複製するだけなんだから、１行でできないかね・・・？
        val tempRealmConfig = RealmConfiguration.Builder()
            .name("temp")
            .deleteRealmIfMigrationNeeded()
            .build()
        val tempBeansRealm = Realm.getInstance(tempRealmConfig)

        val temps = tempBeansRealm.where<BeansData>().findAll()
        tempBeansRealm.executeTransaction { temps.deleteAllFromRealm() }

        tempBeansRealm.beginTransaction()
        for( org in beans) {

            // applyを使って、dst側の一時変数を回避（いかにもKotlinらしい？）
            tempBeansRealm.createObject<BeansData>(org.id).apply {
                date    = org.date
                name    = org.name
                gram    = org.gram
                roast   = org.roast
                shop    = org.shop
                price   = org.price
                memo    = org.memo

                // ここから下のプロパティは範囲限定の際は意味なし
                recent  = org.recent
                rating  = org.rating
                count   = org.count
            }
        }
        tempBeansRealm.commitTransaction()

        // これでオリジナルは用無しなので閉める
        realm.close()


        // BREWからの参照を全部調べ上げて、BEANSの各種参照情報を更新する
        // 評価、最終利用日、利用回数
        var brewRealm = Realm.getInstance(brewRealmConfig)
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

}