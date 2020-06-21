package com.sakuraweb.fotopota.coffeemaker.ui.beans

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sakuraweb.fotopota.coffeemaker.R
import kotlinx.android.synthetic.main.fragment_beans_list.*

// マメ一覧表を出すためのActivity
// ナビゲーション（BottomNavigation）で表示するのとは違い、Brew一覧＞Brew編集＞マメ選択、と来た場合
// 一覧表の中身は、ナビゲーションできた場合と同じにしたいので、そいつのFragmentをinflateするだけ

class BeansListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beans_list)

        // beansListToolbar.title = "使った豆を選んでください"
    }
}