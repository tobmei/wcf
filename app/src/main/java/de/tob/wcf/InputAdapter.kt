package de.tob.wcf

import android.graphics.Color
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
    private val inputList: List<Input>,
    private val onItemClicked: (Input) -> Unit
) : RecyclerView.Adapter<InputAdapter.ViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = FragmentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding) {
            onItemClicked(inputList[it])
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = inputList[position]
        holder.inputImage.setInput(item)
        holder.inputImage.invalidate()
        //holder.inputDimensions.text = "${item.x}x${item.y}"
        if (position != selectedPosition) {
            holder.card.cardElevation = 0F
            holder.itemView.scaleX = 1F
            holder.itemView.scaleY = 1F
            holder.card.setCardBackgroundColor(Color.WHITE)
        } else {
            holder.card.cardElevation = 5F
            holder.itemView.scaleX = 1.15F
            holder.itemView.scaleY = 1.15F
            holder.card.setCardBackgroundColor(Color.LTGRAY)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if(payloads.isNotEmpty()) {
            payloads.forEach {
                if(it == PAYLOAD_SELECT){
                    holder.card.cardElevation = 5F
                    holder.itemView.scaleX = 1.15F
                    holder.itemView.scaleY = 1.15F
                    holder.card.setCardBackgroundColor(Color.LTGRAY)
                } else if (it == PAYLOAD_DESELECT) {
                    holder.card.cardElevation = 0F
                    holder.itemView.scaleX = 1F
                    holder.itemView.scaleY = 1F
                    holder.card.setCardBackgroundColor(Color.WHITE)
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

    companion object {
        private const val PAYLOAD_SELECT = "PAYLOAD_SELECT"
        private const val PAYLOAD_DESELECT = "PAYLOAD_DESELECT"
    }

    override fun getItemCount(): Int {
        return inputList.size
    }


}