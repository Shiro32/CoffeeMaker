package com.sakuraweb.fotopota.coffeemaker.ui.merge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import io.realm.RealmResults
import java.text.SimpleDateFormat
import java.util.*

class BeansMergeRecyclerViewAdapter(beansMergeRealm: RealmResults<BeansData>, matomeBtn:View ) :
    RecyclerView.Adapter<BeansMergeViewHolder>() {

    private val beansList: RealmResults<BeansData> = beansMergeRealm
    private val selectedItemPositions = mutableSetOf<Int>()
    private val matomeBtn = matomeBtn

    // 新しく1行分のViewをXMLから生成し、1行分のViewHolderを生成してViewをセットする
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeansMergeViewHolder {
        // 新しいView（1行）を生成する　レイアウト画面で作った、one_BeansMerge_card_home（1行）
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_beans_flat, parent, false)

        // 1行ビューをもとに、ViewHolder（←自分で作ったヤツ）インスタンスを生成
        // 今作ったView（LinearLayout）を渡す
        return BeansMergeViewHolder(view)
    }

    //選択済みのアイテムのPositionが記録されたSetを外部に渡す
    fun getSelectedItemPositions() = selectedItemPositions.toSet()

    //指定されたPositionのアイテムが選択済みか確認する
    private fun isSelectedItem(position: Int): Boolean = (selectedItemPositions.contains(position))

    //選択モードでないときは選択モードに入る
    private fun addSelectedItem(position: Int) {
        selectedItemPositions.add(position)

        // できれば選択ボタンをactiveにしたいが
        if( selectedItemPositions.size >= 2 ) matomeBtn.isEnabled = true
    }

    //選択モードで最後の一個が選択解除された場合は、選択モードをOFFにする
    private fun removeSelectedItem(position: Int) {
        selectedItemPositions.remove(position)

        if( selectedItemPositions.size < 2 ) matomeBtn.isEnabled = false
    }

    // ViewHolderの表示内容を更新する。RecyclerViewの心臓部
    // 渡されたビューホルダにデータを書き込む
    // RealmDB内のデータから、具体的なビューの表示文字列を生成してあげる
//    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: BeansMergeViewHolder, position: Int) {
        val bean = beansList[position]

        if (isSelectedItem(position))
            holder.itemView.setBackgroundResource(R.color.cardColor2)
        else
            holder.itemView.setBackgroundResource(R.color.cardColorWhite)


        if (bean != null) {

            // 1レコードの各要素を代入する
            // 最新購入日
            val df = SimpleDateFormat("yyyy/MM/dd")
            holder.dateText?.text = df.format(bean.repeatDate)
            holder.pastText?.text =
                "(%s日経過)".format((Date().time - bean.repeatDate?.time as Long) / (1000 * 60 * 60 * 24))

            holder.name?.text = bean.name
            holder.ratingBar?.rating = bean.rating
            holder.ratingText?.text = "%.1f".format(bean.rating)
            holder.roastBar?.setProgress(bean.roast)
            holder.shop?.text = bean.shop
            holder.price?.text = "（%d円)".format(bean.price)
            holder.repeat?.text = "%d回".format(bean.repeat)
            holder.count?.text = "%d回".format(bean.count)

            if (bean.memo != "") {
                holder.memo?.text = bean.memo
                holder.memo?.visibility = View.VISIBLE
                holder.memoLabel?.visibility = View.VISIBLE
            } else {
                holder.memo?.visibility = View.GONE
                holder.memoLabel?.visibility = View.GONE
            }

            // 行タップした際のアクションをリスナで登録
            holder.itemView.setOnClickListener {
                //選択モードでないときは普通のクリックとして扱う
                if ( isSelectedItem(position) )
                    removeSelectedItem(position)
                else
                    addSelectedItem(position)

                onBindViewHolder(holder, position)
                }
            }
        }

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return beansList.size

    }
}

/*

class SelectableAdapterWithKotlin(private val context: Context, private val itemDataList: List<String>, private val isAlwaysSelectable: Boolean): RecyclerView.Adapter<SelectableHolder>(){

    //isAlwaysSelectableがONのときは最初から選択モード
    private var isSelectableMode = isAlwaysSelectable
    private val selectedItemPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableHolder {
        return SelectableHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_multi_url_card, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SelectableHolder, position: Int) {
        with(holder) {
            mainTextView.text = itemDataList[position]
            subTextView.text = "position $position"

            //このアイテムが選択済みの場合はチェックを入れる（✓のイメージを表示する）
            checkLayout.visibility = if (isSelectedItem(position)) View.VISIBLE else View.GONE

            cardView.setOnClickListener {

                //選択モードでないときは普通のクリックとして扱う
                if (!isSelectableMode && !isAlwaysSelectable) Toast.makeText(context, "Normal click", Toast.LENGTH_SHORT).show()
                else {
                    if (isSelectedItem(position)) removeSelectedItem(position)
                    else addSelectedItem(position)

                    onBindViewHolder(holder, position)
                }
            }
            cardView.setOnLongClickListener {

                //ロングクリックで選択モードに入る
                if (isSelectedItem(position)) removeSelectedItem(position)
                else addSelectedItem(position)

                onBindViewHolder(holder, position)

                return@setOnLongClickListener true
            }
        }
    }

    override fun getItemCount(): Int = itemDataList.size

    //選択済みのアイテムのPositionが記録されたSetを外部に渡す
    fun getSelectedItemPositions() = selectedItemPositions.toSet()

    //指定されたPositionのアイテムが選択済みか確認する
    private fun isSelectedItem(position: Int): Boolean = (selectedItemPositions.contains(position))

    //選択モードでないときは選択モードに入る
    private fun addSelectedItem(position: Int){
        if(selectedItemPositions.isEmpty() && !isAlwaysSelectable){
            isSelectableMode = true
            Toast.makeText(context, "Selectable Mode ON", Toast.LENGTH_SHORT).show()
        }
        selectedItemPositions.add(position)
    }

    //選択モードで最後の一個が選択解除された場合は、選択モードをOFFにする
    private fun removeSelectedItem(position: Int){
        selectedItemPositions.remove(position)
        if(selectedItemPositions.isEmpty() && !isAlwaysSelectable){
            isSelectableMode = false
            Toast.makeText(context, "Selectable Mode OFF", Toast.LENGTH_SHORT).show()
        }
    }
}*/
