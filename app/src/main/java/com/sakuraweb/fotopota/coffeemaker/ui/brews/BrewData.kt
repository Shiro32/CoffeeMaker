package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.net.Uri
import io.realm.DynamicRealm
import io.realm.RealmMigration
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmModule
import java.util.*

// コーヒーデータのデータ形式Class
// Realmで使うためには、openにしないといけないので注意！

// ★★データ項目（名前も）を変えた場合は、migrateメソッドに追記し、VERSIONも+1すること
const val BREW_DATA_VERSION = 0L

// この記載と、Configuration時のModules指定をしないと、すべての関連ClassがDB化される
// 個別のClassのバージョンアップができないので、こうやって単独化させてあげる
@RealmModule(classes = [BrewData::class])
class BrewDataModule

open class BrewData : RealmObject() {
    // よくわからんが、PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    lateinit  var date: Date
    var rating: Float = 0.0F
    var methodID: Int = 0
    var place: Int = 0
    var beansID: Long = 0
    var beansPast: Int = 0
    var beansGrind: Float = 0.0F
    var beansUse: Float = 0.0F
    var cups: Float = 0.0F
    var temp: Float = 0.0F
    var steam: Float = 0.0F
    var imageURI: String = ""
    var memo: String=""
}

// データベース構造（名称だけでも）に変更があった場合のMigration処理
// 初期バージョン（0）から順に最新版までたどって、versionを上げていく。すごいねぇ・・・。
class BrewDataMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val realmSchema = realm.schema
        var oldVersion = oldVersion

        if( oldVersion==0L ) {
//            realmSchema.get("BrewData")!!
//                .addField("price", Long::class.java)
//                .removeField("option1")
//                .removeField("option2")
//                .removeField("option3")
//                .removeField("option4")
//                .removeField("option5")
//                .removeField("option6")
//            oldVersion++
        }
    }
}

