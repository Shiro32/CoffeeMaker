package com.sakuraweb.fotopota.coffeemaker.ui.takeouts.select

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.RESULT_TO_HOME
import kotlinx.android.synthetic.main.activity_takeout_select.*

class TakeoutSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_takeout_select)
//        setContentView(R.layout.activity_beans_select_avtivity)

        // まずはタブの処理。adapterから行こう
        takeoutPager.adapter = TakeoutSelectTabAdapter(supportFragmentManager, this)
        takeoutTabLayout.setupWithViewPager(takeoutPager)

        // アイコンをセットしてみたいんだが
        // TODO: 店などのアイコンを設定する
        takeoutTabLayout.getTabAt(0)?.setIcon(R.drawable.takeout100)
        takeoutTabLayout.getTabAt(1)?.setIcon(R.drawable.cup300)
        takeoutTabLayout.getTabAt(2)?.setIcon(R.drawable.rest150)

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(takeoutSelectToolbar) // これやらないと落ちるよ

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    // メニュー設置
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_opt_menu_2, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // メニュー選択の対応
    // TODO: ボタンでの処理と同じなので共通化したいな
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val intentID = this.intent.getLongExtra("id", 0L)
        when( item.itemId ) {
            // saveは面倒くさいので後回し・・・。

            R.id.optMenu2ItemHome -> {
                // 新機軸！ ちゃんとホームまで帰っていく！
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
