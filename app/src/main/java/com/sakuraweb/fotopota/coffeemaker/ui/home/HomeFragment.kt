package com.sakuraweb.fotopota.coffeemaker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sakuraweb.fotopota.coffeemaker.BrewData
import com.sakuraweb.fotopota.coffeemaker.BrewRecyclerViewAdapter
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewConfig
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {

    // とりあえず、Realmのインスタンスを作る
    private lateinit var realm: Realm

    // アダプタのインスタンス
    private lateinit var adapter: BrewRecyclerViewAdapter

    // レイアウトマネージャーのインスタンス
    private lateinit var layoutManager: RecyclerView.LayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View? {

/*      これ、何やってるんだか分からんので消す！
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer { textView.text = it })
*/
        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        root.button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.navigation_dashboard, null))


        // ここまでが、よくわからん下部関係の動き？ AppBarも？
        // ここから下がリスト表示（RecyclerView）

        // realmのインスタンスを作る
        Realm.setDefaultConfiguration(brewConfig)
        realm = Realm.getDefaultInstance()

        // 追加ボタン（fab）のリスナを設定する（EditActivity画面を呼び出す）
        // 第2引数が投げる先のActivity。KOTLINじゃなくJAVAクラスで渡すため、::class.javaにする

//        brewFAB.setOnClickListener {
//            val intent = Intent(this, CourseEditActivity::class.java)
//            startActivity(intent)
//        }

        return root
    }






    // いよいよここでリスト表示
    // RecyclerViewerのレイアウトマネージャーとアダプターを設定してあげれば、あとは自動
    override fun onStart() {
        super.onStart()

        // 全部のコースデータをrealmResults配列に読み込む
        val realmResults = realm.where(BrewData::class.java).findAll().sort("id", Sort.DESCENDING)

        // 1行のViewを表示するレイアウトマネージャーを設定する
        // LinearLayout、GridLayout、独自も選べるが無難にLinearLayoutManagerにする
        layoutManager = LinearLayoutManager(activity)
        brewRecycleView.layoutManager = layoutManager

        // アダプターを設定する
        adapter = BrewRecyclerViewAdapter(realmResults)
        brewRecycleView.adapter = this.adapter
    }

    // 終了処理
    // たぶん、何も選択しないで「戻る」などしたときは、これだけ呼ばれるのかな？

    override fun onDestroy() {
        super.onDestroy()
        realm.close()

//        Toast.makeText(applicationContext, "リスト画面消します！", Toast.LENGTH_LONG).show()
    }



}