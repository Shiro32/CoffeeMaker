package com.sakuraweb.fotopota.coffeemaker.ui.stats

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlinx.android.synthetic.main.fragment_stats.view.*
import java.util.*

// TODO:  表を飛び出してしまう処理（ダークモカチップとか）

// この解析フラグメント全体で使う、解析期間の開始・終了
lateinit var beginPeriod: Date
lateinit var endPeriod: Date


// 統計画面全体を構成する（といっても、自分自身もFragment）
// この配下に、３つのFragment（HOME/TAKEOUT/GRAPH）を持ち、TabLayoutで制御する
// この画面でメニューバーのSpinnerも制御する
class StatsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_stats, container, false)

        // ツールバーやメニューの装備（ホームなのでメニュー無いけど）
        val ma: MainActivity = activity as MainActivity
        ma.supportActionBar?.title = getString(R.string.title_analyze)
        ma.supportActionBar?.show()

        // 計測期間のデフォルト（＝全期間） →　Spinnerのリスナでやってもらえるので不要っぽい
        beginPeriod = getFirstBrewDate()
        endPeriod = Date()

        // ツールバー上の日付スピナーを作る
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        // TODO: これ、3Fragmentの内容に合わせるべきでは？
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

        // やっとこアダプタ・リスナをセット
        val adapter = ArrayAdapter<String>(ma, android.R.layout.simple_spinner_dropdown_item, mList)
        ma.sortSpn.adapter = adapter
        ma.sortSpn.onItemSelectedListener = MonthSpinnerChangeListener()

        // 3Fragmentのタブビューワなどの設定
        root.statsPager.adapter = StatsTabAdapter(childFragmentManager)
        root.statsPager.addOnPageChangeListener(PageChangeListener())
        root.statsTab.setupWithViewPager(root.statsPager)

        root.statsPager.offscreenPageLimit = 2

        root.statsTab.getTabAt(0)?.setIcon(R.drawable.cup300)
        root.statsTab.getTabAt(1)?.setIcon(R.drawable.takeout100)
        root.statsTab.getTabAt(2)?.setIcon(R.drawable.graph)

        return root
    }

    private inner class PageChangeListener() : ViewPager.SimpleOnPageChangeListener () {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            val ma = activity as MainActivity

            // ページ変更アクション
            // ページ前面に来たときに、Spinnerの中身を変更する必要があるならココかな・・・？
//            blackToast(activity as MainActivity, position.toString())
            when( position ) {
                0 -> statsHomeFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                1 -> statsTakeoutFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                2 -> statsGraphicFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
            }
        }
    }

    // スピナの選択処理は、結局はメイン画面でしかできないと思われるので、ここでやろう
    // ほんで、結果をなんとかして子fragmentに伝えて強引に処理させる
    // とにかくリスナはここで固定！！！！！
    private inner class MonthSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            // 本当は「現在のFragment」だけに発行すべきだけど、面倒くさいので両方call
            //　やるとしたら、Pagerからcurrentを拾うか、グローバル変数で管理する
            val ma = activity as MainActivity

            when( ma.statsPager.currentItem ) {
                0 -> statsHomeFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                1 -> statsTakeoutFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                2 -> statsGraphicFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }

    // ここから各種描画開始
    override fun onStart() {
        super.onStart()
    }
}

