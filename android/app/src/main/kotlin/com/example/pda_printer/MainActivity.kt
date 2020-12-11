package com.example.pda_printer

import android.content.*
import android.os.BatteryManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import android.widget.Toast
import com.qs.util.PrintUtils
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant


class MainActivity: FlutterActivity() {
    private val PRINTCHANNEL : String = "com.example.pda_printer/print_text"
    private val CHARGINGCHANNEL : String = "com.example.pda_printer/charging"
    private val BATTERYCHANNEL : String = "com.example.pda_printer/battery"
    private val SCANCHANNEL : String = "com.example.pda_printer/scan"

    var mIntent = Intent("ismart.intent.scandown")
    private var scanBroadcastReceiver: ScanBroadcastReceiver? = null
    var scanResult: String? = ""

    var resx: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        GeneratedPluginRegistrant.registerWith(flutterEngine)

        //Initialize printer
        PrintUtils.initPrintUtils(this)

        EventChannel(flutterEngine.dartExecutor, CHARGINGCHANNEL).setStreamHandler(
                object : StreamHandler {
                    private var chargingStateChangeReceiver: BroadcastReceiver? = null
                    override fun onListen(arguments: Any?, events: EventSink?) {
                        chargingStateChangeReceiver = createChargingStateChangeReceiver(events)
                        registerReceiver(
                                chargingStateChangeReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }

                    override fun onCancel(arguments: Any?) {
                        unregisterReceiver(chargingStateChangeReceiver)
                        chargingStateChangeReceiver = null
                    }
                }
        )

        MethodChannel(flutterEngine.dartExecutor, BATTERYCHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "getBatteryLevel") {
                val batteryLevel: Int = getBatteryLevel()
                if (batteryLevel != -1) {
                    result.success(batteryLevel)
                } else {
                    result.error("UNAVAILABLE", "Battery level not available.", null)
                }
            } else {
                result.notImplemented()
            }
        }

        MethodChannel(flutterEngine.dartExecutor, PRINTCHANNEL).setMethodCallHandler { call, result ->
            if(call.method.equals("PrintText")) {
                val text : String = call.arguments()
                PrintUtils.printText(1, 60, 0, false, text + "\n")
                result.success("You are getting this message from Native Android")
            }
        }

        MethodChannel(flutterEngine.dartExecutor, SCANCHANNEL).setMethodCallHandler { call, resx ->
            if(call.method.equals("ScanText")) {
                scanBroadcastReceiver = ScanBroadcastReceiver(resx)
                val intentFilter = IntentFilter()
                intentFilter.addAction("com.qs.scancode")
                this.registerReceiver(scanBroadcastReceiver, intentFilter)

                sendBroadcast(mIntent)
            }
        }
    }

    private fun createChargingStateChangeReceiver(events: EventSink?): BroadcastReceiver? {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    events?.error("UNAVAILABLE", "Charging status unavailable", null)
                } else {
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    events?.success(if (isCharging) "charging" else "discharging")
                }
            }
        }
    }

    private fun getBatteryLevel(): Int {
        return if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 /
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val barcodeStr = data!!.getStringExtra("ScannedStr")
                scanResult = barcodeStr
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    internal class ScanBroadcastReceiver(val result: MethodChannel.Result?) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // TODO Auto-generated method stub
            var text1: String? = intent.getExtras()?.getString("code")
            try {
                result?.success(text1);
            } catch (e: Exception) {
                Log.e("Sending Scan Result", e.message)
            }
//            tv.setText(text1);
            Toast.makeText(context, "Scan Value：$text1", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        unregisterReceiver(scanBroadcastReceiver)
        PrintUtils.closeApp()
        super.onDestroy()
    }
}
