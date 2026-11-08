package com.tourregister.ui.stops

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tourregister.R
import com.tourregister.data.entity.DetectedStop
import com.tourregister.util.DateUtils

class StopAdapter(private val onClassifyClick: (DetectedStop) -> Unit) : ListAdapter<DetectedStop, StopAdapter.ViewHolder>(object : DiffUtil.ItemCallback<DetectedStop>() {
    override fun areItemsTheSame(a: DetectedStop, b: DetectedStop) = a.id == b.id
    override fun areContentsTheSame(a: DetectedStop, b: DetectedStop) = a == b
}) {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvStopTime)
        val tvDuration: TextView = view.findViewById(R.id.tvStopDuration)
        val tvAddress: TextView = view.findViewById(R.id.tvStopAddress)
        val btnClassify: MaterialButton = view.findViewById(R.id.btnClassify)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_stop, parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stop = getItem(position)
        val dep = if (stop.departureTime > 0) DateUtils.formatTime(stop.departureTime) else "Now"
        holder.tvTime.text = "${DateUtils.formatTime(stop.arrivalTime)} — $dep"
        holder.tvDuration.text = DateUtils.formatDurationFromMillis(if (stop.departureTime > 0) stop.departureTime - stop.arrivalTime else System.currentTimeMillis() - stop.arrivalTime)
        holder.tvAddress.text = stop.address.ifEmpty { "Unknown location" }
        holder.btnClassify.setOnClickListener { onClassifyClick(stop) }
        holder.itemView.setOnClickListener { onClassifyClick(stop) }
    }
}
