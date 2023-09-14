package com.condorserg.scopedstorage.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.condorserg.scopedstorage.R
import com.condorserg.scopedstorage.Video
import com.condorserg.scopedstorage.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate


class VideosListAdapterDelegate(
    private val onLongClicked: (position: Int) -> Unit
) :
    AbsListItemAdapterDelegate<Video, Video, VideosListAdapterDelegate.VideosHolder>() {

    class VideosHolder(
        view: View,
        private val onLongClicked: (position: Int) -> Unit

    ) : RecyclerView.ViewHolder(view) {
        private val videoNameTextView: TextView = view.findViewById(R.id.videoNameTextView)
        private val videoSizeTextView: TextView = view.findViewById(R.id.videoSizeTextView)
        private val videoPreviewImageView: ImageView = view.findViewById(R.id.videoPreviewImageView)

        init {
            view.setOnLongClickListener {
                onLongClicked(adapterPosition)
                true
            }
        }

        fun bind(video: Video) {
            videoNameTextView.text = video.name
            videoSizeTextView.text = "Size: " + (video.size/1000000).toString() + " MB"
            Glide.with(itemView)
                .load(video.uri)
                .error(R.drawable.ic_error)
                .into(videoPreviewImageView)
        }

    }

    override fun isForViewType(item: Video, items: MutableList<Video>, position: Int): Boolean {
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup): VideosHolder {

        return VideosHolder(parent.inflate(R.layout.item_video), onLongClicked)
    }

    override fun onBindViewHolder(
        item: Video,
        holder: VideosHolder,
        payloads: MutableList<Any>
    ) {
        holder.bind(item)
    }
}