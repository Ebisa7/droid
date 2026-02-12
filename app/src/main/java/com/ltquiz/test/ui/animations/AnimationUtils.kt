package com.ltquiz.test.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AnimationUtils {
    // Standard durations
    const val FAST_ANIMATION = 200
    const val MEDIUM_ANIMATION = 300
    const val SLOW_ANIMATION = 500
    
    // Easing curves
    val FastOutSlowInEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val LinearOutSlowInEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val FastOutLinearInEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    
    // Screen transition animations
    fun slideInFromRight(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(MEDIUM_ANIMATION, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(MEDIUM_ANIMATION)
        )
    }
    
    fun slideOutToLeft(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(MEDIUM_ANIMATION, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(MEDIUM_ANIMATION)
        )
    }
    
    fun slideInFromLeft(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(MEDIUM_ANIMATION, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(MEDIUM_ANIMATION)
        )
    }
    
    fun slideOutToRight(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(MEDIUM_ANIMATION, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(MEDIUM_ANIMATION)
        )
    }
    
    fun fadeInTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(MEDIUM_ANIMATION, easing = LinearOutSlowInEasing)
        )
    }
    
    fun fadeOutTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(FAST_ANIMATION, easing = FastOutLinearInEasing)
        )
    }
    
    fun scaleInTransition(): EnterTransition {
        return scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(MEDIUM_ANIMATION, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(MEDIUM_ANIMATION)
        )
    }
    
    fun scaleOutTransition(): ExitTransition {
        return scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(FAST_ANIMATION, easing = FastOutLinearInEasing)
        ) + fadeOut(
            animationSpec = tween(FAST_ANIMATION)
        )
    }
}

@Composable
fun AnimatedVisibilityScope.FadeInContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .animateEnterExit(
                enter = AnimationUtils.fadeInTransition(),
                exit = AnimationUtils.fadeOutTransition()
            )
    ) {
        content()
    }
}

@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    content(scale)
}

@Composable
fun ShimmerAnimation(
    modifier: Modifier = Modifier,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    content(alpha)
}

@Composable
fun BounceAnimation(
    modifier: Modifier = Modifier,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    
    content(offsetY)
}