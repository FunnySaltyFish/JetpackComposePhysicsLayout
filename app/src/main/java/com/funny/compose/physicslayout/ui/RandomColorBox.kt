package com.funny.compose.physicslayout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.random.Random

fun randomColor() = Color(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))

@Composable
fun rememberRandomColor() = rememberSaveable(
    saver = Saver(
        save = { value ->
            value.toArgb()
        },
        restore = {
            Color(it)
        }
    )
) {
    randomColor()
}

@Composable
fun RandomColorBox(modifier: Modifier) {
    Box(modifier = modifier.background(rememberRandomColor()))
}