package com.stark.memorygame.view.common

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class DialogHelper {

    data class Dialog(
        val context: Context,
        val title: String? = null,
        val message: String? = null,
        val view: View? = null,
        val isCancellable: Boolean? = null,
        val positionBtnText: String? = null,
        val negativeBtnText: String? = null,
        val onPositiveClick: () -> Unit,
        val onNegativeClick: () -> Unit = {}
    ) {
        constructor(
            context: Context,
            title: String,
            view: View,
            positionBtnText: String,
            negativeBtnText: String? = null,
            onPositiveClick: () -> Unit,
            onNegativeClick: () -> Unit = {}
        ) : this(
            context = context,
            title = title,
            positionBtnText = positionBtnText,
            negativeBtnText = negativeBtnText,
            onPositiveClick = onPositiveClick,
            onNegativeClick = onNegativeClick
        )
    }

    class AlertDialogBuilder(private val dialog: Dialog) {

        fun build(): AlertDialog {
            val alertDialog = dialog.context.let { context ->
                val builder = AlertDialog.Builder(context)
                builder.apply {
                    dialog.title?.let { setTitle(it) }
                    dialog.message?.let { setMessage(it) }
                    dialog.view?.let { setView(it) }
                    setCancelable(dialog.isCancellable ?: true)
                    setPositiveButton(dialog.positionBtnText) { _, _ ->
                        dialog.onPositiveClick.invoke()
                    }

                    setPositiveButton(dialog.positionBtnText) { _, _ ->
                        dialog.onPositiveClick.invoke()
                    }

                    dialog.onNegativeClick?.let {
                        setNegativeButton(dialog.negativeBtnText) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }
                }
                builder.create()
            }
            alertDialog.show()
            return alertDialog
        }
    }

}