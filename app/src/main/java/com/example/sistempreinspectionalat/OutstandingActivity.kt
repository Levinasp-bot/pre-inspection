package com.example.sistempreinspectionalat

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FieldValue

class OutstandingActivity : ComponentActivity() {
    private val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dutgwdhss/image/upload"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SistemPreinspectionAlatTheme {
                OutstandingChecklistScreen()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OutstandingChecklistScreen() {
        val firestore = FirebaseFirestore.getInstance()
        val checklistList = remember { mutableStateListOf<Map<String, Any>>() }
        val alatMap = remember { mutableStateMapOf<String, Map<String, Any>>() }
        val showDialogTeknik = remember { mutableStateOf(false) }
        val showDialogBima = remember { mutableStateOf(false) }
        val showDialog = remember { mutableStateOf(false) }
        val showPerbaikanDialog = remember { mutableStateOf(false) }
        val keteranganPerbaikan = remember { mutableStateOf("") }
        val perbaikanKeterangan = remember { mutableStateOf("") }
        val perbaikanFotoUri = remember { mutableStateOf<Uri?>(null) }
        val context = LocalContext.current
        val reloadTrigger = remember { mutableStateOf(false) }
        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            perbaikanFotoUri.value = uri
        }

        LaunchedEffect(reloadTrigger.value) {
            val firestore = FirebaseFirestore.getInstance()

            firestore.collection("alat")
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val data = doc.data
                        val kodeAlat = data["kode_alat"]?.toString() ?: continue
                        alatMap[kodeAlat] = data
                    }
                }

            firestore.collection("outstanding")
                .whereEqualTo("outstanding", true)
                .get()
                .addOnSuccessListener { result ->
                    checklistList.clear()

                    for (doc in result) {
                        val data = doc.data
                        val item = mutableMapOf<String, Any>(
                            "kode_alat" to (data["kode_alat"] ?: ""),
                            "tanggal" to (data["tanggal"] ?: ""),
                            "shift" to (data["shift"] ?: ""),
                            "item" to (data["item"] ?: ""),
                            "kondisi" to (data["kondisi"] ?: ""),
                            "keterangan" to (data["keterangan"] ?: ""),
                            "gambar" to (data["gambar"] ?: ""),
                            "status_perbaikan" to (data["status_perbaikan"] ?: ""),
                            "operator_email" to (data["operator_email"] ?: ""),

                            // ✅ estimasi waktu pengerjaan
                            "estimasi_hari" to (data["estimasi_hari"] ?: 0),
                            "estimasi_jam" to (data["estimasi_jam"] ?: 0),
                            "estimasi_menit" to (data["estimasi_menit"] ?: 0),

                            // ✅ estimasi pengadaan sparepart
                            "sparepart_estimasi_hari" to (data["sparepart_estimasi_hari"] ?: 0),
                            "sparepart_estimasi_jam" to (data["sparepart_estimasi_jam"] ?: 0),
                            "sparepart_estimasi_menit" to (data["sparepart_estimasi_menit"] ?: 0)
                        )

                        // ✅ masukkan field dinamis
                        for ((key, value) in data) {
                            if (
                                key.startsWith("keterangan_perbaikan_") ||
                                key.startsWith("gambar_perbaikan_") ||
                                key.startsWith("tanggapan_bima_") ||
                                key.startsWith("instruksi_teknik_") ||
                                key.startsWith("keterangan_reject_")
                            ) {
                                item[key] = value ?: ""
                            }
                        }

                        Log.d("DocumentDebug", "Dokumen ID: ${doc.id}")
                        Log.d("DocumentDebug", "Semua key diambil: ${item.keys}")

                        checklistList.add(item)
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF003366)) // darkBlue
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ===== HEADER CUSTOM =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Outstanding Checklist",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // ===== CONTENT (PUTIH ROUNDED) =====
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(checklistList) { checklist ->
                            val statusPerbaikan = checklist["status_perbaikan"]?.toString() ?: ""
                            val kodeAlat = checklist["kode_alat"]?.toString() ?: ""
                            val alatInfo = alatMap[kodeAlat]
                            val namaAlat = alatInfo?.get("nama")?.toString() ?: ""
                            val showImage = remember { mutableStateOf(false) }
                            val fotoUrl = getLatestImageField(checklist)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 4.dp,
                                        color = Color(0xFFD32F2F),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$kodeAlat – $namaAlat",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${checklist["tanggal"]} | ${checklist["shift"]}",
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Status: ${checklist["status_perbaikan"]}",
                                        fontSize = 14.sp
                                    )
                                    Text(text = "Item: ${checklist["item"]}", fontSize = 14.sp)
                                    Text(
                                        text = "Kondisi: ${checklist["kondisi"]}",
                                        fontSize = 14.sp
                                    )
                                    val latestKeterangan = getLatestKeteranganField(checklist)
                                    if (!latestKeterangan.isNullOrBlank()) {
                                        Text(
                                            text = "Keterangan: $latestKeterangan",
                                            fontSize = 14.sp
                                        )
                                    }

                                    val estimasiHari = checklist["estimasi_hari"]?.toString()?.toIntOrNull()
                                    val estimasiJam = checklist["estimasi_jam"]?.toString()?.toIntOrNull()
                                    val estimasiMenit = checklist["estimasi_menit"]?.toString()?.toIntOrNull()

                                    if (estimasiHari != null || estimasiJam != null || estimasiMenit != null) {
                                        Text(
                                            text = "Estimasi Pengerjaan: ${estimasiHari ?: 0} hari, ${estimasiJam ?: 0} jam, ${estimasiMenit ?: 0} menit",
                                            fontSize = 14.sp,
                                            color = Color(0xFF388E3C), // hijau biar beda
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    val spareHari = checklist["sparepart_estimasi_hari"]?.toString()?.toIntOrNull()
                                    val spareJam = checklist["sparepart_estimasi_jam"]?.toString()?.toIntOrNull()
                                    val spareMenit = checklist["sparepart_estimasi_menit"]?.toString()?.toIntOrNull()

                                    if (spareHari != null || spareJam != null || spareMenit != null) {
                                        Text(
                                            text = "Estimasi Pengadaan Sparepart: ${spareHari ?: 0} hari, ${spareJam ?: 0} jam, ${spareMenit ?: 0} menit",
                                            fontSize = 14.sp,
                                            color = Color(0xFF6A1B9A), // ungu biar beda
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (statusPerbaikan == "menunggu tanggapan PT BIMA") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val isSubmitting = remember { mutableStateOf(false) }
                                        val estimasiHari = remember { mutableStateOf("") }
                                        val estimasiJam = remember { mutableStateOf("") }
                                        val estimasiMenit = remember { mutableStateOf("") }
                                        val selectedSparepartStatus =
                                            remember { mutableStateOf("") }
                                        val tanggapanList = remember { mutableStateListOf("") }
                                        val sparepartHari = remember { mutableStateOf("") }
                                        val sparepartJam = remember { mutableStateOf("") }
                                        val sparepartMenit = remember { mutableStateOf("") }

                                        OutlinedButton(
                                            onClick = { showImage.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF003366)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF003366)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lihat Foto")
                                        }

                                        if (showImage.value) {
                                            AlertDialog(
                                                onDismissRequest = { showImage.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showImage.value = false
                                                    }) {
                                                        Text("Tutup")
                                                    }
                                                },
                                                text = {
                                                    AsyncImage(
                                                        model = fotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = { showDialogBima.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF003366),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Beri Tanggapan")
                                        }

                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }
                                        if (showDialogBima.value) {
                                            AlertDialog(
                                                modifier = Modifier.fillMaxWidth(0.95f),
                                                onDismissRequest = {
                                                    showDialogBima.value = false
                                                    tanggapanList.clear()
                                                    tanggapanList.add("") // reset ke satu input kosong
                                                    estimasiHari.value = ""
                                                    estimasiJam.value = ""
                                                    estimasiMenit.value = ""
                                                    selectedSparepartStatus.value = ""
                                                    sparepartHari.value = ""
                                                    sparepartJam.value = ""
                                                    sparepartMenit.value = ""
                                                },
                                                title = {
                                                    Text(
                                                        "Tanggapan Perbaikan",
                                                        style = MaterialTheme.typography.titleLarge
                                                    )
                                                },
                                                text = {
                                                    Column(
                                                        modifier = Modifier.verticalScroll(
                                                            rememberScrollState()
                                                        )
                                                    ) {
                                                        Text("Silakan masukkan tanggapan Anda.")
                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        // List input tanggapan
                                                        tanggapanList.forEachIndexed { index, value ->
                                                            OutlinedTextField(
                                                                value = value,
                                                                onValueChange = { newValue ->
                                                                    tanggapanList[index] = newValue
                                                                },
                                                                placeholder = { Text("Masukkan tanggapan...") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                        }

                                                        // Tombol tambah input baru
                                                        TextButton(onClick = { tanggapanList.add("") }) {
                                                            Text("+ Tambah Tanggapan")
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text("Estimasi Waktu Pengerjaan:")
                                                        Row(
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            OutlinedTextField(
                                                                value = estimasiHari.value,
                                                                onValueChange = {
                                                                    estimasiHari.value = it
                                                                },
                                                                label = { Text("Hari") },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            OutlinedTextField(
                                                                value = estimasiJam.value,
                                                                onValueChange = {
                                                                    estimasiJam.value = it
                                                                },
                                                                label = { Text("Jam") },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            OutlinedTextField(
                                                                value = estimasiMenit.value,
                                                                onValueChange = {
                                                                    estimasiMenit.value = it
                                                                },
                                                                label = { Text("Menit") },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Text("Apakah perlu indent sparepart?")
                                                        val options = listOf("Indent", "Tidak")
                                                        options.forEach { option ->
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                RadioButton(
                                                                    selected = selectedSparepartStatus.value == option,
                                                                    onClick = {
                                                                        selectedSparepartStatus.value =
                                                                            option
                                                                    }
                                                                )
                                                                Text(option)
                                                            }
                                                        }

                                                        if (selectedSparepartStatus.value == "Indent") {
                                                            Spacer(modifier = Modifier.height(12.dp))
                                                            Text("Estimasi Waktu Pengadaan Sparepart:")
                                                            Row(
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                OutlinedTextField(
                                                                    value = sparepartHari.value,
                                                                    onValueChange = {
                                                                        sparepartHari.value = it
                                                                    },
                                                                    label = { Text("Hari") },
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                OutlinedTextField(
                                                                    value = sparepartJam.value,
                                                                    onValueChange = {
                                                                        sparepartJam.value = it
                                                                    },
                                                                    label = { Text("Jam") },
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                OutlinedTextField(
                                                                    value = sparepartMenit.value,
                                                                    onValueChange = {
                                                                        sparepartMenit.value = it
                                                                    },
                                                                    label = { Text("Menit") },
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            showDialogBima.value = false
                                                            isSubmitting.value = true

                                                            val firestore =
                                                                FirebaseFirestore.getInstance()
                                                            val docRef =
                                                                firestore.collection("outstanding")
                                                                    .whereEqualTo(
                                                                        "kode_alat",
                                                                        kodeAlat
                                                                    )
                                                                    .whereEqualTo(
                                                                        "tanggal",
                                                                        checklist["tanggal"]
                                                                    )
                                                                    .whereEqualTo(
                                                                        "item",
                                                                        checklist["item"]
                                                                    )
                                                                    .limit(1)

                                                            docRef.get()
                                                                .addOnSuccessListener { result ->
                                                                    if (!result.isEmpty) {
                                                                        val doc =
                                                                            result.documents[0]
                                                                        val docId = doc.id

                                                                        // Cari index terakhir dari field tanggapan_bima_x (exclude timestamp)
                                                                        val existingKeys = doc.data?.keys?.filter {
                                                                            it.startsWith("tanggapan_bima_") && !it.contains("timestamp")
                                                                        } ?: emptyList()

                                                                        val lastIndex = existingKeys.mapNotNull {
                                                                            it.removePrefix("tanggapan_bima_").toIntOrNull()
                                                                        }.maxOrNull() ?: 0

                                                                        val nextIndex = lastIndex + 1
                                                                        val fieldName = "tanggapan_bima_$nextIndex"
                                                                        val tsFieldName = "tanggapan_bima_timestamp_$nextIndex"

// Data yang akan diupdate
                                                                        val updateData = mutableMapOf<String, Any>(
                                                                            fieldName to tanggapanList.filter { it.isNotBlank() },
                                                                            tsFieldName to FieldValue.serverTimestamp(),
                                                                            "estimasi_hari" to estimasiHari.value,
                                                                            "estimasi_jam" to estimasiJam.value,
                                                                            "estimasi_menit" to estimasiMenit.value,
                                                                            "status_sparepart" to selectedSparepartStatus.value,
                                                                            "status_perbaikan" to "menunggu tanggapan teknik"
                                                                        )

                                                                        if (selectedSparepartStatus.value == "Indent") {
                                                                            updateData["sparepart_estimasi_hari"] = sparepartHari.value
                                                                            updateData["sparepart_estimasi_jam"] = sparepartJam.value
                                                                            updateData["sparepart_estimasi_menit"] = sparepartMenit.value
                                                                        }

                                                                        firestore.collection("outstanding")
                                                                            .document(docId)
                                                                            .update(updateData)
                                                                            .addOnSuccessListener {
                                                                                tanggapanList.clear()
                                                                                tanggapanList.add("")
                                                                                isSubmitting.value =
                                                                                    false
                                                                                reloadTrigger.value =
                                                                                    !reloadTrigger.value
                                                                            }
                                                                            .addOnFailureListener {
                                                                                isSubmitting.value =
                                                                                    false
                                                                            }
                                                                    } else {
                                                                        isSubmitting.value = false
                                                                    }
                                                                }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF00695C)
                                                        )
                                                    ) {
                                                        Text("Kirim", color = Color.White)
                                                    }
                                                },
                                                dismissButton = {
                                                    OutlinedButton(onClick = {
                                                        showDialogBima.value = false
                                                    }) {
                                                        Text("Batal")
                                                    }
                                                },
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    }

                                    if (statusPerbaikan == "menunggu tanggapan teknik") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val isSubmitting = remember { mutableStateOf(false) }
                                        val showDialogKonfirmasi =
                                            remember { mutableStateOf(false) }
                                        val showDialogReject = remember { mutableStateOf(false) }

                                        OutlinedButton(
                                            onClick = { showImage.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF003366)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF003366)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lihat Foto")
                                        }

                                        if (showImage.value) {
                                            AlertDialog(
                                                onDismissRequest = { showImage.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showImage.value = false
                                                    }) {
                                                        Text("Tutup")
                                                    }
                                                },
                                                text = {
                                                    AsyncImage(
                                                        model = fotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Tombol Konfirmasi
                                        Button(
                                            onClick = { showDialogKonfirmasi.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF003366),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Konfirmasi")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Tombol Reject
                                        OutlinedButton(
                                            onClick = { showDialogReject.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color.Red
                                            ),
                                            border = BorderStroke(1.dp, Color.Red),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Reject")
                                        }

                                        fun getNextRejectIndex(data: Map<String, Any>): Int {
                                            var index = 1
                                            while (data.containsKey("keterangan_reject_$index")) {
                                                index++
                                            }
                                            return index
                                        }

                                        // Dialog Reject
                                        if (showDialogReject.value) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showDialogReject.value = false
                                                },
                                                title = { Text("Reject Perbaikan") },
                                                text = {
                                                    Column {
                                                        Text("Masukkan alasan reject:")
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        OutlinedTextField(
                                                            value = perbaikanKeterangan.value,
                                                            onValueChange = {
                                                                perbaikanKeterangan.value = it
                                                            },
                                                            placeholder = { Text("Keterangan reject...") },
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        isSubmitting.value = true
                                                        val firestore = FirebaseFirestore.getInstance()
                                                        val docRef = firestore.collection("outstanding")
                                                            .whereEqualTo("kode_alat", kodeAlat)
                                                            .whereEqualTo("tanggal", checklist["tanggal"])
                                                            .whereEqualTo("item", checklist["item"])
                                                            .limit(1)

                                                        docRef.get()
                                                            .addOnSuccessListener { result ->
                                                                if (!result.isEmpty) {
                                                                    val doc = result.documents[0]
                                                                    val docId = doc.id
                                                                    val data = doc.data ?: emptyMap()

                                                                    // cari index reject berikutnya
                                                                    var maxIndex = 0
                                                                    data.keys.forEach { key ->
                                                                        if (key.startsWith("keterangan_reject_")) {
                                                                            val idx = key.removePrefix("keterangan_reject_").toIntOrNull() ?: 0
                                                                            if (idx > maxIndex) maxIndex = idx
                                                                        }
                                                                    }
                                                                    val nextIndex = maxIndex + 1
                                                                    val nextFieldReject = "keterangan_reject_$nextIndex"
                                                                    val nextFieldRejectTimestamp = "keterangan_reject_timestamp_$nextIndex"

                                                                    firestore.collection("outstanding")
                                                                        .document(docId).update(
                                                                            mapOf(
                                                                                nextFieldReject to perbaikanKeterangan.value,
                                                                                nextFieldRejectTimestamp to FieldValue.serverTimestamp(), // ✅ timestamp per reject
                                                                                "status_perbaikan" to "menunggu tanggapan PT BIMA"
                                                                            )
                                                                        ).addOnSuccessListener {
                                                                            perbaikanKeterangan.value = ""
                                                                            showDialogReject.value = false
                                                                            isSubmitting.value = false
                                                                            reloadTrigger.value = !reloadTrigger.value
                                                                        }
                                                                } else {
                                                                    isSubmitting.value = false
                                                                }
                                                            }
                                                    }) {
                                                        Text("Submit")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = {
                                                        showDialogReject.value = false
                                                    }) {
                                                        Text("Batal")
                                                    }
                                                }
                                            )
                                        }

                                        // Fungsi helper untuk cari index berikutnya
                                        fun getNextInstruksiIndex(data: Map<String, Any>): Int {
                                            var maxIndex = 0
                                            data.keys.forEach { key ->
                                                if (key.startsWith("instruksi_teknik_")) {
                                                    val index =
                                                        key.removePrefix("instruksi_teknik_")
                                                            .toIntOrNull() ?: 0
                                                    if (index > maxIndex) {
                                                        maxIndex = index
                                                    }
                                                }
                                            }
                                            return maxIndex + 1
                                        }

                                        if (showDialogKonfirmasi.value) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showDialogKonfirmasi.value = false
                                                    perbaikanKeterangan.value = ""
                                                },
                                                title = { Text("Instruksi Perbaikan") },
                                                text = {
                                                    OutlinedTextField(
                                                        value = perbaikanKeterangan.value,
                                                        onValueChange = {
                                                            perbaikanKeterangan.value = it
                                                        },
                                                        placeholder = { Text("Instruksi perbaikan...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        isSubmitting.value = true
                                                        val firestore =
                                                            FirebaseFirestore.getInstance()
                                                        val docRef =
                                                            firestore.collection("outstanding")
                                                                .whereEqualTo("kode_alat", kodeAlat)
                                                                .whereEqualTo(
                                                                    "tanggal",
                                                                    checklist["tanggal"]
                                                                )
                                                                .whereEqualTo(
                                                                    "item",
                                                                    checklist["item"]
                                                                )
                                                                .limit(1)

                                                        docRef.get()
                                                            .addOnSuccessListener { result ->
                                                                if (!result.isEmpty) {
                                                                    val doc = result.documents[0]
                                                                    val docId = doc.id
                                                                    val data =
                                                                        doc.data ?: emptyMap()

                                                                    // cari index instruksi berikutnya
                                                                    val nextIndex = getNextInstruksiIndex(data)
                                                                    val nextFieldInstruksi = "instruksi_teknik_$nextIndex"

                                                                    val sparepartStatus =
                                                                        doc.getString("status_sparepart")
                                                                            ?: ""
                                                                    val statusUpdate =
                                                                        when (sparepartStatus) {
                                                                            "Indent" -> "menunggu pengadaan sparepart"
                                                                            "Tidak" -> "proses perbaikan alat oleh PT BIMA"
                                                                            else -> "proses perbaikan alat oleh PT BIMA"
                                                                        }

                                                                    val nextFieldTimestamp = "instruksi_teknik_timestamp_$nextIndex"

                                                                    firestore.collection("outstanding")
                                                                        .document(docId).update(
                                                                            mapOf(
                                                                                nextFieldInstruksi to perbaikanKeterangan.value,
                                                                                nextFieldTimestamp to FieldValue.serverTimestamp(), // ✅ timestamp per instruksi
                                                                                "status_perbaikan" to statusUpdate
                                                                            )
                                                                        ).addOnSuccessListener {
                                                                            perbaikanKeterangan.value =
                                                                                ""
                                                                            showDialogKonfirmasi.value =
                                                                                false
                                                                            isSubmitting.value =
                                                                                false
                                                                            reloadTrigger.value =
                                                                                !reloadTrigger.value
                                                                        }
                                                                }
                                                            }
                                                    }) {
                                                        Text("Submit")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = {
                                                        showDialogKonfirmasi.value = false
                                                    }) {
                                                        Text("Batal")
                                                    }
                                                }
                                            )
                                        }

                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }
                                    }

                                    if (statusPerbaikan == "menunggu pengadaan sparepart") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val isSubmitting = remember { mutableStateOf(false) }
                                        val showDialog = remember { mutableStateOf(false) }

                                        OutlinedButton(
                                            onClick = { showImage.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF003366)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF003366)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lihat Foto")
                                        }

                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }

                                        if (showImage.value) {
                                            AlertDialog(
                                                onDismissRequest = { showImage.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showImage.value = false
                                                    }) {
                                                        Text("Tutup")
                                                    }
                                                },
                                                text = {
                                                    AsyncImage(
                                                        model = fotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Tombol Sparepart Sudah Tersedia
                                        Button(
                                            onClick = { showDialog.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF003366),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Sparepart Sudah Tersedia")
                                        }

                                        // Dialog konfirmasi
                                        if (showDialog.value) {
                                            AlertDialog(
                                                onDismissRequest = { showDialog.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        isSubmitting.value = true
                                                        val firestore =
                                                            FirebaseFirestore.getInstance()
                                                        val docRef =
                                                            firestore.collection("outstanding")
                                                                .whereEqualTo("kode_alat", kodeAlat)
                                                                .whereEqualTo(
                                                                    "tanggal",
                                                                    checklist["tanggal"]
                                                                )
                                                                .whereEqualTo(
                                                                    "item",
                                                                    checklist["item"]
                                                                )
                                                                .limit(1)

                                                        docRef.get()
                                                            .addOnSuccessListener { result ->
                                                                if (!result.isEmpty) {
                                                                    val docId =
                                                                        result.documents[0].id
                                                                    firestore.collection("outstanding")
                                                                        .document(docId)
                                                                        .update(
                                                                            mapOf(
                                                                                "status_perbaikan" to "proses perbaikan alat oleh PT Bima",
                                                                                "sparepart_ready_timestamp" to FieldValue.serverTimestamp()
                                                                            )
                                                                        )
                                                                        .addOnSuccessListener {
                                                                            showDialog.value = false
                                                                            isSubmitting.value =
                                                                                false
                                                                            checklistList.clear()
                                                                            reloadTrigger.value =
                                                                                !reloadTrigger.value
                                                                        }
                                                                }
                                                            }
                                                    }) {
                                                        Text("Iya")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = {
                                                        showDialog.value = false
                                                    }) {
                                                        Text("Tidak")
                                                    }
                                                },
                                                title = { Text("Konfirmasi") },
                                                text = {
                                                    Text("Apakah sparepart sudah tersedia dan siap dilakukan perbaikan?")
                                                }
                                            )
                                        }
                                    }

                                    if (statusPerbaikan == "proses perbaikan alat oleh PT Bima") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val isSubmitting = remember { mutableStateOf(false) }
                                        OutlinedButton(
                                            onClick = { showImage.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF003366)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF003366)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lihat Foto")
                                        }

                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }

                                        if (showImage.value) {
                                            AlertDialog(
                                                onDismissRequest = { showImage.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showImage.value = false
                                                    }) {
                                                        Text("Tutup")
                                                    }
                                                },
                                                text = {
                                                    AsyncImage(
                                                        model = fotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val launcher = rememberLauncherForActivityResult(
                                            ActivityResultContracts.GetContent()
                                        ) { uri ->
                                            perbaikanFotoUri.value = uri
                                        }

                                        Button(
                                            onClick = { showPerbaikanDialog.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF003366),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Perbaikan Selesai")
                                        }

                                        if (showPerbaikanDialog.value) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showPerbaikanDialog.value = false
                                                    keteranganPerbaikan.value = ""
                                                    perbaikanFotoUri.value = null
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            isSubmitting.value = true
                                                            val uri = perbaikanFotoUri.value
                                                            if (uri == null) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Silakan pilih foto terlebih dahulu",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@Button
                                                            }

                                                            val bitmap =
                                                                MediaStore.Images.Media.getBitmap(
                                                                    context.contentResolver,
                                                                    uri
                                                                )

                                                            uploadImageToCloudinary(bitmap) { imageUrl ->
                                                                if (imageUrl != null) {
                                                                    val firestore =
                                                                        FirebaseFirestore.getInstance()
                                                                    val docRef =
                                                                        firestore.collection("outstanding")
                                                                            .whereEqualTo(
                                                                                "kode_alat",
                                                                                kodeAlat
                                                                            )
                                                                            .whereEqualTo(
                                                                                "tanggal",
                                                                                checklist["tanggal"]
                                                                            )
                                                                            .whereEqualTo(
                                                                                "item",
                                                                                checklist["item"]
                                                                            )
                                                                            .limit(1)

                                                                    docRef.get()
                                                                        .addOnSuccessListener { result ->
                                                                            if (!result.isEmpty) {
                                                                                val doc =
                                                                                    result.documents[0]
                                                                                val docId = doc.id
                                                                                val data = doc.data
                                                                                    ?: emptyMap()

                                                                                val index =
                                                                                    data.keys
                                                                                        .filter {
                                                                                            it.startsWith(
                                                                                                "keterangan_perbaikan_"
                                                                                            )
                                                                                        }
                                                                                        .mapNotNull {
                                                                                            it.removePrefix(
                                                                                                "keterangan_perbaikan_"
                                                                                            )
                                                                                                .toIntOrNull()
                                                                                        }
                                                                                        .maxOrNull()
                                                                                        ?.plus(1)
                                                                                        ?: 0

                                                                                val imgField =
                                                                                    "gambar_perbaikan_$index"
                                                                                val ketField =
                                                                                    "keterangan_perbaikan_$index"
                                                                                val timeField =
                                                                                    "${ketField}_timestamp"

                                                                                val updateData =
                                                                                    mapOf(
                                                                                        imgField to imageUrl,
                                                                                        ketField to keteranganPerbaikan.value,
                                                                                        timeField to FieldValue.serverTimestamp(),
                                                                                        "status_perbaikan" to "menunggu konfirmasi teknik"
                                                                                    )

                                                                                firestore.collection(
                                                                                    "outstanding"
                                                                                ).document(docId)
                                                                                    .update(
                                                                                        updateData
                                                                                    )
                                                                                    .addOnSuccessListener {
                                                                                        showPerbaikanDialog.value =
                                                                                            false
                                                                                        keteranganPerbaikan.value =
                                                                                            ""
                                                                                        perbaikanFotoUri.value =
                                                                                            null
                                                                                        checklistList.clear()
                                                                                        reloadTrigger.value =
                                                                                            !reloadTrigger.value
                                                                                    }
                                                                            }
                                                                        }
                                                                } else {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Upload gambar gagal",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF00695C)
                                                        )
                                                    ) {
                                                        Text("Kirim", color = Color.White)
                                                    }
                                                },
                                                dismissButton = {
                                                    OutlinedButton(onClick = {
                                                        showPerbaikanDialog.value = false
                                                        keteranganPerbaikan.value = ""
                                                        perbaikanFotoUri.value = null
                                                    }) {
                                                        Text("Batal")
                                                    }
                                                },
                                                title = {
                                                    Text(
                                                        "Tanggapan Perbaikan",
                                                        style = MaterialTheme.typography.titleLarge
                                                    )
                                                },
                                                text = {
                                                    Column {
                                                        Text("Masukkan keterangan dan unggah foto perbaikan:")
                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        OutlinedTextField(
                                                            value = keteranganPerbaikan.value,
                                                            onValueChange = {
                                                                keteranganPerbaikan.value = it
                                                            },
                                                            placeholder = { Text("Contoh: Sudah diganti dengan part baru...") },
                                                            modifier = Modifier.fillMaxWidth()
                                                        )

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        Button(
                                                            onClick = { launcher.launch("image/*") },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF005BBB)
                                                            )
                                                        ) {
                                                            Text("Pilih Foto", color = Color.White)
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        perbaikanFotoUri.value?.let {
                                                            AsyncImage(
                                                                model = it,
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(200.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (statusPerbaikan == "menunggu konfirmasi teknik") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val isSubmitting = remember { mutableStateOf(false) }
                                        OutlinedButton(
                                            onClick = { showImage.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF003366)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF003366)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lihat Foto Perbaikan")
                                        }
                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }
                                        if (showImage.value) {
                                            val data = checklist
                                            val maxIndex = data.keys
                                                .filter { it.startsWith("gambar_perbaikan_") }
                                                .mapNotNull {
                                                    it.removePrefix("gambar_perbaikan_")
                                                        .toIntOrNull()
                                                }
                                                .maxOrNull() ?: 0
                                            val lastFotoUrl =
                                                data["gambar_perbaikan_$maxIndex"] as? String ?: ""

                                            AlertDialog(
                                                onDismissRequest = { showImage.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showImage.value = false
                                                    }) {
                                                        Text("Tutup")
                                                    }
                                                },
                                                text = {
                                                    AsyncImage(
                                                        model = lastFotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Tombol Revisi
                                        Button(
                                            onClick = { showPerbaikanDialog.value = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD32F2F),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Revisi")
                                        }

                                        val launcher = rememberLauncherForActivityResult(
                                            ActivityResultContracts.GetContent()
                                        ) { uri ->
                                            perbaikanFotoUri.value = uri
                                        }

                                        if (showPerbaikanDialog.value) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showPerbaikanDialog.value = false
                                                    keteranganPerbaikan.value = ""
                                                    perbaikanFotoUri.value = null
                                                },
                                                confirmButton = {
                                                    Button(
                                                        onClick = {
                                                            isSubmitting.value = true
                                                            val uri = perbaikanFotoUri.value
                                                            if (uri == null) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Silakan pilih foto terlebih dahulu",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@Button
                                                            }

                                                            val bitmap =
                                                                MediaStore.Images.Media.getBitmap(
                                                                    context.contentResolver,
                                                                    uri
                                                                )

                                                            uploadImageToCloudinary(bitmap) { imageUrl ->
                                                                if (imageUrl != null) {
                                                                    val firestore =
                                                                        FirebaseFirestore.getInstance()
                                                                    val docRef =
                                                                        firestore.collection("outstanding")
                                                                            .whereEqualTo(
                                                                                "kode_alat",
                                                                                kodeAlat
                                                                            )
                                                                            .whereEqualTo(
                                                                                "tanggal",
                                                                                checklist["tanggal"]
                                                                            )
                                                                            .whereEqualTo(
                                                                                "item",
                                                                                checklist["item"]
                                                                            )
                                                                            .limit(1)

                                                                    docRef.get()
                                                                        .addOnSuccessListener { result ->
                                                                            if (!result.isEmpty) {
                                                                                val doc =
                                                                                    result.documents[0]
                                                                                val docId = doc.id
                                                                                val data = doc.data
                                                                                    ?: emptyMap()

                                                                                val index =
                                                                                    data.keys
                                                                                        .filter {
                                                                                            it.startsWith(
                                                                                                "keterangan_perbaikan_"
                                                                                            )
                                                                                        }
                                                                                        .mapNotNull {
                                                                                            it.removePrefix(
                                                                                                "keterangan_perbaikan_"
                                                                                            )
                                                                                                .toIntOrNull()
                                                                                        }
                                                                                        .maxOrNull()
                                                                                        ?.plus(1)
                                                                                        ?: 0

                                                                                val imgField =
                                                                                    "gambar_perbaikan_$index"
                                                                                val ketField =
                                                                                    "keterangan_perbaikan_$index"
                                                                                val timeField =
                                                                                    "revisi_operator_${index}_timestamp"

                                                                                val updateData =
                                                                                    mapOf(
                                                                                        imgField to imageUrl,
                                                                                        ketField to keteranganPerbaikan.value,
                                                                                        timeField to FieldValue.serverTimestamp(),
                                                                                        "status_perbaikan" to "perlu perbaikan ulang"
                                                                                    )

                                                                                firestore.collection(
                                                                                    "outstanding"
                                                                                ).document(docId)
                                                                                    .update(
                                                                                        updateData
                                                                                    )
                                                                                    .addOnSuccessListener {
                                                                                        showPerbaikanDialog.value =
                                                                                            false
                                                                                        keteranganPerbaikan.value =
                                                                                            ""
                                                                                        perbaikanFotoUri.value =
                                                                                            null
                                                                                        checklistList.clear()
                                                                                        reloadTrigger.value =
                                                                                            !reloadTrigger.value
                                                                                    }
                                                                            }
                                                                        }
                                                                } else {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Upload ke Cloudinary gagal",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF00695C)
                                                        )
                                                    ) {
                                                        Text("Kirim", color = Color.White)
                                                    }
                                                },
                                                dismissButton = {
                                                    OutlinedButton(onClick = {
                                                        showPerbaikanDialog.value = false
                                                        keteranganPerbaikan.value = ""
                                                        perbaikanFotoUri.value = null
                                                    }) {
                                                        Text("Batal")
                                                    }
                                                },
                                                title = {
                                                    Text(
                                                        "Revisi Perbaikan",
                                                        style = MaterialTheme.typography.titleLarge
                                                    )
                                                },
                                                text = {
                                                    Column {
                                                        Text("Masukkan keterangan dan unggah foto revisi:")
                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        OutlinedTextField(
                                                            value = keteranganPerbaikan.value,
                                                            onValueChange = {
                                                                keteranganPerbaikan.value = it
                                                            },
                                                            placeholder = { Text("Contoh: Part tidak sesuai, perlu diganti ulang...") },
                                                            modifier = Modifier.fillMaxWidth()
                                                        )

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        Button(
                                                            onClick = { launcher.launch("image/*") },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF005BBB)
                                                            )
                                                        ) {
                                                            Text("Pilih Foto", color = Color.White)
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        perbaikanFotoUri.value?.let {
                                                            AsyncImage(
                                                                model = it,
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(200.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                val firestore = FirebaseFirestore.getInstance()
                                                val docRef = firestore.collection("outstanding")
                                                    .whereEqualTo("kode_alat", kodeAlat)
                                                    .whereEqualTo("tanggal", checklist["tanggal"])
                                                    .whereEqualTo("item", checklist["item"])
                                                    .limit(1)

                                                docRef.get().addOnSuccessListener { result ->
                                                    if (!result.isEmpty) {
                                                        val docId = result.documents[0].id
                                                        val update = mapOf(
                                                            "status_perbaikan" to "menunggu konfirmasi operator",
                                                            "konfirmasi_teknik_timestamp" to FieldValue.serverTimestamp()
                                                        )
                                                        firestore.collection("outstanding")
                                                            .document(docId)
                                                            .update(update)
                                                            .addOnSuccessListener {
                                                                checklistList.clear()
                                                                reloadTrigger.value =
                                                                    !reloadTrigger.value
                                                            }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF43A047
                                                )
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Konfirmasi Perbaikan", color = Color.White)
                                        }
                                    }

                                    if (statusPerbaikan == "perlu perbaikan ulang") {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = { showImage.value = true },
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF003366)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF003366)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lihat Foto")
                                        }

                                        if (showImage.value) {
                                            AlertDialog(
                                                onDismissRequest = { showImage.value = false },
                                                confirmButton = {
                                                    TextButton(onClick = { showImage.value = false }) {
                                                        Text("Tutup")
                                                    }
                                                },
                                                text = {
                                                    AsyncImage(
                                                        model = fotoUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = { showDialog.value = true },
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF00695C)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF00695C)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Beri Tanggapan")
                                        }

                                        val isSubmitting = remember { mutableStateOf(false) }
                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                        }

                                        if (showDialog.value) {
                                            AlertDialog(
                                                onDismissRequest = { showDialog.value = false },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            showDialog.value = false
                                                            isSubmitting.value = true

                                                            val firestore = FirebaseFirestore.getInstance()
                                                            val docRef = firestore.collection("outstanding")
                                                                .whereEqualTo("kode_alat", kodeAlat)
                                                                .whereEqualTo("tanggal", checklist["tanggal"])
                                                                .whereEqualTo("item", checklist["item"])
                                                                .limit(1)

                                                            docRef.get().addOnSuccessListener { result ->
                                                                if (!result.isEmpty) {
                                                                    val doc = result.documents[0]
                                                                    val docId = doc.id
                                                                    fun updateFirestore(imageUrl: String?) {
                                                                        val index = getNextPerbaikanRevisionIndex(doc.data ?: emptyMap())
                                                                        val nextFieldImage = "gambar_perbaikan_$index"
                                                                        val nextFieldText = "keterangan_perbaikan_$index"

                                                                        val updateData = mutableMapOf<String, Any>(
                                                                            nextFieldText to perbaikanKeterangan.value,
                                                                            "status_perbaikan" to "menunggu konfirmasi operator"
                                                                        )
                                                                        imageUrl?.let {
                                                                            updateData[nextFieldImage] = it
                                                                        }

                                                                        Log.d("UpdateFirestore", "Data yang dikirim: $updateData")

                                                                        firestore.collection("outstanding").document(docId).update(updateData)
                                                                            .addOnSuccessListener {
                                                                                Log.d("UpdateFirestore", "Dokumen berhasil diupdate.")
                                                                                perbaikanKeterangan.value = ""
                                                                                perbaikanFotoUri.value = null
                                                                                checklistList.clear()
                                                                                reloadTrigger.value = !reloadTrigger.value
                                                                            }
                                                                            .addOnFailureListener {
                                                                                Log.e("UpdateFirestore", "Gagal update dokumen: ${it.message}")
                                                                            }

                                                                        val json = JSONObject().apply {
                                                                            put("kode_alat", kodeAlat)
                                                                            put("tanggal", checklist["tanggal"])
                                                                            put("item", checklist["item"])
                                                                            put("keterangan_perbaikan_0", perbaikanKeterangan.value)
                                                                            put("gambar_perbaikan_0", imageUrl ?: "")
                                                                            put("operator_email", doc.getString("operator_email") ?: "")
                                                                        }

                                                                        val client = OkHttpClient()
                                                                        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                                                                        val request = Request.Builder()
                                                                            .url("https://script.google.com/macros/s/AKfycbw44hR6NoP7QqBg869xtNEn1ZUpjJ2phsNkKPIJdSrDCuXmuIhw6B85LyBtuX5RLmrV/exec")
                                                                            .post(requestBody)
                                                                            .build()

                                                                        client.newCall(request).enqueue(object : Callback {
                                                                            override fun onFailure(call: Call, e: IOException) {
                                                                                Log.e("EmailNotif", "Gagal kirim email", e)
                                                                                isSubmitting.value = false
                                                                            }

                                                                            override fun onResponse(call: Call, response: Response) {
                                                                                Log.d("EmailNotif", "Email berhasil dikirim: ${response.body?.string()}")
                                                                                isSubmitting.value = false
                                                                            }
                                                                        })
                                                                    }

                                                                    val uri = perbaikanFotoUri.value
                                                                    if (uri != null) {
                                                                        val bitmap = uriToBitmap(this@OutstandingActivity, uri)
                                                                        if (bitmap != null) {
                                                                            uploadImageToCloudinary(bitmap) { imageUrl ->
                                                                                if (imageUrl != null) {
                                                                                    updateFirestore(imageUrl)
                                                                                } else {
                                                                                    Log.e("CloudinaryUpload", "Upload gagal, URL null")
                                                                                    updateFirestore(null)
                                                                                }
                                                                            }
                                                                        } else {
                                                                            Log.e("CloudinaryUpload", "Gagal konversi URI ke Bitmap")
                                                                            updateFirestore(null)
                                                                        }
                                                                    } else {
                                                                        updateFirestore(null)
                                                                    }
                                                                } else {
                                                                    isSubmitting.value = false
                                                                }
                                                            }
                                                        }

                                                    ) {
                                                        Text("Submit")
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = {
                                                        showDialog.value = false
                                                        perbaikanKeterangan.value = ""
                                                        perbaikanFotoUri.value = null
                                                    }) {
                                                        Text("Batal")
                                                    }
                                                },
                                                text = {
                                                    Column {
                                                        Text("Upload foto perbaikan dan isi keterangan:")

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(150.dp)
                                                                .clickable { imageLauncher.launch("image/*") },
                                                            shape = RoundedCornerShape(12.dp),
                                                            border = BorderStroke(1.dp, Color.Gray),
                                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                if (perbaikanFotoUri.value != null) {
                                                                    AsyncImage(
                                                                        model = perbaikanFotoUri.value,
                                                                        contentDescription = "Foto terpilih",
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize()
                                                                    )
                                                                } else {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.CameraAlt,
                                                                            contentDescription = "Upload Foto",
                                                                            tint = Color.Gray,
                                                                            modifier = Modifier.size(40.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.height(8.dp))
                                                                        Text("Klik untuk pilih foto", color = Color.Gray)
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        androidx.compose.material3.OutlinedTextField(
                                                            value = perbaikanKeterangan.value,
                                                            onValueChange = { perbaikanKeterangan.value = it },
                                                            placeholder = { Text("Masukkan keterangan perbaikan...") },
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            )
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

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uploadImageToCloudinary(bitmap: Bitmap, onResult: (String?) -> Unit) {

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "image.jpg", RequestBody.create("image/*".toMediaTypeOrNull(), imageData))
            .addFormDataPart("upload_preset", "fotoalat") // Pastikan ini sesuai dengan Cloudinary Anda
            .build()

        val request = Request.Builder()
            .url(cloudinaryUrl) // Pastikan cloudinaryUrl benar
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CloudinaryUpload", "Gagal mengunggah gambar: ${e.message}", e)
                e.printStackTrace()
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    Log.e("CloudinaryUpload", "Upload gagal. Response: $responseBody")
                    onResult(null)
                    return
                }

                try {
                    val jsonObject = JSONObject(responseBody)
                    val imageUrl = jsonObject.optString("secure_url", "")

                    if (imageUrl.isNotEmpty()) {
                        Log.d("CloudinaryUpload", "Upload berhasil! URL: $imageUrl")
                        onResult(imageUrl)
                    } else {
                        Log.e("CloudinaryUpload", "Response tidak mengandung secure_url: $responseBody")
                        onResult(null)
                    }
                } catch (e: JSONException) {
                    Log.e("CloudinaryUpload", "Error parsing JSON: ${e.message}", e)
                    onResult(null)
                }
            }
        })
    }

    fun getLatestKeteranganField(document: Map<String, Any?>): String? {
        val status = document["status_perbaikan"]?.toString()?.lowercase() ?: return null
        Log.d("KeteranganDebug", "Status Perbaikan: $status")

        return when {
            status == "menunggu tanggapan pt bima" -> {
                val instruksiFields = document.keys
                    .filter { it.startsWith("keterangan_reject_") }
                    .mapNotNull { key ->
                        val index = key.removePrefix("keterangan_reject_").toIntOrNull()
                        index?.let { idx -> idx to key }
                    }
                    .sortedByDescending { it.first }

                val latestKey = instruksiFields.firstOrNull()?.second
                val value = if (latestKey != null) {
                    document[latestKey]?.toString()
                } else {
                    document["keterangan"]?.toString()
                }
                Log.d("KeteranganDebug", "Mengambil dari '${latestKey ?: "keterangan"}': $value")
                value
            }

            status == "menunggu tanggapan teknik" -> {
                val tanggapanFields = document.keys
                    .filter { it.startsWith("tanggapan_bima_") && !it.contains("timestamp") }
                    .mapNotNull { key ->
                        val index = key.removePrefix("tanggapan_bima_").toIntOrNull()
                        index?.let { idx -> idx to key }
                    }
                    .sortedByDescending { it.first }

                val latestKey = tanggapanFields.firstOrNull()?.second
                val value = latestKey?.let { document[it]?.toString() }
                Log.d("KeteranganDebug", "Mengambil dari '$latestKey': $value")
                value
            }

            status == "menunggu pengadaan sparepart" || status == "proses perbaikan alat oleh pt bima" -> {
                val instruksiFields = document.keys
                    .filter { it.startsWith("instruksi_teknik_") }
                    .mapNotNull { key ->
                        val index = key.removePrefix("instruksi_teknik_").toIntOrNull()
                        index?.let { idx -> idx to key }
                    }
                    .sortedByDescending { it.first }

                val latestKey = instruksiFields.firstOrNull()?.second ?: "instruksi_teknik"
                val value = document[latestKey]?.toString()
                Log.d("KeteranganDebug", "Mengambil dari '$latestKey': $value")
                value
            }

            status == "menunggu konfirmasi teknik" || status == "menunggu konfirmasi operator" || status == "perlu perbaikan ulang" -> {
                val keteranganFields = document.keys
                    .filter { it.startsWith("keterangan_perbaikan_") }
                    .mapNotNull { key ->
                        val index = key.removePrefix("keterangan_perbaikan_").toIntOrNull()
                        index?.let { idx -> idx to key }
                    }
                    .sortedByDescending { it.first }

                val latestKey = keteranganFields.firstOrNull()?.second
                val value = latestKey?.let { document[it]?.toString() }
                Log.d("KeteranganDebug", "Mengambil dari '$latestKey': $value")
                value
            }

            else -> {
                Log.d("KeteranganDebug", "Status tidak dikenali: $status")
                null
            }
        }
    }

    fun getLatestImageField(document: Map<String, Any>): String? {
        val statusPerbaikan = document["status_perbaikan"]?.toString() ?: ""
        Log.d("ImageDebug", "Status perbaikan: $statusPerbaikan")

        val allKeys = document.keys
        Log.d("ImageDebug", "Semua key di dokumen: $allKeys")

        val imageFields = allKeys
            .filter { it.startsWith("gambar_perbaikan_") }
            .mapNotNull {
                val indexStr = it.substringAfterLast("_")
                val index = indexStr.toIntOrNull()
                if (index != null) {
                    Log.d("ImageDebug", "✔️ Field valid: $it -> index $index")
                    Pair(index, it)
                } else {
                    Log.d("ImageDebug", "❌ Gagal parsing index dari: $it")
                    null
                }
            }
            .sortedBy { it.first }

        val latestField = imageFields.lastOrNull()?.second
        val latestImageUrl = latestField?.let { document[it] as? String }
        val fallback = document["gambar"] as? String

        Log.d("ImageDebug", "Field gambar_perbaikan ditemukan: $imageFields")
        Log.d("ImageDebug", "Field terbaru: $latestField, URL: $latestImageUrl")
        Log.d("ImageDebug", "Fallback 'gambar': $fallback")

        return latestImageUrl ?: fallback
    }

    fun getNextPerbaikanRevisionIndex(document: Map<String, Any>): Int {
        val fields = document.keys
        return fields.count { it.startsWith("gambar_perbaikan_") }
    }
}