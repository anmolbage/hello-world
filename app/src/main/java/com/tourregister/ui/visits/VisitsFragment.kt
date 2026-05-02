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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_visits, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TourRepository(requireContext())
        val rvVisits = view.findViewById<RecyclerView>(R.id.rvVisits)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyVisits)
        adapter = VisitAdapter()
        rvVisits.layoutManager = LinearLayoutManager(requireContext()); rvVisits.adapter = adapter
        repository.getAllVisits().observe(viewLifecycleOwner) { visits ->
            adapter.submitList(visits)
            if (visits.isEmpty()) { rvVisits.visibility = View.GONE; tvEmpty.visibility = View.VISIBLE }
            else { rvVisits.visibility = View.VISIBLE; tvEmpty.visibility = View.GONE }
        }
    }
}
