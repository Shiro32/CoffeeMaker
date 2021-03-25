package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.realm.RealmResults
import java.text.SimpleDateFormat

const val REQUEST_CODE_EQUIP_EDIT = 200


//TODO: RecylcerViewをMAPを使って高速化しよう。あかんよ。

// RecyclerViewをタップで決定するためのリスナ（ハンドラー）
// よくわからんけど、抽象インターフェースで
interface SetEquipListener {
    fun okBtnTapped( ret: EquipData? )
}

class EquipRecyclerViewAdapter(equipRealm: RealmResults<EquipData>, private val listener: SetEquipListener) :
    RecyclerView.Adapter<EquipViewHolder>() {

    private val equipList: RealmResults<EquipData> = equipRealm

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipViewHolder {
        // 新しいView（1行）を生成する　レイアウト画面で作った、one_equip_card_home（1行）
        val view = LayoutInflater.from(parent.context).inflate(equipListLayout, parent, false)

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        val holder = EquipViewHolder(view)
        return holder
    }


    // ViewHolderの表示内容を更新する。RecyclerViewの心臓部
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
    override fun onBindViewHolder(holder: EquipViewHolder, position: Int) {
        val equip = equipList[position]

        if (equip != null) {
            val df = SimpleDateFormat("yyyy/MM/dd")

            holder.dateText?.text = df.format(equip.date)
            holder.name?.text = equip.name
            holder.ratingBar?.rating = equip.rating
            holder.shop?.text = equip.shop
            holder.price?.text = equip.price.toString()+"円"
            holder.maker?.text = equip.maker

            holder.memo?.text = equip.memo

            if( equip.memo!="" ) {
                holder.memo?.text = equip.memo
                holder.memo?.visibility = View.VISIBLE
                holder.memoLabel?.visibility = View.VISIBLE
            } else {
                holder.memo?.visibility = View.GONE
                holder.memoLabel?.visibility = View.GONE
            }

            // 行タップした際のアクションをリスナで登録
            // ボタンは廃止しました
            if (isCalledFromBrewEditToEquip) {
                // Brew-Editから呼び出された場合は、器具選定なのでタップで決定とする
                holder.itemView.setOnClickListener {
                    listener.okBtnTapped(equip)
                }
            } else {
                // Configから呼び出された場合は、豆を編集する
                holder.itemView.setOnClickListener {
                    it.context.startActivity(Intent(it.context, EquipEditActivity::class.java). apply {
                        putExtra( "id", equip.id )
                        putExtra( "mode", EQUIP_EDIT_MODE_EDIT)
                    })
                }
            }
        }
    } // override

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return equipList.size

    }
}
