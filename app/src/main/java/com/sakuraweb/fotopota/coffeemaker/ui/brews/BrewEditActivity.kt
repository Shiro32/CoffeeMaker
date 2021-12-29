package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
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
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansListActivity
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import com.sakuraweb.fotopota.coffeemaker.ui.equip.EquipListActivity
import com.sakuraweb.fotopota.coffeemaker.ui.equip.findEquipNameByID
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutListActivity
import com.warkiz.widget.IndicatorSeekBar
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_brew_edit.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

// BrewEditの動作モード（新規、編集、コピー新規、FABから）
const val BREW_EDIT_MODE_NEW = 1
const val BREW_EDIT_MODE_EDIT = 2
const val BREW_EDIT_MODE_COPY = 3

const val REQUEST_CODE_BEANS_SELECT = 1
const val REQUEST_CODE_TAKEOUT_SELECT = 2
const val REQUEST_CODE_EQUIP_SELECT = 3
const val REQUEST_PHOTO_TAKE        = 444
const val REQUEST_PHOTO_SELECT      = 5555
const val REQUEST_BREW_STORAGE_PERMISSION = 6777

// TODO: ほんの少しでも編集したら「戻る」も要確認　どうやって検出するの？
// TODO: 写真を選択しなかった場合はどうなる？（デフォルトの写真？）
// TODO: 任意の画像も選びたい（COFFEEMAKERフォルダ内のでいいので）
// TODO: 画像が見つからない（削除された）時に落ちない？

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
    private var equipID: Long = 0L
    private var editMode: Int = 0
    private var _imageUri: Uri = Uri.parse("")
    private lateinit var realm: Realm
    private lateinit var inputMethodManager: InputMethodManager

    // 編集画面開始
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brew_edit)

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // 設定メニューで表示・非表示を切り替える
        // BrewEditは必ずBrewDetailsから呼ばれ、すでにConfigは読み込み済みのはず

        if( !configSteamTimeSw ) {  // 蒸らし時間
            brewEditSteamBar.visibility = View.GONE
            brewEditSteamLabel.visibility = View.GONE
        } else {
            brewEditSteamBar.max = configSteamTimeMax
            brewEditSteamBar.min = configSteamTimeMin
        }

        if( !configBrewTimeSw ) {   // 抽出時間
            brewEditBrewTimeBar.visibility = View.GONE
            brewEditBrewTimeLabel.visibility = View.GONE
        } else {
            brewEditBrewTimeBar.max = configBrewTimeMax
            brewEditBrewTimeBar.min = configBrewTimeMin
        }

        if( !configWaterVolumeSw ) {   // 抽出cc
            brewEditWaterVolumeBar.visibility = View.GONE
            brewEditWaterVolumeLabel.visibility = View.GONE
        } else {
            brewEditWaterVolumeBar.max = configWaterVolumeMax
            brewEditWaterVolumeBar.min = configWaterVolumeMin
        }

        if( !configTempSw ) { // 温度
            brewEditTempBar.visibility = View.GONE
            brewEditTempLabel.visibility = View.GONE
        } else {
            brewEditTempBar.max = configTempMax
            brewEditTempBar.min = configTempMin
        }

        if( !configCupsBrewedSw ) {   // 抽出カップ数
            brewEditCupsBar.visibility = View.GONE
            brewEditCupLabel.visibility = View.GONE
        }

        if( !configCupsDrunkSw ) {   // 飲んだカップ数
            brewEditCupsDrunkBar.visibility = View.GONE
            brewEditCupDrunkLabel.visibility = View.GONE
        }

        brewEditGrind2Bar.max = configMillMax   // 先にMAXをセットしておかないと、これを超える数字をセットできない！
        brewEditGrind2Bar.min = configMillMin
        brewEditGrind2Bar.setDecimalScale(if( configMillUnit== GRIND_UNIT_FLOAT ) 1 else 0)

        if( !configMilkSw ) {
            brewEditMilkBar.visibility = View.GONE
            brewEditMilkLabel.visibility = View.GONE
        }
        if( !configSugarSw ) {
            brewEditSugarBar.visibility = View.GONE
            brewEditSugarLabel.visibility = View.GONE
        }

        // ツールバータイトル用（４モード対応）
        val titles:Map<Int,Int> = mapOf(
            BREW_EDIT_MODE_NEW to R.string.titleBrewEditNew,
            BREW_EDIT_MODE_EDIT to R.string.titleBrewEditEdit,
            BREW_EDIT_MODE_COPY to R.string.titleBrewEditCopy
        )

        brewEditWaterVolumeLabel.text = getString(R.string.brewEditWaterVolumeLabel).format(configWaterVolumeUnit)


        // Realmのインスタンスを生成
        // Edit画面終了まで維持（onDestroy）でclose
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
                brewEditCancelBtn.text = getString(R.string.returnBtnLabel)
            }

            BREW_EDIT_MODE_EDIT, BREW_EDIT_MODE_COPY -> {
                val brew = realm.where<BrewData>().equalTo("id", brewID).findFirst()
                if (brew != null) {
                    brewEditRatingBar.rating = brew.rating
                    brewEditMethodText2.text = findEquipNameByID(brew.equipID)

                    brewEditBeansText.text =
                        findBeansNameByID(brew.place, brew.beansID, brew.takeoutID)
                    // 豆データなどはViewに保存できないのでローカル変数に
                    beansID = brew.beansID
                    takeoutID = brew.takeoutID
                    equipID = brew.equipID

                    brewEditCupsBar.setProgress(brew.cups)
                    brewEditCupsDrunkBar.setProgress(brew.cupsDrunk)

                    // Grindを数字入力できるようにする処理（アドホックだなぁ・・・）
                    // どちらのbarを表示するかは、別途用意するSWで決める
                    brewEditGrindSw.isChecked = (brew.beansGrindSw == GRIND_SW_ROTATION)
                    brewEditGrind1Bar.setProgress(brew.beansGrind)
                    brewEditGrind2Bar.setProgress(brew.beansGrind2)
                    brewEditWaterVolumeBar.setProgress(brew.waterVolume)
                    brewEditBeansUseBar.setProgress(brew.beansUse)
                    brewEditTempBar.setProgress(brew.temp)
                    brewEditSteamBar.setProgress(brew.steam)
                    brewEditBrewTimeBar.setProgress(brew.brewTime)
                    brewEditShopText.setText(brew.shop)
                    brewEditSugarBar.setProgress(brew.sugar)
                    brewEditMilkBar.setProgress(brew.milk)
                    brewEditHotIceSW.isChecked = brew.iceHotSw != HOT_COFFEE

                    if( brew.imageURI!="" ) {
                        try {
                            _imageUri = Uri.parse(brew.imageURI)
                            brewEditBrewImage.setImageURI( _imageUri )
                        } catch( e:Exception ) {
                            brewEditBrewImage.setImageResource(android.R.drawable.ic_menu_report_image)
                        }
                    }

                    if (editMode == BREW_EDIT_MODE_EDIT) {
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

        brewEditSugarBar.setIndicatorTextFormat("\${TICK_TEXT}")
        brewEditMilkBar.setIndicatorTextFormat("\${TICK_TEXT}")

        brewEditGrindSw.setOnCheckedChangeListener { _, isChecked ->
            if( isChecked ) {
                brewEditGrind1Bar.visibility = View.GONE
                brewEditGrind2Bar.setIndicatorTextFormat("\${PROGRESS}")
                brewEditGrind2Bar.visibility = View.VISIBLE
                // バーを使わず手入力する場合
                brewEditGrindLabel.visibility = View.VISIBLE
                brewEditGrindLabel2.visibility = View.GONE
                brewEditGrindLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogGrind2), brewEditGrind2Bar, configMillUnit== GRIND_UNIT_FLOAT ) }
            } else {
                brewEditGrind1Bar.setIndicatorTextFormat("\${TICK_TEXT}")
                brewEditGrind1Bar.visibility = View.VISIBLE
                brewEditGrind2Bar.visibility = View.GONE
                // 直接入力は不要
                brewEditGrindLabel.visibility = View.GONE
                brewEditGrindLabel2.visibility = View.VISIBLE
            }
        }

        // 同じことを書くのよねぇ・・・。
        if( brewEditGrindSw.isChecked ) {
            // 回転数表示
            brewEditGrind2Bar.setIndicatorTextFormat("\${PROGRESS}")
            brewEditGrind1Bar.visibility = View.GONE
            brewEditGrind2Bar.visibility = View.VISIBLE
            // バーを使わず手入力する場合
            brewEditGrindLabel.visibility = View.VISIBLE
            brewEditGrindLabel2.visibility = View.GONE
            brewEditGrindLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogGrind2), brewEditGrind2Bar, configMillUnit== GRIND_UNIT_FLOAT ) }
        } else {
            // 名前表示
            brewEditGrind1Bar.setIndicatorTextFormat("\${TICK_TEXT}")
            brewEditGrind1Bar.visibility = View.VISIBLE
            brewEditGrind2Bar.visibility = View.GONE
            // 直接入力は不要
            brewEditGrindLabel.visibility = View.GONE
            brewEditGrindLabel2.visibility = View.VISIBLE
        }


        // ここまでで基本的に画面構成終了
        // ーーーーーーーーーー　ここから各種ボタンのリスナ群設定　－－－－－－－－－－

        // メソッド選択ボタンリスナ
        brewEditMethodText2.setOnClickListener {
            val intent = Intent( this, EquipListActivity::class.java )
            intent.putExtra( "from", "Edit" )
            startActivityForResult( intent, REQUEST_CODE_EQUIP_SELECT )
        }

        // マメ選択ボタンリスナ
        // 抽出方法が店飲みの場合は、店飲みDBを選択する
        brewEditBeansText.setOnClickListener {
            // 店飲みDBを呼び出す
            if( equipID == EQUIP_SHOP ) {
                val intent = Intent(this, TakeoutListActivity::class.java)
                intent.putExtra("from", "Edit")
                startActivityForResult(intent, REQUEST_CODE_TAKEOUT_SELECT) // これで見分ける
            } else {
                val intent = Intent(this, BeansListActivity::class.java)
                intent.putExtra("from", "Edit")
                startActivityForResult(intent, REQUEST_CODE_BEANS_SELECT)
            }
        }

        updateEquip( equipID )
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
                this, DatePickerDialog.OnDateSetListener { _, y, m, d ->
                    brewEditDateText.text = getString(R.string.dateFormat).format(y, m+1, d)
                }, year, month, day
            )
            dtp.show()
        }

        // 日付ボタンの、リスナ登録・アンダーライン設定（ほかにやり方ないのかね・・・？）
        brewEditTimeText.paintFlags = brewEditTimeText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        brewEditTimeText.setOnClickListener {
            val ttp = TimePickerDialog(
                this, TimePickerDialog.OnTimeSetListener { _, h, m ->
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

        // すべてのスライドバーに手入力オプションを付ける
        // GrindBarだけはSWによって処理を分けるので、前段のリスナ等で設定
        brewEditWaterVolumeLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogWaterVolume).format(configWaterVolumeUnit), brewEditWaterVolumeBar, false ) }
        brewEditCupLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogCups), brewEditCupsBar, false) }
        brewEditCupDrunkLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogCupsDrunk), brewEditCupsDrunkBar, false) }
        brewEditBeansUseLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogBeansUse), brewEditBeansUseBar, false ) }
        brewEditTempLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogTemp), brewEditTempBar, false ) }
        brewEditSteamLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogSteam), brewEditSteamBar, false ) }
        brewEditBrewTimeLabel.setOnClickListener { inputNumberDialog(getString(R.string.brewEditDialogBrewTime), brewEditBrewTimeBar, false ) }

        // ーーーーーーーーーー　ツールバー関係　ーーーーーーーーーー
        setSupportActionBar(brewEditToolbar) // これやらないと落ちるよ
        supportActionBar?.title = getString(titles[editMode] as Int)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Log.d("SHIRO", "brew-edit / onCreate")
    } // 編集画面のonCreate


    private fun inputNumberDialog(title:String, bar:IndicatorSeekBar, isFloat:Boolean ) {
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



    // 外飲み（equipID==EQUIP_SHOP）か家のみかで、表示項目を変更する
    private fun updateEquip(id: Long ) {
        if ( id == EQUIP_SHOP) {
            brewEditHomeItems.visibility = View.GONE
            brewEditTakeoutItems.visibility = View.VISIBLE
        } else {
            brewEditHomeItems.visibility = View.VISIBLE
            brewEditTakeoutItems.visibility = View.GONE
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
            val brewCups    = brewEditCupsBar.progressFloat
            val brewCupsDrunk= brewEditCupsDrunkBar.progressFloat
            val brewWaterVolume     = brewEditWaterVolumeBar.progressFloat
            val brewGrindSw = if( brewEditGrindSw.isChecked ) GRIND_SW_ROTATION else GRIND_SW_NAME
            val brewGrind1   = brewEditGrind1Bar.progressFloat
            val brewGrind2 = brewEditGrind2Bar.progressFloat
            val brewBeansUse= brewEditBeansUseBar.progressFloat
            val brewTemp    = brewEditTempBar.progressFloat
            val brewSteam   = brewEditSteamBar.progressFloat
            val brewTime    = brewEditBrewTimeBar.progressFloat
            val brewShop   = brewEditShopText.text.toString()
            val brewMemo   = brewEditMemoText.text.toString()

            val brewSugar   = brewEditSugarBar.progressFloat
            val brewMilk    = brewEditMilkBar.progressFloat
            val brewIceHotSW = if( brewEditHotIceSW.isChecked ) ICE_COFFEE else HOT_COFFEE

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
                        brew.takeoutID = takeoutID
                        brew.equipID = equipID
                        brew.place = if( equipID==EQUIP_SHOP ) BREW_IN_SHOP else BREW_IN_HOME
                        brew.cups = brewCups
                        brew.cupsDrunk = brewCupsDrunk
                        brew.waterVolume = brewWaterVolume
                        brew.beansGrindSw = brewGrindSw
                        brew.beansGrind = brewGrind1
                        brew.beansGrind2 = brewGrind2
                        brew.beansUse = brewBeansUse
                        brew.temp = brewTemp
                        brew.steam = brewSteam
                        brew.brewTime = brewTime
                        brew.shop = brewShop
                        brew.memo = brewMemo
                        brew.sugar = brewSugar
                        brew.milk = brewMilk
                        brew.iceHotSw = brewIceHotSW

//TODO: 写真を選んでないときの処理は必要では？
                        brew.imageURI  = _imageUri.toString()

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
                        brew?.equipID   = equipID
                        brew?.place     = if( equipID==EQUIP_SHOP ) BREW_IN_SHOP else BREW_IN_HOME
                        brew?.cups      = brewCups
                        brew?.cupsDrunk = brewCupsDrunk
                        brew?.waterVolume = brewWaterVolume
                        brew?.beansGrindSw = brewGrindSw
                        brew?.beansGrind= brewGrind1
                        brew?.beansGrind2=brewGrind2
                        brew?.beansUse  = brewBeansUse
                        brew?.temp      = brewTemp
                        brew?.steam     = brewSteam
                        brew?.brewTime  = brewTime
                        brew?.shop      = brewShop
                        brew?.memo      = brewMemo
                        brew?.sugar     = brewSugar
                        brew?.milk      = brewMilk
                        brew?.iceHotSw  = brewIceHotSW

//TODO: 写真を選んでないときの処理は必要では？
                        brew?.imageURI  = _imageUri.toString()
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
                        val id = data?.getLongExtra("id", 0L) as Long
                        val name = data.getStringExtra("name")
                        brewEditBeansText.text = name
                        beansID = id
                        blackToast(applicationContext, "${name}を選択")
                    }
                    RESULT_TO_HOME -> {
                        val intent = Intent()
                        setResult(RESULT_TO_HOME, intent)
                        finish()
                    }
                }
            }

            // 器具選択
            REQUEST_CODE_EQUIP_SELECT -> {
                when( resultCode ) {
                    RESULT_OK -> {
                        val id = data?.getLongExtra("id", 0L) as Long
                        val name = data?.getStringExtra("name")

                        brewEditMethodText2.text = name
                        // 選択によって、家飲み⇔外飲みが変わってしまった場合、豆をクリアする
                        if( equipID != id && (equipID==EQUIP_SHOP || id==EQUIP_SHOP) ) {
                            brewEditBeansText.text = "未選択"
                            equipID = 0
                        }
                        equipID = id
                        updateEquip( equipID )
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

            REQUEST_PHOTO_TAKE -> {

                if( resultCode == RESULT_OK) {
                    //撮影された写真はファイル化されて、外部メモリに格納されているはず
                    //そこを指し示す、URIがグローバル変数に入っているので、そこを使う
                    //なんとなくだけど、intentの中に入っていてgetすべきじゃないかと思うのだけど違うみたい
                    brewEditBrewImage.setImageURI(_imageUri)
//                    blackToast(applicationContext, "do Nothing!!!!!!!!!!!!")
                } else {
                    contentResolver.delete(_imageUri, null, null)
                }
            }

            REQUEST_PHOTO_SELECT -> {
                if( resultCode == RESULT_OK) {
                    brewEditBrewImage.setImageURI(data?.data)
                    _imageUri = data?.data  as Uri
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

    // ここから先はカメラ処理

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        //カメラ起動時のrequestPermissionsの結果が、ここに帰ってくる模様
        //ダイアログを出してユーザー許可を仰ぐので、ＮＧなこともＯＫなこともある

        //WRITE_EXTERNAL_STORAGEに対するパーミションダイアログでかつ許可を選択したなら…
        if(requestCode == REQUEST_BREW_STORAGE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // これでようやく許可が出た
            // ダイアログの許可結果はどこかに保存する必要は無いよう（次のcheckSelfPermissionではOKになる）
            // カメラ→事前確認で許可なし→許可ダイアログ→許可出た→もう一回カメラ起動
            //もう一度カメラアプリを起動。
            onBrewImageBtnClick(brewEditBrewImageBtn)
        }

        // 逆に、ダイアログでユーザーが許可しなかったら、何もしないで終了
        // カメラ→事前確認で許可なし→許可ダイアログ→許可出ない→終了
    }

    fun onBrewImageDeleteBtnClick( view: View ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.linkRemoveConfirmDialogTitle)
        builder.setMessage(R.string.linkRemoveConfirmDialogMessage)
        builder.setCancelable(true)
        builder.setNegativeButton(R.string.linkRemoveConfirmDialogCancelBtn, null)
        builder.setPositiveButton("OK", object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                _imageUri = Uri.parse("")
                brewEditBrewImage.setImageResource(android.R.drawable.ic_menu_camera)
                blackToast(applicationContext, "画像登録を解除しました")
            }
        })
        builder.show()
    }

    fun onBrewImageSelectBtnClick( view: View ) {
        //WRITE_EXTERNAL_STORAGEの許可が下りていないなら…
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示。その際、リクエストコードをREQUEST_STORAGE_PERMISSIONに設定。
            //自分でパーミッションを獲得するためのメソッドを呼び出す（requestPermissions)
            //ダイアログの結果は、別のResultで受け取るのでこの関数はいったん終了
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BREW_STORAGE_PERMISSION )
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_PHOTO_SELECT)
    }

    fun onBrewImageBtnClick( view: View ){
        // XMLレイアウトで直接onclick要素で指定している（引数は自動的にview）→setonclicklistnerをさぼってるだけだけど
        // パーミッション→ファイル名→ContentValues→ContentResolver→Intent→起動

        //WRITE_EXTERNAL_STORAGEの許可が下りていないなら…
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示。その際、リクエストコードをREQUEST_STORAGE_PERMISSIONに設定。
            //自分でパーミッションを獲得するためのメソッドを呼び出す（requestPermissions)
            //ダイアログの結果は、別のResultで受け取るのでこの関数はいったん終了
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BREW_STORAGE_PERMISSION )
            return
        }

        // ファイル名を作る作るだけ
        val photoName	= "COFFEE"+SimpleDateFormat("yyyyMMddHHmmss").format(Date())+".jpg"
        val photoContentValue = ContentValues()
        photoContentValue.put(MediaStore.Images.Media.TITLE, photoName)
        photoContentValue.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

