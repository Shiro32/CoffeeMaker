package com.sakuraweb.fotopota.coffeemaker.ui.brews

import com.sakuraweb.fotopota.coffeemaker.GRIND_UNIT_INT
import com.sakuraweb.fotopota.coffeemaker.HOT_COFFEE
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmModule
import java.util.*

// コーヒーデータのデータ形式Class
// Realmで使うためには、openにしないといけないので注意！

// なぜか、BREWのマイグレーション処理だけはうまく機能している（TakeoutやBeansは壊滅的なのに）
// ★★データ項目（名前も）を変えた場合は、migrateメソッドに追記し、VERSIONも+1すること
// v1   外飲み用のIDを追加
// v2   作った杯数とは別に、飲んだ杯数を追加
// v3   挽度合いを、ネジ回転数で表示できるように
// v4   挽度合いの表示を切り替えるSWを追加
// v5   ミルク・砂糖の有無、アイス・ホットのＳＷ

const val BREW_DATA_VERSION = 8L
var brewDataMigrated5to6 = false
var brewDataMigrated7to8 = false

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
    var methodID: Int = 0   // v5までは内部配列（文字・絵）の番号。v6からは不使用にして、equipIDを使う
    var equipID: Long = 0   // v6から登場
    var place: Int = 0
    var shop: String = ""   // 外飲みの時だけ、購入店舗
    var beansID: Long = 0
    var beansPast: Int = 0
    var beansGrindSw: Int = 0       // v4から登場
    var beansGrindUnit: Int = GRIND_UNIT_INT // v8から登場
    var beansGrind: Float = 0.0F
    var beansGrind2: Float = 0.0F   // v3から登場
    var beansUse: Float = 0.0F
    var cups: Float = 0.0F
    var cupsDrunk: Float = 0.0F
    var waterVolume: Float = 0.0F // v8
    var temp: Float = 0.0F
    var steam: Float = 0.0F
    var imageURI: String = ""
    var memo: String=""
    var takeoutID: Long = 0

    var milk: Float = 0.0F  // v5 単位無しで、0～100
    var sugar: Float = 0.0F // v5　単位無しで、0～100
    var iceHotSw: Int = HOT_COFFEE   // v5 0:Hot, 1:Ice
    var brewTime: Float = 0.0F // v7で登場

    var price: Int = 0 // 家飲み=1、外飲み=2　全然使ってなかったよ・・・
}

// データベース構造（名称だけでも）に変更があった場合のMigration処理
// 初期バージョン（0）から順に最新版までたどって、versionを上げていく。すごいねぇ・・・。
class BrewDataMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val realmSchema = realm.schema
        var oldVersion = oldVersion

        if( oldVersion==0L ) {
            realmSchema.get("BrewData")!!
                .addField("takeoutID", Long::class.java)
            oldVersion++
        }
        if( oldVersion==1L ) {
            realmSchema.get("BrewData")!!
                .addField("cupsDrunk", Float::class.java)
            oldVersion++
        }
        if( oldVersion==2L ) {
            realmSchema.get("BrewData")!!
                .addField("beansGrind2", Float::class.java)
            oldVersion++
        }
        if( oldVersion==3L ) {
            realmSchema.get("BrewData")!!
                .addField("beansGrindSw", Int::class.java)
            oldVersion++
        }
        if( oldVersion==4L ) {
            realmSchema.get("BrewData")!!
                .addField( "milk", Float::class.java)
                .addField( "sugar", Float::class.java)
                .addField( "iceHotSw", Int::class.java)
            oldVersion++
        }
        if( oldVersion==5L ) {
            // 大型アップデート。EQUIPデータベースができたための対応
            // 内部の配列に対するmethodIDを不使用にする
            // かわりに、equipID登場
            realmSchema.get("BrewData")!!
                .addField("equipID", Long::class.java )
            oldVersion++

            brewDataMigrated5to6 = true
        }
        if( oldVersion==6L ) {
            // 抽出時間登場
            realmSchema.get("BrewData")!!
                .addField("brewTime", Float::class.java )
            oldVersion++
        }
        if( oldVersion==7L ) {
            // 挽目の単位、湯量登場
            realmSchema.get("BrewData")!!
                .addField("beansGrindUnit", Int::class.java )
                .addField("waterVolume", Float::class.java )
            oldVersion++

            brewDataMigrated7to8 = true
        }
    }
}

