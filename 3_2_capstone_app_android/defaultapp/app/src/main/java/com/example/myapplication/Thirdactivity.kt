package com.example.myapplication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*


class ThirdActivity : AppCompatActivity() {

    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var calculateButton: Button
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView
    private lateinit var resultTextView: TextView

    private var startDateTime: Calendar = Calendar.getInstance()
    private var endDateTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)

        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        calculateButton = findViewById(R.id.calculateButton)
        startDateTextView = findViewById(R.id.startDateTextView)
        endDateTextView = findViewById(R.id.endDateTextView)
        resultTextView = findViewById(R.id.resultTextView)

        // 시작 날짜 및 시간 설정 버튼
        startDateButton.setOnClickListener {
            pickDateTime(startDateTime) { dateTime ->
                startDateTime = dateTime
                startDateTextView.text = formatDateTime(dateTime)
            }
        }

        // 종료 날짜 및 시간 설정 버튼
        endDateButton.setOnClickListener {
            pickDateTime(endDateTime) { dateTime ->
                endDateTime = dateTime
                endDateTextView.text = formatDateTime(dateTime)
            }
        }

        // 결과 계산 버튼
        calculateButton.setOnClickListener {
            calculateResults()
        }
    }


    // 날짜 및 시간 선택 함수
    private fun pickDateTime(calendar: Calendar, onDateTimePicked: (Calendar) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                onDateTimePicked(calendar)
            }, hour, minute, true).show()
        }, year, month, day).show()
    }

    // 날짜 및 시간 포맷팅 함수
    private fun formatDateTime(calendar: Calendar): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(calendar.time)
    }

    // 결과 계산 함수 (임시 더미 데이터 사용)
    private fun calculateResults() {
        val startDate = formatDateTime(startDateTime) // "yyyy-MM-dd HH:mm" 형식
        val endDate = formatDateTime(endDateTime)

        val dbHelper = AlarmDatabaseHelper(this)

        // 시작 시간의 count 값
        val startCount1 = dbHelper.getClosestCount("Arduino_1", startDate)
        val startCount2 = dbHelper.getClosestCount("Arduino_2", startDate)
        val startCount3 = dbHelper.getClosestCount("Arduino_3", startDate)

        // 종료 시간의 count 값
        val endCount1 = dbHelper.getClosestCount("Arduino_1", endDate)
        val endCount2 = dbHelper.getClosestCount("Arduino_2", endDate)
        val endCount3 = dbHelper.getClosestCount("Arduino_3", endDate)


        // 차이 계산
        val countDifference1 = endCount1 - startCount1
        val countDifference2 = endCount2 - startCount2
        val countDifference3 = endCount3 - startCount3


        // 결과 출력
        resultTextView.text = "변기 1 교체 횟수: $countDifference1\n" +
                "변기 2 교체 횟수: $countDifference2\n" +
                "변기 3 교체 횟수: $countDifference3"
    }

    // 더미 데이터를 사용한 카운트 계산 함수
    private fun getCountForDateRange(startDate: String, endDate: String): Int {
        // 예시: 실제 구현 시 SQLite 데이터베이스 연동 필요
        return (1..10).random() // 1~10 사이의 임의의 값 반환
    }
}
