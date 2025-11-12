package com.example.sistempreinspectionalat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

data class ChecklistLaporan(
    val kode_alat: String = "",
    val shift: String = "",
    val tanggal: String = "",
    val item_statuses: Map<String, String> = emptyMap()
)

class DetailLaporanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kodeAlat = intent.getStringExtra("kode_alat") ?: ""
        val tanggal = intent.getStringExtra("tanggal") ?: ""
        val shift = intent.getStringExtra("shift") ?: ""

        setContent {
            DetailLaporanScreen(
                kodeAlat = kodeAlat,
                tanggal = tanggal,
                shift = shift,
                onBack = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLaporanScreen(
    kodeAlat: String,
    tanggal: String,
    shift: String,
    onBack: () -> Unit
) {
    val TAG = "DetailLaporanScreen"
    val firestore = FirebaseFirestore.getInstance()
    val darkBlue = Color(0xFF003366)
    val context = LocalContext.current
    var checklist by remember { mutableStateOf<ChecklistLaporan?>(null) }
    var items by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(kodeAlat, tanggal, shift) {
        try {
            Log.d(TAG, "Query checklist dengan kode_alat=$kodeAlat, tanggal=$tanggal, shift=$shift")

            val checklistSnap = firestore.collection("checklist")
                .whereEqualTo("kode_alat", kodeAlat)
                .whereEqualTo("tanggal", tanggal)
                .whereEqualTo("shift", shift)
                .limit(1)
                .get()
                .await()

            if (checklistSnap.isEmpty) {
                Log.d(TAG, "Checklist tidak ditemukan")
                checklist = null
                items = emptyList()
            } else {
                val doc = checklistSnap.documents.first()
                val dataMap = doc.data ?: emptyMap()

                // 🔹 List kondisi valid
                val validValues = setOf(
                    "YA", "TIDAK", "ADA",
                    "RUSAK", "MENYALA", "TIDAK MENYALA",
                    "BERSIH", "KOTOR",
                    "NORMAL", "TIDAK NORMAL",
                    "BERFUNGSI", "TIDAK BERFUNGSI",
                    "LANCAR", "TIDAK LANCAR"
                )

                // 🔹 Ambil hanya field dengan value yang sesuai
                val itemStatuses = dataMap.filter { entry ->
                    val value = entry.value as? String ?: ""
                    validValues.contains(value)
                }.map { it.key to (it.value as String) }

                checklist = ChecklistLaporan(
                    kode_alat = dataMap["kode_alat"] as? String ?: "",
                    tanggal = dataMap["tanggal"] as? String ?: "",
                    shift = dataMap["shift"] as? String ?: "",
                    item_statuses = itemStatuses.toMap()
                )

                items = itemStatuses
                Log.d(TAG, "Item statuses ditemukan: $itemStatuses")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat data: ${e.message}", e)
            checklist = null
            items = emptyList()
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
    ) {
        Column {
            // 🔹 Custom TopBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(darkBlue)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Detail Laporan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        checklist != null -> {
                            Text(
                                text = "Kode Alat: ${checklist!!.kode_alat}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkBlue
                            )
                            Text(
                                text = "Shift: ${checklist!!.shift}",
                                color = Color.Gray
                            )
                            Text(
                                text = "Tanggal: ${checklist!!.tanggal}",
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Checklist section
                            Text(
                                text = "Checklist",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkBlue
                            )
                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items) { (itemName, status) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 🔹 Format nama item
                                        val formattedName = itemName
                                            .replace("_", " ") // ganti _ dengan spasi
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                                        Text(
                                            text = formattedName,
                                            fontSize = 14.sp,
                                            color = darkBlue
                                        )

                                        val statusColor = when (status.uppercase()) {
                                            "YA", "RUSAK", "TIDAK MENYALA", "KOTOR", "TIDAK NORMAL", "TIDAK BERFUNGSI", "TIDAK LANCAR", "ADA" -> Color.Red
                                            "TIDAK", "MENYALA", "BERSIH", "NORMAL", "BERFUNGSI", "LANCAR" -> darkBlue
                                            else -> Color.Gray
                                        }
                                        Text(
                                            text = status,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = statusColor
                                        )
                                    }
                                    Divider()
                                }
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Data tidak ditemukan atau gagal dimuat.",
                                    color = darkBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}