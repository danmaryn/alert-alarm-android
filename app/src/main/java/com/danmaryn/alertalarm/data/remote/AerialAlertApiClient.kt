package com.danmaryn.alertalarm.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.danmaryn.alertalarm.data.model.AlertCheckResult
import com.danmaryn.alertalarm.data.model.UkrainianRegions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AerialAlertApiClient {

    private const val API_URL = "https://ubilling.net.ua/aerialalerts/"
    private const val TIMEOUT_MS = 6000

    /**
     * Checks if the device has an active internet connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    /**
     * Performs a network call to fetch active air raid alert status.
     * Supports both direct oblast queries and raion resolution via parent oblast fallback.
     */
    suspend fun checkAlertStatus(context: Context, regionName: String): AlertCheckResult =
        withContext(Dispatchers.IO) {
            if (!isNetworkAvailable(context)) {
                return@withContext AlertCheckResult(
                    isAlertActive = false,
                    regionName = regionName,
                    isNetworkAvailable = false,
                    errorMessage = "Network is offline"
                )
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL(API_URL)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", "AlertAlarm-Android/1.0")
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val rootJson = JSONObject(response.toString())
                    val statesJson = rootJson.optJSONObject("states")

                    if (statesJson != null) {
                        // Check direct match or fallback to parent oblast for raions
                        val targetKey = if (statesJson.has(regionName)) {
                            regionName
                        } else {
                            val parentOblast = UkrainianRegions.all.find { it.name == regionName }?.parentOblast
                            if (parentOblast != null && statesJson.has(parentOblast)) parentOblast else null
                        }

                        if (targetKey != null) {
                            val regionData = statesJson.getJSONObject(targetKey)
                            val alertNow = regionData.optBoolean("alertnow", false)
                            val changedAt = if (regionData.has("changed") && !regionData.isNull("changed")) {
                                regionData.getString("changed")
                            } else {
                                null
                            }

                            return@withContext AlertCheckResult(
                                isAlertActive = alertNow,
                                regionName = regionName,
                                changedAt = changedAt,
                                isNetworkAvailable = true
                            )
                        }
                    }

                    return@withContext AlertCheckResult(
                        isAlertActive = false,
                        regionName = regionName,
                        isNetworkAvailable = true,
                        errorMessage = "Region '$regionName' not found in response"
                    )
                } else {
                    return@withContext AlertCheckResult(
                        isAlertActive = false,
                        regionName = regionName,
                        isNetworkAvailable = true,
                        errorMessage = "HTTP error code: $responseCode"
                    )
                }
            } catch (e: Exception) {
                return@withContext AlertCheckResult(
                    isAlertActive = false,
                    regionName = regionName,
                    isNetworkAvailable = false,
                    errorMessage = e.localizedMessage ?: "Unknown network error"
                )
            } finally {
                connection?.disconnect()
            }
        }
}
