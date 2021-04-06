package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansDateByID
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import com.sakuraweb.fotopota.coffeemaker.ui.equip.findEquipIconByID
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.findTakeoutChainNameByID
import io.realm.RealmResults
import kotlinx.android.synthetic.main.one_brew_card_home.view.*
import java.text.SimpleDateFormat

const val REQUEST_CODE_SHOW_DETAILS = 1

// リスト本体ボディのリスナは不要
//　リスト本体をクリックして確定させたいような場合は、こいつのクラスをマルチ継承するみたい
class BrewRecyclerViewAdapter(brewsRealm: RealmResults<BrewData>):
    RecyclerView.Adapter<BrewViewHolder>() {

    private val brews: RealmResults<BrewData> = brewsRealm

    // ここで外のみのチェック
    // よくこんな方法、思いついたな・・・。
    override fun getItemViewType(position: Int): Int {

        return brews[position]?.place as Int
    }

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrewViewHolder {
        // 家飲み、店飲み、フラット、カードで4種類
        val view: View
        if( viewType == BREW_IN_HOME ) {
            view = LayoutInflater.from(parent.context)
                .inflate( if(brewListLayoutStyle==0) R.layout.one_brew_card_home else R.layout.one_brew_flat_home, parent, false)
                if( !configSteamSw ) {
                    view.oneBrewSteamBar.visibility = View.GONE
                    view.oneBrewSteamLabel.visibility = View.GONE
                }
        }  else {
            view = LayoutInflater.from(parent.context)
                .inflate( if(brewListLayoutStyle==0) R.layout.one_brew_card_shop else R.layout.one_brew_flat_shop, parent, false)
        }

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        // ビューホルダは、内部のローカル変数に1行分のデータを保持
        return BrewViewHolder(view)
    }

    // ViewHolderの表示内容を更新する
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
    override fun onBindViewHolder(holder: BrewViewHolder, position: Int) {
        val bp = brews[position]

        if( bp!=null ) {
            var days: String

            // 家飲みの場合は抽出情報（店飲みの場合は不要）
            if( bp.place == BREW_IN_HOME ) {
                holder.beansPassText?.text = bp.beansPast.toString()

                // Grindを数字入力できるようにする処理（アドホックだなぁ・・・）
                // １個しかないスライダ（beansGrindBar）を、名前・回転数、どちらかで使うSWで分ける
                if( bp.beansGrindSw == GRIND_SW_NAME ) {
                    holder.beansGrindBar?.tickCount = 5
                    holder.beansGrindBar?.min = 1F
                    holder.beansGrindBar?.max = 5F
                    holder.beansGrindBar?.hideThumbText(true)
                    holder.beansGrindBar?.setProgress(bp.beansGrind)
                    holder.beansGrindBar?.customTickTexts(grindLabels)
                } else {
                    holder.beansGrindBar?.tickCount = 2
                    holder.beansGrindBar?.min = 0F
                    holder.beansGrindBar?.max = configMillMax
                    holder.beansGrindBar?.hideThumbText(false)
                    holder.beansGrindBar?.setDecimalScale(1)
                    holder.beansGrindBar?.setProgress(bp.beansGrind2)
//                    holder.beansGrindBar?.customTickTexts(grind2Labels)
                }

                holder.beansUseBar?.setProgress(bp.beansUse)
                holder.cupsBar?.setProgress(bp.cups)
                holder.tempBar?.setProgress(bp.temp)
                holder.steamBar?.setProgress(bp.steam)

                // 豆の経過日数を計算する
                if(bp.beansID>0L) {
                    val d = findBeansDateByID(bp.beansID)
                    if( d!=null ) {
                        val diff = (bp.date.time-d.time)/(1000*60*60*24)
                        days = "（"+diff.toString()+"日経過）"
                        holder.pastText?.text = days
                    }
                }
            } else {
                // 店飲みの場合は、系列店＋実店舗名称を作る
                val chain = findTakeoutChainNameByID(bp.takeoutID)
                holder.shopText?.text = chain + "（" + bp.shop + "）"
            }

            // 家飲み・店飲み共通項目
            val df = SimpleDateFormat("yyyy/MM/dd HH:mm")
            holder.dateText?.text       = df.format(bp.date)
            holder.ratingBar?.rating    = bp.rating
//            holder.methodText?.text     = brewMethodsCR[bp.methodID]
            holder.beansKindText?.text = findBeansNameByID(bp.place, bp.beansID, bp.takeoutID )

            // メモ欄が入ってないときは欄自体消す
            if( bp.memo!="" ) {
                holder.memoText?.text = bp.memo
                holder.memoText?.visibility = View.VISIBLE
//                holder.memoLabel?.visibility = View.VISIBLE
            } else {
                holder.memoText?.visibility = View.GONE
//                holder.memoLabel?.visibility = View.GONE
            }

            // 抽出方法にあったイラスト（アイコン）
//            holder.image?.setImageDrawable(brewMethodsImages.getDrawable(bp.methodID))
            holder.image?.setImageDrawable(brewMethodsImages.getDrawable(findEquipIconByID(bp.equipID)))
            //TODO: findEquipIconByIDを作らないと
            //TODO: findEquipNameByIDもか・・・きりないな
            //TODO: いっそ、findEquipByIDにしちゃう？

            // 行そのもの（Card）のリスナ
            // 行タップすることで編集画面(BrewEdit）に移行
            // 戻り値によって、TO_LISTやTO_HOMEもあり得るのでforResultで呼ぶ
            holder.itemView.setOnClickListener(ItemClickListener(holder.itemView.context, bp))
        }
    } // onCreateViewHolder

    private inner class ItemClickListener(c: Context, b: BrewData) : View.OnClickListener {
        // こうやって独自の変数を渡せばいいんだ！　←　いや親クラス内でローカルにしておけば継承されますが・・・
        // 独自クラスのコンストラクタに設定しておいて、クラス内ローカル変数に保存しておく
        // こうすれば、クラス内のメソッドから参照できる
        val ctx = c
        val ctx2 = ctx as Activity
        val bp = b

        override fun onClick(v: View?) {
            // ここで使えるのはタップされたView（１行レイアウト、one_brew_card_home）

            // ★ポップアップ版
//            val popup = PopupMenu(ctx, v)
//            popup.menuInflater.inflate(R.menu.menu_context_brew, popup.menu)
//            popup.gravity = Gravity.RIGHT
//            popup.setOnMenuItemClickListener(MenuClickListener(ctx, bp))
//            popup.show()

//            ★とりあえずDetailsへ
            val intent = Intent(ctx, BrewDetailsActivity::class.java)
            intent.putExtra("id", bp.id)
            ctx2.startActivityForResult(intent, REQUEST_CODE_SHOW_DETAILS)
        }
    }


    // ポップアップメニューの選択結果に基づいて各種処理
    // 直接ボタンを置くのとどちらがイイか悩ましいけど、とりあえずポップアップ式で
    private inner class MenuClickListener(c: Context, b: BrewData) : PopupMenu.OnMenuItemClickListener {
        val ctx = c
        val bp = b

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when( item?.itemId ) {
                R.id.ctxMenuBrewDetails -> {
                    val it2 = ctx as Activity
                    val intent = Intent(ctx, BrewDetailsActivity::class.java)
                    intent.putExtra("id", bp.id)
                    it2.startActivityForResult(intent, REQUEST_CODE_SHOW_DETAILS)
                }
                R.id.ctxMenuBrewEdit -> {
                    val ctx2 = ctx as Activity
                    val intent = Intent(ctx, BrewEditActivity::class.java)
                    intent.putExtra("id", bp.id)
                    ctx2.startActivityForResult(intent, REQUEST_CODE_SHOW_DETAILS)
                }
            }

            return false
        }
    }

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return brews.size
    }
}

