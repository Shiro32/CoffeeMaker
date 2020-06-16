package com.sakuraweb.fotopota.coffeemaker

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansDataInit
import com.sakuraweb.fotopota.coffeemaker.ui.home.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.home.BrewDataInit
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.createObject
import java.text.SimpleDateFormat
import java.util.*


// グローバル変数たち
lateinit var brewRealmConfig: RealmConfiguration
lateinit var beansRealmConfig: RealmConfiguration

lateinit var brewMethods: Array<String>

// 一番最初に実行されるApplicationクラス
// いつもの、AppCompatActivity（MainActivity）は、manifest.xmlで最初の画面（Acitivity）として実行される
// Application（CustomApplication）も、manifest.xmlで最初のクラスとして実行される
// で、その実行順位が、Application ＞ AppCompatActivityとなっているので、こっちの方が先
// 今回は、データベース作成のために最初にここで起動させる

class StartApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Realm全体の初期化処理
        Realm.init(this)

        // ブリューデータを作る（最初はサンプル込み）
        createBrewData()

        // 豆データを作る
        createBeansData()

        // そのほか、グローバル変数セット
        brewMethods = resources.getStringArray(R.array.method_names)



    }

    private fun createBeansData() {
        // configを設定
        beansRealmConfig = RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .name("beans.realm")
            .build()

        // インスタンス化
        val realm = Realm.getInstance(beansRealmConfig)
        val beans: RealmResults<BeansData> = realm.where(
            BeansData::class.java).findAll().sort("id", Sort.DESCENDING)

        // データ数ゼロならサンプルを作る
        if( beans.size == 0) {
            val beansList = listOf<BeansDataInit>(
                BeansDataInit(
                    "2019/12/31",
                    "マイルドKALDI",
                    200,
                    1,
                    "KALDI",
                    399,
                    "特売"
                ),
                BeansDataInit(
                    "2020/03/01",
                    "ブルーマウンテンブレンド",
                    100,
                    3,
                    "神戸屋珈琲",
                    2000,
                    "超奮発"
                )
            )

            // DB書き込み
            realm.beginTransaction()
            var id = 1
            for(i in beansList.reversed()) {
                var b = realm.createObject<BeansData>(id++)
                b.date      = i.date.toDate("yyyy/MM/dd")
                b.name      = i.name
                b.gram      = i.gram
                b.roast     = i.roast
                b.shop      = i.shop
                b.price     = i.price
                b.memo      = i.memo
            }
            realm.commitTransaction()
            blackToast(this,"BEANSデータベース完了！")
        }
        realm.close()
    }

    private fun createBrewData() {
        brewRealmConfig = RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .name("brews.realm")
            .build()

        val realm = Realm.getInstance(brewRealmConfig)
        val brews: RealmResults<BrewData> = realm.where(
            BrewData::class.java).findAll().sort("id", Sort.DESCENDING)

        // データゼロなら作る
        if( brews.size == 0 ) {
            val brewList = listOf<BrewDataInit>(
                BrewDataInit(
                    "2020/06/01 12:34",
                    2,
                    0,
                    0,
                    1,
                    1,
                    1,
                    1,
                    1,
                    1,
                    "http",
                    "サンプルです"
                ),
                BrewDataInit(
                    "2020/06/02 00:00",
                    5,
                    1,
                    1,
                    1,
                    4,
                    5,
                    1,
                    1,
                    4,
                    "http",
                    "2杯目"
                ),
                BrewDataInit(
                    "2020/06/03 00:00",
                    3,
                    2,
                    2,
                    1,
                    2,
                    5,
                    1,
                    1,
                    4,
                    "http",
                    "最高の出来です"
                ),
                BrewDataInit(
                    "2020/06/04 00:00",
                    2,
                    3,
                    1,
                    1,
                    3,
                    5,
                    1,
                    1,
                    1,
                    "http",
                    "最悪です"
                ),
                BrewDataInit(
                    "2020/06/01 12:34",
                    2,
                    4,
                    1,
                    1,
                    1,
                    1,
                    1,
                    1,
                    1,
                    "http",
                    "サンプルです"
                ),
                BrewDataInit(
                    "2020/06/02 00:00",
                    5,
                    5,
                    1,
                    1,
                    4,
                    5,
                    1,
                    1,
                    4,
                    "http",
                    "2杯目"
                ),
                BrewDataInit(
                    "2020/06/03 00:00",
                    3,
                    6,
                    1,
                    1,
                    2,
                    5,
                    1,
                    1,
                    4,
                    "http",
                    "最高の出来です"
                ),
                BrewDataInit(
                    "2020/06/04 00:00",
                    2,
                    7,
                    1,
                    1,
                    3,
                    5,
                    1,
                    1,
                    1,
                    "http",
                    "最悪です"
                )
            )
            // DBに書き込む
            realm.beginTransaction()
            var id = 1
            for (i in brewList.reversed()) {
                val c = realm.createObject<BrewData>(id++)
                c.date      = i.date.toDate()
                c.rating    = i.rating
                c.methodID  = i.methodID
                c.beansID   = i.beansID
                c.beansPass = i.beansPass
                c.beansGrind= i.beansGrind
                c.beansUse  = i.beansUse
                c.cups      = i.cups
                c.temp      = i.temp
                c.steam     = i.steam
                c.imageURI  = i.imageURI
                c.memo      = i.memo
            }
            realm.commitTransaction()
            blackToast(this,"BREWデータベース完了！")
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

fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm"): Date {
    val df = SimpleDateFormat(pattern)
    val dt = df.parse(this)
    return dt

}