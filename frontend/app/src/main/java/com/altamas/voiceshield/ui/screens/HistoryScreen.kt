package com.altamas.voiceshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altamas.voiceshield.data.RetrofitClient
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.models.HistoryResponse
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onBackClick: () -> Unit
) {

    // =========================================================
    // CONTEXT
    // =========================================================

    val context =
        androidx.compose.ui.platform.LocalContext.current

    // =========================================================
    // COROUTINE SCOPE
    // =========================================================

    val scope =
        rememberCoroutineScope()

    // =========================================================
    // STATE
    // =========================================================

    var historyList by remember {
        mutableStateOf<List<HistoryResponse>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var deletingHistoryId by remember {
        mutableStateOf<Int?>(null)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var selectedHistory by remember {
        mutableStateOf<HistoryResponse?>(null)
    }

    // =========================================================
    // LOAD HISTORY
    // =========================================================

    LaunchedEffect(refreshKey) {

        isLoading = true
        errorMessage = null

        try {

            val tokenManager =
                TokenManager(context)

            val token =
                tokenManager.getToken()

            if (token.isNullOrBlank()) {

                errorMessage =
                    "Authentication token not found."

                isLoading = false

                return@LaunchedEffect
            }

            val response =
                RetrofitClient.api.getHistory(
                    "Bearer $token"
                )

            if (response.isSuccessful) {

                historyList =
                    response.body()
                        ?: emptyList()

            } else {

                errorMessage =
                    "Failed to load history. " +
                            "HTTP ${response.code()}"
            }

        } catch (e: Exception) {

            errorMessage =
                e.message
                    ?: "Unable to load call history."

        } finally {

            isLoading = false
        }
    }

    // =========================================================
    // DELETE CONFIRMATION DIALOG
    // =========================================================

    if (
        showDeleteDialog &&
        selectedHistory != null
    ) {

        AlertDialog(

            onDismissRequest = {

                if (
                    deletingHistoryId == null
                ) {

                    showDeleteDialog = false
                    selectedHistory = null
                }
            },

            title = {

                Text(
                    text = "Delete Call History?"
                )
            },

            text = {

                Text(
                    text =
                        "This call record will be permanently deleted."
                )
            },

            confirmButton = {

                Button(

                    enabled =
                        deletingHistoryId == null,

                    onClick = {

                        val historyId =
                            selectedHistory?.id
                                ?: return@Button

                        deletingHistoryId =
                            historyId

                        // -------------------------------------------------
                        // Run suspend API call inside Compose coroutine
                        // -------------------------------------------------

                        scope.launch {

                            try {

                                val token =
                                    TokenManager(context)
                                        .getToken()

                                if (
                                    token.isNullOrBlank()
                                ) {

                                    errorMessage =
                                        "Authentication token not found."

                                    return@launch
                                }

                                val response =
                                    RetrofitClient.api.deleteHistory(
                                        token =
                                            "Bearer $token",
                                        historyId =
                                            historyId
                                    )

                                if (
                                    response.isSuccessful
                                ) {

                                    // Remove deleted record
                                    // from visible list.

                                    historyList =
                                        historyList.filter {
                                            it.id != historyId
                                        }

                                    showDeleteDialog =
                                        false

                                    selectedHistory =
                                        null

                                } else {

                                    errorMessage =
                                        "Failed to delete history. " +
                                                "HTTP ${response.code()}"
                                }

                            } catch (e: Exception) {

                                errorMessage =
                                    e.message
                                        ?: "Unable to delete history."

                            } finally {

                                deletingHistoryId =
                                    null
                            }
                        }
                    },

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFD32F2F)
                        )
                ) {

                    if (
                        deletingHistoryId != null
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(18.dp),
                            strokeWidth =
                                2.dp,
                            color =
                                Color.White
                        )

                    } else {

                        Text(
                            text = "Delete"
                        )
                    }
                }
            },

            dismissButton = {

                OutlinedButton(

                    enabled =
                        deletingHistoryId == null,

                    onClick = {

                        showDeleteDialog =
                            false

                        selectedHistory =
                            null
                    }
                ) {

                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }

    // =========================================================
    // MAIN SCREEN
    // =========================================================

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp)
        ) {

            // =================================================
            // HEADER
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick =
                        onBackClick
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowBack,

                        contentDescription =
                            "Back",

                        tint =
                            Color.White
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Icon(
                    imageVector =
                        Icons.Default.History,

                    contentDescription =
                        null,

                    tint =
                        Color(0xFF4DA3FF),

                    modifier =
                        Modifier.size(28.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Column {

                    Text(
                        text =
                            "Call History",

                        color =
                            Color.Black,

                        fontSize =
                            25.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Your latest 10 detections",

                        color =
                            Color(0xFF5F5FF5),

                        fontSize =
                            13.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            // =================================================
            // LOADING
            // =================================================

            if (isLoading) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color =
                            Color(0xFF4DA3FF)
                    )
                }

            }

            // =================================================
            // ERROR
            // =================================================

            else if (errorMessage != null) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text =
                                errorMessage
                                    ?: "Something went wrong.",

                            color =
                                Color(0xFFFF6B6B),

                            fontSize =
                                15.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        OutlinedButton(
                            onClick = {

                                refreshKey++
                            }
                        ) {

                            Text(
                                text =
                                    "Try Again"
                            )
                        }
                    }
                }
            }

            // =================================================
            // EMPTY HISTORY
            // =================================================

            else if (historyList.isEmpty()) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.History,

                            contentDescription =
                                null,

                            tint =
                                Color(0xFF52647A),

                            modifier =
                                Modifier.size(64.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )

                        Text(
                            text =
                                "No call history yet",

                            color =
                                Color.White,

                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Your completed voice detections will appear here.",

                            color =
                                Color(0xFF3F51B5),

                            fontSize =
                                14.sp
                        )
                    }
                }
            }

            // =================================================
            // HISTORY LIST
            // =================================================

            else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    items(
                        items =
                            historyList,

                        key = {
                            it.id
                        }
                    ) { history ->

                        HistoryCard(

                            history =
                                history,

                            onDeleteClick = {

                                selectedHistory =
                                    history

                                showDeleteDialog =
                                    true
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================
// HISTORY CARD
// =============================================================

@Composable
private fun HistoryCard(
    history: HistoryResponse,
    onDeleteClick: () -> Unit
) {

    val isSuspicious =
        history.status.equals(
            "SUSPICIOUS",
            ignoreCase = true
        )

    val statusColor =
        if (isSuspicious) {
            Color(0xFFFF5252)
        } else {
            Color(0xFF4CAF50)
        }

    val displayName =
        history.caller_name
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Unknown Caller"

    val displayNumber =
        history.caller_number
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Number unavailable"

    // =========================================================
    // CARD
    // =========================================================

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF101D30)
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            // =================================================
            // CALLER HEADER
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            displayName,

                        color =
                            Color.White,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Phone,

                            contentDescription =
                                null,

                            tint =
                                Color(0xFF8FA3BA),

                            modifier =
                                Modifier.size(15.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text(
                            text =
                                displayNumber,

                            color =
                                Color(0xFF9AA8BB),

                            fontSize =
                                13.sp
                        )
                    }
                }

                // =================================================
                // DELETE BUTTON
                // =================================================

                IconButton(
                    onClick =
                        onDeleteClick
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Delete",

                        tint =
                            Color(0xFFFF6B6B)
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            // =================================================
            // STATUS
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        history.status,

                    color =
                        statusColor,

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Confidence ${
                            String.format(
                                "%.2f",
                                history.confidence
                            )
                        }%",

                    color =
                        Color.White,

                    fontSize =
                        13.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            // =================================================
            // METRICS
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                MetricItem(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Spoof",

                    value =
                        String.format(
                            "%.2f%%",
                            history.spoof_probability * 100
                        )
                )

                MetricItem(
                    modifier =
                        Modifier.weight(1f),

                    title =
                        "Duration",

                    value =
                        formatDuration(
                            history.duration_seconds
                        )
                )
            }

            // =================================================
            // MESSAGE
            // =================================================

            if (
                !history.message.isNullOrBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        history.message
                            ?: "",

                    color =
                        Color(0xFFB6C3D4),

                    fontSize =
                        13.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            // =================================================
            // DATE
            // =================================================

            Text(
                text =
                    formatDetectedAt(
                        history.detected_at
                    ),

                color =
                    Color(0xFF66788E),

                fontSize =
                    11.sp
            )
        }
    }
}

// =============================================================
// METRIC ITEM
// =============================================================

@Composable
private fun MetricItem(
    modifier: Modifier,
    title: String,
    value: String
) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(12.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFF17263A)
            )
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Text(
                text =
                    title,

                color =
                    Color(0xFF8294A9),

                fontSize =
                    11.sp
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    value,

                color =
                    Color.White,

                fontSize =
                    14.sp,

                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

// =============================================================
// DURATION FORMAT
// =============================================================

private fun formatDuration(
    seconds: Double
): String {

    val totalSeconds =
        seconds.toInt()

    val minutes =
        totalSeconds / 60

    val remainingSeconds =
        totalSeconds % 60

    return if (minutes > 0) {

        String.format(
            "%dm %02ds",
            minutes,
            remainingSeconds
        )

    } else {

        String.format(
            "%ds",
            remainingSeconds
        )
    }
}

// =============================================================
// DATE FORMAT
// =============================================================

private fun formatDetectedAt(
    detectedAt: String
): String {

    return detectedAt
        .replace("T", " ")
        .substringBefore(".")
}