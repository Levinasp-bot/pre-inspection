package com.example.sistempreinspectionalat

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

class ChecklistActivity : ComponentActivity() {
    private val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dutgwdhss/image/upload"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val kondisiList = listOf("BAIK", "CUKUP", "TIDAK BAIK")
        val kondisiMap = remember { mutableStateMapOf<String, String>() }
        val keteranganMap = remember { mutableStateMapOf<String, String>() }
        val fotoMap = remember { mutableStateMapOf<String, Bitmap?>() }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(kodeAlat) {
            Log.d("ChecklistScreen", "Mengambil data untuk kodeAlat: $kodeAlat")

            if (kodeAlat.isBlank()) {
                Toast.makeText(context, "kodeAlat kosong", Toast.LENGTH_SHORT).show()
                Log.e("ChecklistScreen", "kodeAlat kosong")
                return@LaunchedEffect
            }

            firestore.collection("alat")
                .whereEqualTo("kode_alat", kodeAlat)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(context, "Dokumen tidak ditemukan", Toast.LENGTH_SHORT).show()
                        Log.e("ChecklistScreen", "Tidak ada dokumen dengan kode_alat = $kodeAlat")
                    } else {
                        val doc = result.documents.first()
                        val visual = doc.get("Visual") as? List<*>
                        val fungsi = doc.get("Fungsi System") as? List<*>

                        if (visual == null && fungsi == null) {
                            Toast.makeText(context, "Field checklist tidak ditemukan", Toast.LENGTH_SHORT).show()
                            Log.e("ChecklistScreen", "Field Visual dan Fungsi System tidak ditemukan")
                        }

                        (visual.orEmpty() + fungsi.orEmpty()).filterIsInstance<String>().forEach {
                            checklistItems.add(it)
                            kondisiMap[it] = ""
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Gagal mengambil checklist", Toast.LENGTH_SHORT).show()
                    Log.e("ChecklistScreen", "Gagal ambil data: ${e.message}")
                }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Checklist Pre-Inspection") })
            },
            content = { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(checklistItems) { item ->
                        ChecklistItemRow(
                            item = item,
                            kondisi = kondisiMap[item] ?: "",
                            kondisiOptions = kondisiList,
                            onKondisiChange = { newValue -> kondisiMap[item] = newValue },
                            keterangan = keteranganMap[item] ?: "",
                            onKeteranganChange = { keteranganMap[item] = it },
                            imageBitmap = fotoMap[item],
                            onImageCapture = { bitmap -> fotoMap[item] = bitmap }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val belumPilihKondisi = kondisiMap.values.any { it.isBlank() }
                                    val adaKekurangan = checklistItems.any { item ->
                                        val kondisi = kondisiMap[item] ?: ""
                                        val keterangan = keteranganMap[item]?.trim().orEmpty()
                                        val foto = fotoMap[item]
                                        if (kondisi == "CUKUP" || kondisi == "TIDAK BAIK") {
                                            keterangan.isBlank() || foto == null
                                        } else false
                                    }

                                    if (belumPilihKondisi) {
                                        Toast.makeText(context, "Masih ada kondisi yang belum dipilih", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    if (adaKekurangan) {
                                        Toast.makeText(context, "Untuk kondisi CUKUP atau TIDAK BAIK, keterangan dan foto wajib diisi", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val checklistData = hashMapOf(
                                        "kode_alat" to kodeAlat,
                                        "shift" to shift,
                                        "tanggal" to tanggal
                                    )

                                    for (item in checklistItems) {
                                        val key = item.lowercase().replace(" ", "_")
                                        val kondisi = kondisiMap[item] ?: ""
                                        checklistData[key] = kondisi

                                        if (kondisi == "CUKUP" || kondisi == "TIDAK BAIK") {
                                            checklistData["${key}_keterangan"] = keteranganMap[item] ?: ""

                                            val bitmap = fotoMap[item]
                                            if (bitmap != null) {
                                                val imageUrl = withContext(Dispatchers.IO) {
                                                    suspendCancellableCoroutine<String?> { cont ->
                                                        uploadImageToCloudinary(bitmap) { url ->
                                                            cont.resume(url, null)
                                                        }
                                                    }
                                                }

                                                if (imageUrl != null) {
                                                    checklistData["${key}_foto_url"] = imageUrl
                                                } else {
                                                    Toast.makeText(context, "Gagal upload gambar untuk $item", Toast.LENGTH_SHORT).show()
                                                    return@launch
                                                }
                                            }
                                        }
                                    }

                                    firestore.collection("checklist")
                                        .add(checklistData)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Checklist berhasil disimpan", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Gagal menyimpan checklist", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChecklistItemRow(
        item: String,
        kondisi: String,
        kondisiOptions: List<String>,
        onKondisiChange: (String) -> Unit,
        keterangan: String,
        onKeteranganChange: (String) -> Unit,
        imageBitmap: Bitmap?,
        onImageCapture: (Bitmap) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { onImageCapture(it) }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = item, fontSize = 16.sp)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = if (kondisi.isNotBlank()) kondisi else "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kondisi") },
                        placeholder = { Text("Pilih Kondisi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        kondisiOptions.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onKondisiChange(label)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (kondisi == "CUKUP" || kondisi == "TIDAK BAIK") {
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
                            .background(Color.Gray.copy(alpha = 0.1f))
                            .clickable { cameraLauncher.launch(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = "Hasil Foto",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Kamera",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Text("Ambil Foto", color = Color.Gray)
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

