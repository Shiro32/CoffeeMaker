package com.sakuraweb.fotopota.coffeemaker

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

// 削除操作をしたときの、YES/NOダイアログ

/*
class DeleteConfirmDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.deleteConfirmDialogTitle)
        builder.setMessage(R.string.deleteConfirmDialogMessage)
        builder.setPositiveButton(R.string.deleteConfirmDialogOkBtn, DeleteConfirmDialogListener)

        return super.onCreateDialog(savedInstanceState)
    }

    private inner class DeleteConfirmDialogListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface?, which: Int) {
            when(which) {
                // はい
                DialogInterface.BUTTON_POSITIVE -> {

                }
            }
        }
    }

}*/
