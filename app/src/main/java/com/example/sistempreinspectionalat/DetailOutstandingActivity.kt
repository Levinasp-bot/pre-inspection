package com.example.sistempreinspectionalat

import android.os.Bundle
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
import androidx.compose.material.icons.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.Locale

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
                        // Header Operator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.operator),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Operator",
                                    color = darkBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                                val ts = dataDetail.value?.get("timestamp_laporan") as? com.google.firebase.Timestamp
                                Text(
                                    text = formatTimestamp(ts),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    item {
                        // Card Operator
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
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

                                val keteranganRaw = dataDetail.value?.get("keterangan")
                                val keteranganList = when (keteranganRaw) {
                                    is List<*> -> keteranganRaw.map { it.toString() }
                                    is String -> listOf(keteranganRaw)
                                    else -> emptyList()
                                }
                                if (keteranganList.isNotEmpty()) {
                                    Text("Keterangan:", color = darkBlue, fontWeight = FontWeight.Bold)
                                    keteranganList.forEachIndexed { i, ket ->
                                        Text("${i + 1}. $ket", color = darkBlue)
                                    }
                                } else {
                                    Text("Keterangan: -", color = darkBlue)
                                }
                            }
                        }
                    }

                    // ==== TIMELINE (BIMA + TEKNIK + SPAREPART READY) ====
                    dataDetail.value?.let { detail ->
                        val allEvents = mutableListOf<Map<String, Any>>()

                        // --- BIMA
                        val bimaEntries = detail.filterKeys { key ->
                            key.startsWith("tanggapan_bima_") && key.removePrefix("tanggapan_bima_").all { it.isDigit() }
                        }
                        bimaEntries.forEach { (key, value) ->
                            val index = key.removePrefix("tanggapan_bima_")
                            val tanggapanList = value as? List<String> ?: emptyList()

                            val hari = detail["estimasi_hari_$index"]?.toString()
                            val jam = detail["estimasi_jam_$index"]?.toString()
                            val menit = detail["estimasi_menit_$index"]?.toString()

                            val statusSparepart = detail["status_sparepart_$index"]?.toString() ?: "Tidak"

                            val spareHari = detail["sparepart_estimasi_hari_$index"]?.toString()
                            val spareJam = detail["sparepart_estimasi_jam_$index"]?.toString()
                            val spareMenit = detail["sparepart_estimasi_menit_$index"]?.toString()

                            val estimasi = formatEstimasi(hari, jam, menit)
                            val spareEstimasi = if (statusSparepart == "Indent") formatEstimasi(spareHari, spareJam, spareMenit) else ""

                            val tsBima = detail["tanggapan_bima_timestamp_$index"] as? com.google.firebase.Timestamp

                            allEvents.add(
                                mapOf<String, Any>(
                                    "type" to "bima",
                                    "index" to index,
                                    "tanggapanList" to tanggapanList,
                                    "estimasi" to estimasi,
                                    "sparepart" to spareEstimasi,
                                    "timestamp" to (tsBima?.seconds ?: 0L),
                                    "ts" to (tsBima ?: "")
                                )
                            )

                            // --- Teknik
                            val rejectList = (detail["keterangan_reject_$index"] as? List<*>)?.map { it.toString() }
                                ?: listOfNotNull(detail["keterangan_reject_$index"] as? String)
                            val instruksiList = (detail["instruksi_teknik_$index"] as? List<*>)?.map { it.toString() }
                                ?: listOfNotNull(detail["instruksi_teknik_$index"] as? String)

                            val tsReject = detail["keterangan_reject_timestamp_$index"] as? com.google.firebase.Timestamp
                            val tsInstruksi = detail["instruksi_teknik_timestamp_$index"] as? com.google.firebase.Timestamp

                            // Reject jadi event sendiri
                            if (rejectList.isNotEmpty()) {
                                allEvents.add(
                                    mapOf(
                                        "type" to "reject",
                                        "index" to index,
                                        "rejectList" to rejectList,
                                        "timestamp" to (tsReject?.seconds ?: 0L),
                                        "ts" to (tsReject ?: "")
                                    )
                                )
                            }

                            // Instruksi jadi event sendiri
                            if (instruksiList.isNotEmpty()) {
                                allEvents.add(
                                    mapOf(
                                        "type" to "instruksi",
                                        "index" to index,
                                        "instruksiList" to instruksiList,
                                        "timestamp" to (tsInstruksi?.seconds ?: 0L),
                                        "ts" to (tsInstruksi ?: "")
                                    )
                                )
                            }
                        }

                        // --- Sparepart Ready
                        val sparepartReadyTs = detail["sparepart_ready_timestamp"] as? com.google.firebase.Timestamp
                        if (sparepartReadyTs != null) {
                            allEvents.add(
                                mapOf(
                                    "type" to "sparepart_ready",
                                    "timestamp" to (sparepartReadyTs.seconds),
                                    "ts" to sparepartReadyTs
                                )
                            )
                        }

                        // --- Laporan Perbaikan PT BIMA
                        detail.keys.filter { it.startsWith("keterangan_perbaikan_") }
                            .mapNotNull { key ->
                                val index = key.removePrefix("keterangan_perbaikan_").toIntOrNull() ?: return@mapNotNull null
                                if (index % 2 == 0) index else null // hanya index genap
                            }
                            .forEach { index ->
                                val perbaikanList = (detail["keterangan_perbaikan_$index"] as? List<*>)?.map { it.toString() }
                                    ?: listOfNotNull(detail["keterangan_perbaikan_$index"] as? String)

                                val tsPerbaikan = detail["keterangan_perbaikan_${index}_timestamp"] as? com.google.firebase.Timestamp
                                val gambarPerbaikan = detail["gambar_perbaikan_$index"] as? String

                                if (perbaikanList.isNotEmpty() || !gambarPerbaikan.isNullOrEmpty()) {
                                    allEvents.add(
                                        mapOf(
                                            "type" to "perbaikan",
                                            "index" to index,
                                            "perbaikanList" to perbaikanList,
                                            "gambarPerbaikan" to (gambarPerbaikan ?: ""),
                                            "timestamp" to (tsPerbaikan?.seconds ?: 0L),
                                            "ts" to (tsPerbaikan ?: "")
                                        )
                                    )
                                }
                            }

                        // --- Revisi Teknik (index ganjil)
                        detail.keys.filter { it.startsWith("keterangan_perbaikan_") }
                            .mapNotNull { key ->
                                val idx = key.removePrefix("keterangan_perbaikan_").toIntOrNull()
                                if (idx != null && idx % 2 == 1) idx else null
                            }
                            .forEach { idx ->
                                val revisiList = (detail["keterangan_perbaikan_$idx"] as? List<*>)?.map { it.toString() }
                                    ?: listOfNotNull(detail["keterangan_perbaikan_$idx"] as? String)

                                val tsRevisi = detail["keterangan_perbaikan_${idx}_timestamp"] as? com.google.firebase.Timestamp
                                val gambarRevisi = detail["gambar_perbaikan_$idx"] as? String  // ambil gambar sesuai index

                                if (revisiList.isNotEmpty() || !gambarRevisi.isNullOrEmpty()) {
                                    allEvents.add(
                                        mapOf(
                                            "type" to "revisi",
                                            "index" to idx,
                                            "revisiList" to revisiList,
                                            "gambarPerbaikan" to (gambarRevisi ?: ""),  // masukkan ke event
                                            "timestamp" to (tsRevisi?.seconds ?: 0L),
                                            "ts" to (tsRevisi ?: "")
                                        )
                                    )
                                }
                            }

                        // --- Konfirmasi Teknik
                        val konfirmasiTs = detail["konfirmasi_teknik_timestamp"] as? com.google.firebase.Timestamp
                        if (konfirmasiTs != null) {
                            allEvents.add(
                                mapOf(
                                    "type" to "konfirmasi",
                                    "timestamp" to konfirmasiTs.seconds,
                                    "ts" to konfirmasiTs
                                )
                            )
                        }

                        // --- Operator: Masih terdapat kerusakan
                        detail.keys.filter { it.startsWith("keterangan_") }
                            .mapNotNull { key ->
                                val index = key.removePrefix("keterangan_").toIntOrNull()
                            }
                            .forEach { idx ->
                                val keteranganList = (detail["keterangan_$idx"] as? List<*>)?.map { it.toString() }
                                    ?: listOfNotNull(detail["keterangan_$idx"] as? String)
                                val gambarUrl = detail["gambar_$idx"] as? String
                                val tsOperator = detail["keterangan_${idx}_timestamp"] as? com.google.firebase.Timestamp

                                if (keteranganList.isNotEmpty() || !gambarUrl.isNullOrEmpty()) {
                                    allEvents.add(
                                        mapOf(
                                            "type" to "operator",
                                            "index" to idx,
                                            "keteranganList" to keteranganList,
                                            "gambarUrl" to (gambarUrl ?: ""),
                                            "timestamp" to (tsOperator?.seconds ?: 0L),
                                            "ts" to (tsOperator ?: "")
                                        )
                                    )
                                }
                            }

                        // --- Konfirmasi Operator
                        val konfirmasiOperatorTs = detail["konfirmasi_operator_timestamp"] as? com.google.firebase.Timestamp
                        if (konfirmasiOperatorTs != null) {
                            allEvents.add(
                                mapOf(
                                    "type" to "konfirmasi_operator",
                                    "timestamp" to konfirmasiOperatorTs.seconds,
                                    "ts" to konfirmasiOperatorTs
                                )
                            )
                        }

                        // --- Verifikasi Manager
                        val verifikasiManagerTs = detail["verifikasi_manager_timestamp"] as? com.google.firebase.Timestamp
                        if (verifikasiManagerTs != null) {
                            allEvents.add(
                                mapOf(
                                    "type" to "verifikasi_manager",
                                    "timestamp" to verifikasiManagerTs.seconds,
                                    "ts" to verifikasiManagerTs
                                )
                            )
                        }

                        // Urutkan ASC
                        val sortedEvents = allEvents.sortedBy { it["timestamp"] as Long }

                        items(sortedEvents.size) { idx ->
                            val event = sortedEvents[idx]
                            when (event["type"]) {
                                "operator" -> TimelineOperatorCard(event, darkBlue)
                                "konfirmasi_operator" -> TimelineKonfirmasiOperatorCard(event, darkBlue)
                                "bima" -> TimelineBimaCard(event, darkBlue)
                                "reject" -> TimelineRejectCard(event, darkBlue)
                                "instruksi" -> TimelineInstruksiCard(event, darkBlue)
                                "perbaikan" -> TimelinePerbaikanCard(event, darkBlue)
                                "revisi" -> TimelineRevisiCard(event, darkBlue)
                                "konfirmasi" -> TimelineKonfirmasiCard(event, darkBlue)
                                "sparepart_ready" -> TimelineSparepartReadyCard(event, darkBlue)
                                "verifikasi_manager" -> TimelineVerifikasiManagerCard(event, darkBlue) // 🔹 ini tambahan
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatEstimasi(hari: String?, jam: String?, menit: String?): String {
    val parts = mutableListOf<String>()
    if (!hari.isNullOrBlank() && hari != "0") parts.add("$hari hari")
    if (!jam.isNullOrBlank() && jam != "0") parts.add("$jam jam")
    if (!menit.isNullOrBlank() && menit != "0") parts.add("$menit menit")
    return if (parts.isEmpty()) "-" else parts.joinToString(" ")
}

@Composable
fun TimelineBimaCard(event: Map<String, Any>, darkBlue: Color) {
    val tanggapanList = event["tanggapanList"] as List<String>
    val estimasi = event["estimasi"] as String
    val sparepart = event["sparepart"] as String?
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.bima),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "PT BIMA",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (tanggapanList.isNotEmpty()) {
                Text("Tanggapan BIMA:", color = darkBlue, fontWeight = FontWeight.Bold)
                tanggapanList.forEachIndexed { i, t -> Text("${i + 1}. $t", color = darkBlue) }
            }
            Text("Estimasi Waktu Perbaikan: $estimasi", color = darkBlue)
            if (!sparepart.isNullOrBlank() && sparepart != "Tidak") {
                Text("Estimasi Waktu Indent: $sparepart", color = darkBlue)
            }
        }
    }
}

