package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.one_equip_card.view.*

// ViewHolderを作る
// 要するに1行分のViewを保持する（データではない）、描画用のインスタンス。
// 保持するView自体はアダプタ側で作って渡されてくる
// 画面に入ってる範囲のViewが用意され、外に出ると再利用されて別要素に切り替わったりする（Recycler）
// RecyclerViewのサブクラス、ViewHolderクラス（引数itemView）を継承して、アプリ用のViewを保持できるようにする
// 継承元のクラス（今回だとRecyclerView.ViewHolder）の引数の型は気にしないのね（変えられないから？）

class EquipViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {
    var name:       TextView?=null
    var maker:      TextView?=null
    var ratingBar:  RatingBar?=null
    var dateText:   TextView?=null
    var price:      TextView?=null
    var memo:       TextView?=null
    var memoLabel:  TextView?=null
    var icon:       ImageView?=null
    var shop:       TextView?=null

    // 初期化処理
    // KOTLINではプライマリコンストラクタは、引数をローカル変数にコピーするだけ （ivに貰っている）
    // より具体的な初期化処理はinitブロックに記載する模様
    // プライマリコンストラクタ（引数代入）→イニシャライザ（init）→セカンダリコンストラクタ
    // のちに、アダプタの方から、holder.dateText = "2020/6/1" みたいに表示できる文字列を設定する

    init {
        name        = iv.oneEquipNameText
        maker        = iv.oneEquipMakerText
        ratingBar   = iv.oneEquipRatingBar
        dateText    = iv.oneEquipDateText
        price       = iv.oneEquipPriceText
        memo        = iv.oneEquipMemoText
        memoLabel   = iv.oneEquipMemoLabel
        icon        = iv.oneEquipIcon
        shop        = iv.oneEquipShopText
    }
}
