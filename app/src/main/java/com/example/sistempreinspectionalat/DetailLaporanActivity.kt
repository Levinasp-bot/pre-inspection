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

        // Ambil intent extras
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

                // Buat map item_statuses dari field yang value-nya "BAIK" atau "TIDAK BAIK"
                val itemStatuses = dataMap.filter { entry ->
                    val value = entry.value as? String ?: ""
                    (value == "BAIK" || value == "TIDAK BAIK")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Laporan", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0066B3)
                )
            )
        },
        containerColor = Color(0xFF0066B3)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF0066B3))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (checklist != null) {
                    Text(
                        text = "Kode Alat: ${checklist!!.kode_alat}",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Shift: ${checklist!!.shift}",
                        color = Color.Black
                    )
                    Text(
                        text = "Tanggal: ${checklist!!.tanggal}",
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    LazyColumn {
                        items(items) { (itemName, status) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = itemName,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                            Divider()
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Data tidak ditemukan atau gagal dimuat.",
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
