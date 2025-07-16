package com.example.websocket

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private lateinit var txtStatus: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val ambNumber = "MH-14-CL-1111"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtStatus = findViewById(R.id.txtStatus)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()
        initWebSocket()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private fun initWebSocket() {
        val request = Request.Builder()
            .url("ws://210.212.165.116:7070/ws/tcr/amb_loc")
            .build()

        val client = OkHttpClient()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    txtStatus.text = "WebSocket Connected"
                }
                startLocationUpdates()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
                runOnUiThread {
                    txtStatus.text = "WebSocket Error: ${t.message}"
                }
            }
        })
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                val json = JSONObject().apply {
                    put("lat", location.latitude)
                    put("lonf", location.longitude)
                    put("amb_no", ambNumber)
                }

                webSocket.send(json.toString())
                Log.d("WebSocket", "Sent: $json")

                runOnUiThread {
                    txtStatus.text = "Sent: ${json.toString()}"
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        webSocket.close(1000, "App closed")
    }
}
