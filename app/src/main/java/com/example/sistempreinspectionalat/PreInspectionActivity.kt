package com.example.sistempreinspectionalat

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

class PreInspectionActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SistemPreinspectionAlatTheme {
                PreInspectionScreen(
                    onNext = { tanggal, shift, alat, kapal ->
                        val intent = Intent(this, ChecklistActivity::class.java)
                        intent.putExtra("tanggal", tanggal)
                        intent.putExtra("shift", shift)
                        intent.putExtra("kode_alat", alat)
                        intent.putExtra("nama_kapal", kapal) // ⬅️ kirim nama kapal
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PreInspectionScreen(onNext: (String, String, String, String) -> Unit) {
    val context = LocalContext.current
    val darkBlue = Color(0xFF003366)
    val white = Color.White

    var selectedDate by remember { mutableStateOf("") }
    var selectedShift by remember { mutableStateOf("") }

    val shiftOptions = listOf("Shift 1 (08.00 - 16.00)", "Shift 2 (16.00 - 00.00)", "Shift 3 (00.00 - 08.00)")

    var alatExpanded by remember { mutableStateOf(false) }
    var shiftExpanded by remember { mutableStateOf(false) }
    var namaKapal by remember { mutableStateOf("") }
    val alatList = remember { mutableStateListOf<Pair<String, String>>() }
    var selectedAlat by remember { mutableStateOf("") }

    // ⏱ Ambil data alat dari Firestore
    LaunchedEffect(true) {
        FirebaseFirestore.getInstance().collection("alat")
            .get()
            .addOnSuccessListener { result ->
                alatList.clear()
                for (document in result) {
                    val kode = document.getString("kode_alat")
                    val nama = document.getString("nama") ?: ""
                    if (!kode.isNullOrEmpty()) alatList.add(kode to nama)
                }
            }
    }

    // 🔷 Layout: Header Biru + Konten Rounded Putih
    Box(modifier = Modifier.fillMaxSize().background(darkBlue)) {
        // 🔷 HEADER BIRU
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(darkBlue)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    (context as? Activity)?.finish()
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Form Pre Inspection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 🟡 KONTEN PUTIH ROUNDED MULAI DARI BAWAH HEADER
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp), // ⬅️ Dorong ke bawah supaya rounded terlihat
            color = white,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 📅 Tanggal
                DatePickerBox(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )

                // 🔽 Dropdown Kode Alat
                ExposedDropdownMenuBox(
                    expanded = alatExpanded,
                    onExpandedChange = { alatExpanded = !alatExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAlat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kode Alat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = alatExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = darkBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = darkBlue,
                            unfocusedTextColor = darkBlue,
                            focusedLabelColor = Color.Gray,
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = darkBlue
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = alatExpanded,
                        onDismissRequest = { alatExpanded = false }
                    ) {
                        alatList.forEach { (kode, nama) ->
                            DropdownMenuItem(
                                text = { Text("$kode ($nama)") }, // tampilkan keduanya
                                onClick = {
                                    selectedAlat = kode // ⬅️ hanya simpan kode
                                    alatExpanded = false
                                }
                            )
                        }
                    }
                }

                // 🔽 Shift
                ExposedDropdownMenuBox(
                    expanded = shiftExpanded,
                    onExpandedChange = { shiftExpanded = !shiftExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedShift,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Shift") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = darkBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = darkBlue,
                            unfocusedTextColor = darkBlue,
                            focusedLabelColor = Color.Gray,
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = darkBlue
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = shiftExpanded,
                        onDismissRequest = { shiftExpanded = false }
                    ) {
                        shiftOptions.forEach { shift ->
                            DropdownMenuItem(
                                text = { Text(shift) },
                                onClick = {
                                    selectedShift = shift
                                    shiftExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = namaKapal,
                    onValueChange = { namaKapal = it },
                    label = { Text("Nama Kapal") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = darkBlue,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = darkBlue,
                        unfocusedTextColor = darkBlue,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = darkBlue
                    )
                )

                // 🔘 Tombol Selanjutnya
                Button(
                    onClick = {
                        if (selectedDate.isEmpty() || selectedShift.isEmpty() || selectedAlat.isEmpty() || namaKapal.isEmpty()) {
                            Toast.makeText(context, "Lengkapi semua field termasuk Nama Kapal!", Toast.LENGTH_SHORT).show()
                        } else {
                            onNext(selectedDate, selectedShift, selectedAlat, namaKapal)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                ) {
                    Text("Selanjutnya", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DatePickerBox(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF003366),
                shape = RoundedCornerShape(4.dp)
            )
            .background(Color.White, shape = RoundedCornerShape(4.dp))
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(dateFormat.format(calendar.time))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (selectedDate.isEmpty()) "Pilih Tanggal" else selectedDate,
            color = if (selectedDate.isEmpty()) Color.Gray else Color(0xFF003366),
            fontSize = 16.sp
        )
    }
}