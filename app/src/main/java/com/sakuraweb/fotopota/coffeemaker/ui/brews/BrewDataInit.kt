package com.sakuraweb.fotopota.coffeemaker.ui.brews

import java.util.*

// BrewDataがRealm内に存在しないときに、作るためのstruct
// ほとんど出番なし。何とかならんのか？

class BrewDataInit (
    var date: String,
    var rating: Float,
    var methodID: Int,
    var place: Int,
    var beansID: Long,
    var beansPast: Int,
    var beansGrind: Float,
    var beansUse: Float,
    var cups: Float,
    var cupsDrunk: Float,
    var temp: Float,
    var steam: Float,
    var imageURI: String,
    var memo: String,
    var takeoutID: Long) {

}