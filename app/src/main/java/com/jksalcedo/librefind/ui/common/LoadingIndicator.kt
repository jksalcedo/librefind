package com.jksalcedo.librefind.ui.common

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun LibreFindLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Int = 64
) {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.avd_anim)
    var atEnd by remember { mutableStateOf(false) }

    // Check system theme and define the color
    val isDark = isSystemInDarkTheme()
    val tintColor = if (isDark) Color.White else Color.Black

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            atEnd = !atEnd
        }
    }

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAnimatedVectorPainter(image, atEnd),
            contentDescription = "Loading...",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(tintColor) // Dynamically tints the vector
        )
    }
}

@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LibreFindLoadingIndicator()
    }
}