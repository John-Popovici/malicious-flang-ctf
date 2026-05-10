package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tadris.flang.R
import de.tadris.flang.databinding.FragmentHomeBinding
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.exception.NotFoundException
import de.tadris.flang.network_api.exception.UnauthorizedException
import de.tadris.flang.network_api.model.*
import de.tadris.flang.ui.activity.LoginActivity
import de.tadris.flang.ui.adapter.GameAdapter
import de.tadris.flang.ui.adapter.GameConfigurationAdapter
import de.tadris.flang.ui.adapter.ServerAnnouncementAdapter
import de.tadris.flang.ui.dialog.CustomGameConfigurationDialog
import de.tadris.flang.ui.dialog.DailyRequestAddDialog
import de.tadris.flang.ui.dialog.GameRequestAcceptDialog
import de.tadris.flang.ui.dialog.GameRequestAddDialog
import de.tadris.flang.ui.view.DailyGameRequestView
import de.tadris.flang.ui.view.GameRequestView
import de.tadris.flang.ui.view.addBottomPadding
import de.tadris.flang.util.GameCache
import kotlin.Exception
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home), GameConfigurationAdapter.ConfigurationListener, GameRequestView.GameRequestListener,
    DailyGameRequestView.DailyGameRequestListener, GameAdapter.GameAdapterListener, CustomGameConfigurationDialog.CustomGameConfigurationListener,
    ServerAnnouncementAdapter.AnnouncementAdapterCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val gameCache by lazy { GameCache.getInstance(requireContext()) }
    private val configurationAdapter = GameConfigurationAdapter(this)
    private val highPriorityAnnouncementAdapter = ServerAnnouncementAdapter(this, emptyList())
    private val lowPriorityAnnouncementAdapter = ServerAnnouncementAdapter(this, emptyList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        _binding = FragmentHomeBinding.bind(root)

        binding.homeContent.addBottomPadding()

        binding.requestConfigurations.adapter = configurationAdapter
        binding.requestConfigurations.layoutManager = GridLayoutManager(requireContext(), 3)

        binding.homeHighPriorityAnnouncements.adapter = highPriorityAnnouncementAdapter
        binding.homeHighPriorityAnnouncements.layoutManager = LinearLayoutManager(requireContext())
        
        binding.homeLowPriorityAnnouncements.adapter = lowPriorityAnnouncementAdapter
        binding.homeLowPriorityAnnouncements.layoutManager = LinearLayoutManager(requireContext())

        binding.homeActiveGames.layoutManager = LinearLayoutManager(requireContext())

        binding.homeLearn.setOnClickListener {
            openTutorial()
        }

        checkFirstStart()

        return root
    }

    override fun onResume() {
        super.onResume()
        startRefreshThread()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startRefreshThread(){
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                withContext(Dispatchers.IO) {
                    DataRepository.getInstance().login(requireContext())
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
            while (isResumed){
                refreshLobby()
                delay(10000)
            }
        }
    }

    private suspend fun refreshLobby(){
        try{
            val serverInfo = withContext(Dispatchers.IO) {
                DataRepository.getInstance().accessOpenAPI().getInfo()
            }
            showInfo(serverInfo)

            val lobby = withContext(Dispatchers.IO) {
                DataRepository.getInstance().accessOpenAPI().getLobby()
            }
            showLobby(lobby)

            val activeGames = withContext(Dispatchers.IO) {
                try {
                    if(DataRepository.getInstance().credentialsAvailable(requireContext()))
                        DataRepository.getInstance().accessRestrictedAPI(requireContext()).findActive()
                    else null
                }catch (e: UnauthorizedException){
                    e.printStackTrace()
                    DataRepository.getInstance().resetLogin()
                    null
                }
            }

            if(activeGames != null){
                showActiveGames(activeGames)
            }
        }catch (e: Exception){
            e.printStackTrace()
            if(_binding != null){
                binding.serverInfo.text = getMessage(e)
            }
        }
    }

    private fun getMessage(e: Exception) = if(e is NotFoundException) getString(R.string.serverOffline) else e.message

    private fun showInfo(info: ServerInfo){
        binding.serverInfo.text = getString(R.string.serverStatusMessage, info.playerCount, info.gameCount)
        
        val highPriorityAnnouncements = info.announcements.filter { it.priority == 1 }
        val lowPriorityAnnouncements = info.announcements.filter { it.priority == 0 }
        
        highPriorityAnnouncementAdapter.updateList(highPriorityAnnouncements)
        lowPriorityAnnouncementAdapter.updateList(lowPriorityAnnouncements)
    }

    private fun showLobby(requestLobby: RequestLobby){
        binding.requestsParent.removeAllViews()
        requestLobby.requests.forEach { request: GameRequest ->
            val requestView = GameRequestView(requireActivity(), binding.requestsParent, request, this)
            binding.requestsParent.addView(requestView.getView())
        }
        requestLobby.dailyRequests.forEach { dailyRequest: DailyGameRequest ->
            val dailyRequestView = DailyGameRequestView(requireActivity(), binding.requestsParent, dailyRequest, this)
            binding.requestsParent.addView(dailyRequestView.getView())
        }
    }

    private fun showActiveGames(games: Games){
        val currentUsername = if(DataRepository.getInstance().credentialsAvailable(requireContext())) {
            CredentialsStorage(requireContext()).getUsername()
        } else null
        
        viewLifecycleOwner.lifecycleScope.launch {
            val allGames = mutableListOf<GameInfo>()
            allGames.addAll(games.games)
            
            val cachedGameIds = gameCache.getCachedGameIds()
            val activeGameIds = games.games.map { it.gameId }.toSet()
            val cachedGamesNotInActive = cachedGameIds.filterNot { activeGameIds.contains(it) }
            
            if (cachedGamesNotInActive.isNotEmpty() && currentUsername != null) {
                try {
                    val cachedGameInfos = withContext(Dispatchers.IO) {
                        cachedGamesNotInActive.mapNotNull { gameId ->
                            try {
                                DataRepository.getInstance().accessOpenAPI().getGameInfo(gameId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }
                    }
                    allGames.addAll(cachedGameInfos)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val sortedGames = if (currentUsername != null) {
                allGames.sortedBy { if(isCurrentUserAtMove(it, currentUsername)) -1 else 1 }
            } else {
                allGames
            }

            binding.homeActiveGames.adapter = GameAdapter(sortedGames.toMutableList(), this@HomeFragment, currentUsername)
        }
    }

    private fun isCurrentUserAtMove(game: GameInfo, username: String): Boolean {
        val colorAtMove = game.toGame().currentState.atMove
        return when (colorAtMove) {
            true -> game.white.username == username
            false -> game.black.username == username
        }
    }

    override fun onClick(configuration: GameConfiguration) {
        if(checkCredentials()){
            when {
                configuration.isCustomGame() -> {
                    CustomGameConfigurationDialog(requireActivity(), this)
                }
                configuration.isDailyGame() -> {
                    DailyRequestAddDialog(requireActivity(), configuration)
                }
                else -> {
                    GameRequestAddDialog(requireActivity(), R.id.action_nav_home_to_nav_game, configuration)
                }
            }
        }
    }


    override fun onChoose(configuration: GameConfiguration) {
        if(configuration.isDailyGame()) {
            DailyRequestAddDialog(requireActivity(), configuration)
        } else {
            GameRequestAddDialog(requireActivity(), R.id.action_nav_home_to_nav_game, configuration)
        }
    }

    override fun onClick(gameRequest: GameRequest) {
        if(checkCredentials()){
            GameRequestAcceptDialog(requireActivity(), R.id.action_nav_home_to_nav_game, gameRequest)
        }
    }

    override fun onClick(dailyGameRequest: DailyGameRequest) {
        if(checkCredentials()){
            val currentUsername = CredentialsStorage(requireContext()).getUsername()
            val isOwnRequest = dailyGameRequest.requester.username == currentUsername
            
            if(isOwnRequest) {
                showCancelDailyGameDialog(dailyGameRequest)
            } else {
                showAcceptDailyGameDialog(dailyGameRequest)
            }
        }
    }

    private fun showCancelDailyGameDialog(dailyGameRequest: DailyGameRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cancelDailyGameRequest)
            .setMessage(R.string.cancelDailyGameRequestMessage)
            .setPositiveButton(R.string.yes) { _, _ ->
                cancelDailyGameRequest(dailyGameRequest)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showAcceptDailyGameDialog(dailyGameRequest: DailyGameRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.acceptDailyGameRequest)
            .setMessage(R.string.acceptDailyGameRequestMessage)
            .setPositiveButton(R.string.yes) { _, _ ->
                acceptDailyGameRequest(dailyGameRequest)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun cancelDailyGameRequest(dailyGameRequest: DailyGameRequest) {
        thread {
            try {
                DataRepository.getInstance().cancelDailyGameRequest(requireContext(), dailyGameRequest)
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), R.string.dailyGameRequestCancelled, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), getString(R.string.errorCancellingDailyGame, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun acceptDailyGameRequest(dailyGameRequest: DailyGameRequest) {
        thread {
            try {
                val result = DataRepository.getInstance().acceptDailyGameRequest(requireContext(), dailyGameRequest)
                activity?.runOnUiThread {
                    if(result.gameId > 0) {
                        Toast.makeText(requireContext(), R.string.dailyGameAccepted, Toast.LENGTH_SHORT).show()
                        // Navigate to the game
                        val bundle = Bundle()
                        bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, result.gameId)
                        findNavController().navigate(R.id.action_nav_home_to_nav_game, bundle)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), getString(R.string.errorAcceptingDailyGame, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkCredentials(): Boolean {
        return if(DataRepository.getInstance().credentialsAvailable(requireContext())){
            true
        }else{
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            false
        }
    }

    private fun checkFirstStart() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if(prefs.getBoolean("firstStart", true)){
            prefs.edit().putBoolean("firstStart", false).apply()
            showTutorialDialog()
        }
    }

    private fun showTutorialDialog() {
        AlertDialog.Builder(activity)
                .setTitle(R.string.tutorialTitle)
                .setMessage(R.string.tutorialQuestion)
                .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int -> openTutorial() }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun openTutorial() {
        findNavController().navigate(R.id.action_nav_home_to_nav_tutorial)
    }

    override fun onClick(gameInfo: GameInfo) {
        val bundle = Bundle()
        bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, gameInfo.gameId)
        findNavController().navigate(R.id.action_nav_home_to_nav_game, bundle)
    }

    override fun onUrlClick(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        activity?.startActivity(intent)
    }

}