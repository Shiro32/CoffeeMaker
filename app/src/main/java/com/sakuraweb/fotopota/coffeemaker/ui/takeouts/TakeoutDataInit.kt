package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

// TakeoutDataがRealm内に存在しないときに、作るためのstruct
// ほとんど出番なし。何とかならんのか？

class TakeoutDataInit (
    var name: String,
    var rating: Float,
    var chain: String,
    var shop: String, // 廃止予定
    var price: Int,
    var count: Int,
    var size: String,
    var memo: String
)
{}
