package com.example.sistempreinspectionalat

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

class ChecklistActivity : ComponentActivity() {
    private val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dutgwdhss/image/upload"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val kodeAlat = intent.getStringExtra("kode_alat") ?: ""
        val tanggal = intent.getStringExtra("tanggal") ?: ""
        val shift = intent.getStringExtra("shift") ?: ""

        setContent {
            SistemPreinspectionAlatTheme {
                ChecklistScreen(
                    kodeAlat = kodeAlat,
                    shift = shift,
                    tanggal = tanggal
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChecklistScreen(kodeAlat: String, shift: String, tanggal: String) {
        val firestore = FirebaseFirestore.getInstance()
        val context = LocalContext.current
        val checklistItems = remember { mutableStateListOf<String>() }
        val kondisiMap = remember { mutableStateMapOf<String, String>() }
        val coroutineScope = rememberCoroutineScope()
        val auth = FirebaseAuth.getInstance()
        val userName = remember { mutableStateOf("") }
        val darkBlue = Color(0xFF003366)
        val keteranganMap = remember { mutableStateMapOf<String, String>() }
        val fotoMap = remember { mutableStateMapOf<String, Bitmap?>() }
        val preInspectionItems = remember { mutableStateListOf<String>() }
        val preOperationItems = remember { mutableStateListOf<String>() }
        val statusAlat = remember { mutableStateOf("READY FOR USE") }

        LaunchedEffect(kodeAlat) {
            firestore.collection("alat")
                .whereEqualTo("kode_alat", kodeAlat)
                .get()
                .addOnSuccessListener { result ->
                    val doc = result.documents.firstOrNull()
                    val preInspection = doc?.get("pre-inspection") as? List<*> ?: emptyList<Any>()
                    val preOperation = doc?.get("pre-operation") as? List<*> ?: emptyList<Any>()

                    preInspection.filterIsInstance<String>().forEach {
                        if (!preInspectionItems.contains(it)) {
                            preInspectionItems.add(it)
                            kondisiMap[it] = ""
                            checklistItems.add(it) // ✅ Tambahkan ke checklistItems
                        }
                    }
                    preOperation.filterIsInstance<String>().forEach {
                        if (!preOperationItems.contains(it)) {
                            preOperationItems.add(it)
                            kondisiMap[it] = ""
                            checklistItems.add(it) // ✅ Tambahkan ke checklistItems
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal ambil checklist", Toast.LENGTH_SHORT).show()
                }
        }

        LaunchedEffect(Unit) {
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrEmpty()) {
                val snapshot = firestore.collection("users").whereEqualTo("uid", uid).get().await()
                if (!snapshot.isEmpty) {
                    userName.value = snapshot.documents.first().getString("nama") ?: ""
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(darkBlue)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // HEADER
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
                            text = "Checklist Pre-Inspection",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

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
                        if (preInspectionItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Pre-Inspection",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            items(preInspectionItems) { item ->
                                ChecklistItemRow(
                                    item = item,
                                    kondisi = kondisiMap[item] ?: "",
                                    onKondisiChange = { newValue -> kondisiMap[item] = newValue },
                                    keterangan = keteranganMap[item] ?: "",
                                    onKeteranganChange = { newValue -> keteranganMap[item] = newValue },
                                    imageBitmap = fotoMap[item],
                                    onImageCapture = { bitmap -> fotoMap[item] = bitmap }
                                )
                            }
                        }

                        if (preOperationItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Pre-Operation",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            items(preOperationItems) { item ->
                                ChecklistItemRow(
                                    item = item,
                                    kondisi = kondisiMap[item] ?: "",
                                    onKondisiChange = { newValue -> kondisiMap[item] = newValue },
                                    keterangan = keteranganMap[item] ?: "",
                                    onKeteranganChange = { newValue -> keteranganMap[item] = newValue },
                                    imageBitmap = fotoMap[item],
                                    onImageCapture = { bitmap -> fotoMap[item] = bitmap }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Status Alat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = statusAlat.value == "READY FOR USE",
                                    onClick = { statusAlat.value = "READY FOR USE" }
                                )
                                Text("READY FOR USE")
                                Spacer(modifier = Modifier.width(16.dp))
                                RadioButton(
                                    selected = statusAlat.value == "BREAK DOWN",
                                    onClick = { statusAlat.value = "BREAK DOWN" }
                                )
                                Text("BREAK DOWN")
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val belumDipilih = kondisiMap.values.any { it.isBlank() }
                                        if (belumDipilih) {
                                            Toast.makeText(context, "Masih ada kondisi yang belum dipilih", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        val data = hashMapOf(
                                            "kode_alat" to kodeAlat,
                                            "shift" to shift,
                                            "tanggal" to tanggal,
                                            "operator" to userName.value
                                        )

                                        checklistItems.forEach { item ->
                                            val key = item.lowercase().replace(" ", "_")
                                            data[key] = kondisiMap[item] ?: ""
                                        }

                                        val kondisiTidakNormalSet = setOf(
                                            "TIDAK BAIK", "TIDAK NORMAL", "YA", "RUSAK",
                                            "TIDAK BERFUNGSI", "TIDAK NYALA", "KOTOR"
                                        )

                                        val itemTidakNormal = checklistItems.filter {
                                            kondisiMap[it] in kondisiTidakNormalSet
                                        }

                                        data["status"] = statusAlat.value

                                        // 🔹 STEP 1: simpan ke checklist
                                        firestore.collection("checklist")
                                            .add(data)
                                            .addOnSuccessListener {
                                                Log.d("Checklist", "Berhasil simpan checklist untuk $kodeAlat")

                                                if (itemTidakNormal.isNotEmpty()) {
                                                    // 🔹 STEP 2: kalau ada item tidak normal
                                                    itemTidakNormal.forEach { item ->
                                                        val gambar = fotoMap[item]
                                                        val keterangan = keteranganMap[item] ?: ""
                                                        val kondisi = kondisiMap[item] ?: "TIDAK DIKETAHUI"

                                                        Log.d("Checklist", "Cek outstanding → kode_alat=$kodeAlat, item=$item")

                                                        firestore.collection("outstanding")
                                                            .whereEqualTo("kode_alat", kodeAlat)
                                                            .whereEqualTo("item", item)
                                                            .whereEqualTo("outstanding", true)
                                                            .get()
                                                            .addOnSuccessListener { querySnapshot ->
                                                                if (!querySnapshot.isEmpty) {
                                                                    val doc = querySnapshot.documents.first()
                                                                    val statusPerbaikan = doc.getString("status_perbaikan") ?: ""
                                                                    Log.d("Checklist", "Outstanding ditemukan → status=$statusPerbaikan")

                                                                    if (statusPerbaikan == "menunggu konfirmasi operator") {
                                                                        val currentData = doc.data ?: emptyMap()
                                                                        val nextIndex = getNextPerbaikanRevisionIndex(currentData)

                                                                        val updateData = hashMapOf<String, Any>(
                                                                            "status_perbaikan" to "perlu perbaikan ulang"
                                                                        )

                                                                        if (gambar != null) {
                                                                            uploadImageToCloudinary(gambar) { imageUrl ->
                                                                                if (!imageUrl.isNullOrEmpty()) {
                                                                                    updateData["gambar_$nextIndex"] = imageUrl
                                                                                    updateData["keterangan_$nextIndex"] = keterangan
                                                                                    doc.reference.update(updateData)
                                                                                    Log.d("Checklist", "Update outstanding revisi → item=$item")
                                                                                }
                                                                            }
                                                                        } else {
                                                                            updateData["gambar_$nextIndex"] = ""
                                                                            updateData["keterangan_$nextIndex"] = keterangan
                                                                            doc.reference.update(updateData)
                                                                            Log.d("Checklist", "Update outstanding revisi tanpa gambar → item=$item")
                                                                        }
                                                                    } else {
                                                                        Log.d("Checklist", "Outstanding ditemukan tapi status=$statusPerbaikan → dilewati")
                                                                    }
                                                                } else {
                                                                    // Insert baru ke outstanding
                                                                    Log.d("Checklist", "Outstanding belum ada, buat baru untuk $item")

                                                                    if (gambar != null) {
                                                                        uploadImageToCloudinary(gambar) { imageUrl ->
                                                                            if (!imageUrl.isNullOrEmpty()) {
                                                                                val dataOutstanding = hashMapOf(
                                                                                    "item" to item,
                                                                                    "gambar" to imageUrl,
                                                                                    "keterangan" to keterangan,
                                                                                    "kode_alat" to kodeAlat,
                                                                                    "kondisi" to kondisi,
                                                                                    "operator" to userName.value,
                                                                                    "outstanding" to true,
                                                                                    "shift" to shift,
                                                                                    "status_perbaikan" to "menunggu tanggapan PT BIMA",
                                                                                    "tanggal" to tanggal,
                                                                                    "status" to statusAlat.value,
                                                                                    "timestamp_laporan" to FieldValue.serverTimestamp()
                                                                                )
                                                                                firestore.collection("outstanding").add(dataOutstanding)
                                                                                Log.d("Checklist", "Outstanding baru dibuat untuk $item")
                                                                            }
                                                                        }
                                                                    } else {
                                                                        val dataOutstanding = hashMapOf(
                                                                            "item" to item,
                                                                            "gambar" to "",
                                                                            "keterangan" to keterangan,
                                                                            "kode_alat" to kodeAlat,
                                                                            "kondisi" to kondisi,
                                                                            "operator" to userName.value,
                                                                            "outstanding" to true,
                                                                            "shift" to shift,
                                                                            "status_perbaikan" to "menunggu tanggapan PT BIMA",
                                                                            "tanggal" to tanggal,
                                                                            "status" to statusAlat.value,
                                                                            "timestamp_laporan" to FieldValue.serverTimestamp()
                                                                        )
                                                                        firestore.collection("outstanding").add(dataOutstanding)
                                                                        Log.d("Checklist", "Outstanding baru dibuat untuk $item tanpa gambar")
                                                                    }
                                                                }
                                                            }
                                                    }
                                                } else {
                                                    // 🔹 STEP 3: kalau semua normal
                                                    firestore.collection("outstanding")
                                                        .whereEqualTo("kode_alat", kodeAlat)
                                                        .get()
                                                        .addOnSuccessListener { querySnapshot ->
                                                            for (doc in querySnapshot) {
                                                                doc.reference.update("status_perbaikan", "menunggu verifikasi manager")
                                                                Log.d("Checklist", "Update outstanding existing → menunggu verifikasi manager")
                                                            }
                                                        }
                                                }

                                                // 🔹 STEP 4: update status alat
                                                firestore.collection("alat")
                                                    .whereEqualTo("kode_alat", kodeAlat)
                                                    .get()
                                                    .addOnSuccessListener { result ->
                                                        val alatDoc = result.documents.firstOrNull()
                                                        alatDoc?.reference?.update("status", statusAlat.value)
                                                        Log.d("Checklist", "Status alat $kodeAlat diupdate → ${statusAlat.value}")
                                                    }

                                                Toast.makeText(context, "Checklist berhasil disimpan", Toast.LENGTH_SHORT).show()

                                                // 🔹 STEP 5: kembali ke MainActivity
                                                val intent = Intent(this@ChecklistActivity, MainActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Gagal menyimpan checklist", Toast.LENGTH_SHORT).show()
                                                Log.e("Checklist", "Gagal simpan checklist: ${it.message}")
                                            }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                            ) {
                                Text("Simpan", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getNextPerbaikanRevisionIndex(data: Map<String, Any>): Int {
        val existingIndexes = data.keys.mapNotNull { key ->
            val match = Regex("gambar_(\\d+)").find(key)
            match?.groupValues?.get(1)?.toInt()
        }
        return if (existingIndexes.isEmpty()) 1 else (existingIndexes.maxOrNull() ?: 0) + 1
    }


    @Composable
    fun ChecklistItemRow(
        item: String,
        kondisi: String,
        onKondisiChange: (String) -> Unit,
        keterangan: String,
        onKeteranganChange: (String) -> Unit,
        imageBitmap: Bitmap?,
        onImageCapture: (Bitmap) -> Unit
    ) {
        val context = LocalContext.current
        val showDialog = remember { mutableStateOf(false) }

        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            bitmap?.let { onImageCapture(it) }
        }

        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                onImageCapture(bitmap)
            }
        }

        val radioOptions = when {
            item.contains("oli", ignoreCase = true) -> listOf("YA", "TIDAK")
            item.contains("lampu", ignoreCase = true) -> listOf("NYALA", "TIDAK NYALA")
            item.contains("ban", ignoreCase = true) -> listOf("YA", "TIDAK")
            item.contains("tangga", ignoreCase = true) -> listOf("RUSAK", "TIDAK")
            item.contains("kabin", ignoreCase = true) -> listOf("BERSIH", "KOTOR")
            item.contains("gerakan", ignoreCase = true) -> listOf("NORMAL", "TIDAK NORMAL")
            item.contains("slewing", ignoreCase = true) -> listOf("BERFUNGSI", "TIDAK BERFUNGSI")
            else -> listOf("BAIK", "TIDAK BAIK")
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text("Pilih Sumber Gambar") },
                confirmButton = {
                    TextButton(onClick = {
                        cameraLauncher.launch(null)
                        showDialog.value = false
                    }) {
                        Text("Kamera")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        galleryLauncher.launch("image/*")
                        showDialog.value = false
                    }) {
                        Text("Galeri")
                    }
                }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    radioOptions.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = kondisi == option,
                                onClick = { onKondisiChange(option) }
                            )
                            Text(text = option)
                        }
                    }
                }

                if (kondisi in listOf("YA", "TIDAK NYALA", "KOTOR", "TIDAK NORMAL", "TIDAK BERFUNGSI", "TIDAK BAIK", "RUSAK")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { onKeteranganChange(it) },
                        label = { Text("Keterangan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.1f))
                            .clickable { showDialog.value = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = "Hasil Foto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Upload",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Text("Ambil atau Pilih Foto", color = Color.Gray)
                            }
                        }
                    }
                }
            }
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
}

