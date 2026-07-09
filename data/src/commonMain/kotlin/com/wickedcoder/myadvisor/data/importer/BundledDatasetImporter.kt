package com.wickedcoder.myadvisor.data.importer

import com.wickedcoder.myadvisor.data.db.DatasetMetaDao
import com.wickedcoder.myadvisor.data.db.DatasetMetaEntity
import com.wickedcoder.myadvisor.data.db.ImportDao
import com.wickedcoder.myadvisor.data.db.UserCardsDao
import com.wickedcoder.myadvisor.domain.serialization.DomainJson
import kotlin.time.Clock

/** Abstracts where the bundled dataset JSON comes from (:shared provides it via compose resources). */
interface DatasetSource {
    suspend fun readDatasetJson(): String
}

class BundledDatasetImporter(
    private val source: DatasetSource,
    private val importDao: ImportDao,
    private val userCardsDao: UserCardsDao,
    private val datasetMetaDao: DatasetMetaDao,
) : DatasetImporter {

    override suspend fun importBundledDatasetIfNewer() {
        val dataset = DomainJson.decodeFromString(DatasetDto.serializer(), source.readDatasetJson())

        // Monotonic-version gate: only strictly newer datasets are imported.
        val currentVersion = datasetMetaDao.get()?.dataVersion?.let(SemVer::parseOrNull)
        val bundledVersion = SemVer.parseOrNull(dataset.dataVersion)
        if (currentVersion != null && bundledVersion != null && bundledVersion <= currentVersion) return

        val errors = DatasetValidator.validate(
            dataset = dataset,
            ownedCardIds = userCardsDao.getOwnedCardIds().toSet(),
        )
        if (errors.isNotEmpty()) throw DatasetValidationException(errors)

        importDao.replaceCatalog(
            issuers = dataset.issuers.map { it.toEntity() },
            categories = dataset.categories.map { it.toEntity() },
            merchantFamilies = dataset.merchantFamilies.map { it.toEntity() },
            merchants = dataset.merchants.map { it.toEntity() },
            cards = dataset.cards.map { it.toEntity() },
            rules = dataset.cards.flatMap { card -> card.allRules().map { it.toEntity(card.id) } },
            exclusions = dataset.cards.flatMap { card ->
                card.exclusions.mapIndexed { index, exclusion -> exclusion.toEntity(card.id, index) }
            },
            offers = emptyList(), // schema only in v1 (data-model.md)
            meta = DatasetMetaEntity(
                schemaVersion = dataset.schemaVersion,
                dataVersion = dataset.dataVersion,
                generatedAt = dataset.generatedAt,
                importedAt = Clock.System.now().toString(),
            ),
        )
    }
}
