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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class ChecklistLaporan(
    val kode_alat: String = "",
    val shift: String = "",
    val tanggal: Any? = null,
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

                val validValues = setOf(
                    "YA", "TIDAK", "ADA", "TIDAK ADA",
                    "BAIK", "TIDAK BAIK",
                    "RUSAK", "MENYALA", "TIDAK MENYALA",
                    "BERSIH", "KOTOR",
                    "NORMAL", "TIDAK NORMAL",
                    "BERFUNGSI", "TIDAK BERFUNGSI",
                    "LANCAR", "TIDAK LANCAR"
                )

                val itemStatuses = dataMap.filter { entry ->
                    val value = entry.value as? String ?: ""
                    validValues.contains(value)
                }.map { it.key to (it.value as String) }

                checklist = ChecklistLaporan(
                    kode_alat = dataMap["kode_alat"] as? String ?: "",
                    tanggal = dataMap["tanggal"],
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

    // 🔹 UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
    ) {
        Column {
            // 🔹 Top Bar
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

                            Text(
                                text = "Checklist",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkBlue
                            )
                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(items) { (itemName, status) ->
                                    // Format nama item
                                    val namaBersih = itemName
                                        .replace("_", " ")
                                        .replace("\\s*\\(.*?\\)".toRegex(), "")
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                                    val warna = when (status.uppercase()) {
                                        "RUSAK", "KOTOR", "TIDAK NORMAL", "TIDAK BERFUNGSI", "TIDAK LANCAR", "TIDAK MENYALA", "TIDAK BAIK" -> Color.Red
                                        "BAIK", "NORMAL", "BERSIH", "BERFUNGSI", "LANCAR", "MENYALA" -> darkBlue
                                        else -> Color.Gray
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = namaBersih,
                                            fontSize = 13.sp,
                                            color = darkBlue,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        )

                                        Text(
                                            text = status,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold, // ✅ tebal
                                            color = warna,
                                            textAlign = TextAlign.End,   // ✅ rata kanan
                                            modifier = Modifier.weight(0.6f)
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
