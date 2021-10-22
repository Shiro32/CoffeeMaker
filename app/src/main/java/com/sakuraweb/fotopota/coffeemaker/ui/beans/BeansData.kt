package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.net.Uri
import io.realm.DynamicRealm
import io.realm.RealmMigration
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmModule
import java.util.*

// コーヒー豆データのデータ形式Class
// ★★データ項目（名前も）を変えた場合は、migrateメソッドに追記し、VERSIONも+1すること
// 他の各種ＤＢと違って、元祖ＤＢをv0ではなく、v1から始めてしまった・・・。
const val BEANS_DATA_VERSION = 3L
var beansDataMigrated1to2 = false
var beansDataMigrated2to3 = false

// v1   元祖
// v2   購入回数（repeat）、豆処理（process）を追加
// v3   初回購入（date）と最新購入（repeatDate）を分けた

@RealmModule(classes = [BeansData::class])
class BeansDataModule

open class BeansData : RealmObject() {
    // PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    var name: String = ""   // 銘柄
    var rating: Float = 0F  // 評価
    lateinit var date: Date         // 最初の購入日
    var repeatDate: Date?=null  // 最近の購入日（v3から） なぜか「?」型にしないとmigrationできなかった
    lateinit var recent: Date       // 最近の利用日
    var gram: Float = 0F    // グラム
    var roast: Float = 0F   // 焙煎（深煎り～浅煎りまで）
    var shop: String = ""   // 購入店
    var price: Int = 0      // 購入価格
    var count: Int = 0      // 利用回数
    var repeat: Int = 1     // 購入回数（v2から）
    var process: Int = 0    // 豆処理（washedなど、v2から）
    var memo: String = ""   // メモ
}


// データベース構造（名称だけでも）に変更があった場合のMigration処理
// 初期バージョン（0）から順に最新版までたどって、versionを上げていく。すごいねぇ・・・。
class BeansDataMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, old: Long, newVersion: Long) {
        val realmSchema = realm.schema
        var oldVersion = old

        // Version 1（何もなし・・・）
        if( oldVersion==0L ) {
            oldVersion++
        }

        // Version 2
        if( oldVersion==1L ) {
            realmSchema.get("BeansData")!!
                .addField("repeat", Int::class.java)
                .addField("process", Int::class.java)
            oldVersion++
            beansDataMigrated1to2 = true
        }

        // Version 3
        if( oldVersion==2L ) {
            realmSchema.get("BeansData")!!
                .addField("repeatDate", Date::class.java)
            oldVersion++
            beansDataMigrated2to3 = true
        }
    }
}
