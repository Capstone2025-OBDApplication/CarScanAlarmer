package com.example.canstone2

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class CustomGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    enum class GraphType { SPEED, RPM, BRAKE, ACCEL}

    data class GraphInfo(
        val label: String,
        val color: Int,
        var isVisible: Boolean = true
    )

    private lateinit var chart: LineChart

    private lateinit var graphInfoMap: Map<GraphType, GraphInfo>
    private lateinit var graphButtons: Map<GraphType, CheckBox>
    private lateinit var graphDataSets: Map<GraphType, LineDataSet>

    private lateinit var lineData: LineData
    private val highlightList = mutableListOf<Float>()

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.custom_graph_view, this, true)

        chart = findViewById(R.id.graphChart)
        chart.description.isEnabled = false

        graphButtons = mapOf(
            GraphType.SPEED to findViewById(R.id.btnSpeed),
            GraphType.RPM to findViewById(R.id.btnRpm),
            GraphType.BRAKE to findViewById(R.id.btnBrake),
            GraphType.ACCEL to findViewById(R.id.btnAccel)
        )

        graphInfoMap = mapOf(
            GraphType.SPEED to GraphInfo("속도", ContextCompat.getColor(context, R.color.orange)),
            GraphType.RPM to GraphInfo("RPM", ContextCompat.getColor(context, R.color.green)),
            GraphType.BRAKE to GraphInfo("브레이크", ContextCompat.getColor(context, R.color.another_blue)),
            GraphType.ACCEL to GraphInfo("엑셀", ContextCompat.getColor(context, R.color.purple))
        )

        graphDataSets = graphInfoMap.mapValues { (type, info) ->
            LineDataSet(mutableListOf(), info.label).apply {
                color = info.color
                setCircleColor(info.color)
                lineWidth = 2f
                setDrawCircles(false)
                isVisible = info.isVisible
            }
        }

        lineData = LineData(graphDataSets.values.toList())
        lineData.setDrawValues(false)
        initChart()
        setupButtons()

        chart.setRenderer(CustomLineChartRenderer(chart, chart.animator, chart.viewPortHandler))
        chart.invalidate()
    }

    private fun initChart() {
        chart.data = lineData
        chart.isDragEnabled = true
        chart.setScaleEnabled(false)
        chart.legend.isEnabled = false

        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 250f
        chart.axisRight.axisMaximum = 200f
    }

    private fun setupButtons() {
        graphButtons.forEach { (type, button) ->
            graphInfoMap[type]?.let { info ->
                CompoundButtonCompat.setButtonTintList(
                    button,
                    ColorStateList.valueOf(info.color)
                )
                button.setOnCheckedChangeListener { _, isChecked ->
                    info.isVisible = isChecked
                    graphDataSets[type]?.isVisible = isChecked
                    updateButtonUI(type)
                    updateChart()

                }
                updateButtonUI(type)
            }
        }
    }

    private fun updateButtonUI(type: GraphType) {
        val info = graphInfoMap[type] ?: return
        val button = graphButtons[type] ?: return
        button.setTextColor(
            if (info.isVisible) ContextCompat.getColor(context, R.color.black) else ContextCompat.getColor(context, R.color.gray)
        )
        CompoundButtonCompat.setButtonTintList(
            button,
            ColorStateList.valueOf(
                if (info.isVisible) info.color else ContextCompat.getColor(context, R.color.gray)
            )
        )
    }

    private fun updateChart() {
        lineData.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    fun updateData(
        xValue: Float,
        speed: Float? = null,
        brake: Float? = null,
        accel: Float? = null,
        rpm: Float? = null,
        highlight: Boolean = false
    ) {
        speed?.let { graphDataSets[GraphType.SPEED]?.addEntry(Entry(xValue, it)) }
        brake?.let { graphDataSets[GraphType.BRAKE]?.addEntry(Entry(xValue, it)) }
        accel?.let { graphDataSets[GraphType.ACCEL]?.addEntry(Entry(xValue, it)) }
        rpm?.let { graphDataSets[GraphType.RPM]?.addEntry(Entry(xValue, it)) }

        if (highlight) highlightList.add(xValue)

        if (xValue > chart.xAxis.axisMaximum) {
            chart.xAxis.axisMaximum = xValue
        }

        chart.moveViewToX(xValue)
        updateChart()
    }

    fun setDataLists(
        speedList: List<Entry>,
        brakeList: List<Entry>,
        accelList: List<Entry>,
        rpmList: List<Entry>,
        highlightXs: List<Float> = emptyList()
    ) {
        graphDataSets[GraphType.SPEED]?.apply {
            clear()
            values = speedList
        }
        graphDataSets[GraphType.BRAKE]?.apply {
            clear()
            values = brakeList
        }
        graphDataSets[GraphType.ACCEL]?.apply {
            clear()
            values = accelList
        }
        graphDataSets[GraphType.RPM]?.apply {
            clear()
            values = rpmList
        }

        highlightList.clear()
        highlightList.addAll(highlightXs)

        chart.xAxis.axisMaximum = (speedList.maxOfOrNull { it.x } ?: 0f).coerceAtLeast(10f)

        updateChart()
    }

    fun setHighlightList(list: List<Float>) {
        highlightList.clear()
        highlightList.addAll(list)
        chart.invalidate()
    }

    inner class CustomLineChartRenderer(
        chart: LineChart,
        animator: com.github.mikephil.charting.animation.ChartAnimator,
        viewPortHandler: com.github.mikephil.charting.utils.ViewPortHandler
    ) : com.github.mikephil.charting.renderer.LineChartRenderer(chart, animator, viewPortHandler) {
        override fun drawExtras(c: Canvas) {
            super.drawExtras(c)
            highlightList.forEach { centerX ->
                val left = chart.getTransformer(chart.axisLeft.axisDependency)
                    .getPixelForValues(centerX - 3, 0f).x.toFloat()
                val right = chart.getTransformer(chart.axisLeft.axisDependency)
                    .getPixelForValues(centerX + 3, 0f).x.toFloat()
                val top = chart.viewPortHandler.contentTop()
                val bottom = chart.viewPortHandler.contentBottom()

                val shader = LinearGradient(
                    left, top, left, bottom,
                    Color.argb(100, 255, 0, 0),
                    Color.argb(0, 255, 0, 0),
                    Shader.TileMode.CLAMP
                )

                val paint = Paint().apply {
                    style = Paint.Style.FILL
                    this.shader = shader
                }
                c.drawRect(left, top, right, bottom, paint)
            }
        }
    }
}