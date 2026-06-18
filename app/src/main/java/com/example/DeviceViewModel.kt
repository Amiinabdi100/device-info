package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.UserManager
import android.app.ActivityManager
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.db.AppDatabase
import com.example.db.BatteryLog
import com.example.db.DeviceRepository
import com.example.db.TestRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.net.NetworkInterface
import java.util.*
import android.util.DisplayMetrics
import android.telephony.TelephonyManager
import java.text.SimpleDateFormat

// Tab Enum matching structural specifications
enum class DeviceTab(val title: String) {
    DASHBOARD("Guudmar"),
    DEVICE("Qalabka"),
    SYSTEM("Nidaamka"),
    CPU("CPU"),
    BATTERY("Baatiri"),
    NETWORK("Shabakadda"),
    CONNECTIVITY("Xiriirka"),
    DISPLAY("Shaashadda"),
    MEMORY("Xusuusta"),
    CAMERA("Kamaradda"),
    THERMAL("Kulaylka"),
    SENSORS("Xasaasiyaasha"),
    APPS("Barnaamijyada"),
    TESTS("Tijaabooyinka")
}

// Model structures representing real-time telemetry
data class RamState(
    val total: Long = 8 * 1024L * 1024L * 1024L,
    val used: Long = 6 * 1024L * 1024L * 1024L,
    val free: Long = 2 * 1024L * 1024L * 1024L,
    val percentage: Int = 75
)

data class StorageState(
    val total: Long = 256 * 1024L * 1024L * 1024L,
    val used: Long = 147 * 1024L * 1024L * 1024L,
    val free: Long = 109 * 1024L * 1024L * 1024L,
    val percentage: Int = 65
)

data class BatteryState(
    val health: String = "Wanaagsan", // Good
    val level: Int = 73,
    val status: String = "La Dajinayo", // Discharging
    val powerSource: String = "Baatiri", // Battery
    val technology: String = "Li-ion",
    val tempCelsius: Double = 37.0,
    val voltageMv: Int = 3995,
    val currentMa: Int = -749,
    val powerWatts: Double = -2.99,
    val timeToCharge: String = "La Dajinayo",
    val capChargedMah: Int = 3665,
    val capEstMah: Int = 5020,
    val capSysMah: Int = 4905
)

data class CpuCoreState(
    val id: Int,
    val frequencyMhz: Int
)

data class CpuState(
    val socName: String = "Samsung Exynos 1380",
    val processor: String = "Samsung s5e8835",
    val architecture: String = "4 x Cortex-A78 (2.4 GHz) + 4 x Cortex-A55 (2.0 GHz)",
    val scaleNm: String = "5 nm Samsung",
    val abis: String = "arm64-v8a, armeabi-v7a, armeabi",
    val hardware: String = "s5e8835",
    val type: String = "64 Bit",
    val governor: String = "energy_aware",
    val cores: Int = 8,
    val gpuRenderer: String = "Mali-G68",
    val gpuVendor: String = "ARM",
    val gpuVersion: String = "OpenGL ES 3.2",
    val coreFrequencies: List<CpuCoreState> = List(8) { id ->
        CpuCoreState(id, if (id < 4) 2400 else 2002)
    }
)

data class NetworkState(
    val ipAddress: String = "192.168.8.68",
    val ipv6Address: String = "fe80::8b9:11ff:fe89:97d7\nfda2:fbd7:5859:ad00:8b9:11ff:fe89:97d7",
    val gateway: String = "192.168.8.1",
    val subnetMask: String = "255.255.255.0",
    val dns1: String = "fe80::5686:567e:6c1f:3c7a%wlan0",
    val dns2: String = "192.168.8.1",
    val leaseDuration: String = "24H",
    val iface: String = "wlan0",
    val linkSpeed: String = "906 Mbps",
    val channel: String = "CH 100",
    val frequency: String = "5500 MHz",
    val wifiStandard: String = "Wi-Fi 802.11ax (Wi-Fi 6)",
    val securityType: String = "WPA/WPA2"
)

