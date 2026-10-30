package com.example.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Documents : Screen("documents")
    object Deadlines : Screen("deadlines")
    object Camera : Screen("camera")
    object ReviewExtracted : Screen("review_extracted")
    object DocumentDetail : Screen("document_detail/{docId}") {
        fun createRoute(docId: Long): String = "document_detail/$docId"
    }
    object Game : Screen("game")
    object ImageStudio : Screen("image_studio")
    object GeminiChat : Screen("gemini_chat")
    object GoogleDriveVault : Screen("google_drive_vault")

    companion object {
        const val ARG_DOC_ID = "docId"
    }
}
