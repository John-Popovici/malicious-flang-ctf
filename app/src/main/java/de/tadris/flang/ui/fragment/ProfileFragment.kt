package de.tadris.flang.ui.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import de.tadris.flang.R
import de.tadris.flang.databinding.FragmentProfileBinding
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.Rating
import de.tadris.flang.network_api.model.User
import de.tadris.flang.ui.adapter.GameAdapter
import de.tadris.flang.ui.adapter.RatingAdapter
import de.tadris.flang.ui.dialog.LoadingDialogViewController
import de.tadris.flang.ui.view.ChartFormatter
import de.tadris.flang.ui.view.ChartFormatter.initChart
import de.tadris.flang.ui.view.addBottomPadding
import de.tadris.flang.util.applyTo
import de.tadris.flang.util.getThemePrimaryColor
import de.tadris.flang.util.getThemeTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment(R.layout.fragment_profile), GameAdapter.GameAdapterListener {

    companion object {
        const val ARGUMENT_USERNAME = "username"
        private const val PAGE_SIZE = 10
    }

    init {
        setHasOptionsMenu(true)
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


    private lateinit var gameAdapter: GameAdapter
    private lateinit var username: String

    private var user: User? = null

    private var loading = false
    private var reachedTheEnd = false
    private var currentOffset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString(ARGUMENT_USERNAME) ?: CredentialsStorage(requireContext()).getUsername()
        loadNewAdapter()
    }

    private fun loadNewAdapter(){
        currentOffset = 0
        reachedTheEnd = false
        val currentUsername = CredentialsStorage(requireContext()).getUsername()
        gameAdapter = GameAdapter(mutableListOf(), this, currentUsername)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!
        _binding = FragmentProfileBinding.bind(root)

        binding.profileContent.addBottomPadding()

        val layoutManager = if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            GridLayoutManager(requireContext(), 2)
        }else{
            LinearLayoutManager(requireContext())
        }
        binding.profileGamesRecyclerView.layoutManager = layoutManager
        binding.profileGamesRecyclerView.adapter = gameAdapter
        binding.ratingsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)

        root.findViewById<NestedScrollView>(R.id.nestedScrollView).setOnScrollChangeListener { v: ViewGroup, _, scrollY, _, _ ->
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight)){
                loadMore()
            }
        }

        binding.profileSwipeToRefresh.setOnRefreshListener {
            loadNewAdapter()
            binding.profileGamesRecyclerView.adapter = gameAdapter
            loadMore()
        }

        (activity as AppCompatActivity).supportActionBar?.title = username

        if(gameAdapter.itemCount < 5){
            loadMore()
        }

        with(binding.profileChart) {
            with(ChartFormatter){
                initChart(requireActivity())
                applyDateFormatter()
            }
            description.text = getString(R.string.ratingHistory)
        }

        loadUserInfo()

        return root
    }

    private fun loadMore(){
        if(loading){ return }
        if(reachedTheEnd){ return }
        loading = true
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                binding.profileSwipeToRefresh.isRefreshing = true
                val newGames = findGames()
                currentOffset+= PAGE_SIZE
                if(newGames.games.isEmpty()){
                    reachedTheEnd = true
                }
                gameAdapter.appendGames(newGames.games)
            }catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }finally {
                binding.profileSwipeToRefresh.isRefreshing = false
                loading = false
            }
        }
    }

    private fun loadUserInfo(){
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                user = getUserInfo()
                updateInfoAndChart()
            }catch (e: Exception){
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateInfoAndChart(){
        updateInfo()
        updateChart()
    }

    private fun updateInfo(){
        val user: User = this.user!!
        user.applyTo(binding.userTitle, binding.userUsername, null)
        binding.userSummary.text = getString(R.string.userSummary,
            SimpleDateFormat.getDateInstance().format(Date(user.registration)),
            user.completedGames)
        
        val onlineColor = if (user.online) {
            requireContext().getColor(R.color.green_400)
        } else {
            requireContext().getColor(R.color.grey_400)
        }
        binding.onlineStatusIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(onlineColor)

        // Display ratings by type
        if (user.ratings.isNotEmpty()) {
            binding.ratingsSection.visibility = View.VISIBLE
            val sortedRatings = sortRatingsByType(user.ratings)
            val ratingAdapter = RatingAdapter(requireContext(), sortedRatings)
            binding.ratingsRecyclerView.adapter = ratingAdapter
        } else {
            binding.ratingsSection.visibility = View.GONE
        }
    }

    private fun updateChart(){
        val chart = binding.profileChart
        val user: User = this.user!!

        // Group history by rating type
        val historyByType = user.history.groupBy { it.type }

        if(historyByType.values.all { it.size < 2 }){
            chart.visibility = View.GONE
            return
        } else {
            chart.visibility = View.VISIBLE
        }

        val dataSets = mutableListOf<LineDataSet>()

        historyByType.forEach { (type, history) ->
            if (history.size >= 2) {
                val entries = history.map { Entry(it.date.toFloat(), it.rating) } + listOf(Entry(System.currentTimeMillis().toFloat(), history.last().rating))
                val dataSet = LineDataSet(entries, getRatingTypeDisplayName(type))
                dataSets.add(dataSet)
            }
        }

        if (dataSets.isNotEmpty()) {
            ChartFormatter.fillChart(requireActivity(), chart, dataSets, LineDataSet.Mode.STEPPED)
        }
    }

    private fun sortRatingsByType(ratings: List<Rating>): List<Rating> {
        val typeOrder = listOf(
            Rating.TYPE_BULLET,
            Rating.TYPE_BLITZ,
            Rating.TYPE_CLASSICAL,
            Rating.TYPE_DAILY,
            Rating.TYPE_PUZZLE
        )

        return ratings.sortedWith { a, b ->
            val aIndex = typeOrder.indexOf(a.type)
            val bIndex = typeOrder.indexOf(b.type)

            when {
                aIndex != -1 && bIndex != -1 -> aIndex.compareTo(bIndex)
                aIndex != -1 && bIndex == -1 -> -1  // Known types before unknown
                aIndex == -1 && bIndex != -1 -> 1   // Unknown types after known
                else -> a.type.compareTo(b.type)    // Unknown types sorted alphabetically
            }
        }
    }

    private fun getRatingTypeDisplayName(type: String): String {
        return when (type) {
            Rating.TYPE_BULLET -> getString(R.string.ratingTypeBullet)
            Rating.TYPE_BLITZ -> getString(R.string.ratingTypeBlitz)
            Rating.TYPE_CLASSICAL -> getString(R.string.ratingTypeClassical)
            Rating.TYPE_DAILY -> getString(R.string.ratingTypeDaily)
            Rating.TYPE_PUZZLE -> getString(R.string.ratingTypePuzzle)
            else -> type.replaceFirstChar { it.uppercase() }
        }
    }

    @WorkerThread
    private suspend fun getUserInfo() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().findUser(username)
    }

    @WorkerThread
    private suspend fun findGames() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().findGames(username, pageSize = PAGE_SIZE, offset = currentOffset)
    }

    override fun onClick(gameInfo: GameInfo) {
        val bundle = Bundle()
        bundle.putLong(OnlineGameFragment.EXTRA_GAME_ID, gameInfo.gameId)
        findNavController().navigate(R.id.action_nav_profile_to_nav_game, bundle)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val credentialsStorage = CredentialsStorage(requireContext())
        // Only show admin menu if user is admin and viewing someone else's profile
        if (credentialsStorage.getRole() == CredentialsStorage.ROLE_ADMIN &&
            username != credentialsStorage.getUsername()) {
            inflater.inflate(R.menu.fragment_profile, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionChatBan -> {
                chatBanUser()
                true
            }
            R.id.actionUnban -> {
                unbanUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun chatBanUser() {
        val dialog = LoadingDialogViewController(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                performChatBan()
                dialog.hide()
                Toast.makeText(requireContext(), R.string.chatBanSuccess, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                dialog.hide()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun unbanUser() {
        val dialog = LoadingDialogViewController(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                performUnban()
                dialog.hide()
                Toast.makeText(requireContext(), R.string.unbanSuccess, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                dialog.hide()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    @WorkerThread
    private suspend fun performChatBan() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessRestrictedAPI(requireContext()).chatBanUser(user!!.username)
    }

    @WorkerThread
    private suspend fun performUnban() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessRestrictedAPI(requireContext()).unbanUser(user!!.username)
    }

}