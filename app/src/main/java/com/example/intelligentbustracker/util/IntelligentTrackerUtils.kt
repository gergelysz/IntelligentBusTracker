package com.example.intelligentbustracker.util

import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

class IntelligentTrackerUtils {

    companion object {
        /**
         * Converts default speed (meters/second)
         * to kilometers/hour.
         */
        fun getSpeedkmph(speedmps: Float): Float {
            return speedmps.times(3.6F)
        }

        private fun getTransitions(): MutableList<ActivityTransition> {
            val transitions = mutableListOf<ActivityTransition>()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

            transitions +=
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()

            return transitions
        }

        fun getActivityTransitionRequest() = ActivityTransitionRequest(getTransitions())

        fun toActivityString(activity: Int): String {
            return when (activity) {
                DetectedActivity.STILL -> "STILL"
                DetectedActivity.WALKING -> "WALKING"
                DetectedActivity.IN_VEHICLE -> "IN VEHICLE"
                DetectedActivity.RUNNING -> "RUNNING"
                else -> "UNKNOWN"
            }
        }

        fun toTransitionType(transitionType: Int): String  {
            return when (transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
                else -> "UNKNOWN"
            }
        }
    }
}