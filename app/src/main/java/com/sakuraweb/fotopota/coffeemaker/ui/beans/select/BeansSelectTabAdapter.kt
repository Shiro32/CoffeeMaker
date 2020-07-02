package com.sakuraweb.fotopota.coffeemaker.ui.beans.select

import android.content.Context


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.beansKind

class BeansSelectTabAdapter (fm:FragmentManager, private val ctx: Context) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
       lateinit var fm: Fragment

        when(position) {
           0 -> { fm = BeansSelectFragment.newInstance( "SPECIAL","hoge1") }
           1 -> { fm = BeansSelectFragment.newInstance("BLEND","hoge2") }
           2 -> { fm = BeansSelectFragment.newInstance("PACK","hoge3") }
        }
        return fm
    }

    override fun getPageTitle(position: Int): CharSequence? {
        // やろうと思えば自作でできるが・・・？
        return beansKind[position]
//        return super.getPageTitle(position)
    }

    override fun getCount(): Int {
        return 3
    }
}
