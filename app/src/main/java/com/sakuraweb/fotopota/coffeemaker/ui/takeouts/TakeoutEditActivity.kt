package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.select.BeansSelectActivity
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.select.TakeoutSelectActivity
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_takeout_edit.*

// TODO: LISTへ戻るメニューっている？ さすがにくどくない？
// TODO: イラストの唐突感を何とかする
// TODO: ほんの少しでも編集したら「戻る」も要確認　どうやって検出するの？

const val REQUEST_CODE_TAKEOUT_NAME_SELECT = 100

class TakeoutEditActivity : AppCompatActivity() {
    private var editMode: Int = 0
    private var takeoutID: Long = 0L
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
//    private var takeoutID: Long = 0L

    // 編集画面構築開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takeout_edit)

        // ツールバータイトル用（３モード対応）
        val titles:Map<Int,Int> = mapOf(
            TAKEOUT_EDIT_MODE_NEW to R.string.titleTakeoutEditNew,
            TAKEOUT_EDIT_MODE_EDIT to R.string.titleTakeoutEditEdit,
            TAKEOUT_EDIT_MODE_COPY to R.string.titleTakeoutEditCopy
        )

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose。configはStartActivityで生成済み
        realm = Realm.getInstance(takeoutRealmConfig)

        // 呼び出し元から、どのような動作を求められているか（新規、編集、コピー）
        editMode = intent.getIntExtra("mode", TAKEOUT_EDIT_MODE_NEW)

        // どこから呼ばれたか
        takeoutID = intent.getLongExtra("id", 0L)

        // 既存データをRealmDBからダイアログのViewに読み込む
        when( editMode ) {
            TAKEOUT_EDIT_MODE_NEW -> {
                // ボタンをグレーとかあったけど、今は特になし
            }

            TAKEOUT_EDIT_MODE_COPY, TAKEOUT_EDIT_MODE_EDIT -> {
                val takeout = realm.where<TakeoutData>().equalTo("id", takeoutID).findFirst()

                if( takeout != null ) {
                    takeoutEditRatingBar.rating = takeout.rating
                    takeoutEditNameEdit.setText(takeout.name)
//                    takeoutEditShopEdit.setText(takeout.shop)
                    takeoutEditChainEdit.setText(takeout.chain)
                    takeoutEditPriceEdit.setText(takeout.price.toString())
                    takeoutEditSizeEdit.setText(takeout.size)
                    takeoutEditMemoEdit.setText(takeout.memo)
                }
            }
        }

//        // 焙煎度合いのTickを文字列で
//        takeoutEditRoastBar.customTickTexts(roastLabels)
//        takeoutEditRoastBar.setIndicatorTextFormat("\${TICK_TEXT}")


        // ここまでで基本的に画面構成終了
        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－

        // テイクアウトメニューセレクト画面へ
        // もちろん結果が欲しいので、forResult付きで
        takeoutEditSelectBtn.setOnClickListener {
            val intent = Intent(it.context, TakeoutSelectActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_TAKEOUT_NAME_SELECT)
        }

        // SAVEボタンのリスナ。あまりにバカでかいので外部に出す
        takeoutEditSaveBtn.setOnClickListener(OKButtonListener())

        // キャンセルボタン
        takeoutEditCancelBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(takeoutEditToolbar) // これやらないと落ちるよ
        supportActionBar?.title = getString(titles[editMode] as Int)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    } // onCreate


    // OKButton（保存）のリスナがあまりに巨大化してきたので独立
    // RealmDBに１件分のTAKEOUTデータを修正・追加する （intentのmodeによって、編集と新規作成両方やる）
    private inner class OKButtonListener() : View.OnClickListener {

        override fun onClick(v: View?) {
            // 各View（Barなどなど）からローカル変数に読み込んでおく
            val takeoutName = takeoutEditNameEdit.text.toString()
            val takeoutRating= takeoutEditRatingBar.progress.toFloat()
            val takeoutChain= takeoutEditChainEdit.text.toString()
//            val takeoutShop = takeoutEditShopEdit.text.toString()
            val takeoutMemo = takeoutEditMemoEdit.text.toString()
            val takeoutSize = takeoutEditSizeEdit.text.toString()
            val takeoutPrice = if (takeoutEditPriceEdit.text.isNullOrEmpty()) {
                0
            } else {
                takeoutEditPriceEdit.text.toString().toInt()
            }

            // Realmに書き込む
            when (editMode) {
                // 新規作成、コピーして新規作成どちらも同じ
                // DB末尾に新規保存。１行でトランザクション完結
                TAKEOUT_EDIT_MODE_COPY, TAKEOUT_EDIT_MODE_NEW -> {
                    realm.executeTransaction {
                        // whereで最後尾を探し、そこに追記
                        val maxID = realm.where<TakeoutData>().max("id")
                        val nextID = (maxID?.toLong() ?: 0L) + 1L

                        val takeout = realm.createObject<TakeoutData>(nextID)
                        takeout.rating = takeoutRating
                        takeout.name = takeoutName
                        takeout.chain = takeoutChain
//                        takeout.shop = takeoutShop
                        takeout.price = takeoutPrice
                        takeout.size = takeoutSize
                        takeout.memo = takeoutMemo
                    }
                    blackToast(applicationContext, "追加しましたぜ")
                }

                // 既存ＤＢの編集登録
                TAKEOUT_EDIT_MODE_EDIT -> {
                    realm.executeTransaction {
                        val takeout = realm.where<TakeoutData>().equalTo("id", takeoutID).findFirst()
                        takeout?.rating = takeoutRating
                        takeout?.name = takeoutName
                        takeout?.chain = takeoutChain
//                        takeout?.shop = takeoutShop
                        takeout?.price = takeoutPrice
                        takeout?.size = takeoutSize
                        takeout?.memo = takeoutMemo
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
            REQUEST_CODE_TAKEOUT_NAME_SELECT -> {
                when( resultCode ) {
                    RESULT_OK -> {
                        val name = data?.getStringExtra("name")
                        val sep = name?.indexOf("】")

                        if( sep!=null && sep!=-1 ) {
                            var chain = name?.substring(1,sep)
                            chain = shopNameList.getOrDefault(chain, chain)

                            takeoutEditChainEdit.setText(chain)
                            takeoutEditNameEdit.setText(name?.substring(sep+1, name.length))
                        } else {
                            takeoutEditNameEdit.setText(name)
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


    // 入力箇所（EditText）以外をタップしたときに、フォーカスをオフにする
    // おおもとのLayoutにfocusableInTouchModeをtrueにしないといけない
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(
            takeoutEditLayout.getWindowToken(),
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        // 背景にフォーカスを移す
        takeoutEditLayout.requestFocus()
        return super.dispatchTouchEvent(event)
    }


}
// class
