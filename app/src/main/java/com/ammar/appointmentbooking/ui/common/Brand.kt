package com.ammar.appointmentbooking.ui.common

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ammar.appointmentbooking.ui.theme.BrandColors

/**
 * The BookEase mark: a calendar page with a check, on the brand gradient.
 * Drawn in code so it stays sharp at any size and needs no image files.
 */
@Composable
fun BrandLogo(size: Dp = 64.dp, modifier: Modifier = Modifier, onGradient: Boolean = false) {
    Box(
        modifier
            .size(size)
            .shadow(if (onGradient) 0.dp else size / 6, RoundedCornerShape(size * 0.28f))
            .clip(RoundedCornerShape(size * 0.28f))
            .then(
                if (onGradient) Modifier.background(Color.White.copy(alpha = 0.18f))
                else Modifier.background(BrandColors.gradient)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.6f)) {
            val w = this.size.width
            val stroke = w * 0.075f
            // Page
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.06f, w * 0.14f),
                size = Size(w * 0.88f, w * 0.80f),
                cornerRadius = CornerRadius(w * 0.14f),
            )
            // Header band
            drawRoundRect(
                color = BrandColors.Teal,
                topLeft = Offset(w * 0.06f, w * 0.14f),
                size = Size(w * 0.88f, w * 0.22f),
                cornerRadius = CornerRadius(w * 0.14f),
            )
            drawRect(BrandColors.Teal, Offset(w * 0.06f, w * 0.26f), Size(w * 0.88f, w * 0.10f))
            // Rings
            listOf(0.30f, 0.70f).forEach { x ->
                drawLine(Color.White, Offset(w * x, w * 0.04f), Offset(w * x, w * 0.24f), stroke, StrokeCap.Round)
            }
            // Check
            val check = Path().apply {
                moveTo(w * 0.30f, w * 0.62f)
                lineTo(w * 0.45f, w * 0.76f)
                lineTo(w * 0.72f, w * 0.48f)
            }
            drawPath(check, BrandColors.Indigo, style = Stroke(stroke * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Logo plus the "BookEase" word mark. */
@Composable
fun BrandLockup(light: Boolean, modifier: Modifier = Modifier, logoSize: Dp = 40.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BrandLogo(logoSize, onGradient = light)
        Spacer(Modifier.width(10.dp))
        Text(
            "BookEase",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (light) Color.White else MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Gradient banner with soft decorative circles, used at the top of key screens. */
@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    bottomRadius: Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = bottomRadius, bottomEnd = bottomRadius))
            .background(BrandColors.gradient)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.45f, center = Offset(size.width * 1.02f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * 0.28f, center = Offset(size.width * 0.05f, size.height * 1.05f))
            drawCircle(BrandColors.Teal.copy(alpha = 0.25f), radius = size.width * 0.06f, center = Offset(size.width * 0.82f, size.height * 0.78f))
        }
        content()
    }
}

/** White status-bar icons while this screen (with a dark gradient header) is shown. */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        var ctx = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        val window = (ctx as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
}

/** Round coloured badge with a letter, used as a service or staff avatar. */
@Composable
fun InitialAvatar(text: String, tint: Color, size: Dp = 48.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.trim().take(1).uppercase().ifEmpty { "•" },
            color = tint,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Square shortcut tile with an icon in a tinted circle. */
@Composable
fun QuickAction(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = tint) }
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Shown while the signed-in profile loads: logo on the brand gradient. */
@Composable
fun BrandSplash() {
    LightStatusBarIcons()
    Box(
        Modifier.fillMaxSize().background(BrandColors.gradient),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLogo(96.dp, onGradient = true)
            Spacer(Modifier.height(18.dp))
            Text("BookEase", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}
