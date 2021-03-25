package com.sakuraweb.fotopota.coffeemaker.ui.equip

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
import com.sakuraweb.fotopota.coffeemaker.ui.beans.REQUEST_CODE_BEANS_NAME_SELECT
import com.sakuraweb.fotopota.coffeemaker.ui.beans.select.BeansSelectActivity
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_edit.*
import kotlinx.android.synthetic.main.activity_equip_edit.*
import java.util.*

// TODO: ほんの少しでも編集したら「戻る」も要確認　どうやって検出するの？
const val EQUIP_EDIT_MODE_NEW = 1
const val EQUIP_EDIT_MODE_EDIT = 2
const val EQUIP_EDIT_MODE_COPY = 3

const val REQUEST_CODE_EQUIP_NAME_SELECT = 100

class EquipEditActivity : AppCompatActivity() {
    private var editMode: Int = 0
    private var equipID: Long = 0L
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
    //    private var equipID: Long = 0L

    // 編集画面構築開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equip_edit)

        // ツールバータイトル用（３モード対応）
        val titles:Map<Int,Int> = mapOf(
            EQUIP_EDIT_MODE_NEW to R.string.titleEquipEditNew,
            EQUIP_EDIT_MODE_EDIT to R.string.titleEquipEditEdit,
            EQUIP_EDIT_MODE_COPY to R.string.titleEquipEditCopy
        )

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose。configはStartActivityで生成済み
        realm = Realm.getInstance(equipRealmConfig)

        // 呼び出し元から、どのような動作を求められているか（新規、編集、コピー）
        editMode = intent.getIntExtra("mode", EQUIP_EDIT_MODE_NEW)

        // どこから呼ばれたか
        equipID = intent.getLongExtra("id", 0L)

        // 入力ダイアログ用に現在日時を取得しておく（インスタンス化と現在日時同時）
        val calender = Calendar.getInstance()

        // 既存データをRealmDBからダイアログのViewに読み込む
        when( editMode ) {
            EQUIP_EDIT_MODE_NEW -> {
                // ボタンをグレーとかあったけど、今は特になし
            }

            EQUIP_EDIT_MODE_COPY, EQUIP_EDIT_MODE_EDIT -> {
                val equip = realm.where<EquipData>().equalTo("id", equipID).findFirst()

                if( equip != null ) {
                    equipEditRatingBar.rating = equip.rating
                    equipEditNameEdit.setText(equip.name)
                    equipEditShopEdit.setText(equip.shop)
                    equipEditPriceEdit.setText(equip.price.toString())

                    if( editMode== EQUIP_EDIT_MODE_EDIT) {
                        // 編集モードではメモ欄をコピー
                        equipEditMemoEdit.setText(equip.memo)
                        // 時刻は既存データのものを再利用
                        calender.time = equip.date  // 日付は面倒なので後でまとめて・・・
                    } else {
                        // 新規モードの時は空欄で
                        equipEditMemoEdit.setText("")
                    }
                }
            }
        }


        // ここまでで基本的に画面構成終了
        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－

        // 日付・時刻選択のダイアログボタン用
        // Date型は意外と使いにくいので、Calendar型で行こう
        val year    = calender.get(Calendar.YEAR)
        val month   = calender.get(Calendar.MONTH)
        val day     = calender.get(Calendar.DAY_OF_MONTH)

        // 日付・時刻をTextViewに事前にセット
        equipEditDateText.text = getString(R.string.dateFormat).format(year,month+1,day)

        // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
        equipEditDateText.paintFlags = equipEditDateText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        equipEditDateText.setOnClickListener {
            val dtp = DatePickerDialog(
                this, DatePickerDialog.OnDateSetListener { view, y, m, d ->
                    equipEditDateText.text = getString(R.string.dateFormat).format(y, m+1, d)
                }, year, month, day
            )
            dtp.show()
        }

        // こんな感じでアイコン選ぶ？
        // もちろん結果が欲しいので、forResult付きで
//        beansEditSelectBtn.setOnClickListener {
//            val intent = Intent(it.context, BeansSelectActivity::class.java)
//            startActivityForResult(intent, REQUEST_CODE_BEANS_NAME_SELECT)
//        }


