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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class OutstandingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SistemPreinspectionAlatTheme {
                OutstandingChecklistScreen()
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

        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            perbaikanFotoUri.value = uri
        }

        LaunchedEffect(Unit) {
            firestore.collection("alat")
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val data = doc.data
                        val kodeAlat = data["kode_alat"]?.toString() ?: continue
                        alatMap[kodeAlat] = data
                    }
                }

            firestore.collection("checklist")
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val data = doc.data
                        val kodeAlat = data["kode_alat"]?.toString() ?: ""

                        val allItems = data.entries
                            .filter { it.key !in listOf("kode_alat", "tanggal", "shift") && !it.key.endsWith("_keterangan") && !it.key.endsWith("_foto_url") }
                            .filter { it.value == "CUKUP" || it.value == "TIDAK BAIK" }

                        allItems.forEach {
                            val baseKey = it.key
                            val item = mapOf(
                                "kode_alat" to kodeAlat,
                                "tanggal" to (data["tanggal"] ?: ""),
                                "shift" to (data["shift"] ?: ""),
                                "item" to baseKey.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                                "kondisi" to it.value.toString(),
                                "keterangan" to (data["${baseKey}_keterangan"] ?: ""),
                                "foto_url" to (data["${baseKey}_foto_url"] ?: "")
                            )
                            checklistList.add(item)
                        }
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
                        val kondisi = checklist["kondisi"]?.toString() ?: ""
                        val bgColor = when (kondisi) {
                            "CUKUP" -> Color.Yellow.copy(alpha = 0.2f)
                            "TIDAK BAIK" -> Color.Red.copy(alpha = 0.2f)
                            else -> Color.White
                        }

                        val kodeAlat = checklist["kode_alat"]?.toString() ?: ""
                        val alatInfo = alatMap[kodeAlat]
                        val namaAlat = alatInfo?.get("nama")?.toString() ?: ""
                        val lokasi = alatInfo?.get("lokasi")?.toString() ?: ""

                        val kodeNamaAlat = if (namaAlat.isNotBlank()) {
                            "$kodeAlat - $namaAlat (${lokasi})"
                        } else {
                            kodeAlat
                        }

                        val showImage = remember { mutableStateOf(false) }
                        val fotoUrl = checklist["foto_url"] as? String

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

                                if ((checklist["keterangan"] as? String)?.isNotBlank() == true) {
                                    Text(
                                        text = "Keterangan: ${checklist["keterangan"]}",
                                        fontSize = 14.sp
                                    )
                                }

                                if (!fotoUrl.isNullOrBlank()) {
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

                                    if (showDialog.value) {
                                        AlertDialog(
                                            onDismissRequest = { showDialog.value = false },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        showDialog.value = false

                                                        val docRef = FirebaseFirestore.getInstance()
                                                            .collection("checklist")
                                                            .whereEqualTo("kode_alat", kodeAlat)
                                                            .whereEqualTo("tanggal", checklist["tanggal"])
                                                            .limit(1)

                                                        docRef.get().addOnSuccessListener { result ->
                                                            if (!result.isEmpty) {
                                                                val doc = result.documents[0]
                                                                val docId = doc.id
                                                                val baseKey = checklist["item"].toString().lowercase().replace(" ", "_")

                                                                perbaikanFotoUri.value?.let { uri ->
                                                                    val storageRef = FirebaseStorage.getInstance().reference
                                                                    val fileName = "foto_perbaikan/${UUID.randomUUID()}.jpg"
                                                                    val imageRef = storageRef.child(fileName)

                                                                    imageRef.putFile(uri)
                                                                        .addOnSuccessListener {
                                                                            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                                                                val updateData = mapOf(
                                                                                    "${baseKey}_foto_perbaikan" to downloadUri.toString(),
                                                                                    "${baseKey}_keterangan_perbaikan" to perbaikanKeterangan.value,
                                                                                    baseKey to "BAIK"
                                                                                )

                                                                                FirebaseFirestore.getInstance()
                                                                                    .collection("checklist")
                                                                                    .document(docId)
                                                                                    .update(updateData)
                                                                            }
                                                                        }
                                                                } ?: run {
                                                                    val updateData = mapOf(
                                                                        "${baseKey}_keterangan_perbaikan" to perbaikanKeterangan.value,
                                                                        baseKey to "BAIK"
                                                                    )

                                                                    FirebaseFirestore.getInstance()
                                                                        .collection("checklist")
                                                                        .document(doc.id)
                                                                        .update(updateData)
                                                                }
                                                            }
                                                        }
                                                        perbaikanKeterangan.value = ""
                                                        perbaikanFotoUri.value = null
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

                                                    OutlinedButton(onClick = { imageLauncher.launch("image/*") }) {
                                                        Text("Pilih Foto")
                                                    }

                                                    perbaikanFotoUri.value?.let {
                                                        Text("Foto terpilih: ${it.lastPathSegment}")
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
}
