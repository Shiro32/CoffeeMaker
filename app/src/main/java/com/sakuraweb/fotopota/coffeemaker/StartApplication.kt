package com.sakuraweb.fotopota.coffeemaker

import android.app.Application
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.provider.Settings.Global.getString
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.sakuraweb.fotopota.coffeemaker.ui.beans.*
import com.sakuraweb.fotopota.coffeemaker.ui.brews.*
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.*
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.createObject
import java.text.SimpleDateFormat
import java.util.*

// 各種定数
const val BREW_IN_HOME = 1
const val BREW_IN_SHOP = 2
const val BREW_METHOD_SHOP = 10

// グローバル変数たち
lateinit var brewRealmConfig: RealmConfiguration
lateinit var beansRealmConfig: RealmConfiguration
lateinit var takeoutRealmConfig: RealmConfiguration

lateinit var brewMethods: Array<String>
lateinit var brewMethodsCR: Array<String>
lateinit var brewMethodsImages: TypedArray

lateinit var beansKind: Array<String>   // 以下３種のタイトル配列
lateinit var beansSpecial: Array<String>
lateinit var beansBlend: Array<String>
lateinit var beansPack: Array<String>

lateinit var takeoutKind: Array<String> // 以下３種のタイトル配列
lateinit var takeoutConvini: Array<String>
lateinit var takeoutCafe: Array<String>
lateinit var takeoutRestaurant: Array<String>

lateinit var roastLabels: Array<String>
lateinit var grindLabels: Array<String>


// 一番最初に実行されるApplicationクラス
// いつもの、AppCompatActivity（MainActivity）は、manifest.xmlで最初の画面（Acitivity）として実行される
// Application（CustomApplication）も、manifest.xmlで最初のクラスとして実行される
// で、その実行順位が、Application ＞ AppCompatActivityとなっているので、こっちの方が先
// 今回は、データベース作成のために最初にここで起動させる

class StartApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Realm全体の初期化処理
        Realm.init(applicationContext)

        // ブリューデータを作る（最初はサンプル込み）
        createBrewData()

        // 豆データを作る
        createBeansData()

        // テイクアウトデータを作る
        createTakeoutData()

        // そのほか、グローバル変数セット
        brewMethodsImages = resources.obtainTypedArray(R.array.method_images)

        // brewMethodsは、改行アリ（Recycler用）、改行無し（Edit用）の２つを作る
        brewMethodsCR = resources.getStringArray(R.array.method_names)
        // 含まれる\nを削除
        brewMethods=arrayOf<String>()
        for( b in brewMethodsCR ) brewMethods += b.replace("\n","")

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

//        setTakeoutTakeDay()

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
