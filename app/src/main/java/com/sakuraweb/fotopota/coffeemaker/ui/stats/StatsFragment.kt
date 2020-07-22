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


// 統計画面全体を構成する
// 統計期間選択用のSpinnerが意外と大変で、その作業が多い
// この配下に、３つのFragment（HOME/TAKEOUT/GRAPH）を持ち、TabLayoutで制御する
class StatsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_stats, container, false)

        // ツールバーやメニューの装備（ホームなのでメニュー無いけど）
        val ma: MainActivity = activity as MainActivity
        ma.supportActionBar?.title = getString(R.string.title_analyze)
        ma.supportActionBar?.show()

        // 計測期間のデフォルト（＝全期間） →　Spinnerのリスナでやってもらえるので不要っぽい
        // TODO: これ全然使っていない気がする
        beginPeriod = getFirstBrewDate()
        endPeriod = Date()

        // ツールバー上の日付スピナーを作る（特定の月、または全期間）
        // fragmentごとにスピナの中身を作り、リスナもセットする（セットし忘れると死ぬ）
        // TODO: これ、3Fragmentの内容に合わせるべきでは？　→　と思ったけど、みんな同じで大丈夫そう
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

        // 3FragmentのViewer, TabLayoutの設定
        root.statsPager.adapter = StatsTabAdapter(childFragmentManager)
        root.statsPager.addOnPageChangeListener(PageChangeListener())
        root.statsTab.setupWithViewPager(root.statsPager)

        // TODO: ん？ これ意味あったっけ？
        root.statsPager.offscreenPageLimit = 2

        // 3Fragmentのタイトルアイコン（だいぶ狭くなるけど）
        root.statsTab.getTabAt(0)?.setIcon(R.drawable.cup300)
        root.statsTab.getTabAt(1)?.setIcon(R.drawable.takeout100)
        root.statsTab.getTabAt(2)?.setIcon(R.drawable.graph)

        return root
    }


    // STATSのページ（３種類）が切り替わったときのリスナ
    // 切り替わった先のFragmentにはなんのLifeCycleイベントも発生しないみたい（onResumeすら）
    // しょうがないので、ページ切り替え完了時に当該ページのFragmentを再描画させる
    // 最初は、各ページのonResumeとか呼び出したけど失敗したので、普通のメソッド（reload）にしてみた
    private inner class PageChangeListener() : ViewPager.SimpleOnPageChangeListener () {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            val ma = activity as MainActivity

            // 切り替わったページ（Fragment）のリロードを行う
            when( position ) {
                0 -> statsHomeFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                1 -> statsTakeoutFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                2 -> statsGraphicFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
            }
        }
    }

    // スピナの選択処理は、結局はメイン画面でしかできないと思われるので、ここでやろう
    // 最初は子供（Fragment）側で処理させてたけど、１個のスピナを３画面のonClickで共有すること自体が不自然
    // もともと親側にあるスピナなので、ここでキャッチして、結果を子fragmentに伝える方式に変更
    // と言っても、ページ切り替え時の処理と同様、reloadするだけ
    private inner class MonthSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

            val ma = activity as MainActivity

            when( ma.statsPager.currentItem ) {
                0 -> statsHomeFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                1 -> statsTakeoutFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
                2 -> statsGraphicFragment?.reload(ma.sortSpn.selectedItemPosition, ma.sortSpn.selectedItem.toString())
            }
        }

        // OnItemSelecctedListenerの実装にはこれを入れないといけない（インターフェースなので）
        // だけど無選択時にやることは無いので、何も書かずさようなら
        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }

/*
    // もちろん不要だけど、なんとなく覚えておくために記載が残っている
    override fun onStart() {
        super.onStart()
    }
*/

}

