package de.tadris.flang.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.GameConfiguration
import de.tadris.flang.network_api.util.DefaultConfigurations
import de.tadris.flang_lib.utils.TimeUtils

class GameConfigurationAdapter(private val listener: ConfigurationListener?) : RecyclerView.Adapter<GameConfigurationAdapter.GameConfigurationViewHolder>() {

    private val configurations = DefaultConfigurations.getConfigurations()

    class GameConfigurationViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val timeText = root.findViewById<TextView>(R.id.configurationTime)!!
        val nameText = root.findViewById<TextView>(R.id.configurationName)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameConfigurationViewHolder {
        return GameConfigurationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_game_configuration, parent, false))
    }

    override fun onBindViewHolder(holder: GameConfigurationViewHolder, position: Int) {
        val context = holder.root.context
        val config = configurations[position]
        if(!config.second.isCustomGame()){
            if(config.second.isDailyGame()){
                // Show time per move for daily games
                val hours = (config.second.time / 1000 / 60 / 60).toInt()
                val days = hours / 24
                holder.timeText.text = if(days > 0) "${days}d" else "${hours}h"
            }else if(config.second.infiniteTime){
                holder.timeText.text = context.getString(R.string.infiniteTimeChar)
            }else{
                holder.timeText.text = TimeUtils.getTimeControlDisplay(config.second.time, config.second.timeIncrement)
            }
            holder.nameText.text = config.first
            holder.nameText.visibility = View.VISIBLE
        }else{
            holder.timeText.text = config.first
            holder.nameText.visibility = View.GONE
        }
        holder.root.setOnClickListener { listener?.onClick(config.second) }
    }

    override fun getItemCount() = configurations.size

    interface ConfigurationListener {

        fun onClick(configuration: GameConfiguration)

    }

}