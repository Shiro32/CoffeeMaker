package com.sakuraweb.fotopota.coffeemaker.ui.beans

// BeansDataがRealm内に存在しないときに、作るためのstruct
// ほとんど出番なし。何とかならんのか？

class BeansDataInit (
    var name: String,
    var rating: Float,
    var date: String,
    var gram: Float,
    var roast: Float,
    var shop: String,
    var price: Int,
    var count: Int,
    var memo: String
)
{}

