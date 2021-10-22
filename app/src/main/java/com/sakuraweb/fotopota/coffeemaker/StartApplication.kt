package com.sakuraweb.fotopota.coffeemaker

//TODO:結局、器具DBっていつも初期化されちゃわない！？
//TODO:お湯の量（mlで）
//TODO:抽出時間
//TODO:増えすぎた表示項目のON・OFF
//TODO:バックアップ
//TODO:写真
//TODO:器具DB内での並び順（外のみを先頭に）
//TODO:器具DB内での外飲みの色
//TODO:器具DB内での外のみの削除禁止（ほかの方法ないもんかね）
//TODO:EQUIPを新造したときに、必ず外飲みだけは追加

import android.app.Application
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.sakuraweb.fotopota.coffeemaker.ui.beans.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.*
import com.sakuraweb.fotopota.coffeemaker.ui.equip.*
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.*
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.text.SimpleDateFormat
import java.util.*

// 各種定数
const val BREW_IN_HOME = 1
const val BREW_IN_SHOP = 2
const val BREW_IN_BOTH = 3
const val EQUIP_SHOP = 10L

const val GRIND_SW_NAME = 0
const val GRIND_SW_ROTATION = 1
const val GRIND_UNIT_INT = 0
const val GRIND_UNIT_FLOAT = 1

const val HOT_COFFEE = 0
const val ICE_COFFEE = 1

const val CARD_STYLE = 0
const val FLAT_STYLE = 1

// グローバル変数たち
lateinit var brewRealmConfig: RealmConfiguration
lateinit var beansRealmConfig: RealmConfiguration
lateinit var takeoutRealmConfig: RealmConfiguration
lateinit var equipRealmConfig: RealmConfiguration

lateinit var brewMethods: Array<String>
lateinit var brewMethodsCR: Array<String>
lateinit var brewMethodsImages: TypedArray

lateinit var beansKind: Array<String>   // 以下３種のタイトル配列
lateinit var beansSpecial: Array<String>
lateinit var beansBlend: Array<String>
lateinit var beansPack: Array<String>
lateinit var beansProcessLabels: Array<String>

lateinit var takeoutKind: Array<String> // 以下３種のタイトル配列
lateinit var takeoutConvini: Array<String>
lateinit var takeoutCafe: Array<String>
lateinit var takeoutRestaurant: Array<String>

lateinit var roastLabels: Array<String>
lateinit var grindLabels: Array<String>
lateinit var sugarLabels: Array<String>
lateinit var milkLabels:  Array<String>

const val brew_list_backup      = "brew_list_backup.realm"
const val bean_list_backup      = "bean_list_backup.realm"
const val takeout_list_backup   = "takeout_list_backup.realm"
const val equip_list_backup     = "equip_list_backup.realm"

// 一番最初に実行されるApplicationクラス
// いつもの、AppCompatActivity（MainActivity）は、manifest.xmlで最初の画面（Acitivity）として実行される
// Application（CustomApplication）も、manifest.xmlで最初のクラスとして実行される
// で、その実行順位が、Application ＞ AppCompatActivityとなっているので、こっちの方が先
// 今回は、データベース作成のために最初にここで起動させる

class StartApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // そのほか、グローバル変数セット
        brewMethodsImages = resources.obtainTypedArray(R.array.method_images)

        // brewMethodsは、改行アリ（Recycler用）、改行無し（Edit用）の２つを作る
        brewMethodsCR = resources.getStringArray(R.array.method_names)
        // 含まれる\nを削除
        brewMethods=arrayOf<String>()
        for( b in brewMethodsCR ) brewMethods += b.replace("\n","")

        // Realm全体の初期化処理
        Realm.init(applicationContext)

        // ブリューデータを作る
        createBrewData()

        // 豆データを作る
        createBeansData()

        // テイクアウトデータを作る
        createTakeoutData()

        // コーヒー器具データを作る
        // 新規の場合はとりあえず標準的な器具をリスト
        createEquipData()


        // EQUIPが初登場したバージョン（BREW_DATA_VERSION=6）の時の処理
        // BREWのmethodIDをやめて、equipIDでEQUIPD DB参照とする
        if( brewDataMigrated5to6  ) {
            // 既存BREWで使っている器具のリストを作る
            var realm = Realm.getInstance(brewRealmConfig)
            val brews = realm.where<BrewData>().findAll()
            var methods = Array(11) { 0 }
            for (b in brews ) {
                methods[b.methodID] += 1
                realm.executeTransaction {
                    val bb = realm.where<BrewData>().equalTo("id", b.id ).findFirst()
                    if( bb!=null ) bb.equipID = bb.methodID.toLong()
                }
            }
            realm.close()
            // 普通は無いと思うけど、外飲みゼロの場合の対策
            methods[10] += 1

            // できたばかりのEQUIPを全部消して実績だけ入れる
            realm = Realm.getInstance(equipRealmConfig)
            val equips = realm.where<EquipData>().findAll()
            // まず全部消す・・・
            for( e in equips ) {
                realm.executeTransaction {
                    realm.where<EquipData>().equalTo("id", e.id)?.findFirst()?.deleteFromRealm()
                }
            }
            // 既存BREWの使用実績を入れる
            for( i in 0..10) {
                if( methods[i] > 0 ) {
                    realm.executeTransaction {
                        val equip = realm.createObject<EquipData>( i )
//TODO: ここで、icon番号を変換することも可能
                        equip.icon = if( i==0 ) 1 else i
                        equip.name = brewMethods[i] // CRはどうすんだっけ・・・？
                        equip.date = (if(i.toLong()==EQUIP_SHOP) "2050/12/31" else "2020/01/01").toDate("yyyy/MM/dd")
                       // 外飲みをいつも先頭に並べるために小細工
//                        equip.recent = (if(i.toLong()==EQUIP_SHOP) "2050/12/31" else "2020/01/01").toDate("yyyy/MM/dd")
                    }
                }
            }
            realm.close()
            blackToast(applicationContext,"器具DB構築完了！")
        }

        // v7→v8で湯量（waterVolume）が追加された
        // 全部ゼロになるのはアカンので、とりあえず、カップ数×120ccにしておこう
        if( brewDataMigrated7to8 ) {
            var realm = Realm.getInstance(brewRealmConfig)
            val brews = realm.where<BrewData>().findAll()
            for( b in brews ) {
                realm.executeTransaction {
                    b.waterVolume = b.cupsDrunk * 120
                }
            }

            realm.close()
            blackToast(applicationContext, "湯量項目追加完了！")
        }

        // BeansDataのマイグレーション処理
        // v1→v2
        // repeat（購入回数）を1にセットするだけ
        if( beansDataMigrated1to2 ) {
            var realm = Realm.getInstance(beansRealmConfig)
            var beans = realm.where<BeansData>().findAll()
            for( b in beans ) {
                realm.executeTransaction {
                    b.repeat = 1
                }
            }
        }
        // v2→v3
        // repeatDateを初期化
        if( beansDataMigrated2to3 ) {
            var realm = Realm.getInstance(beansRealmConfig)
            var beans = realm.where<BeansData>().findAll()
            for( b in beans ) {
                realm.executeTransaction {
                    b.repeatDate = b.date
                }
            }
        }

        // 代表豆銘柄セレクト関係
        beansKind = resources.getStringArray(R.array.beans_kind)
        beansSpecial = resources.getStringArray(R.array.beans_special)
        beansBlend = resources.getStringArray(R.array.beans_blend)
        beansPack = resources.getStringArray(R.array.beans_pack)

        //店飲みセレクト関係
        takeoutKind = resources.getStringArray(R.array.takeout_kind)
        takeoutConvini = resources.getStringArray(R.array.takeout_convini)
        takeoutCafe = resources.getStringArray(R.array.takeout_cafe)
        takeoutRestaurant = resources.getStringArray(R.array.takeout_restaurant)

        roastLabels = resources.getStringArray(R.array.roast_labels)
        grindLabels = resources.getStringArray(R.array.grind_labels)
        milkLabels  = resources.getStringArray(R.array.milk_volume)
        sugarLabels = resources.getStringArray(R.array.sugar_volume)
        beansProcessLabels = resources.getStringArray(R.array.process_labels)
