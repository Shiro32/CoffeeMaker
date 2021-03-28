package com.sakuraweb.fotopota.coffeemaker.ui.config

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import com.sakuraweb.fotopota.coffeemaker.*
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class ConfigFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        // ツールバー設定（何もないけど）
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.show()
//        ac.supportActionBar?.title = getString(R.string.titleBeansListFromBtnv)
        ac.supportActionBar?.title = "いろいろ設定"
        ac.sortSpn.visibility = View.INVISIBLE

        addPreferencesFromResource(R.xml.root_preferences)

        findPreference<Preference>("backup")?.setOnPreferenceClickListener {
            backupRunAndMenuData()
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val a = 1
    }

    private fun backupRunAndMenuData() {
        var ext = context?.getExternalFilesDir(null).toString()

        // 一応、上書き確認を出す
        AlertDialog.Builder(context).apply {
            setTitle(R.string.overwrite_confirm_dialog_title)
            setMessage(R.string.overwrite_confirm_backup_dialog_message)
            setCancelable(true)
            setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
            setPositiveButton("OK",
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        // BREWデータの書き込み
                        var realm = Realm.getInstance(brewRealmConfig)
                        var src = File(realm.path)
                        var dst = File("$ext/$brew_list_backup")
                        src.copyTo(dst, overwrite = true)
                        realm.close()

                        // BEANの書き込み
                        realm = Realm.getInstance(beansRealmConfig)
                        src = File(realm.path)
                        dst = File("$ext/$bean_list_backup")
                        src.copyTo(dst, overwrite = true)
                        realm.close()

                        // BEANの書き込み
                        realm = Realm.getInstance(takeoutRealmConfig)
                        src = File(realm.path)
                        dst = File("$ext/$takeout_list_backup")
                        src.copyTo(dst, overwrite = true)
                        realm.close()

                        // EQUIPの書き込み
                        realm = Realm.getInstance(equipRealmConfig)
                        src = File(realm.path)
                        dst = File("$ext/$equip_list_backup")
                        src.copyTo(dst, overwrite = true)
                        realm.close()

                        // 終わりましたよ、というダイアログ
                        msgBox(getString(R.string.dialog_backup_done_title), String.format(getString(R.string.dialog_backup_done_message),ext,
                            brew_list_backup, bean_list_backup, takeout_list_backup, equip_list_backup ) )
                    }
                })
            show()
        }
    }

    fun msgBox(title: String, txt: String) {
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
}