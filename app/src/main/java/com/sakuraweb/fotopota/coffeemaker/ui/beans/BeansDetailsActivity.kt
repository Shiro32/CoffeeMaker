package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.REQUEST_CODE_BEANS_SELECT
import com.sakuraweb.fotopota.coffeemaker.ui.brews.REQUEST_EDIT_BREW
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_datails.*
import java.util.*

const val REQUEST_EDIT_BEANS = 1

// TODO: イラストの唐突感を何とかする
// TODO: 円やグラムの単位

// Beansの１カードごとの詳細画面
// 当初は編集画面と一体化していたけど、List→Details→Editと３段階に変更

class BeansDetailsActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beans_datails)

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
        // configはStartActivityで生成済み
        realm = Realm.getInstance(beansRealmConfig)

        // 表示データ（クリック元のカードのid
        val intentID = this.intent.getLongExtra("id", 0L)

        // 日付処理用
        val calendar = Calendar.getInstance()

        // Realmからデータの読み込み
        val beans = realm.where<BeansData>().equalTo("id", intentID).findFirst()
        if (beans != null) {
            beansDetailsRatingBar.  rating = beans.rating
            beansDetailsNameText.   setText(beans.name)
            beansDetailsRoastBar.   setProgress(beans.roast)
            beansDetailsGramBar.    setProgress(beans.gram)
            beansDetailsShopText.   setText(beans.shop)
            beansDetailsPriceText.  setText(beans.price.toString())
            beansDetailsMemoText.   setText(beans.memo)

            // 日付Text（ちょい面倒）
            calendar.time = beans.date
            val year    = calendar.get(Calendar.YEAR)
            val month   = calendar.get(Calendar.MONTH)
            val day     = calendar.get(Calendar.DAY_OF_MONTH)
            beansDetailsDateText.text = getString(R.string.dateFormat).format(year,month+1,day)
        }

        // ーーーーーーーーーー　ここから各種のリスナ設定　ーーーーーーーーーー

        // 編集ボタン
        beansDetailsEditBtn.setOnClickListener {
            val intent = Intent(it.context, BeansEditActivity::class.java)
            intent.putExtra("id", intentID)

            // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
            // キャッチできるよう、result付きで呼び出す
            startActivityForResult(intent, REQUEST_EDIT_BEANS)
        }

//        削除ボタンはメニューに移動！
//        // 削除ボタン
//        beansDetailsDeleteBtn.setOnClickListener {
//            realm.executeTransaction {
//                realm.where<BeansData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm()
//            }
//            blackToast(applicationContext, "削除しました")
//            finish()
//        }

        // 一覧へ戻るボタン
        beansDetailsReturnBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(beansDetailsToolbar)
        supportActionBar?.title = "豆の詳細データ"

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


    } // 詳細画面のonCreate

    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    // メニュー設置
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_opt_menu_3, menu)
        return super.onCreateOptionsMenu(menu)
    }


    // メニュー選択の対応
    // TODO: ボタンでの処理と同じなので共通化したいな
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intentID = this.intent.getLongExtra("id", 0L)

        when( item.itemId ) {
            R.id.optMenu3ItemEdit -> {
                val intent = Intent(applicationContext, BeansEditActivity::class.java)
                intent.putExtra("id", intentID)

                // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
                // キャッチできるよう、result付きで呼び出す
                startActivityForResult(intent, REQUEST_EDIT_BREW)
            }

            R.id.optMenu3ItemDelete -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.deleteConfirmDialogTitle)
                builder.setMessage(R.string.deleteConfirmDialogMessage)
                builder.setCancelable(true)
                builder.setNegativeButton(R.string.deleteConfirmDialogCancelBtn, null)
                builder.setPositiveButton("OK", object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        realm.executeTransaction { realm.where<BeansData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm() }
                        blackToast(applicationContext, "削除しました")
                        finish()
                    }
                })
                builder.show()
            }

            R.id.optMenu3ItemHome -> {
                // 新機軸！ ちゃんとホームまで帰っていく！
                val intent = Intent()
                setResult( RESULT_TO_HOME, intent)
                finish()
            }

            R.id.optMenu3ItemCancel -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }




    // 編集画面から戻ってきたときは、この画面をSkipしてリスト画面に飛んでやる
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if( requestCode == REQUEST_EDIT_BEANS ) {
            when( resultCode ) {
                RESULT_TO_LIST, RESULT_OK-> {
                    finish()
                }

                // このコードでHOME画面までスタックをバックトレースしていく。ワンダフル！
                RESULT_TO_HOME -> {
                    val intent = Intent()
                    setResult(RESULT_TO_HOME, intent)
                    finish()
                }
            }


        }
    } // onActivityResult


    // 閉鎖時処理
    // RealmDBを閉める
    override fun onDestroy() {
        super.onDestroy()

        // onCreateでインスタンス化・開いていたDBをようやく閉鎖
        realm.close()
        Log.d("SHIRO", "beans-details / onDestroy")
    } // onDestroy
}
