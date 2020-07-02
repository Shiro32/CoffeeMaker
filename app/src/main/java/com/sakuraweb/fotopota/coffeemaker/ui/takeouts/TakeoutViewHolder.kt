package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.warkiz.widget.IndicatorSeekBar
import kotlinx.android.synthetic.main.one_takeout_card.view.*

class TakeoutViewHolder(iv: View) : RecyclerView.ViewHolder (iv){
    var name:   TextView? = null
    var ratingBar:  RatingBar? = null
    var chain:      TextView? = null
    var shop:       TextView? = null
    var price:      TextView? = null
    var size:       TextView? = null
    var memo:       TextView? = null


    init {
        name        = iv.oneTakeoutNameText
        ratingBar   = iv.oneTakeoutRatingBar
        chain       = iv.oneTakeoutChainText
        shop        = iv.oneTakeoutShopText
        price       = iv.oneTakeoutPriceText
        size        = iv.oneTakeoutSizeText
        memo        = iv.oneTakeoutMemoText

    }
}