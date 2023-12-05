package com.kaist.dd

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AlertDataAdapter (val context: Context) :
    RecyclerView.Adapter<AlertDataAdapter.ViewHolder> () {

    var datas = mutableListOf<AlertData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.log_recycler_item,parent,false)
        return ViewHolder(view)
    }
    override fun getItemCount() : Int = datas.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(datas[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgProfile: ImageView = itemView.findViewById(R.id.label_level_img)
        private val textLevel: TextView = itemView.findViewById(R.id.label_level_text)
        private val textDate: TextView = itemView.findViewById(R.id.label_date_text)
        private val textRunning: TextView = itemView.findViewById(R.id.label_running_text)

        fun bind(item: AlertData) {
            val img = when (item.level) {
                1 -> R.drawable.icon_one
                2 -> R.drawable.icon_two
                3 -> R.drawable.icon_three
                else -> ""
            }
            var levelText = when (item.level) {
                1 -> "CAUTION"
                2 -> "WARNING"
                3 -> "DANGER"
                else -> ""
            }
            Glide.with(itemView).load(img).into(imgProfile)
            textLevel.text = levelText
            textDate.text = item.timestamp
            textRunning.text = context.getString(R.string.running_time) + ": " + item.running
        }
    }
}