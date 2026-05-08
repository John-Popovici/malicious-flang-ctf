package de.tadris.flang.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.OpeningDatabaseEntry
import de.tadris.flang.network_api.model.ServerAnnouncement
import de.tadris.flang.util.Positions

class OpeningDatabaseEntriesAdapter(private val listener: OpeningDatabaseEntryListener,
                                    private var entries: List<OpeningDatabaseEntry>)
    : RecyclerView.Adapter<OpeningDatabaseEntriesAdapter.OpeningDatabaseEntryViewHolder>() {

    class OpeningDatabaseEntryViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val moveText = root.findViewById<TextView>(R.id.openingDatabaseEntryMove)!!
        val positionText = root.findViewById<TextView>(R.id.openingDatabaseEntryMoveName)!!
        val gameCountText = root.findViewById<TextView>(R.id.openingDatabaseGameCount)!!
        val whitePercent = root.findViewById<TextView>(R.id.openingDatabaseEntryWhitePercent)!!
        val blackPercent = root.findViewById<TextView>(R.id.openingDatabaseEntryBlackPercent)!!
    }

    fun updateList(entries: List<OpeningDatabaseEntry>){
        this.entries = entries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpeningDatabaseEntryViewHolder {
        return OpeningDatabaseEntryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_opening_database_entry, parent, false))
    }

    override fun onBindViewHolder(holder: OpeningDatabaseEntryViewHolder, position: Int) {
        val entry = entries[position]

        holder.moveText.text = entry.move
        holder.positionText.text = listener.getPositionName(entry) ?: ""
        holder.gameCountText.text = entry.gameCount.toString()
        holder.whitePercent.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            entry.getWhitePercent().toFloat())
        holder.whitePercent.text = entry.getWhitePercent().toString() + "%"
        holder.blackPercent.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            entry.getBlackPercent().toFloat())
        holder.blackPercent.text = entry.getBlackPercent().toString() + "%"
        holder.root.setOnClickListener { listener.onEntryClick(entry) }
    }

    override fun getItemCount() = entries.size

    interface OpeningDatabaseEntryListener {
        fun onEntryClick(entry: OpeningDatabaseEntry)

        fun getPositionName(entry: OpeningDatabaseEntry): String?
    }

}