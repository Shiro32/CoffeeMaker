package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.one_brew_card_home.view.*
import kotlinx.android.synthetic.main.one_brew_card_home.view.oneBrewBeansKindText
import kotlinx.android.synthetic.main.one_brew_card_home.view.oneBrewDateText
import kotlinx.android.synthetic.main.one_brew_card_home.view.oneBrewImage
import kotlinx.android.synthetic.main.one_brew_card_home.view.oneBrewMemoText
import kotlinx.android.synthetic.main.one_brew_card_home.view.oneBrewPastText
import kotlinx.android.synthetic.main.one_brew_card_home.view.oneBrewRatingBar
import kotlinx.android.synthetic.main.one_brew_card_shop.view.*
import kotlinx.android.synthetic.main.one_brew_flat_home.view.miniBeansText
import kotlinx.android.synthetic.main.one_brew_flat_home.view.miniBrewTimeText
import kotlinx.android.synthetic.main.one_brew_flat_home.view.miniGrindText
import kotlinx.android.synthetic.main.one_brew_flat_home.view.miniSteamText
import kotlinx.android.synthetic.main.one_brew_flat_home.view.miniTempText
import kotlinx.android.synthetic.main.one_brew_flat_home.view.miniVolumeText
import kotlinx.android.synthetic.main.one_brew_flat_home.view.oneBrewMethodText

// ViewHolderを作る
// 要するに1行分のViewを保持する（データではない）、描画用のインスタンス。
// 保持するView自体はアダプタ側で作って渡されてくる
// 画面に入ってる範囲のViewが用意され、外に出ると再利用されて別要素に切り替わったりする（Recycler）
// RecyclerViewのサブクラス、ViewHolderクラス（引数itemView）を継承して、アプリ用のViewを保持できるようにする
// 継承元のクラス（今回だとRecyclerView.ViewHolder）の引数の型は気にしないのね（変えられないから？）

class BrewViewHolder(iv: View) : RecyclerView.ViewHolder(iv){
    var dateText:       TextView? = null
    var pastText:       TextView? = null
    var ratingBar:      RatingBar? = null
    var beansKindText:  TextView? = null
    var beansPassText:  TextView? = null
    var shopText:       TextView? = null
    var memoText:       TextView? = null
    var methodText:     TextView? = null
    var image:          ImageView? = null

    var miniGrindText:  TextView? = null
    var miniBeansText:  TextView? = null
    var miniTempText:   TextView? = null
    var miniSteamText:  TextView? = null
    var miniVolumeText: TextView? = null
    var miniBrewTimeText:TextView?= null
    var miniSugarText:  TextView? = null
    var miniMilkText:   TextView? = null
    var miniCBRText:    TextView? = null
    var miniCupsBrewedText: TextView? = null
    var miniCupsDrunkText:  TextView? = null

    var miniGrind:      LinearLayout? = null
    var miniBeans:      LinearLayout? = null
    var miniTemp:       LinearLayout? = null
    var miniSteam:      LinearLayout? = null
    var miniVolume:     LinearLayout? = null
    var miniBrewTime:   LinearLayout? = null
    var miniSugar:      LinearLayout? = null
    var miniMilk:       LinearLayout? = null
    var miniCBR:        LinearLayout? = null
    var miniCupsBrewed: LinearLayout? = null
    var miniCupsDrunk:  LinearLayout? = null

    // 初期化処理
    // KOTLINではプライマリコンストラクタは、引数をローカル変数にコピーするだけ （ivに貰っている）
    // より具体的な初期化処理はinitブロックに記載する模様
    // プライマリコンストラクタ（引数代入）→イニシャライザ（init）→セカンダリコンストラクタ
    // のちに、アダプタの方から、holder.dateText = "2020/6/1" みたいに表示できる文字列を設定する
    init {
        dateText        = iv.oneBrewDateText
        pastText        = iv.oneBrewPastText
        ratingBar       = iv.oneBrewRatingBar

        beansKindText   = iv.oneBrewBeansKindText
        methodText      = iv.oneBrewMethodText

        // ミニアイコン
        miniGrindText   = iv.miniGrindText
        miniBeansText   = iv.miniBeansText
        miniTempText    = iv.miniTempText
        miniVolumeText  = iv.miniVolumeText
        miniSteamText   = iv.miniSteamText
        miniBrewTimeText= iv.miniBrewTimeText
        miniSugarText   = iv.miniSugarText
        miniMilkText    = iv.miniMilkText
        miniCBRText     = iv.miniCBRText
        miniCupsBrewedText = iv.miniCupsBrewedText
        miniCupsDrunkText  = iv.miniCupsDrunkText

        miniGrind       = iv.miniGrind
        miniBeans       = iv.miniBeans
        miniTemp        = iv.miniTemp
        miniSteam       = iv.miniSteam
        miniVolume      = iv.miniVolume
        miniBrewTime    = iv.miniBrewTime
        miniMilk        = iv.miniMilk
        miniCBR         = iv.miniCBR
        miniSugar       = iv.miniSugar
        miniCupsBrewed  = iv.miniCupsBrewed
        miniCupsDrunk   = iv.miniCupsDrunk

        shopText        = iv.oneBrewShopText
        memoText        = iv.oneBrewMemoText
        image           = iv.oneBrewImage
    }
}
