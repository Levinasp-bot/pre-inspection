package com.example.sistempreinspectionalat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                            val namaAlat = if (!alatDocs.isEmpty)
                                alatDocs.documents[0].getString("nama") ?: "Alat Tidak Dikenal"
                            else
                                "Alat Tidak Dikenal"

                            val item = ReportItem(kodeAlat, namaAlat, tanggal, shift, operator)
                            reports.add(item)
                        }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Daftar Laporan") })
        },
        content = { padding ->
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize()
            ) {
                items(reports) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(report) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF003366)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${report.kodeAlat} - ${report.namaAlat}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${report.tanggal} | ${report.shift}",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Operator: ${report.operator}",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    )
}
