package com.example.sistempreinspectionalat

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

class DetailOutstandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kodeAlat = intent.getStringExtra("kode_alat") ?: ""
        val item = intent.getStringExtra("item") ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    DetailOutstandingScreen(
                        kodeAlat = kodeAlat,
                        item = item,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailOutstandingScreen(
    kodeAlat: String,
    item: String,
    onBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val dataDetail = remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(kodeAlat, item) {
        firestore.collection("outstanding")
            .whereEqualTo("kode_alat", kodeAlat)
            .whereEqualTo("item", item)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    dataDetail.value = result.documents[0].data
                }
            }
    }

    val darkBlue = Color(0xFF003366)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ===== HEADER =====
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detail Outstanding",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // ===== CONTENT =====
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ==== OPERATOR ====
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.operator),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Operator",
                                color = darkBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val imageUrl = dataDetail.value?.get("gambar") as? String

                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Gambar",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Item: ${dataDetail.value?.get("item") ?: item}", color = darkBlue)
                                Text("Kondisi: ${dataDetail.value?.get("kondisi") ?: "-"}", color = darkBlue)
                                Text("Keterangan: ${dataDetail.value?.get("keterangan") ?: "-"}", color = darkBlue)
                            }
                        }
                    }
                    // ==== TANGGAPAN BIMA ====
                    dataDetail.value?.let { detail ->
                        // Ambil semua key tanggapan_bima_ yang diakhiri angka
                        val bimaEntries = detail.filterKeys { key ->
                            key.startsWith("tanggapan_bima_") && key.removePrefix("tanggapan_bima_").all { it.isDigit() }
                        }

                        if (bimaEntries.isNotEmpty()) {
                            val listTanggapan = bimaEntries.map { (key, value) ->
                                val index = key.removePrefix("tanggapan_bima_")

                                val tanggapanList = value as? List<String> ?: emptyList()

                                val hari = detail["estimasi_hari_$index"]?.toString() ?: "0"
                                val jam = detail["estimasi_jam_$index"]?.toString() ?: "0"
                                val menit = detail["estimasi_menit_$index"]?.toString() ?: "0"

                                val spareHari = detail["sparepart_estimasi_hari_$index"]?.toString() ?: "0"
                                val spareJam = detail["sparepart_estimasi_jam_$index"]?.toString() ?: "0"
                                val spareMenit = detail["sparepart_estimasi_menit_$index"]?.toString() ?: "0"

                                val ts = detail["tanggapan_bima_timestamp_$index"] as? com.google.firebase.Timestamp
                                val timestamp = ts?.seconds ?: 0L

                                Log.d("DEBUG_ORDER_BEFORE", "Index=$index | Timestamp=$timestamp | List=$tanggapanList")

                                mapOf(
                                    "index" to index,
                                    "tanggapanList" to tanggapanList,
                                    "estimasi" to "$hari hari $jam jam $menit menit",
                                    "sparepart" to if (spareHari != null && spareJam != null && spareMenit != null)
                                        "$spareHari hari $spareJam jam $spareMenit menit"
                                    else null,
                                    "timestamp" to timestamp
                                )
                            }.sortedBy { it["timestamp"] as Long }
                                .also { sorted ->
                                    sorted.forEach {
                                        Log.d("DEBUG_ORDER_AFTER", "Index=${it["index"]} | Timestamp=${it["timestamp"]} | List=${it["tanggapanList"]}")
                                    }
                                }

                        items(listTanggapan.size) { idx ->
                                val itemBima = listTanggapan[idx]
                                val tanggapanList = itemBima["tanggapanList"] as List<String>
                                val index = itemBima["index"] as String

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.bima),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PT BIMA",
                                        color = darkBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                                            .background(Color.White, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        tanggapanList.forEach { tanggapan ->
                                            Text("Tanggapan BIMA: $tanggapan", color = darkBlue)
                                        }
                                        Text("Estimasi Waktu Perbaikan: ${itemBima["estimasi"]}", color = darkBlue)
                                        itemBima["sparepart"]?.let {
                                            Text("Estimasi Waktu Indent: $it", color = darkBlue)
                                        }
                                    }
                                }

                                // ==== TEKNIK ====
                                val rejectRaw = detail["keterangan_reject_$index"]
                                val rejectList = when (rejectRaw) {
                                    is List<*> -> rejectRaw.map { it.toString() } // Firestore Array
                                    is String -> listOf(rejectRaw) // kalau ternyata single string
                                    else -> emptyList()
                                }

                                val instruksiRaw = detail["instruksi_teknik_$index"]
                                val instruksiList = when (instruksiRaw) {
                                    is List<*> -> instruksiRaw.map { it.toString() }
                                    is String -> listOf(instruksiRaw)
                                    else -> emptyList()
                                }

                                Log.d("DEBUG_TEKNIK", "Index = $index")
                                Log.d("DEBUG_TEKNIK", "keterangan_reject_$index -> $rejectList")
                                Log.d("DEBUG_TEKNIK", "instruksi_teknik_$index -> $instruksiList")

                                if (rejectList.isNotEmpty() || instruksiList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = painterResource(id = R.drawable.teknik), // ganti dengan icon teknik
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Teknik",
                                            color = darkBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                                                .background(Color.White, RoundedCornerShape(16.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (rejectList.isNotEmpty()) {
                                                Text("Keterangan Reject:", color = darkBlue, fontWeight = FontWeight.Bold)
                                                rejectList.forEachIndexed { i, ket ->
                                                    Text("${i + 1}. $ket", color = darkBlue)
                                                }
                                            }
                                            if (instruksiList.isNotEmpty()) {
                                                Text("Instruksi Perbaikan:", color = darkBlue, fontWeight = FontWeight.Bold)
                                                instruksiList.forEachIndexed { i, instruksi ->
                                                    Text("${i + 1}. $instruksi", color = darkBlue)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}