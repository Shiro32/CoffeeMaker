package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.select.BeansSelectActivity
import com.warkiz.widget.IndicatorSeekBar
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_edit.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// TODO: ほんの少しでも編集したら「戻る」も要確認　どうやって検出するの？
// TODO: 起動時に、一回、全データをBREWSからの参照チェックやるべき。いつやる？ Takeoutも同じだけど。

const val REQUEST_CODE_BEANS_NAME_SELECT = 1
const val REQUEST_BEANS_PHOTO_SELECT = 100
const val REQUEST_BEANS_PHOTO_TAKE = 101
const val REQUEST_BEANS_STORAGE_PERMISSION = 102

// BEANS EDIT
// BEANS LISTから呼び出されて、豆データの編集を行う
// intent:（4モード）
//      BEANS_EDIT_MODE_EDIT: 既存データの編集
//      BEANS_EDIT_MODE_COPY: 既存データの複製
//      BEANS_EDIT_MODE_NEW : 新規データ入力
//      BEANS_EDIT_MODE_REPEAT: 再購入データの入力（既存データをコピーしてカウント＋１）
class BeansEditActivity : AppCompatActivity() {
    private var editMode: Int = 0
    private var beansID: Long = 0L
    private var _imageUri: Uri? = null
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 使用マメのIDを保持する
    // 他のBarやMethodのように、Viewに持たせることができないので、ローカル変数に保持する
//    private var beansID: Long = 0L

    // 編集画面構築開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beans_edit)

