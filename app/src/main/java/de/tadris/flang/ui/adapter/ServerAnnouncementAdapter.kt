package de.tadris.flang.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.ServerAnnouncement

class ServerAnnouncementAdapter(
    private val callback: AnnouncementAdapterCallback,
    private var announcements: List<ServerAnnouncement>
) : RecyclerView.Adapter<ServerAnnouncementAdapter.ServerAnnouncementViewHolder>() {

    class ServerAnnouncementViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val titleText = root.findViewById<TextView>(R.id.announcementTitle)!!
        val messageText = root.findViewById<TextView>(R.id.announcementMessage)!!
        val urlIndicator = root.findViewById<View>(R.id.announcementUrlIndicator)!!
    }

    fun updateList(announcements: List<ServerAnnouncement>){
        this.announcements = announcements
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerAnnouncementViewHolder {
        return ServerAnnouncementViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.view_server_announcement, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ServerAnnouncementViewHolder, position: Int) {
        val announcement = announcements[position]
        if(announcement.title.isNotEmpty()){
            holder.titleText.visibility = View.VISIBLE
            holder.titleText.text = announcement.title
        }else{
            holder.titleText.visibility = View.GONE
        }
        if(announcement.text.isNotEmpty()){
            holder.messageText.visibility = View.VISIBLE
            holder.messageText.text = announcement.text
        }else{
            holder.messageText.visibility = View.GONE
        }
        if(announcement.url.isNotEmpty()){
            holder.urlIndicator.visibility = View.VISIBLE
            holder.root.setOnClickListener { callback.onUrlClick(announcement.url) }
        }else{
            holder.urlIndicator.visibility = View.GONE
        }
    }

    override fun getItemCount() = announcements.size

    interface AnnouncementAdapterCallback {

        fun onUrlClick(url: String)

    }

}