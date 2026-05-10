package de.tadris.flang.ui.fragment.analysis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.databinding.ItemAnalysisMoveBinding
import de.tadris.flang_lib.analysis.MoveInfo
import de.tadris.flang_lib.analysis.MoveJudgmentComment
import de.tadris.flang_lib.analysis.MoveJudgmentType
import de.tadris.flang_lib.TYPE_FLANGER
import de.tadris.flang_lib.TYPE_HORSE
import de.tadris.flang_lib.TYPE_KING
import de.tadris.flang_lib.TYPE_PAWN
import de.tadris.flang_lib.TYPE_ROOK
import de.tadris.flang_lib.TYPE_UNI
import de.tadris.flang_lib.getColor
import de.tadris.flang_lib.getFromPieceState
import de.tadris.flang_lib.getType
import de.tadris.flang_lib.isResign

class AnalysisMovesAdapter(
    var fmn: String?,
    private val onMoveClick: (Int, MoveInfo) -> Unit
) : ListAdapter<MoveInfo, AnalysisMovesAdapter.MoveViewHolder>(MoveDiffCallback()) {

    private var selectedMoveIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val binding = ItemAnalysisMoveBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        holder.bind(getItem(position), position, position == selectedMoveIndex)
    }

    fun setSelectedMoveIndex(index: Int) {
        val oldIndex = selectedMoveIndex
        selectedMoveIndex = index
        
        // Notify only the affected items to avoid unnecessary redraws
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (index >= 0) notifyItemChanged(index)
    }

    inner class MoveViewHolder(
        private val binding: ItemAnalysisMoveBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    setSelectedMoveIndex(position)
                    onMoveClick(position, getItem(position))
                }
            }
        }

        fun bind(moveInfo: MoveInfo, position: Int, isSelected: Boolean) {
            // Move number and player symbol
            val moveNumber = moveInfo.ply
            
            binding.moveNumberText.text = "$moveNumber."
            binding.playerSymbolText.text = getPieceSymbol(moveInfo)

            // Move notation
            binding.moveNotationText.text = moveInfo.moveNotation

            // Evaluation
            binding.evaluationText.text = moveInfo.getEvaluationString()

            // Judgment symbol and loss
            val judgment = moveInfo.judgment
            if (judgment != null) {
                binding.judgmentSymbolText.text = getJudgmentSymbol(judgment.type)
                
                if (judgment.centipawnLoss > 0) {
                    binding.centipawnLossText.text = "(-${judgment.centipawnLoss.toInt()})"
                    binding.centipawnLossText.visibility = View.VISIBLE
                } else {
                    binding.centipawnLossText.text = "(0)"
                }

                // Expandable content
                binding.commentText.text = judgment.comment?.let { 
                    getLocalizedComment(it, itemView.context) 
                } ?: itemView.context.getString(R.string.analysisNoComment)
                
                val bestMoveText = moveInfo.bestMoveNotation
                if (bestMoveText != null && bestMoveText != moveInfo.moveNotation) {
                    binding.bestMoveText.text = itemView.context.getString(R.string.analysisBestMoveFormat, bestMoveText)
                    binding.bestMoveText.visibility = View.VISIBLE
                } else {
                    binding.bestMoveText.visibility = View.GONE
                }
            } else {
                binding.judgmentSymbolText.text = ""
                binding.centipawnLossText.visibility = View.GONE
                binding.commentText.text = itemView.context.getString(R.string.analysisNoAnalysisAvailable)
                binding.bestMoveText.visibility = View.GONE
            }

            binding.expandableContent.visibility = if(isSelected) View.VISIBLE else View.GONE
            
            // Update selection highlighting
            if (isSelected) {
                binding.root.setCardBackgroundColor(
                    itemView.context.getColor(R.color.primaryContainer)
                )
                binding.root.alpha = 0.3f
            } else {
                binding.root.setCardBackgroundColor(
                    itemView.context.getColor(R.color.colorBackground)
                )
                binding.root.alpha = 1.0f
            }
        }

        private fun getPieceSymbol(moveInfo: MoveInfo): String {
            val action = fmn?.let { moveInfo.getAction(it) }
            return if (action != null && !action.isResign()) {
                val piece = action.getFromPieceState()
                val isWhite = piece.getColor()
                when (piece.getType()) {
                    TYPE_KING -> if (isWhite) "♔" else "♚"
                    TYPE_PAWN -> if (isWhite) "♙" else "♟"
                    TYPE_HORSE -> if (isWhite) "♘" else "♞"  // Knight
                    TYPE_ROOK -> if (isWhite) "♖" else "♜"
                    TYPE_UNI -> if (isWhite) "♕" else "♛"    // Queen-like (most powerful)
                    TYPE_FLANGER -> if (isWhite) "♗" else "♝" // Bishop-like (unique piece)
                    else -> ""
                }
            } else {
                // Fallback for non-move actions
                if (moveInfo.isWhiteMove) "♔" else "♚"
            }
        }

        private fun getJudgmentSymbol(type: MoveJudgmentType): String {
            return when (type) {
                MoveJudgmentType.EXCELLENT -> "⭐"
                MoveJudgmentType.GOOD -> "✓"
                MoveJudgmentType.INACCURACY -> "?!"
                MoveJudgmentType.MISTAKE, MoveJudgmentType.MISS -> "?"
                MoveJudgmentType.BLUNDER -> "??"
                MoveJudgmentType.BOOK -> "📖"
                MoveJudgmentType.FORCED -> "□"
                MoveJudgmentType.RESIGN -> "#"
            }
        }
        
        private fun getLocalizedComment(comment: MoveJudgmentComment, context: android.content.Context): String {
            return when (comment) {
                MoveJudgmentComment.BEST_MOVE -> context.getString(R.string.analysisCommentBestMove)
                MoveJudgmentComment.FORCED_MOVE -> context.getString(R.string.analysisCommentForcedMove)
                MoveJudgmentComment.GOOD_MOVE -> context.getString(R.string.analysisCommentGoodMove)
                MoveJudgmentComment.INACCURACY -> context.getString(R.string.analysisCommentInaccuracy)
                MoveJudgmentComment.MISTAKE -> context.getString(R.string.analysisCommentMistake)
                MoveJudgmentComment.BLUNDER -> context.getString(R.string.analysisCommentBlunder)
                MoveJudgmentComment.MAJOR_BLUNDER -> context.getString(R.string.analysisCommentMajorBlunder)
                MoveJudgmentComment.MISSED_FORCED_MATE -> context.getString(R.string.analysisCommentMissedForcedMate)
                MoveJudgmentComment.ALLOWS_MATE -> context.getString(R.string.analysisCommentAllowsMate)
                MoveJudgmentComment.MAJOR_MATERIAL_LOSS -> context.getString(R.string.analysisCommentMajorMaterialLoss)
                MoveJudgmentComment.DEFENDING_AGAINST_MATE -> context.getString(R.string.analysisCommentDefendingAgainstMate)
                MoveJudgmentComment.FASTER_MATE_AVAILABLE -> context.getString(R.string.analysisCommentFasterMateAvailable)
                MoveJudgmentComment.DEFENDING_PERFECTLY -> context.getString(R.string.analysisCommentDefendingPerfectly)
                MoveJudgmentComment.BOOK_MOVE -> context.getString(R.string.analysisCommentBook)
                MoveJudgmentComment.RESIGN -> context.getString(R.string.analysisCommentResign)
                MoveJudgmentComment.MISS -> TODO()
            }
        }
    }

    private class MoveDiffCallback : DiffUtil.ItemCallback<MoveInfo>() {
        override fun areItemsTheSame(oldItem: MoveInfo, newItem: MoveInfo): Boolean {
            return oldItem.ply == newItem.ply
        }

        override fun areContentsTheSame(oldItem: MoveInfo, newItem: MoveInfo): Boolean {
            return oldItem == newItem
        }
    }
}