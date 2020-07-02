package com.sakuraweb.fotopota.coffeemaker.ui.beans.select

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter

import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.beansBlend
import com.sakuraweb.fotopota.coffeemaker.beansPack
import com.sakuraweb.fotopota.coffeemaker.beansSpecial
import kotlinx.android.synthetic.main.fragment_beans_select.view.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BeansSelectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BeansSelectFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = "hoge1"
    private var param2: String? = "hoge2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_beans_select, container, false)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
//            root.testText.setText(param1)
        }

        when(param1) {
            "SPECIAL" -> root.beansListView.adapter = ArrayAdapter(root.context, android.R.layout.simple_list_item_1, beansSpecial)
            "BLEND" ->   root.beansListView.adapter = ArrayAdapter(root.context, android.R.layout.simple_list_item_1, beansBlend)
            "PACK" -> {
                root.beansListView.adapter = ArrayAdapter(root.context, android.R.layout.simple_list_item_1, beansPack)

            }
        }
        root.beansListView.onItemClickListener = ListItemClickListener()
        return root
    }

    private inner class ListItemClickListener : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val item = parent?.getItemAtPosition(position) as String
            val intent = Intent()
            intent.putExtra("name", item)
            activity?.setResult(Activity.RESULT_OK, intent)
            activity?.finish()
        }
    }


    override fun onStart() {
        super.onStart()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BeansSelectFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BeansSelectFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}