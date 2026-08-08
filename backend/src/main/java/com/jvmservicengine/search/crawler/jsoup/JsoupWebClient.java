package com.jvmservicengine.search.crawler.jsoup;


import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
public class JsoupWebClient {

    private static final String USER_AGENT = "JVMSearchEngineBot/1.0 (+http://localhost:8080)";
    private static final int TIMEOUT_MS = 5000;

    // 1.5second delay between requests, a safe polite delay
    private static final long POLITENESS_DELAY_MS = 1500;

    // Attempts to fetch a webpage and parse it into a JSoup Document
    // It returns an optional to handle 404s, timeouts etc.
    public Optional<Document> fetchDocument(String url) {
        try {

            Thread.sleep(POLITENESS_DELAY_MS);

            log.debug("Connecting to URL: {}", url);

            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            return Optional.of(document);
        } catch (IOException e){
            log.warn("Network/Parsing error for URL [{}]: {}", url, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e){
            log.error("Crawler thread interrupted during sleep delay", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }


}