        // ツールバータイトル用（４モード対応）
        val titles:Map<Int,Int> = mapOf(
            BEANS_EDIT_MODE_NEW to R.string.titleBeansEditNew,
            BEANS_EDIT_MODE_EDIT to R.string.titleBeansEditEdit,
            BEANS_EDIT_MODE_COPY to R.string.titleBeansEditCopy,
            BEANS_EDIT_MODE_REPEAT to R.string.titleBeansEditRepeat
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
        val firstDate = Calendar.getInstance()
        val repeatDate = Calendar.getInstance()

        // 既存データをRealmDBからダイアログのViewに読み込む
        when( editMode ) {
            // 新規作成
            BEANS_EDIT_MODE_NEW -> {
                beansEditRepeatText.text = "1"
            }

            // 編集、　再購入、複製
            BEANS_EDIT_MODE_REPEAT, BEANS_EDIT_MODE_EDIT, BEANS_EDIT_MODE_COPY -> {
                val beans = realm.where<BeansData>().equalTo("id", beansID).findFirst()

                if (beans != null) {
                    beansEditRatingBar.rating = beans.rating
                    beansEditRatingText.text = String.format("%.1f", beans.rating)

                    beansEditNameEdit.setText(beans.name)
                    beansEditRoastBar.setProgress(beans.roast)
                    beansEditGramBar.setProgress(beans.gram)
                    beansEditShopEdit.setText(beans.shop)
                    beansEditPriceEdit.setText(beans.price.toString())
                    beansEditMemoEdit.setText(beans.memo)

                    beansEditMemoEdit.setText(beans.memo)
                    beansEditProcessSpinner.setSelection(beans.process)

                    when (editMode) {
                        BEANS_EDIT_MODE_EDIT -> {
                            beansEditRepeatText.text = beans.repeat.toString()
                            firstDate.time = beans.date
                            repeatDate.time = beans.repeatDate as Date
                        }
                        BEANS_EDIT_MODE_COPY -> {
                            beansEditRepeatText.text = "1"
                        }
                        BEANS_EDIT_MODE_REPEAT -> {
                            beansEditRepeatText.text = (beans.repeat + 1).toString()
                            firstDate.time = beans.date
                            // ほぼすべての項目をinactiveに
                            beansEditNameEdit.focusable = View.NOT_FOCUSABLE
                            beansEditNameEdit.isEnabled = false
                            beansEditSelectBtn.focusable = View.NOT_FOCUSABLE
                            beansEditSelectBtn.isEnabled = false
                            beansEditRoastBar.focusable = View.NOT_FOCUSABLE
                            beansEditRoastBar.isEnabled = false
                            beansEditGramBar.focusable = View.NOT_FOCUSABLE
                            beansEditGramBar.isEnabled = false
                            beansEditShopEdit.focusable = View.NOT_FOCUSABLE
                            beansEditShopEdit.isEnabled = false
                            beansEditPriceEdit.focusable = View.NOT_FOCUSABLE
                            beansEditPriceEdit.isEnabled = false
                            beansEditProcessSpinner.focusable = View.NOT_FOCUSABLE
                            beansEditProcessSpinner.isEnabled = false
                        }
                    }

                    if (beans.imageURI != "") {
                        try {
                            _imageUri = Uri.parse( beans.imageURI )
                            beansEditImage.setImageURI( _imageUri )
                        } catch( e:Exception ) {
                            beansEditImage.setImageResource(android.R.drawable.ic_menu_report_image)
                        }
                    }
                }
            }
        }

        // 焙煎度合いのTickを文字列で
        beansEditRoastBar.customTickTexts(roastLabels)
        beansEditRoastBar.setIndicatorTextFormat("\${TICK_TEXT}")

        // 精製処理は共通なのでここに
//        val adapter = ArrayAdapter<String>(applicationContext, android.R.layout.simple_spinner_dropdown_item, beansProcessLabels )
//        beansEditProcessSpinner.adapter = adapter

        // ここまでで基本的に画面構成終了
        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－
        beansEditGramButton.setOnClickListener { inputNumberDialog(getString(R.string.beansEditDialogGram), beansEditGramBar, false) }

        beansEditDateText.visibility        = View.INVISIBLE
        beansEditDateLabel.visibility       = View.INVISIBLE
        beansEditRepeatDateText.visibility  = View.INVISIBLE
        beansEditRepeatDateLabel.visibility = View.INVISIBLE
        // 初回購入日のボタン
        if( (editMode==BEANS_EDIT_MODE_NEW) or (editMode==BEANS_EDIT_MODE_COPY) or (editMode== BEANS_EDIT_MODE_EDIT) ) {
            beansEditDateText.visibility        = View.VISIBLE
            beansEditDateLabel.visibility       = View.VISIBLE

            val year    = firstDate.get(Calendar.YEAR)
            val month   = firstDate.get(Calendar.MONTH)
            val day     = firstDate.get(Calendar.DAY_OF_MONTH)

            // 日付・時刻をTextViewに事前にセット
            beansEditDateText.text = getString(R.string.dateFormat).format(year,month+1,day)

            // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
            beansEditDateText.paintFlags = beansEditDateText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            beansEditDateText.setOnClickListener {
                val dtp = DatePickerDialog(
                    this, { _, y, m, d ->
                        beansEditDateText.text = getString(R.string.dateFormat).format(y, m+1, d)
                    }, year, month, day
                )
                dtp.show()
            }
        }

        // 最新リピート購入日のボタン
        if( (editMode== BEANS_EDIT_MODE_REPEAT) or (editMode== BEANS_EDIT_MODE_EDIT) ) {
            beansEditRepeatDateText.visibility  = View.VISIBLE
            beansEditRepeatDateLabel.visibility = View.VISIBLE

            val year    = repeatDate.get(Calendar.YEAR)
            val month   = repeatDate.get(Calendar.MONTH)
            val day     = repeatDate.get(Calendar.DAY_OF_MONTH)

            // 日付・時刻をTextViewに事前にセット
            beansEditRepeatDateText.text = getString(R.string.dateFormat).format(year,month+1,day)

            // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
            beansEditRepeatDateText.paintFlags = beansEditRepeatDateText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            beansEditRepeatDateText.setOnClickListener {
                val dtp = DatePickerDialog(
                    this, { _, y, m, d ->
                        beansEditRepeatDateText.text = getString(R.string.dateFormat).format(y, m+1, d)
                    }, year, month, day
                )
                dtp.show()
            }
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
            lateinit var beansDate: Date
            lateinit var beansRepeatDate: Date

            when( editMode ) {
                BEANS_EDIT_MODE_NEW, BEANS_EDIT_MODE_COPY -> {
                    beansDate = beansEditDateText.text.toString().toDate("yyyy/MM/dd")
                    beansRepeatDate = beansDate
                }
                BEANS_EDIT_MODE_EDIT -> {
                    beansDate = beansEditDateText.text.toString().toDate("yyyy/MM/dd")
                    beansRepeatDate = beansEditRepeatDateText.text.toString().toDate("yyyy/MM/dd")
                }
                BEANS_EDIT_MODE_REPEAT -> {
                    beansRepeatDate = beansEditRepeatDateText.text.toString().toDate("yyyy/MM/dd")
                }
            }

            val beansRating = beansEditRatingBar.progress.toFloat()
            val beansName = beansEditNameEdit.text.toString()
            val beansRoast = beansEditRoastBar.progress.toFloat()
            val beansGram = beansEditGramBar.progress.toFloat()
            val beansShop = beansEditShopEdit.text.toString()
            val beansMemo = beansEditMemoEdit.text.toString()
            val beansPrice = if (beansEditPriceEdit.text.isNullOrEmpty()) 0 else beansEditPriceEdit.text.toString().toInt()
            val beansRepeat      = beansEditRepeatText.text.toString().toInt()
            val beansProcess = beansEditProcessSpinner.selectedItemPosition

            // Realmに書き込む
            when (editMode) {
                // 新規作成の場合
                // DB末尾に新規保存。１行でトランザクション完結
                BEANS_EDIT_MODE_NEW, BEANS_EDIT_MODE_COPY -> {
                    realm.executeTransaction {
                        // whereで最後尾を探し、そこに追記
                        val maxID = realm.where<BeansData>().max("id")
                        val nextID = (maxID?.toLong() ?: 0L) + 1L

                        val beans = realm.createObject<BeansData>(nextID)
                        beans.date      = beansDate
                        beans.repeatDate = beansRepeatDate
                        beans.rating    = beansRating
                        beans.name      = beansName
                        beans.roast     = beansRoast
                        beans.process   = beansProcess
                        beans.gram      = beansGram
                        beans.shop      = beansShop
                        beans.price     = beansPrice
                        beans.memo      = beansMemo
                        beans.repeat    = beansRepeat
                        if( _imageUri!=null ) beans.imageURI  = _imageUri.toString()
                    }
                    blackToast(applicationContext, "追加しましたぜ")
                }

                // 既存ＤＢの編集の場合
                // 新規レコードは作らず、既存レコードに再書き込みして終了
                BEANS_EDIT_MODE_EDIT -> {
                    realm.executeTransaction {
                        val beans = realm.where<BeansData>().equalTo("id", beansID).findFirst()
                        beans?.date     = beansDate
                        beans?.repeatDate = beansRepeatDate
                        beans?.rating   = beansRating
                        beans?.name     = beansName
                        beans?.roast    = beansRoast
                        beans?.process  = beansProcess
                        beans?.gram     = beansGram
                        beans?.shop     = beansShop
                        beans?.price    = beansPrice
                        beans?.memo     = beansMemo
                        beans?.repeat   = beansRepeat
                        if( _imageUri!=null ) beans?.imageURI = _imageUri.toString()
                    }
                    blackToast(applicationContext, "更新完了！")
                }

                // リピート購入の場合
                // 新規レコードは作らず、既存レコードに再書き込みして終了
                BEANS_EDIT_MODE_REPEAT -> {
                    realm.executeTransaction {
                        val beans = realm.where<BeansData>().equalTo("id", beansID).findFirst()
                        beans?.repeatDate = beansRepeatDate
                        beans?.rating   = beansRating
                        beans?.name     = beansName
                        beans?.roast    = beansRoast
                        beans?.process  = beansProcess
                        beans?.gram     = beansGram
                        beans?.shop     = beansShop
                        beans?.price    = beansPrice
                        beans?.memo     = beansMemo
                        beans?.repeat   = beansRepeat
                        beans?.imageURI = _imageUri.toString()
                    }
                    blackToast(applicationContext, "更新完了！")
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
        Log.d("SHIRO", "brew-edit / onDestroy")
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

            REQUEST_BEANS_PHOTO_TAKE -> {
                if( resultCode == RESULT_OK) {
                    //撮影された写真はファイル化されて、外部メモリに格納されているはず
                    //そこを指し示す、URIがグローバル変数に入っているので、そこを使う
                    blackToast( applicationContext, "写真変更" )
                    beansEditImage.setImageURI( _imageUri )
                } else {
                    blackToast( applicationContext, "キャンセル" )
                    contentResolver.delete(_imageUri as Uri, null, null)
                    _imageUri = null
                }
            }

            REQUEST_BEANS_PHOTO_SELECT -> {
                if( resultCode == RESULT_OK) {
                    blackToast(applicationContext,"写真選択")
                    _imageUri = data?.data  as Uri
                    // 選択したファイルに永続的なパーミッションを与える（結局、すべてのエラーはこれが原因・・・！？）
                    this.contentResolver.takePersistableUriPermission(_imageUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    beansEditImage.setImageURI(_imageUri)
                } else {
                    _imageUri = null
                    blackToast(applicationContext, "キャンセル")
                }
            }
        }
    }


    // 入力箇所（EditText）以外をタップしたときに、フォーカスをオフにする
    // おおもとのLayoutにfocusableInTouchModeをtrueにしないといけない
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(
            beansEditLayout.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        // 背景にフォーカスを移す
        beansEditLayout.requestFocus()
        return super.dispatchTouchEvent(event)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //カメラ起動時のrequestPermissionsの結果が、ここに帰ってくる模様
        //ダイアログを出してユーザー許可を仰ぐので、ＮＧなこともＯＫなこともある

        //WRITE_EXTERNAL_STORAGEに対するパーミションダイアログでかつ許可を選択したなら…
        if(requestCode == REQUEST_BEANS_STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // これでようやく許可が出た
            // ダイアログの許可結果はどこかに保存する必要は無いよう（次のcheckSelfPermissionではOKになる）
            // カメラ→事前確認で許可なし→許可ダイアログ→許可出た→もう一回カメラ起動
            //もう一度カメラアプリを起動。
            onBeansImageBtnClick(beansEditImageTakeBtn)
        }

        // 逆に、ダイアログでユーザーが許可しなかったら、何もしないで終了
        // カメラ→事前確認で許可なし→許可ダイアログ→許可出ない→終了
    }

    fun onBeansImageBtnClick( view: View ){
        //WRITE_EXTERNAL_STORAGEの許可が下りていないなら…
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示。その際、リクエストコードをREQUEST_STORAGE_PERMISSIONに設定。
            //自分でパーミッションを獲得するためのメソッドを呼び出す（requestPermissions)
            //ダイアログの結果は、別のResultで受け取るのでこの関数はいったん終了
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BEANS_STORAGE_PERMISSION )
            return
        }

        // 日付データから一意のファイル名を生成
        val photoName	= "CoffeeDiaryBeans"+ SimpleDateFormat("yyyyMMddHHmmss").format(Date())+".jpg"

        // Android Versionによって、外部ストレージへのアクセス方法を変える（面倒くさい・・・）
        if( Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT ) {
            // V10以降はContentResolver経由でアクセス
//            blackToast(applicationContext, "Android 10以降ですね")
            val photoContentValue = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photoName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            _imageUri = contentResolver.insert(collection, photoContentValue)!!

        } else {
            // V9以前はFileProvider経由でアクセスる
//            blackToast( applicationContext, "Android 9以前ですね")
            // 外部メモリのフォルダを決める（共有できるタイプ）
            val photoDir    = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM )
            if( photoDir.exists()==null ) {
                blackToast( applicationContext, "画像フォルダ新規作成！")
                photoDir.mkdirs()
            }
            val photoFile = File( photoDir, photoName )
            _imageUri = FileProvider.getUriForFile( applicationContext, applicationContext.packageName+".fileprovider", photoFile )
        }

        // やっと外部ストレージの準備ができたので、intentにぶち込んでカメラ起動！
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, _imageUri)
        startActivityForResult(intent, REQUEST_BEANS_PHOTO_TAKE )
    }

    fun onBeansImageBtnClick2( view: View ){
        // XMLレイアウトで直接onclick要素で指定している（引数は自動的にview）→setOnClickListenerをさぼってるだけだけど

        //WRITE_EXTERNAL_STORAGEの許可が下りていないなら…
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示。
            //自分でパーミッションを獲得するためのメソッドを呼び出す（requestPermissions)
            //ダイアログの結果は、別のResultで受け取るのでこの関数はいったん終了
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BEANS_STORAGE_PERMISSION )
            return
        }

        // ファイル名作る
        //日時データを「yyyyMMddHHmmss」の形式に整形するフォーマッタを生成。
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        val photoName	= "CoffeeDiaryBeans"+dateFormat.format(Date())+".jpg"

        // 外部メモリのフォルダを決める（共有できるタイプ）
        val photoDir    = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DCIM )
        if( photoDir.exists()==null ) {
            blackToast( applicationContext, "画像フォルダ新規作成！")
            photoDir.mkdirs()
        }

        val photoFile   = File( photoDir, photoName )
        _imageUri = FileProvider.getUriForFile(
            applicationContext,
            applicationContext.packageName+".fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, _imageUri)
        startActivityForResult(intent, REQUEST_BEANS_PHOTO_TAKE )
    }

    fun onBeansImageDeleteBtnClick( view: View ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.linkRemoveConfirmDialogTitle)
        builder.setMessage(R.string.linkRemoveConfirmDialogMessage)
        builder.setCancelable(true)
        builder.setNegativeButton(R.string.linkRemoveConfirmDialogCancelBtn, null)
        builder.setPositiveButton("OK", object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                _imageUri = Uri.parse("")
                beansEditImage.setImageResource(android.R.drawable.ic_menu_camera)
                blackToast(applicationContext, "画像登録を解除しました")
            }
        })
        builder.show()
    }

    fun onBeansImageSelectBtnClick( view: View ) {
        //WRITE_EXTERNAL_STORAGEの許可が下りていないなら…
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示。その際、リクエストコードを2000に設定。
            //自分でパーミッションを獲得するためのメソッドを呼び出す（requestPermissions)
            //ダイアログの結果は、別のResultで受け取るのでこの関数はいったん終了
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 2001)
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_BEANS_PHOTO_SELECT)
    }













    private fun inputNumberDialog(title:String, bar: IndicatorSeekBar, isFloat:Boolean ) {
        val input = EditText(this)
        input.textAlignment = View.TEXT_ALIGNMENT_CENTER
        if( isFloat ) {
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.setText( bar.progressFloat.toString() )
        } else {
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.setText( bar.progress.toString() )
        }

        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage("入力範囲："+bar.min.toInt().toString()+"～"+bar.max.toInt().toString())
            setView(input)
            setCancelable(true)
            setNegativeButton("やめる", null)
            setPositiveButton("OK",
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        var v = input.text.toString().toFloat()
                        if( v<bar.min ) v=bar.min
                        if( v>bar.max ) v=bar.max
                        bar.setProgress( v )
                    }
                })
            show()
        }
    }
}
// class