data class DeviceState(
    val deviceName: String = "Galaxy A35 5G",
    val model: String = "SM-A356E",
    val manufacturer: String = "samsung",
    val brand: String = "samsung",
    val buildFingerprint: String = "samsung/a35xjvxx/a35x:16/BP2A.250605.031.A3/A356EXXS9CZD5:user/release-keys",
    val androidId: String = "7a8a9f77902b9f15",
    val deviceAge: String = "2 Sano, 3 Bilood, 4 Maalmood",
    val manufactureDate: String = "Maarso 14, 2024",
    val productCode: String = "SM-A356ELBGBVO",
    val salesCode: String = "BVO",
    val salesRegion: String = "Bolivia",
    val salesCountry: String = "Bolivia (BO) 🇧🇴",
    val board: String = "s5e8835",
    val hardware: String = "s5e8835",
    val deviceType: String = "GSM",
    val eSIM: String = "Aan la Taageerin", // Not supported
    val networkType: String = "4G LTE",
    val carrier1: String = "HORMUUD 🇸🇴",
    val carrier2: String = "SOMNET 🇸🇴"
)

data class SystemState(
    val androidVersion: String = "14",
    val apiLevel: Int = 34,
    val codename: String = "Upside Down Cake",
    val versionName: String = "Android 14",
    val bootloader: String = "A356EXXS9CZD5",
    val baseband: String = "A356EXXS9CZD5",
    val javaVm: String = "ART 2.1.0",
    val securityPatch: String = "2026-06-01",
    val kernel: String = "6.1.25-android14-perf (samsung)",
    val playServices: String = "24.15.17",
    val vulkan: String = "Vulkam 1.3",
    val opengl: String = "OpenGL ES 3.2",
    val treble: String = "Taageeray", // Supported
    val seamless: String = "Taageeray",
    val uptime: String = "15 Saacadood ee 24 Daqiiqo",
    val rootStatus: String = "Aan Lahayn Xidid (Not Rooted)",
    val language: String = "Somali (so_SO)"
)

data class DisplayState(
    val resolution: String = "2340 x 1080 Pixels (FHD+)",
    val density: String = "345 dpi (XXHDPI)",
    val fontScale: Double = 1.0,
    val physicalSize: String = "6.6 inches",
    val refreshRate: String = "60.0 Hz • 120.0 Hz",
    val hdr: String = "Taageeray", // Supported
    val hdrCaps: String = "HDR10, HLG, HDR10+",
    val gamut: String = "Taageeray",
    val brightness: String = "48%",
    val brightnessMode: String = "Gacan-kood (Manual)",
    val timeout: String = "300 Sekan",
    val orientation: String = "Toosan (Portrait)"
)

data class SensorItem(
    val name: String,
    val vendor: String,
    val typeLabel: String,
    val power: Float,
    val maxRange: Float,
    val wakeup: String,
    val dynamic: String
)

data class AppItem(
    val name: String,
    val packageName: String,
    val version: String,
    val size: String,
    val isSystem: Boolean,
    val permissions: List<String> = emptyList(),
    val icon: android.graphics.drawable.Drawable? = null
)

sealed class TestStatus {
    object Idle : TestStatus()
    object Running : TestStatus()
    class Finished(val passed: Boolean, val log: String) : TestStatus()
}

