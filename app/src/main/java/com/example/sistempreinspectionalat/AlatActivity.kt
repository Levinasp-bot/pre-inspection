package com.example.sistempreinspectionalat

import android.app.Activity
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

class AlatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlatScreen()
        }
    }
}

@Composable
fun AlatScreen() {
    val context = LocalContext.current
    val darkBlue = Color(0xFF003366)
    val alatList = remember { mutableStateListOf<Map<String, String>>() }
    var searchText by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    // Ambil data dari Firestore
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("alat")
            .get()
            .addOnSuccessListener { result ->
                alatList.clear()
                result.documents.forEach { doc ->
                    alatList.add(
                        mapOf(
                            "kode_alat" to (doc.getString("kode_alat") ?: ""),
                            "nama" to (doc.getString("nama") ?: ""),
                            "status" to (doc.getString("status") ?: "")
                        )
                    )
                }
            }
    }

    // Proses filter pencarian dan status
    val filteredList = alatList.filter { alat ->
        val query = searchText.lowercase()
        val matchesSearch =
            alat["kode_alat"]?.lowercase()?.contains(query) == true ||
                    alat["nama"]?.lowercase()?.contains(query) == true

        val matchesStatus = when (selectedFilter) {
            "BREAK DOWN" -> alat["status"]?.equals("BREAK DOWN", ignoreCase = true) == true
            "READY FOR USE" -> alat["status"]?.equals("READY FOR USE", ignoreCase = true) == true
            else -> true
        }

        matchesSearch && matchesStatus
    }

    Box(modifier = Modifier.fillMaxSize().background(darkBlue)) {
        Column {
            // Header
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
                        text = "Alat",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Konten utama
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search + Filter
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Cari nama atau kode alat") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = Color.LightGray,
                                modifier = Modifier.clickable { showFilterDialog = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true
                    )

                    // Daftar alat
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredList) { alat ->
                            val status = alat["status"] ?: ""
                            val containerColor =
                                if (status.equals("BREAK DOWN", ignoreCase = true)) {
                                    Color.Red.copy(alpha = 0.2f)
                                } else {
                                    Color.White
                                }

                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = android.content.Intent(
                                            context,
                                            Class.forName("com.example.sistempreinspectionalat.DetailAlatActivity")
                                        )
                                        intent.putExtra("kode_alat", alat["kode_alat"])
                                        context.startActivity(intent)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                                border = BorderStroke(1.dp, darkBlue)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${alat["kode_alat"]} (${alat["nama"]})",
                                        color = darkBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Status: $status",
                                        color = if (status.equals("BREAK DOWN", ignoreCase = true))
                                            Color.Red else darkBlue,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog filter
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Pilih Status Alat") },
            text = {
                Column {
                    val options = listOf("Semua", "BREAK DOWN", "READY FOR USE")
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFilter = option
                                    showFilterDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedFilter == option),
                                onClick = {
                                    selectedFilter = option
                                    showFilterDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}