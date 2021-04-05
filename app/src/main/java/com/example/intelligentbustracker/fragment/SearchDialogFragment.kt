package com.example.intelligentbustracker.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Station
import java.util.stream.Collectors

class SearchDialogFragment : DialogFragment() {

    private lateinit var listener: SearchDialogListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        activity?.let { it ->
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.search_dialog_layout, null)

            val stationNames: List<String> = BusTrackerApplication.stations.stream().map { x -> x.name }.collect(Collectors.toList())
            val adapter: ArrayAdapter<String> = ArrayAdapter(it, android.R.layout.simple_list_item_1, stationNames)

            val searchView = view.findViewById<SearchView>(R.id.search_view)
            val searchListView = view.findViewById<ListView>(R.id.search_list_view)

            searchView.isIconified = false
            searchView.requestFocusFromTouch()

            searchListView.adapter = adapter
            searchListView.setOnItemClickListener { parent, view, position, id ->
                val stationName = adapter.getItem(position)
                val station = BusTrackerApplication.stations.firstOrNull { x -> x.name == stationName }
                station?.let { listener.searchedForStation(it) }
            }

            searchView.queryHint = "Search for a station..."

            searchView.setOnQueryTextFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    searchView.showKeyboard()
                } else {
                    searchView.hideKeyboard()
                }
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchView.clearFocus()
                    if (stationNames.contains(query)) {
                        adapter.filter.filter(query)
                    } else {
                        Toast.makeText(it, "Bus station not found", Toast.LENGTH_SHORT).show()
                    }
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter.filter(newText)
                    return false
                }
            })

            builder.setView(view)
                .setTitle("Search")
                .setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.cancel()
                }
        }

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as SearchDialogListener
    }

    interface SearchDialogListener {
        fun searchedForStation(station: Station)
    }

    private fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}