package com.orbit.app.net

import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * T061 — validates URLs before they reach the network stack.
 *
 * Enforces the preconditions of contracts/network-gateway-contract.md §4:
 * - URL parses as URI
 * - scheme is https (unless [requireHttps] is false for in-process tests)
 * - host is public (not localhost, RFC1918, link-local, CGNAT, or `.onion`)
 * - port is unset or the scheme's default
 *
 * Pure Kotlin + `java.net` only — safe to unit-test on the JVM.
 *
 * @param allowLoopback TEST-ONLY escape hatch: permits loopback hosts
 *   (localhost / 127.0.0.0/8 / ::1) on any port so MockWebServer-backed
 *   contract tests can exercise the real fetch pipeline. Production call
 *   sites MUST leave this false — the SSRF posture of the `:net` process
 *   depends on it.
 */
class UrlValidator(
    private val requireHttps: Boolean = true,
    private val allowLoopback: Boolean = false,
) {

    sealed class Validation {
        data class Valid(val uri: URI, val host: String) : Validation()
        data class Invalid(val errorKind: String, val reason: String) : Validation()
    }

    fun validate(rawUrl: String): Validation {
        if (rawUrl.isBlank()) {
            return Validation.Invalid("invalid_url", "blank url")
        }

        val uri: URI = try {
            URI(rawUrl.trim())
        } catch (e: URISyntaxException) {
            return Validation.Invalid("invalid_url", "unparseable: ${e.message}")
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
            ?: return Validation.Invalid("invalid_url", "missing scheme")

        if (requireHttps && scheme != "https") {
            return Validation.Invalid("not_https", "scheme=$scheme")
        }
        if (!requireHttps && scheme != "http" && scheme != "https") {
            return Validation.Invalid("blocked_scheme", "scheme=$scheme")
        }

        val rawHost = uri.host?.lowercase(Locale.ROOT)
            ?: return Validation.Invalid("invalid_url", "missing host")

        val host = try {
            IDN.toASCII(rawHost).lowercase(Locale.ROOT)
        } catch (_: IllegalArgumentException) {
            return Validation.Invalid("invalid_url", "invalid IDN host")
        }

        if (host.endsWith(".onion")) {
            return Validation.Invalid("blocked_host", "onion host")
        }

        val isLoopback = host == "localhost" || host.endsWith(".localhost") ||
            (isIpLiteral(host) && isLoopbackIp(host))
        if (isLoopback && !allowLoopback) {
            return Validation.Invalid("blocked_host", "localhost")
        }

        val port = uri.port
        if (!(allowLoopback && isLoopback)) {
            when (scheme) {
                "https" -> if (port != -1 && port != 443) {
                    return Validation.Invalid("blocked_scheme", "port=$port")
                }
                "http" -> if (port != -1 && port != 80) {
                    return Validation.Invalid("blocked_scheme", "port=$port")
                }
            }
        }

        if (isIpLiteral(host) && isPrivateIp(host) && !(allowLoopback && isLoopback)) {
            return Validation.Invalid("blocked_host", "private/link-local/loopback ip")
        }

        return Validation.Valid(uri, host)
    }

    companion object {
        private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

        internal fun isIpLiteral(host: String): Boolean {
            if (IPV4.matches(host)) return true
            // Bracketed IPv6 arrives stripped by URI.host; a bare `:` signals literal.
            return host.contains(':')
        }

        internal fun isLoopbackIp(host: String): Boolean {
            val addr = try {
                InetAddress.getByName(host)
            } catch (_: Exception) {
                return false
            }
            return addr.isLoopbackAddress
        }

        internal fun isPrivateIp(host: String): Boolean {
            val addr = try {
                InetAddress.getByName(host)
            } catch (_: Exception) {
                return false
            }
            return addr.isLoopbackAddress ||
                addr.isLinkLocalAddress ||
                addr.isSiteLocalAddress ||
                addr.isAnyLocalAddress ||
                isCgnat(addr.hostAddress) ||
                isUniqueLocalIpv6(addr.hostAddress)
        }

        private fun isCgnat(ip: String?): Boolean {
            if (ip == null) return false
            // 100.64.0.0/10 — carrier-grade NAT (RFC 6598)
            val m = IPV4.matchEntire(ip) ?: return false
            val parts = m.value.split('.').map { it.toInt() }
            return parts[0] == 100 && parts[1] in 64..127
        }

        private fun isUniqueLocalIpv6(ip: String?): Boolean {
            if (ip == null) return false
            // fc00::/7
            val lower = ip.lowercase(Locale.ROOT)
            return lower.startsWith("fc") || lower.startsWith("fd")
        }
    }
}
