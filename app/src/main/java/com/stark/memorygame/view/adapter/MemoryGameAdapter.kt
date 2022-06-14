package com.stark.memorygame.view.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.stark.memorygame.R
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.model.MemoryCard
import com.stark.memorygame.databinding.MemoryCardBinding
import kotlin.math.min

class MemoryGameAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val memoryCards: List<MemoryCard>,
    private val onCardClickListener: MemoryCardClickListener
) : RecyclerView.Adapter<MemoryGameAdapter.MemoryViewHolder>() {

    companion object {
        private const val MARGIN = 10
    }

    inner class MemoryViewHolder(private val binding: MemoryCardBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val card = memoryCards[position]
            Log.i("checks", "bind: $memoryCards")
            if (card.isFaceUp) {
                card.imageUrl?.let {
                    Picasso.get().load(it).placeholder(R.drawable.ic_baseline_image_24).into(binding.imageButton)
                } ?: binding.imageButton.setImageDrawable(card.identifier)
            } else {
                binding.imageButton.setImageDrawable(R.drawable.bamboo)
            }

            binding.imageButton.alpha = if (card.isMatched) .4f else 1.0f
            val colorStateList = if (card.isMatched) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(binding.imageButton, colorStateList)
            binding.imageButton.setOnClickListener {
                onCardClickListener.onCardClick(position)
            }
        }

        private fun ImageButton.setImageDrawable(id: Int) {
            setImageDrawable(ContextCompat.getDrawable(context, id))
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val cardWidth = parent.width / boardSize.getWidth() - MARGIN
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN)
        val cardSize = min(cardWidth, cardHeight)
        val inflater = LayoutInflater.from(context)
        val rootLayout = MemoryCardBinding.inflate(inflater, parent, false)
        val layoutParams = rootLayout.cardView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.height = cardHeight
        layoutParams.width = cardSize
        layoutParams.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)
        return MemoryViewHolder(rootLayout)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return boardSize.pairs
    }
}

interface MemoryCardClickListener {
    fun onCardClick(position: Int)
}