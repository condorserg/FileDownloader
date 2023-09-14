package com.condorserg.scopedstorage.adapter

import androidx.recyclerview.widget.DiffUtil
import com.condorserg.scopedstorage.Video
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter

class VideosListAdapter(
    //onItemClicked: (video: Video) -> Unit,
    onLongClicked: (position: Int) -> Unit

) : AsyncListDifferDelegationAdapter<Video>(VideoDiffUtilCallBack()) {
    init {
        delegatesManager.addDelegate(VideosListAdapterDelegate(onLongClicked))

    }

    class VideoDiffUtilCallBack : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem == newItem
        }
    }


}