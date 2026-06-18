package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

// Deep slate blue primary color theme matching screenshots
val PrimaryBlue = Color(0xFF3F51B5)
val LightPrimaryBlue = Color(0xFFE8EAF6)
val GrayBackground = Color(0xFFF5F5FA)
val DarkText = Color(0xFF1A1A24)
val MediumGrayText = Color(0xFF757585)
val MetricCardBlue = Color(0xFF3B5998)
val GreenAccent = Color(0xFF4CAF50)
val RedAccent = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: DeviceViewModel = viewModel()) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isFloatingMonitorEnabled by viewModel.isFloatingMonitorEnabled.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportFilePath by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // Floating overlay coordinate state for in-app draggable simulation (perfect visual backup)
    var overlayOffset by remember { mutableStateOf(Offset(20f, 150f)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Info",
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Lagu daray kuwa aad jeceshahay!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = PrimaryBlue
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Habaynta Qalabka Somali v1.0", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Settings Menu",
                            tint = DarkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GrayBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Horizontal scroll tab bar
                CustomScrollableTabRow(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )

                // Selected content screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (currentTab) {
                        DeviceTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavToTab = { viewModel.selectTab(it) },
                            onExportTrigger = {
                                viewModel.exportReport { file ->
                                    exportFilePath = file.absolutePath
                                    showExportDialog = true
                                }
                            }
                        )
                        DeviceTab.DEVICE -> DeviceInfoScreen(viewModel)
                        DeviceTab.SYSTEM -> SystemInfoScreen(viewModel)
                        DeviceTab.CPU -> CpuScreen(viewModel)
                        DeviceTab.BATTERY -> BatteryScreen(viewModel)
                        DeviceTab.NETWORK -> NetworkScreen(viewModel)
                        DeviceTab.CONNECTIVITY -> ConnectivityScreen(viewModel)
                        DeviceTab.DISPLAY -> DisplayScreen(viewModel)
                        DeviceTab.MEMORY -> MemoryScreen(viewModel)
                        DeviceTab.CAMERA -> CameraScreen(viewModel)
                        DeviceTab.THERMAL -> ThermalScreen(viewModel)
                        DeviceTab.SENSORS -> SensorsScreen(viewModel)
                        DeviceTab.APPS -> AppsScreen(viewModel, searchQuery, onQueryChange = { searchQuery = it })
                        DeviceTab.TESTS -> TestsScreen(viewModel)
                    }
                }
            }

            // Draggable Floating Monitor Simulation inside the screen (Daaweynta Sareeye)
            if (isFloatingMonitorEnabled) {
                Box(
                    modifier = Modifier
                        .offset(overlayOffset.x.dp, overlayOffset.y.dp)
                        .width(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE6263238))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                overlayOffset = Offset(
                                    x = (overlayOffset.x + dragAmount.x / 3f).coerceIn(10f, 200f),
                                    y = (overlayOffset.y + dragAmount.y / 3f).coerceIn(10f, 600f)
                                )
                            }
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Daaweynta Sareeye",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dami",
                                tint = Color.Red,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.toggleFloatingMonitor(false) }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val ram by viewModel.ramState.collectAsStateWithLifecycle()
                        val batt by viewModel.batteryState.collectAsStateWithLifecycle()
                        Text("RAM: ${ram.percentage}%", color = Color.LightGray, fontSize = 11.sp)
                        Text("Baatiri: ${batt.level}% [${batt.status}]", color = Color.LightGray, fontSize = 11.sp)
                        Text("Kulaylka: ${batt.tempCelsius}°C", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Export report dialogue
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Warbixinta diyaar waa!") },
            text = { Text("Warbixinta qalabkaaga oo dhamaystiran oo af Somali ah ayaa lagu guuleystay in lagu dhoofiyo kaydka downloads-ka qalabka.") },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    try {
                        val file = File(exportFilePath)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Report via"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lama wadaagi karo xilligan: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("LA WADAAG (SHARE)", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CustomScrollableTabRow(
    selectedTab: DeviceTab,
    onTabSelected: (DeviceTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeviceTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) PrimaryBlue else Color(0xFFF0F2F6))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = tab.title,
                    color = if (isSelected) Color.White else DarkText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DASHBOARD VIEW (Guudmar)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DeviceViewModel,
    onNavToTab: (DeviceTab) -> Unit,
    onExportTrigger: () -> Unit
) {
    val context = LocalContext.current
    val ram by viewModel.ramState.collectAsStateWithLifecycle()
    val storage by viewModel.storageState.collectAsStateWithLifecycle()
    val battery by viewModel.batteryState.collectAsStateWithLifecycle()
    val cpu by viewModel.cpuState.collectAsStateWithLifecycle()
    val sensorCount by viewModel.sensorCount.collectAsStateWithLifecycle()

    val formattedRamUsed = String.format("%.2f GB", ram.used.toDouble() / (1024 * 1024 * 1024))
    val formattedRamTotal = String.format("%.2f GB Total", ram.total.toDouble() / (1024 * 1024 * 1024))
    val formattedRamFree = String.format("%.2f GB Free", ram.free.toDouble() / (1024 * 1024 * 1024))

    val formattedStoreUsed = String.format("%.1f GB", storage.used.toDouble() / (1024 * 1024 * 1024))
    val formattedStoreTotal = String.format("%.1f GB Total", storage.total.toDouble() / (1024 * 1024 * 1024))
    val formattedStoreFree = String.format("%.1f GB Free", storage.free.toDouble() / (1024 * 1024 * 1024))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero visual circuit header banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.img_dashboard_hero),
                        contentDescription = "Circuit Hero",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )
                    Text(
                        text = "FALANQAYNTA TIJAABOOMSKA", // System Analytics & Tests Info
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            }
        }

        // High fidelity gauge card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MetricCardBlue)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Circular gauge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.2f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = (ram.percentage * 3.6f),
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${ram.percentage}%",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right chart text metadata
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "RAM - $formattedRamTotal",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$formattedRamUsed Used",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            formattedRamFree,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        // Visual wave line representation
                        Canvas(modifier = Modifier
                            .fillMaxWidth()
                            .height(25.dp)) {
                            val path = Path()
                            path.moveTo(0f, size.height / 2)
                            for (x in 0..size.width.toInt() step 5) {
                                val y = (size.height / 2) + sin(x.toFloat() * 0.05f) * 12f
                                path.lineTo(x.toFloat(), y)
                            }
                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.7f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // CPU cores real-time matrix
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Statuska CPU",
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid layout for cores
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 4,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cpu.coreFrequencies.forEach { core ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Core ${core.id}",
                                        color = MediumGrayText,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        "${core.frequencyMhz} MHz",
                                        color = PrimaryBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Two split info cards (Tests and Display)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavToTab(DeviceTab.TESTS) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LightPrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "15",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Tijaabooyin", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                            Text("15/15 Dhamaystiran", fontSize = 11.sp, color = MediumGrayText)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavToTab(DeviceTab.DISPLAY) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Screen",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Shaashadda", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                            Text("2340x1080 | 120Hz", fontSize = 11.sp, color = MediumGrayText)
                        }
                    }
                }
            }
        }

        // Quick action command row (Tools, Analyze, Export)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            viewModel.toggleFloatingMonitor(!viewModel.isFloatingMonitorEnabled.value)
                            Toast.makeText(context, "Daaweynta Sareeye (Monitor Overlay) la kiciyay!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = "Tools", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Qalabada (Overlay)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DarkText)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Falanqaynta App-ka iyo Kaydka ee Somali leh!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.BarChart, contentDescription = "Analyze", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Falanqaynta", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DarkText)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onExportTrigger() }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Dhoofso Warbixin", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DarkText)
                    }
                }
            }
        }

        // Linear storage card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kaydka Gudaha (Storage)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${storage.percentage}%", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = storage.percentage / 100f,
                        color = PrimaryBlue,
                        trackColor = Color(0xFFE0E0E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Faaruq ah: $formattedStoreFree, Wadarta: $formattedStoreTotal ($formattedStoreUsed la isticmaalay)", fontSize = 11.sp, color = MediumGrayText)
                }
            }
        }

        // Battery state card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Baatiri (Battery)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${battery.level}%", fontWeight = FontWeight.Bold, color = GreenAccent, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = battery.level / 100f,
                        color = GreenAccent,
                        trackColor = Color(0xFFE0E0E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Volteejka: ${battery.voltageMv}mV • Heerkulka: ${battery.tempCelsius}°C", fontSize = 11.sp, color = MediumGrayText)
                }
            }
        }

        // Sensors and apps counts split buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavToTab(DeviceTab.SENSORS) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Sensors",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("$sensorCount Sensors", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                            Text("Dareerayaal Hawleed", fontSize = 11.sp, color = MediumGrayText)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavToTab(DeviceTab.APPS) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "Apps",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("543 All Apps", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                            Text("Barnaamijyada Ku Jira", fontSize = 11.sp, color = MediumGrayText)
                        }
                    }
                }
            }
        }

        // Privacy state footer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = GreenAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Macluumaadkaagu waa ammaan. Xog uma dirin xagal kasta.",
                        color = Color(0xFF2E7D32),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STANDARD KEY/VALUE LIST COMPONENT
