package com.example.intelligentbustracker.model

data class LeavingHours(val fromStation: String, val weekdayLeavingHours: ArrayList<String>, val saturdayLeavingHours: ArrayList<String>, val sundayLeavingHours: ArrayList<String>)