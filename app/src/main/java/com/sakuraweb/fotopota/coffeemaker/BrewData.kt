package com.sakuraweb.fotopota.coffeemaker

import android.net.Uri
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*


// Realmで使うためには、絶対にopenにしないといけないので注意！

open class BrewData : RealmObject() {
    // よくわからんが、PrimaryKeyというアノテーションで、DBの検索キーにするらしい
    @PrimaryKey
    var id: Long = 0

    lateinit  var date: Date
    var rating: Int = 0
    var methodID: Int = 0
    var beansID: Int = 0
    var beansPass: Int = 0
    var beansGrind: Int = 0
    var beansUse: Int = 0
    var cups: Int = 0
    var temp: Int = 0
    var steam: Int = 0
    var imageURI: String = ""
    var memo: String=""

}




