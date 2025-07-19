package com.example.sistempreinspectionalat

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

data class AlatData(
    @get:PropertyName("Visual") @set:PropertyName("Visual")
    var visual: List<String> = emptyList(),

    @get:PropertyName("Fungsi System") @set:PropertyName("Fungsi System")
    var fungsi_system: List<String> = emptyList(),

    var kode_alat: String = "",
    var lokasi: String = "",
    var nama: String = ""
)

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

        setContent {
            DetailLaporanScreen(kodeAlat)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLaporanScreen(kodeAlat: String) {
    val TAG = "DetailLaporanScreen"
    val firestore = FirebaseFirestore.getInstance()

    var checklist by remember { mutableStateOf<ChecklistLaporan?>(null) }
    var items by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(kodeAlat) {
        try {
            Log.d(TAG, "Mencari checklist dengan kode_alat: $kodeAlat")
            val checklistSnap = firestore.collection("checklist")
                .whereEqualTo("kode_alat", kodeAlat)
                .limit(1)
                .get()
                .await()

            if (checklistSnap.isEmpty) {
                Log.d(TAG, "Checklist tidak ditemukan untuk kode_alat: $kodeAlat")
            } else {
                val checklistData = checklistSnap.documents.firstOrNull()?.toObject<ChecklistLaporan>()
                Log.d(TAG, "Checklist ditemukan: $checklistData")

                checklistData?.let { laporan ->
                    checklist = laporan
                    val alatSnap = firestore.collection("alat")
                        .whereEqualTo("kode_alat", kodeAlat)
                        .limit(1)
                        .get()
                        .await()

                    val alatData = alatSnap.documents.firstOrNull()?.toObject<AlatData>()

                    Log.d(TAG, "Data alat: $alatData")

                    val allItems = (alatData?.visual ?: emptyList()) + (alatData?.fungsi_system ?: emptyList())
                    Log.d(TAG, "Semua item checklist: $allItems")

                    val itemStatusList = allItems.map { itemName ->
                        val status = laporan.item_statuses[itemName] ?: "N/A"
                        Log.d(TAG, "Item: $itemName → Status: $status")
                        itemName to status
                    }

                    items = itemStatusList
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat data: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Detail Laporan") })
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (checklist != null) {
                Text("Kode Alat: ${checklist!!.kode_alat}", style = MaterialTheme.typography.titleMedium)
                Text("Shift: ${checklist!!.shift}")
                Text("Tanggal: ${checklist!!.tanggal}")
                Spacer(modifier = Modifier.height(16.dp))

                Text("Checklist", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                LazyColumn {
                    items(items) { (itemName, status) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(itemName)
                            Text(status, style = MaterialTheme.typography.bodyMedium)
                        }
                        Divider()
                    }
                }
            } else {
                Text("Data tidak ditemukan atau gagal dimuat.")
            }
        }
    }
}