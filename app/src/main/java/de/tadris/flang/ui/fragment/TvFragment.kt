package de.tadris.flang.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import de.tadris.flang.R
import de.tadris.flang.game.FlangTvGameController
import de.tadris.flang.game.GameController
import de.tadris.flang_lib.Color

class TvFragment : GameFragment() {

    override fun createGameController(): GameController {
        return FlangTvGameController(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        root.findViewById<View>(R.id.player1InfoParent).setOnClickListener {
            if(lastGameInfo != null){
                showProfile(player1ViewController.color)
            }
        }
        root.findViewById<View>(R.id.player2InfoParent).setOnClickListener {
            if(lastGameInfo != null){
                showProfile(player2ViewController.color)
            }
        }

        return root
    }

    private fun showProfile(color: Color){
        showProfile(if(color) lastGameInfo!!.white.username else lastGameInfo!!.black.username)
    }

    private fun showProfile(username: String){
        val bundle = Bundle()
        bundle.putString(ProfileFragment.ARGUMENT_USERNAME, username)
        findNavController().navigate(R.id.action_nav_tv_to_nav_profile, bundle)
    }

    override fun getNavigationLinkToAnalysis() = R.id.action_nav_tv_to_nav_analysis
    override fun getNavigationLinkToChat() = R.id.action_nav_tv_to_nav_chat
}