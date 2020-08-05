package com.sakuraweb.fotopota.coffeemaker.ui.stats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.sakuraweb.fotopota.coffeemaker.BREW_IN_BOTH
import com.sakuraweb.fotopota.coffeemaker.BREW_IN_SHOP
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.home.calcCupsDrunkOfPeriod
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_stats_graphical.*
import kotlinx.android.synthetic.main.fragment_stats_takeout.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class StatsGraphical : Fragment() {

    //　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－
    // ここから統計データ表示のメイン処理
    // メイン画面から、統計基本情報を含んだStatsPackを受け取る
    // 期間（begin～last）、そこに含まれるbeansID、takeoutID、表示用ヒントなどを含む
    open fun onCreateStats( spin:StatsPack ) {

        statsGraphCard1Hint.text = spin.msg
        statsGraphCard1Hint2.text = "合計："+calcCupsDrunkOfPeriod(BREW_IN_BOTH, spin.begin, spin.last).toString()+"杯"

        // 各種見栄え
        // X軸
        val xa = chartArea.xAxis
        xa.position = XAxis.XAxisPosition.BOTTOM
        xa.setDrawGridLines(false)
        xa.setDrawAxisLine(true)

        // Y軸（左）
        val ly = chartArea.axisLeft
        ly.setDrawLabels(true)
        ly.spaceBottom = 0F
        ly.granularity = 1F
//        ly.setAxisMinValue(0F)
//        ly.setLabelCount( 10, false)
//        ly.mDecimals = 0

        // Y軸（右）
        val ry = chartArea.axisRight
        ry.setDrawLabels(false)
        ry.spaceBottom = 0F
        ry.granularity = 1F
//        ry.setAxisMinValue(0F)
//        ry.setLabelCount( 10, false)

        // グラフ全体
        chartArea.isClickable = false
        chartArea.setDescription("")
        chartArea.setPinchZoom(false)
        chartArea.isDoubleTapToZoomEnabled = false

        //アニメーション
        chartArea.animateY(1000, Easing.EasingOption.Linear);

        // データ作成は別関数で
        val data:BarData = createBarGraphData(spin.begin, spin.last)
        chartArea.data = data
        chartArea.invalidate()
    }


    private fun createBarGraphData(begin:Calendar, last:Calendar) : BarData {

        // 飲んだ回数データを作る
        val xLabels = mutableListOf<String>()
        val yValues = mutableListOf<BarEntry>()

        val ys = getPastMonthCups(begin, last)

        for( i in 0..ys.size-1 ) {
            xLabels.add( (i+1).toString() )
            yValues.add( BarEntry( ys[ys.size-i-1], i) )
        }
        // 下準備完了

        val barDataSet1 = BarDataSet(yValues, "家飲み＋外飲み")
        barDataSet1.setDrawValues(false)
        barDataSet1.setColor( ColorTemplate.COLORFUL_COLORS[4] )
        barDataSet1.axisDependency=YAxis.AxisDependency.LEFT

        // データセット全体（複数バーが描けるため）
        val barData = BarData( xLabels, barDataSet1 )
        return barData
    }

    // 指定期間中のカップ数をカウントする
    private fun getPastMonthCups(begin: Calendar, last: Calendar) : MutableList<Float> {

        var c1 = Calendar.getInstance()
        var c2 = Calendar.getInstance()

        c1.set(last.get(Calendar.YEAR), last.get(Calendar.MONTH), last.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        c2.set(last.get(Calendar.YEAR), last.get(Calendar.MONTH), last.get(Calendar.DAY_OF_MONTH), 23, 59, 59)

        val realm = Realm.getInstance(brewRealmConfig)
        var ret = mutableListOf<Float>()

        do {
            // １日分のデータを抽出
            val brews = realm.where<BrewData>()
                .between("date", c1.time, c2.time)
                .findAll()
                .sort( "date", Sort.DESCENDING)

            // １日分のdrunkを加算（面倒くさいねぇ・・・）
            var cups = 0F
            for( b in brews) cups += b.cupsDrunk
            ret.add( cups )

            c1.add(Calendar.DAY_OF_MONTH, -1)
            c2.add(Calendar.DAY_OF_MONTH, -1)
        } while( c1 >= begin )

        realm.close()
        return ret
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_graphical, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StatsGraphical().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}


