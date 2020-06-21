package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.beansRealmConfig
import com.sakuraweb.fotopota.coffeemaker.blackToast
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewEditActivity
import com.sakuraweb.fotopota.coffeemaker.ui.brews.REQUEST_EDIT_BREW
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_datails.*
import kotlinx.android.synthetic.main.activity_brew_details.*
import java.util.*

const val REQUEST_EDIT_BEANS = 1

// TODO: 編集画面からの戻り時に、スキップしてリストへ。やり方はBrewも同じ
// TODO: メニューボタン実装、キャンセルボタン名称合わせるなど
// TODO: イラストの唐突感を何とかする
// TODO: スライダが狭すぎ
// TODO: スライダがなぜか変更できる！
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

        // 削除ボタン
        beansDetailsDeleteBtn.setOnClickListener {
            realm.executeTransaction {
                realm.where<BeansData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm()
            }
            blackToast(applicationContext, "削除しました")
            finish()
        }

        // 一覧へ戻るボタン
        beansDetailsReturnBtn.setOnClickListener {
            finish()
        }
    } // 詳細画面のonCreate


    // 編集画面から戻ってきたときは、この画面をSkipしてリスト画面に飛んでやる
    // TODO: バックスタックが若干気になるけど、大丈夫か？
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if( requestCode == REQUEST_EDIT_BEANS && resultCode == Activity.RESULT_OK) finish()
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
