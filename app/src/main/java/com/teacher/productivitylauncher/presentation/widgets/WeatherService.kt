package com.teacher.productivitylauncher.presentation.widgets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume

data class WeatherData(
    val temperature: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val cityName: String,
    val feelsLike: Double
)

class WeatherService(private val context: Context) {

    private val client = OkHttpClient()
    // OpenWeatherMap API key — free এ register করে নাও
    // https://openweathermap.org/api
    private val API_KEY = "YOUR_API_KEY_HERE"

    suspend fun getWeather(): WeatherData? {
        val location = getCurrentLocation() ?: return null
        return fetchWeather(location.first, location.second)
    }

    private suspend fun getCurrentLocation(): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        return withTimeoutOrNull(5000) {
            suspendCancellableCoroutine { continuation ->
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationToken = CancellationTokenSource()

                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationToken.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(Pair(location.latitude, location.longitude))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cancellationToken.cancel()
                }
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double): WeatherData? {
        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$API_KEY&units=metric"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            WeatherData(
                temperature = json.getJSONObject("main").getDouble("temp"),
                description = json.getJSONArray("weather").getJSONObject(0).getString("description"),
                icon = json.getJSONArray("weather").getJSONObject(0).getString("icon"),
                humidity = json.getJSONObject("main").getInt("humidity"),
                windSpeed = json.getJSONObject("wind").getDouble("speed"),
                cityName = json.getString("name"),
                feelsLike = json.getJSONObject("main").getDouble("feels_like")
            )
        } catch (e: Exception) {
            null
        }
    }
}