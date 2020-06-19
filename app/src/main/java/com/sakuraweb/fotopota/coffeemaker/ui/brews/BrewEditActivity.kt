package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.blackToast
import com.sakuraweb.fotopota.coffeemaker.brewRealmConfig
import com.sakuraweb.fotopota.coffeemaker.toDate
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansListActivity
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_brew_edit.*
import java.util.*

const val REQUEST_CODE_BEANS_SELECT = 1

// Brewの各カードの編集画面
// 事実上、全画面表示のダイアログ
// 呼び出し元は、HomeのEditボタン（編集） or FAB（新規）
// Edit - 当該データのRealm IDをIntentで送ってくる
// FAB  - もちろん何もない

class BrewEditActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
    private var beansID: Long = 0L

    // 編集画面開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brew_edit)

        // いつもの背景クリック・確定用
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
        // configはStartActivityで生成済み
        realm = Realm.getInstance(brewRealmConfig)

        // どこから呼ばれたか（New / Edit）をこれで判定
        val intentID = this.intent.getLongExtra("id", 0L)

        // 入力ダイアログ用に現在日時を取得しておく（インスタンス化と現在日時同時）
        val calender = Calendar.getInstance()


        // 既存データをRealmDBからダイアログのViewに読み込む
        if( intentID>0L ) {
            val brew = realm.where<BrewData>().equalTo("id", intentID).findFirst()
            if (brew != null) {
                calender.time = brew.date   // 日付は面倒なので後でまとめてやる
                brewEditRatingBar.rating = brew.rating
                brewEditMethodSpin.setSelection(brew.methodID)
                brewEditBeansText.text = findBeansNameByID(brew.beansID)
                // 豆データだけはViewに保存できないのでローカル変数に
                beansID = brew.beansID
                brewEditCupsBar.setProgress(brew.cups)
                brewEditGrindBar.setProgress(brew.beansGrind)
                brewEditBeansUseBar.setProgress(brew.beansUse)
                brewEditTempBar.setProgress(brew.temp)
                brewEditSteamBar.setProgress(brew.steam)
                brewEditMemoText.setText(brew.memo)
            }
        }  else {
            // 新規データの登録（といっても「削除ボタン」の削除くらい）
            brewEditDeleteBtn.visibility = View.INVISIBLE
        }
        // ここまでで基本的に画面構成終了


        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－

        // マメ選択ボタンリスナ
        brewEditBeansText.paintFlags = brewEditBeansText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        brewEditBeansText.setOnClickListener {
            val intent = Intent(this, BeansListActivity::class.java)
            intent.putExtra("from", "Edit")
            startActivityForResult(intent, REQUEST_CODE_BEANS_SELECT)
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



        // SAVEボタンのリスナ
        // RealmDBに１件分のBREWデータを修正・追加する （intentのidによって、編集と新規作成両方やる）
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


            if( intentID==0L ) {
                // ID=0なのでDB末尾に新規保存。１行でトランザクション完結
                realm.executeTransaction {
                    // whereで最後尾を探し、そこに追記
                    val maxID = realm.where<BrewData>().max("id")
                    val nextID = (maxID?.toLong() ?: 0L) + 1L

                    // ここから書き込み
                    val brew = realm.createObject<BrewData>(nextID)
                    brew.date       = brewDate
                    brew.rating     = brewRating
                    brew.beansID    = beansID
                    brew.methodID   = methodID
                    brew.cups       = brewCups
                    brew.beansGrind = brewGrind
                    brew.beansUse   = brewBeansUse
                    brew.temp       = brewTemp
                    brew.steam      = brewSteam
                    brew.memo       = brewMemo
                }
                blackToast(applicationContext, "追加しましたぜ")

            } else {
                // ID=0ではないので既存ＤＢの修正作業。これも１行でトランザクション完結
                realm.executeTransaction {
                    val brew = realm.where<BrewData>().equalTo("id", intentID).findFirst()
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
            // 編集画面クローズ
            finish()
        } // saveBtn


        // 削除ボタン
        brewEditDeleteBtn.setOnClickListener {
            realm.executeTransaction {
                realm.where<BrewData>().equalTo("id", intentID)?.findFirst()?.deleteFromRealm()
            }
            blackToast(applicationContext, "削除しました")
            finish()
        }

        // キャンセルボタン
        brewEditCancelBtn.setOnClickListener {
            finish()
        }

        Log.d("SHIRO", "brew-edit / onCreate")

    } // 編集画面のonCreate

    override fun onDestroy() {
        super.onDestroy()

        // onCreateでインスタンス化・開いていたDBをようやく閉鎖
        realm.close()
        Log.d("SHIRO", "brew-edit / onDestroy")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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


//    private fun showDatePicker() {
//        val datePickerDialog = DatePickerDialog(
//            this,
//            DatePickerDialog.OnDateSetListener() {view, year, month, dayOfMonth->
//                textView.text = "選択した日付は「${year}/${month + 1}/${dayOfMonth}」です"
//            },
//            2020,
//            3,
//            1)
//        datePickerDialog.show()
//    }