        // SAVEボタンのリスナ。あまりにバカでかいので外部に出す
        equipEditSaveBtn.setOnClickListener(OKButtonListener())

        // キャンセルボタン
        equipEditCancelBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(equipEditToolbar) // これやらないと落ちるよ
//TODO: よくわからないから消したけど、何かしないとあかｎ
//        supportActionBar?.title = getString(titles[editMode] as Int)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    } // onCreate


    // OKButton（保存）のリスナがあまりに巨大化してきたので独立
    // RealmDBに１件分のEQUIPデータを修正・追加する （intentのmodeによって、編集と新規作成両方やる）
    private inner class OKButtonListener() : View.OnClickListener {

        override fun onClick(v: View?) {
            // 各View（Barなどなど）からローカル変数に読み込んでおく
            val equipDate = Date()
            val equipName = equipEditNameEdit.text.toString()
            val equipRating= equipEditRatingBar.progress.toFloat()
            val equipShop= equipEditShopEdit.text.toString()
            val equipMemo = equipEditMemoEdit.text.toString()
            val equipPrice = if (equipEditPriceEdit.text.isNullOrEmpty()) {
                0
            } else {
                equipEditPriceEdit.text.toString().toInt()
            }

            // Realmに書き込む
            when (editMode) {
                // 新規作成、コピーして新規作成どちらも同じ
                // DB末尾に新規保存。１行でトランザクション完結
                EQUIP_EDIT_MODE_COPY, EQUIP_EDIT_MODE_NEW -> {
                    realm.executeTransaction {
                        // whereで最後尾を探し、そこに追記
                        val maxID = realm.where<EquipData>().max("id")
                        val nextID = (maxID?.toLong() ?: 0L) + 1L

                        val equip = realm.createObject<EquipData>(nextID)
                        equip.date = equipDate
                        equip.rating = equipRating
                        equip.name = equipName
                        equip.shop = equipShop
                        equip.price = equipPrice
                        equip.memo = equipMemo
                    }
                    blackToast(applicationContext, "追加しましたぜ")
                }

                // 既存ＤＢの編集登録
                EQUIP_EDIT_MODE_EDIT -> {
                    realm.executeTransaction {
                        val equip = realm.where<EquipData>().equalTo("id", equipID).findFirst()
                        equip?.date = equipDate
                        equip?.rating = equipRating
                        equip?.name = equipName
                        equip?.shop = equipShop
                        equip?.price = equipPrice
                        equip?.memo = equipMemo
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



/*
    // 豆銘柄獲得の画面から、戻ってきたらViewに格納してあげる
    // 店舗名ＤＢを持つのが面倒くさいので、製品名の前半部分を切り離して系列名称にする（適当だなぁ・・・）
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val shopNameList: Map<String, String> = mapOf(
            "スタバ" to "スターバックス",
            "ファミマ" to "ファミリーマート",
            "セブン" to "セブンイレブン"
        )

        when( requestCode ) {
            REQUEST_CODE_EQUIP_NAME_SELECT -> {
                when( resultCode ) {
                    RESULT_OK -> {
                        val name = data?.getStringExtra("name")
                        val sep = name?.indexOf("】")

                        if( sep!=null && sep!=-1 ) {
                            var chain = name?.substring(1,sep)
                            chain = shopNameList.getOrDefault(chain, chain)

                            equipEditShopEdit.setText(chain)
                            equipEditNameEdit.setText(name?.substring(sep+1, name.length))
                        } else {
                            equipEditNameEdit.setText(name)
                        }
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
*/


    // 入力箇所（EditText）以外をタップしたときに、フォーカスをオフにする
    // おおもとのLayoutにfocusableInTouchModeをtrueにしないといけない
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(
            equipEditLayout.getWindowToken(),
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        // 背景にフォーカスを移す
        equipEditLayout.requestFocus()
        return super.dispatchTouchEvent(event)
    }


}
// class
