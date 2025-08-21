package com.bodyshapeeditor.slim_body_photo_editor

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CustomGalleryAdapter(
    private val images: MutableList<Uri>,
    private val onItemClick: (Uri) -> Unit,
    private val category: String

) : RecyclerView.Adapter<CustomGalleryAdapter.ImageViewHolder>() {

    companion object {
        private const val TYPE_SPECIAL = 0
        private const val TYPE_NORMAL = 1
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < 3) TYPE_SPECIAL else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val layoutId = if (category == "customgallery") {
            if (viewType == TYPE_SPECIAL) {
                R.layout.image_itemview_special
            } else {
                R.layout.image_item_view
            }
        } else {
            R.layout.image_item_view
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ImageViewHolder(view)
    }


    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]
        Glide.with(holder.imageView.context).load(uri).centerCrop()// Optional: scale image to fit
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            onItemClick(uri)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun updateData(newImages: List<Uri>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }


}
