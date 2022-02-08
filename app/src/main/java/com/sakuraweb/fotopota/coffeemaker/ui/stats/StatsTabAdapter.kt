package com.sakuraweb.fotopota.coffeemaker.ui.stats

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter

var statsHomeFragment: StatsHome? = null
var statsTakeoutFragment: StatsTakeout? = null
var statsGraphicFragment: StatsGraphical? = null


class StatsTabAdapter(fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT ) {

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {

        return when( position ) {
            STATS_HOME      -> "家飲み統計"
            STATS_TAKEOUT   -> "外飲み統計"
            STATS_GRAPH     -> "グラフで見る"
            else -> ( "なし")
        }
    }

    override fun getItem(position: Int): Fragment {
        lateinit var fm: Fragment

        when( position ) {
            STATS_HOME -> {
                fm = StatsHome.newInstance("ほげ", "ほげほげ")
                statsHomeFragment = fm
            }
            STATS_TAKEOUT-> {
                fm = StatsTakeout.newInstance("ほげ２","ほげほげ2")
                statsTakeoutFragment = fm
            }
            STATS_GRAPH -> {
                fm = StatsGraphical.newInstance("ほい", "ほいほい")
                statsGraphicFragment = fm
            }
        }
        return fm
    }
}