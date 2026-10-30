package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class GameState {
    TITLE,
    PLAYING,
    GAME_OVER
}

enum class ActionType {
    PAY,      // Urgent unpaid bill / notice with pending fee
    ARCHIVE,  // Paid receipt, warranty, standard statement
    FRAUD     // Phishing scam, fake IRS wire, suspicious lottery
}

data class AuditDocument(
    val id: Int,
    val title: String,
    val organization: String,
    val amountText: String,
    val dueDateText: String,
    val snippet: String,
    val warningClue: String?,
    val correctAction: ActionType,
    val explanation: String
)

private val GAME_DOCUMENTS_POOL = listOf(
    AuditDocument(
        id = 1,
        title = "Past Due Electric Service Notice",
        organization = "Pacific Gas & Electric",
        amountText = "$148.20",
        dueDateText = "DUE IN 2 DAYS",
        snippet = "Final billing balance before disconnection. Please remit payment immediately.",
        warningClue = null,
        correctAction = ActionType.PAY,
        explanation = "Urgent utility bill past due! Must PAY immediately to prevent shutoff."
    ),
    AuditDocument(
        id = 2,
        title = "WIRE $5,000 BITCOIN IMMEDIATELY",
        organization = "FEDERAL TAX ARREST UNIT",
        amountText = "$5,000.00",
        dueDateText = "IMMEDIATE ARREST",
        snippet = "Warrant issued for your arrest. Send cryptocurrency or Apple gift cards in 1 hour.",
        warningClue = "Demands Bitcoin & gift cards. Fake tax authority arrest threat.",
        correctAction = ActionType.FRAUD,
        explanation = "IRS/Tax agencies NEVER demand gift cards, crypto, or threaten immediate arrest!"
    ),
    AuditDocument(
        id = 3,
        title = "Grocery Store Receipt (PAID)",
        organization = "Trader Joe's Market",
        amountText = "$42.15 [PAID CASH]",
        dueDateText = "COMPLETED",
        snippet = "Itemized grocery receipt. Payment received in full via Cashier #4.",
        warningClue = null,
        correctAction = ActionType.ARCHIVE,
        explanation = "Already paid receipt. File and ARCHIVE for budget tracking."
    ),
    AuditDocument(
        id = 4,
        title = "Urgent Property Tax Installment",
        organization = "County Tax Collector",
        amountText = "$1,120.00",
        dueDateText = "DUE TOMORROW",
        snippet = "2nd Installment secured property tax. 10% penalty applies if late.",
        warningClue = null,
        correctAction = ActionType.PAY,
        explanation = "Real property tax with strict penalty deadline. Must PAY."
    ),
    AuditDocument(
        id = 5,
        title = "You Won $2,500,000 Lottery!",
        organization = "International Mega Sweepstakes",
        amountText = "$2,500,000.00",
        dueDateText = "CLAIM TODAY",
        snippet = "Send $350 processing fee via Western Union to claim your multimillion jackpot.",
        warningClue = "Advance fee scam: asking you to pay money to receive winnings.",
        correctAction = ActionType.FRAUD,
        explanation = "Classic advance fee fraud! Legitimate lotteries never ask for upfront transfer fees."
    ),
    AuditDocument(
        id = 6,
        title = "Monthly Fiber Internet Statement",
        organization = "Sonic Telecom",
        amountText = "$60.00",
        dueDateText = "DUE IN 3 DAYS",
        snippet = "Monthly gigabit internet service invoice. Auto-pay disabled.",
        warningClue = null,
        correctAction = ActionType.PAY,
        explanation = "Valid telecommunications bill without autopay. Must PAY."
    ),
    AuditDocument(
        id = 7,
        title = "Annual Health Insurance Card & Proof",
        organization = "Blue Shield California",
        amountText = "$0.00 DUE",
        dueDateText = "ACTIVE POLICY",
        snippet = "Your updated insurance member ID card and policy coverage summary.",
        warningClue = null,
        correctAction = ActionType.ARCHIVE,
        explanation = "Active insurance card / statement with no amount due. ARCHIVE."
    ),
    AuditDocument(
        id = 8,
        title = "Suspicious Overdue Domain Invoice",
        organization = "Domain SEO Registry Inc",
        amountText = "$289.00",
        dueDateText = "EXPIRES IN 24H",
        snippet = "This is a solicitation, not a real bill. However it looks like a renewal demand.",
        warningClue = "Hidden disclaimer in tiny print: 'This is a solicitation'.",
        correctAction = ActionType.FRAUD,
        explanation = "Domain slamming scam disguised as an essential renewal invoice."
    ),
    AuditDocument(
        id = 9,
        title = "Vehicle Registration Renewal Notice",
        organization = "Department of Motor Vehicles",
        amountText = "$246.00",
        dueDateText = "DUE IN 5 DAYS",
        snippet = "State vehicle license fee and registration renewal for 2026.",
        warningClue = null,
        correctAction = ActionType.PAY,
        explanation = "Official DMV registration renewal fee. Must PAY to avoid penalties."
    ),
    AuditDocument(
        id = 10,
        title = "Refrigerator Warranty Certificate",
        organization = "Samsung Electronics",
        amountText = "PAID IN FULL",
        dueDateText = "10 YEAR WARRANTY",
        snippet = "Warranty registration receipt and proof of purchase.",
        warningClue = null,
        correctAction = ActionType.ARCHIVE,
        explanation = "Appliance warranty certificate with 0 balance. ARCHIVE for safekeeping."
    )
)

