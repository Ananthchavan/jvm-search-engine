package com.jvmservicengine.search.crawler.jsoup;

import com.jvmservicengine.search.crawler.validation.UrlSafetyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@Slf4j
@RequiredArgsConstructor
public class JsoupWebClient {

    public static final String USER_AGENT = "JVMSearchEngineBot/1.0 (+http://localhost:8080)";
    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024; // 2 MB

    private final UrlSafetyValidator urlSafetyValidator;

    public Optional<Document> fetchDocument(String startUrl) {
        String currentUrl = startUrl;

        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!urlSafetyValidator.isSafe(currentUrl)) {
                log.warn("Fetch aborted – URL failed safety check at hop {}: {}", hop, currentUrl);
                return Optional.empty();
            }

            try {
                log.debug("Fetching (hop {}): {}", hop, currentUrl);
                Connection.Response response = Jsoup.connect(currentUrl)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MS)
                        .followRedirects(false)   // we follow manually
                        .maxBodySize(MAX_BODY_BYTES)
                        .ignoreHttpErrors(true)
                        .execute();

                int status = response.statusCode();

                // 3xx redirect — follow the Location header
                if (status >= 300 && status < 400) {
                    String location = response.header("Location");
                    if (location == null || location.isBlank()) {
                        log.warn("Redirect at hop {} has no Location header for: {}", hop, currentUrl);
                        return Optional.empty();
                    }
                    // Resolve relative redirects
                    currentUrl = resolveRelative(currentUrl, location);
                    log.debug("Following redirect ({}) -> {}", status, currentUrl);
                    continue;
                }

                if (status != 200) {
                    log.debug("HTTP {} for {}", status, currentUrl);
                    return Optional.empty();
                }

                // Content-Type guard: only accept text/html
                String contentType = response.contentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("text/html")) {
                    log.debug("Rejected non-HTML content-type '{}' for: {}", contentType, currentUrl);
                    return Optional.empty();
                }

                Document doc = response.parse();
                return Optional.of(doc);

            } catch (Exception e) {
                log.warn("Network/parsing error for URL [{}]: {}", currentUrl, e.getMessage());
                return Optional.empty();
            }
        }

        log.warn("Exceeded max redirects ({}) starting from: {}", MAX_REDIRECTS, startUrl);
        return Optional.empty();
    }

    /** Resolves a potentially relative {@code location} against {@code base}. */
    private static String resolveRelative(String base, String location) {
        try {
            if (location.startsWith("http://") || location.startsWith("https://")) {
                return location;
            }
            java.net.URL baseUrl = new java.net.URL(base);
            return new java.net.URL(baseUrl, location).toString();
        } catch (Exception e) {
            return location; // best-effort; safety validator will catch bad results
        }
    }
}