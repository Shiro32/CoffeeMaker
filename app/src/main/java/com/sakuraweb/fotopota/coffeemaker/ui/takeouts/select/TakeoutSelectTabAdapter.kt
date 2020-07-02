package com.sakuraweb.fotopota.coffeemaker.ui.takeouts.select

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.sakuraweb.fotopota.coffeemaker.takeoutKind

class TakeoutSelectTabAdapter (fm:FragmentManager, private val ctx: Context) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
       lateinit var fm: Fragment

        when(position) {
           0 -> { fm = TakeoutSelectFragment.newInstance( "SPECIAL","hoge1") }
           1 -> { fm = TakeoutSelectFragment.newInstance("BLEND","hoge2") }
           2 -> { fm = TakeoutSelectFragment.newInstance("PACK","hoge3") }
        }
        return fm
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // やろうと思えば自作でできるが・・・？
        return takeoutKind[position]
//        return super.getPageTitle(position)
    }

    override fun getCount(): Int {
        return 3
    }
}
