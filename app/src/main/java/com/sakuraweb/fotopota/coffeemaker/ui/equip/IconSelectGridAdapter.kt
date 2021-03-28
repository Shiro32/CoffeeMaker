package com.sakuraweb.fotopota.coffeemaker.ui.equip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.sakuraweb.fotopota.coffeemaker.R
import com.sakuraweb.fotopota.coffeemaker.brewMethodsImages
import com.sakuraweb.fotopota.coffeemaker.ui.beans.beansListStyle
import kotlinx.android.synthetic.main.one_equip_card.view.*
import kotlinx.android.synthetic.main.one_equip_icon.view.*
import org.w3c.dom.Text

class IconSelectGridAdapter( ) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.one_equip_icon, parent, false)
        view.oneEquipIconImage.setImageDrawable(brewMethodsImages.getDrawable(position))
        return view
    }
    override fun getCount(): Int {
        return brewMethodsImages.length()
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }
}