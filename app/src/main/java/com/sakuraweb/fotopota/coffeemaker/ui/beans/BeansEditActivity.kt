package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.select.BeansSelectAvtivity
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_edit.*
import java.util.*


// TODO: 粗挽きなどのスライダのポップアップを直す　        m2.setIndicatorTextFormat("\${TICK_TEXT}")
// TODO: どうせなら全部やる？（↑）
// TODO: おそらく空欄があるとクラッシュするので直す
// TODO: 新規作成と既存編集時でタイトルを正しく合わせる
// TODO: 決定時にDetailsを飛ばしてリストまで戻してあげたい（Details側の処理か？）
// TODO: オプションメニュー、戻るメニューの設置、編集画面ボタンの整理（削除の削除、保存・キャンセルの位置等）
// TODO: 削除メニューには確認ダイアログを忘れずに
// TODO: 「使用マメ」テキストがイマイチクリッカブル感が無いので、ボタンにしちゃう？
// TODO: 円やグラムの単位
// TODO: スライダが狭すぎ
// TODO: 日付ポップアップボタン見えにくくない？
// TODO: イラストの唐突感を何とかする

const val REQUEST_CODE_BEANS_NAME_SELECT = 1

class BeansEditActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
//    private var beansID: Long = 0L

    // 編集画面構築開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beans_edit)

        // いつもの背景クリック・確定用
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose。configはStartActivityで生成済み
        realm = Realm.getInstance(beansRealmConfig)

        // どこから呼ばれたか（New / Edit）をこれで判定
        val intentID = this.intent.getLongExtra("id", 0L)

        // 入力ダイアログ用に現在日時を取得しておく（インスタンス化と現在日時同時）
        val calender = Calendar.getInstance()


        // 既存データをRealmDBからダイアログのViewに読み込む
        if( intentID>0L ) {
            val beans = realm.where<BeansData>().equalTo("id", intentID).findFirst()

            if( beans != null ) {
                calender.time = beans.date  // 日付は面倒なので後でまとめて・・・
                beansEditRatingBar.rating = beans.rating
                beansEditNameEdit.setText(beans.name)
                beansEditRoastBar.setProgress(beans.roast)
                beansEditGramBar.setProgress(beans.gram)
                beansEditShopEdit.setText(beans.shop)
                beansEditPriceEdit.setText(beans.price.toString())
                beansEditMemoEdit.setText(beans.memo)
            }
        } else {
            // 新規データの登録（といっても「削除ボタン」の削除くらい）
            beansEditDeleteBtn.visibility = View.INVISIBLE
        }
        // ここまでで基本的に画面構成終了


        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－

        // 日付・時刻選択のダイアログボタン用
        // Date型は意外と使いにくいので、Calendar型で行こう
        val year    = calender.get(Calendar.YEAR)
        val month   = calender.get(Calendar.MONTH)
        val day     = calender.get(Calendar.DAY_OF_MONTH)

        // 日付・時刻をTextViewに事前にセット
        beansEditDateText.text = getString(R.string.dateFormat).format(year,month+1,day)

        // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
        beansEditDateText.paintFlags = beansEditDateText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        beansEditDateText.setOnClickListener {
            val dtp = DatePickerDialog(
                this, DatePickerDialog.OnDateSetListener { view, y, m, d ->
                    beansEditDateText.text = getString(R.string.dateFormat).format(y, m+1, d)
                }, year, month, day
            )
            dtp.show()
        }

        // 豆セレクト画面へ
        // もちろん結果が欲しいので、forResult付きで
        beansEditSelectBtn.setOnClickListener {
            val intent = Intent(it.context, BeansSelectAvtivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_BEANS_NAME_SELECT)
        }


        // いよいよメインディッシュSAVEボタンのリスナ
        // RealmDBに１件分のBEANSデータを修正・追加する （intentのidによって、編集と新規作成両方やる）
        beansEditSaveBtn.setOnClickListener() {
            // 各View（Barなどなど）からローカル変数に読み込んでおく
            val beansDate   = beansEditDateText.text.toString().toDate("yyyy/MM/dd")
            val beansRating  = beansEditRatingBar.progress.toFloat()
            val beansName   = beansEditNameEdit.text.toString()
            val beansRoast  = beansEditRoastBar.progress.toFloat()
            val beansGram   = beansEditGramBar.progress.toFloat()
            val beansShop   = beansEditShopEdit.text.toString()
            val beansPrice  = beansEditPriceEdit.text.toString().toInt()
            val beansMemo   = beansEditMemoEdit.text.toString()

            // Realmに書き込む
            if( intentID==0L ) {
                // ID=0なのでDB末尾に新規保存。１行でトランザクション完結
                realm.executeTransaction {
                    // whereで最後尾を探し、そこに追記
                    val maxID = realm.where<BeansData>().max("id")
                    val nextID = (maxID?.toLong() ?: 0L) + 1L

                    val beans = realm.createObject<BeansData>(nextID)
                    beans.date = beansDate
                    beans.rating = beansRating
                    beans.name = beansName
                    beans.roast = beansRoast
                    beans.gram = beansGram
                    beans.shop = beansShop
                    beans.price = beansPrice
                    beans.memo = beansMemo
                }
                blackToast(applicationContext, "追加しましたぜ")
            } else {
                // ID=0ではないので既存ＤＢの修正作業。これも１行でトランザクション完結
                realm.executeTransaction {
                    val beans = realm.where<BeansData>().equalTo("id", intentID).findFirst()
                    beans?.date = beansDate
                    beans?.rating = beansRating
                    beans?.name = beansName
                    beans?.roast = beansRoast
                    beans?.gram = beansGram
                    beans?.shop = beansShop
                    beans?.price = beansPrice
                    beans?.memo = beansMemo
                }
                blackToast(applicationContext, "修正完了！")
            }

            // 編集画面を閉める
            finish()
        } // SaveBtn


        // 削除ボタン
        beansEditDeleteBtn.setOnClickListener {
            realm.executeTransaction {
                realm.where<BeansData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm()
            }
            blackToast(applicationContext, "削除しました")
            finish()
        }

        // キャンセルボタン
        beansEditCancelBtn.setOnClickListener {
            finish()
        }


    } // onCreate


    // 豆銘柄獲得の画面から、戻ってきたらViewに格納してあげる
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if( resultCode==RESULT_OK && requestCode==REQUEST_CODE_BEANS_NAME_SELECT) {
            val name = data?.getStringExtra("name")

            beansEditNameEdit.setText(name)
        }
    }






    override fun onDestroy() {
        super.onDestroy()

        // onCreateでインスタンス化・開いていたDBをようやく閉鎖
        realm.close()
//        Log.d("SHIRO", "brew-edit / onDestroy")
    }

    // ここで、豆ネーム選択のげったを作る
/*    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            // マメ選択画面
            REQUEST_CODE_BEANS_SELECT -> {
                if (resultCode == RESULT_OK) {
                    val id = data?.getLongExtra("id", 0L)
                    val name = data?.getStringExtra("name")

                    brewEditBeansText.text = name
                    beansID = id as Long
                    blackToast(applicationContext, "${name}/ID${beansID}を選択しました")
                }
            }
        }
    } // onActivityResult
    */

    // 入力箇所（EditText）以外をタップしたときに、フォーカスをオフにする
    // おおもとのLayoutにfocusableInTouchModeをtrueにしないといけない
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(
            beansEditLayout.getWindowToken(),
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        // 背景にフォーカスを移す
        beansEditLayout.requestFocus()
        return super.dispatchTouchEvent(event)
    }


}
// class
