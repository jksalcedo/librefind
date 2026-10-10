package com.jksalcedo.librefind.ui.common

import android.widget.TextView
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlDescription(htmlText: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    val textStyle = LocalTextStyle.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                // Makes <a href="..."> links clickable!
                movementMethod = android.text.method.LinkMovementMethod.getInstance()

                includeFontPadding = false
            }
        },
        update = { textView ->
            // Apply the Compose ARGB colors to the native view
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)

            // Apply to Compose font size
            if (textStyle.fontSize.isSp) {
                textView.textSize = textStyle.fontSize.value
            }

            val formattedHtml = htmlText.replace("\n", "<br>")

            // Parse the HTML
            textView.text = HtmlCompat.fromHtml(
                formattedHtml,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }
    )
}
