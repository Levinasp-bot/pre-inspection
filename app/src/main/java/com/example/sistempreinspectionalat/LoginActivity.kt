package com.example.sistempreinspectionalat

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.app.Activity
import android.content.Context
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SistemPreinspectionAlatTheme {
                LoginScreen()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreen() {
    var nipp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0066B3)) // Biru tua
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(100.dp)) // Dinaikkan dari 100.dp → 80.dp

            // Logo lebih besar
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(130.dp)
                    .padding(bottom = 24.dp)
            )

            // Judul lebih besar dan berjarak
            Text(
                text = "SI-PRILA",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Pre-Inspection & Operation Alat",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f)) // Memberi ruang fleksibel

            // Bottom Card (area biru muda)
            Surface(
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = Color(0xFFE9F3FC),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp) // agar naik ke atas lebih banyak
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Username Field
                    OutlinedTextField(
                        value = nipp,
                        onValueChange = { nipp = it },
                        label = { Text("NIPP", color = Color(0xFF0066B3)) },
                        textStyle = TextStyle(color = Color(0xFF0066B3)),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.user),
                                contentDescription = "Username Icon"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color(0xFF0066B3),
                            focusedTextColor = Color(0xFF0066B3)
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color(0xFF0066B3)) },
                        textStyle = TextStyle(color = Color(0xFF0066B3)),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.padlock),
                                contentDescription = "Password Icon"
                            )
                        },
                        trailingIcon = {
                            val visibilityIcon = if (passwordVisible)
                                painterResource(id = R.drawable.hidden)
                            else
                                painterResource(id = R.drawable.eye)

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    painter = visibilityIcon,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color(0xFF0066B3),
                            focusedTextColor = Color(0xFF0066B3)
                        )
                    )

                    Button(
                        onClick = {
                            isLoading = true
                            loginWithNIPP(nipp, password) { success, message ->
                                isLoading = false
                                if (success) {
                                    // Simpan status login
                                    val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                    sharedPrefs.edit().putBoolean("is_logged_in", true).apply()

                                    val intent = Intent(context, MainActivity::class.java)
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                }else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066B3)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = "Login", color = Color.White)
                        }
                    }

                    TextButton(
                        onClick = { /* TODO: handle forgot password */ },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Lupa Password?",
                            color = Color(0xFF0066B3),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum punya akun?",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        TextButton(
                            onClick = { /* navigasi */ },
                            contentPadding = PaddingValues(0.dp) // ini penting
                        ) {
                            Text(
                                text = "Daftar",
                                color = Color(0xFF0066B3),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun loginWithNIPP(nipp: String, password: String, onResult: (Boolean, String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("users") // Ganti nama koleksi jika berbeda
        .whereEqualTo("nipp", nipp)
        .limit(1)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val document = result.documents[0]
                val email = document.getString("email")

                if (email != null) {
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            onResult(true, null)
                        }
                        .addOnFailureListener {
                            onResult(false, "Password salah atau akun tidak valid.")
                        }
                } else {
                    onResult(false, "Email tidak ditemukan untuk NIPP tersebut.")
                }
            } else {
                onResult(false, "NIPP tidak ditemukan.")
            }
        }
        .addOnFailureListener {
            onResult(false, "Gagal mengakses database.")
        }
}
