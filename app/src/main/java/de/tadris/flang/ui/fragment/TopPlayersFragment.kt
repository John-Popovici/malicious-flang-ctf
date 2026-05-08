package de.tadris.flang.ui.fragment

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.ChipGroup
import de.tadris.flang.R
import de.tadris.flang.network.CredentialsStorage
import de.tadris.flang.network.DataRepository
import de.tadris.flang.network_api.model.DailyStatistics
import de.tadris.flang.network_api.model.GameInfo
import de.tadris.flang.network_api.model.RatingType
import de.tadris.flang.network_api.model.UserInfo
import de.tadris.flang.ui.adapter.GameAdapter
import de.tadris.flang.ui.adapter.UserAdapter
import de.tadris.flang.ui.view.ChartFormatter
import de.tadris.flang.ui.view.ChartFormatter.initChart
import de.tadris.flang.ui.view.addBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class TopPlayersFragment : Fragment(R.layout.fragment_top_players), UserAdapter.UserAdapterListener {

    companion object {
        const val averageStrength = 0.1f
    }

    init {
        setHasOptionsMenu(true)
    }

    private lateinit var topPlayersRecyclerView: RecyclerView
    private lateinit var onlinePlayersRecyclerView: RecyclerView
    private lateinit var gamesChart: CombinedChart
    private lateinit var playersChart: CombinedChart
    private lateinit var solvedPuzzlesChart: CombinedChart
    private lateinit var totalPuzzlesChart: LineChart
    private lateinit var ratingTypeChipGroup: ChipGroup

    private val topPlayersAdapter = UserAdapter(this)
    private val onlinePlayersAdapter = UserAdapter(this)

    private var selectedRatingType = RatingType.BLITZ

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)!!

        root.findViewById<View>(R.id.topPlayerContent).addBottomPadding()

        topPlayersRecyclerView = root.findViewById(R.id.topPlayersRecyclerView)
        topPlayersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        topPlayersRecyclerView.adapter = topPlayersAdapter

        onlinePlayersRecyclerView = root.findViewById(R.id.onlinePlayersRecyclerView)
        onlinePlayersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        onlinePlayersRecyclerView.adapter = onlinePlayersAdapter

        gamesChart = root.findViewById(R.id.topPlayerGamesChart)
        playersChart = root.findViewById(R.id.topPlayerPlayersChart)
        solvedPuzzlesChart = root.findViewById(R.id.topPlayerSolvedPuzzlesChart)
        totalPuzzlesChart = root.findViewById(R.id.topPlayerTotalPuzzlesChart)

        gamesChart.init()
        playersChart.init()
        solvedPuzzlesChart.init()
        totalPuzzlesChart.init()

        root.findViewById<View>(R.id.topPlayersHelp).setOnClickListener {
            openTopPlayersHelp()
        }

        ratingTypeChipGroup = root.findViewById(R.id.ratingTypeChipGroup)
        ratingTypeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedRatingType = when (checkedIds[0]) {
                    R.id.chipBullet -> RatingType.BULLET
                    R.id.chipClassical -> RatingType.CLASSICAL
                    else -> RatingType.BLITZ
                }
                refreshTopPlayers()
            }
        }

        return root
    }

    private fun BarLineChartBase<*>.init(){
        with(ChartFormatter){
            initChart(requireActivity())
        }
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return ChartFormatter.dateFormat.format(Date(
                    System.currentTimeMillis() + TimeUnit.DAYS.toMillis(value.toLong())
                ))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh(){
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                topPlayersAdapter.updateList(findTopPlayers())
                onlinePlayersAdapter.updateList(findOnlinePlayers())
                updateCharts(fetchStats())
            }catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openTopPlayersHelp(){
        AlertDialog.Builder(activity)
            .setTitle(R.string.topPlayers)
            .setMessage(R.string.topPlayersHint)
            .setPositiveButton(R.string.okay, null)
            .show()
    }

    private fun refreshTopPlayers() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                topPlayersAdapter.updateList(findTopPlayers())
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    @WorkerThread
    private suspend fun findTopPlayers() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().getTopPlayers(selectedRatingType.value)
    }

    @WorkerThread
    private suspend fun findOnlinePlayers() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().getOnlinePlayers()
    }

    @WorkerThread
    private suspend fun fetchStats() = withContext(Dispatchers.IO) {
        DataRepository.getInstance().accessOpenAPI().getStats()
    }

    override fun onClick(user: UserInfo) {
        showProfile(user.username)
    }

    private fun showProfile(username: String){
        val bundle = Bundle()
        bundle.putString(ProfileFragment.ARGUMENT_USERNAME, username)
        findNavController().navigate(R.id.action_nav_top_to_nav_profile, bundle)
    }

    private fun updateCharts(stats: DailyStatistics){
        val size = stats.stats.size
        ChartFormatter.fillChart(requireActivity(), gamesChart,
            BarDataSet(
                stats.stats.mapIndexed { index, entry -> BarEntry((index - size).toFloat(), entry.gamesLastDay.toFloat()) },
                getString(R.string.playerGames)
            ),
            LineDataSet(
                calculateFloatingAverage(stats.stats.map { it.gamesLastDay.toFloat() }),
                getString(R.string.floatingAverage)
            )
        )
        ChartFormatter.fillChart(requireActivity(), playersChart,
            BarDataSet(
                stats.stats.mapIndexed { index, entry -> BarEntry((index - size).toFloat(), entry.activePlayersLastDay.toFloat()) },
                getString(R.string.onlinePlayers)
            ),
            LineDataSet(
                calculateFloatingAverage(stats.stats.map { it.activePlayersLastDay.toFloat() }),
                getString(R.string.floatingAverage)
            )
        )
        ChartFormatter.fillChart(requireActivity(), solvedPuzzlesChart,
            BarDataSet(
                stats.stats.mapIndexed { index, entry -> BarEntry((index - size).toFloat(), entry.solvedPuzzles.toFloat()) },
                getString(R.string.solvedPuzzlesPerDay)
            ),
            LineDataSet(
                calculateFloatingAverage(stats.stats.map { it.solvedPuzzles.toFloat() }),
                getString(R.string.floatingAverage)
            )
        )
        ChartFormatter.fillChart(requireActivity(), totalPuzzlesChart, listOf(
            LineDataSet(
                stats.stats.mapIndexed { index, entry -> Entry((index - size).toFloat(), entry.totalPuzzles.toFloat()) },
                getString(R.string.puzzleCatalog)
            )
        ))
    }

    private fun calculateFloatingAverage(data: List<Float>): List<Entry> {
        var currentValue = data[0]
        return data.mapIndexed { index, value ->
            currentValue = (1f - averageStrength) * currentValue + averageStrength * value
            Entry((index - data.size).toFloat(), currentValue)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_top_players, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionSearchUsers -> {
                findNavController().navigate(R.id.action_nav_top_to_nav_user_search)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}