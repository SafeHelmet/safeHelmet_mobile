package org.iotproject.safehelmet

import dev.bluefalcon.Uuid
import platform.CoreBluetooth.CBUUID

actual fun createUuidFromString(uuidString: String): Uuid {
    return CBUUID.UUIDWithString(uuidString)
}
