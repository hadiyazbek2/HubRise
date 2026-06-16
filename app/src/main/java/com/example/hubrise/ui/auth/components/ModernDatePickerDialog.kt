package com.example.hubrise.ui.auth.components

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ModernDatePickerDialog(
    context: Context,
    initialYear: Int = 2000,
    initialMonth: Int = 0,
    initialDay: Int = 1,
    onDateSelected: (String) -> Unit
) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        val datePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                val selectedDate = LocalDate.of(year, month + 1, day)
                onDateSelected(selectedDate.format(formatter))
            },
            initialYear,
            initialMonth,
            initialDay
        )

        // Customize DatePickerDialog to show max date (today)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Show the dialog
        datePickerDialog.show()
    }
}

// Helper function to parse date string to components
fun parseDateString(dateString: String): Triple<Int, Int, Int>? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(dateString, formatter)
        Triple(date.year, date.monthValue - 1, date.dayOfMonth)
    } catch (e: Exception) {
        null
    }
}
