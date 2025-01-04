package org.iotproject.safehelmet

expect fun onConnectClick(): () -> Unit
expect fun onSendToArduinoClick(): () -> Unit
expect fun name(): String