data class FloatingScore(
    val id: Long,
    val text: String,
    val color: Color,
    val xOffset: Float
)

@Composable
fun GameScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("papertrail_game", Context.MODE_PRIVATE) }

    var gameState by remember { mutableStateOf(GameState.TITLE) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(prefs.getInt("high_score", 0)) }
    var lives by remember { mutableIntStateOf(3) }
    var streak by remember { mutableIntStateOf(0) }
    var maxStreak by remember { mutableIntStateOf(0) }
    var documentsAudited by remember { mutableIntStateOf(0) }
    var correctAudits by remember { mutableIntStateOf(0) }

    // Current document & timer state
    var currentDocIndex by remember { mutableIntStateOf(0) }
    var shuffledDeck by remember { mutableStateOf(GAME_DOCUMENTS_POOL.shuffled()) }
    val activeDocument = shuffledDeck.getOrNull(currentDocIndex % shuffledDeck.size) ?: GAME_DOCUMENTS_POOL.first()

    var timeRemaining by remember { mutableFloatStateOf(6.0f) }
    var maxTimeForDoc by remember { mutableFloatStateOf(6.0f) }

    // Animation / juice states
    val shakeOffset = remember { Animatable(0f) }
    val cardDragOffset = remember { Animatable(0f) }
    val cardScale = remember { Animatable(1f) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isHitFlash by remember { mutableStateOf(false) }

    val floatingScores = remember { mutableStateListOf<FloatingScore>() }

    // Haptics helper
    fun triggerHaptic(isError: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (isError) {
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isError) 100L else 30L)
            }
        } catch (_: Exception) { }
    }

    // Process player triage action
    fun handleAction(action: ActionType) {
        if (gameState != GameState.PLAYING) return

        val isCorrect = action == activeDocument.correctAction
        documentsAudited++

        if (isCorrect) {
            correctAudits++
            streak++
            if (streak > maxStreak) maxStreak = streak
            val multiplier = when {
                streak >= 5 -> 3
                streak >= 3 -> 2
                else -> 1
            }
            val points = 100 * multiplier + (timeRemaining * 10).toInt()
            score += points
            if (score > highScore) {
                highScore = score
                prefs.edit().putInt("high_score", highScore).apply()
            }

            triggerHaptic(isError = false)
            feedbackMessage = "CORRECT! +$points ${if (multiplier > 1) "(x$multiplier)" else ""}"

            // Add floating score popup
            val popup = FloatingScore(
                id = System.currentTimeMillis(),
                text = "+$points",
                color = Color(0xFF10B981),
                xOffset = (Math.random() * 40 - 20).toFloat()
            )
            floatingScores.add(popup)
            coroutineScope.launch {
                delay(800)
                floatingScores.remove(popup)
            }
        } else {
            lives--
            streak = 0
            isHitFlash = true
            triggerHaptic(isError = true)
            feedbackMessage = "WRONG! ${activeDocument.explanation}"

            coroutineScope.launch {
                shakeOffset.snapTo(0f)
                shakeOffset.animateTo(24f, tween(40))
                shakeOffset.animateTo(-24f, tween(40))
                shakeOffset.animateTo(16f, tween(40))
                shakeOffset.animateTo(-16f, tween(40))
                shakeOffset.animateTo(0f, tween(40))
                delay(300)
                isHitFlash = false
            }

            if (lives <= 0) {
                gameState = GameState.GAME_OVER
                return
            }
        }

        // Animate card dismissal and load next
        coroutineScope.launch {
            val direction = when (action) {
                ActionType.FRAUD -> -500f
                ActionType.PAY -> 500f
                ActionType.ARCHIVE -> 0f
            }
            cardDragOffset.animateTo(direction, tween(150))
            cardScale.animateTo(0.8f, tween(150))

            // Load next document
            currentDocIndex++
            if (currentDocIndex % shuffledDeck.size == 0) {
                shuffledDeck = GAME_DOCUMENTS_POOL.shuffled()
            }

            // Ramp up speed slightly with progress
            val newMaxTime = (6.0f - (documentsAudited * 0.15f)).coerceAtLeast(3.2f)
            maxTimeForDoc = newMaxTime
            timeRemaining = newMaxTime

            cardDragOffset.snapTo(0f)
            cardScale.snapTo(1f)
        }
    }

    // 60fps Game loop with time delta clamping
    LaunchedEffect(gameState, currentDocIndex) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect

        var lastFrameTime = 0L
        while (gameState == GameState.PLAYING && timeRemaining > 0f) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val rawDt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                    val dt = rawDt.coerceIn(0.001f, 0.05f) // clamped delta time
                    timeRemaining = (timeRemaining - dt).coerceAtLeast(0f)
                }
                lastFrameTime = frameTimeNanos
            }
        }

        // Timer ran out! Strike penalty!
        if (timeRemaining <= 0f && gameState == GameState.PLAYING) {
            lives--
            streak = 0
            isHitFlash = true
            triggerHaptic(isError = true)
            feedbackMessage = "TIME EXPIRED! ${activeDocument.explanation}"

            coroutineScope.launch {
                delay(300)
                isHitFlash = false
            }

            if (lives <= 0) {
                gameState = GameState.GAME_OVER
            } else {
                currentDocIndex++
                timeRemaining = maxTimeForDoc
            }
        }
    }

    // Flash background color when hit
    val hitColor by animateColorAsState(
        targetValue = if (isHitFlash) Color(0xFFEF4444).copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(150),
        label = "hit_flash"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .background(hitColor)
            .testTag("game_screen")
    ) {
        when (gameState) {
            GameState.TITLE -> {
                GameTitleView(
                    highScore = highScore,
                    onStartClick = {
                        score = 0
                        lives = 3
                        streak = 0
                        maxStreak = 0
                        documentsAudited = 0
                        correctAudits = 0
                        currentDocIndex = 0
                        maxTimeForDoc = 6.0f
                        timeRemaining = 6.0f
                        shuffledDeck = GAME_DOCUMENTS_POOL.shuffled()
                        gameState = GameState.PLAYING
                    },
                    onBackClick = onBackClick
                )
            }

            GameState.PLAYING -> {
                GamePlayingView(
                    score = score,
                    highScore = highScore,
                    lives = lives,
                    streak = streak,
                    timeRemaining = timeRemaining,
                    maxTime = maxTimeForDoc,
                    document = activeDocument,
                    feedbackMessage = feedbackMessage,
                    cardDragOffset = cardDragOffset.value,
                    cardScale = cardScale.value,
                    floatingScores = floatingScores,
                    onAction = { handleAction(it) },
                    onDrag = { delta ->
                        coroutineScope.launch {
                            cardDragOffset.snapTo(cardDragOffset.value + delta)
                        }
                    },
                    onDragEnd = {
                        if (cardDragOffset.value > 120f) {
                            handleAction(ActionType.PAY)
                        } else if (cardDragOffset.value < -120f) {
                            handleAction(ActionType.FRAUD)
                        } else {
                            coroutineScope.launch {
                                cardDragOffset.animateTo(0f, tween(150))
                            }
                        }
                    },
                    onExitClick = { gameState = GameState.GAME_OVER }
                )
            }

            GameState.GAME_OVER -> {
                GameOverView(
                    finalScore = score,
                    highScore = highScore,
                    documentsAudited = documentsAudited,
                    correctAudits = correctAudits,
                    maxStreak = maxStreak,
                    onPlayAgainClick = {
                        score = 0
                        lives = 3
                        streak = 0
                        maxStreak = 0
                        documentsAudited = 0
                        correctAudits = 0
                        currentDocIndex = 0
                        maxTimeForDoc = 6.0f
                        timeRemaining = 6.0f
                        shuffledDeck = GAME_DOCUMENTS_POOL.shuffled()
                        gameState = GameState.PLAYING
                    },
                    onExitClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun GameTitleView(
    highScore: Int,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("game_back_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Game")
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST: $highScore",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Center Hero Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF4F46E5)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DEADLINE RUSH",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "DOCUMENT AUDITOR CHALLENGE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HOW TO PLAY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RuleRow(color = Color(0xFF10B981), icon = Icons.Default.Payment, label = "PAY NOW", desc = "Overdue utility bills, taxes, urgent notices")
                    Spacer(modifier = Modifier.height(6.dp))
                    RuleRow(color = Color(0xFF3B82F6), icon = Icons.Default.Archive, label = "ARCHIVE", desc = "Paid receipts, warranties, clean statements")
                    Spacer(modifier = Modifier.height(6.dp))
                    RuleRow(color = Color(0xFFEF4444), icon = Icons.Default.Security, label = "FLAG FRAUD", desc = "Wire scams, gift card demands, fake arrests")
                }
            }
        }

        // Bottom Start Action
        Button(
            onClick = onStartClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_game_button")
        ) {
            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START AUDIT CHALLENGE",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun RuleRow(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.2f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GamePlayingView(
    score: Int,
    highScore: Int,
    lives: Int,
    streak: Int,
    timeRemaining: Float,
    maxTime: Float,
    document: AuditDocument,
    feedbackMessage: String?,
    cardDragOffset: Float,
    cardScale: Float,
    floatingScores: List<FloatingScore>,
    onAction: (ActionType) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onExitClick: () -> Unit
) {
    val progress = (timeRemaining / maxTime).coerceIn(0f, 1f)
    val progressColor = when {
        progress < 0.25f -> Color(0xFFEF4444)
        progress < 0.5f -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD: Score, Combo Multiplier, Lives
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Box
                Column {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%05d", score),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Combo Badge
                if (streak >= 2) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "${when {
                                streak >= 5 -> "x3 COMBO!"
                                streak >= 3 -> "x2 COMBO!"
                                else -> "STREAK $streak"
                            }}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                // Lives Hearts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = if (i <= lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (i <= lives) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Countdown Deadline Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AUDIT TIMER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                    Text(
                        text = String.format("%.1fs", timeRemaining),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = progressColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // CENTER: Interactive Document Card with Swipe Gesture
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(cardDragOffset.roundToInt(), 0) }
                    .rotate((cardDragOffset / 20f).coerceIn(-15f, 15f))
                    .scale(cardScale)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount -> onDrag(dragAmount) },
                            onDragEnd = onDragEnd
                        )
                    }
                    .testTag("game_active_document_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    when {
                        cardDragOffset > 50f -> Color(0xFF10B981)
                        cardDragOffset < -50f -> Color(0xFFEF4444)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Document Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = document.organization.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (document.dueDateText.contains("DUE") || document.dueDateText.contains("IMMEDIATE")) {
                                Color(0xFFEF4444).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = document.dueDateText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (document.dueDateText.contains("DUE") || document.dueDateText.contains("IMMEDIATE")) {
                                    Color(0xFFEF4444)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "AMOUNT: ${document.amountText}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "\"${document.snippet}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (document.warningClue != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = document.warningClue,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swipe Cue Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⟵ Swipe Left: FRAUD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Swipe Right: PAY ⟶",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Floating score popups
            floatingScores.forEach { popup ->
                Text(
                    text = popup.text,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = popup.color,
                    modifier = Modifier.offset(x = popup.xOffset.dp, y = (-40).dp)
                )
            }
        }

        // Feedback / Result banner
        if (feedbackMessage != null) {
            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = if (feedbackMessage.startsWith("CORRECT")) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }

        // BOTTOM: Tactile Triage Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Flag Fraud Button
            Button(
                onClick = { onAction(ActionType.FRAUD) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("action_fraud_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("FRAUD", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            // Archive Button
            Button(
                onClick = { onAction(ActionType.ARCHIVE) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("action_archive_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("ARCHIVE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            // Pay Now Button
            Button(
                onClick = { onAction(ActionType.PAY) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("action_pay_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("PAY NOW", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GameOverView(
    finalScore: Int,
    highScore: Int,
    documentsAudited: Int,
    correctAudits: Int,
    maxStreak: Int,
    onPlayAgainClick: () -> Unit,
    onExitClick: () -> Unit
) {
    val accuracy = if (documentsAudited > 0) {
        (correctAudits.toFloat() / documentsAudited.toFloat() * 100f).roundToInt()
    } else 0

    val isNewHighScore = finalScore > 0 && finalScore >= highScore

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (isNewHighScore) Color(0xFFF59E0B).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isNewHighScore) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isNewHighScore) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isNewHighScore) "NEW RECORD!" else "AUDIT COMPLETE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = if (isNewHighScore) Color(0xFFD97706) else MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "FINAL SCORE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "$finalScore",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                GameOverStatRow("Best Record", "$highScore PTS")
                Spacer(modifier = Modifier.height(8.dp))
                GameOverStatRow("Audited Documents", "$documentsAudited")
                Spacer(modifier = Modifier.height(8.dp))
                GameOverStatRow("Accuracy Rate", "$accuracy%")
                Spacer(modifier = Modifier.height(8.dp))
                GameOverStatRow("Highest Streak", "$maxStreak in a row")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onPlayAgainClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("play_again_button")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PLAY AGAIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onExitClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("exit_game_button")
        ) {
            Text("RETURN TO DASHBOARD", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GameOverStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}