@Composable
fun TimelineRejectCard(event: Map<String, Any>, darkBlue: Color) {
    val rejectList = event["rejectList"] as List<String>
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.teknik),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Teknik - Reject",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Keterangan Reject:", color = darkBlue, fontWeight = FontWeight.Bold)
            rejectList.forEachIndexed { i, ket -> Text("${i + 1}. $ket", color = darkBlue) }
        }
    }
}

@Composable
fun TimelineInstruksiCard(event: Map<String, Any>, darkBlue: Color) {
    val instruksiList = event["instruksiList"] as List<String>
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.teknik),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Teknik - Instruksi",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Instruksi Perbaikan:", color = darkBlue, fontWeight = FontWeight.Bold)
            instruksiList.forEachIndexed { i, ins -> Text("${i + 1}. $ins", color = darkBlue) }
        }
    }
}

@Composable
fun TimelineSparepartReadyCard(event: Map<String, Any>, darkBlue: Color) {
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.bima),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "PT BIMA",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Sparepart sudah tersedia, alat dalam proses perbaikan.", color = darkBlue)
        }
    }
}

@Composable
fun TimelinePerbaikanCard(event: Map<String, Any>, darkBlue: Color) {
    val perbaikanList = event["perbaikanList"] as List<String>
    val gambarPerbaikan = event["gambarPerbaikan"] as? String
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.bima),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "PT BIMA - Laporan Perbaikan",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 🔹 Tampilkan gambar perbaikan paling atas
            if (!gambarPerbaikan.isNullOrEmpty()) {
                AsyncImage(
                    model = gambarPerbaikan,
                    contentDescription = "Gambar Perbaikan",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 🔹 Tampilkan keterangan perbaikan
            Text("Laporan Perbaikan:", color = darkBlue, fontWeight = FontWeight.Bold)
            if (perbaikanList.isNotEmpty()) {
                perbaikanList.forEachIndexed { i, ket ->
                    Text("${i + 1}. $ket", color = darkBlue)
                }
            } else {
                Text("Tidak ada keterangan perbaikan", color = darkBlue)
            }
        }
    }
}

