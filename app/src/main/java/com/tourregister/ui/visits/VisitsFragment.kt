package com.tourregister.ui.visits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tourregister.R
import com.tourregister.data.repository.TourRepository

class VisitsFragment : Fragment() {
    private lateinit var repository: TourRepository
    private lateinit var adapter: VisitAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = inflater.inflate(R.layout.fragment_visits, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TourRepository(requireContext())
        val rv = view.findViewById<RecyclerView>(R.id.rvVisits)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyVisits)
        adapter = VisitAdapter(); rv.layoutManager = LinearLayoutManager(requireContext()); rv.adapter = adapter
        repository.getAllVisits().observe(viewLifecycleOwner) { visits ->
            adapter.submitList(visits)
            rv.visibility = if (visits.isEmpty()) View.GONE else View.VISIBLE
            tvEmpty.visibility = if (visits.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
