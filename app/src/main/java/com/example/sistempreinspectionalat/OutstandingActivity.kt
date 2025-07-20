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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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


class OutstandingActivity : ComponentActivity() {
    private val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dutgwdhss/image/upload"
    val darkBlue = Color(0xFF0066B3)

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
        val showDialog = remember { mutableStateOf(false) }
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
                            "status_perbaikan" to (data["status_perbaikan"] ?: "")
                        )

                        // ⬇️ Tambahkan semua field dinamis
                        for ((key, value) in data) {
                            if (key.startsWith("gambar_perbaikan_") || key.startsWith("keterangan_perbaikan_")) {
                                item[key] = value ?: ""
                            }
                        }

                        checklistList.add(item)

                    }
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Outstanding Checklist") })
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
                        val keteranganRevisi = remember { mutableStateOf("") }
                        val statusPerbaikan = checklist["status_perbaikan"]?.toString() ?: ""

                        val bgColor = when {
                            statusPerbaikan.startsWith("menunggu", ignoreCase = true) -> Color.Yellow.copy(alpha = 0.3f)
                            statusPerbaikan.startsWith("perlu", ignoreCase = true) -> Color.Red.copy(alpha = 0.2f)
                            else -> Color.White
                        }

                        val kodeAlat = checklist["kode_alat"]?.toString() ?: ""
                        val alatInfo = alatMap[kodeAlat]
                        val namaAlat = alatInfo?.get("nama")?.toString() ?: ""

                        val kodeNamaAlat = if (namaAlat.isNotBlank()) {
                            "$kodeAlat - $namaAlat"
                        } else {
                            kodeAlat
                        }

                        val showImage = remember { mutableStateOf(false) }
                        val fotoUrl = getLatestImageField(checklist)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = kodeNamaAlat,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Tanggal: ${checklist["tanggal"]} | Shift: ${checklist["shift"]}",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Item: ${checklist["item"]}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Text(
                                    text = "Kondisi: ${checklist["kondisi"]}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "Status Perbaikan: ${checklist["status_perbaikan"]}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )

                                if ((checklist["keterangan"] as? String)?.isNotBlank() == true) {
                                    val keteranganPerbaikan = getLatestKeteranganField(checklist)
                                    if (!keteranganPerbaikan.isNullOrBlank()) {
                                        Text(
                                            text = "Keterangan: $keteranganPerbaikan",
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                if (statusPerbaikan == "perlu perbaikan PT BIMA") {
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
                                                                        "status_perbaikan" to "menunggu konfirmasi operator" // atau status lain sesuai kebutuhan
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
                                                                        }
                                                                        .addOnFailureListener {
                                                                            Log.e("UpdateFirestore", "Gagal update dokumen: ${it.message}")
                                                                        }
                                                                    checklistList.clear()
                                                                    reloadTrigger.value = !reloadTrigger.value
                                                                    // Kirim notifikasi email
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

                                if (statusPerbaikan == "menunggu konfirmasi operator") {
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
                                            contentColor = Color.Red
                                        ),
                                        border = BorderStroke(1.dp, Color.Red),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Revisi")
                                    }
                                    if (showDialog.value) {
                                        AlertDialog(
                                            onDismissRequest = {
                                                showDialog.value = false
                                                keteranganRevisi.value = ""
                                                perbaikanFotoUri.value = null
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

                                                                val index = getNextPerbaikanRevisionIndex(doc.data ?: emptyMap())
                                                                val nextFieldImage = "gambar_perbaikan_$index"
                                                                val nextFieldText = "keterangan_perbaikan_$index"

                                                                val uri = perbaikanFotoUri.value
                                                                if (uri != null) {
                                                                    val bitmap = uriToBitmap(context, uri)
                                                                    if (bitmap != null) {
                                                                        uploadImageToCloudinary(bitmap) { imageUrl ->
                                                                            val updateData = mutableMapOf<String, Any>(
                                                                                nextFieldText to keteranganRevisi.value,
                                                                                "status_perbaikan" to "perlu perbaikan ulang"
                                                                            )
                                                                            if (imageUrl != null) {
                                                                                updateData[nextFieldImage] = imageUrl
                                                                            }
                                                                            firestore.collection("outstanding").document(docId).update(updateData)
                                                                            showDialog.value = false
                                                                            keteranganRevisi.value = ""
                                                                            perbaikanFotoUri.value = null
                                                                            checklistList.clear()
                                                                            reloadTrigger.value = !reloadTrigger.value
                                                                        }
                                                                    }
                                                                } else {
                                                                    val updateData = mutableMapOf<String, Any>(
                                                                        nextFieldText to keteranganRevisi.value,
                                                                        "status_perbaikan" to "perlu perbaikan ulang"
                                                                    )
                                                                    firestore.collection("outstanding").document(docId).update(updateData)
                                                                    showDialog.value = false
                                                                    keteranganRevisi.value = ""
                                                                    perbaikanFotoUri.value = null
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
                                                TextButton(
                                                    onClick = {
                                                        showDialog.value = false
                                                        keteranganRevisi.value = ""
                                                        perbaikanFotoUri.value = null
                                                    }
                                                ) {
                                                    Text("Batal")
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text("Upload foto dan beri keterangan revisi:")

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
                                                                    contentDescription = "Foto revisi",
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

                                                    OutlinedTextField(
                                                        value = keteranganRevisi.value,
                                                        onValueChange = { keteranganRevisi.value = it },
                                                        placeholder = { Text("Masukkan keterangan...") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
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
                                                    firestore.collection("outstanding").document(doc.id).update(
                                                        mapOf("status_perbaikan" to "menunggu persetujuan manager")
                                                    ).addOnSuccessListener {
                                                        firestore.collection("outstanding")
                                                            .whereEqualTo("outstanding", true)
                                                            .get()
                                                            .addOnSuccessListener { result2 ->
                                                                checklistList.clear()
                                                                for (doc2 in result2) {
                                                                    val data2 = doc2.data
                                                                    val item2 = mutableMapOf<String, Any>(
                                                                        "kode_alat" to (data2["kode_alat"] ?: ""),
                                                                        "tanggal" to (data2["tanggal"] ?: ""),
                                                                        "shift" to (data2["shift"] ?: ""),
                                                                        "item" to (data2["item"] ?: ""),
                                                                        "kondisi" to (data2["kondisi"] ?: ""),
                                                                        "keterangan" to (data2["keterangan"] ?: ""),
                                                                        "gambar" to (data2["gambar"] ?: ""),
                                                                        "status_perbaikan" to (data2["status_perbaikan"] ?: "")
                                                                    )

                                                                    // Tambahkan field dinamis dari gambar_perbaikan_ dan keterangan_perbaikan_
                                                                    for ((key, value) in data2) {
                                                                        if (key.startsWith("gambar_perbaikan_") || key.startsWith("keterangan_perbaikan_")) {
                                                                            item2[key] = value ?: ""
                                                                        }
                                                                    }
                                                                    checklistList.add(item2)
                                                                }
                                                            }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF006400)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF006400)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Konfirmasi Perbaikan")
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

    fun getLatestKeteranganField(document: Map<String, Any>): String? {
        val statusPerbaikan = document["status_perbaikan"]?.toString() ?: ""

        return if (statusPerbaikan == "perlu perbaikan PT BIMA") {
            document["keterangan"] as? String
        } else {
            val textFields = document.keys
                .filter { it.startsWith("keterangan_perbaikan_") }
                .mapNotNull {
                    val index = it.substringAfterLast("_").toIntOrNull()
                    index?.let { idx -> Pair(idx, it) }
                }
                .sortedBy { it.first }

            val latestField = textFields.lastOrNull()?.second
            latestField?.let { document[it] as? String }
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