@Composable
fun TimelineRevisiCard(event: Map<String, Any>, darkBlue: Color) {
    val revisiList = event["revisiList"] as List<String>
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)
    val gambar = event["gambarPerbaikan"] as? String

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.teknik),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Teknik - Revisi",
                    color = darkBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Revisi:", color = darkBlue, fontWeight = FontWeight.Bold)
                revisiList.forEachIndexed { i, ket ->
                    Text("${i + 1}. $ket", color = darkBlue)
                }

                // Tampilkan gambar jika ada
                if (!gambar.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = gambar,
                        contentDescription = "Gambar Revisi",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineKonfirmasiCard(event: Map<String, Any>, darkBlue: Color) {
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.teknik),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Teknik - Konfirmasi",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Perbaikan sudah dikonfirmasi.", color = darkBlue)
        }
    }
}

@Composable
fun TimelineOperatorCard(event: Map<String, Any>, darkBlue: Color) {
    val keteranganList = event["keteranganList"] as List<String>
    val gambarUrl = event["gambarUrl"] as String
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.operator),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Operator",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Masih terdapat kerusakan", color = darkBlue, fontWeight = FontWeight.Bold)

            if (keteranganList.isNotEmpty()) {
                Text("Keterangan:", color = darkBlue, fontWeight = FontWeight.Bold)
                keteranganList.forEachIndexed { i, ket ->
                    Text("${i + 1}. $ket", color = darkBlue)
                }
            }

            if (gambarUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = gambarUrl,
                    contentDescription = "Gambar Kerusakan",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun TimelineKonfirmasiOperatorCard(event: Map<String, Any>, darkBlue: Color) {
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.operator),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Operator - Konfirmasi",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Perbaikan sudah dikonfirmasi oleh Operator.", color = darkBlue)
        }
    }
}

@Composable
fun TimelineVerifikasiManagerCard(event: Map<String, Any>, darkBlue: Color) {
    val ts = event["ts"] as? com.google.firebase.Timestamp
    val timestampStr = formatTimestamp(ts)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.manager), // 🔹 pastikan ada icon manager
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Manager",
                color = darkBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(1.dp, darkBlue, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Text(timestampStr, fontSize = 12.sp, color = Color.Gray)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, darkBlue, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Perbaikan telah diverifikasi oleh Manager, laporan ditutup.",
                color = darkBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatTimestamp(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return "-"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(ts.toDate())
}