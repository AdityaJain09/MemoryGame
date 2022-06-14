package com.stark.memorygame.view.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.databinding.CardImageBinding

class CustomGameAdapter(
    private val context: Context,
    private val images: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<CustomGameAdapter.CustomGameViewHolder>() {

    inner class CustomGameViewHolder(
        private val binding: CardImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri?) {
            uri?.let {
                binding.ivCustomImage.apply {
                    setImageURI(it)
                    setOnClickListener(null)
                }
                return
            }
            binding.ivCustomImage.setOnClickListener {
                imageClickListener.onClickImagePlaceHolder()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomGameViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        val cardWidth = parent.width / if (boardSize.getWidth() > 3) boardSize.getWidth() + 1 else boardSize.getWidth()
        val cardHeight = parent.height / if (boardSize.getHeight() > 3) boardSize.getHeight() + 1 else boardSize.getHeight()
        val cardSideLength = minOf(cardWidth, cardHeight)
        val root = CardImageBinding.inflate(layoutInflater, parent, false)
        val layoutParams = root.ivCustomImage.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        return CustomGameViewHolder(root)
    }

    override fun onBindViewHolder(holder: CustomGameViewHolder, position: Int) {
        holder.bind(if (position < images.size) images[position] else null)
    }

    override fun getItemCount(): Int {
        return boardSize.getTotalPairs()
    }
}

interface ImageClickListener {
    fun onClickImagePlaceHolder()
}