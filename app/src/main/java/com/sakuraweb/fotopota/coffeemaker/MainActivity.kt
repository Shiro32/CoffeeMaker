package com.sakuraweb.fotopota.coffeemaker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*

// 各アクティビティ間のグローバルな移動のためのRESULT CODE
const val RESULT_TO_HOME = 1    // ホーム画面へ戻るコード
const val RESULT_TO_LIST = 2    // BREWやBEANSのリスト画面目へ戻るコード


// メインアクティビティの中に、BREW/BEANS/TAKEOUT/ANALYZEの4Fragmentを切り替える
// メイン画面内のViewやToolbarを変更したいなら、ここにパブリックなメソッドを作り、それを呼び出す
// TODO:メイン画面（Home, Brew, Bean, Setting）をViewHolderで横スクロール？

// メインアクティビティー
// リスト表示画面そのもの
open class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("SHIRO", "MAIN / onCreate")

        // さっぱりわからないけど、ナビゲーション関係の模様
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_beans, R.id.navigation_stats))


        // ↓これでアクションバーの中身を勝手に書き換えている
        // この行を残したままNoActionBar化するとハングアップする
 //       setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)



        // 反省して自前でToolbarを設置し、Activity画面のトップに置く（Fragmentにはおかない）
        setSupportActionBar(mainToolbar)

        // コンテキストメニューをセット
        // TODO: ここでセットすると落ちる。fragmentが出来上がっていないから？
//        registerForContextMenu(sampleButton)

    }

//    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
//        super.onCreateContextMenu(menu, v, menuInfo)
//
//        // TODO: インフレーターが見当たらず、activityを使ったけど大丈夫か？
//        menuInflater.inflate(R.menu.menu_context_brew, menu)
//
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("SHIRO", "MAIN / onActivityResult" )

        // ものすご～く気に入らないけど、これで我慢しよう・・・
        // 配下のfragmentのさらにRecyclerViewのAdapterで呼び出した時は、
        // 当該のfragmentのonResultには戻らず、ここに戻ってきちゃうみたい
        // だったらそれでいいよ、ということでここでやっちゃう
        if( resultCode == RESULT_TO_HOME ) {
            nav_view?.selectedItemId = R.id.navigation_home
        }
    }

    override fun onDestroy() {
        Log.d( "SHIRO", "MAIN / onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        Log.d( "SHIRO", "MAIN / onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d( "SHIRO", "MAIN / onStop")
    }

    override fun onStart() {
        super.onStart()
        Log.d("SHIRO", "MAIN / onStart")
    }
    open fun setSpinnerContent() {
        mainToolbar.title = "やっぱり、MainActivityから書き換えよう"
        sortSpn.visibility = View.GONE
    }
}
