package com.example.data.model

enum class LanguageMode(
    val title: String,
    val subtitle: String,
    val speechLocaleTag: String,
    val promptInstruction: String
) {
    AUTO(
        title = "Auto Detect",
        subtitle = "English, Urdu & Roman Urdu",
        speechLocaleTag = "ur-PK",
        promptInstruction = "Detect whether the user is speaking Urdu (اردو), Roman Urdu (e.g., 'Aap kaise hain'), or English. Respond in the exact same language or dialect they used. Keep spoken answers concise, expressive, and warm."
    ),
    URDU(
        title = "Urdu (اردو)",
        subtitle = "اردو میں بات چیت اور جواب",
        speechLocaleTag = "ur-PK",
        promptInstruction = "The user prefers Urdu. Respond primarily in fluent, polite Urdu (اردو رسم الخط). Keep answers natural for speech synthesis."
    ),
    ROMAN_URDU(
        title = "Roman Urdu",
        subtitle = "Roman Urdu mein baat karein",
        speechLocaleTag = "ur-PK",
        promptInstruction = "The user prefers Roman Urdu (Urdu written in Latin English alphabet, such as 'Assalam-o-Alaikum, main theek hoon, aap bataiye'). Always respond in clear, natural Roman Urdu that sounds natural when read aloud."
    ),
    ENGLISH(
        title = "English",
        subtitle = "English conversations",
        speechLocaleTag = "en-US",
        promptInstruction = "The user prefers English. Respond in polite, friendly, and articulate English."
    )
}
