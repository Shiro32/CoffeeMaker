package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.net.Uri
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

// コーヒー豆データのデータ形式Class
// Realmで使うためには、絶対にopenにしないといけないので注意！

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
    var memo: String = ""   // メモ

    var option1: Float = 0.0F
    var option2: Float = 0.0F
    var option3: String = ""
    var option4: String = ""
    var option5: Long = 0
    var option6: Long = 0

    var dummy: Int = 0
}
