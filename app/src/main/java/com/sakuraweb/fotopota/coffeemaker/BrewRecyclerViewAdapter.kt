package com.sakuraweb.fotopota.coffeemaker

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.realm.RealmResults
import java.text.SimpleDateFormat

/*
アダプタクラスを作る
アダプタとは、ビューに表示するすべてのリストデータを管理し、ビューの各行に当てはめていくオブジェクト

〇表示用ViewHolderインスタンスを生成（onCreateViewHolder）
　レイアウトに従って1行分のViewを生成
　ViewHolderを生成して、上記Viewをセットして完了

〇表示内容の更新（onBindViewHolder）　
　ViewHolderのインスタンスを更新してやる（holder.name = "hoge"）

 ListViewでは、ArrayAdapterやSimpleAdapterなどの既存アダプタが利用できた（データ種別による）
 RecyclerViewでは、アダプがが無いので自分で作ってあげる必要がある
 でたらめなアダプタではいけないので、RecyclerView.adapterでインターフェースを定義されているので、
 必要な3メソッドを実装してやり、のちにメインプログラムでインスタンスを生成、RecycleViewのadapterにセットする
 ３つのメソッドは（onCreateViewHolder, getItemCount, onBindViewHolder）

 なんでも格納できるRealmResultsクラスに対して、BloodPressクラスに限定している
 結局、「RealmResults<BloodPress>」型になる。もう、何が何だか。

 RecyclerViewのサブクラス、Adapterクラス（型引数でViewHolder）を継承する
 */

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
    override fun onBindViewHolder(holder: BrewViewHolder, position: Int) {
        val bp = brews[position]

        if( bp!=null ) {

            val df = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

            holder.dateText?.text       = df.format(bp.date)
            holder.ratingBar?.progress  = bp.rating
            holder.methodText?.text     = bp.methodID.toString()
            holder.beansKindText?.text  = bp.beansID.toString()
            holder.beansPassText?.text  = bp.beansPass.toString()
            holder.beansGrindBar?.setProgress(bp.beansGrind.toFloat())
            holder.beansUseBar?.setProgress(bp.beansUse.toFloat())
            holder.cupsBar?.setProgress(bp.cups.toFloat())
            holder.tempBar?.setProgress(bp.temp.toFloat())
            holder.steamBar?.setProgress(bp.steam.toFloat())
            holder.memoText?.text           = bp.memo

//            val texts = arrayOf<String>("粗挽","中挽","標準","細挽","極細挽")
//            holder.sampleBar?.customTickTexts(texts)
//            holder.sampleBar?.setIndicatorTextFormat("\${TICK_TEXT}")

//            holder.image?.setImageURI(Uri.parse(bp.imageURI))

//            holder.editBtn?.setOnClickListener {
//                val intent = Intent(it.context, CourseEditActivity::class.java)
//                intent.putExtra("id", bp.id)
//                it.context.startActivity(intent)
//            }

        }
    }

    // アダプターの必須昨日の、サイズを返すメソッド
    override fun getItemCount(): Int {
        return brews.size
    }

}