//        setTakeoutTakeDay()

    }


    private fun createEquipData() {
        // configを設定
        equipRealmConfig = RealmConfiguration.Builder()
            .name("equip.realm")
            .modules(EquipDataModule())
            .schemaVersion(EQUIP_DATA_VERSION)
            .migration(EquipDataMigration())
            .build()

        // インスタンス化
        val realm = Realm.getInstance(equipRealmConfig)
        val equips: RealmResults<EquipData> = realm.where(
            EquipData::class.java
        ).findAll().sort("id", Sort.DESCENDING)

        // データ数ゼロならサンプルを作る
        if (equips.size == 0) {
            val equipList = listOf<EquipDataInit>(
                EquipDataInit(0L, "2021/1/1", "ドリッパー", "KALITA", "ABC-012",3.0F, 1, "", 500, ""),
                EquipDataInit(1L, "2021/1/1", "フレンチプレス", "BODUM", "XXX-123", 3.0F, 2,  "",2000, ""),
                EquipDataInit( EQUIP_SHOP, "2050/1/1", "市販コーヒー", "", "", 3.0F, 10, "", 0, "")
            )

            // DB書き込み
            realm.beginTransaction()
            for (i in equipList.reversed()) {
                var b = realm.createObject<EquipData>(i.id)
                b.date = i.date.toDate("yyyy/MM/dd")
                b.name = i.name
                b.maker = i.maker
                b.rating = i.rating
                b.icon = i.icon
                b.shop = i.shop
                b.price = i.price
                b.memo = i.memo
            }
            realm.commitTransaction()
            blackToast(this, "EQUIPデータベース完了！")
        }
        realm.close()
    }


    private fun createBeansData() {
        // configを設定
        beansRealmConfig = RealmConfiguration.Builder()
            .name("beans.realm")
            .modules(BeansDataModule())
            .schemaVersion(BEANS_DATA_VERSION)
            .migration(BeansDataMigration())
            .build()

//        beansRealmConfig = RealmConfiguration.Builder()
//            .name("beans.realm")
//            .modules(BeansDataModule())
//            .deleteRealmIfMigrationNeeded()
//            .build()

        // インスタンス化
        val realm = Realm.getInstance(beansRealmConfig)
        val beans: RealmResults<BeansData> = realm.where(
            BeansData::class.java
        ).findAll().sort("id", Sort.DESCENDING)

        // データ数ゼロならサンプルを作る
        if (beans.size == 0) {
            val beansList = listOf<BeansDataInit>(
                BeansDataInit("キリマンジャロ", 3F, "2020/1/1", 200F, 1F, "KALDI", 399, 1,"サンプルデータです。使用時には削除してください。")
            )

            // DB書き込み
            realm.beginTransaction()
            var id = 1
            for (i in beansList.reversed()) {
                var b = realm.createObject<BeansData>(id++)
                b.date = i.date.toDate("yyyy/MM/dd")
                b.repeatDate = i.date.toDate("yyyy/MM/dd")
                b.name = i.name
                b.gram = i.gram
                b.roast = i.roast
                b.shop = i.shop
                b.price = i.price
                b.memo = i.memo
            }
            realm.commitTransaction()
            blackToast(this, "BEANSデータベース完了！")
        }
        realm.close()
    }


    private fun createBrewData() {
        brewRealmConfig = RealmConfiguration.Builder()
            .name("brews.realm")
            .modules(BrewDataModule())
            .schemaVersion(BREW_DATA_VERSION)
            .migration(BrewDataMigration())
            .build()

//        brewRealmConfig = RealmConfiguration.Builder()
//            .name("brews.realm")
//            .modules(BrewDataModule())
//            .deleteRealmIfMigrationNeeded()
//            .build()

        val realm = Realm.getInstance(brewRealmConfig)
        val brews: RealmResults<BrewData> = realm.where(
            BrewData::class.java
        ).findAll().sort("id", Sort.DESCENDING)


        // データゼロなら作る
        if (brews.size == 0) {
            val brewList = listOf<BrewDataInit>(
                BrewDataInit("2020/01/2 12:34", 2F, 1, 1,1, 1, 3F, 10F, 2F, 1F,90F, 30F, "http", "サンプルデータです。使用時には削除してください。", 0)
            )
            // DBに書き込む
            realm.beginTransaction()
            var id = 1
            for (i in brewList.reversed()) {
                val c = realm.createObject<BrewData>(id++)
                c.date          = i.date.toDate()
                c.place         = i.place
                c.rating        = i.rating
                c.methodID      = i.methodID
                c.beansID       = i.beansID
                c.beansPast     = i.beansPast
                c.beansGrind    = i.beansGrind
                c.beansUse  = i.beansUse
                c.cups      = i.cups
                c.cupsDrunk = i.cupsDrunk
                c.temp      = i.temp
                c.steam     = i.steam
                c.imageURI  = i.imageURI
                c.memo      = i.memo
            }
            realm.commitTransaction()
            blackToast(this, "BREWデータベース完了！")
        }

        realm.close()
    }


    private fun createTakeoutData() {
        // configを設定
        takeoutRealmConfig = RealmConfiguration.Builder()
            .name("takeout.realm")
            .modules(TakeoutDataModule())
            .schemaVersion(TAKEOUT_DATA_VERSION)
            .migration(TakeoutDataMigration())
            .build()

//                beansRealmConfig = RealmConfiguration.Builder()
//                    .name("takeout.realm")
//                    .modules(TakeoutDataModule())
//                    .deleteRealmIfMigrationNeeded()
//                    .build()

        // インスタンス化
        val realm = Realm.getInstance(takeoutRealmConfig)
        val takeouts: RealmResults<TakeoutData> = realm.where(
            TakeoutData::class.java
        ).findAll().sort("id", Sort.DESCENDING)

        // データ数ゼロならサンプルを作る
        if (takeouts.size == 0) {
            val takeoutList = listOf<TakeoutDataInit>(
                TakeoutDataInit("ホットブレンドＳ", 3F, "ファミリーマート", "ＮＹ支店",300, 100, "Regular", "サンプルデータです。使用時には削除してください。")
            )

            // DB書き込み
            realm.beginTransaction()
            var id = 1
            for (i in takeoutList.reversed()) {
                var b = realm.createObject<TakeoutData>(id++)
                b.name = i.name
                b.rating = i.rating
                b.chain = i.chain
                b.shop = i.shop
                b.price = i.price
                b.size = i.size
                b.memo = i.memo
            }
            realm.commitTransaction()

            blackToast(this, "TAKEOUTデータベース完了！")
        }
        realm.close()
    }
}

