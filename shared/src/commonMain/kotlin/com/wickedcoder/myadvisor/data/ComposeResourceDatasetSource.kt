package com.wickedcoder.myadvisor.data

import com.wickedcoder.myadvisor.data.importer.DatasetSource
import myadvisor.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Reads the bundled dataset copied from data/cards.json at build time. */
class ComposeResourceDatasetSource : DatasetSource {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun readDatasetJson(): String =
        Res.readBytes("files/cards.json").decodeToString()
}
