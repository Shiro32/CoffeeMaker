package com.sakuraweb.fotopota.coffeemaker.ui.config

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import androidx.preference.PreferenceFragmentCompat
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.sakuraweb.fotopota.coffeemaker.*
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BEANS_DATA_VERSION
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansData
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansDataMigration
import com.sakuraweb.fotopota.coffeemaker.ui.beans.BeansDataModule
import com.sakuraweb.fotopota.coffeemaker.ui.brews.*
import com.sakuraweb.fotopota.coffeemaker.ui.equip.EquipData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TAKEOUT_DATA_VERSION
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutData
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutDataMigration
import com.sakuraweb.fotopota.coffeemaker.ui.takeouts.TakeoutDataModule
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class ConfigFragment : PreferenceFragmentCompat() {

    override fun onStart() {
        super.onStart()

        // ツールバー設定（何もないけど）
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.show()
        ac.supportActionBar?.title = "いろいろ設定"

        ac.sortSpn.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

/*
        // ツールバー設定（何もないけど）
        val ac = activity as AppCompatActivity
        ac.supportActionBar?.show()
        ac.supportActionBar?.title = "いろいろ設定"

// 試しにオフ
//        ac.sortSpn.visibility = View.INVISIBLE
*/

        // プリファレンスをXMLからインフレートする
        addPreferencesFromResource(R.xml.root_preferences)

        //　各入力項目を数字限定にする。面倒よねぇ・・・。
        findPreference<EditTextPreference>("mill_min")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("mill_max")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("brew_min")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("brew_max")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("steam_min")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("steam_max")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

/*
        findPreference<EditTextPreference>("water_volume_min")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        findPreference<EditTextPreference>("water_volume_max")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
 */

        findPreference<EditTextPreference>("temp_min")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("temp_max")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<EditTextPreference>("beans_buy_max")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        findPreference<Preference>("backup")?.setOnPreferenceClickListener {
            backupData()
            true
        }

        findPreference<Preference>("restore")?.setOnPreferenceClickListener {
            restoreData()
            true
        }

    }

/*    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val a = 1
    }*/

    private fun backupData() {
        var ext = context?.getExternalFilesDir(null ).toString()

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
                            brew_list_backup, bean_list_backup, equip_list_backup, takeout_list_backup ) )
                    }
                })
            show()
        }
    }

    private fun restoreData() {
        brewDataMigrated5to6 = false
        restoreBrewData3()
        restoreBeansData2()
        restoreEquipData2()
        restoreTakeoutData2()
    }

    private fun updateData() {
        if (brewDataMigrated5to6) {
            // 既存BREWで使っている器具のリストを作る
            var realm = Realm.getInstance(brewRealmConfig)
            val brews = realm.where<BrewData>().findAll()
            var methods = Array(11) { 0 }
            for (b in brews) {
                methods[b.methodID] += 1
                realm.executeTransaction {
                    val bb = realm.where<BrewData>().equalTo("id", b.id).findFirst()
                    if (bb != null) bb.equipID = bb.methodID.toLong()
                }
            }
            realm.close()
            // 普通は無いと思うけど、外飲みゼロの場合の対策
            methods[10] += 1

            // できたばかりのEQUIPを全部消して実績だけ入れる
            realm = Realm.getInstance(equipRealmConfig)
            val equips = realm.where<EquipData>().findAll()
            // まず全部消す・・・
            for (e in equips) {
                realm.executeTransaction {
                    realm.where<EquipData>().equalTo("id", e.id)?.findFirst()?.deleteFromRealm()
                }
            }
            // 既存BREWの使用実績を入れる
            for (i in 0..10) {
                if (methods[i] > 0) {
                    realm.executeTransaction {
                        val equip = realm.createObject<EquipData>(i)
//TODO: ここで、icon番号を変換することも可能
                        equip.icon = if (i == 0) 1 else i
                        equip.name = brewMethods[i] // CRはどうすんだっけ・・・？
                        equip.date = (if (i.toLong() == EQUIP_SHOP) "2050/12/31" else "2020/01/01").toDate("yyyy/MM/dd")
                    }
                }
            }
            realm.close()
        }
    }

    private fun restoreBrewData3() {
        val src = File(context?.getExternalFilesDir(null).toString()+"/"+brew_list_backup )
        val dst = File(brewRealmConfig.path)

        if ( src.exists() ) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_brew)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {

                            Realm.deleteRealm(brewRealmConfig)
                            src.copyTo( dst, overwrite = true)
                            val dummy = Realm.getInstance(brewRealmConfig)
                            dummy.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_brew) )

                            updateData()
                        }
                    })
                show()
            }
        } // BREWデータのレストア終了
        else msgBox( getString(R.string.dialog_backup_nothing_title), String.format( getString(R.string.dialog_backup_brew_nothing_message), src.path, brew_list_backup ) )
    }


    private fun restoreBeansData2() {
        val src = File( context?.getExternalFilesDir(null).toString()+"/"+ bean_list_backup )
        val dst = File(beansRealmConfig.path)

        if (src.exists()) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_beans)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            Realm.deleteRealm(beansRealmConfig)
                            src.copyTo( dst, overwrite = true )
                            val dummy = Realm.getInstance( beansRealmConfig )
                            dummy.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_beans) )
                        }
                    })
                show()
            }
        } // BEANSデータのレストア終了
        else msgBox( getString(R.string.dialog_backup_nothing_title), String.format( getString(R.string.dialog_backup_beans_nothing_message), src.path, bean_list_backup ) )
    }

    private fun restoreTakeoutData2() {
        val src = File(context?.getExternalFilesDir(null).toString()+"/"+ takeout_list_backup )
        val dst = File( takeoutRealmConfig.path )

        if (src.exists()) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_takeout)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            Realm.deleteRealm( takeoutRealmConfig )
                            src.copyTo( dst, overwrite = true )
                            val dummy = Realm.getInstance( takeoutRealmConfig )
                            dummy.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_takeout) )
                        }
                    })
                show()
            }
        } // TAKEOUTデータのレストア終了
        else msgBox( getString(R.string.dialog_backup_nothing_title), String.format( getString(R.string.dialog_backup_takeout_nothing_message), src.path, takeout_list_backup ) )
    }

    private fun restoreEquipData2() {
        val src = File(context?.getExternalFilesDir(null).toString()+"/"+ equip_list_backup )
        val dst = File(equipRealmConfig.path )

        if (src.exists()) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_equip)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            Realm.deleteRealm( equipRealmConfig )
                            src.copyTo( dst, overwrite = true )
                            val dummy = Realm.getInstance( equipRealmConfig )
                            dummy.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_equip) )
                        }
                    })
                show()
            }
        } // TAKEOUTデータのレストア終了
        else msgBox( getString(R.string.dialog_backup_nothing_title), String.format( getString(R.string.dialog_backup_equip_nothing_message), src.path, equip_list_backup ) )
    }

    private fun restoreBrewData() {
        val ext = context?.getExternalFilesDir(null).toString()
        val src = File("$ext/$brew_list_backup")

        if (src.exists()) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_brew)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {

                            val srcRealmConfig = RealmConfiguration.Builder()
                                .name(brew_list_backup)
                                .directory(File(ext))
                                .modules(BrewDataModule())
                                .schemaVersion(BREW_DATA_VERSION)
                                .migration(BrewDataMigration())
                                .build()
                            val srcRealm = Realm.getInstance(srcRealmConfig)
                            val dstRealm = Realm.getInstance(brewRealmConfig)

                            dstRealm.beginTransaction()
                            dstRealm.deleteAll()
                            val temp = srcRealm.where<BrewData>().findAll()

                            for (t in temp) {
                                var d = dstRealm.createObject<BrewData>(t.id)
                                d.date = t.date
                                d.rating = t.rating
                                d.methodID = t.methodID
                                d.equipID = t.equipID
                                d.place = t.place
                                d.shop = t.shop
                                d.beansID = t.beansID
                                d.beansPast = t.beansPast
                                d.beansGrindSw = t.beansGrindSw
                                d.beansGrind = t.beansGrind
                                d.beansGrind2 = t.beansGrind2
                                d.beansUse = t.beansUse
                                d.cups = t.cups
                                d.cupsDrunk = t.cupsDrunk
                                d.temp = t.temp
                                d.steam = t.steam
                                d.imageURI = t.imageURI
                                d.memo = t.memo
                                d.takeoutID = t.takeoutID
                                d.milk = t.milk
                                d.sugar = t.sugar
                                d.iceHotSw = t.iceHotSw
                                d.price = t.price
                            }
                            dstRealm.commitTransaction()
                            dstRealm.close()
                            srcRealm.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_brew) )
                        }
                    })
                show()
            }
        } // BREWデータのレストア終了
        else {
            msgBox(
                getString(R.string.dialog_backup_nothing_title),
                String.format(
                    getString(R.string.dialog_backup_brew_nothing_message),
                    ext,
                    brew_list_backup
                )
            )
        }
    }

    private fun restoreBeansData() {
        val ext = context?.getExternalFilesDir(null).toString()
        val src = File("$ext/$bean_list_backup")

        if (src.exists()) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_beans)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {

                            val srcRealmConfig = RealmConfiguration.Builder()
                                .name(bean_list_backup)
                                .directory(File(ext))
                                .modules(BeansDataModule())
                                .schemaVersion(BEANS_DATA_VERSION)
                                .migration(BeansDataMigration())
                                .build()
                            val srcRealm = Realm.getInstance(srcRealmConfig)
                            val dstRealm = Realm.getInstance(beansRealmConfig)

                            dstRealm.beginTransaction()
                            dstRealm.deleteAll()
                            val temp = srcRealm.where<BeansData>().findAll()

                            for (t in temp) {
                                var d = dstRealm.createObject<BeansData>(t.id)
                                d.name      = t.name
                                d.rating    = t.rating
                                d.date      = t.date
                                d.recent    = t.recent
                                d.gram      = t.gram
                                d.roast     = t.roast
                                d.shop      = t.shop
                                d.price     = t.price
                                d.count     = t.count
                                d.memo      = t.memo
                            }
                            dstRealm.commitTransaction()
                            dstRealm.close()
                            srcRealm.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_beans) )
                        }
                    })
                show()
            }
        } // BEANSデータのレストア終了
        else {
            msgBox(
                getString(R.string.dialog_backup_nothing_title),
                String.format(
                    getString(R.string.dialog_backup_beans_nothing_message),
                    ext,
                    bean_list_backup
                )
            )
        }
    }


    private fun restoreTakeoutData() {
        val ext = context?.getExternalFilesDir(null).toString()
        val src = File("$ext/$takeout_list_backup")

        if (src.exists()) {
            AlertDialog.Builder(context).apply {
                setTitle(R.string.overwrite_confirm_dialog_title)
                setMessage(R.string.overwrite_confirm_dialog_message_takeout)
                setCancelable(true)
                setNegativeButton(R.string.overwrite_confirm_dialog_cancel, null)
                setPositiveButton("OK",
                    object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {

                            val srcRealmConfig = RealmConfiguration.Builder()
                                .name(takeout_list_backup)
                                .directory(File(ext))
                                .modules(TakeoutDataModule())
                                .schemaVersion(TAKEOUT_DATA_VERSION)
                                .migration(TakeoutDataMigration())
                                .build()
                            val srcRealm = Realm.getInstance(srcRealmConfig)
                            val dstRealm = Realm.getInstance(takeoutRealmConfig)

                            dstRealm.beginTransaction()
                            dstRealm.deleteAll()
                            val temp = srcRealm.where<TakeoutData>().findAll()

                            for (t in temp) {
                                var d = dstRealm.createObject<TakeoutData>(t.id)
                                d.name      = t.name
                                d.rating    = t.rating
                                d.chain     = t.chain
                                d.shop      = t.shop
                                d.price     = t.price
                                d.count     = t.count
                                d.recent    = t.recent
                                d.first     = t.first
                                d.size      = t.size
                                d.memo      = t.memo
                            }
                            dstRealm.commitTransaction()
                            dstRealm.close()
                            srcRealm.close()
                            blackToast( context, getString(R.string.overwrite_confirm_dialog_done_takeout) )
                        }
                    })
                show()
            }
        } // TAKEOUTデータのレストア終了
        else {
            msgBox(
                getString(R.string.dialog_backup_nothing_title),
                String.format(
                    getString(R.string.dialog_backup_takeout_nothing_message),
                    ext,
                    takeout_list_backup
                )
            )
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