package com.loregate.app.data.seeder

import dev.gitlive.firebase.firestore.FirebaseFirestore
import com.loregate.app.domain.model.ChapterContent
import com.loregate.app.domain.model.ChapterSection
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.KeyFigure
import com.loregate.app.domain.model.Quote
import com.loregate.app.domain.model.SiteType
import com.loregate.app.domain.model.Spear
import com.loregate.app.domain.model.TimelineEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FirestoreDataSeeder(
    private val firestore: FirebaseFirestore
) {

    suspend fun seedInitialData(userLocation: GeoPoint? = null): Result<Unit> = runCatching {
        val sites = buildInitialSites(userLocation)
        val chapters = buildInitialChapters(sites)
        val spears = buildInitialSpears(userLocation)

        val batch = firestore.batch()

        val sitesCol = firestore.collection("historicalSites")
        sites.forEach { site ->
            val docRef = sitesCol.document(site.id)
            batch.set(
                docRef,
                mapOf(
                    "id" to site.id,
                    "nama" to site.nama,
                    "description" to site.description,
                    "imageUrl" to site.imageUrl,
                    "latitude" to site.latitude,
                    "longitude" to site.longitude,
                    "radius" to site.radius,
                    "type" to site.type.name,
                    "isHidden" to site.isHidden,
                    "Location" to site.location,
                    "storyGroup" to site.storyGroup,
                    "chapterOrder" to site.chapterOrder,
                    "nextSiteId" to site.nextSiteId
                )
            )
        }

        val chaptersCol = firestore.collection("chapters")
        chapters.forEach { (siteId, content) ->
            val docRef = chaptersCol.document(siteId)
            batch.set(docRef, content)
        }

        val spearsCol = firestore.collection("spears")
        spears.forEach { spear ->
            val docRef = spearsCol.document(spear.id)
            batch.set(
                docRef,
                mapOf(
                    "id" to spear.id,
                    "name" to spear.name,
                    "latitude" to spear.latitude,
                    "longitude" to spear.longitude,
                    "proximityRadius" to spear.proximityRadius,
                    "description" to spear.description,
                    "imageUrl" to spear.imageUrl,
                    "difficulty" to spear.difficulty,
                    "xpReward" to spear.xpReward,
                    "isActive" to true,
                    "active" to true,
                    "createdAt" to spear.createdAt,
                    "locationName" to spear.locationName,
                    "isCollected" to false,
                    "collected" to false,
                    "collectedByUserId" to null,
                    "collectedAt" to null
                )
            )
        }

        batch.commit()
    }

    private fun buildInitialSites(userLocation: GeoPoint?): List<HistoricalSite> {
        val baseSites = mutableListOf(
            HistoricalSite(
                id = "CANDI_SINGOSARI",
                nama = "Candi Singosari",
                description = "Candi peninggalan Kerajaan Singhasari yang dibangun sebagai tempat pendharmaan Raja terakhir Singhasari, Sri Maharaja Kertanegara.",
                imageUrl = "https://images.unsplash.com/photo-1588668214407-6ea9a6d8c272?q=80&w=1000",
                latitude = -7.887778,
                longitude = 112.663889,
                radius = 150f,
                type = SiteType.TEMPLE,
                isHidden = false,
                location = "Singosari, Malang",
                storyGroup = "Kisah Ken Arok & Singhasari",
                chapterOrder = 1,
                nextSiteId = "CANDI_JAGO"
            ),
            HistoricalSite(
                id = "CANDI_JAGO",
                nama = "Candi Jago",
                description = "Candi bercorak Syiwa-Buddha Tantrayana tempat pendharmaan Raja ke-4 Singhasari, Sri Jaya Wisnuwardhana.",
                imageUrl = "https://images.unsplash.com/photo-1596402184320-417e7178b2cd?q=80&w=1000",
                latitude = -8.005833,
                longitude = 112.764167,
                radius = 150f,
                type = SiteType.TEMPLE,
                isHidden = false,
                location = "Tumpang, Malang",
                storyGroup = "Kisah Ken Arok & Singhasari",
                chapterOrder = 2,
                nextSiteId = "CANDI_KIDAL"
            ),
            HistoricalSite(
                id = "CANDI_KIDAL",
                nama = "Candi Kidal",
                description = "Candi pemuliaan Raja Anusapati yang terkenal dengan relief Garudeya sebagai simbol pembebasan dan bakti seorang anak.",
                imageUrl = "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?q=80&w=1000",
                latitude = -8.025833,
                longitude = 112.708889,
                radius = 150f,
                type = SiteType.TEMPLE,
                isHidden = false,
                location = "Pakis, Malang",
                storyGroup = "Kisah Ken Arok & Singhasari",
                chapterOrder = 3,
                nextSiteId = ""
            ),
            HistoricalSite(
                id = "TUGU_PAHLAWAN",
                nama = "Tugu Pahlawan Surabaya",
                description = "Monumen peringatan keberanian arek-arek Suroboyo dalam Pertempuran 10 November 1945 mempertahankan kemerdekaan.",
                imageUrl = "https://images.unsplash.com/photo-1555899434-94d1368aa7af?q=80&w=1000",
                latitude = -7.245394,
                longitude = 112.738075,
                radius = 150f,
                type = SiteType.MONUMENT,
                isHidden = false,
                location = "Surabaya, Jawa Timur",
                storyGroup = "Jejak Perjuangan Nusantara",
                chapterOrder = 1,
                nextSiteId = "LAWANG_SEWU"
            ),
            HistoricalSite(
                id = "LAWANG_SEWU",
                nama = "Lawang Sewu",
                description = "Gedung bersejarah kantor pusat Nederlandsch-Indische Spoorweg Maatschappij (NIS) dan saksi Pertempuran Lima Hari di Semarang.",
                imageUrl = "https://images.unsplash.com/photo-1577717903315-1691ae25ab3f?q=80&w=1000",
                latitude = -6.983889,
                longitude = 110.410833,
                radius = 150f,
                type = SiteType.MUSEUM,
                isHidden = false,
                location = "Semarang, Jawa Tengah",
                storyGroup = "Jejak Perjuangan Nusantara",
                chapterOrder = 2,
                nextSiteId = ""
            )
        )

        val starterLat = userLocation?.latitude ?: -7.849541
        val starterLng = userLocation?.longitude ?: 112.699094

        baseSites.add(
            HistoricalSite(
                id = "GERBANG_LORE_NUSANTARA",
                nama = "Gerbang Lore Nusantara",
                description = "Titik awal penjelajah waktu LoreGate yang ditempatkan di dekat lokasimu untuk memulai petualangan sejarah secara langsung.",
                imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1000",
                latitude = starterLat,
                longitude = starterLng,
                radius = 200f,
                type = SiteType.MONUMENT,
                isHidden = false,
                location = "Titik Awal Penjelajah",
                storyGroup = "Prolog: Penjelajah Waktu",
                chapterOrder = 1,
                nextSiteId = ""
            )
        )

        return baseSites
    }

    private fun buildInitialChapters(sites: List<HistoricalSite>): Map<String, ChapterContent> {
        return sites.associate { site ->
            site.id to when (site.id) {
                "CANDI_SINGOSARI" -> ChapterContent(
                    site = site,
                    heroImageUrl = site.imageUrl,
                    introduction = "Berdiri megah di lembah antara Gunung Arjuno dan Gunung Semeru, Candi Singosari merupakan saksi bisu puncak kejayaan sekaligus runtuhnya Kerajaan Singhasari pada akhir abad ke-13.",
                    sections = listOf(
                        ChapterSection(
                            title = "Mandala Kertanegara",
                            content = "Candi ini dibangun sekitar tahun 1300 M sebagai tempat pendharmaan bagi Sri Maharaja Kertanegara, raja terakhir Singhasari yang terkenal dengan gagasan penyatuan Nusantara melalui Ekspedisi Pamalayu."
                        ),
                        ChapterSection(
                            title = "Arsitektur Pahat dari Atas ke Bawah",
                            content = "Keunikan Candi Singosari terletak pada ornamen Kala yang belum selesai dipahat sepenuhnya, membuktikan bahwa para empu memahat batu candi dari bagian puncak turun ke bagian kaki candi."
                        )
                    ),
                    keyFigures = listOf(
                        KeyFigure(
                            name = "Kertanegara",
                            imageUrl = site.imageUrl,
                            role = "Raja Terakhir Kerajaan Singhasari (1268–1292 M)"
                        ),
                        KeyFigure(
                            name = "Ken Arok",
                            imageUrl = site.imageUrl,
                            role = "Pendiri Wangsa Rajasa & Kerajaan Singhasari"
                        )
                    ),
                    timeline = listOf(
                        TimelineEvent("1222 M", "Ken Arok mendirikan Kerajaan Tumapel / Singhasari"),
                        TimelineEvent("1275 M", "Kertanegara meluncurkan Ekspedisi Pamalayu"),
                        TimelineEvent("1292 M", "Gugurnya Kertanegara akibat pemberontakan Jayakatwang")
                    ),
                    quote = Quote(
                        text = "Bersatu menjaga cakrawala Nusantara dari ancaman luar.",
                        author = "Sumpah Cakrawala Mandala"
                    ),
                    videoUrl = null,
                    conclusion = "Dari reruntuhan Singhasari inilah kelak Raden Wijaya mendirikan Kerajaan Majapahit yang menyatukan kepulauan Nusantara."
                )

                "CANDI_JAGO" -> ChapterContent(
                    site = site,
                    heroImageUrl = site.imageUrl,
                    introduction = "Candi Jago (Jajaghu) dibangun pada abad ke-13 untuk menghormati Raja Wisnuwardhana. Berbeda dengan candi klasik Jawa Tengah, Candi Jago memiliki teras berundak menyerupai punden berundak prasejarah.",
                    sections = listOf(
                        ChapterSection(
                            title = "Sinkretisme Syiwa-Buddha",
                            content = "Di Candi Jago, ajaran Hindu Syiwa dan Buddha Tantrayana berpadu harmonis. Raja Wisnuwardhana didharmakan di sini dalam wujud Buddha Amoghapasa."
                        ),
                        ChapterSection(
                            title = "Relief Tantri & Kunjarakarna",
                            content = "Dinding candi dihiasi relief bergaya wayang kulit Bali yang menceritakan fabel Tantri Kamandaka, Kunjarakarna, hingga Parthayajna."
                        )
                    ),
                    keyFigures = listOf(
                        KeyFigure(
                            name = "Wisnuwardhana",
                            imageUrl = site.imageUrl,
                            role = "Raja ke-4 Singhasari (1248–1268 M)"
                        )
                    ),
                    timeline = listOf(
                        TimelineEvent("1268 M", "Wafatnya Raja Wisnuwardhana"),
                        TimelineEvent("1280 M", "Penyelesaian pembangunan Candi Jajaghu")
                    ),
                    quote = Quote(
                        text = "Yang mulia bagaikan tempat suci puncak kebajikan.",
                        author = "Kitab Nagarakretagama"
                    ),
                    videoUrl = null,
                    conclusion = "Candi Jago menjadi jembatan penting peralihan gaya seni arca klasik menuju gaya khas Jawa Timur pada era Majapahit."
                )

                "CANDI_KIDAL" -> ChapterContent(
                    site = site,
                    heroImageUrl = site.imageUrl,
                    introduction = "Candi Kidal adalah mahakarya arsitektur ramping Jawa Timur yang dibangun pada tahun 1260 M setelah upacara Sraddha (12 tahun wafatnya Raja Anusapati).",
                    sections = listOf(
                        ChapterSection(
                            title = "Kisah Relief Garudeya",
                            content = "Pada kaki candi terdapat tiga panel relief Garuda yang membebaskan ibunya, Winata, dari perbudakan Kadru dengan menukarkan air suci Amerta. Kisah ini adalah bentuk bakti Anusapati kepada ibundanya, Ken Dedes."
                        )
                    ),
                    keyFigures = listOf(
                        KeyFigure(
                            name = "Anusapati",
                            imageUrl = site.imageUrl,
                            role = "Raja ke-2 Singhasari (1227–1248 M)"
                        ),
                        KeyFigure(
                            name = "Ken Dedes",
                            imageUrl = site.imageUrl,
                            role = "Permaisuri Leluhur Raja-Raja Singhasari & Majapahit"
                        )
                    ),
                    timeline = listOf(
                        TimelineEvent("1248 M", "Wafatnya Raja Anusapati"),
                        TimelineEvent("1260 M", "Upacara Sraddha dan peresmian Candi Kidal")
                    ),
                    quote = Quote(
                        text = "Bakti seorang putra adalah jalan menuju keabadian.",
                        author = "Relief Garudeya Candi Kidal"
                    ),
                    videoUrl = null,
                    conclusion = "Dengan selesainya perjalanan di Candi Kidal, kamu telah menuntaskan seluruh bab Kisah Ken Arok & Kerajaan Singhasari!"
                )

                else -> ChapterContent(
                    site = site,
                    heroImageUrl = site.imageUrl,
                    introduction = site.description,
                    sections = listOf(
                        ChapterSection(
                            title = "Sejarah & Makna Situs",
                            content = "Situs ${site.nama} yang terletak di ${site.location} menyimpan jejak penting perjalanan sejarah bangsa. Melalui LoreGate, setiap langkah fisik di dunia nyata membuka lembaran sejarah yang pernah terjadi di titik koordinat ini."
                        ),
                        ChapterSection(
                            title = "Warisan untuk Masa Depan",
                            content = "Dengan mengunjungi dan mempelajari situs ini secara langsung, kamu turut menjaga ingatan kolektif sejarah Nusantara."
                        )
                    ),
                    keyFigures = listOf(
                        KeyFigure(
                            name = "Penjaga Sejarah Nusantara",
                            imageUrl = site.imageUrl,
                            role = "Tokoh Sejarah ${site.nama}"
                        )
                    ),
                    timeline = listOf(
                        TimelineEvent("Masa Lampau", "Peristiwa bersejarah di ${site.nama}"),
                        TimelineEvent("Masa Kini", "Pelestarian situs ${site.nama} sebagai cagar budaya")
                    ),
                    quote = Quote(
                        text = "Bangsa yang besar adalah bangsa yang menghargai sejarah dan pahlawannya.",
                        author = "Arsip Sejarah Nusantara"
                    ),
                    videoUrl = null,
                    conclusion = "Teruslah menjelajah titik-titik bersejarah lainnya di peta LoreGate untuk membuka seluruh kisah yang tersembunyi."
                )
            }
        }
    }

    private fun buildInitialSpears(userLocation: GeoPoint?): List<Spear> {
        val spears = mutableListOf(
            Spear(
                id = "spear_001",
                name = "Tombak Pertama",
                latitude = -7.85028005627044,
                longitude = 112.69964484060873,
                proximityRadius = 50f,
                description = "Tersembunyi tepat di titik pusat area kampus, menanti penemu pertama.",
                difficulty = "EASY",
                xpReward = 25,
                locationName = "Titik Pusat Kampus"
            ),
            Spear(
                id = "spear_002",
                name = "Tombak Keberanian",
                latitude = -7.850750,
                longitude = 112.700100,
                proximityRadius = 50f,
                description = "Melambangkan keberanian, berada sedikit ke timur laut jalur utama.",
                difficulty = "EASY",
                xpReward = 25,
                locationName = "Koridor Timur Laut"
            ),
            Spear(
                id = "spear_003",
                name = "Tombak Pengetahuan",
                latitude = -7.849900,
                longitude = 112.699050,
                proximityRadius = 50f,
                description = "Berisi kebijaksanaan, tersembunyi di sisi barat yang tenang.",
                difficulty = "MEDIUM",
                xpReward = 50,
                locationName = "Jalur Barat"
            ),
            Spear(
                id = "spear_004",
                name = "Tombak Legendaris",
                latitude = -7.850950,
                longitude = 112.698800,
                proximityRadius = 50f,
                description = "Paling langka dan kuat, di sudut barat daya yang tersembunyi.",
                difficulty = "HARD",
                xpReward = 100,
                locationName = "Sudut Barat Daya"
            ),
            Spear(
                id = "spear_005",
                name = "Tombak Petualang",
                latitude = -7.849600,
                longitude = 112.700450,
                proximityRadius = 50f,
                description = "Untuk para petualang sejati, berada di sisi timur yang jarang dilalui.",
                difficulty = "MEDIUM",
                xpReward = 50,
                locationName = "Jalur Timur"
            ),
            Spear(
                id = "spear_006",
                name = "Tombak Horizon",
                latitude = -7.849250,
                longitude = 112.698500,
                proximityRadius = 55f,
                description = "Mengawasi horizon barat laut, dekat area lapang terbuka.",
                difficulty = "EASY",
                xpReward = 25,
                locationName = "Barat Laut Lapang"
            ),
            Spear(
                id = "spear_007",
                name = "Tombak Penjaga Gerbang",
                latitude = -7.850950,
                longitude = 112.700850,
                proximityRadius = 55f,
                description = "Menjaga sisi timur laut, cocok untuk penjelajah awal.",
                difficulty = "EASY",
                xpReward = 25,
                locationName = "Gerbang Timur Laut"
            ),
            Spear(
                id = "spear_008",
                name = "Tombak Awan",
                latitude = -7.851400,
                longitude = 112.699300,
                proximityRadius = 60f,
                description = "Berada sedikit ke selatan, dibalut kabut tipis.",
                difficulty = "MEDIUM",
                xpReward = 50,
                locationName = "Selatan Berkabut"
            ),
            Spear(
                id = "spear_009",
                name = "Tombak Nadi Bumi",
                latitude = -7.850300,
                longitude = 112.698300,
                proximityRadius = 60f,
                description = "Bergetar seiring langkahmu, tersembunyi di barat pusat.",
                difficulty = "HARD",
                xpReward = 100,
                locationName = "Barat Pusat"
            ),
            Spear(
                id = "spear_010",
                name = "Tombak Fajar",
                latitude = -7.849050,
                longitude = 112.700900,
                proximityRadius = 55f,
                description = "Terbit di sisi utara timur, memberi hadiah awal.",
                difficulty = "EASY",
                xpReward = 25,
                locationName = "Utara Timur"
            )
        )

        // Jika koordinat GPS user tersedia saat seed, tambahkan 4 tombak tepat di sekitar user (radius 4-8 meter)
        // supaya langsung terdeteksi oleh radar 10m di MapScreen!
        if (userLocation != null) {
            val earthRadius = 6371000.0
            val offsetsMeters = listOf(4.0 to 0.0, 6.0 to 90.0, 5.0 to 180.0, 7.0 to 270.0)
            offsetsMeters.forEachIndexed { idx, (distMeters, deg) ->
                val rad = deg * PI / 180.0
                val dLat = (distMeters / earthRadius) * (180.0 / PI) * cos(rad)
                val dLng = (distMeters / earthRadius) * (180.0 / PI) * sin(rad) /
                    cos(userLocation.latitude * PI / 180.0)
                val diff = if (idx == 3) "HARD" else if (idx == 1) "MEDIUM" else "EASY"
                val xp = if (diff == "HARD") 100 else if (diff == "MEDIUM") 50 else 25
                spears.add(
                    Spear(
                        id = "spear_nearby_00${idx + 1}",
                        name = "Tombak Pusaka #${idx + 1}",
                        latitude = userLocation.latitude + dLat,
                        longitude = userLocation.longitude + dLng,
                        proximityRadius = 50f,
                        description = "Tombak pusaka kuno yang beresonansi dengan radar di dekat lokasimu!",
                        difficulty = diff,
                        xpReward = xp,
                        locationName = "Sekitar Lokasimu"
                    )
                )
            }
        }

        return spears
    }
}
