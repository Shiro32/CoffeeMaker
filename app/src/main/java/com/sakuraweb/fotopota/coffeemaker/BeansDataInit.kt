package com.sakuraweb.fotopota.coffeemaker

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

class BeansDataInit(
    var date: String,
    var name: String,
    var gram: Int,
    var roast: Int,
    var shop: String,
    var price: Int,
    var memo: String) {
}

