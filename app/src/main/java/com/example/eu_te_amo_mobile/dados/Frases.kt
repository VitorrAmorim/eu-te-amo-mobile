package com.example.eu_te_amo_mobile.dados

data class Frase(
    val texto: String,
    val idioma: String,
)

const val FRASE_CENTRAL = "Eu te amo"

val LISTA_FRASES = listOf(
    Frase("I love you", "inglês"),
    Frase("Te quiero", "espanhol"),
    Frase("Je t'aime", "francês"),
    Frase("Ich liebe dich", "alemão"),
    Frase("Ti amo", "italiano"),
    Frase("Я тебя люблю", "russo"),
    Frase("Я тебе кохаю", "ucraniano"),
    Frase("사랑해", "coreano"),
    Frase("愛してる", "japonês"),
    Frase("我爱你", "chinês simplificado"),
    Frase("मुझे तुमसे प्यार है", "hindi"),
    Frase("Σ' αγαπώ", "grego"),
    Frase("Ik hou van jou", "holandês"),
    Frase("Jag älskar dig", "sueco"),
    Frase("Kocham cię", "polonês"),
    Frase("Seni seviyorum", "turco"),
    Frase("ฉันรักเธอ", "tailandês"),
    Frase("Anh yêu em", "vietnamita"),
    Frase("Aku cinta kamu", "indonésio"),
    Frase("Szeretlek", "húngaro"),
    Frase("Rakastan sinua", "finlandês"),
    Frase("Nakupenda", "suaíli"),
)
