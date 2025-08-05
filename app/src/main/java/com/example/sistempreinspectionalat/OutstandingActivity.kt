package com.example.sistempreinspectionalat

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

            // Ambil data alat
            firestore.collection("alat")
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val data = doc.data
                        val kodeAlat = data["kode_alat"]?.toString() ?: continue
                        alatMap[kodeAlat] = data
                    }
                }

            // Ambil data outstanding
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
                            "tanggapan_bima" to (data["tanggapan_bima"] ?: ""),
                            "instruksi_teknik" to (data["instruksi_teknik"] ?: "")
                        )

                        for ((key, value) in data) {
                            if (key.startsWith("keterangan_perbaikan_") || key.startsWith("gambar_perbaikan_")) {
                                item[key] = value ?: ""
                            }
                        }

                        // Optional: Log untuk debugging
                        Log.d("DocumentDebug", "Dokumen ID: ${doc.id}")
                        Log.d("DocumentDebug", "Semua key diambil: ${item.keys}")

                        checklistList.add(item)
                    }
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Outstanding Checklist",
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            val intent = Intent(context, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF003366), // Biru sesuai tema kamu
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(12.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(checklistList) { checklist ->
                        val statusPerbaikan = checklist["status_perbaikan"]?.toString() ?: ""
                        val kodeAlat = checklist["kode_alat"]?.toString() ?: ""
                        val alatInfo = alatMap[kodeAlat]
                        val namaAlat = alatInfo?.get("nama")?.toString() ?: ""
                        val showImage = remember { mutableStateOf(false) }
                        val fotoUrl = getLatestImageField(checklist)
                        val estimasiHari = remember { mutableStateOf("") }
                        val estimasiJam = remember { mutableStateOf("") }
                        val estimasiMenit = remember { mutableStateOf("") }
                        val selectedSparepartStatus = remember { mutableStateOf("Indent") }

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
                                Text(text = "${checklist["tanggal"]} | ${checklist["shift"]}", fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Status: ${checklist["status_perbaikan"]}", fontSize = 14.sp)
                                Text(text = "Item: ${checklist["item"]}", fontSize = 14.sp)
                                Text(text = "Kondisi: ${checklist["kondisi"]}", fontSize = 14.sp)

                                val latestKeterangan = getLatestKeteranganField(checklist)
                                if (!latestKeterangan.isNullOrBlank()) {
                                    Text(text = "Keterangan: $latestKeterangan", fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (statusPerbaikan == "menunggu tanggapan PT BIMA") {
                                    Spacer(modifier = Modifier.height(8.dp))

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

                                    Button(
                                        onClick = { showDialogBima.value = true },
                                        shape = RoundedCornerShape(10.dp), // lebih kecil
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF003366), // biru gelap kamu
                                            contentColor = Color.White // font putih
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Beri Tanggapan")
                                    }

                                    val isSubmitting = remember { mutableStateOf(false) }

                                    if (isSubmitting.value) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                    }

                                    if (showDialogBima.value) {
                                        AlertDialog(
                                            onDismissRequest = {
                                                showDialogBima.value = false
                                                perbaikanKeterangan.value = ""
                                            },
                                            title = {
                                                Text(
                                                    "Tanggapan Perbaikan",
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                            },
                                            text = {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        "Silakan masukkan tanggapan Anda terkait perbaikan alat ini.",
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    OutlinedTextField(
                                                        value = perbaikanKeterangan.value,
                                                        onValueChange = { perbaikanKeterangan.value = it },
                                                        placeholder = { Text("Masukkan tanggapan...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        showDialogBima.value = false
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
                                                                fun updateFirestore() {
                                                                    val updateData = mapOf(
                                                                        "tanggapan_bima" to perbaikanKeterangan.value,
                                                                        "tanggapan_bima_timestamp" to FieldValue.serverTimestamp(),
                                                                        "status_perbaikan" to "menunggu tanggapan teknik"
                                                                    )

                                                                    firestore.collection("outstanding").document(docId).update(updateData)
                                                                        .addOnSuccessListener {
                                                                            Log.d("UpdateFirestore", "Dokumen berhasil diupdate.")
                                                                            perbaikanKeterangan.value = ""
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
                                                                        put("tanggapan_bima", perbaikanKeterangan.value)
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

                                                                updateFirestore()

                                                            } else {
                                                                isSubmitting.value = false
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                                                ) {
                                                    Text("Kirim", color = Color.White)
                                                }
                                            },
                                            dismissButton = {
                                                OutlinedButton(
                                                    onClick = {
                                                        showDialogBima.value = false
                                                        perbaikanKeterangan.value = ""
                                                    }
                                                ) {
                                                    Text("Batal")
                                                }
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                if (statusPerbaikan == "menunggu tanggapan teknik") {
                                    Spacer(modifier = Modifier.height(8.dp))

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

                                    Button(
                                        onClick = { showDialogTeknik.value = true },
                                        shape = RoundedCornerShape(10.dp), // lebih kecil
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF003366), // biru gelap kamu
                                            contentColor = Color.White // font putih
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Beri Tanggapan")
                                    }

                                    if (showDialogTeknik.value) {
                                        AlertDialog(
                                            onDismissRequest = {
                                                showDialogTeknik.value = false
                                                perbaikanKeterangan.value = ""
                                                estimasiHari.value = ""
                                                estimasiJam.value = ""
                                                estimasiMenit.value = ""
                                                selectedSparepartStatus.value = ""
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
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

                                                                val statusUpdate = when (selectedSparepartStatus.value) {
                                                                    "Indent" -> "menunggu pengadaan sparepart"
                                                                    "Available" -> "proses perbaikan alat oleh PT Bima"
                                                                    else -> "menunggu pengadaan sparepart"
                                                                }

                                                                val updateData = mapOf(
                                                                    "tanggapan_bima" to perbaikanKeterangan.value,
                                                                    "tanggapan_bima_timestamp" to FieldValue.serverTimestamp(),
                                                                    "estimasi_hari" to estimasiHari.value,
                                                                    "estimasi_jam" to estimasiJam.value,
                                                                    "estimasi_menit" to estimasiMenit.value,
                                                                    "status_perbaikan" to statusUpdate,
                                                                    "status_sparepart" to selectedSparepartStatus.value
                                                                )

                                                                firestore.collection("outstanding").document(docId).update(updateData).addOnSuccessListener {
                                                                    showDialogTeknik.value = false
                                                                    perbaikanKeterangan.value = ""
                                                                    estimasiHari.value = ""
                                                                    estimasiJam.value = ""
                                                                    estimasiMenit.value = ""
                                                                    selectedSparepartStatus.value = ""
                                                                    checklistList.clear()
                                                                    reloadTrigger.value = !reloadTrigger.value
                                                                }
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Submit")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = {
                                                    showDialogTeknik.value = false
                                                    perbaikanKeterangan.value = ""
                                                    estimasiHari.value = ""
                                                    estimasiJam.value = ""
                                                    estimasiMenit.value = ""
                                                    selectedSparepartStatus.value = ""
                                                }) {
                                                    Text("Batal")
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text("Masukkan tanggapan teknisi:")
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    OutlinedTextField(
                                                        value = perbaikanKeterangan.value,
                                                        onValueChange = { perbaikanKeterangan.value = it },
                                                        placeholder = { Text("Tanggapan teknisi...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text("Estimasi Waktu Perbaikan:")

                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        OutlinedTextField(
                                                            value = estimasiHari.value,
                                                            onValueChange = { estimasiHari.value = it },
                                                            label = { Text("Hari") },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        OutlinedTextField(
                                                            value = estimasiJam.value,
                                                            onValueChange = { estimasiJam.value = it },
                                                            label = { Text("Jam") },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        OutlinedTextField(
                                                            value = estimasiMenit.value,
                                                            onValueChange = { estimasiMenit.value = it },
                                                            label = { Text("Menit") },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text("Ketersediaan Sparepart:")

                                                    val options = listOf("Indent", "Available")
                                                    options.forEach { option ->
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            RadioButton(
                                                                selected = selectedSparepartStatus.value == option,
                                                                onClick = { selectedSparepartStatus.value = option }
                                                            )
                                                            Text(option)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                if (statusPerbaikan == "menunggu pengadaan sparepart") {
                                    Spacer(modifier = Modifier.height(8.dp))

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

                                    val showInstruksiDialog = remember { mutableStateOf(false) }
                                    val instruksiPerbaikan = remember { mutableStateOf("") }

                                    Button(
                                        onClick = { showInstruksiDialog.value = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF003366),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Sparepart Sudah Tersedia")
                                    }

                                    if (showInstruksiDialog.value) {
                                        AlertDialog(
                                            onDismissRequest = {
                                                showInstruksiDialog.value = false
                                                instruksiPerbaikan.value = ""
                                            },
                                            confirmButton = {
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
                                                                val updateData = mapOf(
                                                                    "instruksi_teknik" to instruksiPerbaikan.value,
                                                                    "instruksi_teknik_timestamp" to FieldValue.serverTimestamp(),
                                                                    "status_perbaikan" to "proses perbaikan alat oleh PT Bima"
                                                                )

                                                                firestore.collection("outstanding").document(docId)
                                                                    .update(updateData)
                                                                    .addOnSuccessListener {
                                                                        showInstruksiDialog.value = false
                                                                        instruksiPerbaikan.value = ""
                                                                        checklistList.clear()
                                                                        reloadTrigger.value = !reloadTrigger.value
                                                                    }
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                                                ) {
                                                    Text("Kirim", color = Color.White)
                                                }
                                            },
                                            dismissButton = {
                                                OutlinedButton(onClick = {
                                                    showInstruksiDialog.value = false
                                                    instruksiPerbaikan.value = ""
                                                }) {
                                                    Text("Batal")
                                                }
                                            },
                                            title = {
                                                Text("Instruksi Perbaikan", style = MaterialTheme.typography.titleLarge)
                                            },
                                            text = {
                                                Column {
                                                    Text("Masukkan instruksi perbaikan untuk PT BIMA:")
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    OutlinedTextField(
                                                        value = instruksiPerbaikan.value,
                                                        onValueChange = { instruksiPerbaikan.value = it },
                                                        placeholder = { Text("Contoh: Silakan lakukan penggantian part...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                if (statusPerbaikan == "proses perbaikan alat oleh PT Bima") {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Tombol LIHAT FOTO
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

                                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                                        Text("Beri Tanggapan")
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
                                                        val uri = perbaikanFotoUri.value
                                                        if (uri == null) {
                                                            Toast.makeText(context, "Silakan pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }

                                                        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

                                                        uploadImageToCloudinary(bitmap) { imageUrl ->
                                                            if (imageUrl != null) {
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
                                                                        val data = doc.data ?: emptyMap()

                                                                        val index = data.keys
                                                                            .filter { it.startsWith("keterangan_perbaikan_") }
                                                                            .mapNotNull { it.removePrefix("keterangan_perbaikan_").toIntOrNull() }
                                                                            .maxOrNull()?.plus(1) ?: 0

                                                                        val imgField = "gambar_perbaikan_$index"
                                                                        val ketField = "keterangan_perbaikan_$index"
                                                                        val timeField = "${ketField}_timestamp"

                                                                        val updateData = mapOf(
                                                                            imgField to imageUrl,
                                                                            ketField to keteranganPerbaikan.value,
                                                                            timeField to FieldValue.serverTimestamp(),
                                                                            "status_perbaikan" to "menunggu konfirmasi operator"
                                                                        )

                                                                        firestore.collection("outstanding").document(docId)
                                                                            .update(updateData)
                                                                            .addOnSuccessListener {
                                                                                showPerbaikanDialog.value = false
                                                                                keteranganPerbaikan.value = ""
                                                                                perbaikanFotoUri.value = null
                                                                                checklistList.clear()
                                                                                reloadTrigger.value = !reloadTrigger.value
                                                                            }
                                                                    }
                                                                }
                                                            } else {
                                                                Toast.makeText(context, "Upload gambar gagal", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
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
                                                Text("Tanggapan Perbaikan", style = MaterialTheme.typography.titleLarge)
                                            },
                                            text = {
                                                Column {
                                                    Text("Masukkan keterangan dan unggah foto perbaikan:")
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    OutlinedTextField(
                                                        value = keteranganPerbaikan.value,
                                                        onValueChange = { keteranganPerbaikan.value = it },
                                                        placeholder = { Text("Contoh: Sudah diganti dengan part baru...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    Button(
                                                        onClick = { launcher.launch("image/*") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BBB))
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

                                if (statusPerbaikan == "menunggu konfirmasi operator") {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Tombol lihat foto terakhir
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

                                    if (showImage.value) {
                                        val data = checklist
                                        val maxIndex = data.keys
                                            .filter { it.startsWith("gambar_perbaikan_") }
                                            .mapNotNull { it.removePrefix("gambar_perbaikan_").toIntOrNull() }
                                            .maxOrNull() ?: 0
                                        val lastFotoUrl = data["gambar_perbaikan_$maxIndex"] as? String ?: ""

                                        AlertDialog(
                                            onDismissRequest = { showImage.value = false },
                                            confirmButton = {
                                                TextButton(onClick = { showImage.value = false }) {
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

                                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                                                        val uri = perbaikanFotoUri.value
                                                        if (uri == null) {
                                                            Toast.makeText(context, "Silakan pilih foto terlebih dahulu", Toast.LENGTH_SHORT).show()
                                                            return@Button
                                                        }

                                                        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

                                                        uploadImageToCloudinary(bitmap) { imageUrl ->
                                                            if (imageUrl != null) {
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
                                                                        val data = doc.data ?: emptyMap()

                                                                        val index = data.keys
                                                                            .filter { it.startsWith("keterangan_perbaikan_") }
                                                                            .mapNotNull { it.removePrefix("keterangan_perbaikan_").toIntOrNull() }
                                                                            .maxOrNull()?.plus(1) ?: 0

                                                                        val imgField = "gambar_perbaikan_$index"
                                                                        val ketField = "keterangan_perbaikan_$index"
                                                                        val timeField = "revisi_operator_${index}_timestamp"

                                                                        val updateData = mapOf(
                                                                            imgField to imageUrl,
                                                                            ketField to keteranganPerbaikan.value,
                                                                            timeField to FieldValue.serverTimestamp(),
                                                                            "status_perbaikan" to "perlu perbaikan ulang"
                                                                        )

                                                                        firestore.collection("outstanding").document(docId)
                                                                            .update(updateData)
                                                                            .addOnSuccessListener {
                                                                                showPerbaikanDialog.value = false
                                                                                keteranganPerbaikan.value = ""
                                                                                perbaikanFotoUri.value = null
                                                                                checklistList.clear()
                                                                                reloadTrigger.value = !reloadTrigger.value
                                                                            }
                                                                    }
                                                                }
                                                            } else {
                                                                Toast.makeText(context, "Upload ke Cloudinary gagal", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
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
                                            title = { Text("Revisi Perbaikan", style = MaterialTheme.typography.titleLarge) },
                                            text = {
                                                Column {
                                                    Text("Masukkan keterangan dan unggah foto revisi:")
                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    OutlinedTextField(
                                                        value = keteranganPerbaikan.value,
                                                        onValueChange = { keteranganPerbaikan.value = it },
                                                        placeholder = { Text("Contoh: Part lama tidak sesuai, perlu diganti ulang...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Spacer(modifier = Modifier.height(12.dp))

                                                    Button(
                                                        onClick = { launcher.launch("image/*") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005BBB))
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

                                    // Tombol Konfirmasi Perbaikan
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
                                                        "status_perbaikan" to "menunggu verifikasi Manager Operasi",
                                                        "konfirmasi_operator_timestamp" to FieldValue.serverTimestamp()
                                                    )
                                                    firestore.collection("outstanding").document(docId)
                                                        .update(update)
                                                        .addOnSuccessListener {
                                                            checklistList.clear()
                                                            reloadTrigger.value = !reloadTrigger.value
                                                        }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
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
                                                                fun updateFirestore() {
                                                                    val updateData = mapOf(
                                                                        "tanggapan_bima" to perbaikanKeterangan.value,
                                                                        "tanggapan_bima_timestamp" to FieldValue.serverTimestamp(),
                                                                        "status_perbaikan" to "menunggu tanggapan teknik"
                                                                    )

                                                                    firestore.collection("outstanding").document(docId).update(updateData)
                                                                        .addOnSuccessListener {
                                                                            Log.d("UpdateFirestore", "Dokumen berhasil diupdate.")
                                                                            perbaikanKeterangan.value = ""
                                                                        }
                                                                        .addOnFailureListener {
                                                                            Log.e("UpdateFirestore", "Gagal update dokumen: ${it.message}")
                                                                        }

                                                                    val json = JSONObject().apply {
                                                                        put("kode_alat", kodeAlat)
                                                                        put("tanggal", checklist["tanggal"])
                                                                        put("item", checklist["item"])
                                                                        put("tanggapan_bima", perbaikanKeterangan.value)
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

                                                                updateFirestore()

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
        )
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
                val value = document["keterangan"]?.toString()
                Log.d("KeteranganDebug", "Mengambil dari 'keterangan': $value")
                value
            }

            status == "menunggu tanggapan teknik" -> {
                val value = document["tanggapan_bima"]?.toString()
                Log.d("KeteranganDebug", "Mengambil dari 'tanggapan_bima': $value")
                value
            }

            status == "menunggu pengadaan sparepart" || status == "proses perbaikan alat oleh pt bima" -> {
                val value = document["instruksi_teknik"]?.toString()
                Log.d("KeteranganDebug", "Mengambil dari 'instruksi_teknik': $value")
                value
            }

            status == "menunggu konfirmasi operator" || status == "perlu perbaikan ulang" -> {
                val keteranganFields = document.keys
                    .filter { it.startsWith("keterangan_perbaikan_") }
                    .mapNotNull { key: String ->
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