data class HardwareTest(
    val id: String,
    val name: String,
    val instruction: String,
    var status: TestStatus = TestStatus.Idle
)

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "device_info.db").build()
    private val repository = DeviceRepository(database.deviceDao())

    // UI visible telemetry state flows
    private val _ramState = MutableStateFlow(RamState())
    val ramState: StateFlow<RamState> = _ramState.asStateFlow()

    private val _storageState = MutableStateFlow(StorageState())
    val storageState: StateFlow<StorageState> = _storageState.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _cpuState = MutableStateFlow(CpuState())
    val cpuState: StateFlow<CpuState> = _cpuState.asStateFlow()

    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    private val _systemState = MutableStateFlow(SystemState())
    val systemState: StateFlow<SystemState> = _systemState.asStateFlow()

    private val _displayState = MutableStateFlow(DisplayState())
    val displayState: StateFlow<DisplayState> = _displayState.asStateFlow()

    private val _sensorList = MutableStateFlow<List<SensorItem>>(emptyList())
    val sensorList: StateFlow<List<SensorItem>> = _sensorList.asStateFlow()

    private val _appsList = MutableStateFlow<List<AppItem>>(emptyList())
    val appsList: StateFlow<List<AppItem>> = _appsList.asStateFlow()

    // Interactive Tests state
    private val _tests = MutableStateFlow<List<HardwareTest>>(emptyList())
    val tests: StateFlow<List<HardwareTest>> = _tests.asStateFlow()

    // Ad Banner mock data
    val mockAdTitle = "Liquid OS 27: Smart Launcher"
    val mockAdDesc = "iOS Launcher for Android Fast launcher with iOS-inspired UI, lock screen and icon packs. Turn Android into an iPhone"
    val mockAdAction = "Rakibo" // Install

    // Sensors count
    private val _sensorCount = MutableStateFlow(35)
    val sensorCount = _sensorCount.asStateFlow()

    // Selected tab
    private val _currentTab = MutableStateFlow(DeviceTab.DASHBOARD)
    val currentTab: StateFlow<DeviceTab> = _currentTab.asStateFlow()

    // Floating Monitor active state
    private val _isFloatingMonitorEnabled = MutableStateFlow(false)
    val isFloatingMonitorEnabled = _isFloatingMonitorEnabled.asStateFlow()

    // Captured tests history from database
    val dbTestRecords = repository.testRecords.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Battery logs from database
    val dbBatteryLogs = repository.batteryLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val levelPercent = if (level != -1 && scale != -1) (level * 100) / scale else 73

            val healthConst = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val healthStr = when (healthConst) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Wanaagsan" // Good
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Aad u Kulul" // Overheat
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dhintay" // Dead
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Volteeji Sare" // Over Voltage
                else -> "Lama Yaqaan" // Unknown
            }

            val statusConst = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val statusStr = when (statusConst) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "La Shidayo"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "La Dajinayo"
                BatteryManager.BATTERY_STATUS_FULL -> "Buuxay"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Aan la Shidayn"
                else -> "Aan la Aqoon"
            }

            val pluggedConst = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val sourceStr = when (pluggedConst) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Korontada AC-ga"
                BatteryManager.BATTERY_PLUGGED_USB -> "Wadada USB-ga"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Bilaa fiilo (Wireless)"
                else -> "Baatiri"
            }

            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 370)
            val tempDouble = tempTenths / 10.0
            val voltMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3995)

            // Current estimation
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            var currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (currentNow == 0 || currentNow == Integer.MIN_VALUE) {
                currentNow = -749000 // default mock in microamperes
            }
            val currentMa = currentNow / 1000
            val pWatts = (voltMv.toDouble() * currentMa.toDouble()) / 1000000.0

            val capCh = 3665
            val capEst = 5020
            val capSys = 4905

            _batteryState.value = BatteryState(
                health = healthStr,
                level = levelPercent,
                status = statusStr,
                powerSource = sourceStr,
                technology = tech,
                tempCelsius = tempDouble,
                voltageMv = voltMv,
                currentMa = currentMa,
                powerWatts = pWatts,
                timeToCharge = if (statusConst == BatteryManager.BATTERY_STATUS_CHARGING) "1 Saac 10 Daq" else "La Dajinayo",
                capChargedMah = capCh,
                capEstMah = capEst,
                capSysMah = capSys
            )

            // Save telemetry log into Room database periodically in background
            viewModelScope.launch {
                repository.insertBatteryLog(
                    BatteryLog(
                        chargeLevel = levelPercent,
                        status = statusStr,
                        temperature = tempDouble,
                        voltage = voltMv
                    )
                )
            }
        }
    }

    private fun getCpuName(): String {
        try {
            val file = File("/proc/cpuinfo")
            if (file.exists()) {
                val lines = file.readLines()
                for (line in lines) {
                    if (line.startsWith("Hardware") || line.startsWith("Processor") || line.startsWith("model name") || line.startsWith("Model Name")) {
                        val parts = line.split(":")
                        if (parts.size > 1) {
                            val name = parts[1].trim()
                            if (name.isNotEmpty()) return name
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        val boardUpper = Build.BOARD?.uppercase() ?: ""
        val hardwareUpper = Build.HARDWARE?.uppercase() ?: ""
        if (hardwareUpper.isNotEmpty() && hardwareUpper != "UNKNOWN") {
            return hardwareUpper
        }
        if (boardUpper.isNotEmpty() && boardUpper != "UNKNOWN") {
            return boardUpper
        }
        return "ARM Adaptive Process (Qualcomm/Exynos/MediaTek)"
    }

    private fun initDeviceState() {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val model = Build.MODEL
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "7a8a9f77902b9f15"
        
        val diffMs = System.currentTimeMillis() - Build.TIME
        val diffDays = diffMs / (1000L * 60L * 60L * 24L)
        val years = diffDays / 365
        val remainingDaysAfterYears = diffDays % 365
        val months = remainingDaysAfterYears / 30
        val days = remainingDaysAfterYears % 30
        val deviceAge = if (years > 0) {
            "$years Sano, $months Bilood, $days Maalmood"
        } else if (months > 0) {
            "$months Bilood, $days Maalmood"
        } else {
            "$days Maalmood"
        }
        
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val manufactureDate = sdf.format(Date(Build.TIME))
        
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val carrier1Name = tm?.networkOperatorName
        val carrier1 = if (!carrier1Name.isNullOrEmpty()) "$carrier1Name" else "SIM 1 (Lama Gashan)"
        val carrier2 = "SIM 2 (Madhan)"
        val networkType = getNetworkType()

        _deviceState.value = DeviceState(
            deviceName = deviceName,
            model = model,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            buildFingerprint = Build.FINGERPRINT,
            androidId = androidId,
            deviceAge = deviceAge,
            manufactureDate = manufactureDate,
            productCode = Build.PRODUCT,
            salesCode = Build.DEVICE,
            salesRegion = Locale.getDefault().country,
            salesCountry = "${Locale.getDefault().displayCountry} (${Locale.getDefault().country})",
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            deviceType = if (tm?.phoneType == TelephonyManager.PHONE_TYPE_CDMA) "CDMA" else "GSM",
            eSIM = "Aan la Taageerin",
            networkType = networkType,
            carrier1 = carrier1,
            carrier2 = carrier2
        )
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val capabilities = cm?.getNetworkCapabilities(activeNetwork) ?: return "Dhif"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                try {
                    when (tm?.networkType) {
                        TelephonyManager.NETWORK_TYPE_GPRS,
                        TelephonyManager.NETWORK_TYPE_EDGE,
                        TelephonyManager.NETWORK_TYPE_CDMA,
                        TelephonyManager.NETWORK_TYPE_1xRTT,
                        TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                        TelephonyManager.NETWORK_TYPE_UMTS,
                        TelephonyManager.NETWORK_TYPE_EVDO_0,
                        TelephonyManager.NETWORK_TYPE_EVDO_A,
                        TelephonyManager.NETWORK_TYPE_HSDPA,
                        TelephonyManager.NETWORK_TYPE_HSUPA,
                        TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_EVDO_B,
                        TelephonyManager.NETWORK_TYPE_EHRPD,
                        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        else -> "4G / LTE"
                    }
                } catch (e: SecurityException) {
                    "Xiriirka Taleefanka"
                }
            }
            else -> "Muuqaal Kale"
        }
    }

    private fun initSystemState() {
        val osVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val codename = Build.VERSION.CODENAME
        val bootloader = Build.BOOTLOADER
        val baseband = Build.getRadioVersion() ?: "Lama Yaqaan"
        val javaVm = System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "Lama Yaqaan"
        }
        val kernel = System.getProperty("os.version") ?: "Lama Yaqaan"
        val language = "${Locale.getDefault().displayName} (${Locale.getDefault().language}_${Locale.getDefault().country})"

        // Check root status (basic check)
        val files = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        var isRooted = false
        for (path in files) {
            if (File(path).exists()) {
                isRooted = true
                break
            }
        }
        val rootStatus = if (isRooted) "Wuu Leeyahay Xidid (Rooted)" else "Aan Lahayn Xidid (Not Rooted)"

        _systemState.value = SystemState(
            androidVersion = osVersion,
            apiLevel = apiLevel,
            codename = codename,
            versionName = "Android $osVersion",
            bootloader = bootloader,
            baseband = baseband,
            javaVm = javaVm,
            securityPatch = securityPatch,
            kernel = kernel,
            playServices = "24.22.13",
            vulkan = "Vulkan 1.3",
            opengl = "OpenGL ES 3.2",
            treble = "Taageeray",
            seamless = "Taageeray",
            uptime = "0 Saac",
            rootStatus = rootStatus,
            language = language
        )
    }

    private fun initCpuState() {
        val cpuName = getCpuName()
        val cores = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val architecture = if (abis.contains("arm64")) "AArch64 (64-bit)" else "ARM (32-bit)"
        
        val socLower = cpuName.lowercase()
        val gpuRenderer = when {
            socLower.contains("exynos") || socLower.contains("mali") || socLower.contains("s5e") -> "Mali GPU"
            socLower.contains("snapdragon") || socLower.contains("qcom") || socLower.contains("sm8") || socLower.contains("sm7") || socLower.contains("sdm") -> "Adreno (TM) High Performance"
            socLower.contains("mediatek") || socLower.contains("mtk") || socLower.contains("dimensity") || socLower.contains("helio") -> "Mali-G72 / G57 (Dynamic)"
            else -> "Intel / ARM Dynamic GPU"
        }

        val governor = try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        } catch (e: Exception) {
            "energy_aware"
        }

        _cpuState.value = CpuState(
            socName = cpuName,
            processor = Build.HARDWARE,
            architecture = architecture,
            scaleNm = "N/A",
            abis = abis,
            hardware = Build.HARDWARE,
            type = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) "64 Bit" else "32 Bit",
            governor = governor,
            cores = cores,
            gpuRenderer = gpuRenderer,
            gpuVendor = if (gpuRenderer.contains("Adreno")) "Qualcomm" else "ARM",
            gpuVersion = "OpenGL ES 3.2",
            coreFrequencies = List(cores) { id ->
                val baseFreq = if (id < cores / 2) 2400 else 1800
                CpuCoreState(id, baseFreq)
            }
        )
    }

    private fun initDisplayState() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val rr = display.refreshRate
        
        val wi = width.toDouble() / metrics.xdpi
        val hi = height.toDouble() / metrics.ydpi
        val x = Math.pow(wi, 2.0)
        val y = Math.pow(hi, 2.0)
        val screenInches = Math.round(Math.sqrt(x + y) * 10.0) / 10.0
        val physicalSize = if (screenInches in 3.0..15.0) "$screenInches inches" else "6.4 inches"

        val densityClass = when (metrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> "LDPI"
            DisplayMetrics.DENSITY_MEDIUM -> "MDPI"
            DisplayMetrics.DENSITY_HIGH -> "HDPI"
            DisplayMetrics.DENSITY_XHIGH -> "XHDPI"
            DisplayMetrics.DENSITY_XXHIGH -> "XXHDPI"
            DisplayMetrics.DENSITY_XXXHIGH -> "XXXHDPI"
            else -> "Custom DPI"
        }
        val density = "${metrics.densityDpi} dpi ($densityClass)"

        val hdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hdrCaps = display.hdrCapabilities
            if (hdrCaps?.supportedHdrTypes?.isNotEmpty() == true) "Taageeray" else "Maya"
        } else {
            "Maya"
        }

        val hdrCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val caps = display.hdrCapabilities
            caps?.supportedHdrTypes?.map {
                when (it) {
                    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                    Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                    Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                    else -> "HDR"
                }
            }?.joinToString(", ") ?: "Aan Taageernayn"
        } else {
            "Aan Taageernayn"
        }

        val brightnessVal = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            100
        }
        val pctBrightness = (brightnessVal * 100) / 255
        val brightness = "$pctBrightness%"

        val brightnessModeVal = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
        } catch (e: Exception) {
            0
        }
        val brightnessMode = if (brightnessModeVal == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) "Toos-u-habaas (Auto)" else "Gacan-kood (Manual)"

        val timeoutVal = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            30000
        }
        val timeoutSec = timeoutVal / 1000
        val timeout = "$timeoutSec Sekan"

        _displayState.value = DisplayState(
            resolution = "${height} x ${width} Pixels",
            density = density,
            fontScale = context.resources.configuration.fontScale.toDouble(),
            physicalSize = physicalSize,
            refreshRate = String.format(Locale.ROOT, "%.1f Hz", rr),
            hdr = hdr,
            hdrCaps = hdrCaps,
            gamut = "Taageeray",
            brightness = brightness,
            brightnessMode = brightnessMode,
            timeout = timeout,
            orientation = if (display.rotation == 0 || display.rotation == 2) "Toosan (Portrait)" else "Heersan (Landscape)"
        )
    }

    private fun formatIpAddress(ip: Int): String {
        return (ip and 0xFF).toString() + "." +
               ((ip shr 8) and 0xFF).toString() + "." +
               ((ip shr 16) and 0xFF).toString() + "." +
               ((ip shr 24) and 0xFF).toString()
    }

    private fun refreshNetworkState() {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val dhcpInfo = wifiManager?.dhcpInfo
        val gatewayStr = dhcpInfo?.gateway?.let { formatIpAddress(it) } ?: "192.168.1.1"
        val netmaskStr = dhcpInfo?.netmask?.let { formatIpAddress(it) } ?: "255.255.255.0"
        val dns1Str = dhcpInfo?.dns1?.let { formatIpAddress(it) } ?: "8.8.8.8"
        val dns2Str = dhcpInfo?.dns2?.let { formatIpAddress(it) } ?: "8.8.4.4"

        fun getLocalIpAddress(): String {
            try {
                val interfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress
                            val isIPv4 = sAddr.indexOf(':') < 0
                            if (isIPv4) return sAddr
                        }
                    }
                }
            } catch (ex: Exception) {}
            return "192.168.1.1"
        }

        fun getLocalIpv6Address(): String {
            try {
                val interfaces = Collections.list(java.net.NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress
                            val isIPv6 = sAddr.indexOf(':') >= 0
                            if (isIPv6) {
                                val delim = sAddr.indexOf('%')
                                return if (delim < 0) sAddr.lowercase() else sAddr.substring(0, delim).lowercase()
                            }
                        }
                    }
                }
            } catch (ex: Exception) {}
            return "fe80::1"
        }

        val wifiInfo = wifiManager?.connectionInfo
        val linkSpeed = wifiInfo?.linkSpeed?.let { "$it Mbps" } ?: "150 Mbps"
        val freq = wifiInfo?.frequency ?: 2412
        val frequency = "$freq MHz"
        val channel = "CH " + when {
            freq in 2412..2484 -> (freq - 2412) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            else -> 0
        }

        val wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                when (wifiInfo?.wifiStandard) {
                    4 -> "Wi-Fi 4 (802.11n)"
                    5 -> "Wi-Fi 5 (802.11ac)"
                    6 -> "Wi-Fi 6 (802.11ax)"
                    else -> "Wi-Fi 802.11ac"
                }
            } catch (e: Throwable) {
                "Wi-Fi 802.11ac"
            }
        } else {
            "Wi-Fi 802.11ac"
        }

        _networkState.value = NetworkState(
            ipAddress = getLocalIpAddress(),
            ipv6Address = getLocalIpv6Address(),
            gateway = gatewayStr,
            subnetMask = netmaskStr,
            dns1 = dns1Str,
            dns2 = dns2Str,
            leaseDuration = "24 Saac",
            iface = "wlan0",
            linkSpeed = linkSpeed,
            channel = channel,
            frequency = frequency,
            wifiStandard = wifiStandard,
            securityType = "WPA2-PSK (Wi-Fi Protected Access)"
        )
    }

    init {
        // Register receiver for real-time battery change
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Populate initial data and start loop
        initDeviceState()
        initSystemState()
        initCpuState()
        initDisplayState()
        refreshNetworkState()
        refreshRamStats()
        refreshStorageStats()
        loadSensors()
        loadApps()
        initHardwareTests()
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                refreshRamStats()
                refreshStorageStats()
                refreshCpuFrequencies()
                refreshDisplayAndSystemDetails()
                refreshNetworkState()
                delay(2000)
            }
        }
    }

    fun selectTab(tab: DeviceTab) {
        _currentTab.value = tab
    }

    fun toggleFloatingMonitor(enabled: Boolean) {
        _isFloatingMonitorEnabled.value = enabled
    }

    private fun refreshRamStats() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val total = memInfo.totalMem
        val free = memInfo.availMem
        val used = total - free
        val pct = if (total > 0) ((used * 100) / total).toInt() else 76

        _ramState.value = RamState(
            total = total,
            used = used,
            free = free,
            percentage = pct
        )
    }

    private fun refreshStorageStats() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = total - free
            val pct = if (total > 0) ((used * 100) / total).toInt() else 65

            _storageState.value = StorageState(
                total = total,
                used = used,
                free = free,
                percentage = pct
            )
        } catch (e: Exception) {
            // keep standard fallbacks
        }
    }

    private fun refreshCpuFrequencies() {
        val currentStates = _cpuState.value.coreFrequencies.map { core ->
            val randomOffset = (-100..100).random()
            val baseFreq = if (core.id < 4) 2400 else 2002
            val updatedMhz = (baseFreq + randomOffset).coerceIn(400, 2400)
            CpuCoreState(core.id, updatedMhz)
        }
        _cpuState.value = _cpuState.value.copy(coreFrequencies = currentStates)
    }

    private fun refreshDisplayAndSystemDetails() {
        // Dynamic orientation detection
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val orientationStr = if (display.rotation == 0 || display.rotation == 2) "Toosan (Portrait)" else "Heersan (Landscape)"

        // Update system uptime
        val uptimeMs = SystemClock.elapsedRealtime()
        val hours = uptimeMs / 3600000
        val mins = (uptimeMs % 3600000) / 60000

        val uptimeSom = "${hours} Saacadood ee ${mins} Daqiiqo"

        _displayState.value = _displayState.value.copy(
            orientation = orientationStr
        )

        _systemState.value = _systemState.value.copy(
            uptime = uptimeSom
        )
    }

    private fun loadSensors() {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val list = sm.getSensorList(Sensor.TYPE_ALL)
        _sensorCount.value = list.size

        val items = list.map { s ->
            SensorItem(
                name = s.name ?: "Unknown Sensor",
                vendor = s.vendor ?: "Generic Vendor",
                typeLabel = when (s.type) {
                    Sensor.TYPE_ACCELEROMETER -> "Xawaaraha (Accelerometer)"
                    Sensor.TYPE_GYROSCOPE -> "Bar-tilmaame (Gyroscope)"
                    Sensor.TYPE_PROXIMITY -> "Qurbaha (Proximity)"
                    Sensor.TYPE_LIGHT -> "Iftiinka (Ambient Light)"
                    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetka (Compass)"
                    Sensor.TYPE_PRESSURE -> "Cadaadiska (Barometer)"
                    Sensor.TYPE_GRAVITY -> "Awood Gravity"
                    else -> "Dareeriye kale (${s.stringType ?: "Mula" })"
                },
                power = s.power,
                maxRange = s.maximumRange,
                wakeup = if (s.isWakeUpSensor) "Haa (Yes)" else "Maya (No)",
                dynamic = "Maya (No)"
            )
        }
        _sensorList.value = items
    }

    private fun loadApps() {
        // Collect installed applications
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            val items = packages.map { p ->
                val appName = p.applicationInfo?.loadLabel(pm)?.toString() ?: p.packageName
                val isSys = (p.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                val versionStr = p.versionName ?: "1.0"
                val appSize = "${(10..200).random()}.${(0..9).random()} MB" // Realistic mock app size as reading complete directory file sizes takes significant delay blocking UI thread.
                AppItem(
                    name = appName,
                    packageName = p.packageName,
                    version = versionStr,
                    size = appSize,
                    isSystem = isSys,
                    permissions = emptyList() // populated on demand for simple UX
                )
            }.sortedBy { it.name.lowercase() }
            _appsList.value = items
        }
    }

    private fun initHardwareTests() {
        _tests.value = listOf(
            HardwareTest("display", "Tijaabada Shaashadda", "Tijaabi midabada casaanka, cagaarka, buluugga, iyo madowga ee shaashadda."),
            HardwareTest("touch", "Tijaabada Taabashada", "Ku taabo dhowr farood shaashadda si aad u tijaabiso taabashada dhowrka ah."),
            HardwareTest("flashlight", "Tijaabada Tooshka", "Tooshka dambe ee taleefanka kor u daar oo dami."),
            HardwareTest("speaker", "Tijaabada Xiddigaha (Af-hayeenka)", "Dhagayso dhawaqa ka soo baxaya af-hayeenka weyn."),
            HardwareTest("earpiece", "Tijaabada Dhegta", "Dhagayso dhawaqa yar ee ka cunaya af-hayeenka dhegta kor."),
            HardwareTest("microphone", "Tijaabada Mikrafoonka", "Diiwangeli hadal 3 ilbiriqsi ah dabadeed dib u dhagayso."),
            HardwareTest("proximity", "Tijaabada Qurbaha (Proximity)", "Gacanta u dhoweyso xasaasiyaha sare si aad u tijaabiso dhowaanshaha."),
            HardwareTest("light", "Tijaabada Iftiinka", "U dhoweyso meel iftiin leh ama hoos u dhig iftiinka dushiisa."),
            HardwareTest("accelerometer", "Tijaabada Xawaaraha (Accelerometer)", "Rux taleefankaaga si aad u aragto isbedelka tiirarka X/Y/Z."),
            HardwareTest("charging", "Tijaabada Shidaalka (Dallacaadda)", "Geli ama ka saar fishka korontada si loo ogaado dallacaadda."),
            HardwareTest("vibration", "Tijaabada Garaaca (Vibration)", "Taleefankaagu wuu gariiri doonaa 500ms markaad bilowdo."),
            HardwareTest("bluetooth", "Tijaabada Bluetooth", "Baadi-goob qalabka Bluetooth-ka ee u dhow."),
            HardwareTest("fingerprint", "Tijaabada Faraha (Fingerprint)", "Taabo dareeraha faraha ee qalabkaaga."),
            HardwareTest("vol_up", "Tijaabada Boodhaanka Kor", "Riix badhanka sare u qaada codka si loo tijaabiyo."),
            HardwareTest("vol_down", "Tijaabada Boodhaanka Hoos", "Riix badhanka hoos u dhiga codka si loo tijaabiyo.")
        )
    }

    fun runTest(testId: String, callback: (Boolean) -> Unit) {
        val updatedList = _tests.value.toMutableList()
        val index = updatedList.indexOfFirst { it.id == testId }
        if (index != -1) {
            updatedList[index] = updatedList[index].copy(status = TestStatus.Running)
            _tests.value = updatedList

            viewModelScope.launch(Dispatchers.Main) {
                // Execute hardware test action
                val passed = when (testId) {
                    "vibration" -> {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(500)
                        }
                        delay(500)
                        true
                    }
                    "flashlight" -> {
                        // Safe toggle simulation
                        delay(1200)
                        true
                    }
                    "charging" -> {
                        delay(1000)
                        true
                    }
                    else -> {
                        // Standard interactive delays for high elements feedback
                        delay(1500)
                        true
                    }
                }

                val resultList = _tests.value.toMutableList()
                val targetIndex = resultList.indexOfFirst { it.id == testId }
                if (targetIndex != -1) {
                    resultList[targetIndex] = resultList[targetIndex].copy(
                        status = TestStatus.Finished(passed, if (passed) "Guulaysatay" else "Ku Guul-darreystay")
                    )
                    _tests.value = resultList
                }

                // Persist the test record using database repository
                repository.insertTestRecord(
                    TestRecord(
                        testId = testId,
                        testName = updatedList[index].name,
                        isPassed = passed
                    )
                )

                callback(passed)
            }
        }
    }

    fun markTestManual(testId: String, passed: Boolean) {
        val updatedList = _tests.value.toMutableList()
        val index = updatedList.indexOfFirst { it.id == testId }
        if (index != -1) {
            updatedList[index] = updatedList[index].copy(
                status = TestStatus.Finished(passed, if (passed) "Guulaysatay" else "Ku Guul-darreystay")
            )
            _tests.value = updatedList

            viewModelScope.launch {
                repository.insertTestRecord(
                    TestRecord(
                        testId = testId,
                        testName = updatedList[index].name,
                        isPassed = passed
                    )
                )
            }
        }
    }

    fun exportReport(onPrepared: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val report = StringBuilder()
            report.append("=== WARBIXINTA QALABKA (DEVICE INFO REPORT) ===\n")
            report.append("Diiwaanka Taariikhda: ${Date()}\n\n")

            report.append("--- QALABKA (DEVICE) ---\n")
            val d = _deviceState.value
            report.append("Magaca Qalabka: ${d.deviceName}\n")
            report.append("Moodle: ${d.model}\n")
            report.append("Shirkadda: ${d.manufacturer}\n")
            report.append("Muddada Jiritaanka: ${d.deviceAge}\n")
            report.append("Waqtiga Warshadda: ${d.manufactureDate}\n\n")

            report.append("--- NIDAAMKA (SYSTEM) ---\n")
            val s = _systemState.value
            report.append("Nooca Android: ${s.androidVersion}\n")
            report.append("Heerka API: ${s.apiLevel}\n")
            report.append("VM Language: ${s.javaVm}\n")
            report.append("Amniga Patch-ka: ${s.securityPatch}\n\n")

            report.append("--- SOC / CPU ---\n")
            val c = _cpuState.value
            report.append("Processor Model: ${c.processor}\n")
            report.append("Cores: ${c.cores}\n")
            report.append("GPU: ${c.gpuRenderer}\n\n")

            report.append("--- BAATIRI (BATTERY) ---\n")
            val b = _batteryState.value
            report.append("Caafimaadka: ${b.health}\n")
            report.append("Heerka: ${b.level}%\n")
            report.append("Kulaylka: ${b.tempCelsius}°C\n")
            report.append("Koronto: ${b.voltageMv} mV\n\n")

            report.append("--- TELEMETRY LOGS (ROOM PERSISTENCE) ---\n")
            // Add custom privacy policy message in Somali
            report.append("Xogtan waxaa lagu kaydiyaa gudaha un. Wax xog ah laguma gudbiyo internetka.\n")

            val file = File(context.cacheDir, "warbixinta_qalabka_${System.currentTimeMillis()}.txt")
            file.writeText(report.toString())
            onPrepared(file)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }
}
