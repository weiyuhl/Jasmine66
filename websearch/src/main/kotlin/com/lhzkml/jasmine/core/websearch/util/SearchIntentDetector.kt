package com.lhzkml.jasmine.core.websearch.util

import android.util.Log

object SearchIntentDetector {

    private const val TAG = "SearchIntentDetector"

    private val searchKeywords = listOf(
        "search for", "search", "look up", "find me", "find information about", "find info about",
        "google", "bing", "search the web", "web search",
        "suche nach", "suche", "nachschlagen", "finde mir", "finde informationen", "finde informationen über",
        "im web suchen", "websuche", "suche im internet",
        "buscar", "busca", "búscame", "buscar informacion", "buscar información sobre",
        "buscar en la web", "búsqueda web",
        "rechercher", "cherche", "chercher", "trouver des informations", "trouver des informations sur",
        "recherche sur le web", "recherche web",
        "cerca", "cercare", "cerca informazioni", "cerca informazioni su", "cercami",
        "ricerca sul web", "ricerca web",
        "pesquisar", "pesquise", "procure", "procurar informacoes", "procurar informações sobre",
        "pesquisa na web", "buscar na web",
        "поиск", "найди", "найти", "найди информацию", "найди информацию о",
        "поиск в интернете", "веб-поиск",
        "ara", "arama", "bul", "bana bul", "hakkinda bilgi bul",
        "web'de ara", "web aramasi", "internette ara",
        "szukaj", "szukac", "znajdz", "znajdz mi", "znajdz informacje", "znajdź informacje o",
        "szukaj w internecie", "wyszukiwanie w sieci",
        "ابحث عن", "ابحث", "بحث", "ابحث لي", "ابحث عن معلومات",
        "بحث على الإنترنت", "بحث الويب",
        "検索", "探す", "検索して", "調べる", "情報を探す",
        "ウェブ検索", "ネット検索", "インターネット検索",
        "cari", "pencarian", "cari tahu", "temukan", "cari informasi", "cari informasi tentang",
        "cari di web", "pencarian web",
        "검색", "검색해줘", "찾아줘", "정보 찾아",
        "웹 검색", "인터넷 검색", "알아봐줘", "찾아봐",
        "جستجو", "جستجو کن", "دنبال", "برای من پیدا کن", "پیدا کن",
        "اطلاعات درباره", "جستجوی وب", "جستجو در وب",
        "пошук", "знайди", "знайти", "пошукай", "знайди інформацію", "знайди інформацію про",
        "пошук в інтернеті", "веб-пошук",
        "חפש", "חיפוש", "חפש עבור", "מצא", "מצא מידע", "מצא מידע על",
        "חיפוש ברשת", "חיפוש באינטרנט",
        "搜索", "搜一下", "查找", "帮我找", "找信息", "找一下",
        "网络搜索", "上网搜索", "百度一下", "搜索一下",
    )

    private val weatherKeywords = listOf(
        "weather", "temperature", "forecast", "rain", "snow", "sunny", "cloudy",
        "hot", "cold", "warm", "cool", "humid", "wind", "storm", "climate",
        "wetter", "temperatur", "vorhersage", "regen", "schnee", "sonnig", "bewolkt",
        "clima", "tiempo", "temperatura", "pronostico", "lluvia", "nieve", "soleado", "nublado",
        "meteo", "temps", "temperature", "prevision", "pluie", "neige", "ensoleille", "nuageux",
        "meteo", "tempo", "temperatura", "previsioni", "pioggia", "neve", "soleggiato", "nuvoloso",
        "clima", "tempo", "temperatura", "previsao", "chuva", "neve", "ensolarado", "nublado",
        "погода", "температура", "прогноз", "дождь", "снег", "солнечно", "облачно",
        "hava durumu", "sicaklik", "tahmin", "yagmur", "kar", "gunesli", "bulutlu",
        "pogoda", "temperatura", "prognoza", "deszcz", "snieg", "slonecznie", "pochmurno",
        "الطقس", "درجة الحرارة", "التوقعات", "مطر", "ثلج", "مشمس", "غائم",
        "天気", "気温", "予報", "雨", "雪", "晴れ", "曇り",
        "cuaca", "suhu", "ramalan", "hujan", "salju", "cerah", "berawan",
        "날씨", "기온", "온도", "예보", "비", "눈", "맑음", "흐림",
        "هوا", "آب و هوا", "دما", "درجه حرارت", "پیش‌بینی", "باران", "برف", "آفتابی", "ابری",
        "погода", "температура", "прогноз", "дощ", "сніг", "сонячно", "хмарно",
        "מזג האוויר", "טמפרטורה", "תחזית", "גשם", "שלג", "שמשי", "מעונן",
        "天气", "气温", "温度", "天气预报", "下雨", "下雪", "晴天", "多云", "炎热", "寒冷", "风", "气候",
    )

