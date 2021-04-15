package com.example.intelligentbustracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.intelligentbustracker.util.Constants
import com.example.intelligentbustracker.util.IntelligentTrackerUtils
import com.google.android.gms.location.ActivityTransitionResult
import io.karn.notify.Notify
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Info for debugging purposes
                    val info = "Transition: " + IntelligentTrackerUtils.toActivityString(event.activityType) + " (" +
                            IntelligentTrackerUtils.toTransitionType(event.transitionType) + ")" + "   " + SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

                    Notify
                        .with(context)
                        .content {
                            title = "Activity Detected"
                            text = "I can see you are in ${IntelligentTrackerUtils.toActivityString(event.activityType)} state"
                        }
                        .show(Constants.ACTIVITY_TRANSITION_NOTIFICATION_ID)

                    Toast.makeText(context, info, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}