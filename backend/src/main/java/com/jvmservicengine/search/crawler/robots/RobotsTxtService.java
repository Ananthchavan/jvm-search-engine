package com.jvmservicengine.search.crawler.robots;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RobotsTxtService {

    private final Map<String, List<String>> domainRulesCache = new ConcurrentHashMap<>();

    private static final String BOT_NAME = "jvmsearchenginebot";

    public boolean isAllowed(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            String domain = url.getHost();
            String path = url.getPath().isEmpty() ? "/" : url.getPath();

            domainRulesCache.computeIfAbsent(domain, this::fetchDisallowedPaths);

            List<String> disallowedPaths = domainRulesCache.get(domain);


            for (String disallowedPath : disallowedPaths) {
                if (path.startsWith(disallowedPath)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Could not parse URL for robots.txt check: {}", targetUrl);
            return false;
        }
    }

    private List<String> fetchDisallowedPaths(String domain) {
        List<String> disallowedPaths = new ArrayList<>();
        String robotsUrl = "https://" + domain + "/robots.txt";

        try {
            String robotsContent = Jsoup.connect(robotsUrl)
                    .ignoreContentType(true)
                    .timeout(5000)
                    .execute()
                    .body();

            boolean isRelevantUserAgent = false;

            for (String line : robotsContent.split("\n")) {
                line = line.trim().toLowerCase();
                if (line.startsWith("user-agent:")) {
                    isRelevantUserAgent = line.contains("*") || line.contains(BOT_NAME);
                } else if (isRelevantUserAgent && line.startsWith("disallow:")) {
                    String path = line.substring("disallow:".length()).trim();
                    if (!path.isEmpty()) {
                        disallowedPaths.add(path);
                    }
                }
            }
            log.info("Fetched robots.txt for {}. Found {} rules.", domain, disallowedPaths.size());
        } catch (Exception e) {
            log.debug("No valid robots.txt found for {}. Assuming all paths allowed.", domain);
        }
        return disallowedPaths;
    }
}