package com.example.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Modifier extension to make text inside any button or compact container
 * automatically marquee scroll instead of stretching or enlarging the button.
 */
fun Modifier.buttonMarquee(
    iterations: Int = Int.MAX_VALUE,
    initialDelayMillis: Int = 1000,
    repeatDelayMillis: Int = 800,
    velocity: Dp = 35.dp
): Modifier = this.basicMarquee(
    iterations = iterations,
    initialDelayMillis = initialDelayMillis,
    repeatDelayMillis = repeatDelayMillis,
    velocity = velocity
)

/**
 * A Text composable specifically designed for Buttons, Tabs, and Action Chips.
 * It prevents the button from enlarging or wrapping onto multiple lines, ensuring
 * the UI remains predictable and neat. When text overflows, it smoothly marquee scrolls.
 */
@Composable
fun ScrollableButtonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.buttonMarquee(),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
