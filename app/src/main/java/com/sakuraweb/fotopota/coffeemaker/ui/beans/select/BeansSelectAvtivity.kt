package com.sakuraweb.fotopota.coffeemaker.ui.beans.select

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sakuraweb.fotopota.coffeemaker.R
import kotlinx.android.synthetic.main.activity_beans_select_avtivity.*


class BeansSelectAvtivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_beans_select_avtivity)

        // まずはタブの処理。adapterから行こう
        beansPager.adapter = TabAdapter(supportFragmentManager, this)
        tabLayout.setupWithViewPager(beansPager)

    }
}