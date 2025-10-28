package com.example.sistempreinspectionalat

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme

class DetailAlatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kodeAlat = intent.getStringExtra("kode_alat") ?: ""

        setContent {
            SistemPreinspectionAlatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DetailAlatScreen(kodeAlat)
                }
            }
        }
    }
}

// 🔠 Fungsi bantu untuk kapitalisasi setiap kata
fun String.capitalizeWords(): String =
    this.lowercase().split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAlatScreen(kodeAlat: String) {
    val context = LocalContext.current
    val darkBlue = Color(0xFF003366)
    val firestore = FirebaseFirestore.getInstance()
    var alat by remember { mutableStateOf<Map<String, Any>?>(null) }
    var kondisiTerkini by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var riwayatPerbaikan by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var tanggalTerbaru by remember { mutableStateOf("-") }
    var operatorTerakhir by remember { mutableStateOf("-") }

    LaunchedEffect(kodeAlat) {
        try {
            val querySnapshot = firestore.collection("alat")
                .whereEqualTo("kode_alat", kodeAlat)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val alatSnapshot = querySnapshot.documents[0]
                alat = alatSnapshot.data
                val nama = alatSnapshot.getString("nama")
                Log.d("DetailAlat", "Data alat ditemukan: nama = $nama")
            } else {
                Log.w("DetailAlat", "Tidak ada alat dengan kode_alat: $kodeAlat")
            }

            val checklistSnapshot = firestore.collection("checklist")
                .whereEqualTo("kode_alat", kodeAlat)
                .get().await()

            val latestChecklist = checklistSnapshot.documents
                .sortedWith(compareByDescending {
                    val tanggal = it.getString("tanggal") ?: ""
                    val shift = it.getString("shift") ?: ""
                    tanggal + shift
                }).firstOrNull()

            tanggalTerbaru = latestChecklist?.getString("tanggal") ?: "-"
            operatorTerakhir = latestChecklist?.getString("operator") ?: "-"

            val kondisi = latestChecklist?.data?.filterKeys {
                it != "kode_alat" && it != "tanggal" && it != "shift" && it != "operator"
            }?.mapValues { it.value.toString() } ?: emptyMap()
            kondisiTerkini = kondisi

            val outstandingSnapshot = firestore.collection("outstanding")
                .whereEqualTo("kode_alat", kodeAlat)
                .get().await()
            riwayatPerbaikan = outstandingSnapshot.documents.mapNotNull { it.data }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .background(darkBlue)) {

        Column {
            // 🔹 Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(darkBlue)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = kodeAlat,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // 🔹 Konten Utama
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    // --- Data Alat ---
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, darkBlue, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(kodeAlat, fontSize = 22.sp, color = darkBlue, fontWeight = FontWeight.Bold)
                                Text(
                                    alat?.get("nama")?.toString()?.capitalizeWords() ?: "",
                                    fontSize = 14.sp,
                                    color = darkBlue
                                )
                                Text("Inspeksi Terakhir: $tanggalTerbaru", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- Kondisi Terkini ---
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, darkBlue, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Kondisi Terkini", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = darkBlue)
                                Spacer(modifier = Modifier.height(8.dp))
                                kondisiTerkini.forEach { (komponen, kondisi) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = komponen.replace("_", " ").capitalizeWords(),
                                            fontSize = 13.sp,
                                            color = darkBlue
                                        )
                                        Text(
                                            text = kondisi.capitalizeWords(),
                                            fontSize = 13.sp,
                                            color = darkBlue
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- Riwayat Perbaikan ---
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, darkBlue, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Riwayat Perbaikan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = darkBlue)
                                Spacer(modifier = Modifier.height(8.dp))
                                riwayatPerbaikan.forEach { item ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = item["tanggal"]?.toString() ?: "",
                                            fontWeight = FontWeight.SemiBold,
                                            color = darkBlue
                                        )
                                        Text(
                                            text = item["item"]?.toString()?.capitalizeWords() ?: "",
                                            fontSize = 13.sp,
                                            color = darkBlue
                                        )

                                        // Cari key keterangan perbaikan terbaru
                                        val tindakanKeys = item.keys
                                            .filter { it.startsWith("keterangan_perbaikan_") }
                                            .mapNotNull { key ->
                                                val index = key.removePrefix("keterangan_perbaikan_").toIntOrNull()
                                                if (index != null && index % 2 == 0) index to key else null
                                            }
                                            .sortedByDescending { it.first }
                                            .map { it.second }

                                        val tindakanTerbaru = tindakanKeys.firstOrNull()?.let { key ->
                                            item[key]?.toString()?.capitalizeWords() ?: ""
                                        } ?: ""

                                        Text(
                                            text = "Tindakan: $tindakanTerbaru",
                                            fontSize = 13.sp,
                                            color = darkBlue
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
