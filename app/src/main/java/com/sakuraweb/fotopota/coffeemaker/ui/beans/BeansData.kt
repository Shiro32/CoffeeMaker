package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.net.Uri
import io.realm.DynamicRealm
import io.realm.RealmMigration
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmModule
import java.util.*

// コーヒー豆データのデータ形式Class
// Realmで使うためには、絶対にopenにしないといけないので注意！

// ★★データ項目（名前も）を変えた場合は、migrateメソッドに追記し、VERSIONも+1すること
const val BEANS_DATA_VERSION = 0L

@RealmModule(classes = [BeansData::class])
class BeansDataModule

open class BeansData : RealmObject() {
    // PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    var name: String = ""   // 銘柄
    var rating: Float = 0F
    lateinit  var date: Date    // 購入日
    var gram: Float = 0F     // グラム
    var roast: Float = 0F      // 焙煎（深煎り～浅煎りまで）
    var shop: String = ""   // 購入店
    var price: Int = 0      // 購入価格
    var count: Int = 0
    var memo: String = ""   // メモ

}


// データベース構造（名称だけでも）に変更があった場合のMigration処理
// 初期バージョン（0）から順に最新版までたどって、versionを上げていく。すごいねぇ・・・。
class BeansDataMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val realmSchema = realm.schema
        var oldVersion = oldVersion

        if( oldVersion==0L ) {
//            realmSchema.get("BeansData")!!
//                .removeField("option1")
//                .removeField("option2")
//                .removeField("option3")
//                .removeField("option4")
//                .removeField("option5")
//                .removeField("option6")
//                .removeField("dummy")
//
//            realmSchema.remove("BrewData")
            oldVersion++
        }
    }
}
