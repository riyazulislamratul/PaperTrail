package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.GoogleAuthManager
import com.example.cloud.GoogleDriveVaultManager
import com.example.navigation.Screen
import com.example.reminder.ReminderManager
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.DocumentsListScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GeminiChatScreen
import com.example.ui.screens.GoogleDriveVaultScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ReviewVerifyScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.theme.PaperTrailTheme
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.DocumentViewModelFactory
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {

    private val viewModel: DocumentViewModel by viewModels {
        DocumentViewModelFactory((application as PaperTrailApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDocId = intent.getLongExtra(ReminderManager.EXTRA_DOC_ID, -1L)

        setContent {
            PaperTrailTheme {
                PaperTrailApp(
                    viewModel = viewModel,
                    initialDocId = if (initialDocId != -1L) initialDocId else null
                )
            }
        }
    }
}

@Composable
fun PaperTrailApp(
    viewModel: DocumentViewModel,
    initialDocId: Long? = null
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val authManager = remember { GoogleAuthManager(context) }
    val vaultManager = remember { GoogleDriveVaultManager(context) }

    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val filteredDocs by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val deadlines by viewModel.activeDeadlines.collectAsStateWithLifecycle()
    val tempScanData by viewModel.tempScanData.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    // Notification permission request for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission response handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Navigate directly to document detail if triggered from notification
        if (initialDocId != null && initialDocId > 0) {
            navController.navigate(Screen.DocumentDetail.createRoute(initialDocId))
        }
    }

    // Photo picker launcher for dashboard quick action
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageUri(uri, context) {
                navController.navigate(Screen.ReviewExtracted.route)
            }
        }
    }

    // Document / PDF file picker launcher for dashboard quick action
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri)
            if (type == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
                viewModel.processPdfUri(uri, context) {
                    navController.navigate(Screen.ReviewExtracted.route)
                }
            } else {
                viewModel.processImageUri(uri, context) {
                    navController.navigate(Screen.ReviewExtracted.route)
                }
            }
        }
    }

    // Track active destination to toggle bottom navigation bar and FAB visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevelDestination = currentDestination?.hierarchy?.any {
        it.route in listOf(
            Screen.Dashboard.route,
            Screen.Documents.route,
            Screen.Deadlines.route
        )
    } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Dashboard.route } == true,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.route == Screen.Dashboard.route } == true
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Documents.route } == true,
                        onClick = {
                            navController.navigate(Screen.Documents.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.route == Screen.Documents.route } == true
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Description else Icons.Outlined.Description,
                                contentDescription = "Documents"
                            )
                        },
                        label = { Text("Documents") },
                        modifier = Modifier.testTag("nav_documents")
                    )

                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Deadlines.route } == true,
                        onClick = {
                            navController.navigate(Screen.Deadlines.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.route == Screen.Deadlines.route } == true
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Alarm else Icons.Outlined.Alarm,
                                contentDescription = "Deadlines"
                            )
                        },
                        label = { Text("Deadlines") },
                        modifier = Modifier.testTag("nav_reminders")
                    )
                }
            }
        },
        floatingActionButton = {
            if (isTopLevelDestination) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Camera.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_scan")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Scan Document")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(260)
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(200)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(260)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(260)
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(200)
                    )
                }
            ) {
                // 1. Dashboard Screen
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        documents = allDocs,
                        deadlines = deadlines,
                        onScanClick = { navController.navigate(Screen.Camera.route) },
                        onImportImageClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                        onViewAllClick = { navController.navigate(Screen.Documents.route) },
                        onViewDeadlinesClick = { navController.navigate(Screen.Deadlines.route) },
                        onDocumentClick = { doc ->
                            navController.navigate(Screen.DocumentDetail.createRoute(doc.id))
                        },
                        onPlayGameClick = {
                            navController.navigate(Screen.Game.route)
                        },
                        onOpenImageStudioClick = {
                            navController.navigate(Screen.ImageStudio.route)
                        },
                        onOpenGeminiChatClick = {
                            navController.navigate(Screen.GeminiChat.route)
                        },
                        onOpenVaultClick = {
                            navController.navigate(Screen.GoogleDriveVault.route)
                        },
                        authManager = authManager
                    )
                }

                // 2. Documents Screen
                composable(Screen.Documents.route) {
                    DocumentsListScreen(
                        viewModel = viewModel,
                        documents = filteredDocs,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onScanClick = { navController.navigate(Screen.Camera.route) },
                        onDocumentClick = { doc ->
                            navController.navigate(Screen.DocumentDetail.createRoute(doc.id))
                        }
                    )
                }

                // 3. Deadlines / Reminders Screen
                composable(Screen.Deadlines.route) {
                    RemindersScreen(
                        viewModel = viewModel,
                        deadlines = deadlines,
                        onScanClick = { navController.navigate(Screen.Camera.route) },
                        onDocumentClick = { doc ->
                            navController.navigate(Screen.DocumentDetail.createRoute(doc.id))
                        }
                    )
                }

                // 4. Camera / Scan Screen
                composable(Screen.Camera.route) {
                    ScanScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.navigateUp() },
                        onScanExtracted = {
                            navController.navigate(Screen.ReviewExtracted.route)
                        }
                    )
                }

                // 5. Review & Verify Extracted Screen
                composable(Screen.ReviewExtracted.route) {
                    if (tempScanData != null) {
                        ReviewVerifyScreen(
                            viewModel = viewModel,
                            tempData = tempScanData!!,
                            onBackClick = { navController.navigateUp() },
                            onSaved = { docId ->
                                navController.navigate(Screen.DocumentDetail.createRoute(docId)) {
                                    popUpTo(Screen.Camera.route) { inclusive = true }
                                }
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }
                }

                // 6. Document Detail Screen
                composable(
                    route = Screen.DocumentDetail.route,
                    arguments = listOf(
                        navArgument(Screen.ARG_DOC_ID) { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val docId = backStackEntry.arguments?.getLong(Screen.ARG_DOC_ID) ?: 0L
                    DocumentDetailScreen(
                        viewModel = viewModel,
                        docId = docId,
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // 7. Deadline Rush Mini-Game
                composable(Screen.Game.route) {
                    GameScreen(
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // 8. AI Image Studio (Feature 1: Text-to-Image & Image Editing with Time-Limit & Drive Storage)
                composable(Screen.ImageStudio.route) {
                    ImageStudioScreen(
                        documents = allDocs,
                        authManager = authManager,
                        vaultManager = vaultManager,
                        onBackClick = { navController.navigateUp() },
                        onOpenVault = { navController.navigate(Screen.GoogleDriveVault.route) }
                    )
                }

                // 9. Gemini Chatbot (Feature 2: Multi-turn Advisor with Time-Limit & Drive Transcript Storage)
                composable(Screen.GeminiChat.route) {
                    GeminiChatScreen(
                        documents = allDocs,
                        authManager = authManager,
                        vaultManager = vaultManager,
                        onBackClick = { navController.navigateUp() },
                        onOpenVault = { navController.navigate(Screen.GoogleDriveVault.route) }
                    )
                }

                // 10. Google Drive Vault & Account Screen
                composable(Screen.GoogleDriveVault.route) {
                    GoogleDriveVaultScreen(
                        authManager = authManager,
                        vaultManager = vaultManager,
                        onBackClick = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}
