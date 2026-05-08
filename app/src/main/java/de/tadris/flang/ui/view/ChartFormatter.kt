package de.tadris.flang.ui.view

import android.app.Activity
import android.view.View
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import de.tadris.flang.R
import de.tadris.flang.util.getThemeColor
import de.tadris.flang.util.getThemePrimaryColor
import de.tadris.flang.util.getThemeSecondaryColor
import de.tadris.flang.util.getThemeTextColor
import java.text.SimpleDateFormat
import java.util.*

object ChartFormatter {

    val dateFormat = SimpleDateFormat("MMM")

    fun BarLineChartBase<*>.initChart(activity: Activity){
        val color = activity.getThemeTextColor()

        isScaleXEnabled = false
        isScaleYEnabled = false

        axisLeft.textColor = color
        axisRight.textColor = color
        xAxis.textColor = color
        legend.textColor = color
        description.textColor = color

        isHighlightPerDragEnabled = false
        isHighlightPerTapEnabled = false

        //chart.axisLeft.setDrawGridLines(false)
        //chart.axisRight.setDrawGridLines(false)
        setNoDataText("")
        description.text = ""
    }

    fun BarLineChartBase<*>.applyDateFormatter(){
        xAxis.valueFormatter = object : DefaultValueFormatter(1) {
            override fun getFormattedValue(value: Float): String {
                return dateFormat.format(Date(value.toLong()))
            }
        }
    }

    fun fillChart(activity: Activity, chart: LineChart, dataSets: List<LineDataSet>, mode: LineDataSet.Mode = LineDataSet.Mode.LINEAR){
        dataSets.forEachIndexed { index, dataSet ->
            dataSet.setDrawCircles(false)
            dataSet.color = when(index){
                1 -> activity.resources.getColor(R.color.chatPersonRed)
                2 -> activity.resources.getColor(R.color.chatPersonPurple)
                3 -> activity.resources.getColor(R.color.chatPersonLightBlue)
                4 -> activity.resources.getColor(R.color.chatPersonGreen)
                else -> activity.getThemePrimaryColor()
            }
            dataSet.lineWidth = 3f
            dataSet.mode = mode
        }

        val lineData = LineData(dataSets)
        lineData.setDrawValues(false)

        chart.data = lineData
        chart.invalidate()
    }

    fun fillChart(activity: Activity, chart: CombinedChart, barDataSet: BarDataSet, lineDataSet: LineDataSet){
        barDataSet.color = activity.getThemeSecondaryColor()
        val barData = BarData(barDataSet)
        barData.setDrawValues(false)
        barData.barWidth = 1.2f

        lineDataSet.color = activity.getThemePrimaryColor()
        lineDataSet.lineWidth = 3f
        lineDataSet.setDrawCircles(false)
        lineDataSet.mode = LineDataSet.Mode.LINEAR
        val lineData = LineData(lineDataSet)
        lineData.setDrawValues(false)

        val combinedData = CombinedData()
        combinedData.setData(barData)
        combinedData.setData(lineData)

        chart.data = combinedData
        chart.invalidate()
    }

}