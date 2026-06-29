package com.api2api.infr.client.provider;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Guards outbound provider requests against insecure schemes and SSRF targets
 * such as loopback, link-local, private and metadata addresses.
 */
final class OutboundUriGuard {

    private OutboundUriGuard() {
    }

    static URI verify(URI uri, boolean allowInsecureHosts) {
        Objects.requireNonNull(uri, "Upstream URI must not be null");
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Upstream URI scheme must not be null");
        }
        if (!allowInsecureHosts && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Upstream host must use https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Upstream host must not be blank");
        }
        if (!allowInsecureHosts && isBlockedHost(host)) {
            throw new IllegalArgumentException("Upstream host is not allowed");
        }
        return uri;
    }

    private static boolean isBlockedHost(String host) {
        String normalized = host.toLowerCase();
        if (normalized.equals("localhost") || normalized.endsWith(".localhost")) {
            return true;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isAnyLocalAddress()
                        || isMetadataAddress(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException exception) {
            return true;
        }
        return false;
    }

    private static boolean isMetadataAddress(InetAddress address) {
        return "169.254.169.254".equals(address.getHostAddress());
    }
}
