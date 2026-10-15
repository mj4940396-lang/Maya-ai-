package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MayaCardBg
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.TextPrimaryDark

data class QuickPrompt(
    val label: String,
    val text: String,
    val tag: String
)

val DEFAULT_PROMPTS = listOf(
    QuickPrompt("👋 Salam", "Assalam-o-Alaikum Maya! Aap kaisi hain?", "Urdu"),
    QuickPrompt("📖 Urdu Kahani", "Mujhe Urdu mein ek choti aur sabak aamoz kahani sunao.", "Urdu"),
    QuickPrompt("💡 Roman Urdu", "Roman Urdu me daily productivity tips batao.", "Roman Urdu"),
    QuickPrompt("✨ What can you do?", "What are all the things you can help me with?", "English"),
    QuickPrompt("🌙 Poetry / Shayari", "Mirza Ghalib ya Iqbal ka ek khoobsurat sher sunao.", "Urdu"),
    QuickPrompt("🎙️ Practice English", "Can we practice spoken English conversation together?", "English")
)

@Composable
fun QuickPrompts(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quick_prompts_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DEFAULT_PROMPTS.forEachIndexed { index, prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MayaCardBg)
                    .border(1.dp, MayaCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = MayaCyan)
                    ) {
                        onPromptSelected(prompt.text)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("prompt_chip_$index")
            ) {
                Text(
                    text = prompt.label,
                    color = TextPrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
