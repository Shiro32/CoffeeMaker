package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansListActivity
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutListActivity
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_brew_edit.*
import java.util.*

// BrewEditの動作モード（新規、編集、コピー新規、FABから）
const val BREW_EDIT_MODE_NEW = 1
const val BREW_EDIT_MODE_EDIT = 2
const val BREW_EDIT_MODE_COPY = 3

const val REQUEST_CODE_BEANS_SELECT = 1
const val REQUEST_CODE_TAKEOUT_SELECT = 2

// TODO: ほんの少しでも編集したら「戻る」も要確認　どうやって検出するの？

// Brewの各カードの編集画面
// 事実上、全画面表示のダイアログ
// 呼び出し元は、HomeのEditボタン（編集） or FAB（新規）
// Edit - 当該データのRealm IDをIntentで送ってくる
// FAB  - もちろん何もない

class BrewEditActivity : AppCompatActivity() {
    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
    // そのほか、インナークラスやonCreate以外でも使いたい変数を定義
    private var beansID: Long = 0L
    private var brewID: Long = 0L
    private var takeoutID: Long = 0L
    private var editMode: Int = 0
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 編集画面開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brew_edit)

        // ツールバータイトル用（４モード対応）
        val titles:Map<Int,Int> = mapOf(
            BREW_EDIT_MODE_NEW to R.string.titleBrewEditNew,
            BREW_EDIT_MODE_EDIT to R.string.titleBrewEditEdit,
            BREW_EDIT_MODE_COPY to R.string.titleBrewEditCopy
        )

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
        // configはStartActivityで生成済み
        realm = Realm.getInstance(brewRealmConfig)

        // 呼び出し元から、どのような動作を求められているか（新規、編集、コピー）
        editMode = intent.getIntExtra("mode", BREW_EDIT_MODE_NEW)

        // 呼び出しのBREW-LISTの、どこから呼ばれたのか（Realm上のID）
        brewID = intent.getLongExtra("id", 0L)

        // 入力ダイアログ用に現在日時を取得しておく（インスタンス化と現在日時同時）
        val calender = Calendar.getInstance()

        // 既存データをRealmDBからダイアログのViewに読み込む
        when( editMode ) {
            BREW_EDIT_MODE_NEW -> {
                // brewEditDeleteBtn.visibility = View.INVISIBLE
                brewEditCancelBtn.text = getString(R.string.returnBtnLabel)
            }

            BREW_EDIT_MODE_EDIT, BREW_EDIT_MODE_COPY -> {
                val brew = realm.where<BrewData>().equalTo("id", brewID).findFirst()
                if (brew != null) {
                    brewEditRatingBar.rating = brew.rating
                    brewEditMethodSpin.setSelection(brew.methodID)
                    brewEditBeansText.text = findBeansNameByID(brew.place, brew.beansID, brew.takeoutID)
                    // 豆データだけはViewに保存できないのでローカル変数に
                    beansID = brew.beansID
                    takeoutID = brew.takeoutID
                    brewEditCupsBar.setProgress(brew.cups)
                    brewEditCupsDrunkBar.setProgress(brew.cupsDrunk)
                    brewEditGrindBar.setProgress(brew.beansGrind)
                    brewEditBeansUseBar.setProgress(brew.beansUse)
                    brewEditTempBar.setProgress(brew.temp)
                    brewEditSteamBar.setProgress(brew.steam)
                    brewEditShopText.setText(brew.shop)

                    if( editMode==BREW_EDIT_MODE_EDIT )  {
                        // 編集モードの時の処理
                        // 時刻は既存データのものを再利用
                        calender.time = brew.date
                        brewEditMemoText.setText(brew.memo)
                    } else {
                        // 新規モードの時は、メモ欄を削除
                        brewEditMemoText.setText("")
                    }
                }
            }
        }

        // 焙煎度合いのTickを文字列で
        brewEditGrindBar.customTickTexts(grindLabels)
        brewEditGrindBar.setIndicatorTextFormat("\${TICK_TEXT}")


        // ここまでで基本的に画面構成終了
        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－

        // マメ選択ボタンリスナ
        // 抽出方法が店飲みの場合は、店飲みDBを選択する
        brewEditBeansText.setOnClickListener {
            // 店飲みDBを呼び出す
            if( brewEditMethodSpin.selectedItemPosition == BREW_METHOD_SHOP ) {
                val intent = Intent(this, TakeoutListActivity::class.java)
                intent.putExtra("from", "Edit")
                startActivityForResult(intent, REQUEST_CODE_TAKEOUT_SELECT) // これで見分ける
            } else {
                val intent = Intent(this, BeansListActivity::class.java)
                intent.putExtra("from", "Edit")
                startActivityForResult(intent, REQUEST_CODE_BEANS_SELECT)
            }
        }

        // 日付・時刻選択のダイアログボタン用
        // Date型は意外と使いにくいので、Calendar型で行こう
        val year    = calender.get(Calendar.YEAR)
        val month   = calender.get(Calendar.MONTH)
        val day     = calender.get(Calendar.DAY_OF_MONTH)
        val hour    = calender.get(Calendar.HOUR_OF_DAY)
        val min     = calender.get(Calendar.MINUTE)

        // 日付・時刻をTextViewに事前にセット
        brewEditDateText.text = getString(R.string.dateFormat).format(year,month+1,day)
        brewEditTimeText.text = getString(R.string.timeFormat).format(hour,min)

        // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
        brewEditDateText.paintFlags = brewEditDateText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        brewEditDateText.setOnClickListener {
            val dtp = DatePickerDialog(
                this, DatePickerDialog.OnDateSetListener { view, y, m, d ->
                    brewEditDateText.text = getString(R.string.dateFormat).format(y, m+1, d)
                }, year, month, day
            )
            dtp.show()
        }

        // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
        brewEditTimeText.paintFlags = brewEditTimeText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        brewEditTimeText.setOnClickListener {
            val ttp = TimePickerDialog(
                this, TimePickerDialog.OnTimeSetListener { view, h, m ->
                    brewEditTimeText.text = getString(R.string.timeFormat).format(h, m)
                }, hour, min, true
            )
            ttp.show()
        }

        // SAVEボタンのリスナ。デカいので外だし
        brewEditSaveBtn.setOnClickListener(OKButtonListener())

        // キャンセルボタン
        brewEditCancelBtn.setOnClickListener {
            val intent = Intent()
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }

        // 抽出方法のSpinnerを動かしたときのリスナ（店飲みの時、選択肢を減らすため
        brewEditMethodSpin.onItemSelectedListener = MethodSpinnerChangeListener()

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(brewEditToolbar) // これやらないと落ちるよ
        supportActionBar?.title = getString(titles[editMode] as Int)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Log.d("SHIRO", "brew-edit / onCreate")
    } // 編集画面のonCreate



    // 飲み方（家の内外）によって表示を変えるための処理
    // method Spinnerの値変更によって起動される。幸い、最初に初期値を設定するときも呼ばれるみたい。
    private inner class MethodSpinnerChangeListener() : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

            // 店飲みの場合、お湯の温度や豆挽状態を隠す
            if( brewEditMethodSpin.selectedItemPosition == BREW_METHOD_SHOP ) {
                brewEditHomeItems.visibility = View.GONE
                brewEditTakeoutItems.visibility = View.VISIBLE
            } else {
                brewEditHomeItems.visibility = View.VISIBLE
                brewEditTakeoutItems.visibility = View.GONE
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }


    // OKButton（保存）のリスナがあまりに巨大化してきたので独立
    // RealmDBに１件分のBREWデータを修正・追加する （intentのmodeによって、編集と新規作成両方やる）
    private inner class OKButtonListener() : View.OnClickListener {

        override fun onClick(v: View?) {
            Log.d("SHIRO", "brew-edit / Saveボタンリスナ。各Viewから値を取って選択中のRealmを修正してfinishする")

            // 各View（Barなどなど）からローカル変数に読み込んでおく
            val brewDate = (brewEditDateText.text as String + " " + brewEditTimeText.text as String).toDate()
            val brewRating = brewEditRatingBar.rating
            val methodID      = brewEditMethodSpin.selectedItemPosition
            val brewCups    = brewEditCupsBar.progress.toFloat()
            val brewCupsDrunk= brewEditCupsDrunkBar.progress.toFloat()
            val brewGrind   = brewEditGrindBar.progress.toFloat()
            val brewBeansUse= brewEditBeansUseBar.progress.toFloat()
            val brewTemp    = brewEditTempBar.progress.toFloat()
            val brewSteam   = brewEditSteamBar.progress.toFloat()
            val brewShop   = brewEditShopText.text.toString()
            val brewMemo   = brewEditMemoText.text.toString()

            when( editMode ) {
                // 新規作成、コピーして新規作成どちらも同じ
                // DB末尾に新規保存。１行でトランザクション完結
                BREW_EDIT_MODE_COPY, BREW_EDIT_MODE_NEW -> {
                    realm.executeTransaction {
                        // whereで最後尾を探し、そこに追記
                        val maxID = realm.where<BrewData>().max("id")
                        val nextID = (maxID?.toLong() ?: 0L) + 1L

                        // ここから書き込み
                        val brew = realm.createObject<BrewData>(nextID)
                        brew.date = brewDate
                        brew.rating = brewRating
                        brew.beansID = beansID
                        brew.methodID = methodID
                        brew.takeoutID = takeoutID
                        brew.place = if( methodID==BREW_METHOD_SHOP ) BREW_IN_SHOP else BREW_IN_HOME
                        brew.cups = brewCups
                        brew.cupsDrunk = brewCupsDrunk
                        brew.beansGrind = brewGrind
                        brew.beansUse = brewBeansUse
                        brew.temp = brewTemp
                        brew.steam = brewSteam
                        brew.shop = brewShop
                        brew.memo = brewMemo
                    }
                    blackToast(applicationContext, "追加しましたぜ")
                }

                // 既存ＤＢの編集登録
                BREW_EDIT_MODE_EDIT-> {
                    realm.executeTransaction {
                        val brew = realm.where<BrewData>().equalTo("id", brewID).findFirst()
                        brew?.date      = brewDate
                        brew?.rating    = brewRating
                        brew?.beansID   = beansID
                        brew?.takeoutID = takeoutID
                        brew?.methodID  = methodID
                        brew?.place     = if( methodID==BREW_METHOD_SHOP ) BREW_IN_SHOP else BREW_IN_HOME
                        brew?.cups      = brewCups
                        brew?.cupsDrunk = brewCupsDrunk
                        brew?.beansGrind= brewGrind
                        brew?.beansUse  = brewBeansUse
                        brew?.temp      = brewTemp
                        brew?.steam     = brewSteam
                        brew?.shop      = brewShop
                        brew?.memo      = brewMemo
                    }
                    blackToast(applicationContext, "修正完了！")
                }
            }

            // 編集画面クローズ
            // EDIT→DETAILS→LISTへ戻れるよう、setResult
            val intent = Intent()
            setResult(RESULT_TO_LIST, intent)
            finish()
        }
    }


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
//        val intentID = this.intent.getLongExtra("id", 0L)
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
        Log.d("SHIRO", "brew-edit / onDestroy")
    }

    // マメ選択画面から戻ってきたときの処理
    // 豆選択DBと、テイクアウトDBで使い分ける
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            // マメ選択画面
            REQUEST_CODE_BEANS_SELECT -> {
                when( resultCode ) {
                    RESULT_OK -> {
                        val id = data?.getLongExtra("id", 0L)
                        val name = data?.getStringExtra("name")

                        brewEditBeansText.text = name
                        beansID = id as Long
                        blackToast(applicationContext, "${name}を選択")
                    }
                    RESULT_TO_HOME -> {
                        val intent = Intent()
                        setResult(RESULT_TO_HOME, intent)
                        finish()
                    }
                }
            }

            // テイクアウト選択画面
            REQUEST_CODE_TAKEOUT_SELECT -> {
                when( resultCode ) {
                    RESULT_OK -> {
                        val id = data?.getLongExtra("id", 0L)
                        val name = data?.getStringExtra("name")

                        brewEditBeansText.text = name
                        takeoutID = id as Long
                        blackToast(applicationContext, "${name}を選択")
                    }
                    RESULT_TO_HOME -> {
                        val intent = Intent()
                        setResult(RESULT_TO_HOME, intent)
                        finish()
                    }
                }
            }
        }
    } // onActivityResult

    // 入力箇所（EditText）以外をタップしたときに、フォーカスをオフにする
    // おおもとのLayoutにfocusableInTouchModeをtrueにしないといけない
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(brewEditLayout.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)
        // 背景にフォーカスを移す
        brewEditLayout.requestFocus()
        return super.dispatchTouchEvent(event)
    }
} // Class



