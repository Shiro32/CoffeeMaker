package com.sakuraweb.fotopota.coffeemaker.ui.merge

import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.warkiz.widget.IndicatorSeekBar
import kotlinx.android.synthetic.main.one_beans_card.view.*

class BeansMergeViewHolder(iv: View) : RecyclerView.ViewHolder (iv){
    var name:   TextView? = null
    var ratingBar:  RatingBar? = null
    var ratingText: TextView? = null
    var dateText:   TextView? = null
    var pastText:   TextView? = null
    var roastBar:   IndicatorSeekBar? = null
    var shop:       TextView? = null
    var price:      TextView? = null
    var memo:       TextView? = null
    var memoLabel:  TextView? = null
    var repeat:     TextView? = null
    var count:      TextView? = null

    init {
        name        = iv.oneBeansNameText
        ratingBar   = iv.oneBeansRatingBar
        ratingText  = iv.oneBeansRatingText
        dateText    = iv.oneBeansDateText
        pastText    = iv.oneBeansPastText
        roastBar    = iv.oneBeansRoastBar
        shop        = iv.oneBeansShopText
        price       = iv.oneBeansPriceText
        memo        = iv.oneBeansMemoText
        memoLabel   = iv.oneBeansMemoLabel
        repeat      = iv.oneBeansRepeatText
        count       = iv.oneBeansCountText
    }
}