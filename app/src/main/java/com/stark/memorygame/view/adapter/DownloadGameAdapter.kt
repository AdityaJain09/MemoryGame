package com.stark.memorygame.view.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.stark.memorygame.R

class DownloadGameAdapter(
    private val gameList: List<String>? = null,
    private val isGameDownloadable: Boolean,
    private val gameNameClickListener: OnGameNameClickListener
) : RecyclerView.Adapter<DownloadGameAdapter.DownloadGameViewHolder>() {

    inner class DownloadGameViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val gameNameTv = view.findViewById<MaterialTextView>(R.id.gameName_tv)
        fun bind(position: Int) {
            gameList?.let {
                gameNameTv.text = gameList[position]
            }
            if (!isGameDownloadable) {
                gameNameTv.isClickable = false
            }

            gameNameTv.setOnClickListener {
                val gameName = gameNameTv.text.toString()
                gameNameClickListener.onClick(gameName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadGameViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_download_list, parent, false)
        return DownloadGameViewHolder(view)
    }

    override fun onBindViewHolder(holder: DownloadGameViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return gameList?.size ?: 0
    }
}

interface OnGameNameClickListener {
    fun onClick(name: String)
}