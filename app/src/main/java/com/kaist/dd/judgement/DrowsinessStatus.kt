package com.kaist.dd.judgement

import com.kaist.dd.judgement.DrowsinessComputer.Status
import java.time.LocalDateTime
import kotlin.properties.Delegates

class DrowsinessStatus (
    val drowsinessStatusListener: DrowsinessStatusListener? = null
) {
    private var status: Status = Status.STATUS_AWAKE
    private var level by Delegates.notNull<Char>()
    private val drowsinessComputer = DrowsinessComputer(3, 0, seconds = 10)


    fun updateStatus(avgEAR: Double) {
        level = drowsinessComputer.measureLevel(avgEAR)

        //3 step detection
        val currentTime = LocalDateTime.now()
        if(level == 'c') {
            drowsinessComputer.apply(currentTime)
        } else {
            drowsinessComputer.resetDetect()
        }

        val currentStatus = drowsinessComputer.judge(currentTime)

        if (currentStatus != DrowsinessComputer.Status.STATUS_AWAKE &&
            status.value < currentStatus.value) {
            drowsinessStatusListener?.updateMoreDangerousStatus(currentStatus)
        }

        status = currentStatus
    }

    fun getStatus(): Status {
        return status
    }

    fun getStatusText(): String {
        return "%s-%c-%d".format(
            status.name, level,
            drowsinessComputer.historyList.size
        )
    }

    interface DrowsinessStatusListener {
        fun updateMoreDangerousStatus(status: Status)
    }
}