    private val locationKeywords = listOf(
        "in my city", "my city", "my location", "here", "current location",
        "where I am", "locally", "nearby", "around me",
        "in meiner stadt", "meine stadt", "mein standort", "hier", "aktueller standort", "wo ich bin", "in der nahe",
        "en mi ciudad", "mi ciudad", "mi ubicacion", "aqui", "ubicacion actual", "donde estoy", "cerca",
        "dans ma ville", "ma ville", "ma position", "ici", "position actuelle", "ou je suis", "a proximite",
        "nella mia citta", "la mia citta", "la mia posizione", "qui", "posizione attuale", "dove sono", "vicino",
        "na minha cidade", "minha cidade", "minha localizacao", "aqui", "localizacao atual", "onde estou", "perto",
        "в моем городе", "мой город", "мое местоположение", "здесь", "текущее местоположение", "где я", "рядом", "поблизости",
        "sehrimde", "benim sehrim", "konumum", "burada", "mevcut konum", "neredeyim", "yerel", "yakınlarda",
        "w moim miescie", "moje miasto", "moja lokalizacja", "tutaj", "biezaca lokalizacja", "gdzie jestem", "w poblizu",
        "في مدينتي", "مدينتي", "موقعي", "هنا", "الموقع الحالي", "اين انا", "محلياً", "بالقرب",
        "私の街で", "私の街", "自分の位置", "ここ", "現在地", "自分がいる場所", "ローカルで", "近く",
        "di kota saya", "kota saya", "lokasi saya", "di sini", "lokasi saat ini", "di mana saya", "dekat",
        "내 도시", "내 위치", "여기", "현재 위치", "내가 있는 곳", "지역", "근처", "내 주변",
        "در شهر من", "شهر من", "مکان من", "اینجا", "موقعیت فعلی", "جایی که هستم", "نزدیک",
        "у моєму місті", "моє місто", "моє місцезнаходження", "тут", "поточне місцезнаходження", "де я", "поруч", "поблизу",
        "בעיר שלי", "העיר שלי", "המיקום שלי", "כאן", "מיקום נוכחי", "איפה שאני", "ליד", "בסביבה",
        "我的城市", "我的位置", "这里", "当前位置", "我在哪里", "附近", "我附近", "周边", "本地",
    )

