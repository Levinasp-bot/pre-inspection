package com.example.sistempreinspectionalat

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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

        val kondisiList = listOf("BAIK", "CUKUP", "TIDAK BAIK")
        val kondisiMap = remember { mutableStateMapOf<String, String>() }
        val coroutineScope = rememberCoroutineScope()
        val auth = FirebaseAuth.getInstance()
        val userName = remember { mutableStateOf("") }
        val darkBlue = Color(0xFF0066B3)
        val keteranganMap = remember { mutableStateMapOf<String, String>() }
        val fotoMap = remember { mutableStateMapOf<String, Bitmap?>() }

        // Load checklist dari Firestore
        LaunchedEffect(kodeAlat) {
            firestore.collection("alat")
                .whereEqualTo("kode_alat", kodeAlat)
                .get()
                .addOnSuccessListener { result ->
                    val doc = result.documents.firstOrNull()
                    val items = doc?.get("item") as? List<*> ?: emptyList<Any>()
                    items.filterIsInstance<String>().forEach {
                        checklistItems.add(it)
                        kondisiMap[it] = ""
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal ambil checklist", Toast.LENGTH_SHORT).show()
                }
        }

        // Load user
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

                // KONTEN PUTIH ROUNDED
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
                        items(checklistItems) { item ->
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

                                        val itemTidakBaik = checklistItems.filter {
                                            kondisiMap[it] == "TIDAK BAIK"
                                        }

                                        firestore.collection("checklist")
                                            .add(data)
                                            .addOnSuccessListener {
                                                itemTidakBaik.forEach { item ->
                                                    val gambar = fotoMap[item]
                                                    val keterangan = keteranganMap[item] ?: ""

                                                    if (gambar != null) {
                                                        // Upload gambar ke Cloudinary
                                                        uploadImageToCloudinary(gambar) { imageUrl ->
                                                            if (!imageUrl.isNullOrEmpty()) {
                                                                val dataOutstanding = hashMapOf(
                                                                    "item" to item,
                                                                    "gambar" to imageUrl,
                                                                    "keterangan" to keterangan,
                                                                    "kode_alat" to kodeAlat,
                                                                    "kondisi" to "TIDAK BAIK",
                                                                    "operator" to userName.value,
                                                                    "outstanding" to true,
                                                                    "shift" to shift,
                                                                    "status_perbaikan" to "perlu perbaikan PT BIMA",
                                                                    "tanggal" to tanggal
                                                                )

                                                                firestore.collection("outstanding")
                                                                    .add(dataOutstanding)
                                                                    .addOnSuccessListener {
                                                                        Log.d("Firestore", "Data outstanding berhasil disimpan")
                                                                    }
                                                                    .addOnFailureListener {
                                                                        Log.e("Firestore", "Gagal simpan data outstanding", it)
                                                                    }
                                                            }
                                                        }
                                                    } else {
                                                        // Jika tidak ada gambar
                                                        val dataOutstanding = hashMapOf(
                                                            "item" to item,
                                                            "gambar" to "", // kosong jika tidak ada
                                                            "keterangan" to keterangan,
                                                            "kode_alat" to kodeAlat,
                                                            "kondisi" to "TIDAK BAIK",
                                                            "operator" to userName.value,
                                                            "outstanding" to true,
                                                            "shift" to shift,
                                                            "status_perbaikan" to "perlu perbaikan PT BIMA",
                                                            "tanggal" to tanggal
                                                        )

                                                        firestore.collection("outstanding")
                                                            .add(dataOutstanding)
                                                            .addOnSuccessListener {
                                                                Log.d("Firestore", "Data outstanding (tanpa gambar) berhasil disimpan")
                                                            }
                                                            .addOnFailureListener {
                                                                Log.e("Firestore", "Gagal simpan data outstanding", it)
                                                            }
                                                    }
                                                }

                                                Toast.makeText(context, "Checklist berhasil disimpan", Toast.LENGTH_SHORT).show()


                                                if (itemTidakBaik.isNotEmpty()) {
                                                    val keteranganGabungan = itemTidakBaik.joinToString("\n") {
                                                        "- $it: ${keteranganMap[it] ?: "-"}"
                                                    }

                                                    // Siapkan data JSON untuk dikirim ke Apps Script
                                                    val jsonBody = JSONObject().apply {
                                                        put("kode_alat", kodeAlat)
                                                        put("tanggal", tanggal)
                                                        put("nama", userName.value)
                                                        put("checklist", JSONArray(itemTidakBaik))
                                                        put("keterangan", keteranganGabungan)
                                                    }

                                                    val client = OkHttpClient()
                                                    val requestBody = RequestBody.create(
                                                        "application/json; charset=utf-8".toMediaTypeOrNull(),
                                                        jsonBody.toString()
                                                    )

                                                    val request = Request.Builder()
                                                        .url("https://script.google.com/macros/s/AKfycbzZuShOBdWmR9nceO7PmzqjZgf56B1lLVeHO57rGtfs6dHfHaVWqADnRFFdQJkhe_ad/exec") // ganti dengan URL Apps Script kamu
                                                        .post(requestBody)
                                                        .build()

                                                    client.newCall(request).enqueue(object : Callback {
                                                        override fun onFailure(call: Call, e: IOException) {
                                                            Log.e("NotifikasiEmail", "Gagal kirim notifikasi: ${e.message}")
                                                        }

                                                        override fun onResponse(call: Call, response: Response) {
                                                            if (response.isSuccessful) {
                                                                Log.d("NotifikasiEmail", "Berhasil kirim notifikasi ke email")
                                                            } else {
                                                                Log.e("NotifikasiEmail", "Gagal: ${response.body?.string()}")
                                                            }
                                                        }
                                                    })
                                                    val intent = Intent(this@ChecklistActivity, MainActivity::class.java)
                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    startActivity(intent)
                                                    finish()
                                                }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Gagal menyimpan checklist", Toast.LENGTH_SHORT).show()
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

    @OptIn(ExperimentalMaterial3Api::class)
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
        val radioOptions = listOf("BAIK", "TIDAK BAIK")
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { onImageCapture(it) }
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
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center, // <--- ini bagian penting
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    radioOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = kondisi == option,
                                onClick = { onKondisiChange(option) }
                            )
                            Text(text = option)
                        }
                    }
                }

                if (kondisi == "TIDAK BAIK") {
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
                            .clickable { cameraLauncher.launch(null) },
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

