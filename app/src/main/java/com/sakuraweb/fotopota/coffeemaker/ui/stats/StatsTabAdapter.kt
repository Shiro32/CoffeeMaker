package com.sakuraweb.fotopota.coffeemaker.ui.stats

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

var statsHomeFragment: StatsHome? = null
var statsTakeoutFragment: StatsTakeout? = null
var statsGraphicFragment: StatsGraphical? = null


class StatsTabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {

        when( position ) {
            0 -> return( "家飲み統計" )
            1 -> return( "外飲み統計")
            2 -> return( "グラフで見る")
            else -> return( "なし")
        }
    }

    override fun getItem(position: Int): Fragment {
        lateinit var fm: Fragment

        when( position ) {
            0 -> {
                fm = StatsHome.newInstance("ほげ", "ほげほげ")
                statsHomeFragment = fm
            }
            1 -> {
                fm = StatsTakeout.newInstance("ほげ２","ほげほげ2")
                statsTakeoutFragment = fm
            }
            2 -> {
                fm = StatsGraphical.newInstance("ほい", "ほいほい")
                statsGraphicFragment = fm
            }
        }
        return fm
    }
}