package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.sakuraweb.fotopota.coffeemaker.*
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_takeout_datails.*
import java.util.*

// TakeoutEditの動作モード（新規、編集、コピーして新規作成）
const val TAKEOUT_EDIT_MODE_NEW = 1
const val TAKEOUT_EDIT_MODE_EDIT = 2
const val TAKEOUT_EDIT_MODE_COPY = 3

const val REQUEST_EDIT_TAKEOUT = 1

// TODO: イラストの唐突感を何とかする
// TODO: 円やグラムの単位

// Takeoutの１カードごとの詳細画面
// 当初は編集画面と一体化していたけど、List→Details→Editと３段階に変更

class TakeoutDetailsActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takeout_datails)

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
        // configはStartActivityで生成済み
        realm = Realm.getInstance(takeoutRealmConfig)

        // 表示データ（クリック元のカードのid
        val intentID = this.intent.getLongExtra("id", 0L)

        // Realmからデータの読み込み
        val takeout = realm.where<TakeoutData>().equalTo("id", intentID).findFirst()
        if (takeout != null) {
            takeoutDetailsRatingBar.  rating = takeout.rating
            takeoutDetailsNameText.   setText(takeout.name)
            takeoutDetailsChainText.  setText(takeout.chain)
            takeoutDetailsShopText.   setText(takeout.shop)
            takeoutDetailsPriceText.  setText(takeout.price.toString()) // nullをやると死ぬので注意
            takeoutDetailsSizeText.   setText(takeout.size)
            takeoutDetailsMemoText.   setText(takeout.memo)
        }

        // ーーーーーーーーーー　ここから各種のリスナ設定　ーーーーーーーーーー
        // 編集ボタン
        takeoutDetailsEditBtn.setOnClickListener {
            val intent = Intent(it.context, TakeoutEditActivity::class.java)
            intent.putExtra("id", intentID)
            intent.putExtra("mode", TAKEOUT_EDIT_MODE_EDIT)

            // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
            // キャッチできるよう、result付きで呼び出す
            startActivityForResult(intent, REQUEST_EDIT_TAKEOUT)
        }

        // 一覧へ戻るボタン
        takeoutDetailsReturnBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(takeoutDetailsToolbar)
        supportActionBar?.title = getString(R.string.titleTakeoutDetails)

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
        val takeoutID = this.intent.getLongExtra("id", 0L)

        when( item.itemId ) {
            R.id.optMenu3ItemEdit -> {
                val intent = Intent(applicationContext, TakeoutEditActivity::class.java)
                intent.putExtra("id", takeoutID)
                intent.putExtra("mode", TAKEOUT_EDIT_MODE_EDIT)

                // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
                // キャッチできるよう、result付きで呼び出す
                startActivityForResult(intent, REQUEST_EDIT_TAKEOUT)
            }

            R.id.optMenu3ItemCopy -> {
                val intent = Intent(applicationContext, TakeoutEditActivity::class.java)
                intent.putExtra("mode", TAKEOUT_EDIT_MODE_COPY)
                intent.putExtra("id", takeoutID)
                startActivityForResult(intent, REQUEST_EDIT_TAKEOUT)
            }

            R.id.optMenu3ItemDelete -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.deleteConfirmDialogTitle)
                builder.setMessage(R.string.deleteConfirmDialogMessage)
                builder.setCancelable(true)
                builder.setNegativeButton(R.string.deleteConfirmDialogCancelBtn, null)
                builder.setPositiveButton("OK", object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        realm.executeTransaction { realm.where<TakeoutData>().equalTo("id", takeoutID)?.findFirst()?.deleteFromRealm() }
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

        if( requestCode == REQUEST_EDIT_TAKEOUT ) {
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
        Log.d("SHIRO", "takeout-details / onDestroy")
    } // onDestroy
}
