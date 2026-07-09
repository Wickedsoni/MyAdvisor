package com.wickedcoder.myadvisor.data

import com.wickedcoder.myadvisor.data.importer.DatasetDto
import com.wickedcoder.myadvisor.data.importer.DatasetValidator
import com.wickedcoder.myadvisor.domain.serialization.DomainJson
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The CI half of ADR-004's gate: the shipped `data/cards.json` must always
 * parse and validate. Runs on the JVM host so it can read the repo file.
 */
class BundledDatasetValidationTest {

    private fun locateDataset(): File =
        listOf("cards.json", "data/cards.json", "../data/cards.json")
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("data/cards.json not found from working dir ${File(".").absolutePath}")

    @Test
    fun `shipped dataset parses and passes the validator`() {
        val datasetFile = locateDataset().absoluteFile
        val repoRoot = datasetFile.parentFile.parentFile // <repo>/data/cards.json → <repo>

        val dataset = DomainJson.decodeFromString(DatasetDto.serializer(), datasetFile.readText())
        assertEquals(emptyList(), DatasetValidator.validate(dataset))
        assertTrue(dataset.cards.isNotEmpty(), "seed dataset should not be empty")

        // Phase 1 DoD: every seeded rule has a research-trail entry
        dataset.cards.forEach { card ->
            assertTrue(
                File(repoRoot, card.researchRef).isDirectory,
                "card '${card.id}' researchRef '${card.researchRef}' has no research folder in the repo",
            )
        }
    }
}
