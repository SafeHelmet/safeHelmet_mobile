package org.iotproject.safehelmet

import dev.bluefalcon.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class) // This annotation makes the function aware of the experimental API
expect fun createUuidFromString(uuidString: String): Uuid

