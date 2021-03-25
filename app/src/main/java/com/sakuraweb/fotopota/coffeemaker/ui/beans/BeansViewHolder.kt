package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.warkiz.widget.IndicatorSeekBar
import kotlinx.android.synthetic.main.one_beans_card.view.*

class BeansViewHolder(iv: View) : RecyclerView.ViewHolder (iv){
    var name:   TextView? = null
    var ratingBar:  RatingBar? = null
    var dateText:   TextView? = null
    var pastText:   TextView? = null
    var gramBar:    IndicatorSeekBar? = null
    var roastBar:   IndicatorSeekBar? = null
    var shop:       TextView? = null
    var price:      TextView? = null
    var memo:       TextView? = null
    var memoLabel:  TextView? = null


    init {
        name        = iv.oneBeansNameText
        ratingBar   = iv.oneBeansRatingBar
        dateText    = iv.oneBeansDateText
        pastText    = iv.oneBeansPastText
        roastBar    = iv.oneBeansRoastBar
        shop        = iv.oneBeansShopText
        price       = iv.oneBeansPriceText
        memo        = iv.oneBeansMemoText
        memoLabel   = iv.oneBeansMemoLabel

    }
}