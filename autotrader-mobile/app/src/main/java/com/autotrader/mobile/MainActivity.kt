package com.autotrader.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF05070A)
private val Panel = Color(0xFF0C1118)
private val Cyan = Color(0xFF00D4FF)
private val Green = Color(0xFF26D07C)
private val Amber = Color(0xFFFFC857)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AutoTraderApp() } }
}

@Composable
fun AutoTraderApp() {
    var baseUrl by remember { mutableStateOf("http://10.0.2.2:8080") }
    var selected by remember { mutableStateOf("NIFTY") }
    var status by remember { mutableStateOf("Backend not checked") }
    var busy by remember { mutableStateOf(false) }
    var payload by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Cyan)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("AUTOTRADER", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("NIFTY • SENSEX • OPTIONS", color = Color.LightGray, fontSize = 11.sp) }
                        Text("PAPER / SHADOW", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                item { OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Backend URL") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NIFTY", "SENSEX").forEach { s -> FilterChip(selected = selected == s, onClick = { selected = s }, label = { Text(s) }) }
                    }
                }
                item { MarketCard(selected) }
                item {
                    Button(enabled = !busy, onClick = {
                        busy = true; status = "Checking…"
                        MainScope().launch {
                            val r = withContext(Dispatchers.IO) { fetch(baseUrl.trimEnd('/') + "/api/v1/system/status") }
                            status = if (r.first) "Backend online" else "Backend unavailable"
                            payload = r.second.take(700); busy = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "CONNECTING…" else "CHECK BACKEND") }
                }
                item { InfoCard("SYSTEM", status, payload.ifBlank { "Connect your backend URL to load live system status." }) }
                item { InfoCard("RISK", "LIVE ORDERS OFF", "Android is display/control only. Paper and shadow modes remain enabled; broker order placement is not exposed by this APK.") }
                item { InfoCard("STRATEGY LAB", "ATM • OTM • DEEP OTM", "Candidate selection, historical analog evidence, risk state and paper/shadow performance are ready to be connected to the backend APIs.") }
            }
        }
    }
}

@Composable private fun MarketCard(symbol: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(symbol, color = Color.White, fontWeight = FontWeight.Bold); Text("● READY", color = Green, fontSize = 12.sp) }
            Spacer(Modifier.height(14.dp)); Text("Market feed", color = Color.Gray, fontSize = 12.sp); Text("Awaiting backend market stream", color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text("Options universe: ATM + OTM + DEEP OTM", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable private fun InfoCard(title: String, headline: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { Text(title, color = Color.Gray, fontSize = 11.sp); Text(headline, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.height(6.dp)); Text(body, color = Color.LightGray, fontSize = 12.sp) }
    }
}

private fun fetch(url: String): Pair<Boolean,String> = try {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 4000; c.readTimeout = 4000; c.requestMethod = "GET"
    val code = c.responseCode
    val stream = if (code in 200..299) c.inputStream else c.errorStream
    val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
    c.disconnect()
    (code in 200..299) to body
} catch (e: Exception) { false to (e.message ?: "connection error") }
