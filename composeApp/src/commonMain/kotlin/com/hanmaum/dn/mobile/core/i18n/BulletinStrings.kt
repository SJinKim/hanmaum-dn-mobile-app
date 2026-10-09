package com.hanmaum.dn.mobile.core.i18n

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** App chrome is translated; names, section titles and editorial content remain server-owned. */
data class BulletinStrings(
    private val language: AppLocale,
    val title: String,
    val current: String,
    val history: String,
    val worshipOrder: String,
    val sermonSharing: String,
    val sermon: String,
    val scripture: String,
    val openingPrayer: String,
    val songs: String,
    val openingSongs: String,
    val offeringSong: String,
    val announcements: String,
    val scriptureReading: String,
    val sermonProclamation: String,
    val responsePrayer: String,
    val responseSong: String,
    val closing: String,
    val viewSharing: String,
    val emptyTitle: String,
    val emptyBody: String,
    val unavailable: String,
    val errorTitle: String,
    val errorBody: String,
    val historyEmpty: String,
    val sharingEmpty: String,
    val refresh: String,
    val refreshing: String,
    val loading: String,
    val loadMore: String,
    val published: String,
    val offline: String,
    val sunday: String,
    val question: String,
) {
    val masthead: String = "HANMAUM D+N · WEEKLY"
    fun volume(number: Int): String = "VOL. $number"
    fun questionBadge(number: Int): String = "Q$number"
    fun questionNumber(number: Int): String = "$question $number"

    fun questions(count: Int): String = when (language) {
        AppLocale.EN -> "$count ${if (count == 1) "question" else "questions"}"
        AppLocale.KO -> "질문 ${count}개"
        AppLocale.DE -> "$count ${if (count == 1) "Frage" else "Fragen"}"
    }

    fun songCount(count: Int): String = when (language) {
        AppLocale.EN -> "$count ${if (count == 1) "song" else "songs"}"
        AppLocale.KO -> "${count}곡"
        AppLocale.DE -> "$count ${if (count == 1) "Lied" else "Lieder"}"
    }

    fun date(date: LocalDate, full: Boolean = false): String {
        val month = date.monthNumber.toString().padStart(2, '0')
        val day = date.day.toString().padStart(2, '0')
        val short = if (language == AppLocale.DE) "$day.$month" else "$month.$day"
        return when {
            !full -> short
            language == AppLocale.KO -> "${date.year}.$short"
            else -> "$short.${date.year}"
        }
    }

    fun offlineSince(date: String): String = when (language) {
        AppLocale.KO -> "${date}에 저장된 주보예요"
        else -> "$offline $date"
    }

    fun publishedOn(date: String): String = when (language) {
        AppLocale.KO -> "$date $published"
        AppLocale.EN -> "$published · $date"
        AppLocale.DE -> "$published am $date"
    }

    fun metadata(vararg parts: String?): String = parts.mapNotNull {
        it?.takeIf(String::isNotBlank)
    }.joinToString(" · ")

    fun serviceLine(name: String?, time: LocalTime?): String = metadata(name, time?.let {
        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
    })

    fun sharingSummary(reference: String?, count: Int): String = metadata(reference, questions(count))
    fun historyCaption(serviceDate: LocalDate, number: Int?): String =
        metadata(date(serviceDate, full = true), number?.let(::volume))
}

