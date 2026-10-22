package com.loregate.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HeritageTrail(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val siteIds: List<String>,
    val estimatedMinutes: Int,
    val distanceKm: Double,
    val difficulty: String, // "Mudah", "Sedang", "Menantang"
    val bannerImageUrl: String,
    val badgeLabel: String,
    val themeColorHex: Long = 0xFFE65100
) {
    companion object {
        fun buildDefaultTrails(): List<HeritageTrail> = listOf(
            HeritageTrail(
                id = "trail_singhasari_glory",
                title = "Jejak Wangsa Rajasa",
                subtitle = "Napak Tilas Kejayaan Kerajaan Singhasari",
                description = "Telusuri peninggalan adiluhung tiga candi utama di Malang Raya yang menjadi saksi pergulatan kekuasaan, penyatuan Nusantara, dan bakti anak kepada ibunda di era Singhasari.",
                siteIds = listOf("CANDI_SINGOSARI", "CANDI_JAGO", "CANDI_KIDAL"),
                estimatedMinutes = 210, // ~3.5 jam
                distanceKm = 24.5,
                difficulty = "Sedang",
                bannerImageUrl = "https://images.unsplash.com/photo-1588668214407-6ea9a6d8c272?q=80&w=1000",
                badgeLabel = "Wisata Warisan Utama",
                themeColorHex = 0xFFC2410C
            ),
            HeritageTrail(
                id = "trail_prolog_tumapel",
                title = "Prolog Penjelajah Tumapel",
                subtitle = "Uji Radar Pusaka & Titik Awal Sejarah",
                description = "Rute pendek yang dirancang untuk pengelana pemula. Mulai dari Gerbang Lore Nusantara menuju Candi Singosari sambil melacak tombak-tombak pusaka tersembunyi.",
                siteIds = listOf("GERBANG_LORE_NUSANTARA", "CANDI_SINGOSARI"),
                estimatedMinutes = 75,
                distanceKm = 6.2,
                difficulty = "Mudah",
                bannerImageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1000",
                badgeLabel = "Rute Populer Pemula",
                themeColorHex = 0xFF0D9488
            ),
            HeritageTrail(
                id = "trail_perjuangan_kemerdekaan",
                title = "Jejak Perjuangan Nusantara",
                subtitle = "Dari Gelora Surabaya Hingga Pertempuran Semarang",
                description = "Rute lintas kota mengenang keberanian arek-arek Suroboyo mempertahankan kedaulatan dan pertempuran lima hari pejuang muda di Semarang.",
                siteIds = listOf("TUGU_PAHLAWAN", "LAWANG_SEWU"),
                estimatedMinutes = 240,
                distanceKm = 310.0,
                difficulty = "Ekspedisi",
                bannerImageUrl = "https://images.unsplash.com/photo-1555899434-94d1368aa7af?q=80&w=1000",
                badgeLabel = "Jalur Perjuangan",
                themeColorHex = 0xFFB91C1C
            )
        )
    }
}
