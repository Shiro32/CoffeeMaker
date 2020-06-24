package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.net.Uri
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

// コーヒーデータのデータ形式Class
// Realmで使うためには、絶対にopenにしないといけないので注意！

open class BrewData : RealmObject() {
    // よくわからんが、PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    lateinit  var date: Date
    var rating: Float = 0.0F
    var methodID: Int = 0
    var beansID: Long = 0
    var beansPast: Int = 0
    var beansGrind: Float = 0.0F
    var beansUse: Float = 0.0F
    var cups: Float = 0.0F
    var temp: Float = 0.0F
    var steam: Float = 0.0F
    var imageURI: String = ""
    var memo: String=""

    var option1: Float = 0.0F
    var option2: Float = 0.0F
    var option3: String = ""
    var option4: String = ""
    var option5: Long = 0
    var option6: Long = 0
//    var dummy: Int = 0
}




