package com.example.echosight

import android.Manifest
import com.google.android.gms.location.FusedLocationProviderClient
//功能实现部分import
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat


object SOS1 {
    private const val TAG = "SOS1"
    private const val SOS_PREFS_NAME = "SOS_Preferences"
    private const val KEY_EMERGENCY_PHONE = "emergency_phone_number"
    private var fusedLocationClient: FusedLocationProviderClient? = null

    fun setFusedLocationProvider(client: FusedLocationProviderClient) {
        fusedLocationClient = client
    }


    fun startSOS(context: Context) {
        android.widget.Toast.makeText(context, "SOS triggered！", android.widget.Toast.LENGTH_SHORT).show()

        val locationClient = fusedLocationClient ?: run {
            Log.e(TAG, "定位客户端未初始化，请先调用 setFusedLocationProvider")
            sendNoLocationSOS(context)
            return
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }


        locationClient.lastLocation?.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                val mapLink = "https://www.google.com/maps?q=$lat,$lng"
                Log.d(TAG, "获取到位置：$mapLink")

                val phoneNumber = getEmergencyPhoneNumber(context).ifEmpty { "+447350122479" }

                if (phoneNumber.isNotEmpty()) {
                    sendSMS(context, phoneNumber, "Emergency！My location：$mapLink")
                } else {
                    Log.e(TAG, "紧急联系人号码未设置")
                }
            } else {
                Log.w(TAG, "未获取到当前位置，发送兜底求救短信")
                sendNoLocationSOS(context)
            }
        }?.addOnFailureListener { e ->
            Log.e(TAG, "获取位置失败：${e.message}", e)
            sendNoLocationSOS(context)
        }
    }
    fun getEmergencyPhoneNumber(context: Context): String {
        val sharedPref: SharedPreferences = context.getSharedPreferences(SOS_PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_EMERGENCY_PHONE, "") ?: ""
    }
    fun saveEmergencyPhoneNumber(context: Context, phoneNumber: String) {
        val sharedPref: SharedPreferences = context.getSharedPreferences(SOS_PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_EMERGENCY_PHONE, phoneNumber)
            apply()
        }
        Log.d(TAG, "紧急联系人号码已保存：$phoneNumber")
    }
    private fun sendSMS(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d(TAG, "求救短信发送成功：$phoneNumber -> $message")
        } catch (e: Exception) {
            Log.e(TAG, "短信发送失败：${e.message}", e)
        }
    }
    private fun sendNoLocationSOS(context: Context) {
        val phoneNumber = getEmergencyPhoneNumber(context).ifEmpty { "+447350122479" }
        if (phoneNumber.isNotEmpty()) {
            sendSMS(context, phoneNumber, "I am in emergency, but my location cannot be obtained temporarily, please try to call my number to confirm safety.")
        } else {
            Log.e(TAG, "No emergency contact number has been set, so a text message cannot be sent.")
        }
    }
}