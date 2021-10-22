package com.sakuraweb.fotopota.coffeemaker.ui.merge

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.brews.BrewData
import com.sakuraweb.fotopota.coffeemaker.ui.equip.EquipData
import com.sakuraweb.fotopota.coffeemaker.ui.equip.REQUEST_CODE_EQUIP_EDIT
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_beans_merge_list.*
import java.util.Date


class BeansMergeListActivity : AppCompatActivity() {
    private lateinit var realm: Realm                               // とりあえず、Realmのインスタンスを作る
    private lateinit var adapter: BeansMergeRecyclerViewAdapter           // アダプタのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager  // レイアウトマネージャーのインスタンス

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beans_merge_list)

        msgBox2("説明",getString(R.string.mergeDescription) )

        // ーーーーーーーーーー　リスト表示（RecyclerView）　ーーーーーーーーーー
        realm = Realm.getInstance(beansRealmConfig)

        // ーーーーーーーーーー　ツールバーやメニューの装備　ーーーーーーーーーー
        setSupportActionBar(beansMergeToolbar)

        // 戻るボタン。表示だけで、実走はonSupportNavigateUp()で。超面倒くせえ！
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        beansMergeCancelBtn.setOnClickListener { finish() }
        beansMergeMergeBtn.setOnClickListener {

            AlertDialog.Builder(this).apply {
                setTitle("確認")
                setMessage(R.string.mergeConfirm)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            mergeBeans()
                            onStart() // ＤＢ更新後の再表示は、onStartの再呼び出し。本当は違う気もするけど、しゃーない
                        }
                    })
                show()
            }

        }

    }

    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    // 他の関数でデータ変更した場合は、onStartを再呼び出しすればよい（けど大丈夫なのか・・・？）
    override fun onStart() {
        super.onStart()

        // まとめボタンは、最初は不可に
        beansMergeMergeBtn.isEnabled = false

        // ここで初めてデータ読み込み
        val realmResults = realm.where<BeansData>().findAll().sort("id", Sort.ASCENDING )

        layoutManager = LinearLayoutManager(this)
        beansMergeRycycler.layoutManager = layoutManager

        // アダプターを設定する
        adapter = BeansMergeRecyclerViewAdapter(realmResults, beansMergeMergeBtn )
        beansMergeRycycler.adapter = this.adapter
    }


    private fun mergeBeans() {
        val beans = realm.where<BeansData>().findAll().sort("id", Sort.ASCENDING )
        var firstDate   = "2099/1/1".toDate("yyyy/MM/dd")
        var repeatDate  = "1990/1/1".toDate("yyyy/MM/dd")
        var recentDate         = "1990/1/1".toDate("yyyy/MM/dd")
        var selectedBeans = arrayOf<Long>()
        var newName:String=""
        var newID: Long = 0

        realm.executeTransaction {
            // whereで最後尾を探し、そこに追記
            val maxID = realm.where<BeansData>().max("id")
            newID = (maxID?.toLong() ?: 0L) + 1L
            val newBean = realm.createObject<BeansData>(newID)

            newBean.name=""
            newBean.date = "2099/1/1".toDate("yyyy/MM/dd")
            newBean.repeatDate = "1999/1/1".toDate("yyyy/MM/dd")
            newBean.recent = "1999/1/1".toDate("yyyy/MM/dd")
        }

        for(b in adapter.getSelectedItemPositions() ) {
            if( b!=null )  selectedBeans += beans[b]!!.id.toLong()
        }


        realm.executeTransaction {
            val newBean = realm.where<BeansData>().equalTo("id", newID).findFirst()

            for (b in selectedBeans ) {
                var oldBean = realm.where<BeansData>().equalTo("id", b).findFirst()

                if( oldBean!=null ) {

                    // ほとんどの項目は上書きしちゃう
                    if( newBean!=null ) {
                        newBean.name = oldBean.name
                        newBean.roast = oldBean.roast
                        newBean.price = oldBean.price
                        newBean.shop = oldBean.shop
                        newBean.gram = oldBean.gram
                        newBean.process = oldBean.process
                        // メモは全部足しこむ
                        if (oldBean.memo != "") newBean.memo = newBean.memo + oldBean.memo + "\n"

                        // 購入回数・飲んだ回数は足し算
                        newBean.repeat = newBean.repeat + oldBean.repeat
                        newBean.count = newBean.count + oldBean.count

                        // Ratingは再計算のような気もするが
                        newBean.rating = oldBean.rating

                        // 初回購入
                        // 新豆データの方が、旧データより新しい場合、書き換える
                        if (firstDate.after(oldBean.date)) {
                            firstDate = oldBean.date
                        }

                        // 最新購入
                        // 新豆データの方が、旧データより古い場合、書き換える
                        if (repeatDate.before(oldBean.repeatDate)) {
                            repeatDate = oldBean.repeatDate!!
                        }

                        if (recentDate.before(oldBean.recent)) {
                            recentDate = oldBean.recent
                        }
                    }
                }
            } // for

            if( newBean!=null ) {
                newName = "【まとめ】"+newBean.name
                newBean.name = newName
                newBean.date = firstDate
                newBean.repeatDate = repeatDate
                newBean.recent = recentDate
                newBean.repeat = newBean.repeat - 1
            }
        }

        // 古い方のデータを消す！
        // 元の参照を変える必要あるけど・・・
        for (b in selectedBeans) {
            val oldBean = realm.where<BeansData>().equalTo("id", b).findFirst()

            // 古い豆のBREW内の参照を変更
            val brewRealm = Realm.getInstance(brewRealmConfig)
            val brews = brewRealm.where<BrewData>().equalTo("beansID", oldBean?.id).findAll()

            if (brews.size > 0) {
                brewRealm.executeTransaction {
                    for (brew in brews) {
                        brew.beansID = newID
                    }
                }
            }
            brewRealm.close()

            // いよいよ削除！
            realm.executeTransaction {
                realm.where<BeansData>().equalTo("id", b)?.findFirst()?.deleteFromRealm()
            }
        }
        msgBox2("おまとめ作業完了",getString(R.string.mergeComplete).format(selectedBeans.size, newName))
    }


    // ツールバーの「戻る」ボタン
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }



    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    fun msgBox2(title: String, txt: String) {
        val d = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(txt)
            .setCancelable(true)
            .setPositiveButton("OK", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) { }
            })
            .create()

        d.show()
    }

    fun msgBox3(context: Context, title: String, txt: String) {
        AlertDialog.Builder(context).apply {
            setTitle(title)
            setMessage(txt)
            setCancelable(true)
            setPositiveButton("OK", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {}
            })
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when( requestCode ) {
            REQUEST_CODE_EQUIP_EDIT -> {
                when (resultCode) {
                    RESULT_TO_HOME -> {
                        val intent = Intent()
                        setResult(RESULT_TO_HOME, intent)
                        finish()
                    }
                }
            }
        }
    }

}

