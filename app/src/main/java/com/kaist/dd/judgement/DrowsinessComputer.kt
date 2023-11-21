package com.kaist.dd.judgement

import java.time.LocalDateTime

class DrowsinessComputer(
    private val maxHistory: Int = 3,
    private val minutes: Long = 5,
    private val seconds: Long = 0
) {
    enum class Status(val value: Int) {
        STATUS_AWAKE(-1), STATUS_CAUTION(0), STATUS_WARNING(1), STATUS_DANGER(2);

        companion object {
            fun fromInt(value: Int): Status {
                return Status.values().first { it.value == value }
            }
        }
    }

    private val drowsinessDuration: Long = 1000000000
    private var firstDetectedTime: LocalDateTime? = null
    public var historyList = mutableListOf<LocalDateTime>()

    fun resetDetect() {
        firstDetectedTime = null
    }

    fun apply(now: LocalDateTime) {
        if (firstDetectedTime == null) {
            firstDetectedTime = now
        } else {
            val base = now.minusNanos(drowsinessDuration)
            if (firstDetectedTime?.isBefore(base) == true) {
                addHistory(now)
                firstDetectedTime = null
            }
        }
    }

    fun judge(now: LocalDateTime): Status {
        filterHistory(now)
        if (historyList.size > 0) {
            return Status.fromInt(historyList.size-1)
        }
        return Status.STATUS_AWAKE
    }

    private fun addHistory(now: LocalDateTime) {
        if (historyList.size >= 3) {
            historyList =
                historyList.subList(historyList.size - 2, historyList.size)
        }
        historyList.add(now)
    }

    private fun filterHistory(now: LocalDateTime) {
        val base = now.minusMinutes(minutes).minusSeconds(seconds + 1)
        this.historyList =
            this.historyList.filter { base.isBefore(it) } as ArrayList<LocalDateTime>
    }
}