// -------------------------------------------------------------
@Composable
fun DeviceInfoList(items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            items.forEachIndexed { i, pair ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = pair.first,
                        color = MediumGrayText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = pair.second,
                        color = DarkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (i < items.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFFF0F0FA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INDIVIDUAL SCREENS
// -------------------------------------------------------------
@Composable
fun DeviceInfoScreen(viewModel: DeviceViewModel) {
    val d by viewModel.deviceState.collectAsStateWithLifecycle()
    val data = listOf(
        "Magaca Qalabka" to d.deviceName,
        "Moodle Qalabka" to d.model,
        "Shirkadda Iska Leh" to d.manufacturer,
        "Taariikhda la Sameeyey" to d.manufactureDate,
        "Muddada uu Jiraa" to d.deviceAge,
        "Koodha Wax-soo-saarka" to d.productCode,
        "Koodha Iibka (Sales Code)" to d.salesCode,
        "Dalka/Gobolka Iibka" to d.salesCountry,
        "Nooca Board-ka" to d.board,
        "Sifada Qalabka Hardware" to d.hardware,
        "Koodha Brand-ka" to d.brand,
        "Aqoonsiga Android ID" to d.androidId,
        "Nooca Isgaarsiinta" to d.deviceType,
        "Taageerada eSIM" to d.eSIM,
        "Nooca Shabakadda Hadda" to d.networkType,
        "Adeegga Isgaarsiinta 1" to d.carrier1,
        "Adeegga Isgaarsiinta 2" to d.carrier2,
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { DeviceInfoList(data) }
    }
}

@Composable
fun SystemInfoScreen(viewModel: DeviceViewModel) {
    val s by viewModel.systemState.collectAsStateWithLifecycle()
    val data = listOf(
        "Nooca Coden-ka" to s.codename,
        "Heerka API" to s.apiLevel.toString(),
        "Nooca Android-ka" to s.androidVersion,
        "Java VM Version" to s.javaVm,
        "Amniga Patch-ka" to s.securityPatch,
        "Kernel Version" to s.kernel,
        "Google Play Services" to s.playServices,
        "Vulkan API Support" to s.vulkan,
        "OpenGL ES Version" to s.opengl,
        "Statuska Xididka (Root)" to s.rootStatus,
        "Luuqadda Nidaamka" to s.language,
        "Waqtiga Daarnaa (Uptime)" to s.uptime,
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { DeviceInfoList(data) }
    }
}

@Composable
fun CpuScreen(viewModel: DeviceViewModel) {
    val c by viewModel.cpuState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        // High fidelity header band containing chip details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MetricCardBlue)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Chip",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(c.socName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text(c.architecture, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text(c.scaleNm, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val data = listOf(
            "Processor" to c.processor,
            "Cores (Xudumaha)" to c.cores.toString(),
            "Qaab-dhismeedka CPU" to c.type,
            "Governor-ka CPU" to c.governor,
            "Foomka ABIs la Taageero" to c.abis,
            "GPU Renderer" to c.gpuRenderer,
            "GPU Vendor" to c.gpuVendor,
            "GPU Version" to c.gpuVersion
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { DeviceInfoList(data) }
        }
    }
}

@Composable
fun BatteryScreen(viewModel: DeviceViewModel) {
    val b by viewModel.batteryState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MetricCardBlue)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dab-qabadka (Current): ${b.currentMa} mA",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text("${b.tempCelsius} °C", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Wave progress container with battery outline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Battery layout box
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(100.dp)
                            .border(3.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val innerHeight = size.height * (b.level / 100f)
                            drawRect(
                                color = Color.White.copy(alpha = 0.5f),
                                topLeft = Offset(0f, size.height - innerHeight),
                                size = Size(size.width, innerHeight)
                            )
                        }
                        Text(
                            text = "${b.level}%",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Wave canvas animation details
                    Box(modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path()
                            path.moveTo(0f, size.height / 2)
                            for (x in 0..size.width.toInt()) {
                                val rads = x.toFloat() * 0.02f
                                val y = (size.height / 2) + sin(rads) * 15f
                                path.lineTo(x.toFloat(), y)
                            }
                            path.lineTo(size.width, size.height)
                            path.lineTo(0f, size.height)
                            path.close()
                            drawPath(
                                path = path,
                                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent))
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                        ) {
                            Text("Awoodda: ${String.format("%.2f W", b.powerWatts)}", color = Color.White, fontSize = 13.sp)
                            Text(b.status, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val data = listOf(
            "Caafimaadka Baas" to b.health,
            "Heerka Dedaalka" to "${b.level}%",
            "Muuqaalka Dallacaada" to b.status,
            "Isha Korontada" to b.powerSource,
            "Farsamada Baas" to b.technology,
            "Volteejka Baas" to "${b.voltageMv} mV",
            "Waqtiga Dhiman" to b.timeToCharge,
            "Heerka Koronto Hadda" to "${b.currentMa} mA",
            "Awoodda (Charged)" to "${b.capChargedMah} mAh",
            "Awoodda (Estimated)" to "${b.capEstMah} mAh",
            "Awoodda Nidaamka" to "${b.capSysMah} mAh",
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { DeviceInfoList(data) }
        }
    }
}

@Composable
fun NetworkScreen(viewModel: DeviceViewModel) {
    val n by viewModel.networkState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MetricCardBlue)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wi-Fi",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wi-Fi", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text(n.ipAddress, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text("Wi-Fi Standard: " + n.wifiStandard, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { Toast.makeText(context, "Network usage checked!", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "", tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Usage", color = Color.White, fontSize = 10.sp)
                    }

                    Button(
                        onClick = { Toast.makeText(context, "Public IP: 102.129.248.55", Toast.LENGTH_LONG).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = "", tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Public IP", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        val data = listOf(
            "Wadada IP Adhar" to n.ipAddress,
            "Wadada IPv6" to n.ipv6Address,
            "Gateway" to n.gateway,
            "Subnet Mask" to n.subnetMask,
            "DNS 1" to n.dns1,
            "DNS 2" to n.dns2,
            "Duritaanka Lease-ka" to n.leaseDuration,
            "Waji (Interface)" to n.iface,
            "Xawaaraha Link-ga" to n.linkSpeed,
            "Kanaalka (Channel)" to n.channel,
            "Frequency Mowjad" to n.frequency,
            "Standard-ka WiFi-ga" to n.wifiStandard,
            "Nooca Amniga" to n.securityType,
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { DeviceInfoList(data) }
        }
    }
}

@Composable
fun ConnectivityScreen(viewModel: DeviceViewModel) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wi-Fi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("WiFi Standard: Wi-Fi 6", fontSize = 13.sp)
                    Text("Wi-Fi Direct: Ka Taageeray", fontSize = 11.sp, color = MediumGrayText)
                    Text("5GHz Band: Ka Taageeray", fontSize = 11.sp, color = MediumGrayText)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bluetooth", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { Toast.makeText(context, "Bluetooth la hawliyay!", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Kici Bluetooth (Turn on Bluetooth)", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Nfc, contentDescription = "", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NFC", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Taageerada NFC: Taageeray (Supported)", fontSize = 13.sp)
                    Text("Statuska NFC-ga: Off", fontSize = 11.sp, color = MediumGrayText)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("USB Host Status", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("USB Debugging is actively enabled", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun DisplayScreen(viewModel: DeviceViewModel) {
    val d by viewModel.displayState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MetricCardBlue)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Screen",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(d.resolution, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text("Built-in standard screen size", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text("${d.physicalSize} | ${d.refreshRate}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val data = listOf(
            "Xallinta" to d.resolution,
            "Dufshinta (Density)" to d.density,
            "Cabbirka Farta" to d.fontScale.toString(),
            "Baaxadda Jirka" to d.physicalSize,
            "Cusboonaysiinta Shaashadda" to d.refreshRate,
            "Taageerada HDR" to d.hdr,
            "HDR Capabilities" to d.hdrCaps,
            "Wide Color Gamut" to d.gamut,
            "Iftiinka Heerkiisa" to d.brightness,
            "Qaabka Iftiinka" to d.brightnessMode,
            "Waqti Seexasho" to d.timeout,
            "Mihnad (Orientation)" to d.orientation,
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            item { DeviceInfoList(data) }
        }
    }
}

@Composable
fun MemoryScreen(viewModel: DeviceViewModel) {
    val ram by viewModel.ramState.collectAsStateWithLifecycle()
    val storage by viewModel.storageState.collectAsStateWithLifecycle()

    val formattedRamUsed = String.format("%.2f GB", ram.used.toDouble() / (1024 * 1024 * 1024))
    val formattedRamTotal = String.format("%.2f GB", ram.total.toDouble() / (1024 * 1024 * 1024))

    val formattedStoreUsed = String.format("%.2f GB", storage.used.toDouble() / (1024 * 1024 * 1024))
    val formattedStoreTotal = String.format("%.2f GB", storage.total.toDouble() / (1024 * 1024 * 1024))

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RAM Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = "", tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Xusuusta RAM", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("LPDDR4X Spec Speed", fontSize = 11.sp, color = MediumGrayText)
                        Text("$formattedRamUsed used of $formattedRamTotal", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = ram.percentage / 100f,
                            color = PrimaryBlue,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, PrimaryBlue, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${ram.percentage}%", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Mock Ad Card precisely following screenshots layouts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE8E9EC))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0E0FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = "", tint = PrimaryBlue)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(viewModel.mockAdTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(viewModel.mockAdDesc, fontSize = 11.sp, color = MediumGrayText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x1F000000), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Ad", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Play", fontSize = 11.sp, color = MediumGrayText)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Soo dejinta barnaamijka launcher-ka dhowaan...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Rakibo", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // System storage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FolderZip, contentDescription = "", tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("System Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("/system partition space", fontSize = 11.sp, color = MediumGrayText)
                        Text("6.73GB used of 6.73GB", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = 1.0f,
                            color = PrimaryBlue,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, PrimaryBlue, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("100%", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Internal Storage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = "", tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Internal User Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("/data user directory info", fontSize = 11.sp, color = MediumGrayText)
                        Text("$formattedStoreUsed used of $formattedStoreTotal", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = storage.percentage / 100f,
                            color = PrimaryBlue,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, PrimaryBlue, RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${storage.percentage}%", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraScreen(viewModel: DeviceViewModel) {
    val items = listOf(
        "Kamarada Dambe (Rear Main)" to "12 MP (4080 x 3060) • f/1.8 Aperture",
        "Kamarada Hore (Front Face)" to "13 MP (4128 x 3096) • f/2.2 Aperture",
        "Apertures Support" to "f/1.8, f/2.2, f/2.4",
        "Flash Ready" to "Active Flash Supported",
        "Autofocus Modes" to "Continuous, Auto, Video, Picture",
        "Aberration Correction" to "Standard Off",
        "Anti-banding Mode" to "Auto Mode Available",
        "RAW Output Format" to "RAW_SENSOR format supported"
    )
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MetricCardBlue)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "", tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Kamarasaha Qalabka", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Summary of cameras sensors modules", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            }
        }
        item { DeviceInfoList(items) }
    }
}

@Composable
fun ThermalScreen(viewModel: DeviceViewModel) {
    val batt by viewModel.batteryState.collectAsStateWithLifecycle()
    val list = listOf(
        "Baatiri Heerkulka" to "${batt.tempCelsius} °C",
        "CPU Zone 0" to "36.2 °C",
        "CPU Zone 1" to "37.5 °C",
        "GPU Thermal Zone" to "38.0 °C",
        "Board Ambient sensor" to "34.0 °C"
    )
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Heerkulka Zone-yada Qalabka", fontWeight = FontWeight.Bold, color = DarkText, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Cusboonaysiinta live-ka ah 2.0s kasta oo af Somali ah", fontSize = 11.sp, color = MediumGrayText)
            }
        }
        DeviceInfoList(list)
    }
}

