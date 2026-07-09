package com.wickedcoder.myadvisor.domain.serialization

import kotlinx.serialization.json.Json

/**
 * The single Json configuration shared by the dataset importer and the
 * Room `condition_json` column (ADR-006): one serializer, one wire format.
 *
 * Strict by design — unknown keys fail the import, they never reach runtime.
 */
val DomainJson: Json = Json {
    ignoreUnknownKeys = false
    encodeDefaults = false
    classDiscriminator = "type"
}
