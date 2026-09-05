package com.mapme.core.design.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.mapme.core.design.theme.LocalMapMeContentColor
import com.mapme.core.design.theme.MapMeTheme

/**
 * MapMe's text primitive.
 *
 * Colour resolves in order: the explicit argument, then the style, then the
 * surrounding content colour. That ordering is what lets a whole block — the
 * inside of an accent button, a sheet on a lighter ground — be recoloured in
 * one place instead of at every string.
 */
@Composable
fun MapMeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MapMeTheme.type.bodyMedium,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val inherited = LocalMapMeContentColor.current
    val fallback = MapMeTheme.colors.textPrimary
    val resolved = color
        .takeOrElse { style.color }
        .takeOrElse { inherited }
        .takeOrElse { fallback }

    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = resolved,
            textAlign = textAlign ?: style.textAlign,
        ),
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
    )
}