//        val mediaLocation = MediaStore.Images.Media.getContentUri("external")
//        val photoUri = contentResolver.insert(mediaLocation, photoContentValue)
        val photoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoContentValue)

        if( photoUri!=null ) {
            _imageUri = photoUri
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, REQUEST_PHOTO_TAKE)
        }
    }

    fun onBrewImageBtnClick2(view: View) {
        // XMLレイアウトで直接onclick要素で指定している（引数は自動的にview）→setonclicklistnerをさぼってるだけだけど
        // パーミッション→ファイル名→ContentValues→ContentResolver→Intent→起動

        //WRITE_EXTERNAL_STORAGEの許可が下りていないなら…
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            //WRITE_EXTERNAL_STORAGEの許可を求めるダイアログを表示。その際、リクエストコードを2000に設定。
            //自分でパーミッションを獲得するためのメソッドを呼び出す（requestPermissions)
            //ダイアログの結果は、別のResultで受け取るのでこの関数はいったん終了
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_BREW_STORAGE_PERMISSION)
            return
        }

        // ファイル名作るだけ
        //日時データを「yyyyMMddHHmmss」の形式に整形するフォーマッタを生成。
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        val photoName	= "CoffeeDiary"+dateFormat.format(Date())+".jpg"
        val photoDir 	= getExternalFilesDir(Environment.DIRECTORY_DCIM)
        val photoFile	= File( photoDir, photoName )

        _imageUri = FileProvider.getUriForFile(
            applicationContext,
            applicationContext.packageName+".fileprovider",
            photoFile
        )

        //Intentオブジェクトを生成。（imageUriを設定する）
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, _imageUri)

        //アクティビティを起動。
        startActivityForResult(intent, REQUEST_PHOTO_TAKE)
    }

} // Class