@Composable
fun SensorsScreen(viewModel: DeviceViewModel) {
    val sensorList by viewModel.sensorList.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = "Wadarta Xasaasiyaasha: ${sensorList.size}",
                    modifier = Modifier.padding(14.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PrimaryBlue
                )
            }
        }

        items(sensorList) { sensor ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sensor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(LightPrimaryBlue, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(sensor.wakeup, fontSize = 9.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Nooca: ${sensor.typeLabel}", fontSize = 11.sp, color = MediumGrayText)
                    Text("Shirkadda: ${sensor.vendor}", fontSize = 11.sp, color = MediumGrayText)
                    Text("Power Draft: ${sensor.power} mA • Max Range: ${sensor.maxRange}", fontSize = 11.sp, color = MediumGrayText)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// INSTALLED APPS PANEL WITH SEARCH FILTERS
// -------------------------------------------------------------
@Composable
fun AppsScreen(
    viewModel: DeviceViewModel,
    searchQuery: String,
    onQueryChange: (String) -> Unit
) {
    val appsList by viewModel.appsList.collectAsStateWithLifecycle()
    var filterMode by remember { mutableStateOf(0) } // 0: User apps, 1: System apps, 2: All apps

    val filteredApps = appsList.filter {
        val matchesQuery = it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterMode) {
            0 -> !it.isSystem
            1 -> it.isSystem
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App search & segment switcher combo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Somali labelled search textfield
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Mashiinka baaritaanka app-ka...", fontSize = 13.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Segment switch tabs button rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val userCount = appsList.count { !it.isSystem }
                    val sysCount = appsList.count { it.isSystem }

                    Button(
                        onClick = { filterMode = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (filterMode == 0) PrimaryBlue else Color(0xFFF0F0FF)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("User ($userCount)", color = if (filterMode == 0) Color.White else PrimaryBlue, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { filterMode = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (filterMode == 1) PrimaryBlue else Color(0xFFF0F0FF)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("System ($sysCount)", color = if (filterMode == 1) Color.White else PrimaryBlue, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { filterMode = 2 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (filterMode == 2) PrimaryBlue else Color(0xFFF0F0FF)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("All (${appsList.size})", color = if (filterMode == 2) Color.White else PrimaryBlue, fontSize = 11.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredApps) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFECEC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Android, contentDescription = "", tint = PrimaryBlue)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(app.packageName, fontSize = 10.sp, color = MediumGrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Nooca: " + app.version, fontSize = 11.sp, color = MediumGrayText)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFD0D0D0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(app.size, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkText)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TESTS PANEL (Tijaabooyinka) WITH HIGH INTERACTIVE POP-UPS
// -------------------------------------------------------------
@Composable
fun TestsScreen(viewModel: DeviceViewModel) {
    val testsList by viewModel.tests.collectAsStateWithLifecycle()
    val recordsList by viewModel.dbTestRecords.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedInteractiveTest by remember { mutableStateOf<HardwareTest?>(null) }
    var displayColorIndex by remember { mutableStateOf(0) }
    val displayColors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)

    var touchPointsList = remember { mutableStateListOf<Offset>() }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Tijaabooyinka Qalabka (Interactive)",
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Guji mid kasta si aad u bilowdo baarista saxda ah.",
                    fontSize = 11.sp,
                    color = MediumGrayText
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(testsList) { test ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(test.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            // Status badge element
                            when (val status = test.status) {
                                is TestStatus.Idle -> {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFF0F0FF), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("U Diyaar ah", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is TestStatus.Running -> {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                                }
                                is TestStatus.Finished -> {
                                    Box(
                                        modifier = Modifier
                                            .background(if (status.passed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = status.log,
                                            color = if (status.passed) GreenAccent else RedAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(test.instruction, fontSize = 11.sp, color = MediumGrayText)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Launcher Action Button in Somali: "Bilow Tijaabada"
                        Button(
                            onClick = {
                                if (test.id == "display" || test.id == "touch") {
                                    // Launch dynamic immersive overlay dialogs
                                    selectedInteractiveTest = test
                                    displayColorIndex = 0
                                    touchPointsList.clear()
                                } else {
                                    viewModel.runTest(test.id) { passed ->
                                        if (passed) {
                                            Toast.makeText(context, "${test.name}: Guulaysatay!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "${test.name}: Guuldaraystey!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Bilow Tijaabada", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // IMMERSIVE ACTIVE TESTS MODALS
    selectedInteractiveTest?.let { testObj ->
        AlertDialog(
            onDismissRequest = { selectedInteractiveTest = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxSize(),
            confirmButton = {},
            dismissButton = {},
            title = null,
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (testObj.id == "display") {
                        // Fullscreen screen color patterns test
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(displayColors[displayColorIndex])
                                .clickable {
                                    if (displayColorIndex < displayColors.size - 1) {
                                        displayColorIndex++
                                    } else {
                                        viewModel.markTestManual("display", true)
                                        selectedInteractiveTest = null
                                        Toast.makeText(context, "Muuqaalka: Guulaysatay!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Guji meel kasta oo shaashadda ah si aad isugu badasho midabka.\nMidabka hadda: " + when (displayColorIndex) {
                                    0 -> "Gaduud"
                                    1 -> "Cagaar"
                                    2 -> "Buluug"
                                    3 -> "Cadaad"
                                    else -> "Madow"
                                },
                                color = if (displayColors[displayColorIndex] == Color.White) Color.Black else Color.White,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else if (testObj.id == "touch") {
                        // Multi-touch point visual test canvas
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        touchPointsList.add(offset)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                touchPointsList.forEach { point ->
                                    drawCircle(
                                        color = Color.Cyan,
                                        center = point,
                                        radius = 45f
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                                    .align(Alignment.TopCenter),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Taabo shaashadda si aad u tijaabiso dhoor dhibic oo isku mar ah.", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.markTestManual("touch", true)
                                            selectedInteractiveTest = null
                                            Toast.makeText(context, "Taabashada: Guulaysatay!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                                    ) {
                                        Text("Guulaysatay (Pass)", color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.markTestManual("touch", false)
                                            selectedInteractiveTest = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                                    ) {
                                        Text("Ku Guuldareystay", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
