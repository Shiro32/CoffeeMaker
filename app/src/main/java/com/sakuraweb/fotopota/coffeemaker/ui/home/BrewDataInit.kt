package com.sakuraweb.fotopota.coffeemaker.ui.home

import java.util.*

// BrewDataがRealm内に存在しないときに、作るためのstruct
// ほとんど出番なし。何とかならんのか？

class BrewDataInit (
    var date: String,
    var rating: Int,
    var methodID: Int,
    var beansID: Long,
    var beansPass: Int,
    var beansGrind: Int,
    var beansUse: Int,
    var cups: Int,
    var temp: Int,
    var steam: Int,
    var imageURI: String,
    var memo: String) {

}

