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

class StopAdapter(private val onClassifyClick: (DetectedStop) -> Unit) : ListAdapter<DetectedStop, StopAdapter.ViewHolder>(StopDiffCallback()) {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvStopTime)
        val tvDuration: TextView = view.findViewById(R.id.tvStopDuration)
        val tvAddress: TextView = view.findViewById(R.id.tvStopAddress)
        val btnClassify: MaterialButton = view.findViewById(R.id.btnClassify)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_stop, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stop = getItem(position)
        val arrivalStr = DateUtils.formatTime(stop.arrivalTime)
        val departureStr = if (stop.departureTime > 0) DateUtils.formatTime(stop.departureTime) else "Now"
        holder.tvTime.text = "$arrivalStr — $departureStr"
        val durationMs = if (stop.departureTime > 0) stop.departureTime - stop.arrivalTime else System.currentTimeMillis() - stop.arrivalTime
        holder.tvDuration.text = DateUtils.formatDurationFromMillis(durationMs)
        holder.tvAddress.text = stop.address.ifEmpty { "Unknown location" }
        holder.btnClassify.setOnClickListener { onClassifyClick(stop) }
        holder.itemView.setOnClickListener { onClassifyClick(stop) }
    }
}

class StopDiffCallback : DiffUtil.ItemCallback<DetectedStop>() {
    override fun areItemsTheSame(oldItem: DetectedStop, newItem: DetectedStop) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: DetectedStop, newItem: DetectedStop) = oldItem == newItem
}
