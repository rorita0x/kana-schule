package moe.rorita.kanaschule.store

/**
 * Zugriff auf den gespeicherten Lernstand.
 *
 * Absichtlich blockierend und nicht suspend: die Implementierung schreibt ein
 * paar Dutzend Kilobyte, und ein synchrones Interface hält die Tests frei von
 * Coroutine-Infrastruktur. Aufrufer sind dafür zuständig, das nicht auf dem
 * Hauptthread zu tun.
 */
interface ProgressStore {

    /** Liefert bei kaputter oder fehlender Datei einen frischen Zustand. */
    fun load(): AppState

    fun save(state: AppState)

    fun appendReview(entry: ReviewEntry)

    /** Die letzten [limit] Antworten, älteste zuerst. */
    fun readReviews(limit: Int = DEFAULT_REVIEW_LIMIT): List<ReviewEntry>

    /** Was beim letzten Laden schiefging - für eine Meldung im UI. */
    val lastLoadProblem: String? get() = null

    companion object {
        const val DEFAULT_REVIEW_LIMIT = 5000
    }
}

/** Für Tests und Vorschauen: hält alles nur im Speicher. */
class InMemoryProgressStore(initial: AppState = AppState()) : ProgressStore {
    private var state: AppState = initial
    private val reviews = ArrayList<ReviewEntry>()

    override fun load(): AppState = state

    override fun save(state: AppState) {
        this.state = state
    }

    override fun appendReview(entry: ReviewEntry) {
        reviews += entry
    }

    override fun readReviews(limit: Int): List<ReviewEntry> = reviews.takeLast(limit)
}
