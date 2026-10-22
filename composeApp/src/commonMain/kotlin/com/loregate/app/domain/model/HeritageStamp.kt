package com.loregate.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HeritageStamp(
    val id: String,
    val siteId: String,
    val siteName: String,
    val culturalTitle: String,
    val location: String,
    val sealEmoji: String,
    val inkHexColor: Long,
    val description: String,
    val historicalQuote: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null
) {
    companion object {
        fun buildDefaultStamps(): List<HeritageStamp> = listOf(
            HeritageStamp(
                id = "stamp_singosari",
                siteId = "CANDI_SINGOSARI",
                siteName = "Candi Singosari",
                culturalTitle = "Penjaga Mandala Singhasari",
                location = "Singosari, Malang",
                sealEmoji = "🛕",
                inkHexColor = 0xFFB71C1C, // Crimson Royal Red
                description = "Stempel kehormatan atas penjelajahan di pusat pendharmaan Maharaja Kertanegara, pencetus wawasan persatuan Nusantara.",
                historicalQuote = "Bersatu menjaga cakrawala Nusantara dari ancaman luar."
            ),
            HeritageStamp(
                id = "stamp_jago",
                siteId = "CANDI_JAGO",
                siteName = "Candi Jago",
                culturalTitle = "Pemulia Tirta Amoghapasa",
                location = "Tumpang, Malang",
                sealEmoji = "🪷",
                inkHexColor = 0xFFC5A059, // Royal Golden Ochre
                description = "Stempel kebajikan punden berundak Tantrayana tempat pemuliaan Raja Wisnuwardhana.",
                historicalQuote = "Yang mulia bagaikan tempat suci puncak kebajikan."
            ),
            HeritageStamp(
                id = "stamp_kidal",
                siteId = "CANDI_KIDAL",
                siteName = "Candi Kidal",
                culturalTitle = "Penebus Tirta Garudeya",
                location = "Pakis, Malang",
                sealEmoji = "🦅",
                inkHexColor = 0xFF1B5E20, // Sacred Forest Emerald
                description = "Stempel lambang bakti tanpa pamrih Sang Garudeya membebaskan ibunda dari perbudakan.",
                historicalQuote = "Bakti seorang putra adalah jalan menuju keabadian."
            ),
            HeritageStamp(
                id = "stamp_tugu_pahlawan",
                siteId = "TUGU_PAHLAWAN",
                siteName = "Tugu Pahlawan Surabaya",
                culturalTitle = "Banteng Merdeka Suroboyo",
                location = "Surabaya, Jawa Timur",
                sealEmoji = "⚔️",
                inkHexColor = 0xFFD32F2F, // Valor Red
                description = "Stempel keteguhan arek-arek Suroboyo mempertahankan kedaulatan tanah air.",
                historicalQuote = "Merdeka atau mati! Kedaulatan tidak pernah diperjualbelikan."
            ),
            HeritageStamp(
                id = "stamp_lawang_sewu",
                siteId = "LAWANG_SEWU",
                siteName = "Lawang Sewu",
                culturalTitle = "Penjaga Lintasan Sejarah",
                location = "Semarang, Jawa Tengah",
                sealEmoji = "🏛️",
                inkHexColor = 0xFF283593, // Classical Indigo Navy
                description = "Stempel memori arsitektur seribu pintu dan perjuangan pertempuran lima hari.",
                historicalQuote = "Jejak roda sejarah berputar melewati lorong-lorong waktu."
            ),
            HeritageStamp(
                id = "stamp_gerbang_lore",
                siteId = "GERBANG_LORE_NUSANTARA",
                siteName = "Gerbang Lore Nusantara",
                culturalTitle = "Pengelana Cakrawala",
                location = "Titik Awal Penjelajah",
                sealEmoji = "🧭",
                inkHexColor = 0xFFE65100, // Adventurer Orange
                description = "Stempel inisiasi petualang waktu yang membuktikan langkah pertamamu menjelajahi jejak masa lalu.",
                historicalQuote = "Setiap langkah di masa kini menyambung kisah di masa lampau."
            )
        )
    }
}
