package de.tadris.flang.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.Premove
import de.tadris.flang_lib.Game
import de.tadris.flang_lib.getNotationV1

class PremoveAdapter(
    private val referenceGame: Game,
    private var premoves: List<Premove> = emptyList(),
    private val onDeleteClick: (Premove) -> Unit,
) : RecyclerView.Adapter<PremoveAdapter.PremoveViewHolder>() {

    fun updatePremoves(newPremoves: List<Premove>) {
        premoves = newPremoves
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PremoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premove, parent, false)
        return PremoveViewHolder(view)
    }

    override fun onBindViewHolder(holder: PremoveViewHolder, position: Int) {
        holder.bind(premoves[position])
    }

    override fun getItemCount(): Int = premoves.size

    inner class PremoveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val moveText: TextView = itemView.findViewById(R.id.moveText)
        private val moveCondition: TextView = itemView.findViewById(R.id.moveCondition)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(premove: Premove) {
            moveText.text = premove.move.getNotationV1()

            val conditionText = if (premove.fmnCondition != null) {
                itemView.context.getString(R.string.premoveConditionWithFmn, premove.moveCount, premove.fmnCondition?.replaceFirst(referenceGame.getFMNv1(), "") ?: "")
            } else {
                itemView.context.getString(R.string.premoveConditionSimple, premove.moveCount)
            }
            moveCondition.text = conditionText

            deleteButton.setOnClickListener {
                onDeleteClick(premove)
            }
        }
    }
}