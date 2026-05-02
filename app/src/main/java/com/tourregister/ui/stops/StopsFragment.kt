package com.tourregister.ui.stops

import android.content.Intent
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
import com.tourregister.ui.visits.VisitDetailActivity

class StopsFragment : Fragment() {
    private lateinit var repository: TourRepository
    private lateinit var adapter: StopAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_stops, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = TourRepository(requireContext())
        val rvStops = view.findViewById<RecyclerView>(R.id.rvStops)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyStops)
        adapter = StopAdapter { stop ->
            startActivity(Intent(requireContext(), VisitDetailActivity::class.java).apply { putExtra(VisitDetailActivity.EXTRA_STOP_ID, stop.id) })
        }
        rvStops.layoutManager = LinearLayoutManager(requireContext()); rvStops.adapter = adapter
        repository.getUnclassifiedStops().observe(viewLifecycleOwner) { stops ->
            adapter.submitList(stops)
            if (stops.isEmpty()) { rvStops.visibility = View.GONE; tvEmpty.visibility = View.VISIBLE }
            else { rvStops.visibility = View.VISIBLE; tvEmpty.visibility = View.GONE }
        }
    }
}
