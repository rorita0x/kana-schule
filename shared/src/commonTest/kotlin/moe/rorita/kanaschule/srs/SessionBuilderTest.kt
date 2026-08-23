package moe.rorita.kanaschule.srs

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable

class SessionBuilderTest {

    private val now = 2_000_000_000L

    private val threeGroups = UnlockGroups.ordered.take(3)
    private val unlocked = threeGroups.map { it.id }.toSet()
    private val unlockedIds = threeGroups.flatMap { it.itemIds }

    private fun seen(box: Int, dueAtMs: Long, lapses: Int = 0, leech: Boolean = false) =
        ItemState(box = box, dueAtMs = dueAtMs, reps = 5, lapses = lapses, leech = leech)

    private fun build(
        states: Map<KanaId, ItemState>,
        newBudget: Int = SessionBuilder.NEW_MAX,
        targetSize: Int = SessionBuilder.TARGET_SIZE,
        seed: Int = 7,
    ) = SessionBuilder.review(unlocked, states, now, Random(seed), newBudget, targetSize)

    @Test
    fun ersteSessionBestehtNurAusNeuenZeichen() {
        val plan = build(emptyMap())
        assertEquals(SessionBuilder.NEW_MAX, plan.items.size)
        assertEquals(plan.items.toSet(), plan.newItems.toSet())
    }

    @Test
    fun neueZeichenFolgenDerLeiterreihenfolge() {
        val plan = build(emptyMap(), newBudget = 7)
        assertEquals(unlockedIds.take(7), plan.newItems)
    }

    @Test
    fun neuesBudgetVonNullLiefertKeineNeuenZeichen() {
        val plan = build(emptyMap(), newBudget = 0)
        assertTrue(plan.items.isEmpty())
        assertTrue(plan.newItems.isEmpty())
    }

    @Test
    fun faelligeZeichenKommenZuerstUndNachDatum() {
        // Alle gesehen, aber unterschiedlich lange fällig.
        val states = unlockedIds.mapIndexed { index, id ->
            id to seen(box = 5, dueAtMs = now - index * 1000L)
        }.toMap()

        val plan = build(states, newBudget = 0, targetSize = 5)
        // Die fünf am längsten fälligen sind die letzten fünf der Liste.
        assertEquals(unlockedIds.takeLast(5).toSet(), plan.items.toSet())
    }

    @Test
    fun nochNichtFaelligeZeichenKommenNurAlsAuffuellung() {
        val states = unlockedIds.associateWith { seen(box = 8, dueAtMs = now + 999_999) }
        val plan = build(states, newBudget = 0, targetSize = 4)
        assertEquals(4, plan.items.size, "auffüllen statt leere Session")
    }

    @Test
    fun schwacheZeichenKommenAuchWennNichtFaellig() {
        val states = unlockedIds.associateWith { seen(box = 8, dueAtMs = now + 999_999) }
            .toMutableMap()
        val weak = unlockedIds.last()
        states[weak] = seen(box = 1, dueAtMs = now + 999_999, lapses = 4)

        val plan = build(states, newBudget = 0, targetSize = 3)
        assertTrue(weak in plan.items, "Box 1 muss vor Box 8 kommen")
    }

    @Test
    fun leechesKommenMit() {
        val states = unlockedIds.associateWith { seen(box = 8, dueAtMs = now + 999_999) }
            .toMutableMap()
        val leech = unlockedIds[3]
        states[leech] = seen(box = 6, dueAtMs = now + 999_999, lapses = 7, leech = true)

        val plan = build(states, newBudget = 0, targetSize = 2)
        assertTrue(leech in plan.items)
    }

    @Test
    fun sessionUeberschreitetDieZielgroesseNicht() {
        val states = unlockedIds.associateWith { seen(box = 4, dueAtMs = now - 1) }
        val plan = build(states, newBudget = 4, targetSize = 10)
        assertEquals(10, plan.items.size)
        assertEquals(10, plan.items.toSet().size, "kein Zeichen doppelt")
    }

    @Test
    fun gesperrteZeichenTauchenNieAuf() {
        val alles = KanaTable.all.associate { it.id to seen(box = 4, dueAtMs = now - 1) }
        val plan = build(alles, newBudget = 4)
        assertTrue(plan.items.all { it in unlockedIds }, "nur freigeschaltete Zeichen")
    }

    @Test
    fun gleicherSeedGibtGleicheSession() {
        val states = unlockedIds.associateWith { seen(box = 4, dueAtMs = now - 1) }
        assertEquals(build(states, seed = 99).items, build(states, seed = 99).items)
    }

    @Test
    fun unterschiedlicherSeedMischtAnders() {
        val states = unlockedIds.associateWith { seen(box = 4, dueAtMs = now - 1) }
        assertTrue(build(states, seed = 1).items != build(states, seed = 2).items)
    }

    @Test
    fun keineZweiZeichenDerselbenZeileNebeneinander() {
        val states = unlockedIds.associateWith { seen(box = 4, dueAtMs = now - 1) }
        for (seed in 1..50) {
            val items = build(states, newBudget = 0, targetSize = 15, seed = seed).items
            val rows = items.map { KanaTable.require(it).row }
            for (i in 1 until rows.size) {
                assertTrue(
                    rows[i] != rows[i - 1],
                    "Seed $seed: ${rows[i]} zweimal hintereinander an Position $i",
                )
            }
        }
    }

    @Test
    fun neueZeichenStehenNichtGanzVorn() {
        // Genug alte Zeichen, damit getauscht werden kann.
        val states = unlockedIds.drop(4).associateWith { seen(box = 4, dueAtMs = now - 1) }
        for (seed in 1..50) {
            val plan = build(states, newBudget = 4, targetSize = 20, seed = seed)
            val firstTwo = plan.items.take(2)
            assertTrue(
                firstTwo.none { it in plan.newItems },
                "Seed $seed: neues Zeichen auf Position ${plan.items.indexOfFirst { it in plan.newItems }}",
            )
        }
    }
}
