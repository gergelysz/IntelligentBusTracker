package com.example.intelligentbustracker.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.util.GeneralUtils
import com.example.intelligentbustracker.util.MapUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapAssistFragment(private val listener: OnMapPositionClickListener) : DialogFragment() {

    private lateinit var mMap: GoogleMap
    private val mapAssistContext: Context = BusTrackerApplication.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {
            return GeneralUtils.buildFullscreenDialog(it, R.color.darker_orange)
        } ?: return GeneralUtils.buildFullscreenDialog(requireActivity(), R.color.darker_orange)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_map_assist, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_map_assist_container) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap
            MapUtils.setupMap(mMap, mapAssistContext, BusTrackerApplication.mapTheme.value!!)

            mMap.setOnMapClickListener { position ->
                listener.onMapAssistClick(position)
                dismiss()
            }
        }
        return rootView
    }

    interface OnMapPositionClickListener {
        fun onMapAssistClick(position: LatLng)
    }
}