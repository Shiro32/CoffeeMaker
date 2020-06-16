package com.sakuraweb.fotopota.coffeemaker.ui.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.sakuraweb.fotopota.coffeemaker.R
import kotlinx.android.synthetic.main.activity_brew_edit.*

// Brewの各カードの編集画面
// 事実上、全画面表示のダイアログ
// 呼び出し元は、HomeのEditボタン（編集） or FAB（新規）
// Edit - 当該データのRealm IDをIntentで送ってくる
// FAB  - もちろん何もない

class BrewEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brew_edit)

        brewEditSaveBtn.setOnClickListener() {
            Log.d("SHIRO", "brew-edit / Saveボタンリスナ。各Viewから値を取って選択中のRealmを修正してfinishする")
            finish()
        }

        Log.d("SHIRO", "brew-edit / onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("SHIRO", "brew-edit / onDestroy")
    }
}