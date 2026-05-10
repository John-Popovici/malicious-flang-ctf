package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.tadris.flang.R
import de.tadris.flang.bot.NativeCFlangEngine
import de.tadris.flang.game.GameController
import de.tadris.flang.game.OfflineBotGameController
import de.tadris.flang_lib.Variant
import de.tadris.flang_lib.bot.*
import de.tadris.flang_lib.bot.evaluation.FastBoardEvaluation
import de.tadris.flang_lib.bot.evaluation.FastNeoBoardEvaluation
import de.tadris.flang_lib.bot.evaluation.HeatmapEvaluation
import de.tadris.flang_lib.bot.evaluation.StageEvaluation
import de.tadris.flang_lib.bot.fast.FastFlangBot
import de.tadris.flang_lib.bot.evaluation.SimpleFastEvaluation
import kotlin.math.min

class NextGameFragment : GameFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        root.findViewById<View>(R.id.player1InfoParent).setOnClickListener {
            openStrengthChooseDialog()
        }
        root.findViewById<View>(R.id.player2InfoParent).setOnClickListener {
            openStrengthChooseDialog()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (gameController as? OfflineBotGameController)?.updateVariant(Variant.NEXT)
        (gameController as? OfflineBotGameController)
            ?.updateConfiguration("Legacy#4", javaCreator(4), 4)
        baseBoard.variant = Variant.NEXT

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.nextBotGame)
            .setMessage(R.string.nextGameDescription)
            .setPositiveButton(R.string.okay, null)
            .show()
    }

    private fun openStrengthChooseDialog(){
        val arrayAdapter = ArrayAdapter<String>(requireActivity(), android.R.layout.select_dialog_item)
        val options = mutableMapOf(
            "Legacy#1" to Pair(javaCreator(1), 1),
            "Legacy#3" to Pair(javaCreator(3), 3),
            "Legacy#5" to Pair(javaCreator(6), 5),
            "Legacy#5s" to Pair(javaCreator(7), -5),
            "StageBot#3s" to Pair(javaCreator(10) { StageEvaluation() }, -3),
            "HeatBot#3s" to Pair(javaCreator(10) { HeatmapEvaluation() }, -3),
            "SimpleBot#3s" to Pair(javaCreator(10) { SimpleFastEvaluation() }, -3),
        )
        arrayAdapter.addAll(options.keys)
        AlertDialog.Builder(activity)
            .setTitle(R.string.offlineChooseStrength)
            .setAdapter(arrayAdapter) { _, which ->
                val option = options.entries.toList()[which]
                val selectedConfiguration = option.value
                (gameController as? OfflineBotGameController)
                    ?.updateConfiguration(option.key, selectedConfiguration.first, selectedConfiguration.second)
            }
            .show()
    }

    private fun javaCreator(depth: Int, evalFactory: () -> FastBoardEvaluation = { FastNeoBoardEvaluation() }): () -> Engine {
        return {
            FastFlangBot(
                minDepth = min(5, depth),
                maxDepth = depth,
                useOpeningDatabase = false,
                evaluationFactory = evalFactory,
                useLME = true,
                lmeMaxExtension = min(depth, 3),
            )
        }
    }

    override fun createGameController(): GameController {
        return OfflineBotGameController(requireActivity())
    }

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_offline_game_to_nav_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_offline_game_to_nav_chat
}