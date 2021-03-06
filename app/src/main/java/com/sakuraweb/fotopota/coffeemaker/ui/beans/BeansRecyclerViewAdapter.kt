package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import io.realm.RealmResults
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_CODE_SHOW_BEANS_DETAILS = 100

// RecyclerViewをタップで決定するためのリスナ（ハンドラー）
// よくわからんけど、抽象インターフェースで
interface SetBeansListener {
    fun okBtnTapped( ret: BeansData? )
}


class BeansRecyclerViewAdapter(beansRealm: RealmResults<BeansData>, private val listener: SetBeansListener) :
    RecyclerView.Adapter<BeansViewHolder>() {

    private val beansList: RealmResults<BeansData> = beansRealm

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeansViewHolder {
        // 新しいView（1行）を生成する　レイアウト画面で作った、one_beans_card（1行）
        val view = LayoutInflater.from(parent.context).inflate(beansListStyle, parent, false)

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        val holder = BeansViewHolder(view)
        return holder
    }


    // ViewHolderの表示内容を更新する。RecyclerViewの心臓部
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
    override fun onBindViewHolder(holder: BeansViewHolder, position: Int) {
        val bean = beansList[position]

        if (bean != null) {

            val df = SimpleDateFormat("yyyy/MM/dd")

            holder.name?.text = bean.name
            holder.ratingBar?.rating = bean.rating
            holder.dateText?.text = df.format(bean.date)
            // 豆の経過日数を計算する（面倒くせぇ・・・）
            holder.pastText?.text = "（"+((Date().time - bean.date?.time as Long)/(1000*60*60*24)).toString()+"日経過）"
            holder.gramBar?.setProgress(bean.gram)
            holder.roastBar?.setProgress(bean.roast)
            holder.shop?.text = bean.shop
            holder.price?.text = "（"+bean.price.toString()+"円)"

            if( bean.memo!="" ) {
                holder.memo?.text = bean.memo
                holder.memo?.visibility = View.VISIBLE
                holder.memoLabel?.visibility = View.VISIBLE
            } else {
                holder.memo?.visibility = View.GONE
                holder.memoLabel?.visibility = View.GONE
            }

            // 行タップした際のアクションをリスナで登録
            // ボタンは廃止しました
            if (isCalledFromBrewEditToBeans) {
                // Brew-Editから呼び出された場合は、豆を選択なのでタップで決定とする
                holder.itemView.setOnClickListener {
                    listener.okBtnTapped(bean)
                }
            } else {
                // Naviから呼び出された場合は、豆を編集する
                holder.itemView.setOnClickListener {
                    val intent = Intent(it.context, BeansDetailsActivity::class.java)
                    intent.putExtra("id", bean.id)
                    val it2 = it.context as Activity
                    it2.startActivityForResult(intent, REQUEST_CODE_SHOW_BEANS_DETAILS)
//                    it.context.startActivity(intent)
                }
            }


        }

    } // override

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return beansList.size

    }
}