    private val currentInfoKeywords by lazy {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
        listOf(
            (currentYear.toInt() - 2).toString(), (currentYear.toInt() - 1).toString(), currentYear,
            "latest", "newest", "recent", "current", "today", "now",
        "breaking", "news", "update", "announcement", "today's", "this week",
        "right now", "currently",
        "what's the latest", "latest news", "what happened", "news about", "recent news",
        "latest update", "what's new", "what's happening",
        "neueste", "aktuell", "heute", "jetzt", "diese woche", "nachrichten", "aktualisierung",
        "was ist passiert", "nachrichten über", "was ist neu", "neueste nachrichten",
        "ultimas", "reciente", "actual", "hoy", "ahora", "esta semana", "noticias", "actualizacion",
        "qué pasó", "noticias sobre", "últimas noticias", "qué hay de nuevo",
        "dernieres", "recent", "actuel", "aujourd'hui", "maintenant", "cette semaine", "actualites",
        "que s'est-il passé", "actualités sur", "quoi de neuf", "dernières nouvelles",
        "ultime", "recente", "attuale", "oggi", "adesso", "questa settimana", "notizie",
        "cosa è successo", "notizie su", "cosa c'è di nuovo", "ultime notizie",
        "ultimas", "recente", "atual", "hoje", "agora", "esta semana", "noticias",
        "o que aconteceu", "notícias sobre", "o que há de novo", "últimas notícias",
        "последние", "актуальные", "сегодня", "сейчас", "на этой неделе", "новости", "обновление",
        "что произошло", "новости о", "что нового", "последние новости",
        "en son", "guncel", "bugun", "simdi", "bu hafta", "haber", "guncelleme",
        "ne oldu", "hakkında haberler", "en son haberler", "en yeni",
        "ostatnie", "aktualne", "dzisiaj", "teraz", "w tym tygodniu", "wiadomosci",
        "co się stało", "wiadomości o", "co nowego", "najnowsze wiadomości",
        "الأحدث", "حديث", "اليوم", "الآن", "هذا الأسبوع", "أخبار", "تحديث",
        "ماذا حدث", "أخبار عن", "ما الجديد", "آخر الأخبار",
        "最新", "最近", "今日", "今", "今週", "ニュース", "更新",
        "何が起こった", "についてのニュース", "何が起きている", "何が新しい",
        "terbaru", "terkini", "hari ini", "sekarang", "minggu ini", "berita",
        "apa yang terjadi", "berita tentang", "apa yang baru", "berita terkini",
        "최신", "최근", "오늘", "지금", "이번 주", "뉴스", "업데이트",
        "무슨 일이", "뉴스", "새소식", "최신 뉴스",
        "آخرین", "جدیدترین", "امروز", "الان", "در حال حاضر", "این هفته", "اخبار",
        "اعلامیه", "به‌روز", "به روز",
        "останні", "сьогодні", "зараз", "цього тижня", "новини", "оновлення",
        "що сталося", "новини про", "що нового", "найновіші",
        "האחרונות", "היום", "עכשיו", "השבוע", "חדשות", "עדכון",
        "מה קרה", "חדשות על", "מה חדש", "החדשות האחרונות",
        "最新", "最新消息", "最近", "当前", "今天", "本周", "发生了什么",
        "关于的消息", "最新更新", "有什么新鲜事", "什么是最新的",
    )

    fun needsWebSearch(query: String): Boolean {
        val lowerQuery = query.lowercase().trim()

        if (isSimpleMathQuestion(lowerQuery) || isBasicFactualQuestion(lowerQuery)) {
            Log.d(TAG, "Skipping web search for simple query: $lowerQuery")
            return false
        }

        if (lowerQuery.contains("http://") || lowerQuery.contains("https://")) {
            Log.d(TAG, "URL detected, enabling web search: $lowerQuery")
            return true
        }

        if (isDirectUrl(query)) {
            Log.d(TAG, "Direct URL detected, enabling web search: $lowerQuery")
            return true
        }

        val hasSearchKeywords = searchKeywords.any { keyword ->
            lowerQuery.contains(keyword.lowercase())
        }

        val isWeatherQuery = weatherKeywords.any { keyword ->
            lowerQuery.contains(keyword.lowercase())
        }

        val needsCurrentInfo = currentInfoKeywords.any { keyword ->
            lowerQuery.contains(keyword.lowercase())
        }

        val isLocationQuery = locationKeywords.any { keyword ->
            lowerQuery.contains(keyword.lowercase())
        }

        val isCurrentInfoQuestion = lowerQuery.matches(Regex(".*what.*(happening|new|latest|current).*")) ||
                lowerQuery.matches(Regex(".*when.*(is|was|will).*")) ||
                lowerQuery.matches(Regex(".*who.*(won|winning|elected).*")) ||
                lowerQuery.matches(Regex(".*how.*(much|many).*cost.*")) ||
                lowerQuery.matches(Regex(".*price.*of.*")) ||
                lowerQuery.matches(Regex(".*what's happening.*")) ||
                lowerQuery.matches(Regex(".*what's new.*")) ||
                lowerQuery.matches(Regex(".*what are the latest.*")) ||
                lowerQuery.matches(Regex(".*tell me about.*")) ||
                lowerQuery.matches(Regex(".*what's the latest.*"))

        val shouldSearch = hasSearchKeywords || needsCurrentInfo || isCurrentInfoQuestion || isWeatherQuery || isLocationQuery

        if (shouldSearch) {
            Log.d(TAG, "Web search intent detected for: $lowerQuery")
        } else {
            Log.d(TAG, "No web search intent for: $lowerQuery")
        }

        return shouldSearch
    }

    private fun isSimpleMathQuestion(query: String): Boolean {
        val simpleMath = query.matches(Regex("^\\s*\\d+\\s*[+\\-*/]\\s*\\d+\\s*$")) ||
                query.matches(Regex("^\\s*what\\s+is\\s+\\d+\\s*[+\\-*/]\\s*\\d+\\s*\\??\\s*$"))

        if (simpleMath) {
            Log.d(TAG, "Detected simple math query: $query")
        }

        return simpleMath
    }

    private fun isBasicFactualQuestion(query: String): Boolean {
        val basicPatterns = listOf(
            "what is", "what are", "who is", "who was", "when was", "where is",
            "how do", "how does", "why is", "why does", "define", "explain",
        )

        val currentYear = java.time.Year.now().value
        val recentYears = (currentYear - 2..currentYear).map { it.toString() }
        val isBasic = basicPatterns.any { pattern ->
            query.startsWith(pattern) && !query.contains("today") && !query.contains("now") &&
                    !query.contains("current") && !query.contains("latest") &&
                    !recentYears.any { query.contains(it) } && !query.contains("recent")
        }

        if (isBasic) {
            Log.d(TAG, "Detected basic factual question: $query")
        }

        return isBasic
    }

    private fun isDirectUrl(query: String): Boolean {
        val trimmedQuery = query.trim()
        return trimmedQuery.startsWith("http://") ||
                trimmedQuery.startsWith("https://") ||
                trimmedQuery.startsWith("www.") ||
                (trimmedQuery.contains(".") && !trimmedQuery.contains(" ") &&
                 trimmedQuery.length > 4 && trimmedQuery.matches(Regex(".*\\.[a-zA-Z]{2,}$")))
    }

    fun extractSearchQuery(prompt: String): String {
        val lowerPrompt = prompt.lowercase()

        val locationSpecificQuery = when {
            lowerPrompt.contains("temperature") && (lowerPrompt.contains("my city") || lowerPrompt.contains("here")) ->
                "current temperature weather today"
            lowerPrompt.contains("weather") && (lowerPrompt.contains("my city") || lowerPrompt.contains("here")) ->
                "current weather today forecast"
            else -> prompt
        }

        val cleaned = locationSpecificQuery
            .replace(Regex("search for ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("look up ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("find information about ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("what's the latest on ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("tell me about ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("please ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("what's the ", RegexOption.IGNORE_CASE), "")
            .replace(Regex("what is the ", RegexOption.IGNORE_CASE), "")
            .trim()

        return if (cleaned.isNotEmpty()) cleaned else prompt
    }
}