// 黒いToast画面を出すだけ
public fun blackToast(c: Context, s: String) {
    val toast = Toast.makeText(c, s, Toast.LENGTH_SHORT)
    val view: View = toast.view

//    view.background.setColorFilter(Color.rgb(0,0,0), PorterDuff.Mode.SRC_IN)
    view.background.colorFilter = PorterDuffColorFilter(Color.rgb(0,0,0), PorterDuff.Mode.SRC_IN)
    view.findViewById<TextView>(android.R.id.message).setTextColor(Color.rgb(255,255,255))

    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
    toast.show()
}

//fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm"): Date {
public fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm"): Date {
    val df = SimpleDateFormat(pattern)
    val dt = df.parse(this)
    return dt

}

fun Date.toString(pattern: String = "yyyy/MM/dd"): String {
    val df = SimpleDateFormat(pattern)
    val ds = df.format(this)
    return  ds
}

// 最初のBREWの日付をゲット
// なんでここに？ という気もしないではないけど・・・
fun getFirstBrewDate() : Date {
    var begin: Date

    val realm = Realm.getInstance(brewRealmConfig)
    val brews = realm.where<BrewData>().findAll().sort("date",Sort.ASCENDING)

    if( brews.size>0 ) {
        begin = brews[0]?.date as Date
    } else {
        begin = Date()
    }
    realm.close()

    return begin
}

