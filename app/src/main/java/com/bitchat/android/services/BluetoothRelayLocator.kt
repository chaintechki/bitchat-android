package com.bitchat.android.services

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.content.getSystemService
import kotlin.math.pow

/**
 * Simple Bluetooth relay based location provider.
 *
 * The service scans for known Bluetooth relays and estimates the
 * device position based on RSSI derived distances.
 */
class BluetoothRelayLocator(private val context: Context) {
    data class Relay(val mac: String, val latitude: Double, val longitude: Double, val txPower: Int = -59)

    private val bluetoothManager: BluetoothManager? = context.getSystemService()
    private val scanner: BluetoothLeScanner? = bluetoothManager?.adapter?.bluetoothLeScanner

    private val knownRelays = mutableListOf<Relay>()
    private val distances = mutableMapOf<String, Double>()
    private val listeners = mutableListOf<(Location) -> Unit>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
        }
    }

    /** Register a relay with known coordinates */
    fun registerRelay(relay: Relay) {
        knownRelays += relay
    }

    /** Add listener for location updates */
    fun addLocationListener(listener: (Location) -> Unit) {
        listeners += listener
    }

    /** Start BLE scanning for registered relays */
    fun startScanning() {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(null, settings, scanCallback)
    }

    /** Stop BLE scanning */
    fun stopScanning() {
        scanner?.stopScan(scanCallback)
    }

    private fun handleResult(result: ScanResult) {
        val relay = knownRelays.firstOrNull { it.mac.equals(result.device.address, true) } ?: return
        val distance = calculateDistance(relay.txPower, result.rssi)
        distances[relay.mac] = distance
        trilaterate()?.let { location -> listeners.forEach { it(location) } }
    }

    private fun trilaterate(): Location? {
        val relays = knownRelays.filter { distances.containsKey(it.mac) }
        if (relays.size < 3) return null
        val (r1, r2, r3) = relays.take(3)
        val d1 = distances[r1.mac]!!
        val d2 = distances[r2.mac]!!
        val d3 = distances[r3.mac]!!
        val x = (r1.longitude / d1 + r2.longitude / d2 + r3.longitude / d3) / (1 / d1 + 1 / d2 + 1 / d3)
        val y = (r1.latitude / d1 + r2.latitude / d2 + r3.latitude / d3) / (1 / d1 + 1 / d2 + 1 / d3)
        return Location("bluetooth").apply {
            latitude = y
            longitude = x
        }
    }

    companion object {
        private const val TAG = "BluetoothRelayLocator"
        private const val PROPAGATION_N = 2.0 // free-space path loss constant
        fun calculateDistance(txPower: Int, rssi: Int): Double {
            return 10.0.pow((txPower - rssi) / (10 * PROPAGATION_N))
        }
    }
}

