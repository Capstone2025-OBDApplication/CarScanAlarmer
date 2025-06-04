package com.example.canstone2

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.core.content.ContextCompat

class CustomCalendarView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val currentCalendar = Calendar.getInstance()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var blueDates = listOf(1, 18, 22, 15)
    private var redDates = listOf(28)
    private var drivingTable: View? = null

    private lateinit var showCalendarButton: TextView
    private lateinit var calendarLayout: LinearLayout

    interface OnDateSelectedListener {
        fun onDateSelected(date: Calendar, formattedDate: String)
    }
    var dateSelectedListener: OnDateSelectedListener? = null
    fun setOnDateSelectedListener(listener: OnDateSelectedListener) {
        dateSelectedListener = listener
    }
    fun setDrivingTable(view: View) {
        drivingTable = view
    }
    init {
        // custom_calendar_view.xml 레이아웃 인플레이트
        LayoutInflater.from(context).inflate(R.layout.custom_calendar_view, this, true)

        // 인플레이트 후에 findViewById로 가져와야 함!
        showCalendarButton = findViewById(R.id.showCalendarButton)
        calendarLayout = findViewById(R.id.calendarLayout)
        calendarLayout.visibility = View.GONE
        // 내부 헤더의 버튼에 클릭 이벤트 연결
        val headerPrevButton = findViewById<ImageButton>(R.id.prevButton)
        val headerNextButton = findViewById<ImageButton>(R.id.nextButton)
        headerPrevButton.setOnClickListener { previousMonth() }
        headerNextButton.setOnClickListener { nextMonth() }

        // 여기서 클릭 이벤트 연결
        showCalendarButton.setOnClickListener {
            calendarLayout.visibility = View.VISIBLE
            showCalendarButton.visibility = View.GONE
        }

        updateCalendar()
    }


    private fun updateCalendar() {
        val monthTitle = findViewById<TextView>(R.id.monthTitle)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // 년/월 텍스트 업데이트
        val monthFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREA)
        monthTitle.text = monthFormat.format(currentCalendar.time)

        // 선택된 날짜 텍스트 업데이트
        val selectedDateText = findViewById<TextView>(R.id.selectedDateText)
        val fullDateFormat = SimpleDateFormat("yyyy. M. d.(E)", Locale.KOREA)
        selectedDateText.text =fullDateFormat.format(selectedDate.time)

        // 캘린더 날짜 목록 생성
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayList = mutableListOf<Int>()

        val tempCalendar = Calendar.getInstance().apply {
            time = currentCalendar.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
        val emptyDays = firstDayOfWeek - Calendar.SUNDAY

        for (i in 0 until emptyDays) {
            dayList.add(0) // 0은 빈 칸
        }

        for (i in 1..daysInMonth) {
            dayList.add(i)
        }

        recyclerView.layoutManager = GridLayoutManager(context, 7)
        recyclerView.adapter = CalendarAdapter(dayList)
    }

    inner class CalendarAdapter(private val days: List<Int>) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

        inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val dayText: TextView = itemView.findViewById(R.id.dayText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.calendar_day_item, parent, false)
            return CalendarViewHolder(view)
        }

        override fun getItemCount(): Int = days.size

        override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
            val day = days[position]
            if (day == 0) {
                holder.dayText.text = ""
                holder.dayText.setBackgroundResource(0)
                holder.dayText.setTextColor(Color.TRANSPARENT)
                holder.itemView.setOnClickListener(null)
                return
            }
            holder.dayText.text = day.toString()
            //val drivingTable = findViewById<ConstraintLayout>(R.id.gridContainer)


            val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == day
            var textColor = Color.BLACK

            // 조건에 따라 날짜 텍스트 색상은 변경하지만, 배경은 선택된 경우에만 적용합니다.
            when {
                blueDates.contains(day) -> {
                    if (isSelected) {
                        // 선택된 경우: 배경은 rounded_bg (blue 스타일, 아이콘이 흰색)
                        holder.dayText.setBackgroundResource(R.drawable.select_day_blue)
                        textColor = Color.WHITE
                        drivingTable?.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.blue)
                        )

                    } else {
                        // 선택되지 않은 경우: 배경 없음, 텍스트는 blue
                        holder.dayText.setBackgroundResource(0)
                        textColor = Color.BLUE
                    }
                }

                redDates.contains(day) -> {
                    if (isSelected) {
                        // 선택된 경우: 배경은 rounded_bg (red 스타일, 아이콘이 흰색)
                        holder.dayText.setBackgroundResource(R.drawable.select_day_red)
                        textColor = Color.WHITE
                        drivingTable?.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.red)
                        )
                    } else {
                        // 선택되지 않은 경우: 배경 없음, 텍스트는 red
                        holder.dayText.setBackgroundResource(0)
                        textColor = Color.RED
                    }
                }

                isSelected -> {
                    // 그 외의 날짜 중 선택된 경우: 회색 배경, 흰색 텍스트
                    holder.dayText.setBackgroundResource(R.drawable.select_day_blue)
                    textColor = Color.WHITE
                    //drivingTable.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))

                }

                else -> {
                    // 선택되지 않은 경우: 배경 없음, 텍스트는 검정색
                    holder.dayText.setBackgroundResource(0)
                    textColor = Color.GRAY
                }
            }

            holder.dayText.setTextColor(textColor)

            // 날짜 선택 시 updateCalendar() 호출
            holder.itemView.setOnClickListener {
                selectedDate.set(Calendar.DAY_OF_MONTH, day)
                updateCalendar()
                val fireDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = fireDateFormat.format(selectedDate.time)
                dateSelectedListener?.onDateSelected(selectedDate, formattedDate)
            }
        }


    }

    fun setAvailableDates(blueDateList: List<String>, redDateList: List<String>) {
        val calendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun extractDays(dateList: List<String>): List<Int> {
            return dateList.mapNotNull {
                try {
                    calendar.time = sdf.parse(it)!!
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    if (year == currentYear && month == currentMonth) day else null
                } catch (e: Exception) {
                    Log.e("CustomCalendarView", "날짜 파싱 오류: $it")
                    null
                }
            }.distinct().sorted()
        }

        blueDates = extractDays(blueDateList)
        redDates = extractDays(redDateList)

        updateCalendar()
    }
    fun previousMonth() {
        currentCalendar.add(Calendar.MONTH, -1)
        updateCalendar()
    }

    fun nextMonth() {
        currentCalendar.add(Calendar.MONTH, 1)
        updateCalendar()
    }


}
