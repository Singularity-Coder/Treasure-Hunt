package com.singularitycoder.treasurehunt

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.singularitycoder.treasurehunt.databinding.ListItemTreasureBinding

class TreasuresAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var treasureList = mutableListOf<Treasure>()
    private var itemClickListener: (treasure: Treasure, position: Int) -> Unit = { treasure, position -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ListItemTreasureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PersonViewHolder).setData(treasureList[position])
    }

    override fun getItemCount(): Int = treasureList.size

    override fun getItemViewType(position: Int): Int = position

    fun setItemClickListener(listener: (treasure: Treasure, position: Int) -> Unit) {
        itemClickListener = listener
    }

    inner class PersonViewHolder(
        private val itemBinding: ListItemTreasureBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
        fun setData(treasure: Treasure) {
            itemBinding.apply {
                cardBody.setOnClickListener {
                    itemClickListener.invoke(treasure, bindingAdapterPosition)
                }
            }
        }
    }
}
