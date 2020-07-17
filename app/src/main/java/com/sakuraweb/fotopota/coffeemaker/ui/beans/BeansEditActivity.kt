package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.select.BeansSelectActivity
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.select.TakeoutSelectActivity
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_edit.*
import java.util.*

// TODO: LISTへ戻るメニューっている？ さすがにくどくない？
// TODO: イラストの唐突感を何とかする
// TODO: ほんの少しでも編集したら「戻る」も要確認　どうやって検出するの？
// TODO: 起動時に、一回、全データをBREWSからの参照チェックやるべき。いつやる？ Takeoutも同じだけど。

const val REQUEST_CODE_BEANS_NAME_SELECT = 1

class BeansEditActivity : AppCompatActivity() {
    private var editMode: Int = 0
    private var beansID: Long = 0L
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
//    private var beansID: Long = 0L

    // 編集画面構築開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beans_edit)

        // ツールバータイトル用（３モード対応）
        val titles:Map<Int,Int> = mapOf(
            BEANS_EDIT_MODE_NEW to R.string.titleBeansEditNew,
            BEANS_EDIT_MODE_EDIT to R.string.titleBeansEditEdit,
            BEANS_EDIT_MODE_COPY to R.string.titleBeansEditCopy
        )

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose。configはStartActivityで生成済み
        realm = Realm.getInstance(beansRealmConfig)

        // 呼び出し元から、どのような動作を求められているか（新規、編集、コピー）
        editMode = intent.getIntExtra("mode", BEANS_EDIT_MODE_NEW)

        // どこから呼ばれたか
        beansID = intent.getLongExtra("id", 0L)

        // 入力ダイアログ用に現在日時を取得しておく（インスタンス化と現在日時同時）
        val calender = Calendar.getInstance()

        // 既存データをRealmDBからダイアログのViewに読み込む
        when( editMode ) {
            BEANS_EDIT_MODE_NEW -> {
                // ボタンをグレーとかあったけど、今は特になし
            }

            BEANS_EDIT_MODE_COPY, BEANS_EDIT_MODE_EDIT -> {
                val beans = realm.where<BeansData>().equalTo("id", beansID).findFirst()

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
            }
        }

        // 焙煎度合いのTickを文字列で
        beansEditRoastBar.customTickTexts(roastLabels)
        beansEditRoastBar.setIndicatorTextFormat("\${TICK_TEXT}")


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
            val intent = Intent(it.context, BeansSelectActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_BEANS_NAME_SELECT)
        }

        // SAVEボタンのリスナ。あまりにバカでかいので外部に出す
        beansEditSaveBtn.setOnClickListener(OKButtonListener())

        // キャンセルボタン
        beansEditCancelBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(beansEditToolbar) // これやらないと落ちるよ
        supportActionBar?.title = getString(titles[editMode] as Int)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    } // onCreate


    // OKButton（保存）のリスナがあまりに巨大化してきたので独立
    // RealmDBに１件分のBEANSデータを修正・追加する （intentのmodeによって、編集と新規作成両方やる）
    private inner class OKButtonListener() : View.OnClickListener {

        override fun onClick(v: View?) {
            // 各View（Barなどなど）からローカル変数に読み込んでおく
            val beansDate = beansEditDateText.text.toString().toDate("yyyy/MM/dd")
            val beansRating = beansEditRatingBar.progress.toFloat()
            val beansName = beansEditNameEdit.text.toString()
            val beansRoast = beansEditRoastBar.progress.toFloat()
            val beansGram = beansEditGramBar.progress.toFloat()
            val beansShop = beansEditShopEdit.text.toString()
            val beansMemo = beansEditMemoEdit.text.toString()
            val beansPrice = if (beansEditPriceEdit.text.isNullOrEmpty()) {
                0
            } else {
                beansEditPriceEdit.text.toString().toInt()
            }

            // Realmに書き込む
            when (editMode) {
                // 新規作成、コピーして新規作成どちらも同じ
                // DB末尾に新規保存。１行でトランザクション完結
                BEANS_EDIT_MODE_COPY, BEANS_EDIT_MODE_NEW -> {
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
                }

                // 既存ＤＢの編集登録
                BEANS_EDIT_MODE_EDIT -> {
                    realm.executeTransaction {
                        val beans = realm.where<BeansData>().equalTo("id", beansID).findFirst()
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
            }

            // 編集画面を閉める
            // EDIT→DETAILS→LISTへ戻れるよう、setResult
            val intent = Intent()
            setResult(RESULT_TO_LIST, intent)
            finish()
        } // override onClick
    } // OnClickListener

    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    // メニュー設置
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_opt_menu_4, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // メニュー選択の対応
    // TODO: ボタンでの処理と同じなので共通化したいな
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when( item.itemId ) {
            // saveは面倒くさいので後回し・・・。

            R.id.optMenu4ItemHome -> {
                // 新機軸！ ちゃんとホームまで帰っていく！
                val intent = Intent()
                setResult( RESULT_TO_HOME, intent)
                finish()
            }

            R.id.optMenu4ItemCancel -> {
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()

        // onCreateでインスタンス化・開いていたDBをようやく閉鎖
        realm.close()
//        Log.d("SHIRO", "brew-edit / onDestroy")
    }


    // 豆銘柄獲得の画面から、戻ってきたらViewに格納してあげる
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when( requestCode ) {
            REQUEST_CODE_BEANS_NAME_SELECT -> {
                when( resultCode ) {
                    RESULT_OK -> {
                        val name = data?.getStringExtra("name")
                        beansEditNameEdit.setText(name)
                        // ここで銘柄選択ポップアップTOASTを出してもいいんだけど・・・。
                        // BrewEditでも出すのでここでは出さない
                    }
                    RESULT_TO_HOME -> {
                        val intent = Intent()
                        setResult(RESULT_TO_HOME, intent)
                        finish()
                    }
                }
            }
        }
    }


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
