package com.sakuraweb.fotopota.coffeemaker.ui.brews

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
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_brew_details.*

import java.util.*

const val REQUEST_EDIT_BREW = 1

// TODO: 抽出方法のアイコンを出す（メインと同じ）
// TODO: Beansに合わせてイラスト挿入（淹れている瞬間の緩い絵が良い）

// Brewの１カードごとの詳細画面
// 当初は編集画面と一体化していたけど、List→Details→Editと３段階に変更

class BrewDetailsActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    // 詳細画面開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brew_details)

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
        // configはStartActivityで生成済み
        realm = Realm.getInstance(brewRealmConfig)

        // 表示データ（クリック元のカードのid）
        val intentID = this.intent.getLongExtra("id", 0L)

        val calendar = Calendar.getInstance()
        var dateStr:String = ""

        // Realmからデータの読み込み
        val brew = realm.where<BrewData>().equalTo("id", intentID).findFirst()
        if (brew != null) {
            brewDetailsRatingBar.rating = brew.rating
            brewDetailsMethodText.text = brewMethods[brew.methodID]
            brewDetailsBeansText.text = findBeansNameByID(brew.beansID)
            brewDetailsCupsBar.setProgress(brew.cups)
            brewDetailsGrindBar.setProgress(brew.beansGrind)
            brewDetailsBeansUseBar.setProgress(brew.beansUse)
            brewDetailsTempBar.setProgress(brew.temp)
            brewDetailsSteamBar.setProgress(brew.steam)
            brewDetailsMemoText.setText(brew.memo)

            calendar.time = brew.date
            val year    = calendar.get(Calendar.YEAR)
            val month   = calendar.get(Calendar.MONTH)
            val day     = calendar.get(Calendar.DAY_OF_MONTH)
            dateStr   = getString(R.string.dateFormat).format(year,month+1,day)
            brewDetailsDateText.text = dateStr
        }

        // 編集ボタン
        brewDetailsEditBtn.setOnClickListener {
            val intent = Intent(it.context, BrewEditActivity::class.java)
            intent.putExtra("id", intentID)

            // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
            // キャッチできるよう、result付きで呼び出す
            startActivityForResult(intent, REQUEST_EDIT_BREW)
        }



        // 一覧へ戻るボタン
        brewDetailsReturnBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(brewDetailsToolbar)
        supportActionBar?.title = dateStr+"のコーヒーの詳細"

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
                val intent = Intent(applicationContext, BrewEditActivity::class.java)
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
                        realm.executeTransaction { realm.where<BrewData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm() }
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

        if( requestCode == REQUEST_EDIT_BREW ) {
            when( resultCode ) {
                RESULT_TO_LIST, RESULT_OK -> {
                    finish()
                }

                // これで、HOME画面までスタックをバックトレースしていく！ これは機能している
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
        Log.d("SHIRO", "brew-details / onDestroy")
    }
}