/*
        brewEditSaveBtn.setOnClickListener() {
            Log.d("SHIRO", "brew-edit / Saveボタンリスナ。各Viewから値を取って選択中のRealmを修正してfinishする")

            // 各View（Barなどなど）からローカル変数に読み込んでおく
            val brewDate = (brewEditDateText.text as String + " " + brewEditTimeText.text as String).toDate()
            val brewRating = brewEditRatingBar.rating
            val methodID      = brewEditMethodSpin.selectedItemPosition
            val brewCups    = brewEditCupsBar.progress.toFloat()
            val brewGrind   = brewEditGrindBar.progress.toFloat()
            val brewBeansUse= brewEditBeansUseBar.progress.toFloat()
            val brewTemp    = brewEditTempBar.progress.toFloat()
            val brewSteam   = brewEditSteamBar.progress.toFloat()
            val brewMemo   = brewEditMemoText.text.toString()


            when( mode ) {
                BREW_EDIT_MODE_COPY, BREW_EDIT_MODE_NEW -> {
                    // DB末尾に新規保存。１行でトランザクション完結
                    realm.executeTransaction {
                        // whereで最後尾を探し、そこに追記
                        val maxID = realm.where<BrewData>().max("id")
                        val nextID = (maxID?.toLong() ?: 0L) + 1L

                        // ここから書き込み
                        val brew = realm.createObject<BrewData>(nextID)
                        brew.date = brewDate
                        brew.rating = brewRating
                        brew.beansID = beansID
                        brew.methodID = methodID
                        brew.cups = brewCups
                        brew.beansGrind = brewGrind
                        brew.beansUse = brewBeansUse
                        brew.temp = brewTemp
                        brew.steam = brewSteam
                        brew.memo = brewMemo
                    }
                    blackToast(applicationContext, "追加しましたぜ")
                }

                BREW_EDIT_MODE_EDIT-> {
                    // 既存ＤＢの修正作業。これも１行でトランザクション完結
                    realm.executeTransaction {
                        val brew = realm.where<BrewData>().equalTo("id", brewID).findFirst()
                        brew?.date      = brewDate
                        brew?.rating    = brewRating
                        brew?.beansID   = beansID
                        brew?.methodID  = methodID
                        brew?.cups      = brewCups
                        brew?.beansGrind= brewGrind
                        brew?.beansUse  = brewBeansUse
                        brew?.temp      = brewTemp
                        brew?.steam     = brewSteam
                        brew?.memo      = brewMemo
                    }
                    blackToast(applicationContext, "修正完了！")
                }
            }

            // 編集画面クローズ
            // EDIT→DETAILS→LISTへ戻れるよう、setResult
            val intent = Intent()
            setResult(RESULT_TO_LIST, intent)
            finish()
        } // saveBtn
*/
