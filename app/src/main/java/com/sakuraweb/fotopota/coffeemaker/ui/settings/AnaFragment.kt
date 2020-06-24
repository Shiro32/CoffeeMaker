package com.sakuraweb.fotopota.coffeemaker.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.sakuraweb.fotopota.coffeemaker.R
import kotlinx.android.synthetic.main.activity_main.*

class AnaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_ana, container, false)


        // ツールバーやメニューの装備（ホームなのでメニュー無いけど）
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.title = getString(R.string.title_analyze)
        ac.supportActionBar?.show()

        // メニュー構築（実装はonCreateOptionsMenu内で）
        setHasOptionsMenu(true)

                Log.d("SHIRO", "settings / onCreateView")
        return root
    }

    // オプションメニュー設置
    // Fragment内からActivity側のToolbarに無理矢理設置する
    // 本来は逆だけど、こうすればFragmentごとに違うメニューが作れる
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater?.inflate(R.menu.menu_opt_menu_1, menu)

    }

    // オプションメニュー対応
    // あまり項目がないけど（ホームのみ？）
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId ) {
            R.id.optMenu1ItemHome -> {
                activity?.nav_view?.selectedItemId = R.id.navigation_home
            }
        }

        return super.onOptionsItemSelected(item)
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("SHIRO", "settings / onDestroy")
    }
}
