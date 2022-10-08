package de.tob.wcf

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.tob.wcf.databinding.FragmentItemBinding
import de.tob.wcf.db.Input

class InputAdapter(
    private val onItemClicked: (Input) -> Unit
) : ListAdapter<Input, InputAdapter.ViewHolder>(PatternAdapter.InputComparator()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = FragmentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding) {
            onItemClicked(getItem(it))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.inputImage.setInput(item)
        holder.inputImage.invalidate()
        //holder.inputDimensions.text = "${item.x}x${item.y}"
        if (position != selectedPosition) {
            holder.card.cardElevation = 0F
            holder.itemView.scaleX = 1F
            holder.itemView.scaleY = 1F
        }

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if(payloads.isNotEmpty()) {
            payloads.forEach {
                if(it == PAYLOAD_SELECT){
                    holder.card.cardElevation = 5F
                    holder.itemView.scaleX = 1.15F
                    holder.itemView.scaleY = 1.15F
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
        binding: FragmentItemBinding,
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
        val inputImage: Preview = binding.ivInput
        val card: CardView = binding.cardViewInput
    }

    class InputComparator : DiffUtil.ItemCallback<Input>() {
        override fun areItemsTheSame(oldItem: Input, newItem: Input): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Input, newItem: Input): Boolean {
            return ( (oldItem.pixels == newItem.pixels) && (oldItem.x == newItem.x) )
        }
    }

    companion object {
        private const val PAYLOAD_SELECT = "PAYLOAD_SELECT"
        private const val PAYLOAD_DESELECT = "PAYLOAD_DESELECT"
    }


}