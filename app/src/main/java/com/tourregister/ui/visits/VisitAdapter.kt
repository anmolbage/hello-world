package com.tourregister.ui.visits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tourregister.R
import com.tourregister.data.entity.Visit
import com.tourregister.data.entity.VisitPurpose
import com.tourregister.util.DateUtils

class VisitAdapter : ListAdapter<Visit, VisitAdapter.ViewHolder>(VisitDiffCallback()) {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPurpose: TextView = view.findViewById(R.id.tvPurpose)
        val tvDate: TextView = view.findViewById(R.id.tvVisitDate)
        val tvTime: TextView = view.findViewById(R.id.tvVisitTime)
        val tvDuration: TextView = view.findViewById(R.id.tvVisitDuration)
        val tvDistance: TextView = view.findViewById(R.id.tvVisitDistance)
        val tvAddress: TextView = view.findViewById(R.id.tvVisitAddress)
        val tvNotes: TextView = view.findViewById(R.id.tvVisitNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_visit, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val visit = getItem(position)
        val purpose = VisitPurpose.fromName(visit.purpose)
        holder.tvPurpose.text = purpose.displayName
        val bgColor = when (purpose) {
            VisitPurpose.GOVT_MEETING -> R.color.card_govt
            VisitPurpose.OFFICIAL_MEETING -> R.color.card_official
            VisitPurpose.CUSTOMER_MEETING -> R.color.card_customer
            VisitPurpose.PRE_SANCTION_INSPECTION, VisitPurpose.POST_SANCTION_INSPECTION -> R.color.card_inspection
            VisitPurpose.RECOVERY_VISIT, VisitPurpose.NOTICE_SERVE -> R.color.card_recovery
            else -> R.color.card_others
        }
        holder.tvPurpose.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(bgColor))
        holder.tvDate.text = DateUtils.formatDate(visit.date)
        holder.tvTime.text = "${DateUtils.formatTime(visit.timeIn)} — ${DateUtils.formatTime(visit.timeOut)}"
        holder.tvDuration.text = DateUtils.formatDuration(visit.durationMinutes)
        holder.tvDistance.text = DateUtils.formatDistance(visit.distanceKm)
        holder.tvAddress.text = visit.address
        if (visit.notes.isNotBlank()) { holder.tvNotes.text = visit.notes; holder.tvNotes.visibility = View.VISIBLE }
        else holder.tvNotes.visibility = View.GONE
    }
}

class VisitDiffCallback : DiffUtil.ItemCallback<Visit>() {
    override fun areItemsTheSame(oldItem: Visit, newItem: Visit) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Visit, newItem: Visit) = oldItem == newItem
}
