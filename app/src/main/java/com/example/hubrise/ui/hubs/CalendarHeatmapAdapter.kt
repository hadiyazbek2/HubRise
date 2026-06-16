package com.example.hubrise.ui.hubs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hubrise.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendarDay(val date: LocalDate, val isCheckedIn: Boolean)

/** Renders the last 28 days (4 weeks) as a 7-column grid, oldest first. */
class CalendarHeatmapAdapter : RecyclerView.Adapter<CalendarHeatmapAdapter.ViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitCheckinDates(checkinDates: List<String>) {
        val checkedSet = checkinDates.toHashSet()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        days = (27 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            CalendarDay(date, checkedSet.contains(date.format(formatter)))
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDay: TextView = view.findViewById(R.id.tv_day)

        fun bind(day: CalendarDay) {
            tvDay.text = day.date.dayOfMonth.toString()
            tvDay.setBackgroundResource(
                if (day.isCheckedIn) R.drawable.calendar_cell_checked else R.drawable.calendar_cell_empty
            )
            tvDay.setTextColor(
                tvDay.context.getColor(if (day.isCheckedIn) R.color.white else R.color.text_secondary)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(days[position])

    override fun getItemCount() = days.size
}
