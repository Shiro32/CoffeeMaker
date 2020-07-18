package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.REQUEST_EDIT_BEANS
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansDateByID
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_brew_details_home.*
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsBeansText
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsCupsBar
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsDateText
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsTimeText
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsEditBtn
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsMemoText
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsMethodImage
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsMethodText
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsRatingBar
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsReturnBtn
import kotlinx.android.synthetic.main.activity_brew_details_home.brewDetailsToolbar
import kotlinx.android.synthetic.main.activity_brew_details_shop.*
import java.time.LocalDateTime

import java.util.*

const val REQUEST_EDIT_BREW = 1

// Brewの１カードごとの詳細画面
// 当初は編集画面と一体化していたけど、List→Details→Editと３段階に変更

class BrewDetailsActivity : AppCompatActivity() {
    private lateinit var realm: Realm

    // 詳細画面開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
        // configはStartActivityで生成済み
        realm = Realm.getInstance(brewRealmConfig)

        // 表示データ（クリック元のカードのid）
        val intentID = this.intent.getLongExtra("id", 0L)

        val calendar = Calendar.getInstance()

        // Realmからデータの読み込み
        val brew = realm.where<BrewData>().equalTo("id", intentID).findFirst()


        if (brew != null) {
            var days = ""
            // 家飲みの場合は抽出情報（店飲みの場合は不要）
            if( brew.place == BREW_IN_HOME ) {
                // ここでようやくレイアウトをインフレート
                setContentView(R.layout.activity_brew_details_home)

                brewDetailsGrindBar.setProgress(brew.beansGrind)
                brewDetailsBeansUseBar.setProgress(brew.beansUse)
                brewDetailsCupsBar.setProgress(brew.cups)
                brewDetailsCupsDrunkBar.setProgress(brew.cupsDrunk)
                brewDetailsTempBar.setProgress(brew.temp)
                brewDetailsSteamBar.setProgress(brew.steam)
                // 豆の経過日数を計算する
                if(brew.beansID>0L) {
                    val d1 = findBeansDateByID(brew.beansID)?.time
                    if (d1!=null)  days = "（"+((brew.date.time-d1)/(1000*60*60*24)).toString()+"日経過）"
                }
            } else {
                setContentView(R.layout.activity_brew_details_shop)
                brewDetailsShopText.setText(brew.shop)
            }

            // 家飲み・店飲み共通項目
//            val dd: LocalDateTime = LocalDateTime.now()
            brewDetailsRatingBar.rating = brew.rating
            brewDetailsMethodText.text = brewMethods[brew.methodID]
            brewDetailsBeansText.text = findBeansNameByID(brew.place, brew.beansID, brew.takeoutID) + days
            brewDetailsMemoText.setText(brew.memo)

            // 抽出方法にあったイラスト（アイコン）
            brewDetailsMethodImage.setImageDrawable(brewMethodsImages.getDrawable(brew.methodID))

            calendar.time = brew.date
            val year    = calendar.get(Calendar.YEAR)
            val month   = calendar.get(Calendar.MONTH)
            val day     = calendar.get(Calendar.DAY_OF_MONTH)
            val hour    = calendar.get(Calendar.HOUR_OF_DAY)
            val min     = calendar.get(Calendar.MINUTE)

            brewDetailsDateText.text = getString(R.string.dateFormat).format(year,month+1,day)
            brewDetailsTimeText.text = getString(R.string.timeFormat).format(hour,min)
        }



        // ーーーーーーーーーー　ここから各種のリスナ設定　ーーーーーーーーーー
        // 編集ボタン
        brewDetailsEditBtn.setOnClickListener {
            val intent = Intent(it.context, BrewEditActivity::class.java)
            intent.putExtra("id", intentID)
            intent.putExtra("mode", BREW_EDIT_MODE_EDIT)

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
        supportActionBar?.title = getString(R.string.titleBrewDetails)

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
        val brewID = this.intent.getLongExtra("id", 0L)

        when( item.itemId ) {
            R.id.optMenu3ItemEdit -> {
                val intent = Intent(applicationContext, BrewEditActivity::class.java)
                intent.putExtra("id", brewID)
                intent.putExtra("mode", BREW_EDIT_MODE_EDIT)

                // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
                // キャッチできるよう、result付きで呼び出す
                startActivityForResult(intent, REQUEST_EDIT_BREW)
            }

            R.id.optMenu3ItemCopy -> {
                val intent = Intent(applicationContext, BrewEditActivity::class.java)
                intent.putExtra("mode", BREW_EDIT_MODE_COPY)
                intent.putExtra("id", brewID)
                startActivityForResult(intent, REQUEST_EDIT_BEANS)
            }

            R.id.optMenu3ItemDelete -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.deleteConfirmDialogTitle)
                builder.setMessage(R.string.deleteConfirmDialogMessage)
                builder.setCancelable(true)
                builder.setNegativeButton(R.string.deleteConfirmDialogCancelBtn, null)
                builder.setPositiveButton("OK", object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        realm.executeTransaction { realm.where<BrewData>().equalTo("id", brewID)?.findFirst()?.deleteFromRealm() }
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