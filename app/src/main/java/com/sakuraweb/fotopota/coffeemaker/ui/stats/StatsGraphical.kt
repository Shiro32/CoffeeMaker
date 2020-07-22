package com.sakuraweb.fotopota.coffeemaker.ui.stats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.sakuraweb.fotopota.coffeemaker.MainActivity
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_stats_graphical.*
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [StatsGraphical.newInstance] factory method to
 * create an instance of this fragment.
 */
class StatsGraphical : Fragment() {
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
        return inflater.inflate(R.layout.fragment_stats_graphical, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment GraphicalAnalyze.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StatsGraphical().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    open fun reload(spnPosition:Int, spnItem:String ) {
        val ma: MainActivity = activity as MainActivity

        val data = createBarGraphData()
        chart.data = data

        // 各種見栄え
        // X軸
        val xa = chart.xAxis
        xa.position = XAxis.XAxisPosition.BOTTOM
        xa.setDrawGridLines(false)
        xa.setDrawAxisLine(true)

        // Y軸（左）
        val ly = chart.axisLeft
//        ly.setAxisMinValue(0F)

        // Y軸（右）
        val ry = chart.axisRight
        ry.setDrawLabels(false)
//        ry.setAxisMinValue(0F)

        // グラフ全体
        chart.isClickable = false
        chart.setDescription("")
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false

        chart.invalidate()

    }

    private fun createBarGraphData() : BarData {
        val r = Random()

        // データセット全体（複数バーが描けるため）
        val barDataSets = mutableListOf<BarDataSet>()

        // X軸を作る
        val xVals = mutableListOf<String>()
        for(i in 0..30) xVals.add(0, i.toString())

        // データセットＡを作る
        val yVals = mutableListOf<BarEntry>()
        val ys = getPast30DaysCups()

        for( i in 0..30) yVals.add( BarEntry(ys[i], 30-i) )

        val aDataSet = BarDataSet(yVals, "飲んだ回数")
        aDataSet.setDrawValues(false)
        aDataSet.setColor( ColorTemplate.COLORFUL_COLORS[4] )

        // データセット全体にＡを追加する
        barDataSets.add(aDataSet)

        // X軸と合体させる
        val barData = BarData( xVals, barDataSets as List<IBarDataSet>?)

        return barData
    }

    private fun getPast30DaysCups() : MutableList<Float> {
        val cal = Calendar.getInstance()

        val realm = Realm.getInstance(brewRealmConfig)
        var rets = mutableListOf<Float>()

        var begin = Calendar.getInstance()
        var last = Calendar.getInstance()
        begin.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        last.set( cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)

        for( i in 0..30) {
            val brews = realm.where<BrewData>()
                .between("date", begin.time, last.time)
                .findAll()
            rets.add( brews.size.toFloat() )

            begin.add(Calendar.DAY_OF_MONTH, -1)
            last.add(Calendar.DAY_OF_MONTH, -1)
        }

        realm.close()

        return rets
    }
}


