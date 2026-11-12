package com.jvmservicengine.search.crawler.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class UrlSafetyValidator {

    private final Set<Integer> allowedPorts;

    public UrlSafetyValidator(
            @Value("${crawler.allowed-ports:80,443,-1}") List<Integer> allowedPorts) {
        this.allowedPorts = Set.copyOf(allowedPorts);
    }

    public boolean isSafe(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }

        URI uri;
        URL url;
        try {
            uri = URI.create(rawUrl);
            url = uri.toURL();
        } catch (Exception e) {
            log.debug("SSRF-reject: cannot parse URL '{}': {}", rawUrl, e.getMessage());
            return false;
        }

        // 1. Scheme check
        String scheme = url.getProtocol().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            log.debug("SSRF-reject: non-http(s) scheme '{}' in '{}'", scheme, rawUrl);
            return false;
        }

        // 2. No userinfo (e.g. http://user:pass@host/)
        if (uri.getUserInfo() != null) {
            log.debug("SSRF-reject: userinfo present in '{}'", rawUrl);
            return false;
        }

        // 3. Port allowlist (-1 means no explicit port → scheme default)
        int port = url.getPort();
        if (!allowedPorts.contains(port)) {
            log.debug("SSRF-reject: port {} not in allowlist for '{}'", port, rawUrl);
            return false;
        }

        // 4. DNS-resolution check — all resolved addresses must be public
        String host = url.getHost();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrReserved(addr)) {
                    log.warn("SSRF-reject: host '{}' resolved to private/reserved address {} for URL '{}'",
                            host, addr.getHostAddress(), rawUrl);
                    return false;
                }
            }
        } catch (Exception e) {
            // Cannot resolve → reject (fail closed)
            log.debug("SSRF-reject: cannot resolve host '{}': {}", host, e.getMessage());
            return false;
        }

        return true;
    }

    static boolean isPrivateOrReserved(InetAddress addr) {
        if (addr.isLoopbackAddress()) return true;
        if (addr.isSiteLocalAddress()) return true;
        if (addr.isLinkLocalAddress()) return true;
        if (addr.isMulticastAddress()) return true;
        if (addr.isAnyLocalAddress()) return true;

        byte[] b = addr.getAddress();

        if (b.length == 4) {
            // CGNAT: 100.64.0.0/10  → first byte 100, second byte 64–127
            int b0 = b[0] & 0xFF;
            int b1 = b[1] & 0xFF;
            if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;

            // Explicit link-local guard (isSiteLocalAddress misses 169.254 on some JVMs)
            if (b0 == 169 && b1 == 254) return true;
        }

        if (b.length == 16) {
            // IPv4-mapped addresses (::ffff:0:0/96) — check the embedded IPv4 portion
            boolean isMapped = true;
            for (int i = 0; i < 10; i++) if (b[i] != 0) { isMapped = false; break; }
            if (isMapped && (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF) {
                // Reconstruct the embedded IPv4 address and check it
                byte[] ipv4 = new byte[]{b[12], b[13], b[14], b[15]};
                try {
                    return isPrivateOrReserved(InetAddress.getByAddress(ipv4));
                } catch (Exception ignored) {
                    return true; // fail closed
                }
            }
        }

        return false;
    }
}

