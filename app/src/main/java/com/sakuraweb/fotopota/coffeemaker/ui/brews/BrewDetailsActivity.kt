package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.blackToast
import com.sakuraweb.fotopota.coffeemaker.brewMethods
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_brew_details.*
import kotlinx.android.synthetic.main.activity_brew_edit.*

import java.util.*

const val REQUEST_EDIT_BREW = 1

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

            calendar.time = brew.date   // 日付は面倒なので後でまとめてやる
            val year    = calendar.get(Calendar.YEAR)
            val month   = calendar.get(Calendar.MONTH)
            val day     = calendar.get(Calendar.DAY_OF_MONTH)
            brewDetailsDateText.text = getString(R.string.dateFormat).format(year,month+1,day)
        }

        // 編集ボタン
        brewDetailsEditBtn.setOnClickListener {
            val intent = Intent(it.context, BrewEditActivity::class.java)
            intent.putExtra("id", intentID)

            // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
            // キャッチできるよう、result付きで呼び出す
            startActivityForResult(intent, REQUEST_EDIT_BREW)
        }

        // 削除ボタン
        brewDetailsDeleteBtn.setOnClickListener {
            realm.executeTransaction {
                realm.where<BrewData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm()
            }
            blackToast(applicationContext, "削除しました")
            finish()
        }

        // 一覧へ戻るボタン
        brewDetailsReturnBtn.setOnClickListener {
            finish()
        }
    } // 詳細画面のonCreate


    // 編集画面から戻ってきたときは、この画面をSkipしてリスト画面に飛んでやる
    // TODO: バックスタックが若干気になるけど、大丈夫か？
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if( requestCode == REQUEST_EDIT_BREW && resultCode == Activity.RESULT_OK) finish()
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