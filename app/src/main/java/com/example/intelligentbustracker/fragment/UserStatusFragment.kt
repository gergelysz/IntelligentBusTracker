package com.example.intelligentbustracker.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.intelligentbustracker.BusTrackerApplication
import com.example.intelligentbustracker.R
import com.example.intelligentbustracker.model.Direction
import com.example.intelligentbustracker.model.Status
import com.example.intelligentbustracker.model.UserStatus
import com.example.intelligentbustracker.util.Common
import java.util.stream.Collectors

class UserStatusFragment(private val listener: OnStatusChangeListener) : DialogFragment(), AdapterView.OnItemSelectedListener {

    companion object {
        private const val TAG = "UserStatusFragment"
    }

    private lateinit var radioButtonWaitingForBus: RadioButton
    private lateinit var radioButtonOnBus: RadioButton

    private lateinit var radioButtonLocationTrackingYes: RadioButton
    private lateinit var radioButtonLocationTrackingNo: RadioButton

    private lateinit var buttonUserStatusSave: Button
    private lateinit var buttonUserStatusCancel: Button

    private lateinit var busSelectSpinner: Spinner

    private lateinit var radioGroupStatus: RadioGroup
    private lateinit var radioGroupDirection: RadioGroup

    private lateinit var textViewStatus: TextView
    private lateinit var textViewDirection: TextView

    private lateinit var radioButtonDirection1: RadioButton
    private lateinit var radioButtonDirection2: RadioButton

    private val userStatus: UserStatus = UserStatus()

    private lateinit var busNumbers: List<String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireActivity())
        dialog.setContentView(R.layout.fragment_user_status)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        val rootView: View = inflater.inflate(R.layout.fragment_user_status, container, false)

        /** Init View elements */
        initializeViewElements(rootView)

        /** Init visibility of View elements */
        BusTrackerApplication.status.value?.let {
            processVisibilityOfOptions(it)
        }

        radioButtonLocationTrackingYes.setOnClickListener {
            showTrackingOptions()
        }

        radioButtonLocationTrackingNo.setOnClickListener {
            hideTrackingOptions()
        }

        radioButtonWaitingForBus.setOnClickListener {
            hideDirectionOptions()
        }

        radioButtonOnBus.setOnClickListener {
            showDirectionOptions()
        }

        radioButtonDirection1.setOnClickListener {
            userStatus.direction = Direction.DIRECTION_1
        }

        radioButtonDirection2.setOnClickListener {
            userStatus.direction = Direction.DIRECTION_2
        }

        buttonUserStatusSave.setOnClickListener {
            listener.onStatusChange(userStatus)
            dismiss()
        }

        buttonUserStatusCancel.setOnClickListener {
            dismiss()
        }

        busNumbers = BusTrackerApplication.buses.stream().map { x -> x.number.toString() }.collect(Collectors.toList())
        val adapter: ArrayAdapter<String> = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, busNumbers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        busSelectSpinner.onItemSelectedListener = this
        busSelectSpinner.adapter = adapter

        return rootView
    }

    interface OnStatusChangeListener {
        fun onStatusChange(status: UserStatus)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        userStatus.busNumber = busNumbers[position].toInt()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "onNothingSelected: no item selected")
    }

    private fun initializeViewElements(rootView: View) {
        radioButtonWaitingForBus = rootView.findViewById(R.id.radio_button_waiting_for_bus)
        radioButtonOnBus = rootView.findViewById(R.id.radio_button_on_bus)

        radioButtonLocationTrackingYes = rootView.findViewById(R.id.radio_button_location_tracking_yes)
        radioButtonLocationTrackingNo = rootView.findViewById(R.id.radio_button_location_tracking_no)

        buttonUserStatusSave = rootView.findViewById(R.id.button_user_status_save)
        buttonUserStatusCancel = rootView.findViewById(R.id.button_user_status_cancel)

        busSelectSpinner = rootView.findViewById(R.id.spinner_user_status_bus)

        radioGroupStatus = rootView.findViewById(R.id.radio_group_status)
        radioGroupDirection = rootView.findViewById(R.id.radio_group_direction)

        textViewStatus = rootView.findViewById(R.id.text_view_status)
        textViewDirection = rootView.findViewById(R.id.text_view_direction)

        radioButtonDirection1 = rootView.findViewById(R.id.radio_button_direction1)
        radioButtonDirection2 = rootView.findViewById(R.id.radio_button_direction2)
    }

    private fun processVisibilityOfOptions(status: Status) {
        /** Process Status */
        userStatus.status = status
        if (status == Status.WAITING_FOR_BUS) {
            activity?.let {
                if (Common.requestingLocationUpdates(it)) {
                    userStatus.tracking = true
                    radioButtonLocationTrackingYes.isChecked = true
                    radioButtonWaitingForBus.isChecked = true
                    showTrackingOptions()
                } else {
                    userStatus.tracking = false
                    radioButtonLocationTrackingYes.isChecked = false
                    radioButtonOnBus.isChecked = true
                    hideDirectionOptions()
                }
            }
        } else if (status == Status.ON_BUS) {
            activity?.let {
                if (Common.requestingLocationUpdates(it)) {
                    userStatus.tracking = true
                    radioButtonOnBus.isChecked = true
                    showDirectionOptions()
                } else {
                    userStatus.tracking = false
                    radioButtonWaitingForBus.isChecked = true
                    hideDirectionOptions()
                }
            }
        }

        /** Process Tracking */
        activity?.let {
            if (Common.requestingLocationUpdates(it)) {
                userStatus.tracking = true
                showTrackingOptions()
            } else {
                userStatus.tracking = false
                hideTrackingOptions()
            }
        }
    }

    private fun hideTrackingOptions() {
        userStatus.tracking = false

        textViewStatus.visibility = View.INVISIBLE
        radioGroupStatus.visibility = View.INVISIBLE
        busSelectSpinner.visibility = View.INVISIBLE
        textViewDirection.visibility = View.INVISIBLE
        radioGroupDirection.visibility = View.INVISIBLE
    }

    private fun showTrackingOptions() {
        userStatus.tracking = true

        textViewStatus.visibility = View.VISIBLE
        radioGroupStatus.visibility = View.VISIBLE

        if (userStatus.status == Status.ON_BUS) {
            busSelectSpinner.visibility = View.VISIBLE
            textViewDirection.visibility = View.VISIBLE
            radioGroupDirection.visibility = View.VISIBLE
        } else {
            busSelectSpinner.visibility = View.INVISIBLE
            textViewDirection.visibility = View.INVISIBLE
            radioGroupDirection.visibility = View.INVISIBLE
        }
    }

    private fun showDirectionOptions() {
        userStatus.status = Status.ON_BUS

        textViewDirection.visibility = View.VISIBLE
        busSelectSpinner.visibility = View.VISIBLE
        radioGroupDirection.visibility = View.VISIBLE
    }

    private fun hideDirectionOptions() {
        userStatus.status = Status.WAITING_FOR_BUS

        textViewDirection.visibility = View.INVISIBLE
        busSelectSpinner.visibility = View.INVISIBLE
        radioGroupDirection.visibility = View.INVISIBLE
    }
}