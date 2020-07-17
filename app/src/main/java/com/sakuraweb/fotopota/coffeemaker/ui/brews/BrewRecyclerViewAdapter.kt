package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansDateByID
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.findTakeoutChainNameByID
import io.realm.RealmResults
import java.text.SimpleDateFormat

const val REQUEST_CODE_SHOW_DETAILS = 1

// リスト本体ボディのリスナは不要
//　リスト本体をクリックして確定させたいような場合は、こいつのクラスをマルチ継承するみたい
class BrewRecyclerViewAdapter(brewsRealm: RealmResults<BrewData>):
    RecyclerView.Adapter<BrewViewHolder>() {

    private val brews: RealmResults<BrewData> = brewsRealm

    override fun getItemViewType(position: Int): Int {

        return brews[position]?.place as Int

//        if( brews[position]?.methodID == BREW_METHOD_SHOP ) {
//            return BREW_IN_SHOP
//        } else {
//            return BREW_IN_HOME
//        }
    }

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrewViewHolder {
        val view:View

        // 新しいView（1行）を生成する
        // 家飲みと店飲みを分けて作る
        if( viewType == BREW_IN_HOME ) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.one_brew_card_home, parent, false)
        }  else {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.one_brew_card_shop, parent, false)
        }

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        // ビューホルダは、内部のローカル変数に1行分のデータを保持
        val holder = BrewViewHolder(view)
        return holder
    }

    // ViewHolderの表示内容を更新する
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
    override fun onBindViewHolder(holder: BrewViewHolder, position: Int) {
        val bp = brews[position]

        if( bp!=null ) {
            var days = ""

            // 家飲みの場合は抽出情報（店飲みの場合は不要）
            if( bp.methodID != BREW_METHOD_SHOP ) {
                holder.beansPassText?.text = bp.beansPast.toString()
                holder.beansGrindBar?.setProgress(bp.beansGrind.toFloat())
                holder.beansUseBar?.setProgress(bp.beansUse.toFloat())
                holder.cupsBar?.setProgress(bp.cups.toFloat())
                holder.tempBar?.setProgress(bp.temp.toFloat())
                holder.steamBar?.setProgress(bp.steam.toFloat())

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
            holder.methodText?.text     = brewMethodsCR[bp.methodID]
            holder.beansKindText?.text = findBeansNameByID(bp.place, bp.beansID, bp.takeoutID )

            // TODO: 「メモ」というラベルも消さないと。結局、レイアウトで包んだ方がイイかな？
            if( bp.memo!="" ) {
                holder.memoText?.text = bp.memo
                holder.memoText?.visibility = View.VISIBLE
                holder.memoLabel?.visibility = View.VISIBLE
            } else {
                holder.memoText?.visibility = View.GONE
                holder.memoLabel?.visibility = View.GONE
            }

            // 抽出方法にあったイラスト（アイコン）
            holder.image?.setImageDrawable(brewMethodsImages.getDrawable(bp.methodID))

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

