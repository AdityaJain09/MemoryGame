package com.stark.memorygame.view.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.stark.memorygame.R
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.view.common.DialogHelper

interface OnBoardSizeSelectListener {
    fun onSelect(size: BoardSize)
}

interface OnDownloadGameListener {
    fun onDownloadGame(gameName: String)
}

class BoardSelectorDialog(
    private val ctx: Context,
    onBoardSizeSelectListener: OnBoardSizeSelectListener?,
    onDownloadGameListener: OnDownloadGameListener?,
    private val currentBoardSize: BoardSize?,
    private val title: String,
    @LayoutRes private val resourceId: Int
) {

    private lateinit var radioBtnGroup: RadioGroup
    private lateinit var titleTv: TextView
    private lateinit var boardSizeSelectListener: OnBoardSizeSelectListener
    private lateinit var downloadGameListener: OnDownloadGameListener
    private lateinit var view: View
    private lateinit var etDownloadGame: EditText
    private lateinit var gameName: String

    init {
        createDialog()
        onDownloadGameListener?.let { this.downloadGameListener = it }
        onBoardSizeSelectListener?.let { this.boardSizeSelectListener = it }
    }

    @SuppressLint("InflateParams")
    private fun createDialog() {
        if (resourceId == R.layout.dialog_board_size) {
            view = LayoutInflater.from(ctx).inflate(resourceId, null)
            radioBtnGroup = view.findViewById(R.id.radioGroupSize)
            titleTv = view.findViewById(R.id.title)
            titleTv.text = title
            radioBtnGroup.check(
                when(currentBoardSize) {
                    BoardSize.EASY -> R.id.rbEasy
                    BoardSize.MEDIUM -> R.id.rbMedium
                    BoardSize.HARD -> R.id.rbHard
                    else -> R.id.rbVeryHard
                })
        } else {
            view = LayoutInflater.from(ctx).inflate(resourceId, null)
            etDownloadGame = view.findViewById(R.id.etDownloadGame)
        }

        val dialogHelper = DialogHelper.Dialog(
            ctx,
            view = view,
            negativeBtnText = ctx.getString(R.string.close),
            positionBtnText = ctx.getString(R.string.confirm),
            onPositiveClick = {
                if (currentBoardSize != null) {
                    val boardSize: BoardSize = when (radioBtnGroup.checkedRadioButtonId) {
                        R.id.rbEasy -> BoardSize.EASY
                        R.id.rbMedium -> BoardSize.MEDIUM
                        R.id.rbHard -> BoardSize.HARD
                        else -> BoardSize.VERY_HARD
                    }
                    boardSizeSelectListener.onSelect(boardSize)
                } else {
                    gameName = etDownloadGame.text.toString()
                    downloadGameListener.onDownloadGame(gameName)
                }
            }
        )
        DialogHelper.AlertDialogBuilder(dialogHelper).build()
    }
}