package de.tadris.flang.ui.fragment.analysis

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AnalysisPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AnalysisOverviewFragment.newInstance()
            1 -> AnalysisChartFragment.newInstance()
            2 -> AnalysisMovesFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}