package com.sakuraweb.fotopota.coffeemaker.ui.brews

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.*
import android.view.Gravity.*
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewMethods
import com.sakuraweb.fotopota.coffeemaker.brewMethodsImages
import com.sakuraweb.fotopota.coffeemaker.ui.beans.findBeansNameByID
import io.realm.RealmResults
import java.text.SimpleDateFormat

const val REQUEST_CODE_SHOW_DETAILS = 1

// リスト本体ボディのリスナは不要
//　リスト本体をクリックして確定させたいような場合は、こいつのクラスをマルチ継承するみたい
class BrewRecyclerViewAdapter(brewsRealm: RealmResults<BrewData>):
    RecyclerView.Adapter<BrewViewHolder>() {

    private val brews: RealmResults<BrewData> = brewsRealm

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrewViewHolder {

        // 新しいView（1行）を生成する　レイアウト画面で作った、one_brew_card（1行）
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_brew_card, parent, false)

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        // ビューホルダは、内部のローカル変数に1行分のデータを保持（日付、血圧、脈拍）
        val holder = BrewViewHolder(view)
        return holder
    }

    // ViewHolderの表示内容を更新する
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
    override fun onBindViewHolder(holder: BrewViewHolder, position: Int) {
        val bp = brews[position]

        if( bp!=null ) {

            val df = SimpleDateFormat("yyyy/MM/dd HH:mm")

            holder.dateText?.text       = df.format(bp.date)
            holder.ratingBar?.rating    = bp.rating
            holder.methodText?.text     = brewMethods[bp.methodID]
            holder.beansKindText?.text  = findBeansNameByID(bp.beansID)
            holder.memoText?.text       = bp.memo
            holder.beansPassText?.text  = bp.beansPast.toString()

            holder.beansGrindBar?.setProgress(bp.beansGrind.toFloat())
            holder.beansUseBar?.setProgress(bp.beansUse.toFloat())
            holder.cupsBar?.setProgress(bp.cups.toFloat())
            holder.tempBar?.setProgress(bp.temp.toFloat())
            holder.steamBar?.setProgress(bp.steam.toFloat())

            holder.image?.setImageDrawable(brewMethodsImages.getDrawable(bp.methodID))

            // 各カードに配置するボタンなどのリスナ

            // 行そのもの（Card）のリスナ
            // 行タップすることで編集画面(BrewEdit）に移行
            // 戻り値によって、TO_LISTやTO_HOMEもあり得るのでforResultで呼ぶ
            holder.itemView.setOnClickListener(ItemClickListener(holder.itemView.context, bp))

/*
            holder.itemView.setOnClickListener {
                val intent = Intent(it.context, BrewDetailsActivity::class.java)
                intent.putExtra("id", bp.id)
//                it.context.startActivity(intent)

                // ２時間かけて思いつきました！！！！×無限
                // contextを無理矢理変えて、forResultを呼ぶ
                // ここの結果を、メインのFragmentでキャッチして、DetailsやEditの結果を知る
                val it2 = it.context as Activity // 無茶苦茶だけど
                it2.startActivityForResult(intent, REQUEST_CODE_SHOW_DETAILS)

            }
*/
        }

    } // onCreateViewHolder

    private inner class ItemClickListener(c: Context, b: BrewData) : View.OnClickListener {
        // こうやって独自の変数を渡せばいいんだ！
        // 独自クラスのコンストラクタに設定しておいて、クラス内ローカル変数に保存しておく
        // こうすれば、クラス内のメソッドから参照できる
        val ctx = c
        val ctx2 = ctx as Activity
        val bp = b

        override fun onClick(v: View?) {
            // ここで使えるのはタップされたView（１行レイアウト、one_brew_card）

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

