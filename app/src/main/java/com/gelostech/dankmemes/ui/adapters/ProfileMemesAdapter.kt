package com.gelostech.dankmemes.ui.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.gelostech.dankmemes.R
import com.gelostech.dankmemes.data.models.Meme
import com.gelostech.dankmemes.data.models.User
import com.gelostech.dankmemes.data.wrappers.ItemViewModel
import com.gelostech.dankmemes.data.wrappers.ObservableMeme
import com.gelostech.dankmemes.data.wrappers.ObservableUser
import com.gelostech.dankmemes.databinding.ItemProfileBinding
import com.gelostech.dankmemes.databinding.ItemProfileMemeBinding
import com.gelostech.dankmemes.ui.callbacks.MemesCallback
import com.gelostech.dankmemes.ui.callbacks.ProfileMemesCallback
import com.gelostech.dankmemes.utils.inflate
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber

class ProfileMemesAdapter(private val callback: ProfileMemesCallback, private val isMe: Boolean = false): PagedListAdapter<ItemViewModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    enum class VIEW_TYPE {
        PROFILE,
        MEME
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ItemViewModel>() {
            override fun areItemsTheSame(oldItem: ItemViewModel, newItem: ItemViewModel): Boolean {
                return when {
                    oldItem is ObservableMeme && newItem is ObservableMeme -> oldItem.id == newItem.id
                    oldItem is ObservableUser && newItem is ObservableUser -> oldItem.id == newItem.id
                    else -> false
                }
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: ItemViewModel, newItem: ItemViewModel): Boolean {
                return when {
                    oldItem is ObservableMeme && newItem is ObservableMeme -> oldItem == newItem
                    oldItem is ObservableUser && newItem is ObservableUser -> oldItem == newItem
                    else -> false
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ObservableUser -> VIEW_TYPE.PROFILE.ordinal
            else -> VIEW_TYPE.MEME.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE.PROFILE.ordinal -> ProfileHolder(parent.inflate(R.layout.item_profile), callback, isMe)
            else -> MemeHolder(parent.inflate(R.layout.item_profile_meme), callback)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ProfileHolder -> {
                val user = getItem(position) as ObservableUser

                user.user.subscribeBy(
                        onNext = { holder.bind(it) },
                        onError = { Timber.e("Error fetching User") }
                )
            }

            is MemeHolder -> {
                val meme = getItem(position) as ObservableMeme

                meme.meme.subscribeBy(
                        onNext = { holder.bind(it) },
                        onError = {
                            Timber.e("Meme deleted")
                            this.currentList?.dataSource?.invalidate()
                        }
                ).addTo(holder.disposables)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is MemeHolder) holder.apply { disposables.clear() }
        if (holder is ProfileHolder) holder.apply { disposables.clear() }
    }

    inner class ProfileHolder(private val binding: ItemProfileBinding, private val callback: ProfileMemesCallback, private val isMe: Boolean):
            RecyclerView.ViewHolder(binding.root) {
        val disposables = CompositeDisposable()

        fun bind(user: User) {
            Timber.e("Binding User: $user")
            binding.user = user
            binding.callback = callback
            binding.isMe = isMe
        }

    }

    inner class MemeHolder(private val binding: ItemProfileMemeBinding, private val callback: ProfileMemesCallback):
            RecyclerView.ViewHolder(binding.root) {
        val disposables = CompositeDisposable()

        // Bind meme object to layout
        fun bind(meme: Meme) {
            Timber.e("Binding ${meme.id}")

            binding.meme = meme
            binding.callback = callback
            binding.parent.setOnLongClickListener {
                callback.onMemeLongClicked(meme)
                true
            }
        }
    }

}