package org.iotproject.safehelmet

import kotlin.uuid.Uuid as kUuid
import dev.bluefalcon.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
actual fun createUuidFromString(uuidString: String): Uuid {
    return kUuid.parse(uuidString)
}
