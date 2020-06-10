package com.sakuraweb.fotopota.coffeemaker

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.createObject
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

lateinit var  brewConfig: RealmConfiguration

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

        blackToast(this,"BREWデータベース完了！")
    }
}

private fun createBrewData() {
    brewConfig = RealmConfiguration.Builder()
        .deleteRealmIfMigrationNeeded()
        .name("brews.realm")
        .build()

    Realm.setDefaultConfiguration(brewConfig)


    val realm = Realm.getDefaultInstance()
    val brews: RealmResults<BrewData> = realm.where(BrewData::class.java).findAll().sort("id", Sort.DESCENDING)

    // データゼロなら作る
    if( brews.size == 0 ) {
        val brewList = listOf<BrewDataInit>(
            BrewDataInit("2020/06/01 12:34:56", 2, 1, 1, 1, 1, 1, 1, 1, 1, "http", "サンプルです"),
            BrewDataInit("2020/06/01 00:00:00", 2, 1, 1, 1, 1, 1, 1, 1, 1, "http", "サンプルです"),
            BrewDataInit("2020/06/01 00:00:00", 2, 1, 1, 1, 1, 1, 1, 1, 1, "http", "サンプルです"),
            BrewDataInit("2020/06/01 00:00:00", 2, 1, 1, 1, 1, 1, 1, 1, 1, "http", "サンプルです")
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
    }
    realm.close()

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

fun String.toDate(pattern: String = "yyyy/MM/dd HH:mm:ss"): Date {
    val df = SimpleDateFormat(pattern)
    val dt = df.parse(this)
    return dt

/*
    val sdFormat = try {
        SimpleDateFormat(pattern)
    } catch (e: IllegalArgumentException) {
        null
    }
    val date = sdFormat?.let {
        try {
            it.parse(this)
        } catch (e: ParseException){
            null
        }
    }
    return date
*/
}