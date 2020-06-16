package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.net.Uri
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*


// Realmで使うためには、絶対にopenにしないといけないので注意！

open class BeansData : RealmObject() {
    // PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    lateinit  var date: Date    // 購入日
    var name: String = ""   // 銘柄
    var gram: Int = 0     // グラム
    var roast: Int = 0      // 焙煎（深煎り～浅煎りまで）
    var shop: String = ""   // 購入店
    var price: Int = 0      // 購入価格
    var memo: String = ""   // メモ

    var dummy: Int = 0
}
