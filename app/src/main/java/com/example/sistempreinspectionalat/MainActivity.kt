package com.example.sistempreinspectionalat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sistempreinspectionalat.ui.theme.SistemPreinspectionAlatTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SistemPreinspectionAlatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val darkBlue = Color(0xFF003366)
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var outstandingCount by remember { mutableStateOf(0) }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }

    // Ambil data checklist
    LaunchedEffect(Unit) {
        try {
            val result = firestore.collection("outstanding")
                .whereEqualTo("outstanding", true)
                .get()
                .await()

            val count = result.size()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menu",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        Toast.makeText(context, "Logout berhasil", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {} // Kosongkan
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_bg_new),
                    contentDescription = null,
                    contentScale = ContentScale.Crop, // agar memenuhi lebar dan tinggi sesuai rasio
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f) // rasio 16:9 tetap dijaga
                        .align(Alignment.TopCenter)   // pastikan menempel di atas (jika dalam Box)
                )

                // Manual Icon Menu di pojok kiri atas
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                        .padding(16.dp)
                        .statusBarsPadding()
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                // Konten utama
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 72.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HomeMenuItem(
                            label = "Pre\nInspection",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            backgroundColor = darkBlue,
                            onClick = {
                                context.startActivity(Intent(context, PreInspectionActivity::class.java))
                            }
                        )
                        HomeMenuItem(
                            label = "Alat",
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.crane),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            backgroundColor = darkBlue,
                            onClick = {
                                context.startActivity(Intent(context, AlatActivity::class.java))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        HomeMenuItem(
                            label = "Outstanding\nChecklist",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            backgroundColor = darkBlue,
                            badgeCount = outstandingCount,
                            onClick = {
                                context.startActivity(Intent(context, OutstandingActivity::class.java))
                            }
                        )
                        HomeMenuItem(
                            label = "Laporan &\nRiwayat",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            backgroundColor = darkBlue,
                            onClick = {
                                context.startActivity(Intent(context, LaporanActivity::class.java))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeMenuItem(
    label: String,
    icon: @Composable () -> Unit, // ✅ ubah ke composable
    backgroundColor: Color,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(36.dp)) {
                icon() // ✅ tampilkan composable icon/image

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .background(Color.Red, shape = RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SistemPreinspectionAlatTheme {
        HomeScreen()
    }
}