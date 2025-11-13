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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
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
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


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
        val checklistList = remember { mutableStateListOf<Map<String, Any>>() }
        val alatMap = remember { mutableStateMapOf<String, Map<String, Any>>() }
        val darkBlue = Color(0xFF003366)
        val showDialogBima = remember { mutableStateOf(false) }
        val showDialog = remember { mutableStateOf(false) }
        val showPerbaikanDialog = remember { mutableStateOf(false) }
        val keteranganPerbaikan = remember { mutableStateOf("") }
        val perbaikanKeterangan = remember { mutableStateListOf("") }
        val rejectKeterangan = remember { mutableStateListOf("") }
        val konfirmasiKeterangan = remember { mutableStateListOf("") }
        val perbaikanFotoUri = remember { mutableStateOf<Uri?>(null) }
        val context = LocalContext.current
        val reloadTrigger = remember { mutableStateOf(false) }
        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            perbaikanFotoUri.value = uri
        }
        // URL Web App GAS (hasil deploy 2 file terpisah)
        val urlReject = "https://script.google.com/macros/s/AKfycbx2fjLbWq7OscgpR9vWuhRGwyyUU-YUYG-N_Lrbp13bJ-45D574feGQBQZPvYC5asnUhQ/exec"
        val urlKonfirmasi = "https://script.google.com/macros/s/AKfycby-5zu0Nbvnb8-aJAVYxVkj_zyZjcNTkilPdhNf7r3WeTLF-DYYAeX6ZQVLn5uFsZAp/exec"

        val client = OkHttpClient()

        val jabatanUser = remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("uid", currentUser.uid)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents[0]
                            jabatanUser.value = document.getString("jabatan") ?: ""
                        }
                    }
            }
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

                            "sparepart_estimasi_hari" to (data["sparepart_estimasi_hari"] ?: 0),
                            "sparepart_estimasi_jam" to (data["sparepart_estimasi_jam"] ?: 0),
                            "sparepart_estimasi_menit" to (data["sparepart_estimasi_menit"] ?: 0)
                        )

                        for ((key, value) in data) {
                            if (
                                key.startsWith("estimasi_hari_") ||
                                key.startsWith("estimasi_jam_") ||
                                key.startsWith("estimasi_menit_") ||
                                key.startsWith("sparepart_estimasi_hari_") ||
                                key.startsWith("sparepart_estimasi_jam_") ||
                                key.startsWith("sparepart_estimasi_menit_") ||
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
                .background(Color(0xFF003366))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ===== HEADER CUSTOM =====
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
                                            color = Color(0xFF003366),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${checklist["tanggal"]} | ${checklist["shift"]}",
                                        color = Color(0xFF003366),
                                        fontSize = 13.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Status: ${checklist["status_perbaikan"]}",
                                        color = Color(0xFF003366),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Item: ${
                                            checklist["item"]
                                                ?.toString()
                                                ?.removeParenthesesText()
                                                ?.replace("_", " ")
                                                ?.trim()
                                        }",
                                        color = Color(0xFF003366),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Kondisi: ${checklist["kondisi"]}",
                                        color = Color(0xFF003366),
                                        fontSize = 14.sp
                                    )
//                                    val latestKeterangan = getLatestKeteranganField(checklist)
//                                    if (!latestKeterangan.isNullOrBlank()) {
//                                        Text(
//                                            text = "Keterangan: $latestKeterangan",
//                                            color = Color(0xFF003366),
//                                            fontSize = 14.sp
//                                        )
//                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(context, DetailOutstandingActivity::class.java).apply {
                                                putExtra("kode_alat", kodeAlat)
                                                putExtra("item", checklist["item"]?.toString() ?: "")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF003366)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF003366)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Detail Laporan",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (statusPerbaikan == "menunggu tanggapan PT BIMA") {
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

                                        if (jabatanUser.value == "PT BIMA") {

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
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
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
                                                                        tanggapanList[index] =
                                                                            newValue
                                                                    },
                                                                    placeholder = { Text("Masukkan tanggapan...") },
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                            }

                                                            // Tombol tambah input baru
                                                            TextButton(onClick = {
                                                                tanggapanList.add(
                                                                    ""
                                                                )
                                                            }) {
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
                                                                        modifier = Modifier.weight(
                                                                            1f
                                                                        )
                                                                    )
                                                                    Spacer(
                                                                        modifier = Modifier.width(
                                                                            8.dp
                                                                        )
                                                                    )
                                                                    OutlinedTextField(
                                                                        value = sparepartJam.value,
                                                                        onValueChange = {
                                                                            sparepartJam.value = it
                                                                        },
                                                                        label = { Text("Jam") },
                                                                        modifier = Modifier.weight(
                                                                            1f
                                                                        )
                                                                    )
                                                                    Spacer(
                                                                        modifier = Modifier.width(
                                                                            8.dp
                                                                        )
                                                                    )
                                                                    OutlinedTextField(
                                                                        value = sparepartMenit.value,
                                                                        onValueChange = {
                                                                            sparepartMenit.value =
                                                                                it
                                                                        },
                                                                        label = { Text("Menit") },
                                                                        modifier = Modifier.weight(
                                                                            1f
                                                                        )
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
                                                                            val existingKeys =
                                                                                doc.data?.keys?.filter {
                                                                                    it.startsWith("tanggapan_bima_") && !it.contains(
                                                                                        "timestamp"
                                                                                    )
                                                                                } ?: emptyList()

                                                                            val lastIndex =
                                                                                existingKeys.mapNotNull {
                                                                                    it.removePrefix(
                                                                                        "tanggapan_bima_"
                                                                                    ).toIntOrNull()
                                                                                }.maxOrNull() ?: 0

                                                                            val nextIndex =
                                                                                lastIndex + 1
                                                                            val fieldName =
                                                                                "tanggapan_bima_$nextIndex"
                                                                            val tsFieldName =
                                                                                "tanggapan_bima_timestamp_$nextIndex"

                                                                            val updateData =
                                                                                mutableMapOf<String, Any>(
                                                                                    fieldName to tanggapanList.filter { it.isNotBlank() },
                                                                                    tsFieldName to FieldValue.serverTimestamp(),
                                                                                    "estimasi_hari_$nextIndex" to estimasiHari.value,
                                                                                    "estimasi_jam_$nextIndex" to estimasiJam.value,
                                                                                    "estimasi_menit_$nextIndex" to estimasiMenit.value,
                                                                                    "status_sparepart_$nextIndex" to selectedSparepartStatus.value,
                                                                                    "status_perbaikan" to "menunggu tanggapan teknik",
                                                                                    "status" to "READY FOR USE"
                                                                                )

                                                                            if (selectedSparepartStatus.value == "Indent") {
                                                                                updateData["sparepart_estimasi_hari_$nextIndex"] =
                                                                                    sparepartHari.value
                                                                                updateData["sparepart_estimasi_jam_$nextIndex"] =
                                                                                    sparepartJam.value
                                                                                updateData["sparepart_estimasi_menit_$nextIndex"] =
                                                                                    sparepartMenit.value
                                                                            }

                                                                            firestore.collection("outstanding")
                                                                                .document(docId)
                                                                                .update(updateData)
                                                                                .addOnSuccessListener {
                                                                                    tanggapanList.clear()
                                                                                    tanggapanList.add(
                                                                                        ""
                                                                                    )
                                                                                    isSubmitting.value =
                                                                                        false
                                                                                    reloadTrigger.value =
                                                                                        !reloadTrigger.value
                                                                                }
                                                                                .addOnFailureListener {
                                                                                    isSubmitting.value =
                                                                                        false
                                                                                }

                                                                            val url =
                                                                                "https://script.google.com/macros/s/AKfycbw44hR6NoP7QqBg869xtNEn1ZUpjJ2phsNkKPIJdSrDCuXmuIhw6B85LyBtuX5RLmrV/exec"

                                                                            val data = mapOf(
                                                                                "kode_alat" to kodeAlat,
                                                                                "tanggal" to (checklist["tanggal"]
                                                                                    ?: ""),
                                                                                "item" to (checklist["item"]
                                                                                    ?: ""),
                                                                                "tanggapan" to tanggapanList,
                                                                                "estimasi_hari" to estimasiHari.value,
                                                                                "estimasi_jam" to estimasiJam.value,
                                                                                "estimasi_menit" to estimasiMenit.value,
                                                                                "status_sparepart" to selectedSparepartStatus.value
                                                                            )

                                                                            val jsonData =
                                                                                JSONObject(data).toString()

                                                                            val client =
                                                                                OkHttpClient()
                                                                            val mediaType =
                                                                                "application/json; charset=utf-8".toMediaTypeOrNull()
                                                                            val body =
                                                                                jsonData.toRequestBody(
                                                                                    mediaType
                                                                                )

                                                                            val request =
                                                                                Request.Builder()
                                                                                    .url(url)
                                                                                    .post(body)
                                                                                    .build()

                                                                            CoroutineScope(
                                                                                Dispatchers.IO
                                                                            ).launch {
                                                                                try {
                                                                                    client.newCall(
                                                                                        request
                                                                                    ).execute()
                                                                                        .use { response ->
                                                                                            if (!response.isSuccessful) {
                                                                                                Log.e(
                                                                                                    "GAS",
                                                                                                    "Error: ${response.code} - ${response.message}"
                                                                                                )
                                                                                            } else {
                                                                                                Log.d(
                                                                                                    "GAS",
                                                                                                    "Email sent: ${response.body?.string()}"
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                } catch (e: Exception) {
                                                                                    Log.e(
                                                                                        "GAS",
                                                                                        "Exception: ${e.message}"
                                                                                    )
                                                                                }
                                                                            }

                                                                        } else {
                                                                            isSubmitting.value =
                                                                                false
                                                                        }
                                                                    }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF003366), // ✅ Dark Blue
                                                                contentColor = Color.White
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
                                                    shape = RoundedCornerShape(0.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (statusPerbaikan == "menunggu tanggapan teknik") {
                                        val isSubmitting = remember { mutableStateOf(false) }
                                        val showDialogKonfirmasi =
                                            remember { mutableStateOf(false) }
                                        val showDialogReject = remember { mutableStateOf(false) }

//                                        OutlinedButton(
//                                            onClick = { showImage.value = true },
//                                            shape = RoundedCornerShape(10.dp),
//                                            colors = ButtonDefaults.outlinedButtonColors(
//                                                containerColor = Color.White,
//                                                contentColor = Color(0xFF003366)
//                                            ),
//                                            border = BorderStroke(1.dp, Color(0xFF003366)),
//                                            modifier = Modifier.fillMaxWidth()
//                                        ) {
//                                            Text("Lihat Foto")
//                                        }

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

                                        // Tombol Reject
                                        if (jabatanUser.value == "teknik") {

                                            // Tombol Reject
                                            Button(
                                                onClick = { showDialogReject.value = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.Red,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Reject")
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Tombol Konfirmasi Perbaikan (contoh)
                                            Button(
                                                onClick = { showDialogKonfirmasi.value = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF003366),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Konfirmasi Perbaikan")
                                            }
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
                                                    rejectKeterangan.clear()
                                                    rejectKeterangan.add("") // reset ke satu input kosong
                                                },
                                                title = { Text("Reject Perbaikan") },
                                                text = {
                                                    Column {
                                                        Text("Masukkan alasan reject:")
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // loop semua field
                                                        rejectKeterangan.forEachIndexed { index, value ->
                                                            OutlinedTextField(
                                                                value = value,
                                                                onValueChange = { newValue ->
                                                                    rejectKeterangan[index] = newValue
                                                                },
                                                                placeholder = { Text("Keterangan reject...") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                        }

                                                        // tombol tambah alasan baru
                                                        TextButton(onClick = { rejectKeterangan.add("") }) {
                                                            Text("+ Tambah Alasan Reject")
                                                        }
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
                                                                                // simpan langsung list (bukan string tunggal)
                                                                                nextFieldReject to rejectKeterangan.filter { it.isNotBlank() },
                                                                                nextFieldRejectTimestamp to FieldValue.serverTimestamp(),
                                                                                "status_perbaikan" to "menunggu tanggapan PT BIMA"
                                                                            )
                                                                        ).addOnSuccessListener {
                                                                            rejectKeterangan.clear()
                                                                            rejectKeterangan.add("")
                                                                            showDialogReject.value = false
                                                                            isSubmitting.value = false
                                                                            reloadTrigger.value = !reloadTrigger.value
                                                                            // Kirim notifikasi Reject via GAS
                                                                            val jsonReject = JSONObject(
                                                                                mapOf(
                                                                                    "kode_alat" to kodeAlat,
                                                                                    "tanggal" to (checklist["tanggal"] ?: ""),
                                                                                    "item" to (checklist["item"] ?: ""),
                                                                                    "alasan" to rejectKeterangan.filter { it.isNotBlank() }.joinToString("; ")
                                                                                )
                                                                            ).toString()

                                                                            val bodyReject = jsonReject.toRequestBody("application/json; charset=utf-8".toMediaType())
                                                                            val requestReject = Request.Builder()
                                                                                .url(urlReject)
                                                                                .post(bodyReject)
                                                                                .build()

                                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                                try {
                                                                                    client.newCall(requestReject).execute().use { resp ->
                                                                                        Log.d("GAS", "Reject notification sent: ${resp.body?.string()}")
                                                                                    }
                                                                                } catch (e: Exception) {
                                                                                    Log.e("GAS", "Reject notify error: ${e.message}")
                                                                                }
                                                                            }
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

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val selectedStatusAlat = remember { mutableStateOf("Break Down") }

                                        if (showDialogKonfirmasi.value) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    showDialogKonfirmasi.value = false
                                                    konfirmasiKeterangan.clear()
                                                    konfirmasiKeterangan.add("") // reset ke 1 input kosong
                                                },
                                                title = { Text("Instruksi Perbaikan") },
                                                text = {
                                                    Column {
                                                        // Loop input instruksi
                                                        konfirmasiKeterangan.forEachIndexed { index, value ->
                                                            OutlinedTextField(
                                                                value = value,
                                                                onValueChange = { newValue ->
                                                                    konfirmasiKeterangan[index] = newValue
                                                                },
                                                                placeholder = { Text("Instruksi perbaikan...") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                        }

                                                        // Tombol tambah instruksi baru
                                                        TextButton(onClick = { konfirmasiKeterangan.add("") }) {
                                                            Text("+ Tambah Instruksi")
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        Text("Status Alat:", fontWeight = FontWeight.Bold)

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            RadioButton(
                                                                selected = selectedStatusAlat.value == "Break Down",
                                                                onClick = { selectedStatusAlat.value = "Break Down" }
                                                            )
                                                            Text("Break Down")
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            RadioButton(
                                                                selected = selectedStatusAlat.value == "Perbaikan Ringan",
                                                                onClick = { selectedStatusAlat.value = "Perbaikan Ringan" }
                                                            )
                                                            Text("Perbaikan Ringan")
                                                        }
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

                                                                    val nextIndex = getNextInstruksiIndex(data)
                                                                    val nextFieldInstruksi = "instruksi_teknik_$nextIndex"
                                                                    val nextFieldTimestamp = "instruksi_teknik_timestamp_$nextIndex"

                                                                    // 🔎 cari status_sparepart index terakhir
                                                                    val maxSpareIndex = data.keys
                                                                        .filter { it.startsWith("status_sparepart_") }
                                                                        .mapNotNull { it.removePrefix("status_sparepart_").toIntOrNull() }
                                                                        .maxOrNull() ?: 0

                                                                    val lastSpareStatus = data["status_sparepart_$maxSpareIndex"] as? String ?: "Tidak"

                                                                    // 🛠 tentukan status_perbaikan sesuai lastSpareStatus
                                                                    val statusPerbaikan = if (lastSpareStatus == "Indent") {
                                                                        "menunggu pengadaan sparepart"
                                                                    } else {
                                                                        "proses perbaikan alat oleh PT BIMA"
                                                                    }

                                                                    val statusAlat = if (selectedStatusAlat.value == "Break Down") {
                                                                        "BREAK DOWN"
                                                                    } else {
                                                                        "READY FOR USE"
                                                                    }

                                                                    firestore.collection("outstanding")
                                                                        .document(docId)
                                                                        .update(
                                                                            mapOf(
                                                                                nextFieldInstruksi to konfirmasiKeterangan.filter { it.isNotBlank() },
                                                                                nextFieldTimestamp to FieldValue.serverTimestamp(),
                                                                                "status_perbaikan" to statusPerbaikan,
                                                                                "status" to statusAlat
                                                                            )
                                                                        )
                                                                        .addOnSuccessListener {
                                                                            // update collection alat juga
                                                                            firestore.collection("alat")
                                                                                .whereEqualTo("kode_alat", kodeAlat)
                                                                                .limit(1)
                                                                                .get()
                                                                                .addOnSuccessListener { alatResult ->
                                                                                    if (!alatResult.isEmpty) {
                                                                                        val alatDoc = alatResult.documents[0]
                                                                                        firestore.collection("alat")
                                                                                            .document(alatDoc.id)
                                                                                            .update("status", statusAlat)
                                                                                            .addOnSuccessListener {
                                                                                                konfirmasiKeterangan.clear()
                                                                                                konfirmasiKeterangan.add("")
                                                                                                showDialogKonfirmasi.value = false
                                                                                                isSubmitting.value = false
                                                                                                reloadTrigger.value = !reloadTrigger.value
                                                                                                // Kirim notifikasi Konfirmasi via GAS
                                                                                                val jsonKonfirmasi = JSONObject(
                                                                                                    mapOf(
                                                                                                        "kode_alat" to kodeAlat,
                                                                                                        "tanggal" to (checklist["tanggal"] ?: ""),
                                                                                                        "item" to (checklist["item"] ?: ""),
                                                                                                        "instruksi" to konfirmasiKeterangan.filter { it.isNotBlank() }.joinToString("; "),
                                                                                                        "status_alat" to statusAlat,
                                                                                                        "status_perbaikan" to statusPerbaikan
                                                                                                    )
                                                                                                ).toString()

                                                                                                val bodyKonfirmasi = jsonKonfirmasi.toRequestBody("application/json; charset=utf-8".toMediaType())
                                                                                                val requestKonfirmasi = Request.Builder()
                                                                                                    .url(urlKonfirmasi)
                                                                                                    .post(bodyKonfirmasi)
                                                                                                    .build()

                                                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                                                    try {
                                                                                                        client.newCall(requestKonfirmasi).execute().use { resp ->
                                                                                                            Log.d("GAS", "Konfirmasi notification sent: ${resp.body?.string()}")
                                                                                                        }
                                                                                                    } catch (e: Exception) {
                                                                                                        Log.e("GAS", "Konfirmasi notify error: ${e.message}")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                    } else {
                                                                                        isSubmitting.value = false
                                                                                    }
                                                                                }
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

//                                        OutlinedButton(
//                                            onClick = { showImage.value = true },
//                                            shape = RoundedCornerShape(10.dp),
//                                            colors = ButtonDefaults.outlinedButtonColors(
//                                                containerColor = Color.White,
//                                                contentColor = Color(0xFF003366)
//                                            ),
//                                            border = BorderStroke(1.dp, Color(0xFF003366)),
//                                            modifier = Modifier.fillMaxWidth()
//                                        ) {
//                                            Text("Lihat Foto")
//                                        }

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

                                        if (jabatanUser.value == "PT BIMA") {
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
                                                            showDialog.value = false
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
                                                                        val docId =
                                                                            result.documents[0].id
                                                                        firestore.collection("outstanding")
                                                                            .document(docId)
                                                                            .update(
                                                                                mapOf(
                                                                                    "status_perbaikan" to "proses perbaikan alat oleh PT BIMA",
                                                                                    "sparepart_ready_timestamp" to FieldValue.serverTimestamp()
                                                                                )
                                                                            )
                                                                            .addOnSuccessListener {
                                                                                isSubmitting.value =
                                                                                    false
                                                                                checklistList.clear()
                                                                                reloadTrigger.value =
                                                                                    !reloadTrigger.value

                                                                                val client =
                                                                                    OkHttpClient()
                                                                                val url =
                                                                                    "https://script.google.com/macros/s/AKfycbxknth6uQ7ICLgeBZaCHjTSkHJotvSGEkGe5OT60RXeCqbsG1cSICqwy25ICe8hQxVKtA/exec" // ganti URL Web App kamu

                                                                                val json =
                                                                                    JSONObject().apply {
                                                                                        put(
                                                                                            "kode_alat",
                                                                                            kodeAlat
                                                                                        )
                                                                                        put(
                                                                                            "item",
                                                                                            checklist["item"].toString()
                                                                                        )
                                                                                        put(
                                                                                            "tanggal",
                                                                                            checklist["tanggal"].toString()
                                                                                        )
                                                                                    }

                                                                                val body =
                                                                                    RequestBody.create(
                                                                                        "application/json; charset=utf-8".toMediaTypeOrNull(),
                                                                                        json.toString()
                                                                                    )

                                                                                val request =
                                                                                    Request.Builder()
                                                                                        .url(url)
                                                                                        .post(body)
                                                                                        .build()

                                                                                client.newCall(
                                                                                    request
                                                                                ).enqueue(object :
                                                                                    Callback {
                                                                                    override fun onFailure(
                                                                                        call: Call,
                                                                                        e: IOException
                                                                                    ) {
                                                                                        Log.e(
                                                                                            "GAS",
                                                                                            "Gagal kirim notifikasi: ${e.message}"
                                                                                        )
                                                                                    }

                                                                                    override fun onResponse(
                                                                                        call: Call,
                                                                                        response: Response
                                                                                    ) {
                                                                                        response.use {
                                                                                            if (!response.isSuccessful) {
                                                                                                Log.e(
                                                                                                    "GAS",
                                                                                                    "Response gagal: ${response.code}"
                                                                                                )
                                                                                            } else {
                                                                                                Log.d(
                                                                                                    "GAS",
                                                                                                    "Notifikasi sukses: ${response.body?.string()}"
                                                                                                )
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                })
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
                                    }

                                    if (statusPerbaikan == "proses perbaikan alat oleh PT BIMA") {

                                        val isSubmitting = remember { mutableStateOf(false) }
//                                        OutlinedButton(
//                                            onClick = { showImage.value = true },
//                                            shape = RoundedCornerShape(10.dp),
//                                            colors = ButtonDefaults.outlinedButtonColors(
//                                                containerColor = Color.White,
//                                                contentColor = Color(0xFF003366)
//                                            ),
//                                            border = BorderStroke(1.dp, Color(0xFF003366)),
//                                            modifier = Modifier.fillMaxWidth()
//                                        ) {
//                                            Text("Lihat Foto")
//                                        }

                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }

                                        val launcher = rememberLauncherForActivityResult(
                                            ActivityResultContracts.GetContent()
                                        ) { uri ->
                                            perbaikanFotoUri.value = uri
                                        }

                                        if (jabatanUser.value == "PT BIMA") {

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
                                                                showPerbaikanDialog.value = false
                                                                isSubmitting.value = true
                                                                val uri = perbaikanFotoUri.value
                                                                if (uri == null) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Silakan pilih foto terlebih dahulu",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    isSubmitting.value = false
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
                                                                                    val docId =
                                                                                        doc.id
                                                                                    val data =
                                                                                        doc.data
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
                                                                                    ).document(
                                                                                        docId
                                                                                    )
                                                                                        .update(
                                                                                            updateData
                                                                                        )
                                                                                        .addOnSuccessListener {
                                                                                            keteranganPerbaikan.value =
                                                                                                ""
                                                                                            perbaikanFotoUri.value =
                                                                                                null
                                                                                            checklistList.clear()
                                                                                            reloadTrigger.value =
                                                                                                !reloadTrigger.value

                                                                                            val url =
                                                                                                "https://script.google.com/macros/s/AKfycbzpCgmP3HVSTIQSVKHgVByeDwuKepTYCc5GzhQSWLiqIYpuRbXIf7vDa2OmtSunV9MU_g/exec"
                                                                                            val data =
                                                                                                mapOf(
                                                                                                    "kode_alat" to kodeAlat,
                                                                                                    "tanggal" to (checklist["tanggal"]
                                                                                                        ?: ""),
                                                                                                    "item" to (checklist["item"]
                                                                                                        ?: ""),
                                                                                                    "status_perbaikan" to "menunggu konfirmasi teknik"
                                                                                                )
                                                                                            val jsonData =
                                                                                                JSONObject(
                                                                                                    data
                                                                                                ).toString()

                                                                                            val client =
                                                                                                OkHttpClient()
                                                                                            val body =
                                                                                                jsonData.toRequestBody(
                                                                                                    "application/json".toMediaType()
                                                                                                )
                                                                                            val request =
                                                                                                Request.Builder()
                                                                                                    .url(
                                                                                                        url
                                                                                                    )
                                                                                                    .post(
                                                                                                        body
                                                                                                    )
                                                                                                    .build()

                                                                                            CoroutineScope(
                                                                                                Dispatchers.IO
                                                                                            ).launch {
                                                                                                try {
                                                                                                    val response =
                                                                                                        client.newCall(
                                                                                                            request
                                                                                                        )
                                                                                                            .execute()
                                                                                                    Log.d(
                                                                                                        "GAS",
                                                                                                        "Email sent: ${response.body?.string()}"
                                                                                                    )
                                                                                                } catch (e: Exception) {
                                                                                                    Log.e(
                                                                                                        "GAS",
                                                                                                        "Error: ${e.message}"
                                                                                                    )
                                                                                                }
                                                                                            }
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
                                                            "Laporan Perbaikan",
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

                                                            // ✅ Ganti Button jadi Column clickable
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable { launcher.launch("image/*") }
                                                                    .padding(16.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.AddCircle,
                                                                    contentDescription = "Upload",
                                                                    modifier = Modifier.size(48.dp),
                                                                    tint = Color.Gray
                                                                )
                                                                Text(
                                                                    "Ambil atau Pilih Foto",
                                                                    color = Color.Gray
                                                                )
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
                                    }

                                    if (statusPerbaikan == "menunggu konfirmasi teknik") {
                                        val isSubmitting = remember { mutableStateOf(false) }
//                                        OutlinedButton(
//                                            onClick = { showImage.value = true },
//                                            shape = RoundedCornerShape(10.dp),
//                                            colors = ButtonDefaults.outlinedButtonColors(
//                                                containerColor = Color.White,
//                                                contentColor = Color(0xFF003366)
//                                            ),
//                                            border = BorderStroke(1.dp, Color(0xFF003366)),
//                                            modifier = Modifier.fillMaxWidth()
//                                        ) {
//                                            Text("Lihat Foto Perbaikan")
//                                        }
                                        if (isSubmitting.value) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.CenterHorizontally
                                                )
                                            )
                                        }

                                        val launcher = rememberLauncherForActivityResult(
                                            ActivityResultContracts.GetContent()
                                        ) { uri ->
                                            perbaikanFotoUri.value = uri
                                        }

                                        if (jabatanUser.value == "teknik") {
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
                                                                showPerbaikanDialog.value = false
                                                                isSubmitting.value = true
                                                                val uri = perbaikanFotoUri.value
                                                                if (uri == null) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Silakan pilih foto terlebih dahulu",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    isSubmitting.value = false
                                                                    return@Button
                                                                }

                                                                // ✅ sama persis dengan kode lama (yang sukses upload)
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
                                                                                    val docId =
                                                                                        doc.id
                                                                                    val data =
                                                                                        doc.data
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
                                                                                        "keterangan_perbaikan_${index}_timestamp"

                                                                                    val updateData =
                                                                                        mapOf(
                                                                                            imgField to imageUrl,
                                                                                            ketField to keteranganPerbaikan.value,
                                                                                            timeField to FieldValue.serverTimestamp(),
                                                                                            "status_perbaikan" to "perlu perbaikan ulang"
                                                                                        )

                                                                                    firestore.collection(
                                                                                        "outstanding"
                                                                                    )
                                                                                        .document(
                                                                                            docId
                                                                                        )
                                                                                        .update(
                                                                                            updateData
                                                                                        )
                                                                                        .addOnSuccessListener {
                                                                                            // 🔹 Kirim notifikasi email via Apps Script (Revisi)
                                                                                            val urlRevisi =
                                                                                                "https://script.google.com/macros/s/AKfycb-revisi/exec"
                                                                                            val client =
                                                                                                OkHttpClient()
                                                                                            val jsonBody =
                                                                                                JSONObject().apply {
                                                                                                    put(
                                                                                                        "kode_alat",
                                                                                                        kodeAlat
                                                                                                    )
                                                                                                    put(
                                                                                                        "tanggal",
                                                                                                        checklist["tanggal"]
                                                                                                    )
                                                                                                    put(
                                                                                                        "item",
                                                                                                        checklist["item"]
                                                                                                    )
                                                                                                    put(
                                                                                                        "keterangan",
                                                                                                        keteranganPerbaikan.value
                                                                                                    )
                                                                                                }
                                                                                                    .toString()

                                                                                            val body =
                                                                                                RequestBody.create(
                                                                                                    "application/json".toMediaType(),
                                                                                                    jsonBody
                                                                                                )
                                                                                            val request =
                                                                                                Request.Builder()
                                                                                                    .url(
                                                                                                        urlRevisi
                                                                                                    )
                                                                                                    .post(
                                                                                                        body
                                                                                                    )
                                                                                                    .build()

                                                                                            Thread {
                                                                                                try {
                                                                                                    val response =
                                                                                                        client.newCall(
                                                                                                            request
                                                                                                        )
                                                                                                            .execute()
                                                                                                    Log.d(
                                                                                                        "APPS_SCRIPT",
                                                                                                        "Revisi sukses: ${response.body?.string()}"
                                                                                                    )
                                                                                                } catch (e: Exception) {
                                                                                                    Log.e(
                                                                                                        "APPS_SCRIPT",
                                                                                                        "Revisi gagal",
                                                                                                        e
                                                                                                    )
                                                                                                }
                                                                                            }.start()

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

                                                            // ✅ UI Upload Foto
                                                            if (perbaikanFotoUri.value == null) {
                                                                Column(
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable {
                                                                            launcher.launch(
                                                                                "image/*"
                                                                            )
                                                                        }
                                                                        .padding(16.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.AddCircle,
                                                                        contentDescription = "Upload",
                                                                        modifier = Modifier.size(48.dp),
                                                                        tint = Color.Gray
                                                                    )
                                                                    Text(
                                                                        "Ambil atau Pilih Foto",
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                            } else {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(200.dp)
                                                                ) {
                                                                    AsyncImage(
                                                                        model = perbaikanFotoUri.value,
                                                                        contentDescription = null,
                                                                        modifier = Modifier
                                                                            .matchParentSize()
                                                                            .clip(
                                                                                RoundedCornerShape(
                                                                                    8.dp
                                                                                )
                                                                            ),
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                    IconButton(
                                                                        onClick = {
                                                                            launcher.launch(
                                                                                "image/*"
                                                                            )
                                                                        },
                                                                        modifier = Modifier
                                                                            .align(Alignment.TopEnd)
                                                                            .padding(8.dp)
                                                                            .background(
                                                                                Color.White.copy(
                                                                                    alpha = 0.7f
                                                                                ), CircleShape
                                                                            )
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Edit,
                                                                            contentDescription = "Edit Foto",
                                                                            tint = Color.Black
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val firestore = FirebaseFirestore.getInstance()
                                                    val docRef = firestore.collection("outstanding")
                                                        .whereEqualTo("kode_alat", kodeAlat)
                                                        .whereEqualTo(
                                                            "tanggal",
                                                            checklist["tanggal"]
                                                        )
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
                                                                    // 🔹 Kirim notifikasi email via Apps Script (Konfirmasi)
                                                                    val urlKonfirmasi =
                                                                        "https://script.google.com/macros/s/AKfycbzosv_qLE5gVtqBvk0tBOR-5qQFitBj1hT8d8HQPoRnbp1uJJfypAqej7wjs-EcJWeR/exec"
                                                                    val client = OkHttpClient()
                                                                    val jsonBody =
                                                                        JSONObject().apply {
                                                                            put(
                                                                                "kode_alat",
                                                                                kodeAlat
                                                                            )
                                                                            put(
                                                                                "tanggal",
                                                                                checklist["tanggal"]
                                                                            )
                                                                            put(
                                                                                "item",
                                                                                checklist["item"]
                                                                            )
                                                                        }.toString()

                                                                    val body = RequestBody.create(
                                                                        "application/json".toMediaType(),
                                                                        jsonBody
                                                                    )
                                                                    val request = Request.Builder()
                                                                        .url(urlKonfirmasi)
                                                                        .post(body)
                                                                        .build()

                                                                    Thread {
                                                                        try {
                                                                            val response =
                                                                                client.newCall(
                                                                                    request
                                                                                ).execute()
                                                                            Log.d(
                                                                                "APPS_SCRIPT",
                                                                                "Konfirmasi sukses: ${response.body?.string()}"
                                                                            )
                                                                        } catch (e: Exception) {
                                                                            Log.e(
                                                                                "APPS_SCRIPT",
                                                                                "Konfirmasi gagal",
                                                                                e
                                                                            )
                                                                        }
                                                                    }.start()

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
                                    }

                                    if (statusPerbaikan == "perlu perbaikan ulang") {
//                                        OutlinedButton(
//                                            onClick = { showImage.value = true },
//                                            shape = RoundedCornerShape(50),
//                                            colors = ButtonDefaults.outlinedButtonColors(
//                                                containerColor = Color.White,
//                                                contentColor = Color(0xFF003366)
//                                            ),
//                                            border = BorderStroke(1.dp, Color(0xFF003366)),
//                                            modifier = Modifier.fillMaxWidth()
//                                        ) {
//                                            Text("Lihat Foto")
//                                        }

//                                        if (showImage.value) {
//                                            AlertDialog(
//                                                onDismissRequest = { showImage.value = false },
//                                                confirmButton = {
//                                                    TextButton(onClick = { showImage.value = false }) {
//                                                        Text("Tutup")
//                                                    }
//                                                },
//                                                text = {
//                                                    AsyncImage(
//                                                        model = fotoUrl,
//                                                        contentDescription = null,
//                                                        modifier = Modifier
//                                                            .fillMaxWidth()
//                                                            .clip(RoundedCornerShape(8.dp)),
//                                                        contentScale = ContentScale.Fit
//                                                    )
//                                                }
//                                            )
//                                        }
                                        if (jabatanUser.value == "PT BIMA") {
                                            Button(
                                                onClick = { showDialog.value = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF003366),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Perbaikan Ulang Selesai")
                                            }

                                            val isSubmitting = remember { mutableStateOf(false) }
                                            if (isSubmitting.value) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.align(
                                                        Alignment.CenterHorizontally
                                                    )
                                                )
                                            }

                                            if (showDialog.value) {
                                                AlertDialog(
                                                    onDismissRequest = {
                                                        showDialog.value = false
                                                        perbaikanKeterangan.clear()
                                                        perbaikanKeterangan.add("") // reset ke 1 input kosong
                                                        perbaikanFotoUri.value = null
                                                    },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                showDialog.value = false
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
                                                                            fun updateFirestore(
                                                                                imageUrl: String?
                                                                            ) {
                                                                                val index =
                                                                                    getNextPerbaikanRevisionIndex(
                                                                                        doc.data
                                                                                            ?: emptyMap()
                                                                                    )
                                                                                val nextFieldImage =
                                                                                    "gambar_perbaikan_$index"
                                                                                val nextFieldText =
                                                                                    "keterangan_perbaikan_$index"
                                                                                val nextFieldTimestamp =
                                                                                    "keterangan_perbaikan_${index}_timestamp"

                                                                                val updateData =
                                                                                    mutableMapOf<String, Any>(
                                                                                        nextFieldText to perbaikanKeterangan.filter { it.isNotBlank() },
                                                                                        "status_perbaikan" to "menunggu konfirmasi teknik",
                                                                                        nextFieldTimestamp to FieldValue.serverTimestamp()
                                                                                    )

                                                                                imageUrl?.let {
                                                                                    updateData[nextFieldImage] =
                                                                                        it
                                                                                }

                                                                                Log.d(
                                                                                    "UpdateFirestore",
                                                                                    "Data yang dikirim: $updateData"
                                                                                )

                                                                                firestore.collection(
                                                                                    "outstanding"
                                                                                ).document(docId)
                                                                                    .update(
                                                                                        updateData
                                                                                    )
                                                                                    .addOnSuccessListener {
                                                                                        Log.d(
                                                                                            "UpdateFirestore",
                                                                                            "Dokumen berhasil diupdate."
                                                                                        )
                                                                                        perbaikanKeterangan.clear()
                                                                                        perbaikanKeterangan.add(
                                                                                            ""
                                                                                        )
                                                                                        perbaikanFotoUri.value =
                                                                                            null
                                                                                        checklistList.clear()
                                                                                        reloadTrigger.value =
                                                                                            !reloadTrigger.value
                                                                                    }
                                                                                    .addOnFailureListener {
                                                                                        Log.e(
                                                                                            "UpdateFirestore",
                                                                                            "Gagal update dokumen: ${it.message}"
                                                                                        )
                                                                                    }

                                                                                val json =
                                                                                    JSONObject().apply {
                                                                                        put(
                                                                                            "kode_alat",
                                                                                            kodeAlat
                                                                                        )
                                                                                        put(
                                                                                            "tanggal",
                                                                                            checklist["tanggal"]
                                                                                        )
                                                                                        put(
                                                                                            "item",
                                                                                            checklist["item"]
                                                                                        )
                                                                                        put(
                                                                                            "keterangan_perbaikan_0",
                                                                                            perbaikanKeterangan.joinToString(
                                                                                                " | "
                                                                                            )
                                                                                        )
                                                                                        put(
                                                                                            "gambar_perbaikan_0",
                                                                                            imageUrl
                                                                                                ?: ""
                                                                                        )
                                                                                        put(
                                                                                            "operator_email",
                                                                                            doc.getString(
                                                                                                "operator_email"
                                                                                            ) ?: ""
                                                                                        )
                                                                                    }

                                                                                val client =
                                                                                    OkHttpClient()
                                                                                val requestBody =
                                                                                    RequestBody.create(
                                                                                        "application/json".toMediaTypeOrNull(),
                                                                                        json.toString()
                                                                                    )
                                                                                val request =
                                                                                    Request.Builder()
                                                                                        .url("https://script.google.com/macros/s/AKfycbyi75C-IWENqNu6B7EB2i1iAca5PDd9eToVrG7y-lypozfvo-u5natJafUN8it_H5SR6Q/exec")
                                                                                        .post(
                                                                                            requestBody
                                                                                        )
                                                                                        .build()

                                                                                client.newCall(
                                                                                    request
                                                                                ).enqueue(object :
                                                                                    Callback {
                                                                                    override fun onFailure(
                                                                                        call: Call,
                                                                                        e: IOException
                                                                                    ) {
                                                                                        Log.e(
                                                                                            "EmailNotif",
                                                                                            "Gagal kirim email",
                                                                                            e
                                                                                        )
                                                                                        isSubmitting.value =
                                                                                            false
                                                                                    }

                                                                                    override fun onResponse(
                                                                                        call: Call,
                                                                                        response: Response
                                                                                    ) {
                                                                                        Log.d(
                                                                                            "EmailNotif",
                                                                                            "Email berhasil dikirim: ${response.body?.string()}"
                                                                                        )
                                                                                        isSubmitting.value =
                                                                                            false
                                                                                    }
                                                                                })
                                                                            }

                                                                            val uri =
                                                                                perbaikanFotoUri.value
                                                                            if (uri != null) {
                                                                                val bitmap =
                                                                                    uriToBitmap(
                                                                                        this@OutstandingActivity,
                                                                                        uri
                                                                                    )
                                                                                if (bitmap != null) {
                                                                                    uploadImageToCloudinary(
                                                                                        bitmap
                                                                                    ) { imageUrl ->
                                                                                        if (imageUrl != null) {
                                                                                            updateFirestore(
                                                                                                imageUrl
                                                                                            )
                                                                                        } else {
                                                                                            Log.e(
                                                                                                "CloudinaryUpload",
                                                                                                "Upload gagal, URL null"
                                                                                            )
                                                                                            updateFirestore(
                                                                                                null
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                } else {
                                                                                    Log.e(
                                                                                        "CloudinaryUpload",
                                                                                        "Gagal konversi URI ke Bitmap"
                                                                                    )
                                                                                    updateFirestore(
                                                                                        null
                                                                                    )
                                                                                }
                                                                            } else {
                                                                                updateFirestore(null)
                                                                            }
                                                                        } else {
                                                                            isSubmitting.value =
                                                                                false
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
                                                            perbaikanKeterangan.clear()
                                                            perbaikanKeterangan.add("")
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
                                                                    .clickable {
                                                                        imageLauncher.launch(
                                                                            "image/*"
                                                                        )
                                                                    },
                                                                shape = RoundedCornerShape(12.dp),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    Color.Gray
                                                                ),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = Color(
                                                                        0xFFF8F8F8
                                                                    )
                                                                )
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
                                                                                modifier = Modifier.size(
                                                                                    40.dp
                                                                                )
                                                                            )
                                                                            Spacer(
                                                                                modifier = Modifier.height(
                                                                                    8.dp
                                                                                )
                                                                            )
                                                                            Text(
                                                                                "Klik untuk pilih foto",
                                                                                color = Color.Gray
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            // Loop semua input keterangan
                                                            perbaikanKeterangan.forEachIndexed { index, value ->
                                                                OutlinedTextField(
                                                                    value = value,
                                                                    onValueChange = { newValue ->
                                                                        perbaikanKeterangan[index] =
                                                                            newValue
                                                                    },
                                                                    placeholder = { Text("Masukkan keterangan perbaikan...") },
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                            }

                                                            // Tombol tambah keterangan baru
                                                            TextButton(onClick = {
                                                                perbaikanKeterangan.add(
                                                                    ""
                                                                )
                                                            }) {
                                                                Text("+ Tambah Keterangan")
                                                            }
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
            .addFormDataPart("upload_preset", "fotoalat")
            .build()

        val request = Request.Builder()
            .url(cloudinaryUrl)
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