package com.sakuraweb.fotopota.coffeemaker.ui.beans

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import io.realm.RealmResults
import java.text.SimpleDateFormat

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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_beans_card, parent, false)

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

        if( bean!=null ) {
            val df = SimpleDateFormat("yyyy/MM/dd")

            holder.name?.text           = bean.name
            holder.ratingBar?.rating    = bean.rating
            holder.dateText?.text       = df.format(bean.date)
            holder.pastText?.text       = "(１００日経過）"    // TODO:経過日数を作らねば
            holder.gramBar?.setProgress(bean.gram)
            holder.roastBar?.setProgress(bean.roast)
            holder.shop?.text           = bean.shop
            holder.price?.text          = bean.price.toString()
            holder.memo?.text           = bean.memo

            holder.copyBtn?.setOnClickListener {
                val intent = Intent(it.context, BeansEditActivity::class.java)
                intent.putExtra("id", bean.id)
                it.context.startActivity(intent)
            }

            // TODO: 編集は１行タップで。Copyボタンも実装（ダイアログかな？）


            // Brewの編集画面から呼ばれたときは、マメ選択なので、タップで決定とする
            // そのためのリスナを設定。意外と面倒
            if( isCalledFromBrewEdit ) {
                holder.itemView.setOnClickListener {
                    listener.okBtnTapped(bean)
                }
            }

        }

    }

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return beansList.size

    }
}
