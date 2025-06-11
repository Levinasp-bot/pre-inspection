package com.example.sistempreinspectionalat

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.input.pointer.pointerInput


class PreInspectionActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SistemPreinspectionAlatTheme {
                PreInspectionScreen(
                    onNext = { tanggal, shift, alat ->
                        val intent = Intent(this, ChecklistActivity::class.java)
                        intent.putExtra("tanggal", tanggal)
                        intent.putExtra("shift", shift)
                        intent.putExtra("kode_alat", alat)
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
fun PreInspectionScreen(onNext: (String, String, String) -> Unit) {
    val context = LocalContext.current

    val darkBlue = Color(0xFF0066B3)
    val lightBlue = Color(0xFFE9F3FC)

    var selectedDate by remember { mutableStateOf("") }

    val shiftOptions = listOf("Shift 1", "Shift 2", "Shift 3")
    var selectedShift by remember { mutableStateOf("") }

    val alatList = remember { mutableStateListOf<String>() }
    var selectedAlat by remember { mutableStateOf("") }

    // Ambil alat dari Firestore
    LaunchedEffect(true) {
        FirebaseFirestore.getInstance().collection("alat")
            .get()
            .addOnSuccessListener { result ->
                alatList.clear()
                for (document in result) {
                    document.getString("kode_alat")?.let { alatList.add(it) }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Judul di area biru tua
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(darkBlue)
                    .padding(top = 48.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Form Pre-Inspection",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Konten card bawahnya
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(lightBlue, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // === DatePickerTextField (klik muncul kalender) ===
                        DatePickerBox(
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it }
                        )

                        // === Pilih Shift Dropdown ===
                        var shiftExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = shiftExpanded,
                            onExpandedChange = { shiftExpanded = !shiftExpanded }
                        ) {
                            TextField(
                                value = selectedShift,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih Shift") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = darkBlue,
                                    unfocusedTextColor = darkBlue,
                                    focusedLabelColor = darkBlue,
                                    unfocusedLabelColor = darkBlue,
                                    focusedIndicatorColor = darkBlue,
                                    unfocusedIndicatorColor = darkBlue,
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

                        // === Pilih Alat Dropdown ===
                        var alatExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = alatExpanded,
                            onExpandedChange = { alatExpanded = !alatExpanded }
                        ) {
                            TextField(
                                value = selectedAlat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih Alat") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = darkBlue,
                                    unfocusedTextColor = darkBlue,
                                    focusedLabelColor = darkBlue,
                                    unfocusedLabelColor = darkBlue,
                                    focusedIndicatorColor = darkBlue,
                                    unfocusedIndicatorColor = darkBlue,
                                    cursorColor = darkBlue
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = alatExpanded,
                                onDismissRequest = { alatExpanded = false }
                            ) {
                                alatList.forEach { alat ->
                                    DropdownMenuItem(
                                        text = { Text(alat) },
                                        onClick = {
                                            selectedAlat = alat
                                            alatExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Tombol Next
                        Button(
                            onClick = {
                                if (selectedDate.isEmpty() || selectedShift.isEmpty() || selectedAlat.isEmpty()) {
                                    Toast.makeText(context, "Lengkapi semua field!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onNext(selectedDate, selectedShift, selectedAlat)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                        ) {
                            Text("Next", color = Color.White)
                        }
                    }
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
                color = Color(0xFF0066B3),
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
            color = if (selectedDate.isEmpty()) Color.Gray else Color(0xFF0066B3),
            fontSize = 16.sp
        )
    }
}


