package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.net.Uri
import io.realm.DynamicRealm
import io.realm.RealmMigration
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmModule
import java.util.*

// コーヒー器具データのデータ形式Class
// Realmで使うためには、絶対にopenにしないといけないので注意！

// ★★データ項目（名前も）を変えた場合は、migrateメソッドに追記し、VERSIONも+1すること
const val EQUIP_DATA_VERSION = 0L

@RealmModule(classes = [EquipData::class])
class EquipDataModule

open class EquipData : RealmObject() {
    // PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    var icon: Int = 0   // 器具アイコン
    var name: String = ""   // 製品名
    var maker: String = ""      // メーカー
    var rating: Float = 0F
    var date: Date? = null    // 購入日
    var recent: Date? = null   // 最近の利用日
    var shop: String = ""   // 購入店
    var price: Int = 0      // 購入価格
    var count: Int = 0
    var memo: String = ""   // メモ
}


// データベース構造（名称だけでも）に変更があった場合のMigration処理
// 初期バージョン（0）から順に最新版までたどって、versionを上げていく。すごいねぇ・・・。
class EquipDataMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val realmSchema = realm.schema
        var oldVersion = oldVersion

//        if( oldVersion==0L ) {
//            oldVersion = oldVersion
//            realmSchema.get("EquipData")!!
//                .addField("recent", Date::class.java)
//            oldVersion++
//        }

    }
}
