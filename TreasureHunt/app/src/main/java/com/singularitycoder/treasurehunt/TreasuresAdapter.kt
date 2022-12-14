package com.singularitycoder.treasurehunt

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.singularitycoder.treasurehunt.databinding.LayoutTreasureImageBinding
import com.singularitycoder.treasurehunt.databinding.ListItemTreasureBinding
import com.singularitycoder.treasurehunt.helpers.Tab
import com.singularitycoder.treasurehunt.helpers.drawable
import com.singularitycoder.treasurehunt.helpers.toBitmapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TreasuresAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var tab = Tab.EXPLORE.value
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
                tvTreasureName.text = treasure.title
                CoroutineScope(IO).launch {
                    val bitmap = prepareImageBitmap(treasure)
                    withContext(Main) {
                        ivImage.load(bitmap)
                    }
                }
                cardBody.setOnClickListener {
                    itemClickListener.invoke(treasure, bindingAdapterPosition)
                }
            }
        }

        private fun prepareImageBitmap(treasure: Treasure): BitmapDrawable? {
            val imageLayout = LayoutTreasureImageBinding.inflate(LayoutInflater.from(itemBinding.root.context)).apply {
                tvFileExtension.text = treasure.filePath.substringAfterLast(".").ifBlank { "file" }
                if (tab == Tab.EXPLORE.value) {
                    ivImage.setImageDrawable(itemBinding.root.context.drawable(R.drawable.ic_baseline_get_app_24))
                } else {
                    ivImage.setImageDrawable(itemBinding.root.context.drawable(R.drawable.ic_round_open_in_new_24))
                }
            }
            val bitmapDrawableOfLayout = imageLayout.root.toBitmapOf(
                width = 480,
                height = 210
            )?.toDrawable(itemBinding.root.context.resources)
            return bitmapDrawableOfLayout
        }
    }
}
