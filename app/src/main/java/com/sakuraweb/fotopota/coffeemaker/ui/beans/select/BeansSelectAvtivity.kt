package com.sakuraweb.fotopota.coffeemaker.ui.beans.select

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sakuraweb.fotopota.coffeemaker.R
import kotlinx.android.synthetic.main.activity_beans_select_avtivity.*

// TODO: タブが目立ちにくいので、豆アイコンを追加する
// TODO: 最下部にこっそりあるTextViewどうする？　→　キャンセルボタン化？
// TODO: 全データぶち込み
// TODO: メニューやる・・・？　最低でも「戻る」ボタンは付けよう

class BeansSelectAvtivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_beans_select_avtivity)

        // まずはタブの処理。adapterから行こう
        beansPager.adapter = TabAdapter(supportFragmentManager, this)
        tabLayout.setupWithViewPager(beansPager)

        // アイコンをセットしてみたいんだが
        tabLayout.getTabAt(0)?.setIcon(R.drawable.beans150)
        tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_beans)
        tabLayout.getTabAt(2)?.setIcon(R.drawable.pack300)
    }
}
