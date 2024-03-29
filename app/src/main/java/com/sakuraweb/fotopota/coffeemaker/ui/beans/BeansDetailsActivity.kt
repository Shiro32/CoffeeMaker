package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_datails.*
import java.util.*

// BeansEditの動作モード（新規、編集、コピーして新規作成）
const val BEANS_EDIT_MODE_NEW = 1
const val BEANS_EDIT_MODE_EDIT = 2
const val BEANS_EDIT_MODE_COPY = 3
const val BEANS_EDIT_MODE_REPEAT = 4

const val REQUEST_EDIT_BEANS = 1

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
            beansDetailsGramBar.max         = configBeansBuyMax


            beansDetailsRatingBar.rating    = beans.rating
            beansDetailsRatingText.text     = "%.1f".format( beans.rating )
            beansDetailsNameText.text       = beans.name
            beansDetailsRoastBar.setProgress(beans.roast)
            beansDetailsGramBar. setProgress(beans.gram)
            beansDetailsShopText.text       = beans.shop
            beansDetailsPriceText.text      = beans.price.toString() // nullをやると死ぬので注意
            beansDetailsMemoText.text       = beans.memo
            beansDetailsRepeatText.text     = beans.repeat.toString()
            beansDetailsCountText.text      = beans.count.toString()
            beansDetailsProcessText.text    = beansProcessLabels[beans.process]
            beansDetailsCountryText.text    = beans.country

            // おもひで写真
            if( beans.imageURI!="" ) {
                try {
                    beansDetailsImage.setImageURI( Uri.parse(beans.imageURI) )
                } catch (e:Exception) {
                    // 無い時はカメラアイコン
                    beansDetailsImage.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }

            // 豆の経過日数を計算する（面倒くせぇ・・・）
            val days = "（"+((Date().time - beans.date?.time as Long)/(1000*60*60*24)).toString()+"日経過）"

            // 最新購入
            calendar.time = beans.repeatDate
            var year    = calendar.get(Calendar.YEAR)
            var month   = calendar.get(Calendar.MONTH)
            var day     = calendar.get(Calendar.DAY_OF_MONTH)
            beansDetailsRepeatDateText.text = getString(R.string.dateFormat).format(year,month+1,day)+days

            // 初回購入
            calendar.time = beans.date
            year    = calendar.get(Calendar.YEAR)
            month   = calendar.get(Calendar.MONTH)
            day     = calendar.get(Calendar.DAY_OF_MONTH)
            beansDetailsDateText.text = getString(R.string.dateFormat).format(year,month+1,day)

            // 被使用BREWから、入れた時のコメントを全部拾う
            // ratingのように、BeansListで探査して、Beansのレコードに保存するのもアリだけど、
            // List表示があまりに遅くなりそうなので、やめて個別に処理することにした
            // だけど、どうせRatingはBeansListでやっているので、そっちで処理するのも選択肢
            var comments: String = ""
            val brewRealm = Realm.getInstance(brewRealmConfig)
            val brews = brewRealm.where<BrewData>().equalTo("beansID", beans.id).findAll().sort("date", Sort.DESCENDING)
            if( brews.size>0 ) {
                for( b in brews)  {
                    if( b.memo != "" ) {
                        calendar.time = b.date
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)+1 // Todo:ようやく直した（＾＾）
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        comments += "%s（%d/%d/%d）\n\n".format(b.memo, year, month, day)
                    }
                }
            }
            brewRealm.close()
            beansDetailsCommentText.setText( if(comments!="") comments else "なし" )
        }

        // ーーーーーーーーーー　ここから各種のリスナ設定　ーーーーーーーーーー
        // 編集ボタン
        beansDetailsEditBtn.setOnClickListener {
            val intent = Intent(it.context, BeansEditActivity::class.java)
            intent.putExtra("id", intentID)
            intent.putExtra("mode", BEANS_EDIT_MODE_EDIT)

            // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
            // キャッチできるよう、result付きで呼び出す
            startActivityForResult(intent, REQUEST_EDIT_BEANS)
        }

        // 再購入ボタン
        beansDetailsRepeatBtn.setOnClickListener {
            val intent = Intent(applicationContext, BeansEditActivity::class.java)
            intent.putExtra("mode", BEANS_EDIT_MODE_REPEAT)
            intent.putExtra("id", intentID)
            blackToast(applicationContext, "同じ豆を再購入しました！")
            startActivityForResult(intent, REQUEST_EDIT_BEANS)
        }

        // 複製ボタン
        beansDetailsCopyBtn.setOnClickListener {
            val intent = Intent(applicationContext, BeansEditActivity::class.java)
            intent.putExtra("mode", BEANS_EDIT_MODE_COPY)
            intent.putExtra("id", intentID)
            blackToast(applicationContext, "データをコピーしました！")
            startActivityForResult(intent, REQUEST_EDIT_BEANS)
        }

        // 一覧へ戻るボタン
        beansDetailsReturnBtn.setOnClickListener {
            finish()
        }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(beansDetailsToolbar)
        supportActionBar?.title = getString(R.string.titleBeansDetails)

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
        val beansID = this.intent.getLongExtra("id", 0L)

        when( item.itemId ) {
            R.id.optMenu3ItemEdit -> {
                val intent = Intent(applicationContext, BeansEditActivity::class.java)
                intent.putExtra("id", beansID)
                intent.putExtra("mode", BEANS_EDIT_MODE_EDIT)

                // 編集画面に移行して戻ってきたら、この画面を飛ばしてリスト画面に行かせたい
                // キャッチできるよう、result付きで呼び出す
                startActivityForResult(intent, REQUEST_EDIT_BEANS)
            }

            R.id.optMenu3ItemCopy -> {
                val intent = Intent(applicationContext, BeansEditActivity::class.java)
                intent.putExtra("mode", BEANS_EDIT_MODE_COPY)
                intent.putExtra("id", beansID)
                blackToast(applicationContext, "同じデータで複製しました")
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
                        realm.executeTransaction { realm.where<BeansData>().equalTo("id", beansID)?.findFirst()?.deleteFromRealm() }
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

        if( requestCode == REQUEST_EDIT_BEANS ) {
            when( resultCode ) {
                RESULT_TO_LIST, RESULT_OK-> {
                    finish()
                }

                // このコードでHOME画面までスタックをバックトレースしていく。ワンダフル！
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
        Log.d("SHIRO", "beans-details / onDestroy")
    } // onDestroy
}
