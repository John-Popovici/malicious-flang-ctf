package de.tadris.flang.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.network_api.model.Rating
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class RatingAdapter(
    private val context: Context,
    private val ratings: List<Rating>
) : RecyclerView.Adapter<RatingAdapter.RatingViewHolder>() {

    class RatingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ratingType: TextView = itemView.findViewById(R.id.ratingType)
        val ratingValue: TextView = itemView.findViewById(R.id.ratingValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rating, parent, false)
        return RatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        val rating = ratings[position]

        holder.ratingType.text = getRatingTypeDisplayName(rating.type)
        holder.ratingValue.text = formatRating(rating.rating)
    }

    override fun getItemCount(): Int = ratings.size

    private fun getRatingTypeDisplayName(type: String): String {
        return when (type) {
            Rating.TYPE_BULLET -> context.getString(R.string.ratingTypeBullet)
            Rating.TYPE_BLITZ -> context.getString(R.string.ratingTypeBlitz)
            Rating.TYPE_CLASSICAL -> context.getString(R.string.ratingTypeClassical)
            Rating.TYPE_DAILY -> context.getString(R.string.ratingTypeDaily)
            Rating.TYPE_PUZZLE -> context.getString(R.string.ratingTypePuzzle)
            else -> type.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatRating(rating: Float): String {
        return if (rating == 0f) {
            "?"
        } else {
            rating.absoluteValue.roundToInt().toString() + if (rating < 0) "?" else ""
        }
    }
}