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
    private var blueDates = listOf<String>()
    private var redDates = listOf<String>()
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

            val calendarForDay = Calendar.getInstance().apply {
                time = currentCalendar.time
                set(Calendar.DAY_OF_MONTH, day)
            }
            val fireDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fullDate = fireDateFormat.format(calendarForDay.time)

            val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == day &&
                    selectedDate.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                    selectedDate.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)

            var textColor = Color.BLACK

            when {
                blueDates.contains(fullDate) -> {
                    if (isSelected) {
                        holder.dayText.setBackgroundResource(R.drawable.select_day_blue)
                        textColor = Color.WHITE
                        drivingTable?.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
                    } else {
                        holder.dayText.setBackgroundResource(0)
                        textColor = Color.BLUE
                    }
                }

                redDates.contains(fullDate) -> {
                    if (isSelected) {
                        holder.dayText.setBackgroundResource(R.drawable.select_day_red)
                        textColor = Color.WHITE
                        drivingTable?.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
                    } else {
                        holder.dayText.setBackgroundResource(0)
                        textColor = Color.RED
                    }
                }

                isSelected -> {
                    holder.dayText.setBackgroundResource(R.drawable.select_day_blue)
                    textColor = Color.WHITE
                }

                else -> {
                    holder.dayText.setBackgroundResource(0)
                    textColor = Color.GRAY
                }
            }

            holder.dayText.setTextColor(textColor)

            holder.itemView.setOnClickListener {
                selectedDate.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
                selectedDate.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
                selectedDate.set(Calendar.DAY_OF_MONTH, day)
                updateCalendar()

                dateSelectedListener?.onDateSelected(
                    selectedDate,
                    fireDateFormat.format(selectedDate.time)
                )
            }
        }


    }

    fun setAvailableDates(blueDateList: List<String>, redDateList: List<String>) {
        blueDates = blueDateList
        redDates = redDateList

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
