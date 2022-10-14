package de.tob.wcf

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.tob.wcf.databinding.ColorItemBinding
import de.tob.wcf.databinding.FragmentItemBinding
import de.tob.wcf.db.Input

class ColorAdapter(
    private val onItemClicked: (Int) -> Unit
) : ListAdapter<Int, ColorAdapter.ViewHolder>(ColorAdapter.ColorComparator()) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = ColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding) {
            onItemClicked(getItem(it))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.card.setCardBackgroundColor(item)
        //holder.inputDimensions.text = "${item.x}x${item.y}"
        if (position != selectedPosition) {
            holder.card.cardElevation = 0F
            holder.itemView.scaleX = 1F
            holder.itemView.scaleY = 1F
        } else {
            holder.card.cardElevation = 5F
            holder.itemView.scaleX = 1.3F
            holder.itemView.scaleY = 1.3F
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if(payloads.isNotEmpty()) {
            payloads.forEach {
                if(it == PAYLOAD_SELECT){
                    holder.card.cardElevation = 5F
                    holder.itemView.scaleX = 1.3F
                    holder.itemView.scaleY = 1.3F
                } else if (it == PAYLOAD_DESELECT) {
                    holder.card.cardElevation = 0F
                    holder.itemView.scaleX = 1F
                    holder.itemView.scaleY = 1F
                }
            }
        } else
            super.onBindViewHolder(holder, position, payloads)
    }

    inner class ViewHolder(
        binding: ColorItemBinding,
        onItemClicked : (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClicked(bindingAdapterPosition)
                if(bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(selectedPosition, PAYLOAD_DESELECT)
                    selectedPosition = bindingAdapterPosition
                    notifyItemChanged(selectedPosition, PAYLOAD_SELECT)
                }
            }
        }
        val card: CardView = binding.colorCard
    }

    class ColorComparator : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val PAYLOAD_SELECT = "PAYLOAD_SELECT"
        private const val PAYLOAD_DESELECT = "PAYLOAD_DESELECT"
    }


}