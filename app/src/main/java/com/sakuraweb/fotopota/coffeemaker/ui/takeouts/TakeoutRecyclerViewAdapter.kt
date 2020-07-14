package com.sakuraweb.fotopota.coffeemaker.ui.takeouts

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.isCalledFromBrewEditToTakeout
import io.realm.RealmResults

const val REQUEST_CODE_SHOW_TAKEOUT_DETAILS = 100

// RecyclerViewをタップで決定するためのリスナ（ハンドラー）
// よくわからんけど、抽象インターフェースで
interface SetTakeoutListener {
    fun okBtnTapped( ret: TakeoutData? )
}


class TakeoutRecyclerViewAdapter(takeoutRealm: RealmResults<TakeoutData>, private val listener: SetTakeoutListener) :
    RecyclerView.Adapter<TakeoutViewHolder>() {

    private val takeoutList: RealmResults<TakeoutData> = takeoutRealm

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TakeoutViewHolder {
        // 新しいView（1行）を生成する　レイアウト画面で作った、one_takeout_card_home（1行）
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_takeout_card, parent, false)

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        val holder = TakeoutViewHolder(view)
        return holder
    }


    // ViewHolderの表示内容を更新する。RecyclerViewの心臓部
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
    override fun onBindViewHolder(holder: TakeoutViewHolder, position: Int) {
        val bean = takeoutList[position]

        if (bean != null) {

            holder.name?.text = bean.name
            holder.ratingBar?.rating = bean.rating
            holder.chain?.text = bean.chain
//            holder.shop?.text = bean.shop
            holder.price?.text = bean.price.toString()+"円"
            holder.size?.text = bean.size
            holder.memo?.text = bean.memo
            holder.count?.text = findTakeoutUseCount( bean ).toString()

            if( bean.memo!="" ) {
                holder.memo?.text = bean.memo
                holder.memo?.visibility = View.VISIBLE
                holder.memoLabel?.visibility = View.VISIBLE
            } else {
                holder.memo?.visibility = View.GONE
                holder.memoLabel?.visibility = View.GONE
            }
/*
            holder.copyBtn?.setOnClickListener {
                val intent = Intent(it.context, TakeoutEditActivity::class.java)
                intent.putExtra("id", bean.id)
                it.context.startActivity(intent)
            }
*/

            // 行タップした際のアクションをリスナで登録
            // ボタンは廃止しました
            if (isCalledFromBrewEditToTakeout) {
                // Brew-Editから呼び出された場合は、豆を選択なのでタップで決定とする
                holder.itemView.setOnClickListener {
                    listener.okBtnTapped(bean)
                }
            } else {
                // Naviから呼び出された場合は、豆を編集する
                holder.itemView.setOnClickListener {
                    val intent = Intent(it.context, TakeoutDetailsActivity::class.java)
                    intent.putExtra("id", bean.id)
                    val it2 = it.context as Activity
                    it2.startActivityForResult(intent, REQUEST_CODE_SHOW_TAKEOUT_DETAILS)
//                    it.context.startActivity(intent)
                }
            }
        }
    } // override

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return takeoutList.size

    }
}
