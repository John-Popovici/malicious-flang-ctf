package de.tadris.flang.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang.network_api.model.UserResult
import de.tadris.flang.util.applyTo
import de.tadris.flang.util.getTitleColor

class UserAdapter(private val listener: UserAdapterListener,
                  private var users: UserResult = UserResult(emptyList()))
    : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val usernameText = root.findViewById<TextView>(R.id.userUsername)!!
        val ratingText = root.findViewById<TextView>(R.id.userRating)!!
        val titleText = root.findViewById<TextView>(R.id.userTitle)!!
    }

    fun updateList(users: UserResult){
        this.users = users
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_user, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users.users[position]

        user.applyTo(holder.titleText, holder.usernameText, holder.ratingText)
        if(user.hasTitle()){
            holder.titleText.text = user.getDisplayedTitle()
            holder.titleText.setTextColor(user.getTitleColor(holder.titleText.context))
        }
        holder.root.setOnClickListener { listener.onClick(user) }
    }

    override fun getItemCount() = users.users.size

    interface UserAdapterListener {
        fun onClick(user: UserInfo)
    }

}