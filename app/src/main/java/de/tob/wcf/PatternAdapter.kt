package de.tob.wcf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.tob.wcf.databinding.FragmentItemBinding
import de.tob.wcf.databinding.FragmentItemPatternBinding
import de.tob.wcf.db.Input

class PatternAdapter(
    private val onItemClicked: (Input) -> Unit
) : ListAdapter<Input, PatternAdapter.ViewHolder>(InputComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = FragmentItemPatternBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding) {
            onItemClicked(getItem(it))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.pattern.setInput(item)
        holder.tvWeight.visibility = View.GONE
    }

    inner class ViewHolder(
        binding: FragmentItemPatternBinding,
        onItemClicked : (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onItemClicked(bindingAdapterPosition)
            }
        }
        val pattern: Preview = binding.pattern
        val tvWeight: TextView = binding.tvWeight

    }

    class InputComparator : DiffUtil.ItemCallback<Input>() {
        override fun areItemsTheSame(oldItem: Input, newItem: Input): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Input, newItem: Input): Boolean {
            return oldItem.pixels == newItem.pixels
        }
    }


}