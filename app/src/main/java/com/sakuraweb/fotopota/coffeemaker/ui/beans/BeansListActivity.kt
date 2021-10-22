package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.RESULT_TO_HOME
import kotlinx.android.synthetic.main.activity_beans_list.*

// マメ一覧表を出すためのActivity
// ナビゲーション（BottomNavigation）で表示するのとは違い、Brew一覧＞Brew編集＞マメ選択、と来た場合
// 一覧表の中身は、ナビゲーションできた場合と同じにしたいので、そいつのFragmentをinflateするだけ

class BeansListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_beans_list)

        // beansListToolbar.title = "使った豆を選んでください"
        setSupportActionBar(beansListToolbar)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // タイトルはfragment内でintentIDで判断してかき分けましょう
    }

    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_opt_menu_2, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // オプションメニュー対応
    // あまり項目がないけど（ホームのみ？）
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId ) {
            R.id.optMenu2ItemHome -> {
                // 新機能！ いろいろウィンドウ閉めながらHomeまで帰る！
                val intent = Intent()
                setResult( RESULT_TO_HOME, intent)
                finish()
            }

            R.id.optMenu2ItemCancel -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}