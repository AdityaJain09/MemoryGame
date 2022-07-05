package com.stark.memorygame.view.custom_views

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stark.memorygame.R
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.view.adapter.DownloadGameAdapter
import com.stark.memorygame.view.adapter.OnGameNameClickListener
import com.stark.memorygame.view.common.DialogHelper
import org.w3c.dom.Text

interface OnBoardSizeSelectListener {
    fun onSelect(size: BoardSize)
}

interface OnDownloadGameListener {
    fun onDownloadGame(gameName: String)
}

class BoardSelectorDialog(
    private val ctx: Context,
    onBoardSizeSelectListener: OnBoardSizeSelectListener? = null,
    onDownloadGameListener: OnDownloadGameListener? = null,
    private val currentBoardSize: BoardSize? = null,
    private val isGameDownloadable: Boolean = true,
    private val title: String,
    inline val downloadGameList: List<String> = listOf(),
    @LayoutRes private val resourceId: Int
) {

    private lateinit var radioBtnGroup: RadioGroup
    private lateinit var titleTv: TextView
    private lateinit var downloadListAdapter: DownloadGameAdapter
    private lateinit var downloadRv: RecyclerView
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
            val infoText = view.findViewById<TextView>(R.id.info)
            val title = view.findViewById<TextView>(R.id.title_tv)
            val emptyGameTv = view.findViewById<TextView>(R.id.empty_rv_tv)
            downloadRv = view.findViewById(R.id.download_list_rv)

            if (downloadGameList.isEmpty()) {
                emptyGameTv.visibility = View.VISIBLE
            }

            if (!isGameDownloadable) {
                title.text = ctx.getString(R.string.mylist)
                infoText.visibility = View.GONE
                etDownloadGame.visibility = View.GONE
            }

            val layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
            downloadRv.addItemDecoration(
                DividerItemDecoration(
                    ctx,
                    layoutManager.orientation
                )
            )
            downloadListAdapter = DownloadGameAdapter(
                downloadGameList?.toList(),
                isGameDownloadable = isGameDownloadable,
                object : OnGameNameClickListener {
                    override fun onClick(name: String) {
                        etDownloadGame.setText(name)
                    }
                }
            )
            downloadRv.layoutManager = layoutManager
            downloadRv.adapter = downloadListAdapter
        }

        val dialogHelper = DialogHelper.Dialog(
            ctx,
            view = view,
            negativeBtnText = ctx.getString(R.string.close),
            positionBtnText = if (isGameDownloadable) ctx.getString(R.string.confirm) else null,
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