val EnBulletinStrings = BulletinStrings(
    language = AppLocale.EN,
    title = "Bulletin",
    current = "Latest bulletin",
    history = "Past bulletins",
    worshipOrder = "Order of worship",
    sermonSharing = "Sermon sharing",
    sermon = "Today's message",
    scripture = "Scripture",
    openingPrayer = "Opening prayer",
    songs = "Songs",
    openingSongs = "Opening praise",
    offeringSong = "Offering song",
    announcements = "Church news",
    scriptureReading = "Scripture reading",
    sermonProclamation = "Sermon",
    responsePrayer = "Response prayer",
    responseSong = "Response song",
    closing = "Closing",
    viewSharing = "Read sermon sharing",
    emptyTitle = "No bulletin published yet",
    emptyBody = "The bulletin will appear here once it is published. You can still read past editions.",
    unavailable = "This bulletin is no longer available",
    errorTitle = "Couldn't load the bulletin",
    errorBody = "Check your connection and try again.",
    historyEmpty = "No past bulletins yet",
    sharingEmpty = "Sermon sharing hasn't been added yet.",
    refresh = "Refresh",
    refreshing = "Refreshing…",
    loading = "Loading bulletin…",
    loadMore = "Load more",
    published = "Published",
    offline = "Saved copy · saved on",
    sunday = "SUN",
    question = "Question",
)
val KoBulletinStrings = BulletinStrings(
    language = AppLocale.KO,
    title = "주보",
    current = "이번 주 주보",
    history = "지난 주보 보기",
    worshipOrder = "예배 순서",
    sermonSharing = "설교 나눔",
    sermon = "오늘의 말씀",
    scripture = "본문",
    openingPrayer = "대표기도",
    songs = "찬양",
    openingSongs = "예배를 여는 찬양",
    offeringSong = "헌금송",
    announcements = "교회소식",
    scriptureReading = "성경봉독",
    sermonProclamation = "말씀선포",
    responsePrayer = "응답기도",
    responseSong = "응답찬양",
    closing = "마침",
    viewSharing = "설교 나눔 보기",
    emptyTitle = "아직 게시된 주보가 없어요",
    emptyBody = "주보가 게시되면 여기에서 볼 수 있어요. 지난 주보는 언제든 다시 볼 수 있어요.",
    unavailable = "이 주보는 더 이상 볼 수 없어요",
    errorTitle = "주보를 불러오지 못했어요",
    errorBody = "인터넷 연결을 확인한 뒤 다시 시도해 주세요.",
    historyEmpty = "지난 주보가 아직 없어요",
    sharingEmpty = "설교 나눔이 아직 등록되지 않았어요.",
    refresh = "새로고침",
    refreshing = "새로고침 중…",
    loading = "주보를 불러오는 중…",
    loadMore = "더 보기",
    published = "게시",
    offline = "저장된 주보 · 저장일",
    sunday = "SUN",
    question = "질문",
)
val DeBulletinStrings = BulletinStrings(
    language = AppLocale.DE,
    title = "Gemeindebrief",
    current = "Aktueller Gemeindebrief",
    history = "Frühere Ausgaben",
    worshipOrder = "Gottesdienstablauf",
    sermonSharing = "Predigtgespräch",
    sermon = "Die heutige Predigt",
    scripture = "Bibeltext",
    openingPrayer = "Anfangsgebet",
    songs = "Lieder",
    openingSongs = "Lobpreis",
    offeringSong = "Lied zur Kollekte",
    announcements = "Gemeindenachrichten",
    scriptureReading = "Bibellesung",
    sermonProclamation = "Predigt",
    responsePrayer = "Antwortgebet",
    responseSong = "Antwortlied",
    closing = "Abschluss",
    viewSharing = "Predigtgespräch lesen",
    emptyTitle = "Noch kein Gemeindebrief veröffentlicht",
    emptyBody = "Nach der Veröffentlichung findest du den Gemeindebrief hier. Frühere Ausgaben bleiben lesbar.",
    unavailable = "Diese Ausgabe ist nicht mehr verfügbar",
    errorTitle = "Gemeindebrief konnte nicht geladen werden",
    errorBody = "Prüfe deine Verbindung und versuche es erneut.",
    historyEmpty = "Noch keine früheren Ausgaben",
    sharingEmpty = "Das Predigtgespräch wurde noch nicht ergänzt.",
    refresh = "Aktualisieren",
    refreshing = "Wird aktualisiert…",
    loading = "Gemeindebrief wird geladen…",
    loadMore = "Mehr laden",
    published = "Veröffentlicht",
    offline = "Gespeicherte Ausgabe · gespeichert am",
    sunday = "SO",
    question = "Frage",
)
