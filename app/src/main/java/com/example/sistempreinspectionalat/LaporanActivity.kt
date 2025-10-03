package com.example.sistempreinspectionalat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme
import com.google.firebase.firestore.FirebaseFirestore

class LaporanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SistemPreinspectionAlatTheme {
                LaporanScreen(onItemClick = { report ->
                    val intent = Intent(this, DetailLaporanActivity::class.java).apply {
                        putExtra("kode_alat", report.kodeAlat)
                        putExtra("tanggal", report.tanggal)
                        putExtra("shift", report.shift)
                    }
                    startActivity(intent)
                })
            }
        }
    }
}

data class ReportItem(
    val kodeAlat: String,
    val namaAlat: String,
    val tanggal: String,
    val shift: String,
    val operator: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(onItemClick: (ReportItem) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val reports = remember { mutableStateListOf<ReportItem>() }
    val darkBlue = Color(0xFF003366)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("checklist")
            .get()
            .addOnSuccessListener { checklistResult ->
                for (doc in checklistResult) {
                    val kodeAlat = doc.getString("kode_alat") ?: continue
                    val tanggal = doc.getString("tanggal") ?: ""
                    val shift = doc.getString("shift") ?: ""
                    val operator = doc.getString("operator") ?: "Tidak diketahui"

                    firestore.collection("alat")
                        .whereEqualTo("kode_alat", kodeAlat)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { alatDocs ->
                            val namaAlat = if (!alatDocs.isEmpty) {
                                val rawName = alatDocs.documents[0].getString("nama") ?: "Alat Tidak Dikenal"
                                formatNamaAlat(rawName)
                            } else {
                                "Alat Tidak Dikenal"
                            }

                            val item = ReportItem(kodeAlat, namaAlat, tanggal, shift, operator)
                            reports.add(item)
                        }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
    ) {
        Column {
            // 🔹 Custom TopBar
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
                    Text(
                        text = "Daftar Laporan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // 🔹 Konten dengan rounded surface
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(reports) { report ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(report) },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${report.kodeAlat} - ${report.namaAlat}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkBlue
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${report.tanggal} | ${report.shift}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Operator: ${report.operator}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatNamaAlat(raw: String): String {
    return raw.split("_")
        .joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
}
