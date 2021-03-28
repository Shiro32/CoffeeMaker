package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.GridView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.RESULT_TO_HOME
import kotlinx.android.synthetic.main.activity_beans_select_avtivity.*
import kotlinx.android.synthetic.main.activity_icon_select.*

class IconSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_select)

        lateinit var gridView: GridView

        val adapter = IconSelectGridAdapter( )
        equipIconGridView.adapter = adapter

        equipIconGridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val intent = Intent()
            intent.putExtra("id", pos )
            setResult(Activity.RESULT_OK, intent)
            finish()
        }


        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(equipIconSelectToolbar) // これやらないと落ちるよ

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
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intentID = this.intent.getLongExtra("id", 0L)
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
