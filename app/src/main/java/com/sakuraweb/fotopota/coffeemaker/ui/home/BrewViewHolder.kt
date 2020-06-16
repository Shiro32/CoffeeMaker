package com.sakuraweb.fotopota.coffeemaker.ui.home

import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.warkiz.widget.IndicatorSeekBar
import kotlinx.android.synthetic.main.one_brew_card.view.*

// ViewHolderを作る
// 要するに1行分のViewを保持する（データではない）、描画用のインスタンス。
// 保持するView自体はアダプタ側で作って渡されてくる
// 画面に入ってる範囲のViewが用意され、外に出ると再利用されて別要素に切り替わったりする（Recycler）
// RecyclerViewのサブクラス、ViewHolderクラス（引数itemView）を継承して、アプリ用のViewを保持できるようにする
// 継承元のクラス（今回だとRecyclerView.ViewHolder）の引数の型は気にしないのね（変えられないから？）

class BrewViewHolder(iv: View) : RecyclerView.ViewHolder(iv){
    var dateText:       TextView? = null
    var ratingBar:      RatingBar? = null
    var methodText:     TextView? = null
    var beansKindText:  TextView? = null
    var beansPassText:  TextView? = null
    var beansGrindBar:  IndicatorSeekBar? = null
    var beansUseBar:    IndicatorSeekBar? = null
    var cupsBar:        IndicatorSeekBar? = null
    var tempBar:        IndicatorSeekBar? = null
    var steamBar:       IndicatorSeekBar? =null
    var memoText:       TextView? = null
    var image:          ImageView? = null
    var editBtn:        Button? = null
    var copyBtn:        Button? = null

    var sampleBar:      IndicatorSeekBar?=null

    // 初期化処理
    // KOTLINではプライマリコンストラクタは、引数をローカル変数にコピーするだけ （ivに貰っている）
    // より具体的な初期化処理はinitブロックに記載する模様
    // プライマリコンストラクタ（引数代入）→イニシャライザ（init）→セカンダリコンストラクタ
    // のちに、アダプタの方から、holder.dateText = "2020/6/1" みたいに表示できる文字列を設定する
    init {
        dateText        = iv.oneBrewDateText
        ratingBar       = iv.oneBrewRatingBar
        methodText      = iv.oneBrewMethodText

        beansKindText   = iv.oneBrewBeansKindText
        beansPassText   = iv.oneBrewBeansPassText

        // SeekBarだらけ
        beansGrindBar   = iv.brewEditCupBar
        beansUseBar     = iv.oneBrewBeansUseBar
        cupsBar         = iv.oneBrewCupsBar
        tempBar         = iv.oneBrewTempBar
        steamBar        = iv.oneBrewSteamBar

        memoText        = iv.oneBrewMemoText
        image           = iv.oneBrewImage
        editBtn         = iv.oneBrewEditBtn
        copyBtn         = iv.oneBrewCopyBtn

    }
}

