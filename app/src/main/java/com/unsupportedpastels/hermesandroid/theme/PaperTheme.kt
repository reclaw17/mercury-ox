package com.unsupportedpastels.hermesandroid.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import com.unsupportedpastels.hermesandroid.ui.ReadingProfile
import kotlinx.coroutines.launch

internal val LocalReadingProfile = staticCompositionLocalOf { ReadingProfile.Standard }

/**
 * Focus draws a 2 dp black ring. Press does not ripple or fade; button chrome
 * owns the black/white swap.
 */
internal object PaperFocusIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PaperFocusIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = 2
}

private class PaperFocusIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private var focused by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            val focuses = mutableListOf<FocusInteraction.Focus>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> focuses.add(interaction)
                    is FocusInteraction.Unfocus -> focuses.remove(interaction.focus)
                }
                focused = focuses.isNotEmpty()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (focused) {
            drawRect(
                color = PaperBlack,
                style = Stroke(width = PaperFocusRing.toPx()),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaperTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHermesSemanticColors provides PaperSemanticColors) {
        MaterialTheme(
            colorScheme = PaperColors,
            typography = PaperTypography,
        ) {
            CompositionLocalProvider(
                LocalReadingProfile provides ReadingProfile.Paper,
                LocalRippleConfiguration provides null,
                LocalIndication provides PaperFocusIndication,
                content = content,
            )
        }
    }
}

@Composable
internal fun PaperOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        val presses = mutableListOf<PressInteraction.Press>()
        val focuses = mutableListOf<FocusInteraction.Focus>()
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> presses.add(interaction)
                is PressInteraction.Release -> presses.remove(interaction.press)
                is PressInteraction.Cancel -> presses.remove(interaction.press)
                is FocusInteraction.Focus -> focuses.add(interaction)
                is FocusInteraction.Unfocus -> focuses.remove(interaction.focus)
            }
            pressed = presses.isNotEmpty()
            focused = focuses.isNotEmpty()
        }
    }
    val chrome = paperButtonChrome(
        enabled = enabled,
        pressed = pressed,
        focused = focused,
        selected = selected,
    )
    val shape = RoundedCornerShape(0.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .border(width = chrome.outlineWidth, color = chrome.outline, shape = shape)
            .background(chrome.container, shape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            CompositionLocalProvider(LocalContentColor provides chrome.label) {
                content()
            }
        },
    )
}
