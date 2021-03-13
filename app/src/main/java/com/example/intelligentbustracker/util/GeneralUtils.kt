package com.example.intelligentbustracker.util

import android.Manifest
import android.os.Build

class GeneralUtils {

    companion object {

        @JvmStatic
        fun getPermissionList(): Collection<String> {
            val permissions: Collection<String>
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE
                    )
                } else {
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE
                    )
                }
            } else {
                permissions = listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            return permissions
        }
    }
}