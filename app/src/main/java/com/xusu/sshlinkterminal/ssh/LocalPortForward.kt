package com.xusu.sshlinkterminal.ssh

internal data class LocalPortForward(
    val localPort: Int,
    val destinationHost: String,
    val destinationPort: Int
) {
    val localAddress: String get() = "127.0.0.1:$localPort"

    companion object {
        private val specPattern = Regex("""(?:127\.0\.0\.1:)?([0-9]+):(\[[^\[\]\s]+\]|[^:\[\]\s]+):([0-9]+)""")

        fun parse(spec: String): LocalPortForward? {
            val match = specPattern.matchEntire(spec.trim()) ?: return null
            val (local, host, destination) = match.destructured
            val localPort = local.toIntOrNull()?.takeIf { it in 1024..65535 } ?: return null
            val destinationPort = destination.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            return LocalPortForward(localPort, host.removeSurrounding("[", "]"), destinationPort)
        }
    }
}
